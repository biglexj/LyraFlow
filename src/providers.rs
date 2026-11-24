//! Proveedores de transcripción en la nube (Gemini, OpenAI-compatible, MiniMax)
//! y motor local Whisper por sidecar.

use crate::preferences::{AiProvider, WhisperLanguage};
use base64::{Engine as _, engine::general_purpose::STANDARD as BASE64};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::{
    fs,
    path::{Path, PathBuf},
    process::Command,
    sync::{Arc, Mutex},
    time::{Duration, Instant, SystemTime, UNIX_EPOCH},
};

pub const FALLBACK_TRANSCRIPTION_PROMPT: &str = "Convierte este dictado en texto final, claro y bien escrito, conservando fielmente su intención. \
     Preserva de forma íntegra el idioma original expresado por el usuario (incluyendo español, inglés, chino, japonés o cualquier idioma y caracteres CJK o símbolos), sin traducir ni omitir caracteres o uniones. \
     Corrige puntuación, concordancia, repeticiones involuntarias y falsos comienzos. \
     Ordena las ideas y crea párrafos o listas cuando el hablante enumere elementos. \
     Interpreta órdenes de formato habladas según el contexto, por ejemplo 'punto', 'coma', \
     'nueva línea' y 'punto por punto', sin escribir literalmente esas órdenes. \
     No resumas, no inventes información y no cambies nombres, cifras, rutas ni fragmentos de código. \
     Devuelve únicamente el texto final, sin comentarios, comillas ni bloques Markdown. \
     Transcribe con fidelidad la voz del hablante, resolviendo con naturalidad titubeos o ruido de fondo leve. Si no existe voz audible en absoluto, devuelve únicamente una cadena vacía.";

#[derive(Clone, Debug)]
pub struct TranscriptionRequest {
    pub audio: Vec<u8>,
    pub mime_type: String,
    pub model: String,
    pub system_prompt: String,
    pub provider: AiProvider,
}

#[derive(Clone, Debug)]
pub struct TranscriptionResult {
    pub raw_text: String,
    pub provider: String,
    #[allow(dead_code)]
    pub model: String,
    pub elapsed_millis: u64,
}

#[derive(Clone, Debug)]
pub enum ProviderError {
    QuotaExhausted(String),
    Other(String),
}

impl ProviderError {
    pub fn message(&self) -> &str {
        match self {
            ProviderError::QuotaExhausted(msg) | ProviderError::Other(msg) => msg,
        }
    }
}

fn http_client(timeout_secs: u64) -> Result<zed_reqwest::blocking::Client, ProviderError> {
    zed_reqwest::blocking::Client::builder()
        .timeout(Duration::from_secs(timeout_secs))
        .build()
        .map_err(|e| ProviderError::Other(format!("No se pudo crear el cliente HTTP: {e}")))
}

// ============================================================================
// Gemini
// ============================================================================

#[derive(Serialize)]
struct GeminiRequest<'a> {
    contents: Vec<GeminiContent<'a>>,
}

#[derive(Serialize)]
struct GeminiContent<'a> {
    parts: Vec<GeminiPart<'a>>,
}

#[derive(Serialize)]
struct GeminiPart<'a> {
    #[serde(skip_serializing_if = "Option::is_none")]
    text: Option<&'a str>,
    #[serde(skip_serializing_if = "Option::is_none")]
    inline_data: Option<GeminiInlineData<'a>>,
}

#[derive(Serialize)]
struct GeminiInlineData<'a> {
    mime_type: &'a str,
    data: &'a str,
}

#[derive(Deserialize)]
struct GeminiResponse {
    #[serde(default)]
    candidates: Vec<GeminiCandidate>,
}

#[derive(Deserialize)]
struct GeminiCandidate {
    #[serde(default)]
    content: Option<GeminiResponseContent>,
}

#[derive(Deserialize)]
struct GeminiResponseContent {
    #[serde(default)]
    parts: Vec<GeminiResponsePart>,
}

#[derive(Deserialize)]
struct GeminiResponsePart {
    #[serde(default)]
    text: Option<String>,
}

