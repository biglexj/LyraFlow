---
trigger: always_on
---

# 🚀 Regla y Sistema de Auto-Actualización desde GitHub Releases

> [!IMPORTANT]
> LyraFlow implementa la **comprobación automática, descarga interactiva e instalación directa de actualizaciones desde GitHub Releases**, distribuyendo **exclusivamente ejecutables `.exe`** en Windows Desktop.

## Requerimientos Obligatorios

1. **Verificación Silenciosa al Iniciar**:
   - Al iniciar la aplicación, consultar la API de GitHub (`https://api.github.com/repos/biglexj/LyraFlow/releases/latest`) en segundo plano sin interrumpir al usuario.
   - **No mostrar ningún mensaje** si ya se está en la última versión.
2. **Verificación Manual**:
   - Si el usuario pulsa "Buscar actualizaciones" (en Menú o en el diálogo "Acerca de"):
     - Hay nueva versión → Cerrar el diálogo secundario y abrir síncronamente el `UpdateModalDialog` al 80% de ancho (máximo 480.dp).
     - Ya en la última versión → Mostrar un **Toast flotante centrado en la parte superior** (✅ "¡Estás en la última versión de LyraFlow!") que se desvanece automáticamente a los **4 segundos**. No usar modales bloqueantes para este caso.
3. **Transición Flotante y Cierre de Diálogos Secundarios [CRÍTICO]**:
   - Al accionar "Buscar actualizaciones" desde un diálogo (ej. "Acerca de"), la ventana flotante DEBE cerrarse en la misma acción y activar `showUpdateModal = true` de forma síncrona en el hilo principal sin parpadeos ni destellos.
4. **Modal Central Interactivo (Ancho al 80%) [CRÍTICO]**:
   - Mostrar las actualizaciones dentro de un **Modal Central Interactivo (`UpdateModalDialog`)**.
   - El diálogo DEBE usar `DialogProperties(usePlatformDefaultWidth = false)` con `fillMaxWidth(0.80f)` (máximo 480.dp) para garantizar espacio holgado sin compresión.
5. **Sanitización Canónica del `body` en Markdown [CRÍTICO]**:
   - Las notas de versión DEBEN sanitizarse mediante **`sanitizeReleaseNotes(body: String): String`** para eliminar Markdown crudo (`#`, `**`, `*`, `-`, enlaces `[]()` y bloques de código) antes de mostrarse en la UI.
   - Los ítems de lista se reemplazan por viñetas `• `. El resultado se visualiza en un contenedor desplazable legible.
6. **Distribución Exclusiva en EXE (Windows Desktop) [CRÍTICO]**:
   - LyraFlow empaqueta y distribuye **únicamente instaladores EXE** (`LyraFlow-Windows-X.Y.Z.exe`).
   - El analizador `UpdateChecker` DEBE inspeccionar la lista de `assets` de GitHub y seleccionar específicamente el archivo `.exe`, ignorando archivos de hashes (`SHA256SUMS.txt`) u otros formatos.
7. **Actualizaciones In-App de Fricción Cero (Zero-Friction Desktop Update)**:
   - El motor **`AutoDownloader`** descarga el archivo instalador `.exe` en segundo plano con reporte de progreso en tiempo real (0-100%, MB descargados / totales).
   - Al pulsar *"Instalar y Reiniciar 🚀"*, la aplicación:
     1. Libera el bloqueo de instancia única (`SingleInstanceLock.release()`).
     2. Identifica la ruta del ejecutable instalado (`ProcessHandle` o `%LOCALAPPDATA%\LyraFlow\LyraFlow.exe`).
     3. Lanza el instalador desasociado en modo pasivo (`cmd.exe /c timeout /t 2 /nobreak > nul & start /wait "" "$absPath" /passive & start "" "$currentExePath"`).
     4. Finaliza la instancia antigua inmediatamente (`exitProcess(0)`).
   - El usuario jamás debe tener que buscar en carpetas ni ejecutar instaladores manualmente.
8. **Notificación de Usuario (Toast Flotante)**:
   - Toda notificación de "Sin actualizaciones" o errores de red debe mostrarse mediante un **Toast flotante centrado en la parte superior** de la pantalla, con duración exacta de **4 segundos**.
9. **Verificación Previa Obligatoria de la Release en GitHub [CRÍTICO]**:
   - Antes de bump de versión (`versionName` / `versionCode`) o redactar notas (`RELEASE_NOTES.md`), verificar la última tag en GitHub Releases (`https://api.github.com/repos/biglexj/LyraFlow/releases/latest`). La nueva versión debe ser estrictamente superior.
10. **Script Oficial de Release `build-release.ps1` [CRÍTICO]**:
    - NUNCA publicar manualmente. Utilizar siempre:
      ```powershell
      .\build-release.ps1 -Version "X.Y.Z"
      ```
    - El script compila `:composeApp:packageExe`, firma el EXE, genera `SHA256SUMS.txt` y publica la release en GitHub de forma atómica.
    - **Título de Release**: Únicamente `LyraFlow vX.Y.Z`.
11. **Política de Versionado (PATCH por Defecto)**:
    - Toda nueva release es PATCH (`X.Y.Z → X.Y.Z+1`) salvo indicación explícita del usuario.
