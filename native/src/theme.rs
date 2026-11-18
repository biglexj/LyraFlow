//! Roles de color de la demo nativa de LyraFlow.
//!
//! **Aurora** consume los tokens globales de Core (`biglex-app-base`, versión `1.0.0`).
//! La copia empaquetada con la compilación vive en
//! `native/assets/tokens/biglex-app-base.json` —su origen y su procedimiento de
//! actualización están en `native/assets/tokens/README.md`— y el test
//! `aurora_matches_vendored_tokens` comprueba que los valores de este módulo no se
//! desvían de esa copia.
//!
//! **Clásico** conserva la paleta anterior de LyraFlow: los mismos fondos, textos y el
//! violeta `#7F52FF` / `#B499FF` que la aplicación de Compose muestra hoy.

/// Copia de los tokens empaquetada con la demo.
const VENDORED_TOKENS: &str = include_str!("../assets/tokens/biglex-app-base.json");

/// Versión de la paleta declarada por la copia de tokens empaquetada.
pub fn palette_version() -> &'static str {
    const KEY: &str = "\"version\": \"";
    let Some(start) = VENDORED_TOKENS.find(KEY) else {
        return "desconocida";
    };
    let start = start + KEY.len();
    let Some(end) = VENDORED_TOKENS[start..].find('"') else {
        return "desconocida";
    };
    &VENDORED_TOKENS[start..start + end]
}

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum ThemeName {
    Aurora,
    Classic,
}

impl ThemeName {
    pub fn label(self) -> &'static str {
        match self {
            ThemeName::Aurora => "Aurora",
            ThemeName::Classic => "Clásico",
        }
    }

    pub fn id(self) -> &'static str {
        match self {
            ThemeName::Aurora => "aurora",
            ThemeName::Classic => "classic",
        }
    }

    pub fn from_id(value: &str) -> Option<Self> {
        match value {
            "aurora" => Some(ThemeName::Aurora),
            "classic" => Some(ThemeName::Classic),
            _ => None,
        }
    }
}

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum ThemeMode {
    System,
    Light,
    Dark,
}

impl ThemeMode {
    pub fn label(self) -> &'static str {
        match self {
            ThemeMode::System => "Automático",
            ThemeMode::Light => "Claro",
            ThemeMode::Dark => "Oscuro",
        }
    }

    pub fn id(self) -> &'static str {
        match self {
            ThemeMode::System => "system",
            ThemeMode::Light => "light",
            ThemeMode::Dark => "dark",
        }
    }

    pub fn from_id(value: &str) -> Option<Self> {
        match value {
            "system" => Some(ThemeMode::System),
            "light" => Some(ThemeMode::Light),
            "dark" => Some(ThemeMode::Dark),
            _ => None,
        }
    }
}

/// Preferencia de apariencia: el tema elegido y el modo de resolución.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub struct Appearance {
    pub theme: ThemeName,
    pub mode: ThemeMode,
}

impl Default for Appearance {
    /// Aurora es el predeterminado para instalaciones nuevas; el modo automático
    /// sigue al sistema mientras el usuario no elija lo contrario.
    fn default() -> Self {
        Self {
            theme: ThemeName::Aurora,
            mode: ThemeMode::System,
        }
    }
}

impl Appearance {
    /// Modo efectivo: el elegido o el del sistema cuando la preferencia es automática.
    pub fn resolved_dark(&self, system_dark: bool) -> bool {
        match self.mode {
            ThemeMode::System => system_dark,
            ThemeMode::Light => false,
            ThemeMode::Dark => true,
        }
    }
}

/// Roles de color resueltos, en hexadecimal `0xRRGGBB` (el velo es `0xRRGGBBAA`).
#[derive(Clone, Copy, Debug)]
pub struct Palette {
    pub background: u32,
    pub surface: u32,
    pub surface_raised: u32,
    pub foreground: u32,
    pub secondary_foreground: u32,
    pub border_subtle: u32,
    pub control_border: u32,
    pub accent: u32,
    pub on_accent: u32,
    pub accent_foreground: u32,
    pub accent_hover: u32,
    pub accent_pressed: u32,
    pub surface_hover: u32,
    pub surface_pressed: u32,
    pub selection: u32,
    pub focus_ring: u32,
    pub error: u32,
    pub meter: u32,
    pub scrim: u32,
}

