//! Preferencia de apariencia de la demo nativa.
//!
//! La demo guarda su propia preferencia en un archivo de texto simple. No lee ni
//! escribe las preferencias de la instalación de Compose.

use crate::theme::{Appearance, ThemeMode, ThemeName};
use std::{
    fs,
    path::{Path, PathBuf},
};

/// Nombre del archivo de preferencias.
pub const FILE_NAME: &str = "appearance.txt";

/// Carpeta de preferencias de la demo en el sistema.
fn preferences_dir() -> Option<PathBuf> {
    if let Ok(local) = std::env::var("LOCALAPPDATA") {
        return Some(PathBuf::from(local).join("LyraFlow-gpui-demo"));
    }
    if let Ok(config) = std::env::var("XDG_CONFIG_HOME") {
        return Some(PathBuf::from(config).join("lyraflow-gpui-demo"));
    }
    if let Ok(home) = std::env::var("HOME") {
        return Some(PathBuf::from(home).join(".config").join("lyraflow-gpui-demo"));
    }
    None
}

/// Ruta del archivo de preferencias, si el sistema expone una carpeta de usuario.
pub fn preferences_path() -> Option<PathBuf> {
    preferences_dir().map(|directory| directory.join(FILE_NAME))
}

/// Preferencia guardada; la predeterminada cuando no hay archivo o no es legible.
pub fn load() -> Appearance {
    match preferences_path() {
        Some(path) => load_from(&path),
        None => Appearance::default(),
    }
}

/// Preferencia guardada en una ruta concreta.
pub fn load_from(path: &Path) -> Appearance {
    match fs::read_to_string(path) {
        Ok(content) => parse(&content),
        Err(_) => Appearance::default(),
    }
}

/// Interpreta el contenido del archivo de preferencias.
pub fn parse(content: &str) -> Appearance {
    let mut appearance = Appearance::default();
    for line in content.lines() {
        let Some((key, value)) = line.split_once('=') else {
            continue;
        };
        match key.trim() {
            "theme" => {
                if let Some(theme) = ThemeName::from_id(value.trim()) {
                    appearance.theme = theme;
                }
            }
            "mode" => {
                if let Some(mode) = ThemeMode::from_id(value.trim()) {
                    appearance.mode = mode;
                }
            }
            _ => {}
        }
    }
    appearance
}

/// Guarda la preferencia en la ruta del sistema.
pub fn save(appearance: Appearance) -> Result<(), String> {
    let path = preferences_path().ok_or("No se encontró la carpeta de preferencias del sistema.")?;
    save_to(&path, appearance)
}

/// Guarda la preferencia en una ruta concreta.
pub fn save_to(path: &Path, appearance: Appearance) -> Result<(), String> {
    if let Some(parent) = path.parent() {
        fs::create_dir_all(parent)
            .map_err(|error| format!("No se pudo crear la carpeta de preferencias: {error}"))?;
    }
    let content = format!(
        "theme={}\nmode={}\n",
        appearance.theme.id(),
        appearance.mode.id()
    );
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
