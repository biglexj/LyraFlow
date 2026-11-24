//! Descubrimiento dinámico de modelos mediante las APIs de cada proveedor.
//!
//! No introduce listas fijas de modelos para Gemini ni para OpenAI-compatible:
//! los modelos disponibles provienen exclusivamente del escaneo contra la API.

use crate::preferences::AiProvider;
use serde::Deserialize;
use std::time::Duration;

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct DiscoveredModel {
    pub id: String,
    pub display_name: String,
    pub description: String,
}

#[derive(Deserialize)]
struct GeminiModelsListResponse {
    #[serde(default)]
    models: Vec<GeminiModelDto>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct GeminiModelDto {
    name: String,
    #[serde(default)]
    display_name: Option<String>,
    #[serde(default)]
    description: Option<String>,
    #[serde(default)]
    supported_generation_methods: Vec<String>,
}

#[derive(Deserialize)]
struct OpenAiModelsListResponse {
    #[serde(default)]
    data: Vec<OpenAiModelDto>,
}

#[derive(Deserialize)]
struct OpenAiModelDto {
    id: String,
    #[serde(default)]
    owned_by: Option<String>,
}

pub fn discover_models(
    provider: AiProvider,
    api_key: &str,
    endpoint: &str,
) -> Result<Vec<DiscoveredModel>, String> {
    let key = api_key.trim();
    if key.is_empty() {
        return Err("Añade tu clave API para escanear los modelos disponibles.".into());
    }

    match provider {
        AiProvider::Gemini => discover_gemini_models(key),
        AiProvider::OpenAiCompatible => discover_openai_models(key, endpoint),
        AiProvider::MiniMax => Ok(vec![DiscoveredModel {
            id: "asr-1.0".into(),
            display_name: "asr-1.0".into(),
            description: "Transcriptor dedicado de MiniMax".into(),
        }]),
    }
}

fn http_client() -> Result<zed_reqwest::blocking::Client, String> {
    zed_reqwest::blocking::Client::builder()
        .timeout(Duration::from_secs(25))
        .build()
        .map_err(|e| format!("No se pudo inicializar el cliente HTTP: {e}"))
}

fn discover_gemini_models(api_key: &str) -> Result<Vec<DiscoveredModel>, String> {
    let client = http_client()?;
    let url = format!("https://generativelanguage.googleapis.com/v1beta/models?key={api_key}");
    let response = client
        .get(&url)
        .header("Accept", "application/json")
        .send()
        .map_err(|e| format!("Error de red al escanear Gemini: {e}"))?;

    let status = response.status();
    if !status.is_success() {
        return Err(format!(
            "Error al consultar modelos de Gemini (HTTP {}).",
            status.as_u16()
        ));
    }

    let body = response
        .text()
        .map_err(|e| format!("No se pudo leer la respuesta de Gemini: {e}"))?;
    parse_gemini_models_response(&body)
}

pub fn parse_gemini_models_response(json_body: &str) -> Result<Vec<DiscoveredModel>, String> {
    let parsed: GeminiModelsListResponse = serde_json::from_str(json_body)
        .map_err(|e| format!("Respuesta JSON inesperada de Gemini: {e}"))?;

    let mut models: Vec<DiscoveredModel> = parsed
        .models
        .into_iter()
        .filter(|model| {
            let id = model.name.trim_start_matches("models/").to_lowercase();
            let supports_generate = model
                .supported_generation_methods
                .iter()
                .any(|m| m.eq_ignore_ascii_case("generateContent"));
            let is_embedding = id.contains("embedding") || id.contains("aqa");
            let is_pure_image = id.contains("imagen") || id.contains("image-generation");
            let is_legacy_palm = id.contains("bison") || id.contains("gecko");
            supports_generate && !is_embedding && !is_pure_image && !is_legacy_palm
        })
        .map(|model| {
            let clean_id = model
                .name
                .strip_prefix("models/")
                .unwrap_or(&model.name)
                .to_string();
            let display_name = model
                .display_name
                .filter(|d| !d.trim().is_empty())
                .unwrap_or_else(|| clean_id.clone());
            DiscoveredModel {
                id: clean_id,
                display_name,
                description: model.description.unwrap_or_default(),
            }
        })
        .collect();

    models.sort_by(|a, b| {
        let a_flash = a.id.to_lowercase().contains("flash");
        let b_flash = b.id.to_lowercase().contains("flash");
        b_flash.cmp(&a_flash).then_with(|| b.id.cmp(&a.id))
    });

    Ok(models)
}

fn discover_openai_models(
    api_key: &str,
    custom_endpoint: &str,
) -> Result<Vec<DiscoveredModel>, String> {
    let client = http_client()?;
    let target_url = resolve_openai_models_url(custom_endpoint);
    let response = client
        .get(&target_url)
        .header("Authorization", format!("Bearer {api_key}"))
        .header("Accept", "application/json")
        .send()
        .map_err(|e| format!("Error de red al escanear modelos: {e}"))?;

    let status = response.status();
    if !status.is_success() {
        return Err(format!(
            "Error al consultar modelos de OpenAI (HTTP {}).",
            status.as_u16()
        ));
    }

    let body = response
        .text()
        .map_err(|e| format!("No se pudo leer la respuesta de modelos: {e}"))?;
    parse_openai_models_response(&body)
}

pub fn parse_openai_models_response(json_body: &str) -> Result<Vec<DiscoveredModel>, String> {
    let parsed: OpenAiModelsListResponse = serde_json::from_str(json_body)
        .map_err(|e| format!("Respuesta JSON inesperada de modelos: {e}"))?;

    let mut models: Vec<DiscoveredModel> = parsed
        .data
        .into_iter()
        .filter(|model| {
            let id = model.id.to_lowercase();
            let is_embedding = id.contains("embedding");
            let is_image_gen = id.contains("dall-e");
            let is_tts = id.starts_with("tts-");
            let is_whisper = id.starts_with("whisper-");
            let is_moderation = id.contains("moderation");
            let is_legacy = id.contains("babbage") || id.contains("davinci");
            !is_embedding && !is_image_gen && !is_tts && !is_whisper && !is_moderation && !is_legacy
        })
        .map(|model| DiscoveredModel {
            display_name: model.id.clone(),
            description: model
                .owned_by
                .map(|o| format!("Propietario: {o}"))
                .unwrap_or_default(),
            id: model.id,
        })
        .collect();

    models.sort_by(|a, b| b.id.cmp(&a.id));
    Ok(models)
}

pub fn resolve_openai_models_url(custom_endpoint: &str) -> String {
    let trimmed = custom_endpoint.trim();
    if trimmed.is_empty() || trimmed.contains("api.openai.com") {
        return "https://api.openai.com/v1/models".into();
    }
    if let Some(base) = trimmed.strip_suffix("/chat/completions") {
        format!("{base}/models")
    } else if trimmed.ends_with("/models") {
        trimmed.to_string()
    } else {
        format!("{}/models", trimmed.trim_end_matches('/'))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn resolves_openai_models_urls_correctly() {
        assert_eq!(
            resolve_openai_models_url(""),
            "https://api.openai.com/v1/models"
        );
        assert_eq!(
            resolve_openai_models_url("https://api.openai.com/v1/chat/completions"),
            "https://api.openai.com/v1/models"
        );
        assert_eq!(
            resolve_openai_models_url("http://localhost:11434/v1/chat/completions"),
            "http://localhost:11434/v1/models"
        );
        assert_eq!(
            resolve_openai_models_url("https://gateway.example.com/v1/"),
            "https://gateway.example.com/v1/models"
        );
    }

    #[test]
    fn gemini_scanner_filters_non_generation_and_sorts_flash_first() {
        let payload = r#"{
            "models": [
                {"name": "models/embedding-001", "supportedGenerationMethods": ["embedContent"]},
                {"name": "models/imagen-3.0-generate", "supportedGenerationMethods": ["generateContent"]},
                {"name": "models/gemini-2.5-pro", "displayName": "Gemini 2.5 Pro", "supportedGenerationMethods": ["generateContent"]},
                {"name": "models/gemini-2.5-flash", "displayName": "Gemini 2.5 Flash", "supportedGenerationMethods": ["generateContent"]},
                {"name": "models/gemini-3.5-flash", "displayName": "Gemini 3.5 Flash", "supportedGenerationMethods": ["generateContent"]}
            ]
        }"#;
        let models = parse_gemini_models_response(payload).unwrap();
        let ids: Vec<&str> = models.iter().map(|m| m.id.as_str()).collect();
        assert_eq!(
            ids,
            vec!["gemini-3.5-flash", "gemini-2.5-flash", "gemini-2.5-pro"]
        );
    }

    #[test]
    fn openai_scanner_filters_tts_whisper_embeddings() {
        let payload = r#"{
            "data": [
                {"id": "text-embedding-3-large"},
                {"id": "whisper-1"},
                {"id": "tts-1"},
                {"id": "gpt-4o-audio-preview", "owned_by": "openai"},
                {"id": "gpt-5.6-luna", "owned_by": "openai"}
            ]
        }"#;
        let models = parse_openai_models_response(payload).unwrap();
        let ids: Vec<&str> = models.iter().map(|m| m.id.as_str()).collect();
        assert_eq!(ids, vec!["gpt-5.6-luna", "gpt-4o-audio-preview"]);
    }
}
