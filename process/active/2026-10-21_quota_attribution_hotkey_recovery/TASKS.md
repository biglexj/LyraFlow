# Tareas del Proceso: Atribución de Cuota y Recuperación del Atajo Global

- [x] **T01**: Crear rama de trabajo `preview` desde `main` y subir la version a `1.1.9` (`gradle.properties` y `AppVersion.kt`).
- [x] **T02**: Añadir `provider` a `TranscriptionRequest` para atribuir cada peticion a su origen real.
- [x] **T03**: Crear `QuotaExhaustion` con proveedor, modelo y mensaje; normalizar el mensaje por defecto de `QuotaExhaustedException`.
- [x] **T04**: Reescribir `DictationCoordinator`: cuota acotada por proveedor y modelo, mensajes sin proveedor fijo, liberacion por cooldown y salida del estado `Transcribing` en cancelacion.
- [x] **T05**: `MultimodalTranscriptionProvider` propaga el proveedor y el modelo configurados a la peticion.
- [x] **T06**: `OpenAiCompatibleTranscriptionProvider` reporta HTTP 429 como `QuotaExhaustedException`.
- [x] **T07**: Banner de cuota en `HomeScreen` con proveedor y modelo reales, visible solo para la seleccion vigente y con descripcion honesta del respaldo.
- [x] **T08**: Propagar `quotaExhaustion` por `ShellActions` y `LyraFlowApp`.
- [x] **T09**: `Main.kt` resuelve el coordinator vigente en el atajo global y recupera estados huerfanos sin peticion viva.
- [x] **T10**: `Main.kt` incluye proveedor y modelo normalizado en cada peticion y registra la peticion por arrastre como viva.
- [x] **T11**: Actualizar y ampliar `DictationCoordinatorTest` (atribucion, aislamiento por proveedor y por modelo, cancelacion) y `OpenAiCompatibleTranscriptionProviderTest` (HTTP 429).
- [x] **T12**: Ejecutar `./gradlew :composeApp:desktopTest` y verificar la suite completa en verde.
- [x] **T13**: Actualizar `RELEASE_NOTES.md`, `RELEASE_MESSAGE.md` y `ROADMAP.md`.
- [x] **T14**: Generar el instalador local `release/LyraFlow-Windows-1.1.9.exe` con `scripts/release/build-release.ps1 -LocalOnly`.
- [x] **T15**: Cerrar el proceso activo `2026-09-04_icon_and_scripts_refactor` moviendolo a `process/completed/2026/`.
