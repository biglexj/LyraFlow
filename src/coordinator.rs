//! Coordinador de estado de dictado, reintentos, cuota agotada y respaldo con Whisper.

use crate::preferences::{AiProvider, WhisperLanguage};
use crate::providers::{
    self, ProviderError, TranscriptionRequest, TranscriptionResult, WhisperModel,
};
use std::{
    sync::{
        Arc,
        atomic::{AtomicBool, Ordering},
        mpsc,
    },
    time::{Duration, Instant},
};

const COOLDOWN_SECONDS: u64 = 60;

#[derive(Clone, Debug, PartialEq, Eq)]
pub enum DictationState {
    Idle,
    Listening,
    Transcribing {
        model: String,
        attempt: usize,
        max_attempts: usize,
    },
    AttemptFailed {
        attempt: usize,
        max_attempts: usize,
    },
    Completed {
        raw_text: String,
        refined_text: String,
        provider: String,
        elapsed_millis: u64,
    },
    Failed(String),
}

impl DictationState {
    pub fn is_listening(&self) -> bool {
        matches!(self, DictationState::Listening)
    }

    pub fn is_attempt_failed(&self) -> bool {
        matches!(self, DictationState::AttemptFailed { .. })
    }

    pub fn is_processing(&self) -> bool {
        matches!(
            self,
            DictationState::Transcribing { .. } | DictationState::AttemptFailed { .. }
        )
    }
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct QuotaExhaustion {
    pub provider: AiProvider,
    pub model: String,
    pub message: String,
    pub recorded_at: Instant,
}

impl QuotaExhaustion {
    pub fn source(&self) -> String {
        if self.model.trim().is_empty() {
            self.provider.label().to_string()
        } else {
            format!("{} ({})", self.provider.label(), self.model)
        }
    }

    pub fn detail(&self) -> String {
        let clean = self.message.trim_start_matches("⚠️").trim();
        if clean.is_empty() {
            "Espera un momento o cambia de modelo.".into()
        } else {
            clean.to_string()
        }
    }

