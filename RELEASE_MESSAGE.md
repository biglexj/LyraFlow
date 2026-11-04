# 🎙️✨ LyraFlow v1.1.9 — La cuota ahora dice la verdad y el atajo siempre responde

¡Llegó **LyraFlow v1.1.9**! Esta versión corrige los dos detalles que aparecieron al usar la 1.1.8 a diario: el aviso de cuota que siempre hablaba de Gemini y el atajo que dejaba de responder después de quedarte sin saldo. Además, optimiza la pantalla de Ajustes con un diseño adaptativo más limpio, mejora la escala visual del dictado y renueva por completo el instalador de Windows.

### 🌟 Novedades Destacadas

- 🎨 **Ajustes y pantalla de inicio más ergonómicos**: Rediseño integral de Ajustes en dos columnas adaptativas (activas a partir de 720 dp y con escalado optimizado para monitores amplios). Los atajos de dictado y cancelación se configuran en paralelo, la pestaña de Modelos separa la conexión a proveedores del modo de transcripción, y el área principal de dictado ahora escala armónicamente según el tamaño de tu ventana.

- 🎯 **El aviso de cuota dice la verdad**: Si tu cuota se agota en MiniMax, en OpenAI o en un gateway compatible, el mensaje y el aviso del panel principal nombran ese proveedor y ese modelo, no uno fijo. Antes te decía "cuota de Gemini" aunque estuvieras usando otro servicio.
- ⌨️ **El atajo vuelve a funcionar**: El atajo global de dictado se quedaba mudo después de agotar la cuota, y cambiar de modelo o de proveedor no lo revivía. Ahora el atajo siempre trabaja sobre el estado actual de la aplicación y se recupera solo si queda una transcripción a medias.
- 🔄 **Cambiar de proveedor o modelo sirve de verdad**: La cuota agotada ya no se aplica a ciegas a todo. Si cambias de proveedor o de modelo, LyraFlow vuelve a intentar la transcripción en la nube de inmediato en lugar de quedarse atascado en el modo local.
- 📢 **Avisos que corresponden**: El aviso rojo solo aparece cuando la cuota es del proveedor que tienes seleccionado, y ahora te dice con exactitud si tu respaldo local está activo o si lo que necesitas es actualizar la clave.
- 🛠️ **Instalador nuevo, en español y con la marca de LyraFlow**: El instalador de Windows pasa a ser un único EXE con asistente en español, pantalla de entrada con el diseño de LyraFlow, tema claro u oscuro automático según Windows, instalación por usuario sin permisos de administrador y actualización en la misma carpeta. También ofrece ejecutar LyraFlow al terminar de instalar.
- 📦 **Paso desde la versión anterior**: el instalador gestiona la retirada del MSI anterior y conserva su carpeta. Si no puede completar ese paso, muestra cómo resolverlo antes de continuar.

- 🗂️ **El historial se conserva al cerrar la app**: los dictados permanecen en este equipo durante el plazo que elijas. Se mantiene el valor predeterminado de 24 horas.
- 🎙️ **Whisper con descargas de respaldo**: si falla la consulta inicial, se prueban las fuentes alternativas.
- ⌨️ **Mejor acceso con teclado**: campo de instalación editable y descripción accesible para eliminar dictados.

### 🔧 Detalles técnicos

- **Cuota con identidad**: El estado de cuota agotada registra el proveedor y el modelo que la consumieron, y la ventana de enfriamiento de 60 segundos se aplica solo a esa combinación.
- **Atajo sobre el estado vigente**: El registro global del atajo resuelve el coordinador de dictado actual en el momento de pulsarlo, evitando que la interfaz y el atajo se desincronicen al cambiar de modelo, proveedor o clave.
- **Sin estados atascados**: Una petición cancelada libera explícitamente el estado de transcripción, de modo que el atajo nunca queda inutilizable hasta reiniciar.
- **HTTP 429 unificado**: El límite de uso del protocolo OpenAI-compatible ahora se reconoce como cuota agotada y activa el respaldo con Whisper local igual que el resto de proveedores.
- **Descarga**: `LyraFlow-Windows-1.1.9.exe`, con su huella SHA-256 publicada en `SHA256SUMS.txt`.

### Soporte

- **Sitio oficial:** https://www.biglexj.com/
- **GitHub:** https://github.com/biglexj/LyraFlow

---

*LyraFlow v1.1.9 — Avisos que dicen la verdad y un atajo que nunca se queda mudo.*
