# Tokens de Aurora empaquetados

`biglex-app-base.json` es una **copia literal** de los tokens globales de Core:

- Origen: `Docs/global/design/palettes/biglex-app-base.json` (Documentación Core).
- Versión consumida: `1.0.0`.
- Identificador de la paleta: `biglex-app-base`.

La copia viaja con la compilación: la demo no consulta Core en tiempo de ejecución y
declara la versión que consume. `native/src/theme.rs` adapta esos valores a roles de
tema para GPUI y su prueba `aurora_matches_vendored_tokens` falla si el módulo se
desvía de la copia.

## Cómo actualizar la copia

1. Copiar el archivo de Core sin modificarlo.
2. Actualizar los valores de `aurora(claro)` y `aurora(oscuro)` en `native/src/theme.rs`
   para que coincidan con la copia.
3. Ejecutar `mise test gpui` y comprobar que la versión declarada sigue siendo la
   esperada.
4. Registrar la adopción en `process/active/2026-11-13_rust-gpui-demo/`.

La paleta **Clásico** no procede de Core: conserva la paleta propia de LyraFlow
(Compose Multiplatform) y sus valores viven en `native/src/theme.rs`.