    pub fn is_active_for(&self, provider: AiProvider, model: &str) -> bool {
        self.provider == provider
            && self.model == model
            && self.recorded_at.elapsed() < Duration::from_secs(COOLDOWN_SECONDS)
    }
}

#[derive(Clone, Debug)]
pub enum CoordinatorEvent {
    State(DictationState),
    Quota(QuotaExhaustion),
    Done {
        result: TranscriptionResult,
        auto_inject: bool,
    },
}

#[derive(Clone)]
pub struct TranscriptionJobConfig {
    pub request: TranscriptionRequest,
    pub api_key: String,
    pub endpoint: String,
    pub provider_enabled: bool,
    pub whisper_enabled: bool,
    pub whisper_available: bool,
    pub whisper_model: Option<WhisperModel>,
    pub whisper_language: WhisperLanguage,
    pub whisper_llm_refinement: bool,
    pub gemini_key: String,
    pub auto_inject: bool,
    pub active_quota: Option<QuotaExhaustion>,
    pub force_whisper_only: bool,
}

pub fn spawn_transcription(
    config: TranscriptionJobConfig,
    cancel_flag: Arc<AtomicBool>,
    sender: mpsc::Sender<CoordinatorEvent>,
) {
    std::thread::Builder::new()
        .name("lyraflow-transcribe".into())
        .spawn(move || {
            run_transcription(config, &cancel_flag, &sender);
        })
        .ok();
}

fn run_transcription(
    config: TranscriptionJobConfig,
    cancel_flag: &AtomicBool,
    sender: &mpsc::Sender<CoordinatorEvent>,
) {
    let send_state = |state: DictationState| {
        if !cancel_flag.load(Ordering::Relaxed) {
            let _ = sender.send(CoordinatorEvent::State(state));
        }
    };

    let whisper_ready = config.whisper_enabled && config.whisper_available;

    if config.force_whisper_only {
        if !whisper_ready {
            send_state(DictationState::Failed(
                "⚠️ Whisper local está activado pero el modelo aún no se ha instalado.".into(),
            ));
            return;
        }
        run_whisper_fallback(&config, cancel_flag, sender, false, 1, 1, None);
        return;
    }

    if !config.provider_enabled {
        if whisper_ready {
            run_whisper_fallback(&config, cancel_flag, sender, false, 1, 1, None);
        } else if config.whisper_enabled {
            send_state(DictationState::Failed(
                "⚠️ Whisper local está activado pero el modelo aún no se ha instalado.".into(),
            ));
        } else {
            send_state(DictationState::Failed(
                "⚠️ Tanto el dictado en la nube como Whisper local están desactivados.".into(),
            ));
        }
        return;
    }

    if let Some(quota) = &config.active_quota
        && quota.is_active_for(config.request.provider, &config.request.model)
    {
        if whisper_ready {
            run_whisper_fallback(
                &config,
                cancel_flag,
                sender,
                true,
                1,
                1,
                Some(quota.source()),
            );
        } else {
            send_state(DictationState::Failed(format!(
                "⚠️ Cuota de {} agotada. {}",
                quota.source(),
                quota.detail()
            )));
        }
        return;
    }

    let max_provider_attempts = 2usize;
    let total_attempts = if whisper_ready { 3 } else { 2 };
    let model_name = if config.request.model.trim().is_empty() {
        "Modelo seleccionado".to_string()
    } else {
        config.request.model.clone()
    };

    let mut last_error = String::from("Error desconocido");

    for attempt in 1..=max_provider_attempts {
        if cancel_flag.load(Ordering::Relaxed) {
            return;
        }
        send_state(DictationState::Transcribing {
            model: model_name.clone(),
            attempt,
            max_attempts: total_attempts,
        });

        match providers::transcribe_cloud(&config.api_key, &config.endpoint, &config.request) {
            Ok(result) => {
                if !cancel_flag.load(Ordering::Relaxed) {
                    let _ = sender.send(CoordinatorEvent::Done {
                        result,
                        auto_inject: config.auto_inject,
                    });
                }
                return;
            }
            Err(ProviderError::QuotaExhausted(msg)) => {
                let quota = QuotaExhaustion {
                    provider: config.request.provider,
                    model: config.request.model.clone(),
                    message: msg,
                    recorded_at: Instant::now(),
                };
                if !cancel_flag.load(Ordering::Relaxed) {
                    let _ = sender.send(CoordinatorEvent::Quota(quota.clone()));
                }
                if whisper_ready {
                    run_whisper_fallback(
                        &config,
                        cancel_flag,
                        sender,
                        false,
                        1,
                        1,
                        Some(quota.source()),
                    );
                } else {
                    send_state(DictationState::Failed(format!(
                        "⚠️ Cuota de {} agotada. {}",
                        quota.source(),
                        quota.detail()
                    )));
                }
                return;
            }
            Err(ProviderError::Other(msg)) => {
                last_error = msg;
                send_state(DictationState::AttemptFailed {
                    attempt,
                    max_attempts: total_attempts,
                });
                if attempt < max_provider_attempts {
                    for _ in 0..20 {
                        if cancel_flag.load(Ordering::Relaxed) {
                            return;
                        }
                        std::thread::sleep(Duration::from_millis(100));
                    }
                }
            }
        }
    }

    if whisper_ready {
        run_whisper_fallback(&config, cancel_flag, sender, false, 3, 3, None);
        return;
    }

    send_state(DictationState::Failed(last_error));
}

fn run_whisper_fallback(
    config: &TranscriptionJobConfig,
    cancel_flag: &AtomicBool,
    sender: &mpsc::Sender<CoordinatorEvent>,
    autonomous: bool,
    attempt: usize,
    total_attempts: usize,
    quota_source: Option<String>,
) {
    if cancel_flag.load(Ordering::Relaxed) {
        return;
    }
    let label = if autonomous {
        "Whisper local (Autónomo)"
    } else {
        "Whisper local"
    };
    let _ = sender.send(CoordinatorEvent::State(DictationState::Transcribing {
        model: label.into(),
        attempt,
        max_attempts: total_attempts,
    }));

    match providers::transcribe_whisper(
        &config.request.audio,
        config.whisper_model,
        config.whisper_language,
        config.whisper_llm_refinement,
        &config.gemini_key,
        &config.request.model,
        &config.request.system_prompt,
    ) {
        Ok(result) => {
            if !cancel_flag.load(Ordering::Relaxed) {
                let _ = sender.send(CoordinatorEvent::Done {
                    result,
                    auto_inject: config.auto_inject,
                });
            }
        }
        Err(err) => {
            if !cancel_flag.load(Ordering::Relaxed) {
                let msg = if let Some(source) = quota_source {
                    format!(
                        "⚠️ Cuota de {source} agotada y Whisper local falló: {}",
                        err.message()
                    )
                } else {
                    format!(
                        "Error en {} y Whisper local: {}",
                        config.request.provider.label(),
                        err.message()
                    )
                };
                let _ = sender.send(CoordinatorEvent::State(DictationState::Failed(msg)));
            }
        }
    }
}
