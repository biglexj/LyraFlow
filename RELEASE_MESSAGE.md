# 🎙️ LyraFlow 1.0.6

Una actualización enfocada en robustez y planes de contingencia ante fallos en la transcripción en la nube y doble pulsación accidental del atajo.

- 🛡️ **Planes de Contingencia ante Fallos**: El audio grabado se guarda en memoria si ocurre un error o timeout en la nube. Se presentan botones en la UI para reintentar la transcripción usando Gemini o Whisper local de manera inmediata.
- 🔒 **Prevención de Doble Pulsación**: El atajo de teclado global `Ctrl + Espacio` se deshabilita/ignora mientras se está procesando activamente la transcripción, previniendo reinicios accidentales de la grabación.
- 💻 **Adaptador de Whisper local**: Soporte completo para Whisper local en desktop mediante la integración directa con `TranscriptionProvider` y ejecución sidecar offline.
