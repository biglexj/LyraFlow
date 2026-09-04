# Plan de Proceso: Reubicación de Scripts y Estandarización de Iconos

- **Fecha de inicio**: 2026-09-04
- **Responsable**: Antigravity & biglexj
- **Objetivo**: Mover `build-release.ps1` al directorio `scripts/release/` conforme a la taxonomía y guías de Docs, y reemplazar los iconos con fondos opacos por variantes 100% transparentes en `assets/branding/icons/`, recursos de Compose Desktop e instalador.

## Alcance
1. **Reubicación de Script**:
   - `scripts/release/build-release.ps1` como fuente canónica de compilación y publicación.
   - Eliminación completa de `build-release.ps1` en la raíz para mantenerla limpia.
   - Actualización de reglas de agentes y documentación.
2. **Estandarización de Iconos**:
   - Fuente canónica: `assets/branding/icons/icon-transparent.png`.
   - Generación reproducible de `app_icon.ico` (multi-tamaño transparente), `icon.png`, `Square44x44Logo.png`, `Square150x150Logo.png`, `StoreLogo.png`, `Wide310x150Logo.png`.
   - Creación de herramienta de sincronización `scripts/branding/Sync-Icons.ps1`.
3. **Validación**:
   - Compilación y pruebas unitarias con `./gradlew :composeApp:desktopTest`.
   - Inspección matemática de canal alfa en píxeles límite.
