//! Roles de color de la aplicación nativa de LyraFlow.
//!
//! **Aurora** consume los tokens globales de Core (`biglex-app-base`, versión `1.0.0`).
//! La copia empaquetada con la compilación vive en
//! `assets/tokens/biglex-app-base.json` —su origen y su procedimiento de
//! actualización están en `assets/tokens/README.md`— y el test
//! `aurora_matches_vendored_tokens` comprueba que los valores de este módulo no se
//! desvían de esa copia.
//!
//! **Clásico** conserva los roles de color propios de LyraFlow (edición histórica `v-kmp`).

/// Copia de los tokens empaquetada con la compilación.
#[cfg_attr(not(test), allow(dead_code))]
const VENDORED_TOKENS: &str = include_str!("../assets/tokens/biglex-app-base.json");

/// Versión de la paleta declarada por la copia de tokens empaquetada.
#[cfg_attr(not(test), allow(dead_code))]
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
            ThemeMode::System => "Sistema",
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
        match value.trim().to_ascii_lowercase().as_str() {
            "system" | "auto" | "sistema" | "automatic" => Some(ThemeMode::System),
            "light" | "claro" => Some(ThemeMode::Light),
            "dark" | "oscuro" => Some(ThemeMode::Dark),
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

/// Roles de color resueltos, en hexadecimal `0xRRGGBB` (y `0xRRGGBBAA` para fondos con opacidad o velos).
#[derive(Clone, Copy, Debug)]
pub struct Palette {
    pub background: u32,
    pub surface: u32,
    pub surface_card: u32,
    pub surface_muted: u32,
    pub surface_muted_strong: u32,
    pub surface_raised: u32,
    pub foreground: u32,
    pub secondary_foreground: u32,
    pub outline: u32,
    pub border_subtle: u32,
    pub control_border: u32,
    pub accent: u32,
    pub on_accent: u32,
    pub accent_foreground: u32,
    pub primary_container: u32,
    pub on_primary_container: u32,
    pub secondary: u32,
    pub secondary_container: u32,
    pub on_secondary_container: u32,
    pub tertiary: u32,
    pub accent_hover: u32,
    pub accent_pressed: u32,
    pub surface_hover: u32,
    pub surface_pressed: u32,
    pub selection: u32,
    pub focus_ring: u32,
    pub disabled_surface: u32,
    pub disabled_foreground: u32,
    pub error: u32,
    pub error_container: u32,
    pub on_error_container: u32,
    pub meter: u32,
    #[allow(dead_code)]
    pub scrim: u32,
}

/// Paleta de Aurora tal como la define Core.
pub const fn aurora(dark: bool) -> Palette {
    if dark {
        Palette {
            background: 0x020908,
            surface: 0x071512,
            surface_card: 0x071512,
            surface_muted: 0x071512FF,
            surface_muted_strong: 0x0B1D18FF,
            surface_raised: 0x0B1D18,
            foreground: 0xFFFFFF,
            secondary_foreground: 0xFFFFFF,
            outline: 0x5A8377,
            border_subtle: 0x28453E,
            control_border: 0x5A8377,
            accent: 0x00C9B9,
            on_accent: 0x000000,
            accent_foreground: 0x00C9B9,
            primary_container: 0x12352D,
            on_primary_container: 0xFFFFFF,
            secondary: 0x00C9B9,
            secondary_container: 0x12352D,
            on_secondary_container: 0xFFFFFF,
            tertiary: 0xFF8B8B,
            accent_hover: 0x40DFD1,
            accent_pressed: 0x00AB9D,
            surface_hover: 0x10211C,
            surface_pressed: 0x091713,
            selection: 0x12352D,
            focus_ring: 0x00C9B9,
            disabled_surface: 0x142723,
            disabled_foreground: 0x93AAA3,
            error: 0xFF8B8B,
            error_container: 0x93001A,
            on_error_container: 0xFFDAD8,
            meter: 0x00C9B9,
            scrim: 0x0000008F,
        }
    } else {
        Palette {
            background: 0xFAFFFD,
            surface: 0xF6FBF9,
            surface_card: 0xF6FBF9,
            surface_muted: 0xF6FBF9FF,
            surface_muted_strong: 0xEAF6F1FF,
            surface_raised: 0xEAF6F1,
            foreground: 0x000000,
            secondary_foreground: 0x000000,
            outline: 0x69877E,
            border_subtle: 0xC3DAD3,
            control_border: 0x69877E,
            accent: 0x00C9B9,
            on_accent: 0x000000,
            accent_foreground: 0x00695F,
            primary_container: 0xD4EFE8,
            on_primary_container: 0x000000,
            secondary: 0x00695F,
            secondary_container: 0xD4EFE8,
            on_secondary_container: 0x000000,
            tertiary: 0xE55353,
            accent_hover: 0x40DFD1,
            accent_pressed: 0x00AB9D,
            surface_hover: 0xFBFEFC,
            surface_pressed: 0xDEEEE7,
            selection: 0xD4EFE8,
            focus_ring: 0x00695F,
            disabled_surface: 0xE2EBE7,
            disabled_foreground: 0x51665E,
            error: 0xE55353,
            error_container: 0xFFDAD8,
            on_error_container: 0x410007,
            meter: 0x00C9B9,
            scrim: 0x00000052,
        }
    }
}

/// Paleta clásica de LyraFlow, tomada literalmente de `LyraFlowTheme.kt`.
pub const fn classic(dark: bool) -> Palette {
    if dark {
        Palette {
            background: 0x141218,
            surface: 0x141218,
            surface_card: 0x211C29,
            surface_muted: 0x49454F59,
            surface_muted_strong: 0x49454F73,
            surface_raised: 0x211C29,
            foreground: 0xE6E1E9,
            secondary_foreground: 0xCAC4D0,
            outline: 0x938F99,
            border_subtle: 0x49454F,
            control_border: 0x938F99,
            accent: 0xB499FF,
            on_accent: 0x431198,
            accent_foreground: 0xB499FF,
            primary_container: 0x5B34B6,
            on_primary_container: 0xECE3FF,
            secondary: 0x56E3CE,
            secondary_container: 0x005048,
            on_secondary_container: 0xC3F8EF,
            tertiary: 0xFF8B8B,
            accent_hover: 0xB499FF,
            accent_pressed: 0x5B34B6,
            surface_hover: 0x49454F,
            surface_pressed: 0x49454F,
            selection: 0x5B34B6,
            focus_ring: 0xB499FF,
            disabled_surface: 0x49454F,
            disabled_foreground: 0x938F99,
            error: 0xFF8B8B,
            error_container: 0x93001A,
            on_error_container: 0xFFDAD8,
            meter: 0xB499FF,
            scrim: 0x0000008F,
        }
    } else {
        Palette {
            background: 0xF9F7FC,
            surface: 0xFDF8FF,
            surface_card: 0xFDF8FF,
            surface_muted: 0xE7E0EC59,
            surface_muted_strong: 0xE7E0EC73,
            surface_raised: 0xFDF8FF,
            foreground: 0x1C1B20,
            secondary_foreground: 0x49454F,
            outline: 0x7A757F,
            border_subtle: 0xCAC4D0,
            control_border: 0x7A757F,
            accent: 0x7F52FF,
            on_accent: 0xFFFFFF,
            accent_foreground: 0x7F52FF,
            primary_container: 0xECE3FF,
            on_primary_container: 0x24005A,
            secondary: 0x00A896,
            secondary_container: 0xC3F8EF,
            on_secondary_container: 0x00201B,
            tertiary: 0xE55353,
            accent_hover: 0x7F52FF,
            accent_pressed: 0xECE3FF,
            surface_hover: 0xE7E0EC,
            surface_pressed: 0xE7E0EC,
            selection: 0xECE3FF,
            focus_ring: 0x7F52FF,
            disabled_surface: 0xE7E0EC,
            disabled_foreground: 0x7A757F,
            error: 0xE55353,
            error_container: 0xFFDAD8,
            on_error_container: 0x410007,
            meter: 0x7F52FF,
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
            assert_token(tokens, "disabledSurface", palette.disabled_surface);
            assert_token(tokens, "disabledForeground", palette.disabled_foreground);
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
        let light = classic(false);
        let dark = classic(true);
        let light_values = [
            0xF9F7FC, 0xECE3FF, 0xFDF8FF, 0x1C1B20, 0x49454F, 0xCAC4D0, 0x7A757F, 0x7F52FF,
            0xFFFFFF, 0x24005A, 0xE7E0EC, 0xE55353, 0x00A896, 0xC3F8EF, 0x00201B, 0xFFDAD8,
            0x410007,
        ];
        let dark_values = [
            0x141218, 0x211C29, 0x49454F, 0xE6E1E9, 0xCAC4D0, 0x938F99, 0xB499FF, 0x431198,
            0xECE3FF, 0x5B34B6, 0xFF8B8B, 0x56E3CE, 0x005048, 0xC3F8EF, 0x93001A, 0xFFDAD8,
        ];
        let roles = [
            light.background,
            light.surface,
            light.surface_card,
            light.surface_raised,
            light.foreground,
            light.secondary_foreground,
            light.outline,
            light.border_subtle,
            light.control_border,
            light.accent,
            light.on_accent,
            light.accent_foreground,
            light.primary_container,
            light.on_primary_container,
            light.secondary,
            light.secondary_container,
            light.on_secondary_container,
            light.tertiary,
            light.accent_hover,
            light.accent_pressed,
            light.surface_hover,
            light.surface_pressed,
            light.selection,
            light.focus_ring,
            light.disabled_surface,
            light.disabled_foreground,
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
            dark.surface_card,
            dark.surface_raised,
            dark.foreground,
            dark.secondary_foreground,
            dark.outline,
            dark.border_subtle,
            dark.control_border,
            dark.accent,
            dark.on_accent,
            dark.accent_foreground,
            dark.primary_container,
            dark.on_primary_container,
            dark.secondary,
            dark.secondary_container,
            dark.on_secondary_container,
            dark.tertiary,
            dark.accent_hover,
            dark.accent_pressed,
            dark.surface_hover,
            dark.surface_pressed,
            dark.selection,
            dark.focus_ring,
            dark.disabled_surface,
            dark.disabled_foreground,
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
