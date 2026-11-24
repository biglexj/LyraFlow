#![cfg_attr(target_os = "windows", windows_subsystem = "windows")]

mod about;
mod audio;
mod coordinator;
mod history;
mod icons;
mod modal_backdrop;
mod platform_win;
mod preferences;
mod providers;
mod scanner;
mod secrets;
mod text_input;
mod theme;
mod updates;

use audio::{Capture, MAX_SECONDS};
use coordinator::{CoordinatorEvent, DictationState, QuotaExhaustion, TranscriptionJobConfig};
use gpui::{
    App, Application, Bounds, ClickEvent, ClipboardItem, Context, ExternalPaths, FocusHandle,
    FontWeight, KeyBinding, KeyDownEvent, SharedString, Subscription, Timer, TitlebarOptions,
    Window, WindowAppearance, WindowBounds, WindowOptions, actions, div, prelude::*, px, rgb, rgba,
    size,
};
use history::TranscriptionHistoryEntry;
use icons::{IconType, LyraAssets, lyra_icon};
use platform_win::{
    DesktopTextInjector, HotkeySignal, KeyboardShortcut, ShortcutKey, ShortcutModifier,
    WindowsGlobalShortcut,
};
use preferences::{
    AiProvider, AppPreferences, HistoryRetention, SystemPromptMode, WhisperLanguage,
};
use providers::{TranscriptionRequest, WhisperModel, WhisperSetupState};
use std::{
    collections::BTreeSet,
    fs,
    path::PathBuf,
    sync::{
        Arc, Mutex,
        atomic::{AtomicBool, Ordering},
        mpsc,
    },
    time::{Duration, Instant},
};
use text_input::{TextFieldState, render_text_field};
use theme::{Palette, ThemeMode, ThemeName};

actions!(
    lyraflow,
    [
        ToggleRecord,
        CancelRecord,
        ToggleMode,
        OpenAbout,
        CloseAbout,
        UseAurora,
        UseClassic,
        FocusNext,
        FocusPrevious
    ]
);

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum Destination {
    Home,
    History,
    Settings,
}

impl Destination {
    fn label(self) -> &'static str {
        match self {
            Destination::Home => "Inicio",
            Destination::History => "Historial",
            Destination::Settings => "Ajustes",
        }
    }

    fn icon(self) -> IconType {
        match self {
            Destination::Home => IconType::Home,
            Destination::History => IconType::History,
            Destination::Settings => IconType::Settings,
        }
    }
}

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum SettingsTab {
    General,
    Models,
}

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum ActiveDialog {
    None,
    About,
    ApiKey,
    ModelSelector,
    WhisperSelector,
}

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum ShortcutCaptureTarget {
    None,
    Primary,
    Cancel,
}

struct ScanOutcome {
    provider: AiProvider,
    auto_select_best: bool,
    result: Result<Vec<String>, String>,
}

struct FocusControls {
    nav_theme: FocusHandle,
    nav_home: FocusHandle,
    nav_history: FocusHandle,
    nav_settings: FocusHandle,
    nav_about: FocusHandle,
    record: FocusHandle,
    cancel: FocusHandle,
    inject_result: FocusHandle,
    clear_result: FocusHandle,
    about_close: FocusHandle,
    shortcut_primary: FocusHandle,
    shortcut_cancel: FocusHandle,
}

impl FocusControls {
    fn new(cx: &mut Context<LyraFlowApp>) -> Self {
        let handle = |cx: &mut Context<LyraFlowApp>| cx.focus_handle().tab_stop(true);
        Self {
            nav_theme: handle(cx),
            nav_home: handle(cx),
            nav_history: handle(cx),
            nav_settings: handle(cx),
            nav_about: handle(cx),
            record: handle(cx),
            cancel: handle(cx),
            inject_result: handle(cx),
            clear_result: handle(cx),
            about_close: handle(cx),
            shortcut_primary: handle(cx),
            shortcut_cancel: handle(cx),
        }
    }
}

struct LyraFlowApp {
    icon: Arc<gpui::Image>,
    focus: FocusHandle,
    controls: FocusControls,
    prefs: AppPreferences,
    api_key: String,
    api_key_field: TextFieldState,
    dialog_api_key_field: TextFieldState,
    endpoint_field: TextFieldState,
    model_field: TextFieldState,
    prompt_field: TextFieldState,
    destination: Destination,
    settings_tab: SettingsTab,
    active_dialog: ActiveDialog,
    about_tech_open: bool,
    about_backdrop: Option<Arc<gpui::Image>>,
    update_state: updates::State,
    update_manual: bool,
    update_rx: Option<mpsc::Receiver<Result<updates::Outcome, String>>>,
    show_experimental: bool,
    system_dark: bool,

    // Audio + coordinación
    capture: Option<Capture>,
    dictation_state: DictationState,
    last_request: Option<TranscriptionRequest>,
    quota_exhaustion: Option<QuotaExhaustion>,
    transcription_cancel: Option<Arc<AtomicBool>>,
    coord_tx: mpsc::Sender<CoordinatorEvent>,
    coord_rx: mpsc::Receiver<CoordinatorEvent>,

    // Descubrimiento de modelos
    is_scanning_models: bool,
    scan_tx: mpsc::Sender<ScanOutcome>,
    scan_rx: mpsc::Receiver<ScanOutcome>,

    // Whisper local
    whisper_state: Arc<Mutex<WhisperSetupState>>,
    selected_whisper_model: WhisperModel,

    // Historial
    history_entries: Vec<TranscriptionHistoryEntry>,

    // Plataforma Windows
    hotkey_tx: mpsc::Sender<HotkeySignal>,
    hotkey_rx: mpsc::Receiver<HotkeySignal>,
    global_shortcut: Option<WindowsGlobalShortcut>,
    text_injector: DesktopTextInjector,
    shortcut_capture: ShortcutCaptureTarget,
    shortcut_feedback: Option<(ShortcutCaptureTarget, String)>,

    // Telemetría y mensajes UI
    seconds: f64,
    peak: f32,
    wave_phase: f32,
    toast: Option<(String, Instant)>,
    _appearance_observer: Subscription,
}

fn is_dark(appearance: WindowAppearance) -> bool {
    matches!(
        appearance,
        WindowAppearance::Dark | WindowAppearance::VibrantDark
    )
}

fn mode_icon(mode: ThemeMode) -> IconType {
    match mode {
        ThemeMode::System => IconType::System,
        ThemeMode::Light => IconType::Sun,
        ThemeMode::Dark => IconType::Moon,
    }
}

fn format_duration(seconds: f64) -> String {
    let total = seconds.max(0.0) as u64;
    format!("{:02}:{:02}", total / 60, total % 60)
}

fn status_title(state: &DictationState) -> String {
    match state {
        DictationState::Idle => "Listo para escucharte".into(),
        DictationState::Listening => "Te estoy escuchando".into(),
        DictationState::Transcribing { .. } | DictationState::AttemptFailed { .. } => {
            "Dándole forma a tus ideas".into()
        }
        DictationState::Completed { elapsed_millis, .. } => {
            format!("Listo en {elapsed_millis} ms")
        }
        DictationState::Failed(_) => "Algo interrumpió el dictado".into(),
    }
}

fn map_shortcut_key(key: &str) -> Option<ShortcutKey> {
    match key.to_lowercase().as_str() {
        "space" => Some(ShortcutKey::Space),
        "a" => Some(ShortcutKey::A),
        "b" => Some(ShortcutKey::B),
        "c" => Some(ShortcutKey::C),
        "d" => Some(ShortcutKey::D),
        "e" => Some(ShortcutKey::E),
        "f" => Some(ShortcutKey::F),
        "g" => Some(ShortcutKey::G),
        "h" => Some(ShortcutKey::H),
        "i" => Some(ShortcutKey::I),
        "j" => Some(ShortcutKey::J),
        "k" => Some(ShortcutKey::K),
        "l" => Some(ShortcutKey::L),
        "m" => Some(ShortcutKey::M),
        "n" => Some(ShortcutKey::N),
        "o" => Some(ShortcutKey::O),
        "p" => Some(ShortcutKey::P),
        "q" => Some(ShortcutKey::Q),
        "r" => Some(ShortcutKey::R),
        "s" => Some(ShortcutKey::S),
        "t" => Some(ShortcutKey::T),
        "u" => Some(ShortcutKey::U),
        "v" => Some(ShortcutKey::V),
        "w" => Some(ShortcutKey::W),
        "x" => Some(ShortcutKey::X),
        "y" => Some(ShortcutKey::Y),
        "z" => Some(ShortcutKey::Z),
        "f1" => Some(ShortcutKey::F1),
        "f2" => Some(ShortcutKey::F2),
        "f3" => Some(ShortcutKey::F3),
        "f4" => Some(ShortcutKey::F4),
        "f5" => Some(ShortcutKey::F5),
        "f6" => Some(ShortcutKey::F6),
        "f7" => Some(ShortcutKey::F7),
        "f8" => Some(ShortcutKey::F8),
        "f9" => Some(ShortcutKey::F9),
        "f10" => Some(ShortcutKey::F10),
        "f11" => Some(ShortcutKey::F11),
        _ => None,
    }
}

fn key_down_to_shortcut(event: &KeyDownEvent) -> Option<KeyboardShortcut> {
    let key = map_shortcut_key(&event.keystroke.key)?;
    let mut modifiers = BTreeSet::new();
    if event.keystroke.modifiers.control {
        modifiers.insert(ShortcutModifier::Control);
    }
    if event.keystroke.modifiers.alt {
        modifiers.insert(ShortcutModifier::Alt);
    }
    if event.keystroke.modifiers.shift {
        modifiers.insert(ShortcutModifier::Shift);
    }
    if event.keystroke.modifiers.platform {
        modifiers.insert(ShortcutModifier::Windows);
    }
    Some(KeyboardShortcut { modifiers, key })
}

impl LyraFlowApp {
    fn new(window: &mut Window, cx: &mut Context<Self>) -> Self {
        let focus = cx.focus_handle();
        focus.focus(window);
        let controls = FocusControls::new(cx);
        let prefs = preferences::load_all();
        let api_key = secrets::load_api_key(prefs.provider);

        let api_key_field = TextFieldState::new(&api_key, cx);
        let dialog_api_key_field = TextFieldState::new(&api_key, cx);
        let endpoint_field = TextFieldState::new(&prefs.endpoint, cx);
        let model_field = TextFieldState::new(&prefs.model, cx);
        let initial_prompt = if prefs.prompt_mode == SystemPromptMode::Custom
            && (prefs.system_prompt == preferences::DEFAULT_SYSTEM_PROMPT
                || prefs.system_prompt == preferences::LITERAL_SYSTEM_PROMPT)
        {
            ""
        } else {
            &prefs.system_prompt
        };
        let prompt_field = TextFieldState::new(initial_prompt, cx);

        let system_dark = is_dark(window.appearance());
        let appearance_observer = cx.observe_window_appearance(window, |view, window, cx| {
            let dark = is_dark(window.appearance());
            if view.system_dark != dark {
                view.system_dark = dark;
                cx.notify();
            }
        });

        let (coord_tx, coord_rx) = mpsc::channel();
        let (scan_tx, scan_rx) = mpsc::channel();
        let (hotkey_tx, hotkey_rx) = mpsc::channel();

        let global_shortcut = WindowsGlobalShortcut::start(
            prefs.shortcut.clone(),
            prefs.cancel_shortcut.clone(),
            hotkey_tx.clone(),
        );
        platform_win::set_auto_start_enabled(prefs.launch_at_startup);

        let whisper_initial = providers::current_whisper_state(None);
        let selected_whisper_model = whisper_initial.model.unwrap_or(WhisperModel::Base);
        let whisper_state = Arc::new(Mutex::new(whisper_initial));

        let history_entries = history::load_history(prefs.history_retention.hours());

        cx.spawn(async move |view, cx| {
            loop {
                Timer::after(Duration::from_millis(50)).await;
                if view.update(cx, |view, cx| view.tick(cx)).is_err() {
                    break;
                }
            }
        })
        .detach();

        let mut app = Self {
            icon: Arc::new(gpui::Image::from_bytes(
                gpui::ImageFormat::Png,
                include_bytes!("../assets/branding/icons/icon-transparent.png").to_vec(),
            )),
            focus,
            controls,
            prefs,
            api_key,
            api_key_field,
            dialog_api_key_field,
            endpoint_field,
            model_field,
            prompt_field,
            destination: Destination::Home,
            settings_tab: SettingsTab::General,
            active_dialog: ActiveDialog::None,
            about_tech_open: false,
            about_backdrop: None,
            update_state: updates::State::Idle,
            update_manual: false,
            update_rx: None,
            show_experimental: false,
            system_dark,
            capture: None,
            dictation_state: DictationState::Idle,
            last_request: None,
            quota_exhaustion: None,
            transcription_cancel: None,
            coord_tx,
            coord_rx,
            is_scanning_models: false,
            scan_tx,
            scan_rx,
            whisper_state,
            selected_whisper_model,
            history_entries,
            hotkey_tx,
            hotkey_rx,
            global_shortcut: Some(global_shortcut),
            text_injector: DesktopTextInjector::default(),
            shortcut_capture: ShortcutCaptureTarget::None,
            shortcut_feedback: None,
            seconds: 0.0,
            peak: 0.0,
            wave_phase: 0.0,
            toast: None,
            _appearance_observer: appearance_observer,
        };

        platform_win::apply_titlebar_icon();
        window.defer(cx, |_, _| {
            platform_win::apply_titlebar_icon();
        });

        if !app.api_key.trim().is_empty()
            && app
                .prefs
                .discovered_models
                .get(&app.prefs.provider)
                .map(|m| m.is_empty())
                .unwrap_or(true)
        {
            app.scan_models(app.prefs.model.trim().is_empty(), None);
        }

        app.check_updates_mode(false, cx);
        app
    }

    fn show_toast(&mut self, msg: impl Into<String>) {
        self.toast = Some((msg.into(), Instant::now() + Duration::from_secs(4)));
    }

    fn tick(&mut self, cx: &mut Context<Self>) {
        let mut changed = false;
        if let Some(receiver) = &self.update_rx {
            match receiver.try_recv() {
                Ok(result) => {
                    let manual = self.update_manual;
                    self.update_manual = false;
                    self.update_state = match &result {
                        Ok(outcome) => updates::State::Done(outcome.clone()),
                        Err(error) => updates::State::Failed(error.clone()),
                    };
                    if manual
                        && matches!(
                            result,
                            Ok(updates::Outcome::Current(_))
                                | Ok(updates::Outcome::Ahead(_)) if !cfg!(debug_assertions)
                        )
                    {
                        self.show_toast("No se han encontrado actualizaciones");
                    } else if manual && matches!(result, Ok(updates::Outcome::Current(_))) {
                        self.show_toast("No se han encontrado actualizaciones");
                    }
                    self.update_rx = None;
                    changed = true;
                }
                Err(mpsc::TryRecvError::Disconnected) => {
                    self.update_manual = false;
                    self.update_state = updates::State::Failed("La consulta se interrumpió. Vuelve a intentarlo.".into());
                    self.update_rx = None;
                    changed = true;
                }
                Err(mpsc::TryRecvError::Empty) => {}
            }
        }

        if let Some((_, expires_at)) = &self.toast
            && Instant::now() >= *expires_at
        {
            self.toast = None;
            changed = true;
        }

        while let Ok(signal) = self.hotkey_rx.try_recv() {
            changed = true;
            match signal {
                HotkeySignal::ToggleRecord => self.toggle_dictation(cx),
                HotkeySignal::CancelRecord => self.cancel_dictation(cx),
            }
        }

        while let Ok(event) = self.coord_rx.try_recv() {
            changed = true;
            match event {
                CoordinatorEvent::State(state) => {
                    self.dictation_state = state;
                }
                CoordinatorEvent::Quota(quota) => {
                    self.quota_exhaustion = Some(quota);
                }
                CoordinatorEvent::Done {
                    result,
                    auto_inject,
                } => {
                    if !result.raw_text.trim().is_empty() && self.history_enabled() {
                        let entry = TranscriptionHistoryEntry {
                            id: format!(
                                "{}-{}",
                                history::now_millis(),
                                self.history_entries.len() + 1000
                            ),
                            timestamp_ms: history::now_millis(),
                            raw_transcript: result.raw_text.clone(),
                            refined_text: result.raw_text.clone(),
                            provider_name: result.provider.clone(),
                            audio_duration_ms: result.elapsed_millis,
                        };
                        let mut updated = vec![entry];
                        updated.extend(self.history_entries.clone());
                        if let Ok(saved) =
                            history::save_history(&updated, self.prefs.history_retention.hours())
                        {
                            self.history_entries = saved;
                        }
                    }
                    let final_text = result.raw_text.clone();
                    self.dictation_state = DictationState::Completed {
                        raw_text: result.raw_text,
                        refined_text: final_text.clone(),
                        provider: result.provider,
                        elapsed_millis: result.elapsed_millis,
                    };
                    if auto_inject && !final_text.trim().is_empty() {
                        let injector = std::mem::take(&mut self.text_injector);
                        let text_to_inject = final_text;
                        std::thread::spawn(move || {
                            injector.inject(&text_to_inject);
                        });
                    }
                }
            }
        }

        while let Ok(outcome) = self.scan_rx.try_recv() {
            changed = true;
            self.is_scanning_models = false;
            match outcome.result {
                Ok(models) => {
                    if models.is_empty() {
                        self.show_toast("No se detectaron modelos compatibles en este proveedor.");
                    } else {
                        self.prefs
                            .discovered_models
                            .insert(outcome.provider, models.clone());
                        if outcome.provider == self.prefs.provider
                            && (outcome.auto_select_best || self.prefs.model.trim().is_empty())
                        {
                            self.prefs.model = models[0].clone();
                            self.model_field.value = self.prefs.model.clone();
                        }
                        let _ = preferences::save_all(&self.prefs);
                        self.show_toast(format!("Modelos escaneados: {}", models.len()));
                    }
                }
                Err(err) => {
                    self.show_toast(err);
                }
            }
        }

        if let Some(capture) = &self.capture {
            self.wave_phase = (self.wave_phase + 0.44) % std::f32::consts::TAU;
            let (seconds, peak, error) = match capture.progress.lock() {
                Ok(progress) => (
                    progress.samples as f64
                        / f64::from(capture.sample_rate)
                        / f64::from(capture.channels),
                    progress.peak,
                    progress.error.clone(),
                ),
                Err(_) => (
                    self.seconds,
                    0.0,
                    Some("No se pudo leer el estado del micrófono.".into()),
                ),
            };
            self.seconds = seconds;
            self.peak = peak;
            if let Some(error) = error {
                self.capture.take();
                self.peak = 0.0;
                self.dictation_state = DictationState::Failed(error);
            } else if seconds >= f64::from(MAX_SECONDS) {
                self.stop_and_transcribe(cx);
            }
            changed = true;
        } else if self.dictation_state.is_processing() || self.dictation_state.is_attempt_failed() {
            self.wave_phase = (self.wave_phase + 0.34) % std::f32::consts::TAU;
            changed = true;
        } else if let Ok(ws) = self.whisper_state.lock()
            && ws.downloading
        {
            changed = true;
        }

        if changed {
            cx.notify();
        }
    }

