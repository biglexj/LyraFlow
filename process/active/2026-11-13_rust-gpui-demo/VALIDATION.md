# Validación

Primera demo compilada y abierta en Windows el 2026-11-13.

- [x] `bun test scripts/tests/gpui.test.ts`: 3 pruebas, 0 fallos; variantes Cargo, plan sin ejecución y rechazo de release/publish GPUI.
- [x] `mise check gpui`: sin errores.
- [x] `mise test gpui`: 4 pruebas, 0 fallos; conversión PCM, finalización WAV, descarte al cancelar/cerrar y errores sin falso éxito. Son pruebas sintéticas, sin abrir el micrófono.
- [x] `mise dev gpui`: ejecutable compilado y ventana abierta. Panel oscuro inspeccionado; `Ctrl + T` cambió al tema claro.
- [x] Captura física observada en curso en la ventana: dispositivo DM30 USB Microphone, contador avanzando e indicador de nivel. El inicio de esa toma coincidió con interacción del usuario; no se atribuye al agente.
- [ ] Detener esa toma, comprobar y escuchar el WAV físico; cancelar otra toma. La prueba sintética de esos caminos no sustituye esta revisión.
- [x] `mise dev gpui --plan` y `mise build gpui --plan`: comandos esperados, sin ejecutar Cargo.
- [x] `git diff --check`: sin errores de espacios.

El ejecutable abierto es de desarrollo. `mise build gpui` (optimización release) no se ejecutó. Cargo avisó sobre incompatibilidad futura de la dependencia transitiva `proc-macro-error2 2.0.1`; no impidió compilar ni ejecutar.

Una compilación o una prueba sintética no demuestra la captura física, transcripción ni equivalencia completa con Compose. No se ha instalado ni publicado la demo.

## Fase 2 — Apariencia Aurora y «Acerca de» (2026-11-17)

- [x] `mise check gpui`: sin errores ni avisos del paquete.
- [x] `mise test gpui`: 18 pruebas, 0 fallos. Añadidas: los tokens de Aurora coinciden con la copia empaquetada, la versión declarada es la fijada, el texto de Aurora no se atenúa, Clásico conserva el violeta de LyraFlow y solo usa valores del diseño actual o de `LyraFlowTheme.kt`, la resolución del modo sigue la preferencia, una instalación nueva empieza en Aurora, la preferencia sobrevive a un reinicio, un archivo ausente o roto no bloquea la demo y el «Acerca de» muestra la información mínima con destinos oficiales y HTTPS.
- [x] `bun test scripts/tests`: 7 pruebas, 0 fallos. Dos fallos anteriores (`release` aceptaba variantes y `publish` no rechazaba opciones desconocidas) venían del refactor local del publicador y se corrigieron restituyendo las guardas; el resto del cambio previo se conserva sin mezclar su alcance.
- [x] Ventana abierta y revisada: Aurora en oscuro y en claro, Clásico en oscuro, «Acerca de» en oscuro y en claro, cambio de tema en vivo, restauración de la preferencia tras reiniciar la demo, cierre del «Acerca de» con `Esc`, recorrido con `Tab` y foco visible en el control alcanzado.
- [x] La composición del panel se conserva respecto a la primera entrega: los controles de color están en la cabecera y no hay apartados nuevos en el cuerpo.
- [x] El velo del «Acerca de» se midió sobre la captura: el texto de fondo pasa de RGB(230,225,233) a RGB(101,99,102), coherente con el velo del 56 % del modo oscuro.

Límites de esta fase:

- La revisión visual y la aceptación de Biglex no están registradas.
- La aplicación de Compose mantiene su paleta (Clásico): el cambio de color base afecta a la aplicación nativa.
- La demo no incluye actualizador, así que el «Acerca de» no ofrece comprobar actualizaciones.
- GPUI 0.2.2 dibuja el icono de la aplicación con defectos a 48 px; el diálogo lo muestra a 40 px.
- No se ha instalado ni publicado la demo; `mise build gpui` (release) no se ejecutó en esta fase.
