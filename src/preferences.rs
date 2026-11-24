//! Preferencias locales de la aplicación nativa de LyraFlow.
//!
//! Guarda su propia configuración en `%LOCALAPPDATA%\LyraFlow-gpui-demo\appearance.txt`
//! sin leer ni escribir las preferencias de la instalación de Compose.

use crate::platform_win::KeyboardShortcut;
use crate::theme::{Appearance, ThemeMode, ThemeName};
use std::{
    collections::BTreeMap,
    fs,
    path::{Path, PathBuf},
};

/// Nombre del archivo de preferencias.
pub const FILE_NAME: &str = "appearance.txt";

pub const DEFAULT_SYSTEM_PROMPT: &str = "\
Eres el motor de edición de LyraFlow. Convierte una transcripción de voz en texto final claro, coherente, natural y fiel a lo expresado por el usuario.

PRIORIDADES

1. FIDELIDAD

* Conserva la intención, significado, tono e idioma original.
* No traduzcas.
* Preserva nombres, cifras, fechas, URLs, rutas, comandos, código, símbolos y caracteres especiales o CJK.
* No resumas, inventes, completes ni añadas información que el usuario no haya expresado.

2. LIMPIEZA DEL DICTADO

* Corrige ortografía, puntuación, concordancia y errores evidentes de transcripción cuando el contexto permita identificarlos con seguridad.
* Elimina muletillas, repeticiones accidentales, tartamudeos textuales y falsos comienzos cuando no aporten significado.
* Reorganiza ligeramente frases mal construidas para que resulten comprensibles, sin alterar lo que quiso decir el hablante.
* Mantén expresiones coloquiales y el estilo natural del usuario.

3. ESTRUCTURA
   Detecta la estructura implícita del discurso y refléjala en el texto.

* Si el usuario dice “primero”, “segundo”, “tercero”, etc., crea una enumeración ordenada.
* Si enumera “uno, dos, tres”, organiza cada elemento por separado cuando corresponda.
* Si utiliza “A, B, C”, conserva esa estructura.
* Si presenta varios puntos, requisitos, características, pasos o ejemplos, sepáralos mediante listas cuando mejore claramente la lectura.
* Si cambia de tema o desarrolla una idea distinta, crea un nuevo párrafo.
* En dictados largos, separa también los cambios de enfoque dentro de un mismo tema. Agrupa las frases relacionadas y deja una línea en blanco entre párrafos; evita devolver todo como un único bloque.
* Un dictado breve con una sola idea puede quedar en un párrafo. No fuerces un salto después de cada frase ni inventes títulos.
* No conviertas automáticamente todo en listas: utiliza el formato que mejor represente la forma en que habló el usuario.

4. ÓRDENES DE FORMATO HABLADAS
   Interpreta expresiones como “punto”, “coma”, “dos puntos”, “nueva línea”, “nuevo párrafo”, “entre comillas”, “abre paréntesis”, “cierra paréntesis” o “punto por punto” como instrucciones de formato cuando el contexto lo indique. No las escribas literalmente en esos casos.

5. INSULTOS Y EXPRESIONES FUERTES

* Omite únicamente insultos, ataques o palabras ofensivas utilizadas directamente para agredir o degradar a una persona o grupo.
* No elimines palabras fuertes usadas como exclamación, frustración, énfasis, cita, explicación o parte necesaria del significado.
* Expresiones como “carajo”, “diablos”, “no puede ser” o “¿qué pasó acá?” pueden conservarse cuando sean expresiones naturales y no ataques directos.
* Al eliminar un insulto directo, reconstruye la oración de forma natural sin indicar que fue censurado.

6. SALIDA
   Devuelve exclusivamente el texto final procesado. No añadas explicaciones, comentarios, encabezados artificiales, comillas envolventes ni bloques de código.
   Si no existe voz o contenido inteligible suficiente, devuelve una cadena vacía.";

pub const LITERAL_SYSTEM_PROMPT: &str = "\
Transcribe fielmente la voz del usuario. Conserva sus palabras, su orden, modismos, repeticiones e ideas, sin resumir, reformular, completar ni añadir información.