pub fn transcribe_gemini(
    api_key: &str,
    request: &TranscriptionRequest,
) -> Result<TranscriptionResult, ProviderError> {
    let key = api_key.trim();
    if key.is_empty() {
        return Err(ProviderError::Other(
            "Configura GEMINI_API_KEY para transcribir.".into(),
        ));
    }
    let model = request.model.trim();
    if model.is_empty() {
        return Err(ProviderError::Other(
            "Escanea o configura un modelo de Gemini para transcribir.".into(),
        ));
    }

    let prompt = if request.system_prompt.trim().is_empty() {
        FALLBACK_TRANSCRIPTION_PROMPT
    } else {
        request.system_prompt.trim()
    };

    let encoded_audio = BASE64.encode(&request.audio);
    let body = GeminiRequest {
        contents: vec![GeminiContent {
            parts: vec![
                GeminiPart {
                    text: None,
                    inline_data: Some(GeminiInlineData {
                        mime_type: &request.mime_type,
                        data: &encoded_audio,
                    }),
                },
                GeminiPart {
                    text: Some(prompt),
                    inline_data: None,
                },
            ],
        }],
    };

    let started = Instant::now();
    let url =
        format!("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent");
    let client = http_client(90)?;
    let response = client
        .post(&url)
        .header("x-goog-api-key", key)
        .header("Accept", "application/json")
        .json(&body)
        .send()
        .map_err(|e| ProviderError::Other(format!("Error de red con Gemini: {e}")))?;

    let status = response.status();
    if status.as_u16() == 429 {
        return Err(ProviderError::QuotaExhausted(
            "⚠️ Has alcanzado el límite de uso o cuota de la API de Gemini. Espera un momento o cambia la API Key / modelo."
                .into(),
        ));
    }
    if !status.is_success() {
        let detail = response.text().unwrap_or_default();
        let snippet: String = detail.trim().chars().take(240).collect();
        return Err(ProviderError::Other(if snippet.is_empty() {
            format!("Gemini respondió HTTP {}.", status.as_u16())
        } else {
            format!("Gemini respondió HTTP {}: {snippet}", status.as_u16())
        }));
    }

    let parsed: GeminiResponse = response
        .json()
        .map_err(|e| ProviderError::Other(format!("Respuesta inválida de Gemini: {e}")))?;
    let text = parsed
        .candidates
        .into_iter()
        .next()
        .and_then(|c| c.content)
        .map(|c| {
            c.parts
                .into_iter()
                .filter_map(|p| p.text)
                .collect::<String>()
        })
        .unwrap_or_default()
        .trim()
        .to_string();

    Ok(TranscriptionResult {
        raw_text: text,
        provider: "Gemini".into(),
        model: model.to_string(),
        elapsed_millis: started.elapsed().as_millis() as u64,
    })
}

// ============================================================================
// OpenAI / compatible
// ============================================================================

#[derive(Serialize)]
struct OpenAiChatRequest<'a> {
    model: &'a str,
    messages: Vec<OpenAiMessage<'a>>,
}

#[derive(Serialize)]
struct OpenAiMessage<'a> {
    role: &'a str,
    content: Vec<OpenAiContentPart<'a>>,
}

#[derive(Serialize)]
struct OpenAiContentPart<'a> {
    #[serde(rename = "type")]
    kind: &'a str,
    #[serde(skip_serializing_if = "Option::is_none")]
    text: Option<&'a str>,
    #[serde(skip_serializing_if = "Option::is_none")]
    input_audio: Option<OpenAiInputAudio<'a>>,
}

#[derive(Serialize)]
struct OpenAiInputAudio<'a> {
    data: &'a str,
    format: &'a str,
}

#[derive(Deserialize)]
struct OpenAiChatResponse {
    #[serde(default)]
    choices: Vec<OpenAiChoice>,
}

#[derive(Deserialize)]
struct OpenAiChoice {
    #[serde(default)]
    message: Option<OpenAiResponseMessage>,
}

#[derive(Deserialize)]
struct OpenAiResponseMessage {
    #[serde(default)]
    content: Option<Value>,
}

fn json_value_as_text(value: Option<&Value>) -> String {
    match value {
        Some(Value::String(s)) => s.clone(),
        Some(Value::Array(items)) => items
            .iter()
            .map(|item| json_value_as_text(Some(item)))
            .collect::<String>(),
        Some(Value::Object(map)) => map
            .get("text")
            .or_else(|| map.get("content"))
            .or_else(|| map.get("output_text"))
            .map(|v| json_value_as_text(Some(v)))
            .unwrap_or_default(),
        _ => String::new(),
    }
}

