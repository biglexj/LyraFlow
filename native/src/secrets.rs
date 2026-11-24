//! Almacenamiento cifrado de claves API por proveedor (DPAPI en Windows).

use crate::preferences::AiProvider;
use base64::{Engine as _, engine::general_purpose::STANDARD as BASE64};
use serde::{Deserialize, Serialize};
use std::{
    collections::BTreeMap,
    fs,
    path::{Path, PathBuf},
};

#[derive(Default, Serialize, Deserialize)]
struct StoredSecrets {
    keys: BTreeMap<String, String>,
}

fn secrets_path() -> Option<PathBuf> {
    if let Ok(local) = std::env::var("LOCALAPPDATA")
        && !local.trim().is_empty()
    {
        return Some(
            PathBuf::from(local)
                .join("LyraFlow-gpui-demo")
                .join("secrets.json"),
        );
    }
    if let Ok(home) = std::env::var("HOME") {
        return Some(
            PathBuf::from(home)
                .join(".config")
                .join("lyraflow-gpui-demo")
                .join("secrets.json"),
        );
    }
    None
}

pub fn environment_api_key(provider: AiProvider) -> String {
    if let Ok(global) = std::env::var("LYRAFLOW_API_KEY")
        && !global.trim().is_empty()
    {
        return global.trim().to_string();
    }
    std::env::var(provider.api_key_env_var())
        .ok()
        .map(|s| s.trim().to_string())
        .unwrap_or_default()
}

pub fn load_api_key(provider: AiProvider) -> String {
    let stored = secrets_path()
        .map(|path| load_api_key_from(&path, provider))
        .unwrap_or_default();
    if !stored.trim().is_empty() {
        stored
    } else {
        environment_api_key(provider)
    }
}

pub fn save_api_key(provider: AiProvider, value: &str) -> Result<(), String> {
    let path = secrets_path().ok_or("No se encontró la carpeta de secretos del sistema.")?;
    save_api_key_to(&path, provider, value)
}

pub fn load_api_key_from(path: &Path, provider: AiProvider) -> String {
    let Ok(raw) = fs::read_to_string(path) else {
        return String::new();
    };
    let Ok(stored) = serde_json::from_str::<StoredSecrets>(&raw) else {
        return String::new();
    };
    let Some(encoded) = stored.keys.get(provider.id()) else {
        return String::new();
    };
    unprotect_string(encoded).unwrap_or_default()
}

pub fn save_api_key_to(path: &Path, provider: AiProvider, value: &str) -> Result<(), String> {
    let mut stored: StoredSecrets = fs::read_to_string(path)
        .ok()
        .and_then(|raw| serde_json::from_str(&raw).ok())
        .unwrap_or_default();

    let trimmed = value.trim();
    if trimmed.is_empty() {
        stored.keys.remove(provider.id());
    } else {
        let protected = protect_string(trimmed)?;
        stored.keys.insert(provider.id().to_string(), protected);
    }

    if let Some(parent) = path.parent() {
        fs::create_dir_all(parent)
            .map_err(|e| format!("No se pudo crear la carpeta de credenciales: {e}"))?;
    }
    let json = serde_json::to_string_pretty(&stored)
        .map_err(|e| format!("No se pudieron serializar las credenciales: {e}"))?;
    fs::write(path, json).map_err(|e| format!("No se pudieron guardar las credenciales: {e}"))
}

#[cfg(target_os = "windows")]
fn protect_string(value: &str) -> Result<String, String> {
    use windows::Win32::{
        Foundation::{HLOCAL, LocalFree},
        Security::Cryptography::{CRYPT_INTEGER_BLOB, CRYPTPROTECT_UI_FORBIDDEN, CryptProtectData},
    };
    let mut bytes = value.as_bytes().to_vec();
    let input = CRYPT_INTEGER_BLOB {
        cbData: bytes.len() as u32,
        pbData: bytes.as_mut_ptr(),
    };
    let mut output = CRYPT_INTEGER_BLOB::default();
    unsafe {
        CryptProtectData(
            &input,
            None,
            None,
            None,
            None,
            CRYPTPROTECT_UI_FORBIDDEN,
            &mut output,
        )
        .map_err(|e| format!("Error al cifrar la clave con DPAPI: {e}"))?;
        let slice = std::slice::from_raw_parts(output.pbData, output.cbData as usize);
        let encoded = BASE64.encode(slice);
        let _ = LocalFree(Some(HLOCAL(output.pbData as _)));
        Ok(encoded)
    }
}

#[cfg(target_os = "windows")]
fn unprotect_string(encoded: &str) -> Result<String, String> {
    use windows::Win32::{
        Foundation::{HLOCAL, LocalFree},
        Security::Cryptography::{
            CRYPT_INTEGER_BLOB, CRYPTPROTECT_UI_FORBIDDEN, CryptUnprotectData,
        },
    };
    let mut raw = BASE64
        .decode(encoded.trim())
        .map_err(|e| format!("Base64 inválido en credencial: {e}"))?;
    let input = CRYPT_INTEGER_BLOB {
        cbData: raw.len() as u32,
        pbData: raw.as_mut_ptr(),
    };
    let mut output = CRYPT_INTEGER_BLOB::default();
    unsafe {
        CryptUnprotectData(
            &input,
            None,
            None,
            None,
            None,
            CRYPTPROTECT_UI_FORBIDDEN,
            &mut output,
        )
        .map_err(|e| format!("Error al descifrar la clave con DPAPI: {e}"))?;
        let slice = std::slice::from_raw_parts(output.pbData, output.cbData as usize);
        let plain = String::from_utf8(slice.to_vec())
            .map_err(|e| format!("UTF-8 inválido al descifrar la clave: {e}"))?;
        let _ = LocalFree(Some(HLOCAL(output.pbData as _)));
        Ok(plain)
    }
}

#[cfg(not(target_os = "windows"))]
fn protect_string(value: &str) -> Result<String, String> {
    Ok(BASE64.encode(value.as_bytes()))
}

#[cfg(not(target_os = "windows"))]
fn unprotect_string(encoded: &str) -> Result<String, String> {
    let raw = BASE64
        .decode(encoded.trim())
        .map_err(|e| format!("Base64 inválido en credencial: {e}"))?;
    String::from_utf8(raw).map_err(|e| format!("UTF-8 inválido al descifrar la clave: {e}"))
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::time::{SystemTime, UNIX_EPOCH};

    fn temp_secrets(label: &str) -> PathBuf {
        let ts = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_nanos();
        std::env::temp_dir().join(format!("lyra-secrets-{label}-{ts}.json"))
    }

    #[test]
    fn dpapi_roundtrip_and_isolation_per_provider() {
        let path = temp_secrets("roundtrip");
        save_api_key_to(&path, AiProvider::Gemini, "AIza-test-key-123").unwrap();
        save_api_key_to(&path, AiProvider::OpenAiCompatible, "sk-openai-456").unwrap();

        assert_eq!(
            load_api_key_from(&path, AiProvider::Gemini),
            "AIza-test-key-123"
        );
        assert_eq!(
            load_api_key_from(&path, AiProvider::OpenAiCompatible),
            "sk-openai-456"
        );
        assert_eq!(load_api_key_from(&path, AiProvider::MiniMax), "");

        let raw_file = fs::read_to_string(&path).unwrap();
        assert!(!raw_file.contains("AIza-test-key-123"));
        assert!(!raw_file.contains("sk-openai-456"));

        save_api_key_to(&path, AiProvider::Gemini, "").unwrap();
        assert_eq!(load_api_key_from(&path, AiProvider::Gemini), "");

        fs::remove_file(&path).unwrap();
    }
}
