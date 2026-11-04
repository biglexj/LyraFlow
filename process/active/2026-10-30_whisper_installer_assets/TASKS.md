# Instalador de Whisper local — Tareas

- Estado: `IN_PROGRESS`
- Fecha: `2026-10-30`
- Proyecto: `LyraFlow`

## Ejecución

- [x] T01 — Diagnosticar el error «Collection contains no element» al instalar Whisper.
  - Hallazgo: `WhisperInstaller.latestWindowsAsset()` consultaba `/releases/latest` y hacía `.first { name == "whisper-bin-x64.zip" }`. La release estable actual (`v1.9.5`, igual que `v1.9.3` y `v1.9.4`) se publica **sin adjuntos**: upstream movió los binarios a las builds `b####`, por lo que la lista de assets llega vacía y `.first {}` lanza `NoSuchElementException`.
- [x] T02 — Extraer el selector de assets a `WhisperReleaseAssets.selectWindowsPackage(releasesJson)` (función pura, tolerante a payloads inesperados).
- [x] T03 — Resolver el paquete recorriendo `/releases?per_page=30` y quedándose con la release más reciente que sí incluya `whisper-bin-x64.zip`.
- [x] T04 — Añadir respaldo con URL fijadas (`b5454`, `v1.9.0`) verificadas por `HEAD` cuando la API no responde (límite de peticiones) o no publica adjuntos.
- [x] T05 — Sustituir el error genérico por un mensaje en español que indica qué revisar.
- [x] T06 — Cubrir el selector con `WhisperReleaseAssetsTest` (4 casos) y ejecutar la suite `desktopTest`.
- [x] T07 — Compilar la app de escritorio (`createDistributable` + `packageExe`) y verificar que el JAR de la imagen contiene la corrección.
  - Nota: `packageDistributionForCurrentOS` reutilizó una imagen de aplicación obsoleta del 21-10; hubo que forzar `createDistributable` y `packageExe` para que el EXE distributable contenga el JAR con la corrección.

## Notas técnicas

- La API `/releases/latest` excluye las pre-releases y ahora devuelve una release sin binarios; el listado completo es la única fuente fiable.
- `whisper-bin-x64.zip` conserva `Release/whisper-cli.exe` dentro del ZIP, así que `WhisperPaths.executable()` (que recorre el árbol) lo sigue localizando tras extraer en `runtime/`.
- Los modelos de HuggingFace (`ggerganov/whisper.cpp`) siguen respondiendo 200: la descarga del modelo no era el origen del fallo.

## Bloqueos

- La validación manual (instalar desde la interfaz) queda pendiente de una sesión con escritorio interactivo.
- La ejecución del binario descargado en pruebas automatizadas está bloqueada por la política de seguridad del entorno.