/// Paleta de Aurora tal como la define Core.
pub const fn aurora(dark: bool) -> Palette {
    if dark {
        Palette {
            background: 0x020908,
            surface: 0x071512,
            surface_raised: 0x0B1D18,
            foreground: 0xFFFFFF,
            secondary_foreground: 0xFFFFFF,
            border_subtle: 0x28453E,
            control_border: 0x5A8377,
            accent: 0x00C9B9,
            on_accent: 0x000000,
            accent_foreground: 0x00C9B9,
            accent_hover: 0x40DFD1,
            accent_pressed: 0x00AB9D,
            surface_hover: 0x10211C,
            surface_pressed: 0x091713,
            selection: 0x12352D,
            focus_ring: 0x00C9B9,
            error: 0xFF8B8B,
            meter: 0x00C9B9,
            scrim: 0x0000008F,
        }
    } else {
        Palette {
            background: 0xFAFFFD,
            surface: 0xF6FBF9,
            surface_raised: 0xEAF6F1,
            foreground: 0x000000,
            secondary_foreground: 0x000000,
            border_subtle: 0xC3DAD3,
            control_border: 0x69877E,
            accent: 0x00C9B9,
            on_accent: 0x000000,
            accent_foreground: 0x00695F,
            accent_hover: 0x40DFD1,
            accent_pressed: 0x00AB9D,
            surface_hover: 0xFBFEFC,
            surface_pressed: 0xDEEEE7,
            selection: 0xD4EFE8,
            focus_ring: 0x00695F,
            error: 0xE55353,
            meter: 0x00C9B9,
            scrim: 0x00000052,
        }
    }
}

/// Paleta clásica de LyraFlow.
///
/// Conserva el diseño tal como está: los fondos, textos y el violeta que la demo
/// ya mostraba. Los roles que ese diseño no definía se toman literalmente de
/// [`LyraFlowTheme.kt`](../../composeApp/src/commonMain/kotlin/com/biglexj/lyraflow/core/theme/LyraFlowTheme.kt)
/// —`primary`, `primaryContainer`, `surfaceVariant`, `outline`, `secondary` y
/// `error`—, sin aproximaciones.
pub const fn classic(dark: bool) -> Palette {
    if dark {
        Palette {
            background: 0x141218,
            surface: 0x211C29,
            surface_raised: 0x49454F,
            foreground: 0xE6E1E9,
            secondary_foreground: 0xCAC4D0,
            border_subtle: 0x49454F,
            control_border: 0x938F99,
            accent: 0xB499FF,
            on_accent: 0x431198,
            accent_foreground: 0xECE3FF,
            accent_hover: 0xB499FF,
            accent_pressed: 0x5B34B6,
            surface_hover: 0x49454F,
            surface_pressed: 0x49454F,
            selection: 0x5B34B6,
            focus_ring: 0xB499FF,
            error: 0xFF8B8B,
            meter: 0x56E3CE,
            scrim: 0x0000008F,
        }
    } else {
        Palette {
            background: 0xF9F7FC,
            surface: 0xECE3FF,
            surface_raised: 0xFDF8FF,
            foreground: 0x1C1B20,
            secondary_foreground: 0x49454F,
            border_subtle: 0xCAC4D0,
            control_border: 0x7A757F,
            accent: 0x7F52FF,
            on_accent: 0xFFFFFF,
            accent_foreground: 0x24005A,
            accent_hover: 0x7F52FF,
            accent_pressed: 0xECE3FF,
            surface_hover: 0xE7E0EC,
            surface_pressed: 0xE7E0EC,
            selection: 0xECE3FF,
            focus_ring: 0x7F52FF,
            error: 0xE55353,
            meter: 0x00A896,
            scrim: 0x00000052,
        }
    }
}