    fn toggle_dictation(&mut self, cx: &mut Context<Self>) {
        if self.dictation_state.is_processing() {
            return;
        }
        if self.capture.is_some() {
            self.stop_and_transcribe(cx);
        } else {
            self.text_injector.remember_foreground_target();
            match Capture::start() {
                Ok(capture) => {
                    self.capture = Some(capture);
                    self.seconds = 0.0;
                    self.peak = 0.0;
                    self.dictation_state = DictationState::Listening;
                }
                Err(error) => {
                    self.dictation_state = DictationState::Failed(format!("{error:#}"));
                }
            }
            cx.notify();
        }
    }

    fn stop_and_transcribe(&mut self, cx: &mut Context<Self>) {
        let Some(capture) = self.capture.take() else {
            return;
        };
        self.peak = 0.0;
        match capture.finish() {
            Ok(recording) => {
                self.seconds = recording.seconds;
                let _ = fs::remove_file(&recording.path);
                let has_key = !self.api_key.trim().is_empty();
                let req = TranscriptionRequest {
                    audio: recording.wav_16khz_mono,
                    mime_type: "audio/wav".into(),
                    model: self.prefs.model.clone(),
                    system_prompt: self.prefs.effective_system_prompt(has_key).to_string(),
                    provider: self.prefs.provider,
                };
                self.launch_transcription(req, false);
            }
            Err(error) => {
                self.dictation_state = DictationState::Failed(format!("{error:#}"));
            }
        }
        cx.notify();
    }

    fn launch_transcription(&mut self, request: TranscriptionRequest, force_whisper_only: bool) {
        if let Some(prev) = self.transcription_cancel.take() {
            prev.store(true, Ordering::Relaxed);
        }
        self.last_request = Some(request.clone());
        let cancel_flag = Arc::new(AtomicBool::new(false));
        self.transcription_cancel = Some(cancel_flag.clone());

        let ws_guard = self.whisper_state.lock().ok();
        let whisper_available = ws_guard.as_ref().map(|w| w.available).unwrap_or(false);
        let whisper_model = ws_guard.as_ref().and_then(|w| w.model);
        drop(ws_guard);

        let gemini_key = if self.prefs.provider == AiProvider::Gemini {
            self.api_key.clone()
        } else {
            secrets::load_api_key(AiProvider::Gemini)
        };

        let config = TranscriptionJobConfig {
            request,
            api_key: self.api_key.clone(),
            endpoint: self.prefs.endpoint.clone(),
            provider_enabled: self.prefs.provider_enabled,
            whisper_enabled: self.prefs.whisper_enabled,
            whisper_available,
            whisper_model,
            whisper_language: self.prefs.whisper_language,
            whisper_llm_refinement: self.prefs.whisper_llm_refinement,
            gemini_key,
            auto_inject: self.prefs.auto_inject,
            active_quota: self.quota_exhaustion.clone(),
            force_whisper_only,
        };
        coordinator::spawn_transcription(config, cancel_flag, self.coord_tx.clone());
    }

    fn cancel_dictation(&mut self, cx: &mut Context<Self>) {
        if self.active_dialog != ActiveDialog::None {
            self.active_dialog = ActiveDialog::None;
            cx.notify();
            return;
        }
        if let Some(capture) = self.capture.take() {
            self.seconds = 0.0;
            self.peak = 0.0;
            let _ = capture.cancel();
            self.show_toast("Grabación cancelada");
        }
        if let Some(flag) = self.transcription_cancel.take() {
            flag.store(true, Ordering::Relaxed);
        }
        self.dictation_state = DictationState::Idle;
        cx.notify();
    }

    fn handle_dropped_files(&mut self, paths: &[PathBuf], cx: &mut Context<Self>) {
        let Some(target) = paths.first() else {
            return;
        };
        if !target.is_file() {
            return;
        }
        let ext = target
            .extension()
            .and_then(|e| e.to_str())
            .map(|e| e.to_lowercase())
            .unwrap_or_default();

        let (mime_type, is_image) = match ext.as_str() {
            "png" => ("image/png", true),
            "jpg" | "jpeg" => ("image/jpeg", true),
            "webp" => ("image/webp", true),
            "bmp" => ("image/bmp", true),
            "gif" => ("image/gif", true),
            "mp3" => ("audio/mp3", false),
            "m4a" => ("audio/m4a", false),
            "ogg" => ("audio/ogg", false),
            "flac" => ("audio/flac", false),
            "aac" => ("audio/aac", false),
            "wma" => ("audio/wma", false),
            "wav" => ("audio/wav", false),
            _ => {
                self.show_toast("Formato no compatible. Arrastra un audio o una imagen.");
                cx.notify();
                return;
            }
        };

        let Ok(bytes) = fs::read(target) else {
            self.show_toast("No se pudo leer el archivo arrastrado.");
            cx.notify();
            return;
        };

        let has_key = !self.api_key.trim().is_empty();
        let prompt = if is_image {
            "Extrae y transcribe fielmente todo el texto visible en esta imagen (reconocimiento OCR), o describe con claridad y precisión el contenido si no contiene texto visible. Devuelve únicamente el resultado final limpio, estructurado y sin comentarios adicionales.".to_string()
        } else {
            self.prefs.effective_system_prompt(has_key).to_string()
        };

        let req = TranscriptionRequest {
            audio: bytes,
            mime_type: mime_type.into(),
            model: self.prefs.model.clone(),
            system_prompt: prompt,
            provider: self.prefs.provider,
        };
        self.launch_transcription(req, false);
        cx.notify();
    }

    fn scan_models(&mut self, auto_select_best: bool, key_override: Option<String>) {
        let key = key_override.unwrap_or_else(|| self.api_key.clone());
        if key.trim().is_empty() || self.is_scanning_models {
            return;
        }
        self.is_scanning_models = true;
        let provider = self.prefs.provider;
        let endpoint = self.prefs.endpoint.clone();
        let tx = self.scan_tx.clone();
        std::thread::Builder::new()
            .name("lyraflow-scan".into())
            .spawn(move || {
                let result = scanner::discover_models(provider, &key, &endpoint)
                    .map(|items| items.into_iter().map(|m| m.id).collect());
                let _ = tx.send(ScanOutcome {
                    provider,
                    auto_select_best,
                    result,
                });
            })
            .ok();
    }

    fn update_api_key(&mut self, new_key: String, cx: &mut Context<Self>) {
        let was_empty = self.api_key.trim().is_empty();
        let trimmed = new_key.trim().to_string();
        self.api_key = trimmed.clone();
        self.api_key_field.value = trimmed.clone();
        self.dialog_api_key_field.value = trimmed.clone();

        if let Err(err) = secrets::save_api_key(self.prefs.provider, &trimmed) {
            self.show_toast(err);
        }
        self.quota_exhaustion = None;

        if was_empty && !trimmed.is_empty() && self.prefs.prompt_mode == SystemPromptMode::Literal {
            self.prefs.prompt_mode = SystemPromptMode::Smart;
            self.prefs.system_prompt = preferences::DEFAULT_SYSTEM_PROMPT.to_string();
            self.prompt_field.value = self.prefs.system_prompt.clone();
            let _ = preferences::save_all(&self.prefs);
        }

        if !trimmed.is_empty() {
            self.scan_models(
                was_empty || self.prefs.model.trim().is_empty(),
                Some(trimmed),
            );
        }
        cx.notify();
    }

    fn select_provider(&mut self, provider: AiProvider, cx: &mut Context<Self>) {
        if self.prefs.provider == provider {
            return;
        }
        self.prefs.provider = provider;
        self.prefs.endpoint = provider.default_endpoint().to_string();
        self.endpoint_field.value = self.prefs.endpoint.clone();

        let saved_models = self
            .prefs
            .discovered_models
            .get(&provider)
            .cloned()
            .unwrap_or_default();
        self.prefs.model = saved_models.first().cloned().unwrap_or_else(|| {
            if provider == AiProvider::MiniMax {
                "asr-1.0".into()
            } else {
                String::new()
            }
        });
        self.model_field.value = self.prefs.model.clone();

        self.api_key = secrets::load_api_key(provider);
        self.api_key_field.value = self.api_key.clone();
        self.dialog_api_key_field.value = self.api_key.clone();

        self.save_prefs(cx);

        if !self.api_key.trim().is_empty() && saved_models.is_empty() {
            self.scan_models(self.prefs.model.trim().is_empty(), None);
        }
    }

    fn restart_global_shortcuts(&mut self) {
        self.global_shortcut = None;
        self.global_shortcut = Some(WindowsGlobalShortcut::start(
            self.prefs.shortcut.clone(),
            self.prefs.cancel_shortcut.clone(),
            self.hotkey_tx.clone(),
        ));
    }

    fn clear_result(&mut self, cx: &mut Context<Self>) {
        self.dictation_state = DictationState::Idle;
        self.seconds = 0.0;
        self.peak = 0.0;
        cx.notify();
    }

    fn save_prefs(&mut self, cx: &mut Context<Self>) {
        if !self.history_enabled() && self.destination == Destination::History {
            self.destination = Destination::Home;
        }
        if let Err(error) = preferences::save_all(&self.prefs) {
            self.show_toast(error);
        }
        cx.notify();
    }

    fn set_theme(&mut self, theme: ThemeName, cx: &mut Context<Self>) {
        self.prefs.appearance.theme = theme;
        self.save_prefs(cx);
    }

    fn set_mode(&mut self, mode: ThemeMode, cx: &mut Context<Self>) {
        self.prefs.appearance.mode = mode;
        self.save_prefs(cx);
    }

    fn toggle_mode(&mut self, _: &ToggleMode, _: &mut Window, cx: &mut Context<Self>) {
        let dark = self.prefs.appearance.resolved_dark(self.system_dark);
        let mode = if dark {
            ThemeMode::Light
        } else {
            ThemeMode::Dark
        };
        self.set_mode(mode, cx);
    }

    fn cycle_mode(&mut self, cx: &mut Context<Self>) {
        let mode = match self.prefs.appearance.mode {
            ThemeMode::System => ThemeMode::Light,
            ThemeMode::Light => ThemeMode::Dark,
            ThemeMode::Dark => ThemeMode::System,
        };
        self.set_mode(mode, cx);
    }

    fn open_about(&mut self, _: &OpenAbout, window: &mut Window, cx: &mut Context<Self>) {
        if self.active_dialog == ActiveDialog::About { return; }
        self.about_backdrop = modal_backdrop::capture(window).ok();
        self.active_dialog = ActiveDialog::About;
        cx.notify();
    }

    fn check_updates(&mut self, cx: &mut Context<Self>) {
        self.check_updates_mode(true, cx);
    }

    fn check_updates_mode(&mut self, manual: bool, cx: &mut Context<Self>) {
        if matches!(self.update_state, updates::State::Checking) { return; }
        let (tx, rx) = mpsc::channel();
        self.update_rx = Some(rx);
        self.update_manual = manual;
        self.update_state = updates::State::Checking;
        std::thread::spawn(move || { let _ = tx.send(updates::check(about::VERSION)); });
        cx.notify();
    }

    fn close_about(&mut self, _: &CloseAbout, _: &mut Window, cx: &mut Context<Self>) {
        self.active_dialog = ActiveDialog::None;
        self.about_backdrop = None;
        cx.notify();
    }

    fn inject_last_result(&mut self, cx: &mut Context<Self>) {
        if let DictationState::Completed { refined_text, .. } = &self.dictation_state {
            let text = refined_text.clone();
            cx.write_to_clipboard(ClipboardItem::new_string(text.clone()));
            let injector = std::mem::take(&mut self.text_injector);
            std::thread::spawn(move || {
                injector.inject(&text);
            });
            self.show_toast("Texto copiado e insertado");
            cx.notify();
        }
    }

    fn history_enabled(&self) -> bool {
        self.prefs.history_retention != HistoryRetention::Disabled
    }

    fn visible_destinations(&self) -> Vec<Destination> {
        if self.history_enabled() {
            vec![
                Destination::Home,
                Destination::History,
                Destination::Settings,
            ]
        } else {
            vec![Destination::Home, Destination::Settings]
        }
    }

    fn is_aurora(&self) -> bool {
        self.prefs.appearance.theme == ThemeName::Aurora
    }

    fn platform_subtitle(&self) -> String {
        let hk_status = self
            .global_shortcut
            .as_ref()
            .map(|h| h.status.as_str())
            .unwrap_or("Pendiente");
        format!("Windows · {hk_status}")
    }
}

fn filter_chip(
    id: impl Into<SharedString>,
    label: impl Into<SharedString>,
    selected: bool,
    aurora: bool,
    palette: &Palette,
    listener: impl Fn(&ClickEvent, &mut Window, &mut App) + 'static,
) -> impl IntoElement {
    let (bg, text, border) = if selected {
        if aurora {
            (
                rgb(palette.selection),
                rgb(palette.foreground),
                rgb(palette.accent_foreground),
            )
        } else {
            (
                rgb(palette.secondary_container),
                rgb(palette.on_secondary_container),
                rgb(palette.secondary),
            )
        }
    } else {
        (
            rgba(0x00000000),
            rgb(palette.secondary_foreground),
            rgb(palette.outline),
        )
    };
    div()
        .id(id.into())
        .cursor_pointer()
        .h(px(34.))
        .flex()
        .flex_row()
        .items_center()
        .justify_center()
        .flex_shrink_0()
        .whitespace_nowrap()
        .px_3()
        .rounded_lg()
        .border_1()
        .border_color(border)
        .bg(bg)
        .text_sm()
        .font_weight(if selected {
            FontWeight::SEMIBOLD
        } else {
            FontWeight::MEDIUM
        })
        .text_color(text)
        .hover(|style| style.bg(rgb(palette.surface_hover)))
        .active(|style| style.bg(rgb(palette.surface_pressed)))
        .on_click(listener)
        .child(label.into())
}

fn settings_section(
    title: &'static str,
    supporting: &'static str,
    palette: &Palette,
    children: impl IntoIterator<Item = gpui::AnyElement>,
) -> impl IntoElement {
    div()
        .w_full()
        .min_w_0()
        .rounded_2xl()
        .border_1()
        .border_color(rgb(palette.border_subtle))
        .bg(rgba(palette.surface_muted))
        .p_4()
        .flex()
        .flex_col()
        .gap_3()
        .child(
            div()
                .w_full()
                .min_w_0()
                .flex()
                .flex_col()
                .gap(px(3.))
                .child(
                    div()
                        .text_base()
                        .font_weight(FontWeight::MEDIUM)
                        .text_color(rgb(palette.foreground))
                        .child(title),
                )
                .when(!supporting.is_empty(), |col| {
                    col.child(
                        div()
                            .text_xs()
                            .text_color(rgb(palette.secondary_foreground))
                            .child(supporting),
                    )
                }),
        )
        .children(children)
}

