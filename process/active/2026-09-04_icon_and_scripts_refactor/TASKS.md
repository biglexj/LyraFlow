# Tareas del Proceso: Reubicación de Scripts y Estandarización de Iconos

- [x] **T01**: Crear `scripts/release/build-release.ps1` con resolución `$root` relativa de 2 niveles.
- [x] **T02**: Crear script delegador en la raíz `build-release.ps1` para retrocompatibilidad.
- [x] **T03**: Actualizar reglas de agentes (`.agents/rules/auto_updater.md`, `.agents/rules/base.md`) y documentación (`README.md`, `Docs/packaging.md`).
- [x] **T04**: Crear script automatizado de sincronización `scripts/branding/Sync-Icons.ps1` conforme a Docs.
- [x] **T05**: Ejecutar `Sync-Icons.ps1` para regenerar `app_icon.ico` (multi-resolución transparente), `icon.png` y los logos en `Image/`.
- [x] **T06**: Verificar canal alfa en todos los artefactos generados (0% opacidad en fondos y esquinas).
- [x] **T07**: Ejecutar suite de pruebas `./gradlew :composeApp:desktopTest`.
- [x] **T08**: Validar ejecución en seco de `scripts/release/build-release.ps1 -LocalOnly -SkipTests -SkipBuild`.