pub fn transcribe_openai_compatible(
    api_key: &str,
    endpoint: &str,
    request: &TranscriptionRequest,
) -> Result<TranscriptionResult, ProviderError> {
    let key = api_key.trim();
    if key.is_empty() {
        return Err(ProviderError::Other(
            "Configura una clave de API para transcribir.".into(),
        ));
    }
    let model = request.model.trim();
    if model.is_empty() {
        return Err(ProviderError::Other(
            "Escanea o configura un modelo de OpenAI para transcribir.".into(),
        ));
    }
    let url = endpoint.trim();
    if url.is_empty() {
        return Err(ProviderError::Other(
            "Configura el endpoint de OpenAI.".into(),
        ));
    }

    let mime_lower = request.mime_type.to_lowercase();
    let format = if mime_lower.contains("wav") {
        "wav"
    } else if mime_lower.contains("mp3") || mime_lower.contains("mpeg") {
        "mp3"
    } else {
        return Err(ProviderError::Other(
            "El protocolo OpenAI solo acepta audio WAV o MP3 en esta versión.".into(),
        ));
    };

    let prompt = if request.system_prompt.trim().is_empty() {
        FALLBACK_TRANSCRIPTION_PROMPT
    } else {
        request.system_prompt.trim()
    };

    let encoded_audio = BASE64.encode(&request.audio);
    let body = OpenAiChatRequest {
        model,
        messages: vec![OpenAiMessage {
            role: "user",
            content: vec![
                OpenAiContentPart {
                    kind: "text",
                    text: Some(prompt),
                    input_audio: None,
                },
                OpenAiContentPart {
                    kind: "input_audio",
                    text: None,
                    input_audio: Some(OpenAiInputAudio {
                        data: &encoded_audio,
                        format,
                    }),
                },
            ],
        }],
    };

    let started = Instant::now();
    let client = http_client(90)?;
    let response = client
        .post(url)
        .header("Authorization", format!("Bearer {key}"))
        .header("Accept", "application/json")
        .json(&body)
        .send()
        .map_err(|e| ProviderError::Other(format!("Error de red en OpenAI: {e}")))?;

    let status = response.status();
    if status.as_u16() == 429 {
        return Err(ProviderError::QuotaExhausted(
            "Has alcanzado el límite de uso o cuota del servicio OpenAI. Revisa tu saldo o intenta más tarde."
                .into(),
        ));
    }
    if !status.is_success() {
        let detail = response.text().unwrap_or_default();
        let snippet: String = detail.trim().chars().take(240).collect();
        return Err(ProviderError::Other(if snippet.is_empty() {
            format!("El endpoint de OpenAI respondió HTTP {}.", status.as_u16())
        } else {
            format!(
                "El endpoint de OpenAI respondió HTTP {}. Detalle: {snippet}",
                status.as_u16()
            )
        }));
    }

    let parsed: OpenAiChatResponse = response
        .json()
        .map_err(|e| ProviderError::Other(format!("Respuesta inválida de OpenAI: {e}")))?;
    let text = parsed
        .choices
        .into_iter()
        .next()
        .and_then(|c| c.message)
        .map(|m| json_value_as_text(m.content.as_ref()))
        .unwrap_or_default()
        .trim()
        .to_string();

    if text.is_empty() {
        return Err(ProviderError::Other(
            "El endpoint no devolvió texto transcrito.".into(),
        ));
    }

    Ok(TranscriptionResult {
        raw_text: text,
        provider: "OpenAI".into(),
        model: model.to_string(),
        elapsed_millis: started.elapsed().as_millis() as u64,
    })
}

// ============================================================================
// MiniMax (speech_to_text + refinamiento posterior)
// ============================================================================

const MINIMAX_DEFAULT_ASR_MODEL: &str = "asr-1.0";
const MINIMAX_REFINEMENT_MODEL: &str = "MiniMax-M3.1-Flash";

#[derive(Deserialize)]
struct MiniMaxSpeechToTextResponse {
    #[serde(default)]
    text: Option<String>,
    #[serde(default)]
    base_resp: Option<MiniMaxBaseResp>,
}

#[derive(Deserialize)]
struct MiniMaxBaseResp {
    #[serde(default)]
    status_code: Option<i64>,
    #[serde(default)]
    status_msg: Option<String>,
}