fn switch_row(
    id: &'static str,
    title: &'static str,
    supporting: &'static str,
    checked: bool,
    palette: &Palette,
    listener: impl Fn(&ClickEvent, &mut Window, &mut App) + 'static,
) -> impl IntoElement {
    let track_bg = if checked {
        rgb(palette.accent)
    } else {
        rgba(palette.surface_muted_strong)
    };
    let track_border = if checked {
        rgba(0x00000000)
    } else {
        rgb(palette.outline)
    };
    let knob_bg = if checked {
        rgb(palette.on_accent)
    } else {
        rgb(palette.outline)
    };
    div()
        .id(id)
        .cursor_pointer()
        .w_full()
        .py_1()
        .flex()
        .items_center()
        .justify_between()
        .gap_3()
        .on_click(listener)
        .child(
            div()
                .flex_1()
                .min_w_0()
                .flex()
                .flex_col()
                .gap(px(2.))
                .child(
                    div()
                        .text_sm()
                        .font_weight(FontWeight::MEDIUM)
                        .text_color(rgb(palette.foreground))
                        .child(title),
                )
                .child(
                    div()
                        .text_xs()
                        .text_color(rgb(palette.secondary_foreground))
                        .child(supporting),
                ),
        )
        .child(
            div()
                .w(px(46.))
                .h(px(26.))
                .flex_shrink_0()
                .rounded_full()
                .border_1()
                .border_color(track_border)
                .bg(track_bg)
                .px(px(3.))
                .flex()
                .items_center()
                .when(checked, |track| track.justify_end())
                .when(!checked, |track| track.justify_start())
                .child(
                    div()
                        .size(if checked { px(18.) } else { px(14.) })
                        .rounded_full()
                        .bg(knob_bg),
                ),
        )
}

fn modal_veil_color(aurora: bool, dark: bool) -> u32 {
    if dark {
        if aurora { 0x020908D4 } else { 0x141218D4 }
    } else if aurora {
        0xFAFFFDC7
    } else {
        0xF9F7FCC7
    }
}

impl LyraFlowApp {
    fn render_nav_destination(
        &self,
        dest: Destination,
        aurora: bool,
        palette: &Palette,
        wide: bool,
        cx: &mut Context<Self>,
    ) -> impl IntoElement + use<> {
        let selected = self.destination == dest;
        let (bg, text_color, icon_color) = if selected {
            if aurora {
                (
                    rgb(palette.selection),
                    rgb(palette.foreground),
                    rgb(palette.accent_foreground),
                )
            } else {
                (
                    rgb(palette.secondary_container),
                    rgb(palette.on_secondary_container),
                    rgb(palette.on_secondary_container),
                )
            }
        } else {
            (
                rgba(0x00000000),
                rgb(palette.secondary_foreground),
                rgb(palette.secondary_foreground),
            )
        };
        let focus = match dest {
            Destination::Home => &self.controls.nav_home,
            Destination::History => &self.controls.nav_history,
            Destination::Settings => &self.controls.nav_settings,
        };
        let id = match dest {
            Destination::Home => "nav-home",
            Destination::History => "nav-history",
            Destination::Settings => "nav-settings",
        };
        let mut btn = div()
            .id(id)
            .cursor_pointer()
            .h(px(64.))
            .rounded_2xl()
            .bg(bg)
            .px_1()
            .py(px(9.))
            .flex()
            .flex_col()
            .items_center()
            .justify_center()
            .gap_1()
            .track_focus(focus)
            .tab_stop(true)
            .hover(|style| {
                if !selected {
                    style.bg(rgb(palette.surface_hover))
                } else {
                    style
                }
            })
            .focus(|style| style.border_1().border_color(rgb(palette.focus_ring)))
            .on_click(cx.listener(move |view, _, _, cx| {
                view.destination = dest;
                cx.notify();
            }))
            .child(lyra_icon(dest.icon(), 22.0, icon_color))
            .child(
                div()
                    .text_sm()
                    .font_weight(FontWeight::SEMIBOLD)
                    .text_color(text_color)
                    .child(dest.label()),
            );
        if wide {
            btn = btn.w_full();
        } else {
            btn = btn.flex_1();
        }
        btn
    }

    fn render_navigation_rail(
        &self,
        aurora: bool,
        palette: &Palette,
        cx: &mut Context<Self>,
    ) -> impl IntoElement {
        let destinations = self.visible_destinations();
        let (theme_bg, theme_text, theme_icon) = if aurora {
            (
                rgb(palette.selection),
                rgb(palette.foreground),
                rgb(palette.accent_foreground),
            )
        } else {
            (
                rgb(palette.primary_container),
                rgb(palette.on_primary_container),
                rgb(palette.on_primary_container),
            )
        };

        let rail = div()
            .w(px(98.))
            .h_full()
            .flex_shrink_0()
            .border_r_1()
            .border_color(rgb(palette.border_subtle))
            .bg(rgb(palette.surface))
            .px_2()
            .py(px(18.))
            .flex()
            .flex_col()
            .items_center();

        let mut nav_items = Vec::with_capacity(destinations.len());
        for dest in destinations {
            nav_items.push(self.render_nav_destination(dest, aurora, palette, true, cx));
        }

        rail.child(
            div()
                .id("nav-theme")
                .cursor_pointer()
                .w(px(78.))
                .h(px(64.))
                .rounded_2xl()
                .bg(theme_bg)
                .px_1()
                .py(px(9.))
                .flex()
                .flex_col()
                .items_center()
                .justify_center()
                .gap_1()
                .track_focus(&self.controls.nav_theme)
                .tab_stop(true)
                .focus(|style| style.border_1().border_color(rgb(palette.focus_ring)))
                .on_click(cx.listener(|view, _, _, cx| view.cycle_mode(cx)))
                .child(lyra_icon(
                    mode_icon(self.prefs.appearance.mode),
                    22.0,
                    theme_icon,
                ))
                .child(
                    div()
                        .text_sm()
                        .font_weight(FontWeight::SEMIBOLD)
                        .text_color(theme_text)
                        .child("Tema"),
                ),
        )
        .child(div().flex_1())
        .child(
            div()
                .w(px(78.))
                .flex()
                .flex_col()
                .gap(px(10.))
                .children(nav_items),
        )
        .child(div().flex_1())
        .child(
            div()
                .id("nav-about")
                .cursor_pointer()
                .w(px(78.))
                .h(px(64.))
                .rounded_2xl()
                .bg(rgba(palette.surface_muted_strong))
                .px_1()
                .py(px(9.))
                .flex()
                .flex_col()
                .items_center()
                .justify_center()
                .gap_1()
                .track_focus(&self.controls.nav_about)
                .tab_stop(true)
                .hover(|style| style.bg(rgb(palette.surface_hover)))
                .focus(|style| style.border_1().border_color(rgb(palette.focus_ring)))
                .on_click(
                    cx.listener(|view, _, window, cx| view.open_about(&OpenAbout, window, cx)),
                )
                .child(lyra_icon(
                    IconType::Info,
                    22.0,
                    rgb(palette.secondary_foreground),
                ))
                .child(
                    div()
                        .text_sm()
                        .font_weight(FontWeight::SEMIBOLD)
                        .text_color(rgb(palette.secondary_foreground))
                        .child("Acerca de"),
                ),
        )
    }

    fn render_navigation_bar(
        &self,
        aurora: bool,
        palette: &Palette,
        cx: &mut Context<Self>,
    ) -> impl IntoElement {
        let destinations = self.visible_destinations();
        let bar = div()
            .w_full()
            .h(px(76.))
            .flex_shrink_0()
            .border_t_1()
            .border_color(rgb(palette.border_subtle))
            .bg(rgb(palette.surface))
            .px_4()
            .py_2()
            .flex()
            .flex_row()
            .items_center()
            .gap(px(10.));
        let mut nav_items = Vec::with_capacity(destinations.len());
        for dest in destinations {
            nav_items.push(self.render_nav_destination(dest, aurora, palette, false, cx));
        }

        bar.children(nav_items).child(
            div()
                .id("nav-about-bar")
                .cursor_pointer()
                .flex_1()
                .h(px(60.))
                .rounded_2xl()
                .bg(rgba(palette.surface_muted_strong))
                .px_1()
                .py(px(8.))
                .flex()
                .flex_col()
                .items_center()
                .justify_center()
                .gap_1()
                .hover(|style| style.bg(rgb(palette.surface_hover)))
                .on_click(
                    cx.listener(|view, _, window, cx| view.open_about(&OpenAbout, window, cx)),
                )
                .child(lyra_icon(
                    IconType::Info,
                    22.0,
                    rgb(palette.secondary_foreground),
                ))
                .child(
                    div()
                        .text_sm()
                        .font_weight(FontWeight::SEMIBOLD)
                        .text_color(rgb(palette.secondary_foreground))
                        .child("Acerca de"),
                ),
        )
    }

