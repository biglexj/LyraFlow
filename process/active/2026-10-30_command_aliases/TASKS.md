# Alias de comandos estándar — Tareas

- Estado: `IN_PROGRESS`
- Fecha: `2026-10-30`
- Proyecto: `LyraFlow`

## Ejecución

- [x] T01 — Localizar el estándar en Core (`Docs/global/architecture/command-scripts-standard.md`, sección E) y la implementación de referencia en `Luna---Fetch`.
- [x] T02 — Registrar los alias de Gradle en el `build.gradle.kts` raíz.
  - Gradle no admite `:` en el nombre de una tarea, así que los alias con dos puntos viven en `package.json` y los de Gradle son su equivalente sin separador (`devIsolated`, `releaseLocal`, `releasePublish`).
- [x] T03 — Implementar `devIsolated` propagando `lyraflow.multiInstance` al subproyecto `composeApp`.
  - El `SingleInstanceLock` ya respeta `lyraflow.allowMultipleInstances`; basta inyectar la propiedad del sistema en la tarea `run`.
  - La propiedad se fija desde la raíz durante la configuración, antes de que se evalúe el script de `composeApp`.
- [x] T04 — Añadir el wrapper raíz `build-release.ps1` que redirige a `scripts/release/build-release.ps1`.
- [x] T05 — Añadir el `package.json` de conveniencia con la taxonomía completa de alias.
- [x] T06 — Añadir el lanzador `dev.bat` de doble clic.
- [x] T07 — Documentar los alias en `README.md` y `Docs/packaging.md`.
- [x] T08 — Validar el mapeo de argumentos del wrapper con un stub aislado y la propagación de la bandera con un init script de Gradle.

## Notas técnicas

- `packageExe` no comprueba si la imagen de aplicación cambió, así que el alias `build` depende de `:composeApp:createDistributable` y el pipeline de release invoca `createDistributable` **antes** de `packageLyraFlowInstaller`.
- Gradle hace coincidir `build` con la tarea homonima de cada subproyecto, así que `gradlew build` también ejecuta `:composeApp:build` (assemble de Android y pruebas). Para compilar solo el escritorio está `buildDesktop`.
- En `package.json` los scripts de Gradle usan `cmd /c .\gradlew.bat`: sin la ruta explícita, `cmd` no encuentra el `.bat` en el directorio actual y falla con «no se reconoce como un comando interno o externo».
- La versión de `package.json` (`1.1.9`) debe seguir a `gradle.properties` (`lyraflow.versionName`); es la única duplicación del proyecto.
- LyraFlow solo distribuye Windows: `release:android` no se expone porque no hay flujo de publicación de APK. `build:android` sí compila el APK.

## Bloqueos

- La validación manual (arranque de `dev`, `dev.bat` y dos instancias simultáneas) requiere escritorio interactivo.
