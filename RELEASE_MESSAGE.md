# 🎙️ LyraFlow 1.0.8

LyraFlow adopta la interfaz Material Expressive, suma la sección oficial "Acerca de" con modalidades de apoyo, comprobación de actualizaciones desde GitHub Releases y feedback transparente:

- ℹ️ **Badge y Modal "Acerca de"**: Información de versión, autoría (`biglexj`), licencia MIT y accesos a donaciones (Yape/Plin/Web), Buy Me a Coffee y GitHub.
- 🚀 **Auto-Actualizador desde GitHub Releases**: Verificación silenciosa en background y chequeo manual desde Ajustes / Acerca de.
- 🧹 **Sanitización de Release Notes**: Limpieza automática de Markdown crudo (`sanitizeMarkdown()`) para notas claras en el banner.
- 💬 **Toast Global "Estás al día"**: Notificación flotante animada (`AnimatedVisibility`) con auto-ocultado de 4 segundos al verificar manualmente sin nuevas versiones.
- 🎨 **Material Expressive UI**: Interfaz rediseñada con paleta de colores vibrantes (`#7F52FF`), formas expresivas y animaciones fluidas.
- 🔄 **Reintentos Automáticos**: Recuperación transparente de hasta 2 intentos ante fallos temporales de red o API.
- 🌐 **Preservación Multilingüe y CJK**: Prompt reforzado para respetar de forma íntegra caracteres CJK (chino y japonés), uniones y símbolos.