    fn render_home(
        &self,
        viewport: gpui::Size<gpui::Pixels>,
        aurora: bool,
        palette: &Palette,
        cx: &mut Context<Self>,
    ) -> impl IntoElement {
        let listening = self.dictation_state.is_listening();
        let attempt_failed = self.dictation_state.is_attempt_failed();
        let processing = self.dictation_state.is_processing();
        let wide = viewport.width >= px(720.);
        let available_height = if wide {
            f32::from(viewport.height)
        } else {
            (f32::from(viewport.height) - 76.0).max(320.0)
        };
        let compact = available_height < 680.0;
        let viz_height = ((available_height - 540.0) * 0.22 + 56.0).clamp(48.0, 112.0);
        let button_height = if compact { 48.0 } else { 56.0 };
        let hero_gap = if compact { 12.0 } else { 16.0 };
        let hero_py = if compact { 16.0 } else { 22.0 };
        let outer_pad = if compact { 20.0 } else { 24.0 };
        let outer_gap = if compact { 12.0 } else { 16.0 };

        let subtitle = match &self.dictation_state {
            DictationState::Listening => {
                "Habla con naturalidad. LyraFlow se encarga del resto.".to_string()
            }
            DictationState::Transcribing {
                model,
                attempt,
                max_attempts,
            } => format!("Procesando con {model} (intento {attempt} de {max_attempts})…"),
            DictationState::AttemptFailed {
                attempt,
                max_attempts,
            } => format!("Reintentando ({attempt}/{max_attempts})…"),
            _ => format!(
                "Pulsa {} o arrastra un archivo de audio o imagen aquí.",
                self.prefs.shortcut.label()
            ),
        };

        let (badge_bg, badge_icon) = if aurora {
            (rgb(palette.selection), rgb(palette.accent_foreground))
        } else {
            (
                rgb(palette.primary_container),
                rgb(palette.on_primary_container),
            )
        };

        let bars = 17usize;
        let visualizer = div()
            .w_full()
            .h(px(viz_height))
            .px_4()
            .flex()
            .flex_row()
            .items_center()
            .justify_between()
            .children((0..bars).map(|index| {
                let pulse = ((self.wave_phase + index as f32 * 0.72).sin() + 1.0) / 2.0;
                let energy = if attempt_failed {
                    0.28 + 0.52 * pulse
                } else if listening {
                    0.12 + self.peak.max(0.08) * 0.88 * pulse
                } else if processing {
                    0.24 + 0.68 * pulse
                } else {
                    0.18 + 0.08 * (index as f32).sin().max(0.0)
                };
                let color = if attempt_failed {
                    rgb(palette.error)
                } else if listening || processing {
                    rgb(palette.meter)
                } else {
                    rgb(palette.border_subtle)
                };
                div()
                    .w(px(10.))
                    .h(px((viz_height * energy).max(8.0)))
                    .rounded_full()
                    .bg(color)
            }));

        let ws_guard = self.whisper_state.lock().ok();
        let whisper_available = ws_guard.as_ref().map(|w| w.available).unwrap_or(false);
        let whisper_detail_str = ws_guard
            .as_ref()
            .map(|w| w.detail.clone())
            .unwrap_or_else(|| "Clic para instalar Whisper local".into());
        let whisper_downloading = ws_guard.as_ref().map(|w| w.downloading).unwrap_or(false);
        let whisper_progress = ws_guard.as_ref().and_then(|w| w.progress);
        drop(ws_guard);

        let provider = self.prefs.provider;
        let has_key = !self.api_key.trim().is_empty();
        let provider_title = if self.prefs.provider_enabled {
            provider.label().to_string()
        } else {
            format!("{} (Desactivado)", provider.label())
        };
        let provider_detail = if !self.prefs.provider_enabled {
            "Dictado por nube desactivado".to_string()
        } else if !has_key {
            "Añade tu API key para escanear".to_string()
        } else if self.prefs.model.trim().is_empty() {
            "Pulsa para escanear o elegir modelo".to_string()
        } else {
            self.prefs.model.clone()
        };

        let whisper_title = if self.prefs.whisper_enabled {
            "Whisper local"
        } else {
            "Whisper local (Desactivado)"
        };
        let whisper_detail = if !self.prefs.whisper_enabled {
            "Dictado offline desactivado".to_string()
        } else {
            whisper_detail_str
        };

        let completed = match &self.dictation_state {
            DictationState::Completed { refined_text, .. } => Some(refined_text.clone()),
            _ => None,
        };
        let failed_msg = match &self.dictation_state {
            DictationState::Failed(msg) => Some(msg.clone()),
            _ => None,
        };
        let result_text = if let Some(ref text) = completed {
            if text.trim().is_empty() {
                "No se detectó voz clara.".to_string()
            } else {
                text.clone()
            }
        } else if let Some(ref msg) = failed_msg {
            msg.clone()
        } else {
            "Tu transcripción aparecerá aquí.".to_string()
        };

        let can_inject = completed.as_ref().is_some_and(|t| !t.trim().is_empty());
        let can_clear = self.dictation_state != DictationState::Idle;
        let record_enabled = !processing
            && (listening
                || (self.prefs.provider_enabled && has_key && !self.prefs.model.trim().is_empty())
                || (self.prefs.whisper_enabled && whisper_available));

        let active_quota = self
            .quota_exhaustion
            .as_ref()
            .filter(|q| q.is_active_for(self.prefs.provider, &self.prefs.model))
            .cloned();

        let hero_card = div()
            .w_full()
            .flex_1()
            .min_h(px(260.))
            .rounded_3xl()
            .border_1()
            .border_color(rgb(palette.border_subtle))
            .bg(rgb(palette.surface_card))
            .px_6()
            .py(px(hero_py))
            .flex()
            .flex_col()
            .items_center()
            .justify_center()
            .gap(px(hero_gap));

        let result_card = div()
            .w_full()
            .flex_shrink_0()
            .rounded_3xl()
            .border_1()
            .border_color(rgb(palette.border_subtle))
            .bg(rgba(palette.surface_muted))
            .px_5()
            .py_4()
            .flex()
            .flex_col()
            .gap_2();

        let inject_btn = {
            let (bg, text) = if can_inject {
                (
                    rgb(palette.secondary_container),
                    rgb(palette.on_secondary_container),
                )
            } else {
                (
                    rgb(palette.disabled_surface),
                    rgb(palette.disabled_foreground),
                )
            };
            let mut btn = div()
                .id("inject-result")
                .h(px(32.))
                .px_3()
                .rounded_xl()
                .bg(bg)
                .text_color(text)
                .text_xs()
                .font_weight(FontWeight::SEMIBOLD)
                .flex()
                .flex_row()
                .items_center()
                .gap(px(6.))
                .track_focus(&self.controls.inject_result)
                .tab_stop(true)
                .focus(|style| style.border_1().border_color(rgb(palette.focus_ring)))
                .child(lyra_icon(IconType::Copy, 14.0, text))
                .child("Insertar");
            if can_inject {
                btn = btn
                    .cursor_pointer()
                    .hover(|style| style.bg(rgb(palette.surface_hover)))
                    .on_click(cx.listener(|view, _, _, cx| view.inject_last_result(cx)));
            }
            btn
        };

        let clear_btn = {
            let text = if can_clear {
                rgb(palette.foreground)
            } else {
                rgb(palette.disabled_foreground)
            };
            let mut btn = div()
                .id("clear-result")
                .h(px(32.))
                .px_3()
                .rounded_xl()
                .border_1()
                .border_color(rgb(if can_clear {
                    palette.outline
                } else {
                    palette.border_subtle
                }))
                .text_color(text)
                .text_xs()
                .font_weight(FontWeight::SEMIBOLD)
                .flex()
                .flex_row()
                .items_center()
                .gap(px(6.))
                .track_focus(&self.controls.clear_result)
                .tab_stop(true)
                .focus(|style| style.border_color(rgb(palette.focus_ring)))
                .child(lyra_icon(IconType::Clear, 14.0, text))
                .child("Limpiar");
            if can_clear {
                btn = btn
                    .cursor_pointer()
                    .hover(|style| style.bg(rgb(palette.surface_hover)))
                    .on_click(cx.listener(|view, _, _, cx| view.clear_result(cx)));
            }
            btn
        };

        let mut retry_buttons = Vec::new();
        if failed_msg.is_some() && self.last_request.is_some() {
            retry_buttons.push(
                div()
                    .id("retry-provider")
                    .cursor_pointer()
                    .h(px(32.))
                    .px_3()
                    .rounded_xl()
                    .bg(rgb(palette.secondary_container))
                    .text_color(rgb(palette.on_secondary_container))
                    .text_xs()
                    .font_weight(FontWeight::SEMIBOLD)
                    .flex()
                    .items_center()
                    .hover(|s| s.bg(rgb(palette.surface_hover)))
                    .on_click(cx.listener(|view, _, _, cx| {
                        if let Some(req) = view.last_request.clone() {
                            view.quota_exhaustion = None;
                            view.launch_transcription(req, false);
                            cx.notify();
                        }
                    }))
                    .child(format!("Reintentar {}", provider.label()))
                    .into_any_element(),
            );
            if whisper_available {
                retry_buttons.push(
                    div()
                        .id("retry-whisper")
                        .cursor_pointer()
                        .h(px(32.))
                        .px_3()
                        .rounded_xl()
                        .bg(rgb(palette.secondary_container))
                        .text_color(rgb(palette.on_secondary_container))
                        .text_xs()
                        .font_weight(FontWeight::SEMIBOLD)
                        .flex()
                        .items_center()
                        .hover(|s| s.bg(rgb(palette.surface_hover)))
                        .on_click(cx.listener(|view, _, _, cx| {
                            if let Some(req) = view.last_request.clone() {
                                view.launch_transcription(req, true);
                                cx.notify();
                            }
                        }))
                        .child("Reintentar con Whisper")
                        .into_any_element(),
                );
            }
        }

        let record_btn = {
            let (bg, text) = if record_enabled {
                (rgb(palette.accent), rgb(palette.on_accent))
            } else {
                (
                    rgb(palette.disabled_surface),
                    rgb(palette.disabled_foreground),
                )
            };
            let mut btn = div()
                .id("record")
                .flex_1()
                .h(px(button_height))
                .rounded_2xl()
                .bg(bg)
                .text_color(text)
                .text_sm()
                .font_weight(FontWeight::SEMIBOLD)
                .flex()
                .items_center()
                .justify_center()
                .track_focus(&self.controls.record)
                .tab_stop(true)
                .focus(|style| style.border_1().border_color(rgb(palette.focus_ring)))
                .child(if processing {
                    "Procesando…"
                } else if listening {
                    "Terminar y transcribir"
                } else {
                    "Comenzar dictado"
                });
            if record_enabled {
                btn = btn
                    .cursor_pointer()
                    .hover(|style| style.bg(rgb(palette.accent_hover)))
                    .active(|style| style.bg(rgb(palette.accent_pressed)))
                    .on_click(cx.listener(|view, _, _, cx| view.toggle_dictation(cx)));
            }
            btn
        };

        div()
            .id("home-scroll")
            .size_full()
            .overflow_y_scroll()
            .on_drop::<ExternalPaths>(cx.listener(|view, paths: &ExternalPaths, _, cx| {
                view.handle_dropped_files(paths.paths(), cx);
            }))
            .child(
                div()
                    .w_full()
                    .min_h(px(available_height))
                    .p(px(outer_pad))
                    .flex()
                    .flex_col()
                    .gap(px(outer_gap))
                    .child(
                        div()
                            .flex_shrink_0()
                            .flex()
                            .flex_col()
                            .gap_1()
                            .child(
                                div()
                                    .text_3xl()
                                    .font_weight(FontWeight::SEMIBOLD)
                                    .text_color(rgb(palette.foreground))
                                    .child("Tu voz, ya bien escrita"),
                            )
                            .child(
                                div()
                                    .text_sm()
                                    .text_color(rgb(palette.secondary_foreground))
                                    .child(self.platform_subtitle()),
                            ),
                    )
                    .when_some(active_quota, |col, quota| {
                        let fallback_active = self.prefs.whisper_enabled && whisper_available;
                        col.child(
                            div()
                                .w_full()
                                .flex_shrink_0()
                                .rounded_2xl()
                                .bg(rgb(palette.error_container))
                                .text_color(rgb(palette.on_error_container))
                                .p_4()
                                .flex()
                                .flex_row()
                                .items_center()
                                .gap_3()
                                .child(lyra_icon(
                                    IconType::Settings,
                                    22.0,
                                    rgb(palette.on_error_container),
                                ))
                                .child(
                                    div()
                                        .flex_1()
                                        .min_w_0()
                                        .flex()
                                        .flex_col()
                                        .gap(px(2.))
                                        .child(
                                            div()
                                                .text_sm()
                                                .font_weight(FontWeight::SEMIBOLD)
                                                .child(format!("⚠️ Cuota de {} agotada", quota.source())),
                                        )
                                        .child(
                                            div()
                                                .text_xs()
                                                .child(if fallback_active {
                                                    "Transcripción autónoma con Whisper local activa. Las siguientes peticiones se procesarán localmente."
                                                } else {
                                                    "Actualiza tu API Key o elige otro proveedor o modelo para reanudar la transcripción en la nube."
                                                }),
                                        ),
                                )
                                .child(
                                    div()
                                        .id("quota-update-key")
                                        .cursor_pointer()
                                        .flex_shrink_0()
                                        .px_3()
                                        .py_2()
                                        .rounded_xl()
                                        .text_xs()
                                        .font_weight(FontWeight::SEMIBOLD)
                                        .hover(|s| s.bg(rgb(palette.surface_hover)))
                                        .on_click(cx.listener(|view, _, _, cx| {
                                            view.dialog_api_key_field.value = view.api_key.clone();
                                            view.active_dialog = ActiveDialog::ApiKey;
                                            cx.notify();
                                        }))
                                        .child("Actualizar Key"),
                                ),
                        )
                    })
                    .child(
                        hero_card
                            .child(
                                div()
                                    .rounded_2xl()
                                    .bg(badge_bg)
                                    .p(px(if compact { 12.0 } else { 14.0 }))
                                    .child(lyra_icon(
                                        IconType::Mic,
                                        if compact { 30.0 } else { 36.0 },
                                        badge_icon,
                                    )),
                            )
                            .child(
                                div()
                                    .text_2xl()
                                    .font_weight(FontWeight::SEMIBOLD)
                                    .text_color(rgb(palette.foreground))
                                    .child(status_title(&self.dictation_state)),
                            )
                            .child(
                                div()
                                    .text_base()
                                    .text_color(rgb(palette.secondary_foreground))
                                    .child(subtitle),
                            )
                            .when(self.seconds > 0.0 || listening, |col| {
                                col.child(
                                    div()
                                        .text_sm()
                                        .font_weight(FontWeight::SEMIBOLD)
                                        .text_color(rgb(palette.accent_foreground))
                                        .child(format!(
                                            "{} · {}",
                                            if listening {
                                                "Grabando"
                                            } else {
                                                "Audio capturado"
                                            },
                                            format_duration(self.seconds)
                                        )),
                                )
                            })
                            .child(visualizer)
                            .child(
                                div()
                                    .w_full()
                                    .flex()
                                    .flex_row()
                                    .gap_3()
                                    .child(record_btn)
                                    .when(listening || processing, |row| {
                                        row.child(
                                            div()
                                                .id("cancel")
                                                .cursor_pointer()
                                                .px_5()
                                                .h(px(button_height))
                                                .rounded_2xl()
                                                .border_1()
                                                .border_color(rgb(palette.control_border))
                                                .bg(rgb(palette.surface))
                                                .text_color(rgb(palette.foreground))
                                                .text_sm()
                                                .font_weight(FontWeight::SEMIBOLD)
                                                .flex()
                                                .items_center()
                                                .justify_center()
                                                .track_focus(&self.controls.cancel)
                                                .tab_stop(true)
                                                .hover(|style| style.bg(rgb(palette.surface_hover)))
                                                .active(|style| {
                                                    style.bg(rgb(palette.surface_pressed))
                                                })
                                                .focus(|style| {
                                                    style.border_color(rgb(palette.focus_ring))
                                                })
                                                .on_click(cx.listener(|view, _, _, cx| {
                                                    view.cancel_dictation(cx)
                                                }))
                                                .child("Cancelar"),
                                        )
                                    }),
                            ),
                    )
                    .child(
                        result_card
                            .child(
                                div()
                                    .w_full()
                                    .flex()
                                    .flex_row()
                                    .items_center()
                                    .justify_between()
                                    .gap_3()
                                    .child(
                                        div()
                                            .text_xl()
                                            .font_weight(FontWeight::SEMIBOLD)
                                            .text_color(rgb(palette.foreground))
                                            .child("Último resultado"),
                                    )
                                    .child(
                                        div()
                                            .flex()
                                            .flex_row()
                                            .items_center()
                                            .flex_wrap()
                                            .gap_2()
                                            .children(retry_buttons)
                                            .child(inject_btn)
                                            .child(clear_btn),
                                    ),
                            )
                            .child(
                                div()
                                    .text_sm()
                                    .text_color(rgb(if failed_msg.is_some() {
                                        palette.error
                                    } else {
                                        palette.foreground
                                    }))
                                    .child(result_text),
                            ),
                    )
                    .child(
                        div()
                            .w_full()
                            .flex_shrink_0()
                            .flex()
                            .flex_row()
                            .gap_3()
                            .child(self.render_status_card(
                                "status-provider",
                                provider_title,
                                provider_detail,
                                self.prefs.provider_enabled && has_key && !self.prefs.model.trim().is_empty(),
                                None,
                                palette,
                                cx.listener(|view, _, _, cx| {
                                    if view.api_key.trim().is_empty() {
                                        view.dialog_api_key_field.value = view.api_key.clone();
                                        view.active_dialog = ActiveDialog::ApiKey;
                                    } else {
                                        view.active_dialog = ActiveDialog::ModelSelector;
                                    }
                                    cx.notify();
                                }),
                            ))
                            .child(self.render_status_card(
                                "status-whisper",
                                whisper_title.into(),
                                whisper_detail,
                                self.prefs.whisper_enabled && whisper_available,
                                whisper_progress,
                                palette,
                                cx.listener(move |view, _, _, cx| {
                                    if !whisper_downloading {
                                        view.active_dialog = ActiveDialog::WhisperSelector;
                                        cx.notify();
                                    }
                                }),
                            )),
                    ),
            )
    }

    #[allow(clippy::too_many_arguments)]
    fn render_status_card(
        &self,
        id: &'static str,
        title: String,
        detail: String,
        available: bool,
        progress: Option<f32>,
        palette: &Palette,
        on_click: impl Fn(&ClickEvent, &mut Window, &mut App) + 'static,
    ) -> impl IntoElement {
        div()
            .id(id)
            .cursor_pointer()
            .flex_1()
            .min_w_0()
            .rounded_2xl()
            .border_1()
            .border_color(rgb(palette.border_subtle))
            .bg(rgba(palette.surface_muted_strong))
            .p_4()
            .flex()
            .flex_row()
            .items_center()
            .gap_3()
            .hover(|style| style.bg(rgb(palette.surface_hover)))
            .on_click(on_click)
            .child(
                div()
                    .size(px(10.))
                    .rounded_full()
                    .flex_shrink_0()
                    .bg(rgb(if available {
                        palette.secondary
                    } else {
                        palette.tertiary
                    })),
            )
            .child(
                div()
                    .flex_1()
                    .min_w_0()
                    .flex()
                    .flex_col()
                    .gap(px(2.))
                    .child(
                        div()
                            .text_base()
                            .font_weight(FontWeight::MEDIUM)
                            .text_color(rgb(palette.foreground))
                            .child(title),
                    )
                    .child(
                        div()
                            .text_sm()
                            .text_color(rgb(palette.secondary_foreground))
                            .child(detail),
                    )
                    .when_some(progress, |col, pct| {
                        let clamped = pct.clamp(0.0, 1.0);
                        col.child(
                            div()
                                .w_full()
                                .h(px(4.))
                                .mt_1()
                                .rounded_full()
                                .bg(rgb(palette.border_subtle))
                                .child(
                                    div()
                                        .h_full()
                                        .w(gpui::relative(clamped))
                                        .rounded_full()
                                        .bg(rgb(palette.secondary)),
                                ),
                        )
                    }),
            )
    }

    fn render_history(
        &self,
        aurora: bool,
        palette: &Palette,
        cx: &mut Context<Self>,
    ) -> impl IntoElement {
        let (badge_bg, badge_text) = if aurora {
            (rgb(palette.selection), rgb(palette.foreground))
        } else {
            (
                rgb(palette.primary_container),
                rgb(palette.on_primary_container),
            )
        };

        let header = div()
            .w_full()
            .flex_shrink_0()
            .flex()
            .flex_row()
            .items_center()
            .justify_between()
            .child(
                div()
                    .flex()
                    .flex_col()
                    .gap(px(2.))
                    .child(
                        div()
                            .text_xl()
                            .font_weight(FontWeight::BOLD)
                            .text_color(rgb(palette.foreground))
                            .child("Historial de Transcripciones"),
                    )
                    .child(
                        div()
                            .text_xs()
                            .text_color(rgb(palette.secondary_foreground))
                            .child("Recuperación rápida y copiado en 1-clic"),
                    ),
            )
            .when(!self.history_entries.is_empty(), |row| {
                row.child(
                    div()
                        .id("history-clear-all")
                        .cursor_pointer()
                        .px_3()
                        .py_2()
                        .rounded_xl()
                        .border_1()
                        .border_color(rgb(palette.outline))
                        .text_sm()
                        .font_weight(FontWeight::SEMIBOLD)
                        .text_color(rgb(palette.foreground))
                        .flex()
                        .flex_row()
                        .items_center()
                        .gap_2()
                        .hover(|s| s.bg(rgb(palette.surface_hover)))
                        .on_click(cx.listener(|view, _, _, cx| {
                            if let Ok(saved) =
                                history::save_history(&[], view.prefs.history_retention.hours())
                            {
                                view.history_entries = saved;
                            }
                            cx.notify();
                        }))
                        .child(lyra_icon(IconType::Clear, 16.0, rgb(palette.foreground)))
                        .child("Limpiar todo"),
                )
            });

        let body = if self.history_entries.is_empty() {
            div()
                .flex_1()
                .w_full()
                .flex()
                .flex_col()
                .items_center()
                .justify_center()
                .gap_3()
                .child(lyra_icon(
                    IconType::History,
                    48.0,
                    rgb(palette.secondary_foreground),
                ))
                .child(
                    div()
                        .text_base()
                        .text_color(rgb(palette.secondary_foreground))
                        .child("No hay transcripciones recientes"),
                )
                .child(
                    div()
                        .text_xs()
                        .text_color(rgb(palette.outline))
                        .child("Tus dictados procesados aparecerán aquí automáticamente."),
                )
                .into_any_element()
        } else {
            let cards: Vec<_> = self
                .history_entries
                .iter()
                .enumerate()
                .map(|(idx, entry)| {
                    let entry_id = entry.id.clone();
                    let text_to_copy = entry.display_text().to_string();
                    let provider_label = if entry.provider_name.trim().is_empty() {
                        "Transcripción".to_string()
                    } else {
                        entry.provider_name.clone()
                    };
                    div()
                        .w_full()
                        .rounded_2xl()
                        .border_1()
                        .border_color(rgb(palette.border_subtle))
                        .bg(rgba(palette.surface_muted))
                        .p_4()
                        .flex()
                        .flex_col()
                        .gap_2()
                        .child(
                            div()
                                .w_full()
                                .flex()
                                .flex_row()
                                .items_center()
                                .justify_between()
                                .child(
                                    div()
                                        .rounded_lg()
                                        .bg(badge_bg)
                                        .px_2()
                                        .py_1()
                                        .text_xs()
                                        .font_weight(FontWeight::SEMIBOLD)
                                        .text_color(badge_text)
                                        .child(provider_label),
                                )
                                .child(
                                    div()
                                        .id(SharedString::from(format!("history-del-{idx}")))
                                        .cursor_pointer()
                                        .p_2()
                                        .rounded_lg()
                                        .hover(|s| s.bg(rgb(palette.surface_hover)))
                                        .on_click(cx.listener(move |view, _, _, cx| {
                                            let remaining: Vec<_> = view
                                                .history_entries
                                                .iter()
                                                .filter(|e| e.id != entry_id)
                                                .cloned()
                                                .collect();
                                            if let Ok(saved) = history::save_history(
                                                &remaining,
                                                view.prefs.history_retention.hours(),
                                            ) {
                                                view.history_entries = saved;
                                            }
                                            cx.notify();
                                        }))
                                        .child(lyra_icon(
                                            IconType::Clear,
                                            16.0,
                                            rgb(palette.secondary_foreground),
                                        )),
                                ),
                        )
                        .child(
                            div()
                                .text_sm()
                                .text_color(rgb(palette.foreground))
                                .child(entry.display_text().to_string()),
                        )
                        .child(
                            div().w_full().flex().justify_end().child(
                                div()
                                    .id(SharedString::from(format!("history-copy-{idx}")))
                                    .cursor_pointer()
                                    .px_4()
                                    .py_2()
                                    .rounded_xl()
                                    .bg(rgb(palette.accent))
                                    .text_color(rgb(palette.on_accent))
                                    .text_xs()
                                    .font_weight(FontWeight::SEMIBOLD)
                                    .flex()
                                    .flex_row()
                                    .items_center()
                                    .gap_2()
                                    .hover(|s| s.bg(rgb(palette.accent_hover)))
                                    .on_click(cx.listener(move |view, _, _, cx| {
                                        cx.write_to_clipboard(ClipboardItem::new_string(
                                            text_to_copy.clone(),
                                        ));
                                        view.show_toast("Texto copiado al portapapeles");
                                        cx.notify();
                                    }))
                                    .child(lyra_icon(IconType::Copy, 16.0, rgb(palette.on_accent)))
                                    .child("Copiar al portapapeles"),
                            ),
                        )
                })
                .collect();

            div()
                .id("history-list")
                .flex_1()
                .w_full()
                .overflow_y_scroll()
                .flex()
                .flex_col()
                .gap_3()
                .children(cards)
                .into_any_element()
        };

        div()
            .size_full()
            .p_6()
            .flex()
            .flex_col()
            .gap_4()
            .child(header)
            .child(body)
    }