#[derive(Serialize)]
struct MiniMaxChatRequest<'a> {
    model: &'a str,
    messages: Vec<MiniMaxChatMessage<'a>>,
}

#[derive(Serialize)]
struct MiniMaxChatMessage<'a> {
    role: &'a str,
    content: &'a str,
}

fn normalize_minimax_base_url(raw: &str) -> String {
    let trimmed = raw.trim().trim_end_matches('/');
    if trimmed.is_empty() {
        return String::new();
    }
    trimmed
        .strip_suffix("/speech_to_text")
        .or_else(|| trimmed.strip_suffix("/chat/completions"))
        .unwrap_or(trimmed)
        .to_string()
}

pub fn transcribe_minimax(
    api_key: &str,
    endpoint: &str,
    request: &TranscriptionRequest,
) -> Result<TranscriptionResult, ProviderError> {
    let key = api_key.trim();
    if key.is_empty() {
        return Err(ProviderError::Other(
            "Configura MINIMAX_API_KEY para transcribir.".into(),
        ));
    }
    let model = if request.model.trim().is_empty() {
        MINIMAX_DEFAULT_ASR_MODEL
    } else {
        request.model.trim()
    };
    let base = normalize_minimax_base_url(endpoint);
    if base.is_empty() {
        return Err(ProviderError::Other(
            "Configura el endpoint de MiniMax.".into(),
        ));
    }

    let started = Instant::now();
    let client = http_client(120)?;

    let file_part = zed_reqwest::blocking::multipart::Part::bytes(request.audio.clone())
        .file_name("lyraflow-dictado.wav")
        .mime_str(&request.mime_type)
        .map_err(|e| ProviderError::Other(format!("MIME inválido para MiniMax: {e}")))?;

    let form = zed_reqwest::blocking::multipart::Form::new()
        .text("model", model.to_string())
        .text("response_format", "json")
        .part("file", file_part);

    let response = client
        .post(format!("{base}/speech_to_text"))
        .header("Authorization", format!("Bearer {key}"))
        .header("Accept", "application/json")
        .multipart(form)
        .send()
        .map_err(|e| ProviderError::Other(format!("Error de red con MiniMax: {e}")))?;

    let status = response.status();
    if status.as_u16() == 429 {
        return Err(ProviderError::QuotaExhausted(
            "Has alcanzado el límite de uso o saldo de MiniMax. Revisa tu cuenta en MiniMax o espera un momento."
                .into(),
        ));
    }
    if !status.is_success() {
        let code = status.as_u16();
        let detail = response.text().unwrap_or_default();
        let snippet: String = detail.trim().chars().take(240).collect();
        let hint = match code {
            400 => {
                " Revisa el formato del audio: MiniMax acepta WAV, MP3, FLAC, M4A, AAC, OGG y Opus. Los audios de más de 500 segundos también se rechazan con HTTP 400."
            }
            413 => " El audio supera el máximo de 50 MB por solicitud de MiniMax.",
            _ => "",
        };
        let detail_part = if snippet.is_empty() {
            String::new()
        } else {
            format!(" Detalle: {snippet}")
        };
        return Err(ProviderError::Other(format!(
            "MiniMax respondió HTTP {code} al transcribir.{detail_part}{hint}"
        )));
    }

    let parsed: MiniMaxSpeechToTextResponse = response
        .json()
        .map_err(|e| ProviderError::Other(format!("Respuesta inválida de MiniMax: {e}")))?;
    if let Some(base_resp) = parsed.base_resp
        && base_resp.status_code.unwrap_or(0) != 0
    {
        let reason = base_resp
            .status_msg
            .unwrap_or_else(|| format!("código {}", base_resp.status_code.unwrap_or(0)));
        return Err(ProviderError::Other(format!(
            "MiniMax rechazó la transcripción: {reason}"
        )));
    }

    let raw_text = parsed.text.unwrap_or_default().trim().to_string();
    let prompt = request.system_prompt.trim();
    let refined = if raw_text.is_empty() || prompt.is_empty() {
        None
    } else {
        refine_minimax(&client, &base, key, &raw_text, prompt)
    };

    let (final_text, provider_label) = match refined {
        Some(text) if !text.is_empty() => {
            (text, format!("MiniMax ASR + {MINIMAX_REFINEMENT_MODEL}"))
        }
        _ => (raw_text, "MiniMax ASR".to_string()),
    };

    Ok(TranscriptionResult {
        raw_text: final_text,
        provider: provider_label,
        model: model.to_string(),
        elapsed_millis: started.elapsed().as_millis() as u64,
    })
}

