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
- [x] `./gradlew :composeApp:desktopTest`: 51/51 pruebas superadas (0 fallos), 14 clases de prueba.

## 6. Empaquetado Local de la Versión 1.1.9
- [x] `scripts/release/build-release.ps1 -Version "1.1.9" -LocalOnly` ejecutado con éxito (pruebas, empaquetado, firma y sumas).
- [x] Instalador generado: `release/LyraFlow-Windows-1.1.9.exe` (72.526.192 bytes / 69,17 MB).
- [x] Firma de autenticode aplicada con el certificado local del proyecto y publicante `CN=biglexj` (validado por `Assert-SignedArtifact` del propio script). La cadena es autofirmada y sin timestamp, por lo que Windows reporta `UnknownError` al evaluar la confianza de la raiz; es el estado propio de las releases previas (1.1.7 y 1.1.8) y no una regresion de este ciclo.
- [x] `release/SHA256SUMS.txt` generado: `f0ec09b75b204abec985f0e92cec43742c95853dfd6b7151e964e8eb6072818d`.

## 7. Estado del Lanzamiento
- [x] Trabajo desarrollado en la rama `preview`; la fusion a `main`, el tag y la publicacion en GitHub quedan pendientes de la instruccion explicita de lanzamiento.
- [x] `v1.1.8` ya publicada en GitHub se mantiene inmutable; la 1.1.9 se preparó como parche siguiente.