    fn render_settings(
        &self,
        viewport: gpui::Size<gpui::Pixels>,
        aurora: bool,
        palette: &Palette,
        window: &Window,
        cx: &mut Context<Self>,
    ) -> impl IntoElement {
        let content_width = if viewport.width >= px(720.) {
            f32::from(viewport.width) - 98.0
        } else {
            f32::from(viewport.width)
        };
        let two_columns = content_width >= 720.0;

        let tab_button = |id: &'static str, tab: SettingsTab, label: &'static str| {
            let selected = self.settings_tab == tab;
            div()
                .id(id)
                .cursor_pointer()
                .flex_1()
                .py_3()
                .flex()
                .flex_col()
                .items_center()
                .justify_center()
                .border_b_2()
                .border_color(if selected {
                    rgb(palette.accent_foreground)
                } else {
                    rgba(0x00000000)
                })
                .text_sm()
                .font_weight(FontWeight::SEMIBOLD)
                .text_color(rgb(if selected {
                    palette.accent_foreground
                } else {
                    palette.secondary_foreground
                }))
                .hover(|style| style.bg(rgb(palette.surface_hover)))
                .on_click(cx.listener(move |view, _, _, cx| {
                    view.settings_tab = tab;
                    cx.notify();
                }))
                .child(label)
        };

        let header = div()
            .w_full()
            .px_6()
            .pt_6()
            .flex()
            .flex_col()
            .gap_3()
            .child(
                div()
                    .text_2xl()
                    .font_weight(FontWeight::SEMIBOLD)
                    .text_color(rgb(palette.foreground))
                    .child("Ajustes"),
            )
            .child(
                div()
                    .w(px(260.))
                    .border_b_1()
                    .border_color(rgb(palette.border_subtle))
                    .flex()
                    .flex_row()
                    .child(tab_button("tab-general", SettingsTab::General, "General"))
                    .child(tab_button("tab-models", SettingsTab::Models, "Modelos")),
            );

        let body = if self.settings_tab == SettingsTab::General {
            self.render_settings_general(two_columns, aurora, palette, cx)
                .into_any_element()
        } else {
            self.render_settings_models(two_columns, aurora, palette, window, cx)
                .into_any_element()
        };

        div()
            .size_full()
            .min_w_0()
            .min_h_0()
            .flex()
            .flex_col()
            .child(header)
            .child(
                div()
                    .id("settings-scroll")
                    .flex_1()
                    .min_h_0()
                    .min_w_0()
                    .w_full()
                    .overflow_y_scroll()
                    .px_6()
                    .py_5()
                    .child(body),
            )
    }

    fn render_shortcut_recorder(
        &self,
        id: &'static str,
        target: ShortcutCaptureTarget,
        title: &'static str,
        current_label: String,
        focus: &FocusHandle,
        palette: &Palette,
        cx: &mut Context<Self>,
    ) -> impl IntoElement {
        let capturing = self.shortcut_capture == target;
        let feedback = self
            .shortcut_feedback
            .as_ref()
            .filter(|(t, _)| *t == target)
            .map(|(_, msg)| msg.clone());

        div()
            .flex_1()
            .min_w_0()
            .flex()
            .flex_col()
            .gap_2()
            .child(
                div()
                    .text_sm()
                    .font_weight(FontWeight::MEDIUM)
                    .text_color(rgb(palette.foreground))
                    .child(title),
            )
            .child(
                div()
                    .w_full()
                    .py_1()
                    .flex()
                    .flex_row()
                    .items_center()
                    .justify_between()
                    .gap_2()
                    .child(
                        div()
                            .text_sm()
                            .font_weight(FontWeight::SEMIBOLD)
                            .text_color(rgb(palette.foreground))
                            .child(current_label),
                    )
                    .child(
                        div()
                            .id(id)
                            .cursor_pointer()
                            .px_3()
                            .py(px(6.))
                            .rounded_xl()
                            .border_1()
                            .border_color(rgb(if capturing {
                                palette.focus_ring
                            } else {
                                palette.outline
                            }))
                            .bg(if capturing {
                                rgb(palette.selection)
                            } else {
                                rgba(0x00000000)
                            })
                            .text_xs()
                            .font_weight(FontWeight::SEMIBOLD)
                            .text_color(rgb(palette.foreground))
                            .track_focus(focus)
                            .tab_stop(true)
                            .hover(|s| s.bg(rgb(palette.surface_hover)))
                            .on_click(cx.listener({
                                let focus = focus.clone();
                                move |view, _, window, cx| {
                                    view.shortcut_capture = target;
                                    view.shortcut_feedback = None;
                                    focus.focus(window);
                                    cx.notify();
                                }
                            }))
                            .on_key_down(cx.listener(move |view, event, _, cx| {
                                if view.shortcut_capture != target {
                                    return;
                                }
                                if let Some(candidate) = key_down_to_shortcut(event) {
                                    let conflict = match target {
                                        ShortcutCaptureTarget::Primary => {
                                            view.prefs.cancel_shortcut.conflicts_with(&candidate)
                                        }
                                        ShortcutCaptureTarget::Cancel => {
                                            view.prefs.shortcut.conflicts_with(&candidate)
                                        }
                                        ShortcutCaptureTarget::None => false,
                                    };
                                    let err = candidate
                                        .validation_error()
                                        .map(|s| s.to_string())
                                        .or_else(|| {
                                            if conflict {
                                                Some(
                                                    "Esa combinación ya la usa el otro atajo."
                                                        .into(),
                                                )
                                            } else {
                                                None
                                            }
                                        });
                                    if let Some(err_msg) = err {
                                        view.shortcut_feedback = Some((target, err_msg));
                                    } else {
                                        match target {
                                            ShortcutCaptureTarget::Primary => {
                                                view.prefs.shortcut = candidate;
                                            }
                                            ShortcutCaptureTarget::Cancel => {
                                                view.prefs.cancel_shortcut = candidate;
                                            }
                                            ShortcutCaptureTarget::None => {}
                                        }
                                        view.shortcut_capture = ShortcutCaptureTarget::None;
                                        view.shortcut_feedback =
                                            Some((target, "Atajo actualizado.".into()));
                                        view.restart_global_shortcuts();
                                        view.save_prefs(cx);
                                    }
                                } else {
                                    view.shortcut_feedback = Some((
                                        target,
                                        "Mantén 1 o 2 modificadores y pulsa una tecla principal."
                                            .into(),
                                    ));
                                }
                                cx.stop_propagation();
                                cx.notify();
                            }))
                            .child(if capturing {
                                "Pulsa las teclas…"
                            } else {
                                "Cambiar"
                            }),
                    ),
            )
            .when_some(feedback, |col, msg| {
                let ok = msg.contains("actualizado");
                col.child(
                    div()
                        .text_xs()
                        .text_color(rgb(if ok {
                            palette.secondary
                        } else {
                            palette.secondary_foreground
                        }))
                        .child(msg),
                )
            })
    }

    fn render_settings_general(
        &self,
        two_columns: bool,
        aurora: bool,
        palette: &Palette,
        cx: &mut Context<Self>,
    ) -> impl IntoElement {
        let appearance_section = settings_section(
            "Apariencia",
            "",
            palette,
            [
                div()
                    .text_xs()
                    .font_weight(FontWeight::SEMIBOLD)
                    .text_color(rgb(palette.secondary_foreground))
                    .child("Tema de la aplicación")
                    .into_any_element(),
                div()
                    .w_full()
                    .min_w_0()
                    .flex_shrink_0()
                    .flex()
                    .flex_row()
                    .flex_wrap()
                    .items_start()
                    .gap_2()
                    .child(filter_chip(
                        "theme-aurora",
                        ThemeName::Aurora.label(),
                        self.prefs.appearance.theme == ThemeName::Aurora,
                        aurora,
                        palette,
                        cx.listener(|view, _, _, cx| view.set_theme(ThemeName::Aurora, cx)),
                    ))
                    .child(filter_chip(
                        "theme-classic",
                        ThemeName::Classic.label(),
                        self.prefs.appearance.theme == ThemeName::Classic,
                        aurora,
                        palette,
                        cx.listener(|view, _, _, cx| view.set_theme(ThemeName::Classic, cx)),
                    ))
                    .into_any_element(),
                div()
                    .text_xs()
                    .font_weight(FontWeight::SEMIBOLD)
                    .text_color(rgb(palette.secondary_foreground))
                    .child("Modo de color")
                    .into_any_element(),
                div()
                    .w_full()
                    .min_w_0()
                    .flex_shrink_0()
                    .flex()
                    .flex_row()
                    .flex_wrap()
                    .items_start()
                    .gap_2()
                    .children(
                        [ThemeMode::System, ThemeMode::Light, ThemeMode::Dark].map(|mode| {
                            filter_chip(
                                format!("mode-{}", mode.id()),
                                mode.label(),
                                self.prefs.appearance.mode == mode,
                                aurora,
                                palette,
                                cx.listener(move |view, _, _, cx| view.set_mode(mode, cx)),
                            )
                        }),
                    )
                    .into_any_element(),
            ],
        );

        let history_section = settings_section(
            "Historial",
            "Se conserva al cerrar la app. Desactivarlo borra los dictados guardados.",
            palette,
            [div()
                .w_full()
                .min_w_0()
                .flex_shrink_0()
                .flex()
                .flex_row()
                .flex_wrap()
                .items_start()
                .gap_2()
                .children(
                    [
                        HistoryRetention::Disabled,
                        HistoryRetention::Hours24,
                        HistoryRetention::Hours48,
                        HistoryRetention::Hours72,
                    ]
                    .map(|period| {
                        filter_chip(
                            format!("history-{}", period.id()),
                            period.label(),
                            self.prefs.history_retention == period,
                            aurora,
                            palette,
                            cx.listener(move |view, _, _, cx| {
                                view.prefs.history_retention = period;
                                if let Ok(saved) =
                                    history::save_history(&view.history_entries, period.hours())
                                {
                                    view.history_entries = saved;
                                }
                                view.save_prefs(cx);
                            }),
                        )
                    }),
                )
                .into_any_element()],
        );

        let shortcuts_container = if two_columns {
            div().w_full().flex().flex_row().gap_5()
        } else {
            div().w_full().flex().flex_col().gap_3()
        }
        .child(self.render_shortcut_recorder(
            "shortcut-primary-btn",
            ShortcutCaptureTarget::Primary,
            "Iniciar y detener",
            self.prefs.shortcut.label(),
            &self.controls.shortcut_primary,
            palette,
            cx,
        ))
        .child(self.render_shortcut_recorder(
            "shortcut-cancel-btn",
            ShortcutCaptureTarget::Cancel,
            "Cancelar dictado",
            self.prefs.cancel_shortcut.label(),
            &self.controls.shortcut_cancel,
            palette,
            cx,
        ));

        let behavior_container = if two_columns {
            div().w_full().flex().flex_row().gap_5()
        } else {
            div().w_full().flex().flex_col().gap_3()
        }
        .child(div().flex_1().min_w_0().child(switch_row(
            "switch-auto-inject",
            "Insertar automáticamente",
            "Pega el resultado en la aplicación activa.",
            self.prefs.auto_inject,
            palette,
            cx.listener(|view, _, _, cx| {
                view.prefs.auto_inject = !view.prefs.auto_inject;
                view.save_prefs(cx);
            }),
        )))
        .child(div().flex_1().min_w_0().child(switch_row(
            "switch-autostart",
            "Iniciar con Windows",
            "Se abre en el área de notificación.",
            self.prefs.launch_at_startup,
            palette,
            cx.listener(|view, _, _, cx| {
                view.prefs.launch_at_startup = !view.prefs.launch_at_startup;
                platform_win::set_auto_start_enabled(view.prefs.launch_at_startup);
                view.save_prefs(cx);
            }),
        )));

        let bottom_row = if two_columns {
            div()
                .w_full()
                .flex()
                .flex_row()
                .items_start()
                .gap_5()
                .child(div().flex_1().min_w_0().child(appearance_section))
                .child(div().flex_1().min_w_0().child(history_section))
        } else {
            div()
                .w_full()
                .flex()
                .flex_col()
                .gap_4()
                .child(appearance_section)
                .child(history_section)
        };

        div()
            .w_full()
            .flex()
            .flex_col()
            .gap_4()
            .child(settings_section(
                "Control del dictado",
                "",
                palette,
                [
                    shortcuts_container.into_any_element(),
                    div()
                        .text_xs()
                        .text_color(rgb(palette.secondary_foreground))
                        .child("Combinaciones de 2 o 3 teclas. F12 y Windows + Espacio están reservados.")
                        .into_any_element(),
                ],
            ))
            .child(settings_section(
                "Comportamiento",
                "",
                palette,
                [behavior_container.into_any_element()],
            ))
            .child(bottom_row)
    }

