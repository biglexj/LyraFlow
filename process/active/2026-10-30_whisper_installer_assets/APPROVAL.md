# Instalador de Whisper local — Aprobación

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

- El fallo «Collection contains no element» venía de `.first {}` sobre la lista de adjuntos vacía de la release estable de whisper.cpp.
- Ahora se recorre el listado de releases y se elige la más reciente con `whisper-bin-x64.zip`, con respaldo fijo y mensajes en español.
- Queda pendiente la comprobación manual desde la interfaz.

## Destino

- `APPROVED` con todos los controles completos → `process/completed/YYYY/`.
- `CANCELLED`, `SUPERSEDED` o cierre incompleto → `process/archive/YYYY/`.
- `REWORK` → permanece en `process/active/`.

Mueve la carpeta completa y no conserves una copia duplicada en `active`.
