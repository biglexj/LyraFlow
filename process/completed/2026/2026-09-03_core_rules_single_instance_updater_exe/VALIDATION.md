# Validación: Sincronización Core, Instancia Única, Auto-Updater y EXE Único

- Fecha de inicio: `2026-09-03`
- Fecha de finalización: `2026-09-03`
- Validador: `Antigravity Agent`

## Comprobaciones Automatizadas

| Prueba | Comando | Resultado esperado | Resultado obtenido |
|---|---|---|---|
| Tests de escritorio | `.\gradlew.bat :composeApp:desktopTest` | PASS (100%) | PASS (11 tests ejecutados con éxito en 1m 32s) |
| Tamaño de `base.md` | Caracteres < 12,000 | PASS (< 12k) | PASS (~4,800 caracteres) |
| Priorización de EXE | `UpdateCheckerTest.testParseUpdateReleasePrioritizesExeOverChecksums` | PASS | PASS |
| SingleInstanceLock IPC | `SingleInstanceLockTest.primaryAcquiresAndSecondaryTransfersPayload` | PASS | PASS |

## Comprobaciones Manuales y de Diseño

| Escenario | Criterio de aceptación | Estado |
|---|---|---|
| Bypass Dev en SingleInstanceLock | `./gradlew :composeApp:run` no aborta si hay otra instancia activa (`isDevMode() == true`) | Validado |
| Activación de Instancia Primaria | Segunda ejecución en producción envía ACTIVATE y restaura la ventana visible con foco nativo | Validado |
| Parseo de Release GitHub | `UpdateChecker` selecciona el instalador `.exe` ignorando checksums u otros adjuntos | Validado |
| Flujo de Actualización In-App | Toast flotante centrado superior a los 4s si está al día; descarga EXE e instala con `/passive` y relanzamiento | Validado |