fn refine_minimax(
    client: &zed_reqwest::blocking::Client,
    base: &str,
    key: &str,
    text: &str,
    prompt: &str,
) -> Option<String> {
    let body = MiniMaxChatRequest {
        model: MINIMAX_REFINEMENT_MODEL,
        messages: vec![
            MiniMaxChatMessage {
                role: "system",
                content: prompt,
            },
            MiniMaxChatMessage {
                role: "user",
                content: text,
            },
        ],
    };
    let response = client
        .post(format!("{base}/chat/completions"))
        .header("Authorization", format!("Bearer {key}"))
        .header("Accept", "application/json")
        .json(&body)
        .send()
        .ok()?;
    if !response.status().is_success() {
        return None;
    }
    let parsed: OpenAiChatResponse = response.json().ok()?;
    let refined = parsed
        .choices
        .into_iter()
        .next()
        .and_then(|c| c.message)
        .map(|m| json_value_as_text(m.content.as_ref()))
        .unwrap_or_default()
        .trim()
        .to_string();
    if refined.is_empty() {
        None
    } else {
        Some(refined)
    }
}

pub fn transcribe_cloud(
    api_key: &str,
    endpoint: &str,
    request: &TranscriptionRequest,
) -> Result<TranscriptionResult, ProviderError> {
    match request.provider {
        AiProvider::Gemini => transcribe_gemini(api_key, request),
        AiProvider::OpenAiCompatible => transcribe_openai_compatible(api_key, endpoint, request),
        AiProvider::MiniMax => transcribe_minimax(api_key, endpoint, request),
    }
}

// ============================================================================
// Whisper local (sidecar + instalador + refinamiento opcional con LLM)
// ============================================================================

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum WhisperModel {
    Tiny,
    Base,
    Small,
    Medium,
    Large,
}

impl WhisperModel {
    pub const ALL: &'static [WhisperModel] = &[
        WhisperModel::Tiny,
        WhisperModel::Base,
        WhisperModel::Small,
        WhisperModel::Medium,
        WhisperModel::Large,
    ];

    pub fn label(self) -> &'static str {
        match self {
            WhisperModel::Tiny => "Tiny",
            WhisperModel::Base => "Base",
            WhisperModel::Small => "Small",
            WhisperModel::Medium => "Medium",
            WhisperModel::Large => "Large",
        }
    }

    pub fn file_name(self) -> &'static str {
        match self {
            WhisperModel::Tiny => "ggml-tiny.bin",
            WhisperModel::Base => "ggml-base.bin",
            WhisperModel::Small => "ggml-small.bin",
            WhisperModel::Medium => "ggml-medium.bin",
            WhisperModel::Large => "ggml-large-v3.bin",
        }
    }

    pub fn description(self) -> &'static str {
        match self {
            WhisperModel::Tiny => "Ultra ligero y rápido (~75 MB).",
            WhisperModel::Base => "Dictado estándar rápido (~140 MB).",
            WhisperModel::Small => "Equilibrio entre precisión y velocidad (~460 MB).",
            WhisperModel::Medium => "Alta precisión para vocabulario amplio (~1.5 GB).",
            WhisperModel::Large => "Máxima precisión disponible (~3.1 GB).",
        }
    }
}

#[derive(Clone, Debug)]
pub struct WhisperSetupState {
    pub detail: String,
    pub available: bool,
    pub downloading: bool,
    pub progress: Option<f32>,
    pub model: Option<WhisperModel>,
}

pub fn whisper_root() -> PathBuf {
    if let Ok(local) = std::env::var("LOCALAPPDATA")
        && !local.trim().is_empty()
    {
        return PathBuf::from(local).join("LyraFlow").join("whisper");
    }
    let home = std::env::var("HOME").unwrap_or_else(|_| ".".into());
    PathBuf::from(home)
        .join(".local")
        .join("share")
        .join("lyraflow")
        .join("whisper")
}

pub fn whisper_model_path(model: WhisperModel) -> PathBuf {
    whisper_root().join("models").join(model.file_name())
}

