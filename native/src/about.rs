//! Contenido del diálogo «Acerca de» de LyraFlow nativo.
//!
//! Sigue la base de soporte oficial de Core: identidad, créditos y apoyo, con los
//! destinos oficiales del ecosistema.

pub const APP_NAME: &str = "LyraFlow";
pub const VERSION: &str = env!("CARGO_PKG_VERSION");
pub const LICENSE: &str = "MIT";
pub const AUTHOR: &str = "biglexj";
pub const LICENSE_TEXT: &str = include_str!("../../LICENSE");
pub const PLATFORM: &str = if cfg!(all(target_os = "windows", target_arch = "x86_64")) {
    "Windows 64-bit"
} else if cfg!(target_os = "windows") {
    "Windows"
} else if cfg!(target_os = "linux") {
    "Linux"
} else {
    std::env::consts::OS
};
pub const DESCRIPTION: &str = "Dictado por voz fluido e inteligente integrado con IA multimodal.";
pub const REPO_URL: &str = "https://github.com/biglexj/LyraFlow";
pub const APP_PAGE_URL: &str = "https://biglexj.com/desarrollo/lyraflow";
pub const THANKS: &str = "Gracias por usar LyraFlow. Es un proyecto de código abierto desarrollado por biglexj. Si te resulta útil, puedes apoyar su desarrollo continuo.";

#[derive(Debug)]
pub struct Link {
    pub label: &'static str,
    pub url: &'static str,
}

/// Destinos oficiales de apoyo del ecosistema.
pub const SUPPORT_LINKS: &[Link] = &[
    Link {
        label: "Donaciones oficiales",
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
        label: "Enviar Feedback / Reportar Error",
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
        assert_eq!(VERSION, "2.0.0");
        assert_eq!(LICENSE, "MIT");
        assert_eq!(AUTHOR, "biglexj");
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
        assert_eq!(APP_PAGE_URL, "https://biglexj.com/desarrollo/lyraflow");
    }

    #[test]
    fn every_destination_is_secure() {
        for link in SUPPORT_LINKS.iter().chain(CREDIT_LINKS.iter()) {
            assert!(
                link.url.starts_with("https://"),
                "Enlace sin HTTPS: {link:?}"
            );
            assert!(!link.label.is_empty());
        }
        assert!(APP_PAGE_URL.starts_with("https://"));
    }
}
