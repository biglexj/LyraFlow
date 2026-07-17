# Release Notes - LyraFlow 🎙️✨

## [1.0.3] "Alfajor" - 2026-07-16

La clave de Gemini ahora se conserva cifrada para el usuario actual de Windows y deja de perderse al salir. El menú de bandeja adopta la identidad turquesa de LyraFlow, con estados hover y esquinas suavemente redondeadas.

## [1.0.2] "Alfajor" - 2026-07-16

La X vuelve a ocultar LyraFlow en la bandeja del sistema en lugar de terminar el proceso. El atajo global continúa disponible sin ocupar espacio en la barra de tareas, y el menú del icono permite restaurar la ventana o salir completamente.

## [1.0.1] "Alfajor" - 2026-07-16

LyraFlow migra oficialmente a Kotlin Multiplatform con una interfaz Material 3 Expressive, temas claro/oscuro, dictado mediante hotkey, telemetría visible del micrófono y transcripción contextual con Gemini. La implementación heredada WPF fue retirada.

La distribución oficial de Windows incluye instaladores EXE, MSI y MSIX firmados, todos generados desde una misma versión y acompañados por hashes SHA-256. El builder valida, etiqueta, sincroniza y publica automáticamente la release en GitHub.

El atajo predeterminado es `Ctrl + Espacio`. Puede grabarse otra combinación de 2 o 3 teclas desde Ajustes; el cambio se guarda y se aplica inmediatamente.

## [1.0.0] "Alfajor" - 2026-03-26

### ✨ Nuevas Características
- **Inicio Automático con Windows**: Ya puedes configurar LyraFlow para que inicie junto al sistema. Arranca minimizado en el *system tray* para una experiencia fluida.
- **Inyección Inteligente (Anti-AutoSend)**: Se implementó un nuevo sistema de inyección que utiliza `Shift + Enter` para los saltos de línea. Esto permite enviar párrafos completos y listas a aplicaciones de chat sin que el mensaje se envíe solo.
- **Reorganización de Ajustes**: El panel de ajustes ha sido rediseñado. La sección **GENERAL** ahora está al inicio para un acceso rápido al *Auto-start* y al *Atajo Global*.
- **Refinamiento de Contexto AI**: Se actualizó el formato de `context.md` para ser más robusto, permitiendo definir Tareas, Contexto, Formato y Restricciones de forma más clara para Gemini.

### 🛠️ Mejoras y Correcciones
- **Whisper Background Loading**: La inicialización de modelos Whisper ahora ocurre en un hilo secundario, eliminando bloqueos en la UI al cargar modelos pesados.
- **Gestión de Modelos**: Corregido el bug de descarga para ser insensible a mayúsculas/minúsculas en el nombre de los modelos.
- **Estabilidad de UI**: Corregidos errores de sintaxis en el `StatusOverlay` y eliminadas llaves duplicadas.
- **Logging**: Mejorado el sistema de logs con perfiles de rendimiento detallados.

---
*LyraFlow v1.0.0 "Alfajor" - Elevando la productividad vocal.*
