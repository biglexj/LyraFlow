# 🎙️✨ LyraFlow 1.1.3 — Fallback Autónomo Gemini/Whisper & Auto-Updater Interactivo

¡Llegó **LyraFlow v1.1.3**! Una actualización mayor en la resiliencia del sistema de transcripción y refinamiento completo del flujo de auto-actualizaciones in-app.

### 🌟 Novedades Destacadas

- ⚡ **Fallback Inteligente de 3 Intentos**: Si la API de Gemini falla por cualquier error de red o timeout no relacionado con cuota, el coordinador realiza un reintento automático con Gemini; si este segundo intento vuelve a fallar, conmuta automáticamente a **Whisper local** como tercer intento de respaldo.
- 🛑 **Modo Autónomo por Cuota Agotada (HTTP 429)**: Ante un error de cuota agotada en Gemini (`QuotaExhaustedException`), LyraFlow activa inmediatamente la transcripción autónoma con **Whisper local** y despliega un anuncio informativo en pantalla. Toda dictación posterior opera 100% en local hasta que actualices tu API Key.
- 🎛️ **Selector Interactivo de Variantes Whisper**: Posibilidad de conmutar y descargar cualquiera de los 5 modelos de Whisper local (`Tiny`, `Base`, `Small`, `Medium`, `Large`) desde el modal interactivo con indicador de modelo activo (`(Activo)`).
- 🚀 **Auto-Actualización Canónica (`UpdateModalDialog`)**: Modal interactivo al 80% de ancho con notas sanitizadas, barra de progreso de descarga en megabytes y porcentaje en vivo, más el botón `"Instalar y Reiniciar 🚀"`.
- 🖼️ **Experiencia Continua en "Acerca de"**: La comprobación de actualizaciones dentro del modal "Acerca de" no cierra la ventana y muestra el estado de forma síncrona en su sección inferior.
- 🛠️ **Inmunidad a Deadlocks GPU/Skiko**: Compatibilidad explícita de renderizado gráfica en Windows (`-Dskiko.renderApi=SOFTWARE_COMPAT`).

---
*LyraFlow v1.1.3 — Transcripción sin fricción, a la velocidad de tu voz.*
