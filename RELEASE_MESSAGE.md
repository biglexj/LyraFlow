# 🎙️✨ LyraFlow 1.1.8 — MiniMax, atajo para cancelar y Ajustes en dos pestañas

¡Llegó **LyraFlow v1.1.8**! Una versión para dejar de pelearte con los ajustes y recuperar el control cuando un dictado se alarga demasiado.

### 🌟 Novedades Destacadas

- 🤖 **Nuevo proveedor MiniMax**: MiniMax se une a Gemini y OpenAI en el selector de proveedor. Usa el transcriptor dedicado de MiniMax para convertir tu voz en texto y luego lo refina con su modelo de texto, así el resultado llega limpio igual que con el resto de proveedores. Solo necesitas tu clave `MINIMAX_API_KEY`.
- ⛔ **Atajo global para cancelar el dictado**: Si se te va la mano y el audio se alarga, pulsa `Ctrl + Alt + Espacio` y LyraFlow descarta la grabación sin transcribirla ni insertar nada. También funciona mientras el texto se está procesando, para abandonar esa petición. Es configurable en Ajustes y no puede coincidir con el atajo principal.
- 🐛 **El indicador inferior vuelve a aparecer**: El pequeño medidor de estado del borde inferior a veces desaparecía y no regresaba. Ahora se vigila solo, se recupera y se mantiene al frente aunque el sistema lo envíe hacia atrás.
- 🗂️ **Ajustes reorganizados en dos pestañas**: `General` y `Modelos`. Ya no hay que hacer scroll infinito: el título y las pestañas quedan fijos arriba y cada pestaña se reparte en dos columnas.
- 🎛️ **Selector de proveedor en tres columnas**: Gemini, OpenAI / compatible y MiniMax se eligen de un vistazo, cada uno con una descripción de una sola línea.

### 🔧 Detalles técnicos

- **Transcriptor MiniMax en dos etapas**: el audio entra por `POST /speech_to_text` (modelo `asr-1.0`) y el texto obtenido se procesa después con `/chat/completions` (`MiniMax-M3.1-Flash`). Si el refinamiento falla, se conserva la transcripción original en lugar de perder el audio.
- **Respuestas a límites y saldos**: LyraFlow reconoce el error HTTP 429 de MiniMax y conmuta a **Whisper local** como respaldo, e informa con claridad cuando el audio supera los límites de MiniMax (500 segundos y 50 MB).
- **Dos atajos registrados en paralelo**: el registro global de Windows usa un identificador de hotkey independiente para el atajo de cancelar, y se reportan los que no estén disponibles en lugar de fallar en silencio.
- **Recuperación del overlay de estado**: vigilancia periódica integrada y reafirmación nativa `HWND_TOPMOST` mediante JNA.

---

*LyraFlow v1.1.8 — Más proveedores, menos scroll y siempre una salida cuando el dictado se alarga.*