Añade la puntuación y las mayúsculas necesarias para facilitar la lectura, sin corregir la gramática del hablante ni cambiar su vocabulario o su intención. No elimines muletillas, titubeos o repeticiones que sean audibles.

Separa el texto en párrafos cuando cambie la idea o el enfoque. Agrupa las frases relacionadas y deja una línea en blanco entre párrafos. En un dictado largo, evita entregar todo en un único bloque; un dictado breve con una sola idea puede quedar en un párrafo. No reordenes las frases ni conviertas automáticamente el discurso en listas o encabezados.

Devuelve únicamente el texto transcrito, sin comentarios, aclaraciones, comillas envolventes ni bloques Markdown. No inventes palabras para rellenar silencios o ruido. Si no existe voz inteligible, devuelve una cadena vacía.";

pub const CUSTOM_SYSTEM_PROMPT_EXAMPLE: &str = "Corrige ortografía y puntuación, elimina muletillas y redacta con un tono claro y profesional.";

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum HistoryRetention {
    Disabled,
    Hours24,
    Hours48,
    Hours72,
}

impl HistoryRetention {
    pub fn hours(self) -> u64 {
        match self {
            HistoryRetention::Disabled => 0,
            HistoryRetention::Hours24 => 24,
            HistoryRetention::Hours48 => 48,
            HistoryRetention::Hours72 => 72,
        }
    }

    pub fn label(self) -> &'static str {
        match self {
            HistoryRetention::Disabled => "Desactivado",
            HistoryRetention::Hours24 => "24 horas",
            HistoryRetention::Hours48 => "48 horas",
            HistoryRetention::Hours72 => "72 horas",
        }
    }

    pub fn id(self) -> &'static str {
        match self {
            HistoryRetention::Disabled => "disabled",
            HistoryRetention::Hours24 => "24h",
            HistoryRetention::Hours48 => "48h",
            HistoryRetention::Hours72 => "72h",
        }
    }

    pub fn from_id(value: &str) -> Option<Self> {
        match value {
            "disabled" => Some(HistoryRetention::Disabled),
            "24h" => Some(HistoryRetention::Hours24),
            "48h" => Some(HistoryRetention::Hours48),
            "72h" => Some(HistoryRetention::Hours72),
            _ => None,
        }
    }
}

#[derive(Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Debug)]
pub enum AiProvider {
    Gemini,
    OpenAiCompatible,
    MiniMax,
}

impl AiProvider {
    pub fn label(self) -> &'static str {
        match self {
            AiProvider::Gemini => "Gemini",
            AiProvider::OpenAiCompatible => "OpenAI",
            AiProvider::MiniMax => "MiniMax",
        }
    }

    pub fn id(self) -> &'static str {
        match self {
            AiProvider::Gemini => "gemini",
            AiProvider::OpenAiCompatible => "openai",
            AiProvider::MiniMax => "minimax",
        }
    }

    pub fn from_id(value: &str) -> Option<Self> {
        match value {
            "gemini" => Some(AiProvider::Gemini),
            "openai" => Some(AiProvider::OpenAiCompatible),
            "minimax" => Some(AiProvider::MiniMax),
            _ => None,
        }
    }

    pub fn default_endpoint(self) -> &'static str {
        match self {
            AiProvider::Gemini => "https://generativelanguage.googleapis.com/v1beta",
            AiProvider::OpenAiCompatible => "https://api.openai.com/v1/chat/completions",
            AiProvider::MiniMax => "https://api.minimax.io/v1",
        }
    }

    pub fn api_key_env_var(self) -> &'static str {
        match self {
            AiProvider::Gemini => "GEMINI_API_KEY",
            AiProvider::OpenAiCompatible => "OPENAI_API_KEY",
            AiProvider::MiniMax => "MINIMAX_API_KEY",
        }
    }
}

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum SystemPromptMode {
    Smart,
    Literal,
    Custom,
}

