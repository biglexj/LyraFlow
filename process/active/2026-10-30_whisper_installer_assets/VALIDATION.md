# Instalador de Whisper local — Validación

- Estado: `PENDING`

## Comprobaciones

- [x] V01 — Agente — Consultar `https://api.github.com/repos/ggml-org/whisper.cpp/releases/latest`. Esperado: `"assets":[]`, es decir, la release estable vigente no publica binarios.
- [x] V02 — Agente — Consultar `https://api.github.com/repos/ggml-org/whisper.cpp/releases?per_page=30` y resolver con `WhisperReleaseAssets.selectWindowsPackage`. Esperado: `https://github.com/ggml-org/whisper.cpp/releases/download/b5454/whisper-bin-x64.zip`.
- [x] V03 — Agente — `HEAD` sobre la URL resuelta. Esperado: estado HTTP 200.
- [x] V04 — Agente — Descargar `whisper-bin-x64.zip` (b5454) y listar su contenido. Esperado: incluye `Release/whisper-cli.exe`.
- [x] V05 — Agente — `HEAD` sobre las URL de respaldo (`b5454` y `v1.9.0`). Esperado: ambas respond 200 y ambos ZIP contienen `Release/whisper-cli.exe`.
- [x] V06 — Agente — `GET` sobre las URLs de modelo de HuggingFace (`ggml-tiny.bin`, `ggml-base.bin`, `ggml-small.bin`). Esperado: HTTP 200 en las tres.
- [x] V07 — Agente — `./gradlew :composeApp:desktopTest`. Esperado: suite completa en verde, incluidos los 4 casos de `WhisperReleaseAssetsTest`.
- [x] V08 — Agente — Ejecutar el instalador real (`WhisperInstaller.install(WhisperModel.Tiny)`) con `LOCALAPPDATA` redirigido a una carpeta temporal. Esperado: estado «Whisper Tiny listo», `whisper-cli.exe` extraído en `runtime/Release/` y `ggml-tiny.bin` (77 MB) descargado.
- [ ] V09 — Tester — En la app, pulsar «Instalar Whisper local» con el modelo Tiny. Esperado: progreso de «Descargando Whisper» y, al terminar, el estado «Whisper Tiny listo».
- [ ] V10 — Tester — Repetir la instalación con la red bloqueada o con la API de GitHub limitada. Esperado: mensaje en español explicando que no se encontró el paquete (sin «Collection contains no element»).

## Registro de fallos

- Fallo técnico → crear o reabrir una tarea.
- Plan incorrecto → regresar a `PLAN.md`.
- Entorno bloqueado → registrar el bloqueo sin marcar la validación.
