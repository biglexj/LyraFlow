//! Iconos vectoriales de LyraFlow equivalentes a `LyraIcon.kt`.

use gpui::{AssetSource, Hsla, IntoElement, Result, SharedString, Styled, px, svg};
use std::borrow::Cow;

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum IconType {
    Home,
    History,
    Settings,
    Mic,
    Copy,
    Clear,
    System,
    Sun,
    Moon,
    Info,
    Github,
    Globe,
    BiglexWeb,
    ExternalLink,
    Bug,
    Heart,
    Coffee,
    Shield,
}

impl IconType {
    pub fn asset_path(self) -> &'static str {
        match self {
            IconType::Home => "icons/home.svg",
            IconType::History => "icons/history.svg",
            IconType::Settings => "icons/settings.svg",
            IconType::Mic => "icons/mic.svg",
            IconType::Copy => "icons/copy.svg",
            IconType::Clear => "icons/clear.svg",
            IconType::System => "icons/system.svg",
            IconType::Sun => "icons/sun.svg",
            IconType::Moon => "icons/moon.svg",
            IconType::Info => "icons/info.svg",
            IconType::Github => "icons/github.svg",
            IconType::Globe => "icons/globe.svg",
            IconType::BiglexWeb => "icons/biglexj-symbol.svg",
            IconType::ExternalLink => "icons/external-link.svg",
            IconType::Bug => "icons/bug.svg",
            IconType::Heart => "icons/heart.svg",
            IconType::Coffee => "icons/coffee.svg",
            IconType::Shield => "icons/shield.svg",
        }
    }
}

pub struct LyraAssets;

impl AssetSource for LyraAssets {
    fn load(&self, path: &str) -> Result<Option<Cow<'static, [u8]>>> {
        let bytes: Option<&'static [u8]> = match path {
            "icons/home.svg" => Some(SVG_HOME),
            "icons/history.svg" => Some(SVG_HISTORY),
            "icons/settings.svg" => Some(SVG_SETTINGS),
            "icons/mic.svg" => Some(SVG_MIC),
            "icons/copy.svg" => Some(SVG_COPY),
            "icons/clear.svg" => Some(SVG_CLEAR),
            "icons/system.svg" => Some(SVG_SYSTEM),
            "icons/sun.svg" => Some(SVG_SUN),
            "icons/moon.svg" => Some(SVG_MOON),
            "icons/info.svg" => Some(SVG_INFO),
            "icons/github.svg" => Some(SVG_GITHUB),
            "icons/globe.svg" => Some(SVG_GLOBE),
            "icons/biglexj-symbol.svg" => Some(include_bytes!("../../assets/branding/icons/biglexj-symbol-white.svg")),
            "icons/external-link.svg" => Some(SVG_EXTERNAL),
            "icons/bug.svg" => Some(SVG_BUG),
            "icons/heart.svg" => Some(SVG_HEART),
            "icons/coffee.svg" => Some(SVG_COFFEE),
            "icons/shield.svg" => Some(SVG_SHIELD),
            _ => None,
        };
        Ok(bytes.map(Cow::Borrowed))
    }

    fn list(&self, path: &str) -> Result<Vec<SharedString>> {
        if path == "icons" || path.is_empty() {
            Ok(vec![
                "icons/home.svg".into(),
                "icons/history.svg".into(),
                "icons/settings.svg".into(),
                "icons/mic.svg".into(),
                "icons/copy.svg".into(),
                "icons/clear.svg".into(),
                "icons/system.svg".into(),
                "icons/sun.svg".into(),
                "icons/moon.svg".into(),
                "icons/info.svg".into(),
                "icons/github.svg".into(),
                "icons/globe.svg".into(),
                "icons/biglexj-symbol.svg".into(),
                "icons/external-link.svg".into(),
                "icons/bug.svg".into(),
                "icons/heart.svg".into(),
                "icons/coffee.svg".into(),
                "icons/shield.svg".into(),
            ])
        } else {
            Ok(vec![])
        }
    }
}

pub fn lyra_icon(icon: IconType, size_px: f32, color: impl Into<Hsla>) -> impl IntoElement {
    svg()
        .path(icon.asset_path())
        .size(px(size_px))
        .text_color(color.into())
}

const SVG_HOME: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#000000" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
  <path d="M4.3 11.5 L12 4.8 L19.7 11.5"/>
  <rect x="6.5" y="10.3" width="11" height="9.1" rx="1.2"/>
</svg>"##;

const SVG_HISTORY: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#000000" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
  <circle cx="12" cy="12" r="9.1"/>
  <path d="M12 6.7 V12 H15.6"/>
