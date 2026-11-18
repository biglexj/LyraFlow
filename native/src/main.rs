#![cfg_attr(target_os = "windows", windows_subsystem = "windows")]

mod about;
mod audio;
mod preferences;
mod theme;

use audio::{Capture, MAX_SECONDS, Recording};
use gpui::{
    App, Application, Bounds, ClickEvent, ClipboardItem, Context, FocusHandle, KeyBinding,
    SharedString, Subscription, Timer, TitlebarOptions, Window, WindowAppearance, WindowBounds,
    WindowOptions, actions, div, prelude::*, px, relative, rgb, rgba, size,
};
use std::{sync::Arc, time::Duration};
use theme::{Appearance, Palette, ThemeMode, ThemeName};

actions!(
    lyraflow,
    [
        ToggleRecord,
        CancelRecord,
        ToggleMode,
        CopyPath,
        OpenAbout,
        CloseAbout,
        UseAurora,
        UseClassic,
        FocusNext,
        FocusPrevious
    ]
);

/// Identificadores de los controles que reciben foco por teclado.
struct FocusControls {
    record: FocusHandle,
    cancel: FocusHandle,
    theme_aurora: FocusHandle,
    theme_classic: FocusHandle,
    mode: FocusHandle,
    about: FocusHandle,
    about_donate: FocusHandle,
    about_coffee: FocusHandle,
    about_feedback: FocusHandle,
    about_github: FocusHandle,
    about_close: FocusHandle,
    copy_path: FocusHandle,
}

impl FocusControls {
    fn new(cx: &mut Context<Demo>) -> Self {
        // Cada control con foco propio debe ser un punto de tabulación alcanzable
        // con el teclado.
        let handle = |cx: &mut Context<Demo>| cx.focus_handle().tab_stop(true);
        Self {
            record: handle(cx),
            cancel: handle(cx),
            theme_aurora: handle(cx),
            theme_classic: handle(cx),
            mode: handle(cx),
            about: handle(cx),
            about_donate: handle(cx),
            about_coffee: handle(cx),
            about_feedback: handle(cx),
            about_github: handle(cx),
            about_close: handle(cx),
            copy_path: handle(cx),
        }
    }
}

struct Demo {
    icon: Arc<gpui::Image>,
    focus: FocusHandle,
    controls: FocusControls,
    appearance: Appearance,
    system_dark: bool,
    about_open: bool,
    capture: Option<Capture>,
    recording: Option<Recording>,
    message: String,
    appearance_notice: Option<String>,
    error: bool,
    seconds: f64,
    peak: f32,
    _appearance_observer: Subscription,
}

fn is_dark(appearance: WindowAppearance) -> bool {
    matches!(
        appearance,
        WindowAppearance::Dark | WindowAppearance::VibrantDark
    )
}

/// Rótulo del control de modo: el estado actual, con el vocabulario de la aplicación.
fn mode_button_label(mode: ThemeMode) -> String {
    format!("Tema {}", mode.label().to_lowercase())
}

impl Demo {
    fn new(window: &mut Window, cx: &mut Context<Self>) -> Self {
        let focus = cx.focus_handle();
        focus.focus(window);
        let controls = FocusControls::new(cx);
        let appearance = preferences::load();
        let system_dark = is_dark(window.appearance());
        let appearance_observer = cx.observe_window_appearance(window, |view, window, cx| {
            let dark = is_dark(window.appearance());
            if view.system_dark != dark {
                view.system_dark = dark;
                cx.notify();
            }
        });
        cx.spawn(async move |view, cx| {
            loop {
                Timer::after(Duration::from_millis(80)).await;
                if view.update(cx, |view, cx| view.tick(cx)).is_err() {
                    break;
                }
            }
        })
        .detach();
        Self {
            icon: Arc::new(gpui::Image::from_bytes(
                gpui::ImageFormat::Png,
                include_bytes!("../../assets/branding/icons/icon-transparent.png").to_vec(),
            )),
            focus,
            controls,
            appearance,
            system_dark,
            about_open: false,
            capture: None,
            recording: None,
            message:
                "Pulsa Grabar para probar el micrófono. El audio se guarda solo en este equipo."
                    .into(),
            appearance_notice: None,
            error: false,
            seconds: 0.0,
            peak: 0.0,
            _appearance_observer: appearance_observer,
        }
    }

