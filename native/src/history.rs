//! Repositorio persistente de historial de transcripciones con retención configurable.

use serde::{Deserialize, Serialize};
use std::{
    fs,
    path::{Path, PathBuf},
    time::{SystemTime, UNIX_EPOCH},
};

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TranscriptionHistoryEntry {
    pub id: String,
    pub timestamp_ms: u64,
    pub raw_transcript: String,
    pub refined_text: String,
    pub provider_name: String,
    pub audio_duration_ms: u64,
}

impl TranscriptionHistoryEntry {
    pub fn display_text(&self) -> &str {
        if self.refined_text.trim().is_empty() {
            &self.raw_transcript
        } else {
            &self.refined_text
        }
    }
}

pub fn default_history_path() -> Option<PathBuf> {
    if let Ok(local) = std::env::var("LOCALAPPDATA")
        && !local.trim().is_empty()
    {
        return Some(
            PathBuf::from(local)
                .join("LyraFlow-gpui-demo")
                .join("history.json"),
        );
    }
    if let Ok(home) = std::env::var("HOME") {
        return Some(
            PathBuf::from(home)
                .join(".local")
                .join("share")
                .join("lyraflow-gpui-demo")
                .join("history.json"),
        );
    }
    None
}

pub fn now_millis() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_millis() as u64)
        .unwrap_or(0)
}

pub fn load_history(retention_hours: u64) -> Vec<TranscriptionHistoryEntry> {
    let Some(path) = default_history_path() else {
        return Vec::new();
    };
    load_history_from(&path, retention_hours, now_millis())
}

pub fn save_history(
    entries: &[TranscriptionHistoryEntry],
    retention_hours: u64,
) -> Result<Vec<TranscriptionHistoryEntry>, String> {
    let retained = retain_entries(entries.to_vec(), retention_hours, now_millis());
    let Some(path) = default_history_path() else {
        return Ok(retained);
    };
    persist_history_to(&path, &retained)?;
    Ok(retained)
}

pub fn retain_entries(
    entries: Vec<TranscriptionHistoryEntry>,
    retention_hours: u64,
    now_ms: u64,
) -> Vec<TranscriptionHistoryEntry> {
    if retention_hours == 0 {
        return Vec::new();
    }
    let cutoff = now_ms.saturating_sub(retention_hours * 3_600_000);
    let mut filtered: Vec<TranscriptionHistoryEntry> = entries
        .into_iter()
        .filter(|e| e.timestamp_ms >= cutoff)
        .collect();
    filtered.sort_by(|a, b| b.timestamp_ms.cmp(&a.timestamp_ms));
    let mut seen = std::collections::HashSet::new();
    filtered.retain(|e| seen.insert(e.id.clone()));
    filtered
}

pub fn load_history_from(
    path: &Path,
    retention_hours: u64,
    now_ms: u64,
) -> Vec<TranscriptionHistoryEntry> {
    if !path.is_file() {
        return Vec::new();
    }
    let Ok(raw) = fs::read_to_string(path) else {
        return Vec::new();
    };
    let Ok(entries) = serde_json::from_str::<Vec<TranscriptionHistoryEntry>>(&raw) else {
        return Vec::new();
    };
    let retained = retain_entries(entries, retention_hours, now_ms);
    let _ = persist_history_to(path, &retained);
    retained
}

pub fn persist_history_to(
    path: &Path,
    entries: &[TranscriptionHistoryEntry],
) -> Result<(), String> {
    if entries.is_empty() {
        if path.exists() {
            let _ = fs::remove_file(path);
        }
        return Ok(());
    }
    if let Some(parent) = path.parent() {
        fs::create_dir_all(parent)
            .map_err(|e| format!("No se pudo crear la carpeta del historial: {e}"))?;
    }
    let tmp = path.with_extension("json.tmp");
    let json = serde_json::to_string_pretty(entries)
        .map_err(|e| format!("No se pudo serializar el historial: {e}"))?;
    fs::write(&tmp, json).map_err(|e| format!("No se pudo escribir el historial: {e}"))?;
    if let Err(e) = fs::rename(&tmp, path) {
        let _ = fs::remove_file(&tmp);
        return Err(format!("No se pudo actualizar el historial: {e}"));
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn temp_history(label: &str) -> PathBuf {
        let ts = now_millis();
        std::env::temp_dir().join(format!("lyra-history-{label}-{ts}.json"))
    }

    #[test]
    fn retains_only_entries_within_window_and_persists_atomically() {
        let path = temp_history("retain");
        let now = 100 * 3_600_000;
        let fresh = TranscriptionHistoryEntry {
            id: "1".into(),
            timestamp_ms: now - 1_000,
            raw_transcript: "hola".into(),
            refined_text: "Hola.".into(),
            provider_name: "Gemini".into(),
            audio_duration_ms: 420,
        };
        let expired = TranscriptionHistoryEntry {
            id: "2".into(),
            timestamp_ms: now - 30 * 3_600_000,
            raw_transcript: "viejo".into(),
            refined_text: "Viejo.".into(),
            provider_name: "Gemini".into(),
            audio_duration_ms: 300,
        };
        persist_history_to(&path, &[fresh.clone(), expired]).unwrap();
        let loaded = load_history_from(&path, 24, now);
        assert_eq!(loaded, vec![fresh]);

        let cleared = retain_entries(loaded, 0, now);
        assert!(cleared.is_empty());
        persist_history_to(&path, &cleared).unwrap();
        assert!(!path.exists());
    }
}
