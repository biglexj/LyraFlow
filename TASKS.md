# 📋 LyraFlow — TASKS.md (Sprint Activo: v1.1.0 Released)

## 🟢 Fase 0: Arquitectura & Drag & Drop + Historial Temporal (COMPLETADO)
- [x] Implementar Listener Drag & Drop nativo AWT en `Main.kt` con acción `ACTION_COPY`.
- [x] Crear entidad `TranscriptionHistoryEntity`, `HistoryDao` y `AppDatabase` Room en capa de datos.
- [x] Implementar `HistoryRepository` y `HistoryViewModel` con purga automática según `HistoryRetentionPeriod`.
- [x] Diseñar e integrar `HistoryScreen.kt` con barra de búsqueda, copia rápida y borrado.
- [x] Adaptar `SettingsScreen.kt` con selector de retención (24h, 7 días, 30 días, ilimitado, deshabilitado).
- [x] Implementar auto-fallback offline en `Voz original` y bloqueo de modos IA (`🔒`) cuando no hay API Key configurada.
- [x] Limpiar UI y eliminar duplicación de chips de modelos.

## 🧪 Checklist de Verificación y Pruebas
- [x] Compilación limpia de pruebas unitarias (`.\gradlew desktopTest`) — `BUILD SUCCESSFUL`.
- [x] Verificación de versionado oficial a **v1.1.0** en `gradle.properties`, `SettingsScreen`, `AboutDialog`, `RELEASE_NOTES` y `RELEASE_MESSAGE`.
