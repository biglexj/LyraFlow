# Personalización del instalador EXE (branding, español e «Iniciar LyraFlow») — Aprobación

- Estado: `APPROVED`
- Fecha: `2026-10-21`
- Proyecto: `LyraFlow`

## Solicitud

Personalizar el instalador EXE de Windows de LyraFlow: traducirlo al español, aplicar la identidad visual de LyraFlow (fondo, cinta e icono) y ofrecer al final las opciones **Finalizar** e **Iniciar**.

## Decisiones

- Se mantiene la distribución exclusiva por EXE y la firma con `CN=biglexj`.
- La personalización se aplica sobre la plantilla WiX de `package/windows`, no sobre el artefacto ya firmado.
- El botón de arranque se ofrece como casilla opcional del diálogo final de WixUI («Iniciar LyraFlow al terminar»), que convive con el botón «Finalizar» ya localizado.
- El cierre de la aplicación en ejecución se resuelve con `taskkill` antes de `RemoveExistingProducts`, para que la actualización no termine con «ended prematurely».
- La publicación (firma, tag y release) sigue requiriendo instrucción explícita de lanzamiento.

## Autorización

- [x] Plan aprobado para ejecución por Biglex.