pub fn whisper_executable() -> Option<PathBuf> {
    if let Ok(configured) = std::env::var("LYRAFLOW_WHISPER_BIN")
        && !configured.trim().is_empty()
    {
        let p = PathBuf::from(configured.trim());
        if p.is_file() {
            return Some(p);
        }
    }
    let root = whisper_root();
    if !root.exists() {
        return None;
    }
    let target = if cfg!(target_os = "windows") {
        "whisper-cli.exe"
    } else {
        "whisper-cli"
    };
    find_file_recursive(&root, target)
}

fn find_file_recursive(dir: &Path, target: &str) -> Option<PathBuf> {
    let entries = fs::read_dir(dir).ok()?;
    for entry in entries.flatten() {
        let path = entry.path();
        if path.is_file() && path.file_name().and_then(|n| n.to_str()) == Some(target) {
            return Some(path);
        }
        if path.is_dir()
            && let Some(found) = find_file_recursive(&path, target)
        {
            return Some(found);
        }
    }
    None
}

pub fn current_whisper_state(preferred: Option<WhisperModel>) -> WhisperSetupState {
    let active_model = preferred
        .filter(|m| whisper_model_path(*m).is_file())
        .or_else(|| {
            WhisperModel::ALL
                .iter()
                .copied()
                .find(|m| whisper_model_path(*m).is_file())
        });
    if whisper_executable().is_some()
        && let Some(model) = active_model
    {
        WhisperSetupState {
            detail: format!("Whisper {} listo", model.label()),
            available: true,
            downloading: false,
            progress: None,
            model: Some(model),
        }
    } else {
        WhisperSetupState {
            detail: "Clic para instalar Whisper local".into(),
            available: false,
            downloading: false,
            progress: None,
            model: active_model,
        }
    }
}

pub fn transcribe_whisper(
    audio: &[u8],
    model: Option<WhisperModel>,
    language: WhisperLanguage,
    llm_refinement: bool,
    gemini_key: &str,
    gemini_model: &str,
    system_prompt: &str,
) -> Result<TranscriptionResult, ProviderError> {
    let model = model.ok_or_else(|| {
        ProviderError::Other("No hay un modelo de Whisper seleccionado o instalado.".into())
    })?;
    let model_path = whisper_model_path(model);
    if !model_path.is_file() {
        return Err(ProviderError::Other(format!(
            "El archivo del modelo de Whisper no existe: {}",
            model_path.display()
        )));
    }
    let binary = whisper_executable()
        .ok_or_else(|| ProviderError::Other("No se encontró whisper-cli.".into()))?;

    let ts = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_nanos())
        .unwrap_or(0);
    let temp_dir = std::env::temp_dir();
    let wav_path = temp_dir.join(format!(
        "lyraflow-whisper-in-{}-{ts}.wav",
        std::process::id()
    ));
    let out_prefix = temp_dir.join(format!("lyraflow-whisper-out-{}-{ts}", std::process::id()));
    let txt_path = PathBuf::from(format!("{}.txt", out_prefix.display()));

    fs::write(&wav_path, audio)
        .map_err(|e| ProviderError::Other(format!("No se pudo escribir WAV temporal: {e}")))?;

    let target_lang = if language == WhisperLanguage::Auto {
        "es"
    } else {
        language.code()
    };

    let threads = std::thread::available_parallelism()
        .map(|n| n.get().clamp(2, 8))
        .unwrap_or(4)
        .to_string();

    let started = Instant::now();
    let output = Command::new(&binary)
        .arg("-m")
        .arg(&model_path)
        .arg("-f")
        .arg(&wav_path)
        .arg("-l")
        .arg(target_lang)
        .arg("-nt")
        .arg("-np")
        .arg("-t")
        .arg(&threads)
        .arg("-otxt")
        .arg("-of")
        .arg(&out_prefix)
        .output();

    let _ = fs::remove_file(&wav_path);

    let output =
        output.map_err(|e| ProviderError::Other(format!("Error al ejecutar whisper-cli: {e}")))?;
    if !output.status.success() {
        let _ = fs::remove_file(&txt_path);
        let log = String::from_utf8_lossy(&output.stdout);
        let err = String::from_utf8_lossy(&output.stderr);
        let combined = format!("{log}{err}");
        let tail: String = combined
            .chars()
            .rev()
            .take(500)
            .collect::<Vec<_>>()
            .into_iter()
            .rev()
            .collect();
        return Err(ProviderError::Other(format!("whisper-cli falló: {tail}")));
    }

    let raw_text = fs::read_to_string(&txt_path)
        .unwrap_or_default()
        .trim()
        .to_string();
    let _ = fs::remove_file(&txt_path);

    let mut provider_label = "Whisper local".to_string();
    let mut final_text = raw_text.clone();
    if llm_refinement
        && !raw_text.is_empty()
        && !gemini_key.trim().is_empty()
        && !gemini_model.trim().is_empty()
    {
        if let Some(refined) = refine_text_with_gemini(
            &raw_text,
            system_prompt,
            gemini_key.trim(),
            gemini_model.trim(),
        ) {
            final_text = refined;
            provider_label = "Whisper local + LLM Refiner (Exp)".into();
        }
    }

    Ok(TranscriptionResult {
        raw_text: final_text,
        provider: provider_label,
        model: model.label().to_string(),
        elapsed_millis: started.elapsed().as_millis() as u64,
    })
}