    fn render_settings_models(
        &self,
        two_columns: bool,
        aurora: bool,
        palette: &Palette,
        window: &Window,
        cx: &mut Context<Self>,
    ) -> impl IntoElement {
        let provider = self.prefs.provider;
        let has_key = !self.api_key.trim().is_empty();
        let available_models = self.prefs.available_models(provider);
        let model_dirty = !self.model_field.value.trim().is_empty()
            && self.model_field.value.trim() != self.prefs.model.trim();

        let connection_section = settings_section(
            "Conexión",
            "Proveedor y credenciales de acceso.",
            palette,
            [
                div()
                    .w_full()
                    .min_w_0()
                    .flex_shrink_0()
                    .flex()
                    .flex_row()
                    .flex_wrap()
                    .items_start()
                    .gap_2()
                    .children(
                        [
                            AiProvider::Gemini,
                            AiProvider::OpenAiCompatible,
                            AiProvider::MiniMax,
                        ]
                        .map(|option| {
                            filter_chip(
                                format!("provider-{}", option.id()),
                                option.label(),
                                provider == option,
                                aurora,
                                palette,
                                cx.listener(move |view, _, _, cx| {
                                    view.select_provider(option, cx);
                                }),
                            )
                        }),
                    )
                    .into_any_element(),
                render_text_field(
                    "settings-api-key",
                    &format!("Clave API de {}", provider.label()),
                    "Pega aquí tu clave de API",
                    &self.api_key_field,
                    true,
                    true,
                    false,
                    Some("Pegar clave API desde el portapapeles"),
                    palette,
                    window,
                    cx.listener(|view, event, _, cx| {
                        if view.api_key_field.handle_key_down(event, false, cx) {
                            let val = view.api_key_field.value.clone();
                            view.update_api_key(val, cx);
                        }
                        cx.stop_propagation();
                    }),
                    Some(Box::new(cx.listener(|view, _, _, cx| {
                        view.api_key_field.revealed = !view.api_key_field.revealed;
                        cx.notify();
                    }))),
                    Some(Box::new(cx.listener(|view, _, _, cx| {
                        if let Some(item) = cx.read_from_clipboard()
                            && let Some(text) = item.text()
                        {
                            let clean = text.replace(['\r', '\n'], "");
                            view.update_api_key(clean, cx);
                        }
                    }))),
                )
                .into_any_element(),
                div()
                    .text_xs()
                    .text_color(rgb(palette.secondary_foreground))
                    .child("La clave se guarda cifrada con DPAPI para tu usuario de Windows.")
                    .into_any_element(),
                if provider == AiProvider::OpenAiCompatible || provider == AiProvider::MiniMax {
                    div()
                        .w_full()
                        .flex()
                        .flex_col()
                        .gap_1()
                        .child(render_text_field(
                            "settings-endpoint",
                            "URL completa del endpoint",
                            provider.default_endpoint(),
                            &self.endpoint_field,
                            false,
                            has_key,
                            false,
                            Some("Pegar URL del endpoint desde el portapapeles"),
                            palette,
                            window,
                            cx.listener(|view, event, _, cx| {
                                if view.endpoint_field.handle_key_down(event, false, cx) {
                                    view.prefs.endpoint = view.endpoint_field.value.clone();
                                    view.save_prefs(cx);
                                }
                                cx.stop_propagation();
                            }),
                            None,
                            Some(Box::new(cx.listener(|view, _, _, cx| {
                                if let Some(item) = cx.read_from_clipboard()
                                    && let Some(text) = item.text()
                                {
                                    let clean = text.replace(['\r', '\n'], "");
                                    view.endpoint_field.value = clean.clone();
                                    view.prefs.endpoint = clean;
                                    view.save_prefs(cx);
                                }
                            }))),
                        ))
                        .child(
                            div()
                                .text_xs()
                                .text_color(rgb(palette.secondary_foreground))
                                .child(if provider == AiProvider::MiniMax {
                                    "URL base del servicio de MiniMax."
                                } else {
                                    "El endpoint y el modelo deben admitir audio mediante Chat Completions."
                                }),
                        )
                        .into_any_element()
                } else {
                    div().into_any_element()
                },
            ],
        );

        let model_section = settings_section(
            "Modelo",
            "Escanea o selecciona el modelo del proveedor conectado.",
            palette,
            [
                render_text_field(
                    "settings-model",
                    "Identificador del modelo",
                    "Escanea o pega el ID del modelo",
                    &self.model_field,
                    false,
                    has_key,
                    false,
                    Some("Pegar identificador del modelo desde el portapapeles"),
                    palette,
                    window,
                    cx.listener(|view, event, _, cx| {
                        if view.model_field.handle_key_down(event, false, cx) {
                            cx.notify();
                        }
                        cx.stop_propagation();
                    }),
                    None,
                    Some(Box::new(cx.listener(|view, _, _, cx| {
                        if let Some(item) = cx.read_from_clipboard()
                            && let Some(text) = item.text()
                        {
                            view.model_field.value = text.replace(['\r', '\n'], "");
                            cx.notify();
                        }
                    }))),
                )
                .into_any_element(),
                div()
                    .w_full()
                    .min_w_0()
                    .flex_shrink_0()
                    .flex()
                    .flex_row()
                    .flex_wrap()
                    .items_center()
                    .gap_2()
                    .child({
                        let enabled = has_key && model_dirty;
                        let mut btn = div()
                            .id("save-model-btn")
                            .px_4()
                            .py_2()
                            .rounded_full()
                            .bg(rgb(if enabled {
                                palette.accent
                            } else {
                                palette.disabled_surface
                            }))
                            .text_color(rgb(if enabled {
                                palette.on_accent
                            } else {
                                palette.disabled_foreground
                            }))
                            .text_sm()
                            .font_weight(FontWeight::SEMIBOLD)
                            .child("Guardar modelo");
                        if enabled {
                            btn = btn
                                .cursor_pointer()
                                .hover(|s| s.bg(rgb(palette.accent_hover)))
                                .on_click(cx.listener(|view, _, _, cx| {
                                    let target = view.model_field.value.trim().to_string();
                                    if !target.is_empty() {
                                        view.prefs.model = target.clone();
                                        view.model_field.value = target.clone();
                                        view.show_toast(format!("Modelo guardado: {target}"));
                                        view.save_prefs(cx);
                                    }
                                }));
                        }
                        btn
                    })
                    .when(!available_models.is_empty(), |row| {
                        row.child(
                            div()
                                .id("open-models-modal")
                                .cursor_pointer()
                                .px_3()
                                .py_2()
                                .rounded_full()
                                .text_sm()
                                .font_weight(FontWeight::SEMIBOLD)
                                .text_color(rgb(palette.accent_foreground))
                                .hover(|s| s.bg(rgb(palette.surface_hover)))
                                .on_click(cx.listener(|view, _, _, cx| {
                                    view.active_dialog = ActiveDialog::ModelSelector;
                                    cx.notify();
                                }))
                                .child(format!("Ver modelos ({})", available_models.len())),
                        )
                    })
                    .child({
                        let enabled = has_key && !self.is_scanning_models;
                        let mut btn = div()
                            .id("scan-models-btn")
                            .px_3()
                            .py_2()
                            .rounded_full()
                            .text_sm()
                            .font_weight(FontWeight::SEMIBOLD)
                            .text_color(rgb(if enabled {
                                palette.accent_foreground
                            } else {
                                palette.disabled_foreground
                            }))
                            .child(if self.is_scanning_models {
                                "Escaneando…"
                            } else {
                                "Escanear"
                            });
                        if enabled {
                            btn = btn
                                .cursor_pointer()
                                .hover(|s| s.bg(rgb(palette.surface_hover)))
                                .on_click(cx.listener(|view, _, _, cx| {
                                    view.active_dialog = ActiveDialog::ModelSelector;
                                    view.scan_models(false, None);
                                    cx.notify();
                                }));
                        }
                        btn
                    })
                    .into_any_element(),
                if !has_key {
                    div()
                        .text_xs()
                        .text_color(rgb(palette.accent_foreground))
                        .child("Añade tu clave en Conexión para escanear o elegir modelos.")
                        .into_any_element()
                } else {
                    div().into_any_element()
                },
            ],
        );

        let effective_mode = if !has_key {
            SystemPromptMode::Literal
        } else {
            self.prefs.prompt_mode
        };

        let transcription_section = settings_section(
            "Transcripción",
            "Modo de dictado, instrucciones y motor local.",
            palette,
            [
                div()
                    .text_sm()
                    .font_weight(FontWeight::MEDIUM)
                    .text_color(rgb(palette.foreground))
                    .child("Modo de transcripción")
                    .into_any_element(),
                div()
                    .w_full()
                    .min_w_0()
                    .flex_shrink_0()
                    .flex()
                    .flex_row()
                    .flex_wrap()
                    .items_start()
                    .gap_2()
                    .children(
                        [
                            SystemPromptMode::Smart,
                            SystemPromptMode::Literal,
                            SystemPromptMode::Custom,
                        ]
                        .map(|mode| {
                            let enabled = has_key || mode == SystemPromptMode::Literal;
                            filter_chip(
                                format!("prompt-{}", mode.id()),
                                mode.label(),
                                effective_mode == mode,
                                aurora,
                                palette,
                                cx.listener(move |view, _, _, cx| {
                                    if !enabled {
                                        return;
                                    }
                                    view.prefs.prompt_mode = mode;
                                    match mode {
                                        SystemPromptMode::Smart => {
                                            view.prefs.system_prompt =
                                                preferences::DEFAULT_SYSTEM_PROMPT.to_string();
                                        }
                                        SystemPromptMode::Literal => {
                                            view.prefs.system_prompt =
                                                preferences::LITERAL_SYSTEM_PROMPT.to_string();
                                        }
                                        SystemPromptMode::Custom => {
                                            if view.prefs.system_prompt
                                                == preferences::DEFAULT_SYSTEM_PROMPT
                                                || view.prefs.system_prompt
                                                    == preferences::LITERAL_SYSTEM_PROMPT
                                            {
                                                view.prefs.system_prompt.clear();
                                            }
                                        }
                                    }
                                    view.prompt_field.value = view.prefs.system_prompt.clone();
                                    view.save_prefs(cx);
                                }),
                            )
                        }),
                    )
                    .into_any_element(),
                div()
                    .text_xs()
                    .text_color(rgb(palette.secondary_foreground))
                    .child(if has_key {
                        effective_mode.description()
                    } else {
                        "Añade una clave API en Conexión para activar el refinamiento con IA."
                    })
                    .into_any_element(),
                if has_key && effective_mode == SystemPromptMode::Custom {
                    div()
                        .w_full()
                        .min_w_0()
                        .flex()
                        .flex_col()
                        .gap_2()
                        .child(render_text_field(
                            "settings-custom-prompt",
                            "Instrucciones personalizadas",
                            &format!("Ejemplo: {}", preferences::CUSTOM_SYSTEM_PROMPT_EXAMPLE),
                            &self.prompt_field,
                            false,
                            true,
                            true,
                            Some("Pegar instrucciones desde el portapapeles"),
                            palette,
                            window,
                            cx.listener(|view, event, _, cx| {
                                if view.prompt_field.handle_key_down(event, true, cx) {
                                    view.prefs.system_prompt = view.prompt_field.value.clone();
                                    view.save_prefs(cx);
                                }
                                cx.stop_propagation();
                            }),
                            None,
                            Some(Box::new(cx.listener(|view, _, _, cx| {
                                if let Some(item) = cx.read_from_clipboard()
                                    && let Some(text) = item.text()
                                {
                                    view.prompt_field.value = text.clone();
                                    view.prefs.system_prompt = text;
                                    view.save_prefs(cx);
                                }
                            }))),
                        ))
                        .child(
                            div()
                                .text_xs()
                                .text_color(rgb(palette.secondary_foreground))
                                .child(format!(
                                    "Ejemplo sencillo: «{}»",
                                    preferences::CUSTOM_SYSTEM_PROMPT_EXAMPLE
                                )),
                        )
                        .child(
                            div()
                                .w_full()
                                .flex()
                                .flex_row()
                                .flex_wrap()
                                .items_center()
                                .gap_4()
                                .child(
                                    div()
                                        .id("use-example-prompt")
                                        .cursor_pointer()
                                        .text_xs()
                                        .font_weight(FontWeight::SEMIBOLD)
                                        .text_color(rgb(palette.accent_foreground))
                                        .on_click(cx.listener(|view, _, _, cx| {
                                            view.prefs.system_prompt =
                                                preferences::CUSTOM_SYSTEM_PROMPT_EXAMPLE
                                                    .to_string();
                                            view.prompt_field.value =
                                                view.prefs.system_prompt.clone();
                                            view.save_prefs(cx);
                                        }))
                                        .child("Usar ejemplo"),
                                )
                                .child(
                                    div()
                                        .id("reset-smart-prompt")
                                        .cursor_pointer()
                                        .text_xs()
                                        .font_weight(FontWeight::SEMIBOLD)
                                        .text_color(rgb(palette.secondary_foreground))
                                        .on_click(cx.listener(|view, _, _, cx| {
                                            view.prefs.prompt_mode = SystemPromptMode::Smart;
                                            view.prefs.system_prompt =
                                                preferences::DEFAULT_SYSTEM_PROMPT.to_string();
                                            view.prompt_field.value =
                                                view.prefs.system_prompt.clone();
                                            view.save_prefs(cx);
                                        }))
                                        .child("Restablecer modo inteligente"),
                                ),
                        )
                        .into_any_element()
                } else {
                    div().into_any_element()
                },
                div()
                    .w_full()
                    .h(px(1.))
                    .bg(rgb(palette.border_subtle))
                    .into_any_element(),
                div()
                    .text_sm()
                    .font_weight(FontWeight::MEDIUM)
                    .text_color(rgb(palette.foreground))
                    .child("Idioma de Whisper local")
                    .into_any_element(),
                div()
                    .w_full()
                    .min_w_0()
                    .flex_shrink_0()
                    .flex()
                    .flex_row()
                    .flex_wrap()
                    .items_start()
                    .gap_2()
                    .children(WhisperLanguage::ALL.iter().copied().map(|lang| {
                        filter_chip(
                            format!("whisper-lang-{}", lang.code()),
                            lang.label(),
                            self.prefs.whisper_language == lang,
                            aurora,
                            palette,
                            cx.listener(move |view, _, _, cx| {
                                view.prefs.whisper_language = lang;
                                view.save_prefs(cx);
                            }),
                        )
                    }))
                    .into_any_element(),
                div()
                    .id("toggle-experimental")
                    .cursor_pointer()
                    .text_sm()
                    .font_weight(FontWeight::SEMIBOLD)
                    .text_color(rgb(palette.accent_foreground))
                    .on_click(cx.listener(|view, _, _, cx| {
                        view.show_experimental = !view.show_experimental;
                        cx.notify();
                    }))
                    .child(if self.show_experimental {
                        "Ocultar opciones experimentales"
                    } else {
                        "Opciones experimentales"
                    })
                    .into_any_element(),
                if self.show_experimental {
                    switch_row(
                        "switch-whisper-llm",
                        "Whisper + Refinamiento por LLM (Experimental)",
                        "Utiliza Whisper local para transcribir audio a texto y luego procesa el resultado con un modelo LLM de texto para corregir formato y sintaxis.",
                        self.prefs.whisper_llm_refinement,
                        palette,
                        cx.listener(|view, _, _, cx| {
                            view.prefs.whisper_llm_refinement = !view.prefs.whisper_llm_refinement;
                            view.save_prefs(cx);
                        }),
                    )
                    .into_any_element()
                } else {
                    div().into_any_element()
                },
            ],
        );

        let left_column = div()
            .w_full()
            .min_w_0()
            .flex()
            .flex_col()
            .gap_4()
            .child(connection_section)
            .child(model_section);

        if two_columns {
            div()
                .w_full()
                .min_w_0()
                .flex()
                .flex_row()
                .items_start()
                .gap_5()
                .child(div().flex_1().min_w_0().child(left_column))
                .child(div().flex_1().min_w_0().child(transcription_section))
                .into_any_element()
        } else {
            div()
                .w_full()
                .min_w_0()
                .flex()
                .flex_col()
                .gap_4()
                .child(left_column)
                .child(transcription_section)
                .into_any_element()
        }
    }

    fn render_api_key_dialog(
        &self,
        aurora: bool,
        dark: bool,
        palette: &Palette,
        window: &Window,
        cx: &mut Context<Self>,
    ) -> impl IntoElement {
        let can_save = !self.dialog_api_key_field.value.trim().is_empty();
        div()
            .id("apikey-scrim")
            .absolute()
            .inset_0()
            .flex()
            .items_center()
            .justify_center()
            .p_5()
            .bg(rgba(modal_veil_color(aurora, dark)))
            .on_click(cx.listener(|view, _, _, cx| {
                view.active_dialog = ActiveDialog::None;
                cx.notify();
            }))
            .child(
                div()
                    .id("apikey-card")
                    .w(px(440.))
                    .max_w_full()
                    .rounded_3xl()
                    .bg(rgb(palette.surface_raised))
                    .border_1()
                    .border_color(rgb(palette.border_subtle))
                    .shadow_lg()
                    .p_6()
                    .flex()
                    .flex_col()
                    .gap_4()
                    .occlude()
                    .on_click(|_, _, cx| cx.stop_propagation())
                    .child(
                        div()
                            .text_xl()
                            .font_weight(FontWeight::SEMIBOLD)
                            .text_color(rgb(palette.foreground))
                            .child(format!("Conectar con {}", self.prefs.provider.label())),
                    )
                    .child(render_text_field(
                        "dialog-api-key",
                        "Clave de API",
                        "Pega aquí tu clave",
                        &self.dialog_api_key_field,
                        true,
                        true,
                        false,
                        Some("Pegar clave API desde el portapapeles"),
                        palette,
                        window,
                        cx.listener(|view, event, _, cx| {
                            if view.dialog_api_key_field.handle_key_down(event, false, cx) {
                                cx.notify();
                            }
                            cx.stop_propagation();
                        }),
                        Some(Box::new(cx.listener(|view, _, _, cx| {
                            view.dialog_api_key_field.revealed =
                                !view.dialog_api_key_field.revealed;
                            cx.notify();
                        }))),
                        Some(Box::new(cx.listener(|view, _, _, cx| {
                            if let Some(item) = cx.read_from_clipboard()
                                && let Some(text) = item.text()
                            {
                                view.dialog_api_key_field.value = text.replace(['\r', '\n'], "");
                                cx.notify();
                            }
                        }))),
                    ))
                    .child(
                        div()
                            .w_full()
                            .flex()
                            .flex_row()
                            .justify_end()
                            .gap_2()
                            .child(
                                div()
                                    .id("apikey-cancel")
                                    .cursor_pointer()
                                    .px_4()
                                    .py_2()
                                    .rounded_xl()
                                    .text_sm()
                                    .font_weight(FontWeight::SEMIBOLD)
                                    .text_color(rgb(palette.secondary_foreground))
                                    .hover(|s| s.bg(rgb(palette.surface_hover)))
                                    .on_click(cx.listener(|view, _, _, cx| {
                                        view.active_dialog = ActiveDialog::None;
                                        cx.notify();
                                    }))
                                    .child("Cancelar"),
                            )
                            .child({
                                let mut btn = div()
                                    .id("apikey-save")
                                    .px_4()
                                    .py_2()
                                    .rounded_xl()
                                    .text_sm()
                                    .font_weight(FontWeight::SEMIBOLD)
                                    .text_color(rgb(if can_save {
                                        palette.accent_foreground
                                    } else {
                                        palette.disabled_foreground
                                    }))
                                    .child("Guardar");
                                if can_save {
                                    btn = btn
                                        .cursor_pointer()
                                        .hover(|s| s.bg(rgb(palette.surface_hover)))
                                        .on_click(cx.listener(|view, _, _, cx| {
                                            let key = view.dialog_api_key_field.value.clone();
                                            view.active_dialog = ActiveDialog::None;
                                            view.update_api_key(key, cx);
                                        }));
                                }
                                btn
                            }),
                    ),
            )
    }

