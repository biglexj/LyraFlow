# 📋 LyraFlow — TASKS.md (Sprint Activo: v1.1.2 Released)

## 🟢 Fase 1: Estándares de Plantillas Maestras & Auto-Updater Auto-Restart (COMPLETADO)
- [x] Implementar cierre automático de proceso previo (`taskkill /f /im LyraFlow.exe`) y cadena de auto-reinicio en `DesktopAutoUpdater.kt`.
- [x] Sincronizar reglas locales de `.agents/rules/` con plantillas maestras en `D:\Proyectos\biglexj\Scripts\templates` (Sección 6 Instaladores, GitHub Pre-Check y `build-release.ps1`).

## 🔴 Fase 2: Pendientes de Plantillas y Deuda Técnica (EN PROGRESO)
- [ ] Implementar **Sección 6 (Personalización y Localización del Instalador)**: Configurar interfaz en español (`es-PE`/`es-ES`), banner e imagen de branding oficial y metadatos de copyright `biglexj`.

## 🧪 Checklist de Verificación y Pruebas
- [x] Compilación limpia de pruebas unitarias (`.\gradlew :composeApp:desktopTest`) — `BUILD SUCCESSFUL`.
- [x] Verificación de versionado oficial a **v1.1.2** en `gradle.properties`, `SettingsScreen`, `AboutDialog`, `RELEASE_NOTES` y `RELEASE_MESSAGE`.
