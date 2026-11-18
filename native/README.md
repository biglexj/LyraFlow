# LyraFlow — demo Rust + GPUI

Prototipo de la migración en la rama `codex/rust-gpui-demo`. Convive con Compose; no sustituye la app instalada. La versión `0.1.0` identifica exclusivamente este paquete experimental, sin cambiar la versión del producto.

Desde la raíz del repositorio:

```powershell
mise dev gpui
mise check gpui
mise test gpui
mise build gpui
```

`dev` abre la ventana nativa. `build` genera `native/target/release/lyraflow-gpui-demo.exe` en Windows. Todas las tareas usan `Cargo.lock`; `--plan` muestra el comando sin ejecutarlo. No hay instalador ni publicación para la demo.

## Alcance

- Panel de captura con el icono y los colores actuales de LyraFlow; temas claro y oscuro durante la sesión.
- Grabación real mediante CPAL, duración calculada a partir del audio recibido, nivel de entrada y límite de cinco minutos.
- WAV PCM de 16 bits con la frecuencia y los canales nativos del micrófono predeterminado; formatos de entrada `f32`, `i16` y `u16`. Todavía no se convierte a mono de 16 kHz.
- Detener finaliza el WAV y muestra su ruta. Cancelar elimina la toma activa; cerrar la ventana descarta una toma en curso. Las grabaciones finalizadas permanecen en la carpeta temporal `LyraFlow-gpui-demo` del sistema hasta su limpieza manual o por el sistema operativo.
- Copiar la ruta del último audio al portapapeles.
- Errores reales de dispositivo y de escritura visibles, sin transcripciones de ejemplo.

## Apariencia

La demo ofrece **Aurora** y **Clásico** en el apartado *Apariencia*, con los modos **Automático**, **Claro** y **Oscuro**.

- **Aurora** es el tema predeterminado y usa los tokens globales de Core (`biglex-app-base`, versión `1.0.0`). La copia consumida por la compilación está en [`assets/tokens/biglex-app-base.json`](assets/tokens/biglex-app-base.json); su origen y su procedimiento de actualización, en [`assets/tokens/README.md`](assets/tokens/README.md). La adaptación a roles de GPUI vive en [`src/theme.rs`](src/theme.rs) y una prueba comprueba que no se desvía de la copia.
- **Clásico** conserva la paleta anterior de LyraFlow: los violetas `#7F52FF` y `#B499FF`, y sus fondos y textos propios.
- La preferencia se guarda en un archivo propio de la demo (`%LOCALAPPDATA%\LyraFlow-gpui-demo\appearance.txt` en Windows). No lee ni escribe las preferencias de la instalación de Compose.

## Acerca de

El acceso **Acerca de** abre un diálogo con la identidad del producto (nombre, versión y licencia MIT), la autoría, los destinos oficiales de apoyo y los enlaces de ayuda y créditos, siguiendo la base de soporte oficial de Core. Se cierra con el botón *Cerrar*, con `Esc` o pulsando fuera del diálogo.

## Atajos

Los atajos son **locales a la ventana**: `Ctrl + Espacio` graba/detiene; `Ctrl + Alt + Espacio` o `Esc` cancelan; `Ctrl + T` cambia entre modo claro y oscuro; `Ctrl + Mayús + C` copia la ruta; `Tab` y `Mayús + Tab` recorren los controles con foco visible. El micrófono solo se abre al pulsar Grabar o su atajo.

Pendientes de migración: transcripción con proveedores, Whisper, atajos globales, inserción de texto, historial, ajustes persistentes, instancia única, bandeja, actualizaciones y accesibilidad completa. Esta demo no lee claves ni preferencias de la instalación de Compose y no hace solicitudes a proveedores de IA.

## Entorno

Rust estable con Cargo, herramientas de compilación MSVC y Windows SDK (`fxc.exe`) en Windows. Dependencias directas fijadas: GPUI `0.2.2`, CPAL `0.16.0` y Hound `3.5.1`; el árbol completo queda fijado en `Cargo.lock`. La primera compilación descarga y prepara las dependencias. En Linux, GPUI y CPAL requieren sus bibliotecas de sistema; esa plataforma y Android no forman parte de la validación inicial.

Referencias técnicas: [GPUI](https://gpui.rs/), [API de GPUI 0.2.2](https://docs.rs/gpui/0.2.2/gpui/), [CPAL](https://docs.rs/cpal/0.16.0/cpal/), [tokens Aurora de Core](../../../Docs/global/design/app-colors.md) y [base de soporte oficial](../../../Docs/features/official-support/official-support-baseline.md).

Validación y siguientes pasos: [proceso de migración](../process/active/2026-11-13_rust-gpui-demo/PLAN.md).
