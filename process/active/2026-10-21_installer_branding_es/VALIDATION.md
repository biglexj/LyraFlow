# Personalización del instalador EXE (branding, español e «Iniciar LyraFlow») — Validación

- Estado: `PENDING`

> **Actualización de alcance (2026-10-26).** El instalador que se distribuye desde la 1.1.9 es el **EXE autónomo de Inno Setup** (`release\LyraFlow-Windows-1.1.9.exe`), generado con la plantilla de `packaging/windows/`. La plantilla WiX de `package/windows/` y su tarea `:composeApp:packageLyraFlowInstaller` siguen en el repositorio como camino MSI **no publicado** (ver `packaging/windows/MIGRATION.md`). Las comprobaciones V08 a V10 se ajustan al artefacto que se publica.

## Comprobaciones

- [x] V01 — Agente — `:composeApp:packageLyraFlowInstaller` genera el EXE. Esperado: `BUILD SUCCESSFUL` y `composeApp/build/compose/binaries/main/exe/LyraFlow-1.1.9.exe` (~72 MB).
- [x] V02 — Agente — Extraer el MSI incrustado del EXE y consultar la tabla `Property`. Esperado: `ProductLanguage = 1034`, `ARPPRODUCTICON = LyraFlowIcon`, `WIXUI_EXITDIALOGOPTIONALTEXT = "LyraFlow se ha instalado correctamente."` y `WIXUI_EXITDIALOGOPTIONALCHECKBOXTEXT = "Iniciar LyraFlow al terminar"`.
- [x] V03 — Agente — Consultar la tabla `ControlEvent` del MSI. Esperado: fila `ExitDialog | Finish | DoAction | LaunchLyraFlow | WIXUI_EXITDIALOGOPTIONALCHECKBOX=1`.
- [x] V04 — Agente — Exportar la tabla `Binary` del MSI. Esperado: `WixUI_Bmp_Banner` (85.894 B) y `WixUI_Bmp_Dialog` (461.814 B) corresponden a los bitmaps de marca, e `Icon Id="LyraFlowIcon"` presente.
- [x] V05 — Agente — Contar los ficheros del MSI frente al instalador estándar. Esperado: 228 ficheros, incluyendo `.package`, sin pérdidas en la aplicación.
- [x] V06 — Agente — Descompilar el MSI y revisar el diálogo de carpeta existente. Esperado: título «Instalación de [ProductName]» y botones «&Sí» / «&No» en español.
- [x] V07 — Agente — Instalar en silencio un MSI de laboratorio y leer la propiedad `Installed`. Esperado: vacía, por lo que la casilla opcional del `ExitDialog` es visible.
- [ ] V08 — Tester — Ejecutar `release\LyraFlow-Windows-1.1.9.exe` en la interfaz de Windows. Esperado: instalador en español, pantalla única de entrada con la identidad visual de LyraFlow, ruta de instalación editable, tema claro/oscuro automático según Windows, y pantallas nativas de progreso y de éxito.
- [ ] V09 — Tester — Marcar «Ejecutar LyraFlow» en la pantalla final y pulsar «Finalizar». Esperado: LyraFlow se inicia desde `%LOCALAPPDATA%\Programs\LyraFlow` y la instalación termina sin el error «ended prematurely».
- [ ] V10 — Tester — Instalar la 1.1.9 en un equipo con la 1.1.8 (MSI) instalada, siguiendo `packaging/windows/MIGRATION.md`. Esperado: el MSI se desinstala sin errores, el EXE instala en `%LOCALAPPDATA%\Programs\LyraFlow` y no queda una segunda copia de la aplicación.

## Registro de fallos

- Fallo técnico → crear o reabrir una tarea.
- Plan incorrecto → regresar a `PLAN.md`.
- Entorno bloqueado → registrar el bloqueo sin marcar la validación.

Al aprobar una comprobación, cambia `[ ]` por `[x]`. Si falla, mantenla pendiente y añade una sola línea con el motivo y la tarea relacionada.
