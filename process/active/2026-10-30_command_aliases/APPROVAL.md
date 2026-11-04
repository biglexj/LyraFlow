# Alias de comandos estándar — Aprobación

- Estado: `PENDING`

## Controles

- [x] Validación técnica del agente.
- [ ] Validación funcional del tester.
- [ ] Aprobación final de Biglex.
- [ ] `ROADMAP.md` actualizado.
- [ ] Sesión cerrada con resumen breve.

## Decisión

- [ ] `APPROVED`
- [ ] `REWORK`
- [ ] `CANCELLED`
- [ ] `SUPERSEDED`

## Resumen

- LyraFlow no implementaba la capa de alias del estándar global de Core: había que memorizar `:composeApp:run` y `scripts/release/build-release.ps1`.
- Ahora hay alias de Gradle en la raíz, `package.json` con la taxonomía de dos puntos, wrapper `build-release.ps1` en la raíz y lanzador `dev.bat`.
- `devIsolated` reutiliza la tarea `run` con `lyraflow.allowMultipleInstances`, sin duplicar el cableado interno del plugin de Compose.

## Destino

- `APPROVED` con todos los controles completos → `process/completed/YYYY/`.
- `CANCELLED`, `SUPERSEDED` o cierre incompleto → `process/archive/YYYY/`.
- `REWORK` → permanece en `process/active/`.

Mueve la carpeta completa y no conserves una copia duplicada en `active`.
