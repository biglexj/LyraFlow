# Tareas: Sincronización Core, Instancia Única, Auto-Updater y EXE Único

- [x] **Tarea 1: Sincronización y Compresión de Reglas**
  - [x] 1.1 Sintetizar `base.md` para situarlo bajo 12,000 caracteres (~4,800 chars) y ratificar distribución exclusiva en EXE.
  - [x] 1.2 Actualizar `auto_updater.md` con reglas de Core-Docs, toast flotante (4s), cero fricción y formato EXE exclusivo.
  - [x] 1.3 Actualizar `desktop_app_standards.md` con especificaciones de Single Instance Lock (bypass dev y liberación previa a update) y distribución EXE.

- [x] **Tarea 2: Lógica de Instancia Única (SingleInstanceLock)**
  - [x] 2.1 Refactorizar `SingleInstanceLock.kt` con bypass `isDevMode`, listener IPC reactivo y cola para órdenes previas a la creación de ventana.
  - [x] 2.2 Integrar reactivación robusta y manejo de ventana en `Main.kt` con APIs Win32 (`ShowWindow SW_RESTORE`, `SetForegroundWindow`).

- [x] **Tarea 3: Motor de Actualizaciones In-App y Distribución EXE**
  - [x] 3.1 Actualizar `UpdateChecker.kt` para parsear JSON con `kotlinx.serialization` y priorizar específicamente el instalador `.exe`.
  - [x] 3.2 Actualizar `AutoDownloader.kt` con nombre `.exe`.
  - [x] 3.3 Actualizar `InstallerUtils.desktop.kt` con liberación de lock, modo pasivo (`/passive`) y relanzamiento automático.
  - [x] 3.4 Actualizar pruebas unitarias en `UpdateCheckerTest.kt` verificando la priorización del ejecutable sobre `SHA256SUMS.txt`.

- [x] **Tarea 4: Interfaz de Usuario (Toast Flotante y Modales)**
  - [x] 4.1 Implementar Toast Flotante Centrado Superior (4s con animación fade+slide) en `LyraFlowApp.kt`.
  - [x] 4.2 Ajustar flujo de verificación manual en `AboutDialog.kt` y `LyraFlowApp.kt` para cierre fluido y transición síncrona.

- [x] **Tarea 5: Verificación y Cierre**
  - [x] 5.1 Ejecutar suite `:composeApp:desktopTest` (100% de tests exitosos).
  - [x] 5.2 Completar `VALIDATION.md` y `APPROVAL.md`.
