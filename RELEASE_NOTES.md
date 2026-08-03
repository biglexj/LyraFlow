# 🎙️✨ LyraFlow — Historial de Versiones

📌 **Versión actual: `1.1.3` · Versión mínima requerida: `1.0.0`**

> [!IMPORTANT]
> **Regla del .9 para Versionado:**
> - Nunca se debe pasar de una versión de parche `.9` (ej. de `1.0.9` no se pasa a `1.0.10`). Al alcanzar el límite del parche `.9`, se incrementa el número menor/secundario (ej. pasando a `1.1.0`).
> - De igual manera, al alcanzar el límite de la versión menor `1.9.9` (o ante hitos de arquitectura significativos posteriores a `1.9.x`), se debe saltar obligatoriamente al siguiente número mayor completo, pasando a **`2.0.0`**. No se permiten números como `1.9.10` o `1.10.x`.
> - **Nombres de Dulces para Versiones Mayores:** Cada versión mayor debe nombrarse con un nombre de dulce o postre al estilo de las versiones clásicas de Android en orden alfabético. El nombre debe reflejarse coordinadamente en el `README.md`, en este archivo y, cuando corresponda, en el `package.json` del frontend.

## [1.1.3] - 2026-08-03

LyraFlow v1.1.3 introduce la conmutación autónoma por agotamiento de cuota, sistema de reintentos inteligentes y el motor canónico de auto-actualización in-app:
- **Fallback Inteligente de 3 Intentos**: Reintento automático transparente en Gemini ante fallos temporales de red/timeout; si el segundo intento con Gemini falla, conmuta automáticamente a **Whisper local** como 3er intento de respaldo.
- **Transcripción Autónoma por Cuota Agotada (HTTP 429)**: Al detectar `QuotaExhaustedException`, conmuta inmediatamente a Whisper local y opera de forma autónoma hasta renovar la clave API.
- **Selector Interactivo de Variantes Whisper**: Gestión y descarga entre los 5 modelos de Whisper local (`Tiny`, `Base`, `Small`, `Medium`, `Large`) con distintivo de modelo activo.
- **Auto-Actualización Canónica (`UpdateModalDialog`)**: Modal adaptativo al 80% de ancho con notas sanitizadas, barra de progreso de descarga en tiempo real en MB/% y auto-instalación/reinicio.
- **Experiencia Continua en "Acerca de"**: Comprobación síncrona dentro del modal de "Acerca de" sin cierre de ventana.
- **Inmunidad a Deadlocks GPU/Skiko**: Compatibilidad explícita de renderizado gráfica en Windows (`-Dskiko.renderApi=SOFTWARE_COMPAT`).

## [1.1.2] - 2026-08-02

LyraFlow v1.1.2 optimiza el motor de auto-actualización silencioso con cierre forzado de instancias activas y auto-reinicio sin fricción:
- **Cierre Silencioso de Instancia Previa**: El instalador finaliza limpiamente cualquier proceso activo `LyraFlow.exe` antes de proceder con la actualización pasiva, previniendo bloqueos de archivos en uso.
- **Auto-Reinicio Automático de Ejecutable**: Cadena de ejecución en segundo plano que espera el término del instalador para volver a abrir automáticamente la aplicación recién actualizada.
- **Sincronización de Plantillas Maestras**: Actualización de límites de tamaño de archivo (800-1200 líneas) e integración con los estándares del ecosistema `biglexj`.

## [1.1.1] - 2026-08-02