impl SystemPromptMode {
    pub fn label(self) -> &'static str {
        match self {
            SystemPromptMode::Smart => "Inteligente",
            SystemPromptMode::Literal => "Voz original",
            SystemPromptMode::Custom => "Personalizado",
        }
    }

    pub fn description(self) -> &'static str {
        match self {
            SystemPromptMode::Smart => "Refina puntuación, formato y corrige muletillas",
            SystemPromptMode::Literal => {
                "Conserva tus palabras, con puntuación y párrafos para facilitar la lectura"
            }
            SystemPromptMode::Custom => {
                "Define cómo quieres que la IA edite y dé formato a tu dictado"
            }
        }
    }

    pub fn id(self) -> &'static str {
        match self {
            SystemPromptMode::Smart => "smart",
            SystemPromptMode::Literal => "literal",
            SystemPromptMode::Custom => "custom",
        }
    }

    pub fn from_id(value: &str) -> Option<Self> {
        match value {
            "smart" => Some(SystemPromptMode::Smart),
            "literal" => Some(SystemPromptMode::Literal),
            "custom" => Some(SystemPromptMode::Custom),
            _ => None,
        }
    }
}

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum WhisperLanguage {
    Auto,
    Spanish,
    English,
    Portuguese,
    German,
    French,
    Italian,
    Russian,
    Japanese,
    Chinese,
}

impl WhisperLanguage {
    pub const ALL: &'static [WhisperLanguage] = &[
        WhisperLanguage::Auto,
        WhisperLanguage::Spanish,
        WhisperLanguage::English,
        WhisperLanguage::Portuguese,
        WhisperLanguage::German,
        WhisperLanguage::French,
        WhisperLanguage::Italian,
        WhisperLanguage::Russian,
        WhisperLanguage::Japanese,
        WhisperLanguage::Chinese,
    ];

    pub fn label(self) -> &'static str {
        match self {
            WhisperLanguage::Auto => "Automático (Idioma del sistema)",
            WhisperLanguage::Spanish => "Español",
            WhisperLanguage::English => "Inglés",
            WhisperLanguage::Portuguese => "Portugués",
            WhisperLanguage::German => "Alemán",
            WhisperLanguage::French => "Francés",
            WhisperLanguage::Italian => "Italiano",
            WhisperLanguage::Russian => "Ruso",
            WhisperLanguage::Japanese => "Japonés",
            WhisperLanguage::Chinese => "Chino",
        }
    }

    pub fn code(self) -> &'static str {
        match self {
            WhisperLanguage::Auto => "auto",
            WhisperLanguage::Spanish => "es",
            WhisperLanguage::English => "en",
            WhisperLanguage::Portuguese => "pt",
            WhisperLanguage::German => "de",
            WhisperLanguage::French => "fr",
            WhisperLanguage::Italian => "it",
            WhisperLanguage::Russian => "ru",
            WhisperLanguage::Japanese => "ja",
            WhisperLanguage::Chinese => "zh",
        }
    }

    pub fn from_code(value: &str) -> Option<Self> {
        Self::ALL.iter().copied().find(|lang| lang.code() == value)
    }
}

#[derive(Clone, PartialEq, Eq, Debug)]
pub struct AppPreferences {
    pub appearance: Appearance,
    pub auto_inject: bool,
    pub launch_at_startup: bool,
    pub history_retention: HistoryRetention,
    pub provider: AiProvider,
    pub provider_enabled: bool,
    pub whisper_enabled: bool,
    pub model: String,
    pub endpoint: String,
    pub prompt_mode: SystemPromptMode,
    pub system_prompt: String,
    pub whisper_language: WhisperLanguage,
    pub whisper_llm_refinement: bool,
    pub shortcut: KeyboardShortcut,
    pub cancel_shortcut: KeyboardShortcut,
    pub discovered_models: BTreeMap<AiProvider, Vec<String>>,
}

impl Default for AppPreferences {
    fn default() -> Self {
        Self {
            appearance: Appearance::default(),
            auto_inject: true,
            launch_at_startup: true,
            history_retention: HistoryRetention::Hours24,
            provider: AiProvider::Gemini,
            provider_enabled: true,
            whisper_enabled: true,
            model: String::new(),
            endpoint: AiProvider::Gemini.default_endpoint().to_string(),
            prompt_mode: SystemPromptMode::Smart,
            system_prompt: DEFAULT_SYSTEM_PROMPT.to_string(),
            whisper_language: WhisperLanguage::Auto,
            whisper_llm_refinement: false,
            shortcut: KeyboardShortcut::default_primary(),
            cancel_shortcut: KeyboardShortcut::default_cancel(),
            discovered_models: BTreeMap::new(),
        }
    }
}