    fn tick(&mut self, cx: &mut Context<Self>) {
        let Some(capture) = &self.capture else { return };
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
            self.message = error;
            self.error = true;
            self.peak = 0.0;
        } else if seconds >= f64::from(MAX_SECONDS) {
            self.stop();
        }
        cx.notify();
    }

    fn toggle_record(&mut self, _: &ToggleRecord, _: &mut Window, cx: &mut Context<Self>) {
        if self.capture.is_some() {
            self.stop();
        } else {
            match Capture::start() {
                Ok(capture) => {
                    self.message = format!("Grabando desde {}. Máximo 5 minutos.", capture.device);
                    self.capture = Some(capture);
                    self.seconds = 0.0;
                    self.peak = 0.0;
                    self.error = false;
                }
                Err(error) => {
                    self.message = format!("{error:#}");
                    self.error = true;
                }
            }
        }
        cx.notify();
    }

    fn stop(&mut self) {
        let Some(capture) = self.capture.take() else {
            return;
        };
        self.peak = 0.0;
        match capture.finish() {
            Ok(recording) => {
                self.seconds = recording.seconds;
                self.message =
                    "Audio guardado. La transcripción todavía no está conectada en esta demo."
                        .into();
                self.recording = Some(recording);
                self.error = false;
            }
            Err(error) => {
                self.message = format!("{error:#}");
                self.error = true;
            }
        }
    }

    fn cancel(&mut self, _: &CancelRecord, _: &mut Window, cx: &mut Context<Self>) {
        if self.about_open {
            self.about_open = false;
            cx.notify();
            return;
        }
        if let Some(capture) = self.capture.take() {
            self.seconds = 0.0;
            self.peak = 0.0;
            match capture.cancel() {
                Ok(()) => {
                    self.message = "Grabación cancelada. Se eliminó el audio de esta toma.".into();
                    self.error = false;
                }
                Err(error) => {
                    self.message = format!("{error:#}");
                    self.error = true;
                }
            }
            cx.notify();
        }
    }

    fn apply_appearance(&mut self, appearance: Appearance, cx: &mut Context<Self>) {
        self.appearance = appearance;
        self.appearance_notice = match preferences::save(appearance) {
            Ok(()) => None,
            Err(error) => Some(error),
        };
        cx.notify();
    }

    fn set_theme(&mut self, theme: ThemeName, cx: &mut Context<Self>) {
        self.apply_appearance(
            Appearance {
                theme,
                mode: self.appearance.mode,
            },
            cx,
        );
    }

    fn set_mode(&mut self, mode: ThemeMode, cx: &mut Context<Self>) {
        self.apply_appearance(
            Appearance {
                theme: self.appearance.theme,
                mode,
            },
            cx,
        );
    }

    fn toggle_mode(&mut self, _: &ToggleMode, _: &mut Window, cx: &mut Context<Self>) {
        let dark = self.appearance.resolved_dark(self.system_dark);
        let mode = if dark {
            ThemeMode::Light
        } else {
            ThemeMode::Dark
        };
        self.set_mode(mode, cx);
    }

    /// Recorre los modos como lo hace la aplicación de Compose: automático, claro y oscuro.
    fn cycle_mode(&mut self, cx: &mut Context<Self>) {
        let mode = match self.appearance.mode {
            ThemeMode::System => ThemeMode::Light,
            ThemeMode::Light => ThemeMode::Dark,
            ThemeMode::Dark => ThemeMode::System,
        };
        self.set_mode(mode, cx);
    }

    fn open_about(&mut self, _: &OpenAbout, _: &mut Window, cx: &mut Context<Self>) {
        self.about_open = true;
        cx.notify();
    }

    fn close_about(&mut self, _: &CloseAbout, _: &mut Window, cx: &mut Context<Self>) {
        self.about_open = false;
        cx.notify();
    }

    fn copy_path(&mut self, _: &CopyPath, _: &mut Window, cx: &mut Context<Self>) {
        if let Some(recording) = &self.recording {
            cx.write_to_clipboard(ClipboardItem::new_string(
                recording.path.display().to_string(),
            ));
            self.message = "Ruta del último audio copiada al portapapeles.".into();
            self.error = false;
            cx.notify();
        }
    }
}