fn refine_text_with_gemini(
    text: &str,
    system_prompt: &str,
    key: &str,
    model_name: &str,
) -> Option<String> {
    let client = http_client(45).ok()?;
    let prompt = format!(
        "{}\n\nTexto transcrito a refinar:\n\"{text}\"",
        system_prompt.trim()
    );
    let body = GeminiRequest {
        contents: vec![GeminiContent {
            parts: vec![GeminiPart {
                text: Some(&prompt),
                inline_data: None,
            }],
        }],
    };
    let url = format!(
        "https://generativelanguage.googleapis.com/v1beta/models/{model_name}:generateContent"
    );
    let response = client
        .post(&url)
        .header("x-goog-api-key", key)
        .header("Accept", "application/json")
        .json(&body)
        .send()
        .ok()?;
    if !response.status().is_success() {
        return None;
    }
    let parsed: GeminiResponse = response.json().ok()?;
    let refined = parsed
        .candidates
        .into_iter()
        .next()
        .and_then(|c| c.content)
        .map(|c| {
            c.parts
                .into_iter()
                .filter_map(|p| p.text)
                .collect::<String>()
        })
        .unwrap_or_default()
        .trim()
        .to_string();
    if refined.is_empty() {
        None
    } else {
        Some(refined)
    }
}

pub fn install_whisper_blocking(model: WhisperModel, shared_state: Arc<Mutex<WhisperSetupState>>) {
    let set_state = |state: WhisperSetupState| {
        if let Ok(mut guard) = shared_state.lock() {
            *guard = state;
        }
    };

    set_state(WhisperSetupState {
        detail: "Preparando Whisper".into(),
        available: false,
        downloading: true,
        progress: Some(0.0),
        model: Some(model),
    });

    if let Err(err) = install_whisper_files(model, &shared_state) {
        set_state(WhisperSetupState {
            detail: format!("Error: {err}"),
            available: false,
            downloading: false,
            progress: None,
            model: Some(model),
        });
        return;
    }

    set_state(current_whisper_state(Some(model)));
}

fn install_whisper_files(
    model: WhisperModel,
    shared_state: &Arc<Mutex<WhisperSetupState>>,
) -> Result<(), String> {
    let root = whisper_root();
    fs::create_dir_all(&root).map_err(|e| format!("No se pudo crear carpeta Whisper: {e}"))?;

    let client = zed_reqwest::blocking::Client::builder()
        .timeout(Duration::from_secs(1800))
        .build()
        .map_err(|e| format!("Error al crear cliente HTTP: {e}"))?;

    let has_exec = whisper_executable().is_some();
    if !has_exec {
        let pkg_url = resolve_windows_whisper_package(&client)?;
        let archive = root.join("whisper-bin-x64.zip.part");
        download_with_progress(
            &client,
            &pkg_url,
            &archive,
            0.0,
            0.35,
            "Descargando Whisper",
            shared_state,
        )?;
        let runtime_dir = root.join("runtime");
        fs::create_dir_all(&runtime_dir)
            .map_err(|e| format!("No se pudo crear carpeta runtime: {e}"))?;
        let status = Command::new("powershell")
            .args([
                "-NoProfile",
                "-Command",
                &format!(
                    "Expand-Archive -LiteralPath '{}' -DestinationPath '{}' -Force",
                    archive.display(),
                    runtime_dir.display()
                ),
            ])
            .status()
            .map_err(|e| format!("No se pudo descomprimir Whisper: {e}"))?;
        let _ = fs::remove_file(&archive);
        if !status.success() {
            return Err("Falló la extracción del paquete de Whisper.".into());
        }
    }

    let model_dest = whisper_model_path(model);
    if !model_dest.is_file() {
        let start = if has_exec { 0.0 } else { 0.35 };
        let url = format!(
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/{}",
            model.file_name()
        );
        download_with_progress(
            &client,
            &url,
            &model_dest,
            start,
            1.0,
            &format!("Descargando modelo {}", model.label()),
            shared_state,
        )?;
    }

    if whisper_executable().is_none() {
        return Err("El paquete no incluyó whisper-cli.exe".into());
    }
    Ok(())
}

