# 🎙️✨ LyraFlow 1.1.9 — La cuota ahora dice la verdad y el atajo siempre responde

¡Llegó **LyraFlow v1.1.9**! Esta versión corrige los dos detalles que aparecieron al usar la 1.1.8 a diario: el aviso de cuota que siempre hablaba de Gemini y el atajo que dejaba de responder después de quedarte sin saldo.

### 🌟 Novedades Destacadas

- 🎯 **El aviso de cuota dice la verdad**: Si tu cuota se agota en MiniMax, en OpenAI o en un gateway compatible, el mensaje y el aviso del panel principal nombran ese proveedor y ese modelo, no uno fijo. Antes te decía "cuota de Gemini" aunque estuvieras usando otro servicio.
- ⌨️ **El atajo vuelve a funcionar**: El atajo global de dictado se quedaba mudo después de agotar la cuota, y cambiar de modelo o de proveedor no lo revivía. Ahora el atajo siempre trabaja sobre el estado actual de la aplicación y se recupera solo si queda una transcripción a medias.
- 🔄 **Cambiar de proveedor o modelo sirve de verdad**: La cuota agotada ya no se aplica a ciegas a todo. Si cambias de proveedor o de modelo, LyraFlow vuelve a intentar la transcripción en la nube de inmediato en lugar de quedarse atascado en el modo local.
- 📢 **Avisos que corresponden**: El aviso rojo solo aparece cuando la cuota es del proveedor que tienes seleccionado, y ahora te dice con exactitud si tu respaldo local está activo o si lo que necesitas es actualizar la clave.

### 🔧 Detalles técnicos

- **Cuota con identidad**: El estado de cuota agotada registra el proveedor y el modelo que la consumieron, y la ventana de enfriamiento de 60 segundos se aplica solo a esa combinación.
- **Atajo sobre el estado vigente**: El registro global del atajo resuelve el coordinador de dictado actual en el momento de pulsarlo, evitando que la interfaz y el atajo se desincronicen al cambiar de modelo, proveedor o clave.
- **Sin estados atascados**: Una petición cancelada libera explícitamente el estado de transcripción, de modo que el atajo nunca queda inutilizable hasta reiniciar.
- **HTTP 429 unificado**: El límite de uso del protocolo OpenAI-compatible ahora se reconoce como cuota agotada y activa el respaldo con Whisper local igual que el resto de proveedores.

---

*LyraFlow v1.1.9 — Avisos que dicen la verdad y un atajo que nunca se queda mudo.*