LyraFlow alcanza la versión menor 1.1.0 incorporando dictado multimodal inteligente, atajo global de voz original, auto-actualizador silencioso in-app, garantía de instancia única optimizada y persistencia completa de interfaz:
- **Pestañas de System Prompt (*Voz Original* vs *Inteligente*)**: Selector en formato pill M3 Expressive para alternar entre transcripción inteligente procesada por IA o transcripción literal (*Voz original*) sin refinar ni modificar palabras del usuario.
- **Auto-Migración Reactiva al Ingresar API Key**: Al pegar o escribir una clave API por primera vez, la interfaz migra automáticamente de *Voz original* a *Inteligente* respetando las preferencias posteriores del usuario.
- **Auto-Actualización In-App Silenciosa (Fricción Cero)**: Comprobación background al iniciar la app. Al pulsar *"Actualizar ahora"*, descarga el paquete ejecutable en segundo plano mostrando el progreso real en MB/porcentaje y ofrece el botón *"Instalar y Reiniciar 🚀"* para actualizar en caliente de forma totalmente pasiva y silenciosa.
- **Single-Instance Lock con Socket Listener Ping & Bypass Dev**: El sistema de instancia única previene duplicados en producción enfocando la ventana activa al relanzar la app, reconociendo el flag `-Dlyraflow.dev=true` para permitir desarrollo y pruebas simultáneas.
- **Persistencia de Dimensiones y Estado de Ventana (Sección 5)**: Memoria transparente de ancho, alto y estado de maximizado entre sesiones para restaurar exactamente la geometría elegida por el usuario.
- **Catálogo Oficial GPT-5.6 (OpenAI / Compatible)**: Actualización de modelos predeterminados a la serie GPT-5.6 (`gpt-5.6-luna`, `gpt-5.6-terra`, `gpt-5.6-sol`, `gpt-audio-5.6`) con migración automática de claves y nombres antiguos guardados en preferencias.
- **Importación Directa por Arrastre (Drag & Drop)**: Soporte completo para arrastrar archivos de audio (`.wav`, `.mp3`, `.m4a`, `.ogg`) directamente sobre la ventana principal para iniciar la transcripción instantánea.
- **Pestaña de Historial Temporal & Retención Configurable**: Persistencia local en memoria/SQLite con vista dedicada de dictados recientes, borrado individual, copia en 1 clic y selector de retención (24 horas, 7 días, 30 días o ilimitado).
- **Integración con Feedback Center**: Botón en modal "Acerca de" con enlace directo a GitHub Issues para reporte transparente de errores y sugerencias.

## [1.0.9] - 2026-07-27

LyraFlow mejora la detección del tema oscuro nativo en tiempo real y rediseña el selector de apariencia con estética Material 3 Expressive:
- **Detección Dinámica de Modo Oscuro Nativo**: Integración directa con el Registro del Sistema Operativo en tiempo real para alternar al modo oscuro de forma instantánea sin necesidad de reiniciar la aplicación al cambiar las preferencias del sistema.
- **Rediseño del Selector de Apariencia (Pill Container)**: Reemplazo de los selectores tradicionales por una barra de navegación segmentada con bordes suaves (*Material 3 Expressive*) e iconos ilustrativos (Automático, Claro, Oscuro).
- **Actualización de Reglas y Estándares del Sistema**: Consolidación del marco de trabajo y guía de desarrollo de interfaz multiplataforma.

## [1.0.8] - 2026-07-25

LyraFlow adopta la interfaz Material Expressive, suma la sección oficial "Acerca de" con modalidades de apoyo, comprobación de actualizaciones desde GitHub Releases y feedback transparente:
- **Badge y Diálogo "Acerca de la Aplicación"**: Nueva ventana con información de versión, autoría (`biglexj`), licencia MIT y enlaces de apoyo directo (`https://www.biglexj.com/donaciones`), Buy Me a Coffee y GitHub.
- **Auto-Actualizador desde GitHub Releases**: Sistema de comprobación silenciosa al iniciar la aplicación y verificación manual con descarga o redirección a releases de GitHub.
- **Sanitización de Changelogs**: Limpieza automática de Markdown crudo (`sanitizeMarkdown()`) para mostrar notas de versión comprensibles y sin código/formato roto en el `UpdateBanner`.
- **Toast Global "Estás al día"**: Notificación flotante no intrusiva con animación `AnimatedVisibility` (fade + slide) que se auto-descarta a los 4 segundos al verificar manualmente sin nuevas versiones disponibles.
- **Material Expressive UI**: Implementación del nuevo lenguaje de diseño M3 Expressive con paleta de colores vibrantes (`#7F52FF`), geometrías suaves (`16.dp`–`36.dp`) y micro-animaciones fluidas.
- **Reintentos Automáticos de Transcripción**: Sistema de auto-recuperación transparente que ejecuta un máximo de 2 intentos en caso de fallos temporales de red o de la API de transcripción.
- **Preservación Multilingüe y CJK**: Refuerzo de las instrucciones prompt para conservar de forma íntegra caracteres CJK (chino, japonés), símbolos y uniones de texto sin traducir ni omitir fragmentos.

## [1.0.7] - 2026-07-24

