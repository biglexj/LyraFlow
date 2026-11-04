# Alias de comandos estándar — Plan

- Estado: `IN_PROGRESS`
- Fecha: `2026-10-30`
- Proyecto: `LyraFlow`

## Objetivo

Implantar en LyraFlow el estándar global de scripts de compilación, ejecución y alias de comandos (Documentación Core, `global/architecture/command-scripts-standard.md`, sección E para Kotlin Multiplatform) para que no haya que memorizar `:composeApp:run` ni rutas de `scripts/`.

## Alcance

- Incluye: alias de Gradle en la raíz, wrapper `build-release.ps1` en la raíz, `package.json` de conveniencia con la taxonomía de dos puntos, lanzador `dev.bat`, documentación en `README.md` y `Docs/packaging.md`.
- No incluye: migración del script de release a `scripts/build/`, publicación de artefactos Android ni cambios en el pipeline de firma.

## Enfoque

1. Tomar como referencia la implementación de `Luna---Fetch` (mismo stack KMP del ecosistema) y la plantilla de Core.
2. Registrar en `build.gradle.kts` raíz las tareas `dev`, `devIsolated`, `devAndroid`, `test`, `check`, `build`, `releaseLocal` y `releasePublish`.
3. Exponer en `package.json` los alias canónicos con dos puntos (`dev:isolated`, `build:desktop`, `release:local`, …).
4. Añadir el wrapper raíz `build-release.ps1` que acepta `local` como argumento posicional.
5. Añadir `dev.bat` para el arranque por doble clic.
6. Validar el mapeo de argumentos del wrapper y la propagación de la bandera multiinstancia.

## Criterios de finalización

- [x] `.\gradlew.bat tasks` muestra los alias registrados.
- [x] `npm run` lista los alias canónicos.
- [x] El wrapper raíz mapea `local`, `-LocalOnly`, versión y conmutadores.
- [x] `devIsolated` inyecta `lyraflow.allowMultipleInstances` solo cuando se pide.
- [x] `README.md` y `Docs/packaging.md` documentan los alias.
- [ ] Validación manual del arranque en modo desarrollo y del modo multiinstancia.

## Autorización

- [ ] Plan aprobado para ejecución.
