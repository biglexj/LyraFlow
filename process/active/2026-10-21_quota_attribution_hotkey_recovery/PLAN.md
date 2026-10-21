# Plan del Proceso: Atribución de Cuota y Recuperación del Atajo Global

- **Versión objetivo**: 1.1.9 (parche posterior a la 1.1.8 ya publicada e inmutable)
- **Fecha**: 2026-10-21
- **Solicitante**: Biglex

## Objetivo

Corregir dos defectos reportados durante el uso real de la 1.1.8:

1. El aviso de cuota agotada siempre menciona a Gemini, aunque el proveedor y el
   modelo seleccionados sean otros.
2. Tras agotarse la cuota, el atajo global de dictado deja de responder y no se
   recupera al cambiar de modelo o de proveedor.

## Alcance

### Incluido

- Identidad de proveedor y modelo en el estado de cuota agotada, con mensajes y
  banner derivados de la seleccion vigente.
- Cuota acotada al proveedor y al modelo que la consumieron: cambiar de proveedor
  o de modelo vuelve a intentar la transcripcion en la nube.
- El atajo global opera siempre sobre el coordinator vigente, no sobre una
  instancia huerfana creada antes de un cambio de modelo, proveedor o API key.
- Recuperacion del atajo cuando el estado de transcripcion quedo atascado sin
  peticion viva asociada.
- El HTTP 429 de OpenAI-compatible se reporta como cuota agotada y activa el
  respaldo local autonomo igual que el resto de proveedores.

### Fuera de alcance

- Rediseño de la pantalla de Ajustes, del indicador de estado o del bandeje.
- Nuevos proveedores, nuevos modelos o cambios de endpoints por defecto.
- Cambios en la politica de cooldown (60 s) ya existente.

## Estrategia

1. Extender el dominio: `TranscriptionRequest` transporta el proveedor y
   `QuotaExhaustion` registra proveedor, modelo y mensaje.
2. Reescribir el `DictationCoordinator` para que la cuota sea atribuida y acotada,
   para que los mensajes nombren al proveedor real y para que ningun camino
   deje el estado en `Transcribing` (cancelacion incluida).
3. Resolver en `Main.kt` la desincronizacion entre el coordinator que muestra la
   interfaz y el que atiende el atajo global, mediante una referencia viva.
4. Ajustar la interfaz para mostrar el banner solo cuando la cuota corresponde a
   la seleccion vigente y para describir con verdad el respaldo disponible.
5. Cubrir con pruebas unitarias la atribucion, el aislamiento entre proveedores y
   modelos, y la no persistencia del estado atascado.

## Criterios de finalizacion

- [x] El aviso de cuota nombra al proveedor y al modelo seleccionados, nunca a uno fijo.
- [x] Cambiar de proveedor o de modelo vuelve a intentar la transcripcion en la nube.
- [x] El atajo global responde tras agotarse la cuota y tras cambiar de modelo, proveedor o API key.
- [x] Ningun camino deja el estado en `Transcribing` sin peticion viva.
- [x] Suite `./gradlew :composeApp:desktopTest` en verde.
- [x] Instalador `LyraFlow-Windows-1.1.9.exe` generado localmente.