impl AppPreferences {
    pub fn available_models(&self, provider: AiProvider) -> Vec<String> {
        let mut out = Vec::new();
        if let Some(list) = self.discovered_models.get(&provider) {
            for item in list {
                let trimmed = item.trim();
                if !trimmed.is_empty() && !out.iter().any(|existing| existing == trimmed) {
                    out.push(trimmed.to_string());
                }
            }
        }
        let current = self.model.trim();
        if provider == self.provider
            && !current.is_empty()
            && !out.iter().any(|existing| existing == current)
        {
            out.insert(0, current.to_string());
        }
        out
    }

    pub fn effective_system_prompt(&self, has_api_key: bool) -> &str {
        if !has_api_key {
            return LITERAL_SYSTEM_PROMPT;
        }
        match self.prompt_mode {
            SystemPromptMode::Smart => DEFAULT_SYSTEM_PROMPT,
            SystemPromptMode::Literal => LITERAL_SYSTEM_PROMPT,
            SystemPromptMode::Custom => {
                if self.system_prompt.trim().is_empty() {
                    DEFAULT_SYSTEM_PROMPT
                } else {
                    &self.system_prompt
                }
            }
        }
    }
}

/// Carpeta de configuración del sistema (con respaldo de compatibilidad `LyraFlow-gpui-demo`).
fn app_dir_for(win_name: &str, unix_name: &str) -> Option<PathBuf> {
    if let Ok(local) = std::env::var("LOCALAPPDATA") {
        return Some(PathBuf::from(local).join(win_name));
    }
    if let Ok(config) = std::env::var("XDG_CONFIG_HOME") {
        return Some(PathBuf::from(config).join(unix_name));
    }
    if let Ok(home) = std::env::var("HOME") {
        return Some(PathBuf::from(home).join(".config").join(unix_name));
    }
    None
}

fn preferences_dir() -> Option<PathBuf> {
    app_dir_for("LyraFlow", "lyraflow")
}

fn legacy_preferences_dir() -> Option<PathBuf> {
    app_dir_for("LyraFlow-gpui-demo", "lyraflow-gpui-demo")
}

/// Ruta del archivo de preferencias, si el sistema expone una carpeta de usuario.
pub fn preferences_path() -> Option<PathBuf> {
    preferences_dir().map(|directory| directory.join(FILE_NAME))
}

#[allow(dead_code)]
pub fn load() -> Appearance {
    load_all().appearance
}

pub fn load_all() -> AppPreferences {
    if let Some(path) = preferences_path()
        && path.is_file()
    {
        return load_all_from(&path);
    }
    if let Some(legacy_dir) = legacy_preferences_dir() {
        let legacy_path = legacy_dir.join(FILE_NAME);
        if legacy_path.is_file() {
            return load_all_from(&legacy_path);
        }
    }
    AppPreferences::default()
}

#[allow(dead_code)]
pub fn load_from(path: &Path) -> Appearance {
    load_all_from(path).appearance
}

pub fn load_all_from(path: &Path) -> AppPreferences {
    match fs::read_to_string(path) {
        Ok(content) => parse_all(&content),
        Err(_) => AppPreferences::default(),
    }
}

#[allow(dead_code)]
pub fn parse(content: &str) -> Appearance {
    parse_all(content).appearance
}