/// Botón secundario: superficie neutra, texto legible y borde de control.
fn secondary_button(
    id: &'static str,
    label: impl Into<SharedString>,
    palette: &Palette,
    focus: &FocusHandle,
    listener: impl Fn(&ClickEvent, &mut Window, &mut App) + 'static,
) -> impl IntoElement {
    div()
        .id(id)
        .cursor_pointer()
        .px_4()
        .py_2()
        .rounded_lg()
        .border_1()
        .border_color(rgb(palette.control_border))
        .bg(rgb(palette.surface))
        .text_color(rgb(palette.foreground))
        .font_weight(gpui::FontWeight::SEMIBOLD)
        .track_focus(focus)
        .tab_stop(true)
        .hover(|style| style.bg(rgb(palette.surface_hover)))
        .active(|style| style.bg(rgb(palette.surface_pressed)))
        .focus(|style| style.border_color(rgb(palette.focus_ring)))
        .on_click(listener)
        .child(label.into())
}

/// Opción de un grupo de selección: la elegida queda marcada con el color de selección.
fn option_button(
    id: &'static str,
    label: &'static str,
    selected: bool,
    palette: &Palette,
    focus: &FocusHandle,
    listener: impl Fn(&ClickEvent, &mut Window, &mut App) + 'static,
) -> impl IntoElement {
    let (background, text, border) = if selected {
        (
            rgb(palette.selection),
            rgb(palette.accent_foreground),
            rgb(palette.accent_foreground),
        )
    } else {
        (
            rgb(palette.surface),
            rgb(palette.foreground),
            rgb(palette.border_subtle),
        )
    };
    div()
        .id(id)
        .cursor_pointer()
        .px_4()
        .py_2()
        .rounded_lg()
        .border_1()
        .border_color(border)
        .bg(background)
        .text_color(text)
        .font_weight(gpui::FontWeight::SEMIBOLD)
        .track_focus(focus)
        .tab_stop(true)
        .hover(|style| style.bg(rgb(palette.surface_hover)))
        .active(|style| style.bg(rgb(palette.surface_pressed)))
        .focus(|style| style.border_color(rgb(palette.focus_ring)))
        .on_click(listener)
        .child(label)
}

/// Enlace del «Acerca de»: texto en el color de acento activo.
fn about_link(
    id: &'static str,
    label: &'static str,
    palette: &Palette,
    focus: &FocusHandle,
    url: &'static str,
    primary: bool,
) -> impl IntoElement {
    let (background, text, border) = if primary {
        (
            rgb(palette.accent),
            rgb(palette.on_accent),
            rgba(0x00000000),
        )
    } else {
        (
            rgba(0x00000000),
            rgb(palette.accent_foreground),
            rgb(palette.control_border),
        )
    };
    div()
        .id(id)
        .cursor_pointer()
        .px_4()
        .py_3()
        .rounded_lg()
        .border_1()
        .border_color(border)
        .bg(background)
        .text_color(text)
        .font_weight(gpui::FontWeight::SEMIBOLD)
        .track_focus(focus)
        .tab_stop(true)
        .hover(|style| {
            if primary {
                style.bg(rgb(palette.accent_hover))
            } else {
                style.bg(rgb(palette.surface_hover))
            }
        })
        .active(|style| {
            if primary {
                style.bg(rgb(palette.accent_pressed))
            } else {
                style.bg(rgb(palette.surface_pressed))
            }
        })
        .focus(|style| style.border_color(rgb(palette.focus_ring)))
        .on_click(move |_, _, cx| cx.open_url(url))
        .child(label)
}