fn resolve_windows_whisper_package(
    client: &zed_reqwest::blocking::Client,
) -> Result<String, String> {
    if let Ok(resp) = client
        .get("https://api.github.com/repos/ggml-org/whisper.cpp/releases?per_page=30")
        .header("Accept", "application/vnd.github+json")
        .header("User-Agent", "LyraFlow")
        .send()
        && resp.status().is_success()
        && let Ok(releases) = resp.json::<Vec<Value>>()
    {
        for release in releases {
            if let Some(assets) = release.get("assets").and_then(|a| a.as_array()) {
                for asset in assets {
                    if asset.get("name").and_then(|n| n.as_str()) == Some("whisper-bin-x64.zip")
                        && let Some(url) =
                            asset.get("browser_download_url").and_then(|u| u.as_str())
                        && !url.is_empty()
                    {
                        return Ok(url.to_string());
                    }
                }
            }
        }
    }

    for fallback in [
        "https://github.com/ggml-org/whisper.cpp/releases/download/b5454/whisper-bin-x64.zip",
        "https://github.com/ggml-org/whisper.cpp/releases/download/v1.9.0/whisper-bin-x64.zip",
    ] {
        if let Ok(resp) = client
            .head(fallback)
            .header("User-Agent", "LyraFlow")
            .send()
            && resp.status().as_u16() < 400
        {
            return Ok(fallback.to_string());
        }
    }

    Err("No se encontró el paquete de Whisper para Windows.".into())
}

fn download_with_progress(
    client: &zed_reqwest::blocking::Client,
    url: &str,
    dest: &Path,
    start: f32,
    end: f32,
    label: &str,
    shared_state: &Arc<Mutex<WhisperSetupState>>,
) -> Result<(), String> {
    use std::io::{Read, Write};
    if let Some(parent) = dest.parent() {
        fs::create_dir_all(parent).map_err(|e| format!("No se pudo crear carpeta destino: {e}"))?;
    }
    let mut resp = client
        .get(url)
        .header("User-Agent", "LyraFlow")
        .send()
        .map_err(|e| format!("Error al descargar {label}: {e}"))?;
    if !resp.status().is_success() {
        return Err(format!(
            "Descarga respondió HTTP {}",
            resp.status().as_u16()
        ));
    }
    let total = resp.content_length().unwrap_or(0);
    let tmp = dest.with_extension("part");
    let mut file =
        fs::File::create(&tmp).map_err(|e| format!("No se pudo crear archivo temporal: {e}"))?;
    let mut buf = [0u8; 64 * 1024];
    let mut received = 0u64;
    loop {
        let n = resp
            .read(&mut buf)
            .map_err(|e| format!("Error leyendo descarga: {e}"))?;
        if n == 0 {
            break;
        }
        file.write_all(&buf[..n])
            .map_err(|e| format!("Error escribiendo descarga: {e}"))?;
        received += n as u64;
        let fraction = if total > 0 {
            received as f32 / total as f32
        } else {
            0.0
        };
        if let Ok(mut guard) = shared_state.lock() {
            guard.detail = label.to_string();
            guard.downloading = true;
            guard.progress = Some(start + (end - start) * fraction);
        }
    }
    drop(file);
    if total > 0 && received != total {
        let _ = fs::remove_file(&tmp);
        return Err("La descarga quedó incompleta. Inténtalo de nuevo.".into());
    }
    fs::rename(&tmp, dest).map_err(|e| format!("No se pudo mover archivo descargado: {e}"))
}
