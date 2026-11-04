# Alias de comandos estándar — Validación

- Estado: `PENDING`

## Comprobaciones

- [x] V01 — Agente — `./gradlew tasks --all`. Esperado: aparecen `dev`, `devIsolated`, `devAndroid`, `test`, `check`, `build`, `buildDesktop`, `releaseLocal` y `releasePublish` con su descripción.
- [x] V02 — Agente — `npm run`. Esperado: se listan los alias canónicos (`dev`, `dev:isolated`, `build:desktop`, `release:local`, `release:publish`, …).
- [x] V03 — Agente — `npm run build:desktop` y `npm test` desde PowerShell. Esperado: ambas completan en verde invocando los alias de Gradle.
- [x] V03b — Agente — `npm run build:desktop` con `cmd /c gradlew.bat` (sin ruta). Esperado: falla con «no se reconoce como un comando interno o externo». Se corrigió usando la ruta explícita `.\gradlew.bat`; `cmd` no busca el directorio actual para el comando cuando lo invoca npm.
- [x] V04 — Agente — Wrapper `build-release.ps1` contra un stub del script interno. Esperado: `local` → `-LocalOnly`, `-LocalOnly` → `-LocalOnly`, `1.2.0` → `-Version 1.2.0`, y los conmutadores `-SkipTests`/`-SkipSigning` se reenvían.
- [x] V05 — Agente — `./gradlew devIsolated --dry-run` con init script que inspecciona las propiedades del sistema de la tarea `run`. Esperado: `lyraflow.allowMultipleInstances=true` y `lyraflow.channel=dev`.
- [x] V06 — Agente — `./gradlew dev --dry-run` con el mismo init script. Esperado: solo `lyraflow.channel=dev` (sin la bandera multiinstancia).
- [x] V07 — Agente — `./gradlew <alias> --dry-run` para `build`, `test`, `check`, `dev`, `devIsolated` y `devAndroid`. Esperado: cada alias resuelve a su tarea destino (`:composeApp:createDistributable`, `:composeApp:desktopTest`, `:composeApp:compileDebugKotlinAndroid`, `:composeApp:run`, `:composeApp:installDebug`).
- [x] V08 — Agente — `./gradlew :composeApp:desktopTest`. Esperado: suite en verde (no se ve afectada por los alias).
- [ ] V09 — Tester — `.\gradlew.bat dev` y comprobar que la app arranca en modo desarrollo.
- [ ] V10 — Tester — `.\gradlew.bat devIsolated` con la app ya abierta. Esperado: coexisten dos instancias (la segunda no cierra la primera).
- [ ] V11 — Tester — Doble clic en `dev.bat`. Esperado: la app arranca y la consola muestra `[LyraFlow] Iniciando modo desarrollo`.
- [ ] V12 — Tester — `.\build-release.ps1 local`. Esperado: instalador EXE firmado y `SHA256SUMS.txt` en `release/`, sin commits ni tags.

## Registro de fallos

- Fallo técnico → crear o reabrir una tarea.
- Plan incorrecto → regresar a `PLAN.md`.
- Entorno bloqueado → registrar el bloqueo sin marcar la validación.