    fn render_model_selector_dialog(
        &self,
        viewport: gpui::Size<gpui::Pixels>,
        aurora: bool,
        dark: bool,
        palette: &Palette,
        cx: &mut Context<Self>,
    ) -> impl IntoElement {
        let provider = self.prefs.provider;
        let models = self.prefs.available_models(provider);
        let is_enabled = self.prefs.provider_enabled;
        let (cols, modal_w, max_h) = model_grid_layout(
            models.len(),
            f32::from(viewport.width),
            f32::from(viewport.height),
        );
        let list_max_h = (max_h - 156.0).max(160.0);

        let rows: Vec<_> = models
            .chunks(cols)
            .enumerate()
            .map(|(row_idx, chunk)| {
                let mut cells = Vec::with_capacity(cols);
                for col_idx in 0..cols {
                    if let Some(model_id) = chunk.get(col_idx) {
                        let idx = row_idx * cols + col_idx;
                        let selected = is_enabled && *model_id == self.prefs.model;
                        let bg = if selected {
                            if aurora {
                                rgb(palette.selection)
                            } else {
                                rgb(palette.primary_container)
                            }
                        } else {
                            rgba(palette.surface_muted_strong)
                        };
                        let text = if selected && !aurora {
                            rgb(palette.on_primary_container)
                        } else {
                            rgb(palette.foreground)
                        };
                        let chosen = model_id.clone();
                        cells.push(
                            div()
                                .id(SharedString::from(format!("modelsel-opt-{idx}")))
                                .cursor_pointer()
                                .flex_1()
                                .min_w_0()
                                .rounded_xl()
                                .border_1()
                                .border_color(rgb(if selected {
                                    palette.accent_foreground
                                } else {
                                    palette.border_subtle
                                }))
                                .bg(bg)
                                .px_3()
                                .py(px(10.))
                                .flex()
                                .flex_row()
                                .items_center()
                                .justify_between()
                                .gap_2()
                                .hover(|s| s.bg(rgb(palette.surface_hover)))
                                .on_click(cx.listener(move |view, _, _, cx| {
                                    view.prefs.model = chosen.clone();
                                    view.prefs.provider_enabled = true;
                                    view.model_field.value = chosen.clone();
                                    view.active_dialog = ActiveDialog::None;
                                    view.save_prefs(cx);
                                }))
                                .child(
                                    div()
                                        .flex_1()
                                        .min_w_0()
                                        .truncate()
                                        .text_xs()
                                        .font_weight(FontWeight::MEDIUM)
                                        .text_color(text)
                                        .child(model_id.clone()),
                                )
                                .when(selected, |cell| {
                                    cell.child(
                                        div()
                                            .size(px(8.))
                                            .rounded_full()
                                            .flex_shrink_0()
                                            .bg(rgb(palette.accent_foreground)),
                                    )
                                })
                                .into_any_element(),
                        );
                    } else {
                        cells.push(div().flex_1().min_w_0().into_any_element());
                    }
                }
                div().w_full().flex().flex_row().gap_2().children(cells)
            })
            .collect();

        div()
            .id("modelsel-scrim")
            .absolute()
            .inset_0()
            .flex()
            .items_center()
            .justify_center()
            .p_5()
            .bg(rgba(modal_veil_color(aurora, dark)))
            .on_click(cx.listener(|view, _, _, cx| {
                view.active_dialog = ActiveDialog::None;
                cx.notify();
            }))
            .child(
                div()
                    .id("modelsel-card")
                    .w(px(modal_w))
                    .max_w(px((f32::from(viewport.width) * 0.70).max(340.0)))
                    .max_h(px(max_h))
                    .overflow_hidden()
                    .rounded_3xl()
                    .bg(rgb(palette.surface_raised))
                    .border_1()
                    .border_color(rgb(palette.border_subtle))
                    .shadow_lg()
                    .p_6()
                    .flex()
                    .flex_col()
                    .gap_3()
                    .occlude()
                    .on_click(|_, _, cx| cx.stop_propagation())
                    .child(
                        div()
                            .w_full()
                            .flex_shrink_0()
                            .flex()
                            .flex_row()
                            .items_center()
                            .justify_between()
                            .child(
                                div()
                                    .text_xl()
                                    .font_weight(FontWeight::SEMIBOLD)
                                    .text_color(rgb(palette.foreground))
                                    .child(format!("Modelo de {}", provider.label())),
                            )
                            .child({
                                let enabled = !self.is_scanning_models && !self.api_key.trim().is_empty();
                                let mut btn = div()
                                    .id("modelsel-scan")
                                    .px_3()
                                    .py_1()
                                    .rounded_xl()
                                    .text_xs()
                                    .font_weight(FontWeight::SEMIBOLD)
                                    .text_color(rgb(if enabled {
                                        palette.accent_foreground
                                    } else {
                                        palette.disabled_foreground
                                    }))
                                    .child(if self.is_scanning_models {
                                        "Escaneando…"
                                    } else {
                                        "Escanear"
                                    });
                                if enabled {
                                    btn = btn
                                        .cursor_pointer()
                                        .hover(|s| s.bg(rgb(palette.surface_hover)))
                                        .on_click(cx.listener(|view, _, _, cx| {
                                            view.scan_models(false, None);
                                            cx.notify();
                                        }));
                                }
                                btn
                            }),
                    )
                    .child(
                        div()
                            .flex_shrink_0()
                            .text_sm()
                            .text_color(rgb(palette.secondary_foreground))
                            .child(if models.is_empty() {
                                "No hay modelos escaneados todavía. Pulsa «Escanear» para consultar la API.".to_string()
                            } else {
                                format!("Elige el modelo que procesará tu dictado ({} disponibles):", models.len())
                            }),
                    )
                    .when(!models.is_empty(), |col| {
                        col.child(
                            div()
                                .id("modelsel-list")
                                .w_full()
                                .max_h(px(list_max_h))
                                .overflow_y_scroll()
                                .flex()
                                .flex_col()
                                .gap_2()
                                .children(rows),
                        )
                    })
                    .child(
                        div()
                            .w_full()
                            .flex_shrink_0()
                            .pt_2()
                            .flex()
                            .flex_row()
                            .items_center()
                            .justify_between()
                            .child(
                                div()
                                    .id("modelsel-toggle")
                                    .cursor_pointer()
                                    .px_3()
                                    .py_2()
                                    .rounded_xl()
                                    .text_sm()
                                    .font_weight(FontWeight::SEMIBOLD)
                                    .text_color(rgb(if is_enabled {
                                        palette.error
                                    } else {
                                        palette.accent_foreground
                                    }))
                                    .hover(|s| s.bg(rgb(palette.surface_hover)))
                                    .on_click(cx.listener(|view, _, _, cx| {
                                        view.prefs.provider_enabled = !view.prefs.provider_enabled;
                                        view.active_dialog = ActiveDialog::None;
                                        view.save_prefs(cx);
                                    }))
                                    .child(if is_enabled {
                                        format!("Desactivar {}", provider.label())
                                    } else {
                                        format!("Activar {}", provider.label())
                                    }),
                            )
                            .child(
                                div()
                                    .id("modelsel-close")
                                    .cursor_pointer()
                                    .px_4()
                                    .py_2()
                                    .rounded_xl()
                                    .text_sm()
                                    .font_weight(FontWeight::SEMIBOLD)
                                    .text_color(rgb(palette.accent_foreground))
                                    .hover(|s| s.bg(rgb(palette.surface_hover)))
                                    .on_click(cx.listener(|view, _, _, cx| {
                                        view.active_dialog = ActiveDialog::None;
                                        cx.notify();
                                    }))
                                    .child("Cerrar"),
                            ),
                    ),
            )
    }

    fn render_whisper_dialog(
        &self,
        aurora: bool,
        dark: bool,
        palette: &Palette,
        cx: &mut Context<Self>,
    ) -> impl IntoElement {
        let ws = self.whisper_state.lock().ok();
        let current_model = ws.as_ref().and_then(|w| w.model);
        drop(ws);
        let is_enabled = self.prefs.whisper_enabled;
        let is_same = Some(self.selected_whisper_model) == current_model;

        div()
            .id("whisper-scrim")
            .absolute()
            .inset_0()
            .flex()
            .items_center()
            .justify_center()
            .p_5()
            .bg(rgba(modal_veil_color(aurora, dark)))
            .on_click(cx.listener(|view, _, _, cx| {
                view.active_dialog = ActiveDialog::None;
                cx.notify();
            }))
            .child(
                div()
                    .id("whisper-card")
                    .w(px(520.))
                    .max_w_full()
                    .rounded_3xl()
                    .bg(rgb(palette.surface_raised))
                    .border_1()
                    .border_color(rgb(palette.border_subtle))
                    .shadow_lg()
                    .p_6()
                    .flex()
                    .flex_col()
                    .gap_3()
                    .occlude()
                    .on_click(|_, _, cx| cx.stop_propagation())
                    .child(
                        div()
                            .text_xl()
                            .font_weight(FontWeight::SEMIBOLD)
                            .text_color(rgb(palette.foreground))
                            .child("Modelo de Whisper local"),
                    )
                    .child(
                        div()
                            .text_xs()
                            .font_weight(FontWeight::SEMIBOLD)
                            .text_color(rgb(palette.secondary_foreground))
                            .child("Idioma de transcripción:"),
                    )
                    .child(
                        div()
                            .w_full()
                            .flex()
                            .flex_row()
                            .flex_wrap()
                            .gap_1()
                            .children(WhisperLanguage::ALL.iter().copied().map(|lang| {
                                filter_chip(
                                    format!("wlang-{}", lang.code()),
                                    lang.label(),
                                    self.prefs.whisper_language == lang,
                                    aurora,
                                    palette,
                                    cx.listener(move |view, _, _, cx| {
                                        view.prefs.whisper_language = lang;
                                        view.save_prefs(cx);
                                    }),
                                )
                            })),
                    )
                    .child(
                        div()
                            .text_sm()
                            .text_color(rgb(palette.secondary_foreground))
                            .child(
                                "Elige la variante de Whisper que procesará tus dictados offline:",
                            ),
                    )
                    .child(div().w_full().flex().flex_col().gap_2().children(
                        WhisperModel::ALL.iter().copied().enumerate().map(|(i, m)| {
                            let selected = self.selected_whisper_model == m;
                            let active = is_enabled && Some(m) == current_model;
                            let bg = if selected {
                                if aurora {
                                    rgb(palette.selection)
                                } else {
                                    rgb(palette.primary_container)
                                }
                            } else {
                                rgba(palette.surface_muted_strong)
                            };
                            div()
                                .id(SharedString::from(format!("wmodel-{i}")))
                                .cursor_pointer()
                                .w_full()
                                .rounded_xl()
                                .border_1()
                                .border_color(rgb(if selected {
                                    palette.accent_foreground
                                } else {
                                    palette.border_subtle
                                }))
                                .bg(bg)
                                .p_3()
                                .flex()
                                .flex_col()
                                .gap(px(2.))
                                .hover(|s| s.bg(rgb(palette.surface_hover)))
                                .on_click(cx.listener(move |view, _, _, cx| {
                                    view.selected_whisper_model = m;
                                    cx.notify();
                                }))
                                .child(
                                    div()
                                        .w_full()
                                        .flex()
                                        .flex_row()
                                        .items_center()
                                        .justify_between()
                                        .child(
                                            div()
                                                .text_sm()
                                                .font_weight(FontWeight::SEMIBOLD)
                                                .text_color(rgb(palette.foreground))
                                                .child(m.label()),
                                        )
                                        .when(active, |row| {
                                            row.child(
                                                div()
                                                    .rounded_lg()
                                                    .bg(rgb(palette.accent))
                                                    .text_color(rgb(palette.on_accent))
                                                    .px_2()
                                                    .py(px(1.))
                                                    .text_xs()
                                                    .font_weight(FontWeight::SEMIBOLD)
                                                    .child("Activo"),
                                            )
                                        }),
                                )
                                .child(
                                    div()
                                        .text_xs()
                                        .text_color(rgb(palette.secondary_foreground))
                                        .child(m.description()),
                                )
                        }),
                    ))
                    .child(
                        div()
                            .w_full()
                            .pt_2()
                            .flex()
                            .flex_row()
                            .items_center()
                            .justify_between()
                            .child(
                                div()
                                    .id("whisper-toggle")
                                    .cursor_pointer()
                                    .px_3()
                                    .py_2()
                                    .rounded_xl()
                                    .text_sm()
                                    .font_weight(FontWeight::SEMIBOLD)
                                    .text_color(rgb(if is_enabled {
                                        palette.error
                                    } else {
                                        palette.accent_foreground
                                    }))
                                    .hover(|s| s.bg(rgb(palette.surface_hover)))
                                    .on_click(cx.listener(|view, _, _, cx| {
                                        view.prefs.whisper_enabled = !view.prefs.whisper_enabled;
                                        view.active_dialog = ActiveDialog::None;
                                        view.save_prefs(cx);
                                    }))
                                    .child(if is_enabled {
                                        "Desactivar Whisper"
                                    } else {
                                        "Activar Whisper"
                                    }),
                            )
                            .child(
                                div()
                                    .id("whisper-confirm")
                                    .cursor_pointer()
                                    .px_4()
                                    .py_2()
                                    .rounded_xl()
                                    .text_sm()
                                    .font_weight(FontWeight::SEMIBOLD)
                                    .text_color(rgb(palette.accent_foreground))
                                    .hover(|s| s.bg(rgb(palette.surface_hover)))
                                    .on_click(cx.listener(move |view, _, _, cx| {
                                        if !is_same {
                                            view.prefs.whisper_enabled = true;
                                            let model = view.selected_whisper_model;
                                            let shared = view.whisper_state.clone();
                                            std::thread::Builder::new()
                                                .name("lyraflow-whisper-install".into())
                                                .spawn(move || {
                                                    providers::install_whisper_blocking(
                                                        model, shared,
                                                    );
                                                })
                                                .ok();
                                            view.save_prefs(cx);
                                        }
                                        view.active_dialog = ActiveDialog::None;
                                        cx.notify();
                                    }))
                                    .child(if is_same { "Cerrar" } else { "Instalar" }),
                            ),
                    ),
            )
    }

    fn render_update_panel(
        &self,
        dark: bool,
        palette: &Palette,
        cx: &mut Context<Self>,
    ) -> impl IntoElement {
        let checking = matches!(self.update_state, updates::State::Checking);
        let available = matches!(self.update_state, updates::State::Done(updates::Outcome::Available(_)));
        let failed = matches!(self.update_state, updates::State::Failed(_));
        let message = match &self.update_state {
            updates::State::Idle => format!("Versión instalada · v{}", about::VERSION),
            updates::State::Checking => "Buscando actualizaciones…".into(),
            updates::State::Done(updates::Outcome::Current(_)) => {
                "No se han encontrado actualizaciones".into()
            }
            updates::State::Done(updates::Outcome::Available(version)) => {
                format!("v{version} disponible · fuente oficial")
            }
            updates::State::Done(updates::Outcome::Ahead(version)) => {
                if cfg!(debug_assertions) {
                    format!("Tu v{} es más reciente que la publicada · v{version}", about::VERSION)
                } else {
                    "No se han encontrado actualizaciones".into()
                }
            }
            updates::State::Done(updates::Outcome::NoRelease) => {
                "No se encontró una versión estable publicada.".into()
            }
            updates::State::Failed(error) => error.clone(),
        };
        let btn_label = if checking {
            "Buscando…"
        } else if available {
            "Actualizar"
        } else if failed {
            "Reintentar"
        } else {
            "Buscar actualizaciones"
        };
        let panel_fill = if dark {
            rgba((palette.foreground << 8) | 0x08)
        } else {
            rgba((palette.surface << 8) | 0xFF)
        };
        let btn_fill = if available {
            rgba((palette.accent << 8) | 0xFF)
        } else if dark {
            rgba((palette.foreground << 8) | 0x0E)
        } else {
            rgba((palette.surface_raised << 8) | 0xFF)
        };
        let action_btn = div()
            .id("about-check-updates")
            .flex_shrink_0()
            .px_3()
            .py(px(7.))
            .rounded_lg()
            .border_1()
            .border_color(rgb(if available { palette.accent } else { palette.control_border }))
            .bg(btn_fill)
            .text_xs()
            .font_weight(FontWeight::SEMIBOLD)
            .text_color(rgb(if available { 0xFFFFFF } else { palette.foreground }))
            .when(checking, |b| b.opacity(0.6).cursor_default())
            .when(!checking, |b| b.cursor_pointer().hover(|s| s.bg(rgb(if available { palette.accent } else { palette.surface_hover }))))
            .on_click(cx.listener(move |view, _, _, cx| {
                if available {
                    cx.open_url(about::APP_PAGE_URL);
                } else {
                    view.check_updates(cx);
                }
            }))
            .child(btn_label);

        div().w_full().rounded_2xl().border_1().border_color(rgb(palette.border_subtle))
            .bg(panel_fill).px_4().py_3()
            .child(div().flex().flex_wrap().items_center().justify_between().gap_3()
                .child(div().flex_1().min_w(px(180.)).flex().flex_col().gap(px(3.))
                    .child(div().flex().items_center().gap_2()
                        .child(lyra_icon(IconType::History, 15., rgb(palette.accent_foreground)))
                        .child(div().text_sm().font_weight(FontWeight::SEMIBOLD).child("Actualizaciones")))
                    .child(div().text_xs()
                        .text_color(rgb(if failed { palette.error } else { palette.secondary_foreground }))
                        .child(message)))
                .child(action_btn))
    }

