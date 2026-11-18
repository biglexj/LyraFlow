# Primera demo Rust + GPUI

Biglex autorizó crear una rama nueva y una demo sencilla de LyraFlow con Rust + GPUI. Confirmó expresamente esas tecnologías el 2026-11-13.

Rama: `codex/rust-gpui-demo`, desde `preview`. Existía un cambio local en `scripts/mise-task.ts`; se conserva y no se incluye en un commit de esta tarea sin separar su alcance.

La primera entrega es un paquete aislado en `native/`: panel nativo con identidad de LyraFlow, captura real a WAV, nivel y duración, detener/cancelar, tema y copia de ruta. Mise incorpora la variante `gpui` para dev/build/check/test. Compose y la instalación personal permanecen operativos.

Criterios: compilar la demo, probar el ciclo de vida del WAV y el despacho de comandos, abrir la ventana y comprobar sus controles básicos. Registrar por separado pruebas sintéticas y micrófono físico. La migración completa continúa con proveedores, Whisper, atajos globales, inserción y persistencia; no está incluida en esta primera demo.

## Fase 2 — Apariencia Aurora y «Acerca de»

Biglex (2026-11-17) ordena un color base nuevo con el color clásico disponible, y un «Acerca de» nuevo, sin rehacer la distribución de la demo.

La dirección vigente es la de Core: **Aurora** es la base común y **Clásico** conserva los temas anteriores de cada aplicación. Fuentes: [inventario y adopción de tokens](../../../../Docs/process/active/2026-11-13_global-color-tokens/ADOPCION.md), [colores de las aplicaciones](../../../../Docs/global/design/app-colors.md), [gestión de temas](../../../../Docs/global/design/theme-management.md) y [soporte oficial y «Acerca de»](../../../../Docs/features/official-support/official-support-baseline.md).

Alcance de esta fase:

- **Apariencia** en la cabecera del panel, junto al acceso «Acerca de»: tema Aurora/Clásico y modo Automático/Claro/Oscuro, con la preferencia guardada y restaurada al reiniciar. La distribución del panel no cambia: no se añaden apartados ni se reordenan las secciones.
- **Adaptador de tokens**: Aurora consume la copia versionada de Core (`native/assets/tokens/biglex-app-base.json`, versión `1.0.0`) con su origen documentado; los hexadecimales viven concentrados en `native/src/theme.rs`.
- **Clásico** conserva la paleta propia de LyraFlow, incluida en la paleta de Compose.
- **«Acerca de»** con identidad, autoría, licencia, agradecimiento, destinos oficiales de apoyo y ayuda y créditos.

Fuera de alcance: cambiar la apariencia de la aplicación de Compose (sigue en Clásico hasta que la aplicación nativa la sustituya), la comprobación de actualizaciones dentro del diálogo (la demo no incluye actualizador), la instalación y la publicación.

Criterios: compilar, probar la resolución de temas, la persistencia de la preferencia y el contenido del diálogo; abrir la ventana y comprobar Aurora y Clásico en claro y oscuro, la restauración de la preferencia al reiniciar y el cierre del «Acerca de». Biglex revisa la aceptación visual.