/// Paleta del tema elegido en el modo resuelto.
pub fn palette(theme: ThemeName, dark: bool) -> Palette {
    match theme {
        ThemeName::Aurora => aurora(dark),
        ThemeName::Classic => classic(dark),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn hex(value: u32) -> String {
        format!("#{:06X}", value)
    }

    fn assert_token(tokens: &str, key: &str, value: u32) {
        let expected = format!("\"{key}\": \"{}\"", hex(value));
        assert!(
            tokens.contains(&expected),
            "Los tokens empaquetados no contienen {expected}"
        );
    }

    #[test]
    fn aurora_matches_vendored_tokens() {
        for dark in [false, true] {
            let palette = aurora(dark);
            let mode = if dark { "dark" } else { "light" };
            let tokens = VENDORED_TOKENS
                .split_once(&format!("\"{mode}\""))
                .expect("El modo de la copia de tokens debe existir.")
                .1;
            assert_token(tokens, "background", palette.background);
            assert_token(tokens, "surface", palette.surface);
            assert_token(tokens, "surfaceRaised", palette.surface_raised);
            assert_token(tokens, "foreground", palette.foreground);
            assert_token(tokens, "borderSubtle", palette.border_subtle);
            assert_token(tokens, "accent", palette.accent);
            assert_token(tokens, "onAccent", palette.on_accent);
            assert_token(tokens, "accentForeground", palette.accent_foreground);
            assert_token(tokens, "accentHover", palette.accent_hover);
            assert_token(tokens, "accentPressed", palette.accent_pressed);
            assert_token(tokens, "surfaceHover", palette.surface_hover);
            assert_token(tokens, "surfacePressed", palette.surface_pressed);
            assert_token(tokens, "selection", palette.selection);
            assert_token(tokens, "controlBorder", palette.control_border);
            assert_token(tokens, "focusRing", palette.focus_ring);
        }
    }

    #[test]
    fn aurora_text_is_not_dimmed() {
        // Core: la jerarquía se expresa con tamaño y peso, sin atenuar el texto.
        assert_eq!(aurora(false).secondary_foreground, aurora(false).foreground);
        assert_eq!(aurora(true).secondary_foreground, aurora(true).foreground);
    }

    #[test]
    fn classic_keeps_the_lyraflow_violet() {
        assert_eq!(classic(false).accent, 0x7F52FF);
        assert_eq!(classic(true).accent, 0xB499FF);
        assert_eq!(classic(false).background, 0xF9F7FC);
        assert_eq!(classic(true).background, 0x141218);
    }

    #[test]
    fn classic_only_uses_values_of_the_lyraflow_palette() {
        // Cada rol del tema clásico es un valor literal del diseño actual de la demo
        // o de LyraFlowTheme.kt. 0x211C29 es la superficie de la tarjeta que la demo
        // ya mostraba en oscuro.
        let light = classic(false);
        let dark = classic(true);
        let light_values = [
            0xF9F7FC, 0xECE3FF, 0xFDF8FF, 0x1C1B20, 0x49454F, 0xCAC4D0, 0x7A757F, 0x7F52FF,
            0xFFFFFF, 0x24005A, 0xE7E0EC, 0xE55353, 0x00A896,
        ];
        let dark_values = [
            0x141218, 0x211C29, 0x49454F, 0xE6E1E9, 0xCAC4D0, 0x938F99, 0xB499FF, 0x431198,
            0xECE3FF, 0x5B34B6, 0xFF8B8B, 0x56E3CE,
        ];
        let roles = [
            light.background,
            light.surface,
            light.surface_raised,
            light.foreground,
            light.secondary_foreground,
            light.border_subtle,
            light.control_border,
            light.accent,
            light.on_accent,
            light.accent_foreground,
            light.accent_hover,
            light.accent_pressed,
            light.surface_hover,
            light.surface_pressed,
            light.selection,
            light.focus_ring,
            light.error,
            light.meter,
        ];
        for value in roles {
            assert!(
                light_values.contains(&value),
                "El tema clásico claro usa un valor ajeno a LyraFlowTheme.kt: {value:#08X}"
            );
        }
        let roles = [
            dark.background,
            dark.surface,
            dark.surface_raised,
            dark.foreground,
            dark.secondary_foreground,
            dark.border_subtle,
            dark.control_border,
            dark.accent,
            dark.on_accent,
            dark.accent_foreground,
            dark.accent_hover,
            dark.accent_pressed,
            dark.surface_hover,
            dark.surface_pressed,
            dark.selection,
            dark.focus_ring,
            dark.error,
            dark.meter,
        ];
        for value in roles {
            assert!(
                dark_values.contains(&value),
                "El tema clásico oscuro usa un valor ajeno a LyraFlowTheme.kt: {value:#08X}"
            );
        }
    }

    #[test]
    fn mode_resolution_follows_the_preference() {
        let automatic = Appearance {
            theme: ThemeName::Aurora,
            mode: ThemeMode::System,
        };
        assert!(automatic.resolved_dark(true));
        assert!(!automatic.resolved_dark(false));

        let light = Appearance {
            theme: ThemeName::Aurora,
            mode: ThemeMode::Light,
        };
        assert!(!light.resolved_dark(true));

        let dark = Appearance {
            theme: ThemeName::Classic,
            mode: ThemeMode::Dark,
        };
        assert!(dark.resolved_dark(false));
    }

    #[test]
    fn new_installations_start_on_aurora() {
        let appearance = Appearance::default();
        assert_eq!(appearance.theme, ThemeName::Aurora);
        assert_eq!(appearance.mode, ThemeMode::System);
    }

    #[test]
    fn the_vendored_copy_declares_the_pinned_version() {
        assert_eq!(palette_version(), "1.0.0");
        assert!(VENDORED_TOKENS.contains("\"id\": \"biglex-app-base\""));
    }
}