    fn render_about_dialog(
        &self,
        viewport: gpui::Size<gpui::Pixels>,
        _aurora: bool,
        dark: bool,
        palette: &Palette,
        cx: &mut Context<Self>,
    ) -> impl IntoElement {
        let wide = viewport.width >= px(600.);
        let about_w = px(660.).min((viewport.width - px(48.)).max(px(1.)));
        let panel_fill = if dark {
            rgba((palette.foreground << 8) | 0x08)
        } else {
            rgba((palette.surface << 8) | 0xFF)
        };
        let control_fill = if dark {
            rgba((palette.foreground << 8) | 0x0D)
        } else {
            rgba((palette.surface_raised << 8) | 0xFF)
        };
        let badge = |text: String| {
            div()
                .rounded_md()
                .px_2()
                .py(px(3.))
                .text_xs()
                .bg(rgb(palette.selection))
                .text_color(rgb(palette.accent_foreground))
                .child(text)
        };
        let panel = || {
            div()
                .w_full()
                .rounded_2xl()
                .border_1()
                .border_color(rgb(palette.border_subtle))
                .bg(panel_fill)
                .px_4()
                .py_3()
                .flex()
                .flex_col()
                .gap_2()
        };
        let link = |id: &'static str, title: &'static str, url: &'static str, icon: IconType| {
            div()
                .id(id)
                .cursor_pointer()
                .rounded_lg()
                .border_1()
                .border_color(rgb(palette.control_border))
                .bg(control_fill)
                .px_3()
                .py(px(7.))
                .text_xs()
                .font_weight(FontWeight::SEMIBOLD)
                .flex().items_center().gap_2()
                .hover(|style| style.bg(rgb(palette.surface_hover)))
                .on_click(move |_, _, cx| cx.open_url(url))
                .child(lyra_icon(icon, 14., rgb(palette.foreground)))
                .child(title)
        };
        let support = |id: &'static str,
                       title: &'static str,
                       note: &'static str,
                       url: &'static str,
                       color: u32,
                       icon: IconType| {
            let fill = (color << 8) | if dark { 0x1E } else { 0x16 };
            let border = (color << 8) | if dark { 0x52 } else { 0x45 };
            let bubble_bg = (color << 8) | if dark { 0x29 } else { 0x1E };
            let hover_fill = (color << 8) | if dark { 0x33 } else { 0x26 };
            div()
                .id(id)
                .flex_1()
                .min_w_0()
                .cursor_pointer()
                .px_3()
                .py(px(10.))
                .rounded_xl()
                .border_1()
                .border_color(rgba(border))
                .bg(rgba(fill))
                .flex()
                .items_center()
                .gap(px(10.))
                .hover(|style| style.bg(rgba(hover_fill)))
                .on_click(move |_, _, cx| cx.open_url(url))
                .child(
                    div()
                        .size(px(32.))
                        .rounded_lg()
                        .bg(rgba(bubble_bg))
                        .border_1()
                        .border_color(rgba(border))
                        .flex_shrink_0()
                        .flex()
                        .items_center()
                        .justify_center()
                        .child(lyra_icon(icon, 16., rgb(color))),
                )
                .child(
                    div()
                        .flex_1()
                        .min_w_0()
                        .flex()
                        .flex_col()
                        .gap(px(2.))
                        .child(div().text_sm().font_weight(FontWeight::SEMIBOLD).child(title))
                        .child(div().text_xs().text_color(rgb(palette.secondary_foreground)).child(note)),
                )
                .child(lyra_icon(IconType::ExternalLink, 13., rgb(palette.secondary_foreground)))
        };
        let donate = support(
            "about-donate",
            about::SUPPORT_LINKS[0].label,
            "Yape, Plin y transferencias directas",
            about::SUPPORT_LINKS[0].url,
            if dark { 0xEC4899 } else { 0xDB2777 },
            IconType::Heart,
        );
        let coffee = support(
            "about-coffee",
            about::SUPPORT_LINKS[1].label,
            "Apoyo para café y servidores",
            about::SUPPORT_LINKS[1].url,
            if dark { 0xF59E0B } else { 0xD97706 },
            IconType::Coffee,
        );
        let support_buttons = div()
            .w_full()
            .flex()
            .gap_2()
            .when(wide, |row| row.flex_row())
            .when(!wide, |row| row.flex_col())
            .child(donate)
            .child(coffee);

        div().id("about-scrim").absolute().inset_0().flex().items_center().justify_center()
            .p_4()
            .on_click(cx.listener(|view, _, window, cx| view.close_about(&CloseAbout, window, cx)))
            .children(self.about_backdrop.as_ref().map(|image| gpui::img(image.clone()).absolute().inset_0().size_full()))
            .child(div().absolute().inset_0().bg(rgba((palette.background << 8) | 0x8A)))
            .child(div().id("about-card").w(about_w)
                .max_h((viewport.height - px(40.)).max(px(120.))).overflow_y_scroll()
                .rounded_2xl().border_1().border_color(rgb(palette.border_subtle))
                .bg(rgb(palette.surface_raised)).text_color(rgb(palette.foreground))
                .shadow_lg().px_5().py_4().flex().flex_col().gap(px(10.)).occlude()
                .on_click(|_, _, cx| cx.stop_propagation())
                .child(div().w_full().h(px(3.)).rounded_full().bg(rgb(palette.accent)))
                .child(div().w_full().flex().items_center().gap_3()
                    .child(lyra_icon(IconType::Info, 22., rgb(palette.accent_foreground)))
                    .child(div().flex_1().min_w_0().flex().flex_col().gap(px(2.))
                        .child(div().text_sm().font_weight(FontWeight::BOLD).child("Acerca de LyraFlow"))
                        .child(div().text_xs().text_color(rgb(palette.secondary_foreground))
                            .child("Información del producto, licencia y canales oficiales")))
                    .child(div().id("about-close").cursor_pointer().size(px(30.)).rounded_lg()
                        .flex().items_center().justify_center().border_1()
                        .border_color(rgb(palette.border_subtle))
                        .track_focus(&self.controls.about_close).tab_stop(true)
                        .hover(|style| style.bg(rgb(palette.surface_hover)))
                        .focus(|style| style.border_color(rgb(palette.focus_ring)))
                        .on_click(cx.listener(|view, _, window, cx| view.close_about(&CloseAbout, window, cx)))
                        .child("×")))
                .child(panel().child(div().flex().items_center().gap(px(14.))
                    .child(
                        div()
                            .size(px(58.))
                            .rounded_xl()
                            .bg(control_fill)
                            .border_1()
                            .border_color(rgb(palette.border_subtle))
                            .p(px(7.))
                            .flex_shrink_0()
                            .flex()
                            .items_center()
                            .justify_center()
                            .child(gpui::img(self.icon.clone()).size_full()),
                    )
                    .child(div().flex_1().min_w_0().flex().flex_col().gap(px(5.))
                        .child(div().text_base().font_weight(FontWeight::BOLD).child(about::APP_NAME))
                        .child(div().text_xs().text_color(rgb(palette.secondary_foreground)).child(about::DESCRIPTION))
                        .child(div().flex().flex_wrap().gap(px(6.))
                            .child(badge(format!("v{}", about::VERSION)))
                            .child(badge(about::PLATFORM.into()))
                            .child(badge(format!("Licencia {}", about::LICENSE)))
                            .child(badge(format!("{} © 2026", about::AUTHOR)))))))
                .child(self.render_update_panel(dark, palette, cx))
                .child(panel()
                    .child(div().text_sm().font_weight(FontWeight::SEMIBOLD)
                        .text_color(rgb(palette.accent_foreground)).child("Apoyo oficial y canales de la comunidad"))
                    .child(div().text_xs().text_color(rgb(palette.secondary_foreground)).child(about::THANKS))
                    .child(support_buttons)
                    .child(div().flex().flex_wrap().gap_2()
                        .child(link("about-website", "biglexj.com", about::APP_PAGE_URL, IconType::BiglexWeb))
                        .child(link("about-github", "@biglexj", about::CREDIT_LINKS[1].url, IconType::Github))
                        .child(link("about-repo", "Repositorio", about::REPO_URL, IconType::ExternalLink))
                        .child(link("about-feedback", "Reportar un problema", about::CREDIT_LINKS[0].url, IconType::Bug))))
                .child(div().id("about-tech-toggle").w_full().cursor_pointer().rounded_lg()
                    .border_1().border_color(rgb(palette.border_subtle)).bg(panel_fill)
                    .px_3().py(px(9.)).text_xs().flex().items_center().justify_center().gap_2()
                    .hover(|style| style.bg(rgb(palette.surface_hover)))
                    .on_click(cx.listener(|view, _, _, cx| {
                        view.about_tech_open = !view.about_tech_open;
                        cx.notify();
                    }))
                    .child(lyra_icon(IconType::Shield, 15., rgb(palette.secondary_foreground)))
                    .child(if self.about_tech_open { "Ocultar licencia e información técnica" }
                        else { "Licencia e información técnica" }))
                .when(self.about_tech_open, |card| card.child(panel()
                    .child(div().text_xs().text_color(rgb(palette.secondary_foreground))
                        .child(format!("LyraFlow {} · {} · Rust + GPUI · Paleta Aurora {}",
                            about::VERSION, about::PLATFORM, theme::palette_version())))
                    .child(div().text_xs().child(about::LICENSE_TEXT))))
            )
    }
}

impl Render for LyraFlowApp {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        let viewport = window.viewport_size();
        let wide = viewport.width >= px(720.);
        let dark = self.prefs.appearance.resolved_dark(self.system_dark);
        let aurora = self.is_aurora();
        let palette = theme::palette(self.prefs.appearance.theme, dark);

        let content = match self.destination {
            Destination::Home => self
                .render_home(viewport, aurora, &palette, cx)
                .into_any_element(),
            Destination::History => self.render_history(aurora, &palette, cx).into_any_element(),
            Destination::Settings => self
                .render_settings(viewport, aurora, &palette, window, cx)
                .into_any_element(),
        };

        let shell = if wide {
            div()
                .size_full()
                .flex()
                .flex_row()
                .child(self.render_navigation_rail(aurora, &palette, cx))
                .child(
                    div()
                        .flex_1()
                        .min_w_0()
                        .h_full()
                        .overflow_hidden()
                        .child(content),
                )
        } else {
            div()
                .size_full()
                .flex()
                .flex_col()
                .child(
                    div()
                        .flex_1()
                        .min_h_0()
                        .w_full()
                        .overflow_hidden()
                        .child(content),
                )
                .child(self.render_navigation_bar(aurora, &palette, cx))
        };
        if self.active_dialog != ActiveDialog::About { self.about_backdrop = None; }
        let shell = if self.active_dialog == ActiveDialog::About && self.about_backdrop.is_some() {
            shell.opacity(0.)
        } else if self.active_dialog != ActiveDialog::None {
            shell.opacity(0.18)
        } else {
            shell
        };

        let mut root = div()
            .id("root")
            .relative()
            .size_full()
            .font_family("Segoe UI")
            .text_base()
            .bg(rgb(palette.background))
            .text_color(rgb(palette.foreground))
            .track_focus(&self.focus)
            .key_context("LyraFlowDemo")
            .on_action(cx.listener(|view, _: &ToggleRecord, _, cx| view.toggle_dictation(cx)))
            .on_action(cx.listener(|view, _: &CancelRecord, _, cx| view.cancel_dictation(cx)))
            .on_action(cx.listener(Self::toggle_mode))
            .on_action(cx.listener(Self::open_about))
            .on_action(cx.listener(Self::close_about))
            .on_action(
                cx.listener::<UseAurora>(|view, _, _, cx| view.set_theme(ThemeName::Aurora, cx)),
            )
            .on_action(
                cx.listener::<UseClassic>(|view, _, _, cx| view.set_theme(ThemeName::Classic, cx)),
            )
            .on_action(cx.listener::<FocusNext>(|_, _, window, _| window.focus_next()))
            .on_action(cx.listener::<FocusPrevious>(|_, _, window, _| window.focus_prev()))
            .child(shell);

        if let Some((toast_msg, _)) = &self.toast {
            root = root.child(
                div()
                    .absolute()
                    .top_4()
                    .left_0()
                    .right_0()
                    .flex()
                    .justify_center()
                    .child(
                        div()
                            .rounded_full()
                            .bg(rgb(palette.foreground))
                            .text_color(rgb(palette.background))
                            .px_5()
                            .py(px(10.))
                            .text_sm()
                            .font_weight(FontWeight::SEMIBOLD)
                            .child(toast_msg.clone()),
                    ),
            );
        }

        match self.active_dialog {
            ActiveDialog::None => {}
            ActiveDialog::About => {
                root = root.child(self.render_about_dialog(viewport, aurora, dark, &palette, cx));
            }
            ActiveDialog::ApiKey => {
                root = root.child(self.render_api_key_dialog(aurora, dark, &palette, window, cx));
            }
            ActiveDialog::ModelSelector => {
                root = root
                    .child(self.render_model_selector_dialog(viewport, aurora, dark, &palette, cx));
            }
            ActiveDialog::WhisperSelector => {
                root = root.child(self.render_whisper_dialog(aurora, dark, &palette, cx));
            }
        }

        root
    }
}

fn model_grid_layout(count: usize, viewport_width: f32, viewport_height: f32) -> (usize, f32, f32) {
    let max_w = (viewport_width * 0.70).max(340.0);
    let max_h = (viewport_height * 0.80).max(280.0);
    if count <= 4 {
        let cols = if viewport_width < 520.0 || count <= 1 {
            1
        } else {
            2
        };
        let modal_w = max_w.min(480.0);
        (cols, modal_w, max_h)
    } else {
        let cols = if max_w >= 720.0 {
            4
        } else if max_w >= 520.0 {
            3
        } else if max_w >= 380.0 {
            2
        } else {
            1
        };
        (cols, max_w, max_h)
    }
}

fn main() {
    Application::new()
        .with_assets(LyraAssets)
        .run(|cx: &mut App| {
            cx.bind_keys([
                KeyBinding::new("ctrl-space", ToggleRecord, Some("LyraFlowDemo")),
                KeyBinding::new("ctrl-alt-space", CancelRecord, Some("LyraFlowDemo")),
                KeyBinding::new("escape", CancelRecord, Some("LyraFlowDemo")),
                KeyBinding::new("ctrl-t", ToggleMode, Some("LyraFlowDemo")),
                KeyBinding::new("tab", FocusNext, Some("LyraFlowDemo")),
                KeyBinding::new("shift-tab", FocusPrevious, Some("LyraFlowDemo")),
            ]);
            cx.on_window_closed(|cx| {
                if cx.windows().is_empty() {
                    cx.quit();
                }
            })
            .detach();
            let bounds = Bounds::centered(None, size(px(1080.), px(760.)), cx);
            if let Err(error) = cx.open_window(
                WindowOptions {
                    window_bounds: Some(WindowBounds::Windowed(bounds)),
                    window_min_size: Some(size(px(480.), px(520.))),
                    titlebar: Some(TitlebarOptions {
                        title: Some(SharedString::from("LyraFlow")),
                        ..Default::default()
                    }),
                    app_id: Some("com.biglexj.lyraflow".into()),
                    ..Default::default()
                },
                |window, cx| cx.new(|cx| LyraFlowApp::new(window, cx)),
            ) {
                eprintln!("No se pudo abrir LyraFlow GPUI: {error:#}");
                cx.quit();
            }
            cx.activate(true);
        });
}

#[cfg(test)]
mod tests {
    use super::model_grid_layout;

    #[test]
    fn model_selector_uses_4_cols_and_70_by_80_bounds_for_many_models() {
        let (cols, modal_w, max_h) = model_grid_layout(50, 1210.0, 870.0);
        assert_eq!(cols, 4);
        assert!((modal_w - 847.0).abs() < 0.01);
        assert!((max_h - 696.0).abs() < 0.01);
    }

    #[test]
    fn model_selector_uses_2_cols_and_narrower_width_for_few_models() {
        let (cols, modal_w, max_h) = model_grid_layout(4, 1210.0, 870.0);
        assert_eq!(cols, 2);
        assert_eq!(modal_w, 480.0);
        assert!((max_h - 696.0).abs() < 0.01);
    }
}
