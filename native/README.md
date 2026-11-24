# LyraFlow — aplicación nativa Rust + GPUI

Aplicación nativa LyraFlow 2.0.0, confirmada por Biglex como estable el 2026-11-19. Los nombres internos y rutas con gpui-demo se conservan por compatibilidad de datos y comandos. Esta corrección no publica ni instala una versión.

Desde la raíz del repositorio:

```powershell
mise dev
mise check
mise test
mise build
```

`dev` abre la ventana nativa. `build` genera `native/target/release/lyraflow-gpui-demo.exe` en Windows. Todas las tareas usan `Cargo.lock`; `--plan` muestra el comando sin ejecutarlo. No hay instalador ni publicación para la demo.

## Alcance

- Interfaz completa de LyraFlow en Rust + GPUI (`Inicio`, `Historial`, `Ajustes` y `Acerca de`), con diseño adaptable (`LyraNavigationRail` y `LyraNavigationBar`).
- Captura de audio real mediante CPAL con conversión a WAV PCM de 16 bits mono a `16 kHz` para transcripción, nivel en vivo y límite de cinco minutos.
- Arrastrar y soltar archivos de audio o imagen sobre la vista **Inicio** para transcribirlos o extraer texto (OCR).
- Proveedores en la nube (**Gemini**, **OpenAI / compatible** y **MiniMax** con transcripción `asr-1.0` y refinamiento posterior) con descubrimiento dinámico de modelos mediante escaneo contra la API (sin listas de modelos fijas para Gemini ni OpenAI-compatible).
- Almacenamiento cifrado de claves API por proveedor con **DPAPI** en Windows (`%LOCALAPPDATA%\LyraFlow-gpui-demo\secrets.json`) y respaldo en variables de entorno (`LYRAFLOW_API_KEY`, `GEMINI_API_KEY`, `OPENAI_API_KEY`, `MINIMAX_API_KEY`).
- Coordinación de dictado con 2 reintentos, detección de cuota agotada (`HTTP 429`) por `(proveedor, modelo)` con enfriamiento de 60 s y respaldo automático con **Whisper local** (`whisper-cli.exe`, instalación y descarga de modelos bajo `%LOCALAPPDATA%\LyraFlow\whisper`).
- Atajos globales de Windows (`RegisterHotKey`), editor interactivo en **Ajustes**, inyección de texto en la aplicación activa (`SendInput`), arranque con Windows e historial persistente en JSON con retención configurable.

## Diseño y apariencia

La interfaz replica el diseño de la aplicación de Compose en Rust + GPUI: barra de navegación lateral (`LyraNavigationRail` o barra inferior por debajo de `720 px`), vistas **Inicio** (`DictationHero` con visualizador de 17 barras, **Último resultado** y tarjetas de estado), **Historial** y **Ajustes** (`General` y `Modelos`), más el acceso **Acerca de**.

- **Aurora** es el tema predeterminado y usa los tokens globales de Core (`biglex-app-base`, versión `1.0.0`). La copia consumida por la compilación está en [`assets/tokens/biglex-app-base.json`](assets/tokens/biglex-app-base.json); su origen y su procedimiento de actualización, en [`assets/tokens/README.md`](assets/tokens/README.md). La adaptación a roles de GPUI vive en [`src/theme.rs`](src/theme.rs) y una prueba comprueba que no se desvía de la copia.
- **Clásico** conserva literalmente la paleta de [`LyraFlowTheme.kt`](../composeApp/src/commonMain/kotlin/com/biglexj/lyraflow/core/theme/LyraFlowTheme.kt) (`#7F52FF` / `#B499FF`, fondos y textos propios).
- El selector de color (**Aurora** / **Clásico**) y modo (**Automático** / **Claro** / **Oscuro**) vive en **Ajustes → General → Apariencia**; el botón **Tema** de la barra lateral recorre los modos igual que la aplicación de Compose.
- Las preferencias de la demo se guardan en un archivo propio (`%LOCALAPPDATA%\LyraFlow-gpui-demo\appearance.txt` en Windows). No lee ni escribe las preferencias de la instalación de Compose.

## Acerca de

El acceso **Acerca de** presenta un diálogo compacto con cabecera y cierre, logo oficial de LyraFlow, versión del paquete, plataforma, licencia y autoría. Sigue la distribución de la referencia de Prisma Upscaler, con apoyo oficial, enlaces propios y licencia MIT real desplegable. Los botones de apoyo se apilan en ventanas estrechas y el contenido puede desplazarse cuando falta altura. Se cierra con ×, Esc o pulsando fuera del diálogo. No muestra estados de actualización sin comprobar.

El bloque intermedio **Actualizaciones de LyraFlow** consulta las releases estables públicas de GitHub al iniciar y al pulsar **Buscar actualizaciones**, con timeout de 15 segundos y trabajo en segundo plano. Distingue versión nueva, igualdad, copia instalada posterior a la publicada, ausencia de release y errores. Los resultados solo consultan versiones; la descarga e instalación automática continúan pendientes. Cuando hay una versión nueva, se ofrece visitar la ficha del producto.

En Windows, [`src/modal_backdrop.rs`](src/modal_backdrop.rs) toma una instantánea exclusivamente del cliente de esta ventana antes de abrir el «Acerca de», la reduce, desenfoca y desatura en RAM. El fondo queda congelado mientras el modal está abierto; la instantánea se descarta al cerrarlo, no se guarda ni se transmite. Si falla la captura o en otras plataformas, se usa un fondo uniforme del tema. La ventana y la posición del contenido pueden cambiar mientras el modal está cerrado; al reabrir se toma una instantánea nueva.

## Atajos

- **Atajos globales en Windows** (`RegisterHotKey`): `Ctrl + Espacio` alterna grabar/detener y `Ctrl + Alt + Espacio` cancela el dictado en curso; ambos son editables en **Ajustes → General → Control del dictado**.
- **Atajos de ventana**: `Esc` cancela o cierra diálogos; `Ctrl + T` alterna claro/oscuro; `Tab` y `Mayús + Tab` recorren los controles con foco visible.

Pendientes de integrar en el paquete nativo respecto a la distribución instalada: bandeja del sistema, instancia única y descarga/instalación de actualizaciones (`mise release`/`publish` siguen bloqueados para `gpui`). Esta versión guarda su configuración, credenciales cifradas e historial en `%LOCALAPPDATA%\LyraFlow-gpui-demo\` sin modificar la instalación de Compose.

## Entorno

Rust estable con Cargo, herramientas de compilación MSVC y Windows SDK (`fxc.exe`) en Windows. Dependencias directas fijadas: GPUI `0.2.2`, CPAL `0.16.0` y Hound `3.5.1`; el árbol completo queda fijado en `Cargo.lock`. La primera compilación descarga y prepara las dependencias. En Linux, GPUI y CPAL requieren sus bibliotecas de sistema; esa plataforma y Android no forman parte de la validación inicial.

Referencias técnicas: [GPUI](https://gpui.rs/), [API de GPUI 0.2.2](https://docs.rs/gpui/0.2.2/gpui/), [CPAL](https://docs.rs/cpal/0.16.0/cpal/), [tokens Aurora de Core](../../../Docs/global/design/app-colors.md) y [base de soporte oficial](../../../Docs/features/official-support/official-support-baseline.md).

Validación y siguientes pasos: [proceso de migración](../process/active/2026-11-13_rust-gpui-demo/PLAN.md).
