# Empaquetado de LyraFlow

Windows se distribuye exclusivamente con Inno Setup: `LyraFlow-Windows-X.Y.Z.exe`.
La versión debe coincidir en `gradle.properties`, `package.json` y `AppVersion.kt` antes de ejecutar el pipeline.

## Preparar sin publicar

`mise release` prepara la entrega local. Los alias de package.json y Gradle apuntan directamente al pipeline de scripts/release/, sin wrapper en la raíz.
El pipeline oficial está en `scripts/release/build-release.ps1`; ejecuta las pruebas de escritorio, genera la imagen con `createDistributable`, compila Inno Setup, firma el EXE y crea `SHA256SUMS.txt`.

`-SkipBuild` solo reutiliza una imagen cuyas huellas coincidan con el código y todos sus archivos. La evidencia se guarda en `composeApp/build/app-image-identity.json`; no se acepta forzar una imagen obsoleta.
El builder no modifica la plantilla compartida ni copia ejecutables sin firmar a otras carpetas del workspace.

## Publicación explícita

- `mise publish --plan`: describe el flujo sin procesos hijos, red ni escrituras.
- `mise publish --check`: comprueba requisitos en modo de lectura; devuelve error si falta algo.
- `mise publish` o `npm run release:publish`: ejecuta el pipeline oficial con `-Publish -Channel all`.
- `mise publish github`: publica únicamente en GitHub.
- `scripts/release/build-release.ps1 -Publish -Channel github`: acceso directo al mismo pipeline.

Se exige una rama `preview` o `main` limpia que pueda avanzar `origin/main` sin sobrescribir su historial, una versión inédita y autenticación de GitHub CLI. El script no incorpora cambios sin revisar mediante `git add -A` ni crea commits de trabajo. Hace push atómico de main y del tag nuevo, publica la release y descarga el EXE para cotejar su SHA-256 antes de sincronizar Aurora.
Una publicación no permite omitir pruebas, build o firma.

El canal `all` requiere `LYRAFLOW_AURORA_APP_ID` (identificador real de la ficha de LyraFlow) y `AURORA_RELEASE_TOKEN` en el entorno seguro. Usa el contrato HTTP de Aurora y verifica por lectura notas, versión, contador, URL y hash. No se buscan ni se guardan credenciales en el repositorio.
Si GitHub ya se publicó y falla Aurora, hay que revisar ese destino y continuar solo la sincronización; no recrear el tag ni reemplazar assets. `Publish-AuroraRelease` está separado para permitir esa recuperación una vez verificada la release existente.

## Instalador y actualización

`packaging/windows/setup.iss` usa la plantilla visual compartida y adaptaciones locales:

- `LegacyMigration.iss`: detecta los MSI por el UpgradeCode histórico, conserva su carpeta y los retira antes de copiar el EXE nuevo. Ante un fallo de retirada, detiene la instalación con un mensaje.
- `Accessibility.iss`: campo de ruta editable, etiqueta asociada y navegación nativa con Tab y Shift+Tab.
- `identity.iss`: identidad estable de actualización.
- `version.iss`: versión generada por el builder.

La app lanza Inno con argumentos separados `/SILENT /NORESTART /SP- /RESTARTAPP /LOG`. El instalador conoce el destino y relanza la app después de instalarla correctamente. Ya no se usa cmd para relanzar la ruta de una versión anterior.

WiX/MSIX y el actualizador duplicado se retiraron del código activo. La tarea compatible `packageLyraFlowInstaller` apunta a Inno Setup; las tareas de jpackage que generaban instaladores MSI/EXE antiguos fallan con un mensaje que indica el comando vigente.

## Alcance de la validación

Compilar, firmar y verificar hashes no demuestra la actualización real. La migración desde una instalación pública de 1.1.8, el reinicio, el dictado con micrófono y la revisión con lector de pantalla deben registrarse por separado. No ejecutar la migración sobre la instalación personal solo para obtener una marca de validación.

## Archivos locales y limpieza de raíz

El certificado de firma se conserva fuera de la raíz, en `.local/signing/LyraFlow_Dev_Certificate.pfx` (ignorado en Git), o se indica con `LYRAFLOW_SIGNING_CERTIFICATE`. No se incluyen credenciales en documentación ni artefactos.
Los wrappers `dev.bat` y `build-release.ps1` de la raíz fueron retirados. `gradlew` y `gradlew.bat` se conservan como parte de Gradle.
El archivo raíz context.md ya no se utiliza: las instrucciones efectivas viven en AppPreferences.kt. Su versión anterior queda en el historial Git. Los restos .NET de bin/ y obj/ se reubicaron en temp/, sin formar parte del build Kotlin.