LyraFlow amplía la configuración de proveedores y facilita el cambio de modelo desde la pantalla principal:
- **Selector de modelos en Inicio**: la tarjeta del proveedor abre el modal de API cuando falta la clave y el selector de modelos cuando ya está configurada.
- **Proveedores multimodales extensibles**: Gemini y OpenAI/compatible comparten una configuración editable para conectar APIs, gateways y modelos compatibles con audio.
- **Claves y preferencias por proveedor**: cada proveedor conserva su clave, endpoint y modelo sin sobrescribir la configuración de los demás.
- **Mantenimiento**: se retiró el archivo local de instrucciones del agente y se reforzó la cobertura de pruebas de preferencias y transcripción.

## [1.0.6] - 2026-07-20

LyraFlow añade planes de contingencia para mejorar la robustez frente a errores de la API en la nube (Gemini) y doble pulsación accidental del atajo de teclado:
- **Planes de Contingencia ante Fallos**: El audio grabado se mantiene en memoria cuando ocurre un error en la nube (como timeouts). Se presentan dos botones en la UI para reintentar transcribir usando Gemini o Whisper local (si está instalado).
- **Prevención de Interrupciones**: El atajo de teclado `Ctrl + Espacio` se bloquea/ignora mientras se está procesando activamente la transcripción para evitar que se reinicie el dictado de forma accidental.
- **Adaptador de Whisper local**: Soporte completo para Whisper local en desktop mediante una implementación dedicada de `TranscriptionProvider`.
- **Proveedores multimodales extensibles**: Gemini usa el catálogo actualizado y LyraFlow incorpora un adaptador OpenAI-compatible configurable para OpenAI, gateways y modelos compatibles con `input_audio`, con claves separadas por proveedor.

## [1.0.5] - 2026-07-19

Una actualización centrada en optimizar el menú de la bandeja, dar sensibilidad al micrófono en el indicador flotante y refinar la visualización de los iconos:
- **Menú del Sistema Compacto y con Auto-Cierre**: Rediseñado al estilo nativo moderno con iconos vectoriales, soporte de temas claro/oscuro de Windows, atajos ESC/Space y cierre automático al hacer clic fuera de la ventana.
- **Visualizador de Onda Reactivo**: El visualizador flotante ahora responde dinámicamente a la amplitud/volumen real del micrófono (onda calmada en silencio, animada al hablar).
- **Iconos Transparentes**: Restauración del fondo transparente del icono en la bandeja de sistema y la barra de título de la aplicación.

## [1.0.4] - 2026-07-18

LyraFlow incorpora un indicador flotante transparente para distinguir cuándo está listo, escuchando o transcribiendo, y recupera el foco de la última aplicación externa antes de insertar el resultado. También permite iniciar con Windows minimizado, elegir entre Tiny, Base, Small, Medium y Large para Whisper local, y conservar la clave de Gemini cifrada para el usuario actual.

La ventana principal gana espacio a 1210 × 870 dp; su icono de apariencia alterna entre sistema, sol y luna, y los estados hover respetan los bordes redondeados.

## [1.0.3] - 2026-07-16

La clave de Gemini ahora se conserva cifrada para el usuario actual de Windows y deja de perderse al salir. El menú de bandeja adopta la identidad turquesa de LyraFlow, con estados hover y esquinas suavemente redondeadas.

## [1.0.2] - 2026-07-16

La X vuelve a ocultar LyraFlow en la bandeja del sistema en lugar de terminar el proceso. El atajo global continúa disponible sin ocupar espacio en la barra de tareas, y el menú del icono permite restaurar la ventana o salir completamente.

## [1.0.1] - 2026-07-16

LyraFlow migra oficialmente a Kotlin Multiplatform con una interfaz Material 3 Expressive, temas claro/oscuro, dictado mediante hotkey, telemetría visible del micrófono y transcripción contextual con Gemini. La implementación heredada WPF fue retirada.

La distribución oficial de Windows incluye instaladores EXE, MSI y MSIX firmados, todos generados desde una misma versión y acompañados por hashes SHA-256. El builder valida, etiqueta, sincroniza y publica automáticamente la release en GitHub.

El atajo predeterminado es `Ctrl + Espacio`. Puede grabarse otra combinación de 2 o 3 teclas desde Ajustes; el cambio se guarda y se aplica inmediatamente.

## [1.0.0] - 2026-03-26

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
*LyraFlow v1.1.0 — Transcripción sin fricción, a la velocidad de tu voz.*