</svg>"##;

const SVG_SETTINGS: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#000000" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
  <circle cx="12" cy="12" r="3.8"/>
  <path d="M12 2.6 V5.3 M12 18.7 V21.4 M2.6 12 H5.3 M18.7 12 H21.4 M5.35 5.35 L7.25 7.25 M16.75 16.75 L18.65 18.65 M18.65 5.35 L16.75 7.25 M7.25 16.75 L5.35 18.65"/>
</svg>"##;

const SVG_MIC: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#000000" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
  <rect x="8.6" y="2.9" width="6.8" height="11.5" rx="3.4"/>
  <path d="M6 10.8 A6 6 0 0 0 18 10.8"/>
  <path d="M12 18 V21.1"/>
</svg>"##;

const SVG_COPY: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#000000" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
  <rect x="7.2" y="4.3" width="12" height="13" rx="1.4"/>
  <rect x="4.3" y="7.2" width="12" height="13" rx="1.4"/>
</svg>"##;

const SVG_CLEAR: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#000000" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
  <path d="M6 6 L18 18 M18 6 L6 18"/>
</svg>"##;

const SVG_SYSTEM: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#000000" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
  <rect x="3.8" y="4.8" width="16.4" height="11" rx="1.7"/>
  <path d="M12 15.8 V18.7 M7.7 19.2 H16.3"/>
</svg>"##;

const SVG_SUN: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#000000" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
  <circle cx="12" cy="12" r="4.8"/>
  <path d="M12 1.7 V4.3 M12 19.7 V22.3 M1.7 12 H4.3 M19.7 12 H22.3 M4.7 4.7 L6.6 6.6 M17.4 17.4 L19.3 19.3 M19.3 4.7 L17.4 6.6 M6.6 17.4 L4.7 19.3"/>
</svg>"##;

const SVG_MOON: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#000000">
  <path d="M14.9 2.4 C9.1 4.3 5.5 9.4 6.5 14.9 C7.4 20.2 12.7 23.0 17.8 20.2 C11.8 18.7 10.1 13.2 11.8 8.6 C12.7 5.8 13.9 3.6 14.9 2.4 Z"/>
</svg>"##;

const SVG_INFO: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#000000" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
  <circle cx="12" cy="12" r="9.1"/>
  <circle cx="12" cy="7.7" r="0.9" fill="#000000" stroke="none"/>
  <path d="M12 10.6 V16.8"/>
</svg>"##;

const SVG_GITHUB: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#000000"><path d="M12 .8a11.2 11.2 0 0 0-3.54 21.83c.56.1.76-.24.76-.54v-2.08c-3.12.68-3.78-1.33-3.78-1.33-.51-1.3-1.25-1.65-1.25-1.65-1.02-.7.08-.69.08-.69 1.13.08 1.73 1.16 1.73 1.16 1 1.72 2.63 1.22 3.27.93.1-.73.39-1.22.71-1.5-2.49-.28-5.11-1.25-5.11-5.54 0-1.22.44-2.22 1.15-3-.12-.29-.5-1.42.11-2.96 0 0 .94-.3 3.08 1.15A10.7 10.7 0 0 1 12 6.2c.95 0 1.9.13 2.79.38 2.14-1.45 3.08-1.15 3.08-1.15.61 1.54.23 2.67.11 2.96.71.78 1.15 1.78 1.15 3 0 4.3-2.62 5.25-5.12 5.53.4.35.76 1.03.76 2.08v3.09c0 .3.2.65.77.54A11.2 11.2 0 0 0 12 .8Z"/></svg>"##;
const SVG_GLOBE: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#000000" stroke-width="1.8" stroke-linecap="round"><circle cx="12" cy="12" r="9"/><ellipse cx="12" cy="12" rx="4" ry="9"/><path d="M3 12h18M5 6h14M5 18h14"/></svg>"##;
const SVG_EXTERNAL: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#000000" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 3h7v7M21 3 10 14M10 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-5"/></svg>"##;
const SVG_BUG: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#000000" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="7" y="7" width="10" height="14" rx="5"/><path d="m8 3 2 4m6-4-2 4M3 9l4 2m10 0 4-2M3 15h4m10 0h4M4 22l4-4m8 0 4 4M12 11v10"/></svg>"##;
const SVG_HEART: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#000000" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z"/></svg>"##;
const SVG_COFFEE: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#000000" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 8h13v10a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3ZM17 8h2a3 3 0 0 1 0 6h-2M7 2v3m4-3v3m4-3v3"/></svg>"##;
const SVG_SHIELD: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#000000" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6Z"/><path d="m8 12 3 3 5-6"/></svg>"##;