impl Render for Demo {
    fn render(&mut self, _: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        let dark = self.appearance.resolved_dark(self.system_dark);
        let palette = theme::palette(self.appearance.theme, dark);
        let active = self.capture.is_some();
        let status = if active {
            "Grabando"
        } else if self.error {
            "Revisa el micrófono"
        } else {
            "Listo para grabar"
        };
        let status_color = if self.error || active {
            rgb(palette.error)
        } else {
            rgb(palette.accent_foreground)
        };
        let clock = format!(
            "{:02}:{:02}",
            self.seconds as u64 / 60,
            self.seconds as u64 % 60
        );

        let root = div()
            .id("root")
            .size_full()
            .flex()
            .flex_col()
            .p_5()
            .gap_4()
            .overflow_y_scroll()
            .font_family("Segoe UI")
            .text_base()
            .bg(rgb(palette.background))
            .text_color(rgb(palette.foreground))
            .track_focus(&self.focus)
            .key_context("LyraFlowDemo")
            .on_action(cx.listener(Self::toggle_record))
            .on_action(cx.listener(Self::cancel))
            .on_action(cx.listener(Self::toggle_mode))
            .on_action(cx.listener(Self::copy_path))
            .on_action(cx.listener(Self::open_about))
            .on_action(cx.listener(Self::close_about))
            .on_action(cx.listener::<UseAurora>(|view, _, _, cx| {
                view.set_theme(ThemeName::Aurora, cx)
            }))
            .on_action(cx.listener::<UseClassic>(|view, _, _, cx| {
                view.set_theme(ThemeName::Classic, cx)
            }))
            .on_action(cx.listener::<FocusNext>(|_, _, window, _| window.focus_next()))
            .on_action(cx.listener::<FocusPrevious>(|_, _, window, _| window.focus_prev()))
            .child(
                div()
                    .flex()
                    .items_center()
                    .justify_between()
                    .child(
                        div()
                            .flex()
                            .items_center()
                            .gap_3()
                            .child(gpui::img(self.icon.clone()).size_10())
                            .child(
                                div().flex().flex_col().child(
                                    div()
                                        .text_xl()
                                        .font_weight(gpui::FontWeight::SEMIBOLD)
                                        .child(about::APP_NAME),
                                ).child(
                                    div()
                                        .text_xs()
                                        .text_color(rgb(palette.secondary_foreground))
                                        .child("Demo · Rust + GPUI"),
                                ),
                            ),
                    )
                    .child(
                        div()
                            .flex()
                            .items_center()
                            .gap_2()
                            .child(option_button(
                                "theme-aurora",
                                ThemeName::Aurora.label(),
                                self.appearance.theme == ThemeName::Aurora,
                                &palette,
                                &self.controls.theme_aurora,
                                cx.listener(|view, _, _, cx| {
                                    view.set_theme(ThemeName::Aurora, cx)
                                }),
                            ))
                            .child(option_button(
                                "theme-classic",
                                ThemeName::Classic.label(),
                                self.appearance.theme == ThemeName::Classic,
                                &palette,
                                &self.controls.theme_classic,
                                cx.listener(|view, _, _, cx| {
                                    view.set_theme(ThemeName::Classic, cx)
                                }),
                            ))
                            .child(secondary_button(
                                "mode",
                                mode_button_label(self.appearance.mode),
                                &palette,
                                &self.controls.mode,
                                cx.listener(|view, _, _, cx| view.cycle_mode(cx)),
                            ))
                            .child(secondary_button(
                                "about",
                                "Acerca de",
                                &palette,
                                &self.controls.about,
                                cx.listener(|view, _, window, cx| {
                                    view.open_about(&OpenAbout, window, cx)
                                }),
                            )),
                    ),
            )
            .child(
                div()
                    .flex()
                    .flex_col()
                    .gap_1()
                    .child(
                        div()
                            .text_3xl()
                            .font_weight(gpui::FontWeight::SEMIBOLD)
                            .child("Tu voz, ya bien escrita"),
                    )
                    .child(
                        div()
                            .text_sm()
                            .text_color(rgb(palette.secondary_foreground))
                            .child("Primera prueba nativa: captura de audio local."),
                    ),
            )
            .child(
                div()
                    .flex()
                    .flex_col()
                    .gap_4()
                    .p_6()
                    .rounded_2xl()
                    .bg(rgb(palette.surface))
                    .child(
                        div()
                            .flex()
                            .items_center()
                            .justify_between()
                            .child(
                                div()
                                    .text_color(status_color)
                                    .font_weight(gpui::FontWeight::SEMIBOLD)
                                    .child(status),
                            )
                            .child(div().text_3xl().child(clock)),
                    )
                    .child(
                        div()
                            .h_2()
                            .w_full()
                            .rounded_full()
                            .bg(rgb(palette.border_subtle))
                            .child(
                                div()
                                    .h_full()
                                    .w(relative(self.peak))
                                    .rounded_full()
                                    .bg(rgb(palette.meter)),
                            ),
                    )
                    .child(
                        div()
                            .flex()
                            .gap_3()
                            .items_center()
                            .child(
                                div()
                                    .id("record")
                                    .cursor_pointer()
                                    .px_6()
                                    .py_3()
                                    .rounded_xl()
                                    .border_1()
                                    .border_color(rgba(0x00000000))
                                    .bg(rgb(palette.accent))
                                    .text_color(rgb(palette.on_accent))
                                    .font_weight(gpui::FontWeight::SEMIBOLD)
                                    .track_focus(&self.controls.record)
                                    .tab_stop(true)
                                    .hover(|style| style.bg(rgb(palette.accent_hover)))
                                    .active(|style| style.bg(rgb(palette.accent_pressed)))
                                    .focus(|style| style.border_color(rgb(palette.focus_ring)))
                                    .on_click(cx.listener(|view, _, window, cx| {
                                        view.toggle_record(&ToggleRecord, window, cx)
                                    }))
                                    .child(if active {
                                        "Detener y guardar"
                                    } else {
                                        "Grabar"
                                    }),
                            )
                            .when(active, |row| {
                                row.child(
                                    div()
                                        .id("cancel")
                                        .cursor_pointer()
                                        .px_4()
                                        .py_3()
                                        .rounded_xl()
                                        .border_1()
                                        .border_color(rgb(palette.control_border))
                                        .bg(rgb(palette.surface))
                                        .text_color(rgb(palette.foreground))
                                        .font_weight(gpui::FontWeight::SEMIBOLD)
                                        .track_focus(&self.controls.cancel)
                                        .tab_stop(true)
                                        .hover(|style| style.bg(rgb(palette.surface_hover)))
                                        .active(|style| style.bg(rgb(palette.surface_pressed)))
                                        .focus(|style| {
                                            style.border_color(rgb(palette.focus_ring))
                                        })
                                        .on_click(cx.listener(|view, _, window, cx| {
                                            view.cancel(&CancelRecord, window, cx)
                                        }))
                                        .child("Cancelar"),
                                )
                            })
                            .child(
                                div()
                                    .text_sm()
                                    .text_color(rgb(palette.secondary_foreground))
                                    .child("Ctrl + Espacio"),
                            ),
                    )
                    .child(
                        div()
                            .min_h_10()
                            .text_sm()
                            .text_color(rgb(if self.error {
                                palette.error
                            } else {
                                palette.secondary_foreground
                            }))
                            .child(self.message.clone()),
                    ),
            )
            .child(
                div()
                    .flex()
                    .flex_col()
                    .gap_2()
                    .child(
                        div()
                            .font_weight(gpui::FontWeight::SEMIBOLD)
                            .child("Último audio"),
                    )
                    .children(self.recording.as_ref().map(|recording| {
                        div()
                            .flex()
                            .flex_col()
                            .gap_2()
                            .child(
                                div()
                                    .text_sm()
                                    .text_color(rgb(palette.secondary_foreground))
                                    .child(format!(
                                        "{:.1} s · WAV PCM 16 bits · {} Hz · {} canal(es)",
                                        recording.seconds, recording.sample_rate, recording.channels
                                    )),
                            )
                            .child(
                                div()
                                    .text_sm()
                                    .truncate()
                                    .text_color(rgb(palette.secondary_foreground))
                                    .child(recording.path.display().to_string()),
                            )
                            .child(
                                div()
                                    .id("copy-path")
                                    .cursor_pointer()
                                    .text_color(rgb(palette.accent_foreground))
                                    .track_focus(&self.controls.copy_path)
                                    .tab_stop(true)
                                    .focus(|style| {
                                        style.text_color(rgb(palette.focus_ring))
                                    })
                                    .on_click(cx.listener(|view, _, window, cx| {
                                        view.copy_path(&CopyPath, window, cx)
                                    }))
                                    .child("Copiar ruta del audio · Ctrl + Mayús + C"),
                            )
                    }))
                    .when(self.recording.is_none(), |column| {
                        column.child(
                            div()
                                .text_sm()
                                .text_color(rgb(palette.secondary_foreground))
                                .child("Todavía no has guardado ninguna grabación."),
                        )
                    }),
            )
            .child(div().flex_1())
            .child(
                div()
                    .text_xs()
                    .text_color(rgb(palette.secondary_foreground))
                    .flex()
                    .flex_col()
                    .gap_1()
                    .child("Pendiente de migrar: transcripción, Whisper, historial, ajustes e inserción de texto.")
                    .child("Atajos solo con esta ventana activa: Ctrl + Espacio · Esc para cancelar · Ctrl + T para el tema.")
                    .when(self.appearance_notice.is_some(), |column| {
                        column.child(
                            div()
                                .text_xs()
                                .text_color(rgb(palette.error))
                                .child(self.appearance_notice.clone().unwrap_or_default()),
                        )
                    }),
            );

        if !self.about_open {
            return root;
        }

        let about_card = div()
            .id("about-scrim")
            .absolute()
            .inset_0()
            .flex()
            .items_center()
            .justify_center()
            .bg(rgba(palette.scrim))
            .on_click(cx.listener(|view, _, window, cx| {
                view.close_about(&CloseAbout, window, cx)
            }))
            .child(
                div()
                    .w(px(560.))
                    .flex()
                    .flex_col()
                    .gap_4()
                    .p_6()
                    .rounded_2xl()
                    .bg(rgb(palette.surface_raised))
                    .border_1()
                    .border_color(rgb(palette.border_subtle))
                    .child(
                        div()
                            .flex()
                            .items_center()
                            .gap_4()
                            .child(gpui::img(self.icon.clone()).size_10())
                            .child(
                                div()
                                    .flex()
                                    .flex_col()
                                    .gap_1()
                                    .child(
                                        div()
                                            .text_xl()
                                            .font_weight(gpui::FontWeight::SEMIBOLD)
                                            .child(about::APP_NAME),
                                    )
                                    .child(
                                        div()
                                            .text_sm()
                                            .text_color(rgb(palette.accent_foreground))
                                            .child(format!(
                                                "v{} · {} License",
                                                about::VERSION,
                                                about::LICENSE
                                            )),
                                    )
                                    .child(
                                        div()
                                            .text_sm()
                                            .text_color(rgb(palette.secondary_foreground))
                                            .child(about::TAGLINE),
                                    ),
                            ),
                    )
                    .child(
                        div()
                            .text_sm()
                            .text_color(rgb(palette.foreground))
                            .child(about::DESCRIPTION),
                    )
                    .child(
                        div()
                            .text_sm()
                            .text_color(rgb(palette.secondary_foreground))
                            .child(about::SCOPE_NOTE),
                    )
                    .child(
                        div()
                            .flex()
                            .flex_col()
                            .gap_2()
                            .child(
                                div()
                                    .text_sm()
                                    .font_weight(gpui::FontWeight::SEMIBOLD)
                                    .child("Agradecimiento y apoyo"),
                            )
                            .child(about_link(
                                "about-donate",
                                about::SUPPORT_LINKS[0].label,
                                &palette,
                                &self.controls.about_donate,
                                about::SUPPORT_LINKS[0].url,
                                true,
                            ))
                            .child(about_link(
                                "about-coffee",
                                about::SUPPORT_LINKS[1].label,
                                &palette,
                                &self.controls.about_coffee,
                                about::SUPPORT_LINKS[1].url,
                                false,
                            )),
                    )
                    .child(
                        div()
                            .flex()
                            .flex_col()
                            .gap_2()
                            .child(
                                div()
                                    .text_sm()
                                    .font_weight(gpui::FontWeight::SEMIBOLD)
                                    .child("Ayuda y créditos"),
                            )
                            .child(about_link(
                                "about-feedback",
                                about::CREDIT_LINKS[0].label,
                                &palette,
                                &self.controls.about_feedback,
                                about::CREDIT_LINKS[0].url,
                                false,
                            ))
                            .child(about_link(
                                "about-github",
                                about::CREDIT_LINKS[1].label,
                                &palette,
                                &self.controls.about_github,
                                about::CREDIT_LINKS[1].url,
                                false,
                            ))
                            .child(
                                div()
                                    .text_sm()
                                    .text_color(rgb(palette.secondary_foreground))
                                    .child(format!(
                                        "Licencia {} · Autor {} · Paleta Aurora {}",
                                        about::LICENSE,
                                        about::AUTHOR,
                                        theme::palette_version()
                                    )),
                            ),
                    )
                    .child(
                        div()
                            .text_sm()
                            .text_color(rgb(palette.secondary_foreground))
                            .child(about::THANKS),
                    )
                    .child(
                        div()
                            .id("about-close")
                            .cursor_pointer()
                            .px_4()
                            .py_3()
                            .rounded_lg()
                            .border_1()
                            .border_color(rgb(palette.control_border))
                            .bg(rgb(palette.surface))
                            .text_color(rgb(palette.foreground))
                            .font_weight(gpui::FontWeight::SEMIBOLD)
                            .track_focus(&self.controls.about_close)
                            .tab_stop(true)
                            .hover(|style| style.bg(rgb(palette.surface_hover)))
                            .active(|style| style.bg(rgb(palette.surface_pressed)))
                            .focus(|style| style.border_color(rgb(palette.focus_ring)))
                            .on_click(cx.listener(|view, _, window, cx| {
                                view.close_about(&CloseAbout, window, cx)
                            }))
                            .child("Cerrar"),
                    ),
            );

        root.child(about_card)
    }
}