pub fn parse_all(content: &str) -> AppPreferences {
    let mut prefs = AppPreferences::default();
    for line in content.lines() {
        let Some((key, value)) = line.split_once('=') else {
            continue;
        };
        let value = value.trim();
        match key.trim() {
            "theme" => {
                if let Some(theme) = ThemeName::from_id(value) {
                    prefs.appearance.theme = theme;
                }
            }
            "mode" => {
                if let Some(mode) = ThemeMode::from_id(value) {
                    prefs.appearance.mode = mode;
                }
            }
            "auto_inject" => {
                if let Ok(flag) = value.parse() {
                    prefs.auto_inject = flag;
                }
            }
            "launch_at_startup" => {
                if let Ok(flag) = value.parse() {
                    prefs.launch_at_startup = flag;
                }
            }
            "history_retention" => {
                if let Some(retention) = HistoryRetention::from_id(value) {
                    prefs.history_retention = retention;
                }
            }
            "provider" => {
                if let Some(provider) = AiProvider::from_id(value) {
                    prefs.provider = provider;
                }
            }
            "provider_enabled" => {
                if let Ok(flag) = value.parse() {
                    prefs.provider_enabled = flag;
                }
            }
            "whisper_enabled" => {
                if let Ok(flag) = value.parse() {
                    prefs.whisper_enabled = flag;
                }
            }
            "model" => {
                prefs.model = value.to_string();
            }
            "endpoint" => {
                if !value.is_empty() {
                    prefs.endpoint = value.to_string();
                }
            }
            "prompt_mode" => {
                if let Some(mode) = SystemPromptMode::from_id(value) {
                    prefs.prompt_mode = mode;
                }
            }
            "system_prompt_b64" => {
                use base64::{Engine as _, engine::general_purpose::STANDARD as BASE64};
                if let Ok(bytes) = BASE64.decode(value)
                    && let Ok(decoded) = String::from_utf8(bytes)
                {
                    prefs.system_prompt = decoded;
                }
            }
            "whisper_language" => {
                if let Some(lang) = WhisperLanguage::from_code(value) {
                    prefs.whisper_language = lang;
                }
            }
            "whisper_llm_refinement" => {
                if let Ok(flag) = value.parse() {
                    prefs.whisper_llm_refinement = flag;
                }
            }
            "shortcut" => {
                if let Some(sc) = KeyboardShortcut::parse(value) {
                    prefs.shortcut = sc;
                }
            }
            "cancel_shortcut" => {
                if let Some(sc) = KeyboardShortcut::parse(value) {
                    prefs.cancel_shortcut = sc;
                }
            }
            key_name if key_name.starts_with("discovered.") => {
                if let Some(prov_id) = key_name.strip_prefix("discovered.")
                    && let Some(provider) = AiProvider::from_id(prov_id)
                {
                    let models: Vec<String> = value
                        .split(',')
                        .map(|s| s.trim().to_string())
                        .filter(|s| !s.is_empty())
                        .collect();
                    if !models.is_empty() {
                        prefs.discovered_models.insert(provider, models);
                    }
                }
            }
            _ => {}
        }
    }
    if prefs.cancel_shortcut.conflicts_with(&prefs.shortcut) {
        prefs.cancel_shortcut = KeyboardShortcut::default_cancel();
    }
    if prefs.endpoint.trim().is_empty() {
        prefs.endpoint = prefs.provider.default_endpoint().to_string();
    }
    prefs
}

#[allow(dead_code)]
pub fn save(appearance: Appearance) -> Result<(), String> {
    let mut prefs = load_all();
    prefs.appearance = appearance;
    save_all(&prefs)
}

pub fn save_all(prefs: &AppPreferences) -> Result<(), String> {
    let path =
        preferences_path().ok_or("No se encontró la carpeta de preferencias del sistema.")?;
    save_all_to(&path, prefs)
}

#[allow(dead_code)]
pub fn save_to(path: &Path, appearance: Appearance) -> Result<(), String> {
    let mut prefs = load_all_from(path);
    prefs.appearance = appearance;
    save_all_to(path, &prefs)
}

