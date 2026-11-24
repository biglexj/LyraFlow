//! Consulta de releases estables; no descarga ni ejecuta instaladores.
use semver::Version;
use serde::Deserialize;
use std::{io::Read, time::Duration};

const ENDPOINT: &str = "https://api.github.com/repos/biglexj/LyraFlow/releases/latest";

#[derive(Clone, Debug, PartialEq)]
pub enum Outcome {
    Current(String),
    Available(String),
    Ahead(String),
    NoRelease,
}

#[derive(Default)]
pub enum State {
    #[default]
    Idle,
    Checking,
    Done(Outcome),
    Failed(String),
}

#[derive(Deserialize)]
struct Release {
    tag_name: String,
    draft: bool,
    prerelease: bool,
}

pub fn parse_release(body: &str, installed: &str) -> Result<Outcome, String> {
    let release: Release = serde_json::from_str(body)
        .map_err(|_| "GitHub devolvió una respuesta de versión no válida.".to_string())?;
    if release.draft || release.prerelease {
        return Err("La respuesta no corresponde a una versión estable publicada.".into());
    }
    let remote = Version::parse(release.tag_name.trim().trim_start_matches('v'))
        .map_err(|_| "La versión publicada tiene un formato desconocido.".to_string())?;
    let local = Version::parse(installed).map_err(|_| "No se pudo leer la versión instalada.".to_string())?;
    if !remote.pre.is_empty() { return Err("La versión recibida es preliminar.".into()); }
    let remote = Version { build: semver::BuildMetadata::EMPTY, ..remote };
    let local = Version { build: semver::BuildMetadata::EMPTY, ..local };
    Ok(match remote.cmp(&local) {
        std::cmp::Ordering::Greater => Outcome::Available(remote.to_string()),
        std::cmp::Ordering::Equal => Outcome::Current(remote.to_string()),
        std::cmp::Ordering::Less => Outcome::Ahead(remote.to_string()),
    })
}

pub fn check(installed: &str) -> Result<Outcome, String> {
    let client = zed_reqwest::blocking::Client::builder().timeout(Duration::from_secs(15))
        .user_agent(concat!("LyraFlow/", env!("CARGO_PKG_VERSION")))
        .build().map_err(|_| "No se pudo iniciar la consulta de actualizaciones.".to_string())?;
    let response = client.get(ENDPOINT).header("Accept", "application/vnd.github+json")
        .send().map_err(|_| "No se pudo conectar con GitHub. Revisa tu conexión y vuelve a intentarlo.".to_string())?;
    if response.status().as_u16() == 404 { return Ok(Outcome::NoRelease); }
    if !response.status().is_success() {
        return Err(format!("GitHub no permitió comprobar las actualizaciones (HTTP {}). Reinténtalo más tarde.", response.status().as_u16()));
    }
    let mut body = String::new();
    response.take(1_048_577).read_to_string(&mut body)
        .map_err(|_| "No se pudo leer la respuesta de GitHub.".to_string())?;
    if body.len() > 1_048_576 { return Err("La respuesta de actualizaciones es demasiado grande.".into()); }
    parse_release(&body, installed)
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn distinguishes_new_equal_and_older_releases() {
        let release = |tag| format!(r#"{{"tag_name":"{tag}","draft":false,"prerelease":false}}"#);
        assert_eq!(parse_release(&release("v2.0.1"), "2.0.0"), Ok(Outcome::Available("2.0.1".into())));
        assert_eq!(parse_release(&release("v2.0.0"), "2.0.0"), Ok(Outcome::Current("2.0.0".into())));
        assert_eq!(parse_release(&release("v1.1.9"), "2.0.0"), Ok(Outcome::Ahead("1.1.9".into())));
        assert_eq!(parse_release(&release("v2.0.0+ci"), "2.0.0"), Ok(Outcome::Current("2.0.0".into())));
    }
    #[test]
    fn rejects_invalid_or_unpublished_data() {
        for body in ["{}", "not json", r#"{"tag_name":"v3.0.0","draft":true,"prerelease":false}"#,
            r#"{"tag_name":"v3.0.0-beta.1","draft":false,"prerelease":false}"#] {
            assert!(parse_release(body, "2.0.0").is_err());
        }
    }
}
