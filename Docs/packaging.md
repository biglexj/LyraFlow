# Empaquetado de LyraFlow

Windows se distribuye exclusivamente con Inno Setup: `LyraFlow-Windows-X.Y.Z.exe`.
La versión debe coincidir en `Cargo.toml` (`version` y `version_code`) y `package.json` antes de ejecutar el pipeline.

## Preparar sin publicar

`mise release` prepara la entrega local. Los alias de `package.json` y Mise apuntan directamente al pipeline de `scripts/release/`, sin lanzadores en la raíz.
El pipeline oficial está en `scripts/release/build-release.ps1`; ejecuta `cargo test --locked`, compila `target/release/LyraFlow.exe` (`cargo build --locked --release`), prepara el staging en `build/inno-staging/app/`, compila Inno Setup, firma el EXE y crea `SHA256SUMS.txt`.

`-SkipBuild` solo reutiliza un staging cuyas huellas coincidan con el código y todos sus archivos. La evidencia se guarda en `build/app-image-identity.json`; no se acepta forzar una imagen obsoleta.
El builder no modifica la plantilla compartida ni copia ejecutables sin firmar a otras carpetas del workspace.

## Publicación explícita

- `mise publish --plan`: describe el flujo sin procesos hijos, red ni escrituras.
- `mise publish --check`: comprueba requisitos en modo de lectura; devuelve error si falta algo.
- `mise publish` o `npm run release:publish`: ejecuta el pipeline oficial con `-Publish -Channel all`.
- `mise publish github`: publica únicamente en GitHub.
- `scripts/release/build-release.ps1 -Publish -Channel github`: acceso directo al mismo pipeline.

Se exige una rama `main` o `preview` limpia que pueda avanzar `origin/main` sin sobrescribir su historial, una versión inédita y autenticación de GitHub CLI. El script no incorpora cambios sin revisar mediante `git add -A` ni crea commits de trabajo. Hace push atómico de `main` y del tag nuevo, publica la release y descarga el EXE para cotejar su SHA-256 antes de sincronizar Aurora.
Una publicación no permite omitir pruebas, build o firma.

El canal `all` requiere `LYRAFLOW_AURORA_APP_ID` (identificador real de la ficha de LyraFlow) y `AURORA_RELEASE_TOKEN` en el entorno seguro. Usa el contrato HTTP de Aurora y verifica por lectura notas, versión, contador, URL y hash. No se buscan ni se guardan credenciales en el repositorio.
Si GitHub ya se publicó y falla Aurora, hay que revisar ese destino y continuar solo la sincronización; no recrear el tag ni reemplazar assets. `Publish-AuroraRelease` está separado para permitir esa recuperación una vez verificada la release existente.

## Instalador y actualización

`packaging/windows/setup.iss` usa la plantilla visual compartida y adaptaciones locales:

- `LegacyMigration.iss`: detecta los MSI por el UpgradeCode histórico, conserva su carpeta y los retira antes de copiar el EXE nuevo. Ante un fallo de retirada, detiene la instalación con un mensaje.
- `Accessibility.iss`: campo de ruta editable, etiqueta asociada y navegación nativa con Tab y Shift+Tab.
- `identity.iss`: identidad estable de actualización (`MyAppExe = LyraFlow.exe`).
- `version.iss`: versión generada por el builder desde `Cargo.toml`.

El instalador conoce el destino, cierra instancias activas de `LyraFlow.exe` antes de copiar archivos y relanza la aplicación al finalizar.

## Alcance de la validación

Compilar, firmar y verificar hashes no demuestra la actualización real. La migración desde una instalación pública anterior (por ejemplo `1.1.9`), el reinicio, el dictado con micrófono y la revisión con lector de pantalla deben registrarse por separado. No ejecutar la migración sobre la instalación personal solo para obtener una marca de validación.

## Archivos locales

El certificado de firma se conserva fuera de la raíz, en `.local/signing/LyraFlow_Dev_Certificate.pfx` (ignorado en Git), o se indica con `LYRAFLOW_SIGNING_CERTIFICATE`. No se incluyen credenciales en documentación ni artefactos.
