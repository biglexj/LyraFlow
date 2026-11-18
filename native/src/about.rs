//! Contenido del diálogo «Acerca de» de la demo nativa.
//!
//! Sigue la base de soporte oficial de Core: identidad, créditos y apoyo, con los
//! destinos oficiales del ecosistema.

pub const APP_NAME: &str = "LyraFlow";
pub const VERSION: &str = env!("CARGO_PKG_VERSION");
pub const LICENSE: &str = "MIT";
pub const AUTHOR: &str = "biglexj";
pub const TAGLINE: &str = "Habla. Ya está escrito.";
pub const DESCRIPTION: &str =
    "Dictado por voz con transcripción mediante IA multimodal, pensado para el uso diario.";
pub const THANKS: &str =
    "Si esta herramienta agiliza tu flujo de trabajo diario, considera apoyar su desarrollo continuo.";
pub const SCOPE_NOTE: &str =
    "Esta demo nativa no sustituye la aplicación instalada: la transcripción y las demás funciones siguen en Compose.";

#[derive(Debug)]
pub struct Link {
    pub label: &'static str,
    pub url: &'static str,
}

/// Destinos oficiales de apoyo del ecosistema.
pub const SUPPORT_LINKS: &[Link] = &[
    Link {
        label: "Donaciones oficiales (Yape / Plin / Web)",
        url: "https://www.biglexj.com/donaciones",
    },
    Link {
        label: "Buy Me a Coffee",
        url: "https://buymeacoffee.com/biglexj",
    },
];

/// Documentación, feedback y créditos.
pub const CREDIT_LINKS: &[Link] = &[
    Link {
        label: "Enviar feedback o reportar un error",
        url: "https://github.com/biglexj/LyraFlow/issues",
    },
    Link {
        label: "Perfil de GitHub",
        url: "https://github.com/biglexj",
    },
];

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn the_about_dialog_shows_the_required_information() {
        assert_eq!(APP_NAME, "LyraFlow");
        assert!(!VERSION.is_empty());
        assert_eq!(LICENSE, "MIT");
        assert_eq!(AUTHOR, "biglexj");
        assert!(!TAGLINE.is_empty());
        assert!(!DESCRIPTION.is_empty());
        assert!(!THANKS.is_empty());
    }

    #[test]
    fn the_official_destinations_are_present() {
        let urls: Vec<&str> = SUPPORT_LINKS
            .iter()
            .chain(CREDIT_LINKS.iter())
            .map(|link| link.url)
            .collect();
        assert!(urls.contains(&"https://www.biglexj.com/donaciones"));
        assert!(urls.contains(&"https://buymeacoffee.com/biglexj"));
        assert!(urls.contains(&"https://github.com/biglexj"));
    }

    #[test]
    fn every_destination_is_secure() {
        for link in SUPPORT_LINKS.iter().chain(CREDIT_LINKS.iter()) {
            assert!(link.url.starts_with("https://"), "Enlace sin HTTPS: {link:?}");
            assert!(!link.label.is_empty());
        }
    }
}