pub fn save_all_to(path: &Path, prefs: &AppPreferences) -> Result<(), String> {
    use base64::{Engine as _, engine::general_purpose::STANDARD as BASE64};
    if let Some(parent) = path.parent() {
        fs::create_dir_all(parent)
            .map_err(|error| format!("No se pudo crear la carpeta de preferencias: {error}"))?;
    }
    let prompt_b64 = BASE64.encode(prefs.system_prompt.as_bytes());
    let mut content = format!(
        "theme={}\nmode={}\nauto_inject={}\nlaunch_at_startup={}\nhistory_retention={}\nprovider={}\nprovider_enabled={}\nwhisper_enabled={}\nmodel={}\nendpoint={}\nprompt_mode={}\nsystem_prompt_b64={}\nwhisper_language={}\nwhisper_llm_refinement={}\nshortcut={}\ncancel_shortcut={}\n",
        prefs.appearance.theme.id(),
        prefs.appearance.mode.id(),
        prefs.auto_inject,
        prefs.launch_at_startup,
        prefs.history_retention.id(),
        prefs.provider.id(),
        prefs.provider_enabled,
        prefs.whisper_enabled,
        prefs.model,
        prefs.endpoint,
        prefs.prompt_mode.id(),
        prompt_b64,
        prefs.whisper_language.code(),
        prefs.whisper_llm_refinement,
        prefs.shortcut.serialize(),
        prefs.cancel_shortcut.serialize(),
    );
    for (provider, models) in &prefs.discovered_models {
        if !models.is_empty() {
            content.push_str(&format!(
                "discovered.{}={}\n",
                provider.id(),
                models.join(",")
            ));
        }
    }
    fs::write(path, content)
        .map_err(|error| format!("No se pudo guardar la preferencia de apariencia: {error}"))
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::time::{SystemTime, UNIX_EPOCH};

    fn temporary_file(label: &str) -> PathBuf {
        let timestamp = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_nanos();
        std::env::temp_dir().join(format!("lyraflow-gpui-{label}-{timestamp}.txt"))
    }

    #[test]
    fn the_preference_survives_a_restart() {
        let path = temporary_file("appearance");
        let appearance = Appearance {
            theme: ThemeName::Classic,
            mode: ThemeMode::Dark,
        };
        save_to(&path, appearance).unwrap();
        assert_eq!(load_from(&path), appearance);
        fs::remove_file(&path).unwrap();
    }

    #[test]
    fn full_preferences_survive_a_restart() {
        let path = temporary_file("full");
        let mut discovered = BTreeMap::new();
        discovered.insert(
            AiProvider::OpenAiCompatible,
            vec!["custom-model-a".into(), "custom-model-b".into()],
        );
        let prefs = AppPreferences {
            appearance: Appearance {
                theme: ThemeName::Classic,
                mode: ThemeMode::Light,
            },
            auto_inject: false,
            launch_at_startup: false,
            history_retention: HistoryRetention::Hours72,
            provider: AiProvider::OpenAiCompatible,
            provider_enabled: true,
            whisper_enabled: false,
            model: "custom-model-a".into(),
            endpoint: "http://localhost:8080/v1/chat/completions".into(),
            prompt_mode: SystemPromptMode::Literal,
            system_prompt: "Mi prompt".into(),
            whisper_language: WhisperLanguage::Spanish,
            whisper_llm_refinement: true,
            shortcut: KeyboardShortcut::default_primary(),
            cancel_shortcut: KeyboardShortcut::default_cancel(),
            discovered_models: discovered,
        };
        save_all_to(&path, &prefs).unwrap();
        assert_eq!(load_all_from(&path), prefs);
        fs::remove_file(&path).unwrap();
    }

    #[test]
    fn a_missing_file_keeps_the_default_appearance() {
        let path = temporary_file("missing");
        assert_eq!(load_from(&path), Appearance::default());
    }

    #[test]
    fn unknown_values_fall_back_to_the_default() {
        let content = "theme=neon\nmode=sepia\n";
        assert_eq!(parse(content), Appearance::default());
        let partial = "theme=classic\n";
        let parsed = parse(partial);
        assert_eq!(parsed.theme, ThemeName::Classic);
        assert_eq!(parsed.mode, ThemeMode::System);
    }

    #[test]
    fn a_broken_file_does_not_block_the_demo() {
        let path = temporary_file("broken");
        fs::write(&path, "no se parece a una preferencia").unwrap();
        assert_eq!(load_from(&path), Appearance::default());
        fs::remove_file(&path).unwrap();
    }
}
