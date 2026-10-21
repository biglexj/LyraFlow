# Tareas del Proceso: Reubicación de Scripts y Estandarización de Iconos

- [x] **T01**: Crear `scripts/release/build-release.ps1` con resolución `$root` relativa de 2 niveles.
- [x] **T02**: Configurar excepción `!scripts/release/` en `.gitignore` para permitir seguimiento de scripts de release.
- [x] **T03**: Eliminar script de raíz `build-release.ps1` conforme a la solicitud explícita del usuario.
- [x] **T04**: Eliminar la carpeta `Image/` redundante en la raíz del proyecto.
- [x] **T05**: Migrar assets de runtime (`Square44x44Logo.png`, `Square150x150Logo.png`, `StoreLogo.png`, `Wide310x150Logo.png`) a `composeApp/src/desktopMain/resources/`.
- [x] **T06**: Actualizar `composeApp/build.gradle.kts` para remover la dependencia del directorio `Image/` eliminado.
- [x] **T07**: Crear y ejecutar script automatizado `scripts/branding/Sync-Icons.ps1` conforme a Core-Docs para derivar todos los iconos transparentes desde `assets/branding/icons/icon-transparent.png`.
- [x] **T08**: Regenerar `composeApp/src/desktopMain/resources/app_icon.ico` con canal alfa puro en sus 7 resoluciones (16..256).
- [x] **T09**: Actualizar reglas de agentes (`.agents/rules/auto_updater.md`, `.agents/rules/base.md` sintetizado a <5k chars) y documentación (`README.md`, `Docs/packaging.md`).
- [x] **T10**: Ejecutar suite de pruebas `./gradlew :composeApp:desktopTest` (40/40 tests en verde).
- [x] **T11**: Implementar reintento resiliente de firma con `signtool.exe` en `build-release.ps1` para mitigar bloqueos transitorios de I/O / antivirus en Windows.
- [x] **T12**: Compilar y empaquetar localmente la versión 1.1.7 (`.\scripts\release\build-release.ps1 -Version "1.1.7" -LocalOnly`).
- [x] **T13**: Validar generación de instalador firmado `release/LyraFlow-Windows-1.1.7.exe` y catálogo de sumas `release/SHA256SUMS.txt`.