fn main() {
    Application::new().run(|cx: &mut App| {
        cx.bind_keys([
            KeyBinding::new("ctrl-space", ToggleRecord, Some("LyraFlowDemo")),
            KeyBinding::new("ctrl-alt-space", CancelRecord, Some("LyraFlowDemo")),
            KeyBinding::new("escape", CancelRecord, Some("LyraFlowDemo")),
            KeyBinding::new("ctrl-t", ToggleMode, Some("LyraFlowDemo")),
            KeyBinding::new("ctrl-shift-c", CopyPath, Some("LyraFlowDemo")),
            KeyBinding::new("tab", FocusNext, Some("LyraFlowDemo")),
            KeyBinding::new("shift-tab", FocusPrevious, Some("LyraFlowDemo")),
        ]);
        cx.on_window_closed(|cx| {
            if cx.windows().is_empty() {
                cx.quit();
            }
        })
        .detach();
        let bounds = Bounds::centered(None, size(px(900.), px(780.)), cx);
        if let Err(error) = cx.open_window(
            WindowOptions {
                window_bounds: Some(WindowBounds::Windowed(bounds)),
                window_min_size: Some(size(px(760.), px(700.))),
                titlebar: Some(TitlebarOptions {
                    title: Some(SharedString::from("LyraFlow — Demo GPUI")),
                    ..Default::default()
                }),
                app_id: Some("com.biglexj.lyraflow.gpui-demo".into()),
                ..Default::default()
            },
            |window, cx| cx.new(|cx| Demo::new(window, cx)),
        ) {
            eprintln!("No se pudo abrir LyraFlow GPUI: {error:#}");
            cx.quit();
        }
        cx.activate(true);
    });
}
