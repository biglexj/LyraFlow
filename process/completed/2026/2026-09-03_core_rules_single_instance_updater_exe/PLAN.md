# Sincronización Core: Instancia Única, Auto-Updater y Distribución Exclusiva EXE — Plan

- Estado: `ACTIVE`
- Fecha: `2026-09-03`
- Proyecto: `LyraFlow`

## Objetivo

Sincronizar LyraFlow con los estándares oficiales de Core-Docs y Luna Fetch, garantizando la instancia única (Single-Instance Lock), auto-actualización in-app de fricción cero, y distribución exclusiva en formato ejecutable EXE.

## Alcance

- Incluye:
  - Sincronización y compresión de `.agents/rules/base.md` (< 12,000 caracteres) y actualización de `auto_updater.md` y `desktop_app_standards.md`.
  - Mecanismo robusto de `SingleInstanceLock` con bypass `isDevMode`, listener IPC reactivo y reactivación nativa de ventana.
  - Actualización de `UpdateChecker` para seleccionar exclusivamente el asset `.exe` de GitHub Releases.
  - Actualización de `InstallerUtils.desktop.kt` para liberación de socket, instalación pasiva (`/passive`) y auto-relanzamiento del ejecutable instalado.
  - Integración visual de Toast Flotante Centrado Superior (4s) en `LyraFlowApp` y transición fluida desde `AboutDialog`.
  - Pruebas unitarias para `UpdateChecker`.
- No incluye:
  - Generación de paquetes MSI o MSIX (deprecados para este proyecto).

## Enfoque

1. Crear proceso activo y actualizar las reglas en `.agents/rules/`.
2. Actualizar la lógica de `SingleInstanceLock` y cableado en `Main.kt`.
3. Actualizar el motor de actualización (`UpdateChecker`, `AutoDownloader`, `InstallerUtils`).
4. Integrar UI de Toast flotante y flujos de diálogo en `LyraFlowApp` y `AboutDialog`.
5. Ejecutar y validar tests unitarios (`:composeApp:desktopTest`).

## Criterios de finalización

- [ ] `.agents/rules/base.md` sintetizado y por debajo de 12,000 caracteres.
- [ ] Reglas `auto_updater.md` y `desktop_app_standards.md` alineadas a Core-Docs y distribución EXE.
- [ ] `SingleInstanceLock` con bypass dev, despacho a primer plano y liberación adecuada.
- [ ] Detección y descarga exclusiva de assets `.exe` y relanzamiento sin fricción en escritorio.
- [ ] Toast flotante superior visible ante verificación manual al día.
- [ ] Suite de pruebas `:composeApp:desktopTest` superada al 100%.

## Autorización

- [x] Plan aprobado para ejecución por el usuario.
