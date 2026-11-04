# Validación del Proceso: Atribución de Cuota y Recuperación del Atajo Global

## 1. Atribución de la Cuota al Proveedor y al Modelo
- [x] El aviso del panel principal nombra al proveedor y al modelo agotados (`MiniMax (asr-1.0)`) en lugar de mencionar siempre a Gemini.
- [x] Los mensajes de error del coordinador (`DictationState.Failed`) derivan del proveedor y del modelo de la petición, no de un texto fijo.
- [x] El banner solo se muestra cuando la cuota corresponde a la selección vigente (mismo proveedor y mismo modelo normalizado).
- [x] El subtítulo del banner distingue entre respaldo local disponible y necesidad de actualizar la clave o cambiar de modelo.
- [x] `QuotaExhaustedException` ya no lleva un mensaje por defecto que nombre a Gemini.

## 2. Aislamiento de la Cuota entre Selecciones
- [x] La cuota agotada de un proveedor no bloquea a otro proveedor: al cambiar se vuelve a intentar la nube (`quotaOfOneProviderDoesNotBlockAnotherProvider`).
- [x] La cuota agotada de un modelo no bloquea a otro modelo del mismo proveedor (`quotaOfOneModelDoesNotBlockAnotherModelOfTheSameProvider`).
- [x] La ventana de enfriamiento de 60 segundos se libera sola y devuelve el control al proveedor.

## 3. Recuperación del Atajo Global
- [x] El atajo global resuelve el coordinador vigente en el momento de la pulsación (`rememberUpdatedState`), evitando operar sobre instancias huérfanas tras cambiar de modelo, proveedor o API key.
- [x] Un estado de transcripción atascado sin petición viva se recupera en lugar de bloquear el atajo de forma permanente.
- [x] La cancelación de una petición libera el estado de transcripción de forma explícita (`doesNotRemainTranscribingAfterACancelledRequest`).
- [x] La petición lanzada por arrastre de archivos queda registrada como petición viva.

## 4. Comportamiento por Proveedor
- [x] El HTTP 429 de OpenAI-compatible se reporta como `QuotaExhaustedException` y activa el respaldo local autónomo (`reportsTooManyRequestsAsQuotaExhausted`).
- [x] `MultimodalTranscriptionProvider` propaga el proveedor y el modelo configurados a la petición.

## 5. Pruebas Automatizadas
- [x] `./gradlew :composeApp:desktopTest`: 51/51 pruebas superadas (0 fallos, 0 errores, 0 omitidas), 14 clases de prueba. Reejecución forzada (`--rerun`) el 2026-10-28 para validar el estado final del árbol de trabajo.

## 6. Empaquetado Local de la Versión 1.1.9
- [x] `scripts/release/build-release.ps1 -LocalOnly` ejecutado con éxito (pruebas, empaquetado, firma y sumas). Modo local: no toca Git ni GitHub.
- [x] Instalador que se publicará: `release/LyraFlow-Windows-1.1.9.exe` (59.905.744 bytes / 57,13 MB), compilado con Inno Setup desde `packaging/windows/`. Sustituye al instalador MSI de jpackage + WiX (72.526.192 bytes) que se preparó en la primera fase; el procedimiento de paso está en `packaging/windows/MIGRATION.md`.
- [x] Metadatos del ejecutable: `FileVersion` = `1.1.9`, `ProductVersion` = `1.1.9`, `ProductName` = `LyraFlow`, `CompanyName` = `Biglex`.
- [x] Firma de autenticode aplicada con el certificado local del proyecto y publicante `CN=biglexj` (validado por `Assert-SignedArtifact` del propio script). La cadena es autofirmada y sin timestamp, por lo que Windows reporta `UnknownError` al evaluar la confianza de la raiz; es el estado propio de las releases previas (1.1.7 y 1.1.8) y no una regresion de este ciclo.
- [x] `release/SHA256SUMS.txt` generado sin BOM y con finales de linea LF, verificable con `sha256sum -c`: `112f3272bb44c36bf10562da659e9d44be1e5740c375ae069fdc06b195a682a6`. La comprobacion cruzada (`sha256sum -c SHA256SUMS.txt`) devuelve `OK`.
- [x] La copia en el centro de releases del workspace (`12. Releases`) queda pendiente de refresco por el orquestador compartido: `Build-Installer.ps1` la deja sin firmar porque la firma se aplica despues, en el script de release.

## 7. Estado del Lanzamiento
- [x] Trabajo desarrollado en la rama `preview`; la fusion a `main`, el tag y la publicacion en GitHub quedan pendientes de la instruccion explicita de lanzamiento.
- [x] `v1.1.8` ya publicada en GitHub se mantiene inmutable; la 1.1.9 se preparó como parche siguiente.
- [ ] La version 1.1.9 sigue **sin publicar**: no existe tag `v1.1.9` ni GitHub Release. El cambio de instalador (MSI → EXE de Inno Setup) obliga a desinstalar el MSI de la 1.1.8 antes de instalar el EXE nuevo.
