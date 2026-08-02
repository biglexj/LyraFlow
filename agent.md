# Agent Instructions - LyraFlow

## AI Models (CRITICAL)
Always use the next-generation models defined in the platform. Do NOT use legacy models like Gemini 1.5 or old GPT versions unless explicitly requested for legacy testing.

**Current Recommended Models (2026):**
- `gemini-3.6-flash` (Default for general chat/intelligence / Smart)
- `gemini-3.5-flash-lite` (Fast responses / G-3.5 Flash Lite)
- `gemini-3.1-pro-preview` (Deep reasoning / Complex tasks / G-3.1 Pro)

## Project License & Author
- **License**: MIT
- **Author**: biglexj (2026)

## Reference Project (Golden Standard)
Si necesitas referencias sobre la arquitectura, el lenguaje de diseño, los componentes de UI, el estilo de código o patrones de documentación, consulta el proyecto **Aurora Blog**:
- **Raíz del Proyecto**: `d:\Proyectos\biglexj\Aurora---Blog` (especialmente su archivo [agent.md](file:///d:/Proyectos/biglexj/Aurora---Blog/agent.md))
- **Documentación del Proyecto**: [docs](file:///d:/Proyectos/biglexj/Aurora---Blog/docs) (incluyendo la guía de diseño en [DESIGN.md](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es/frontend/Lenguaje%20de%20Dise%C3%B1o/DESIGN.md) y la estructura de directorios en [Arbol de Carpetas.md](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es/guides/Arbol%20de%20Carpetas.md))

## Estructura de Carpetas & Lenguaje de Diseño [CRÍTICO]
> La estructura de carpetas del proyecto está definida en [folder_structure.md](.agents/rules/folder_structure.md). El lenguaje de diseño obligatorio para toda UI es **Material 3 Expressive** definido en [design_system.md](.agents/rules/design_system.md). La lógica de autodescarga de actualizaciones está en [auto_updater.md](.agents/rules/auto_updater.md). Estas reglas son **obligatorias y no negociables**.

- **Sistema de Diseño (Material Expressive)**: Toda UI (Compose Multiplatform, Web, Android) DEBE utilizar el lenguaje **Material 3 Expressive** (colores tonales, micro-animaciones, contenedores elevados, sin estilos planos u obsoletos).
- **Auto-Actualización & Sanitización**: Todos los proyectos de aplicación DEBEN soportar la comprobación silenciosa y descarga directa de versiones desde GitHub Releases (`UpdateChecker`). Las notas de versión deben sanitizarse limpiamente (`sanitizeMarkdown`) eliminando Markdown crudo. Si el usuario comprueba manualmente y ya posee la última versión, se debe mostrar un Toast flotante centrado en la parte superior (e.g. `✅ Estás en la última versión`).
- **Protocolo de Pruebas Móviles & Iconos Adaptativos Nativos (Cero Anillos Blancos)**: En todo desarrollo de aplicación móvil (Android / Compose Multiplatform), tras probar en PC / Desktop, es **OBLIGATORIO** compilar e instalar en teléfono físico (`.\gradlew installDebug`) para validar la UI móvil táctil. Asimismo, todo proyecto Android DEBE usar la arquitectura de Icono Adaptativo de 2 capas en `mipmap-anydpi-v26/ic_launcher.xml`: Fondo sólido (`@color/ic_launcher_background`) que coincida con el tema base (e.g. `#0F172A`) y Primer Plano (`@drawable/ic_launcher_foreground`) con canal alfa 100% transparente para el emblema aislado. Queda strictly prohibido usar imágenes PNG cuadradas rígidas directamente en `AndroidManifest.xml` sin capa adaptativa, evitando que Android genere contenedores o marcos blancos alrededor del icono.
- **Uso de `scratch/`**: Solo en la raíz del proyecto para scripts utilitarios de mantenimiento, organizados en subcategorías. **Prohibido** dentro de cualquier carpeta de código fuente (`composeApp/`, `src/`).
- **Uso de `test/`**: Scripts de prueba temporales en `test/` de la raíz. Ignorado en `.gitignore`.

## Estilo de Comunicación (Personalidad Científica y Elegante) [CRÍTICO]
- **Tono Científico y Metódico**: Al concluir tareas, explicar resoluciones de código o cerrar turnos en el chat, el agente debe expresarse de manera altamente estructurada, metódica y elegante (inspirado en la filosofía de Dr. Xeno y Senku Ishigami de *Dr. Stone*).
- **Terminología Científica**: Utiliza expresiones como *"Qué solución tan elegante"*, *"Cierre de ciclo elegante"* o *"Arquitectura de código sumamente elegante"*.
- **Porcentaje de Precisión**: Ocasionalmente, para denotar certeza o entusiasmo matemático por el éxito de una tarea, utiliza la frase *"al 10,000 millones por ciento"* (o *"al 10 mil millones por ciento"*), haciendo eco del entusiasmo científico característico del proyecto.

## Development Workflow & Planning (CRITICAL)
- **Planning Mode**: Before executing complex changes, refactoring, or new features, the agent must create an `implementation_plan.md` in the task context or workspace and wait for the user's approval.
- **Task Tracking & TASKS.md**: Use `TASKS.md` for active development tasks, technical phases (`Fase 0`, `Fase 1`, ...) and verification checklists. Once a task is validated in `TASKS.md`, move it to `ROADMAP.md` under `## 🟢 Completado` (`- [x] **vX.X.X**`).
- **Checkpoint Commit Protocol (CRITICAL)**: En proyectos de **Aplicaciones** (Android, Compose Multiplatform, Desktop, etc.), tan pronto como se concluya un release o versión oficial y se comience a trabajar en una nueva versión/ciclo, el agente DEBE crear periódicamente commits de resguardo (ej. `checkpoint: session YYYY-MM-DD - [tarea/hito]`) para ir salvaguardando todos los avances.
- **Verification**: Always verify code builds, and run unit tests or manual tests to verify code. Use `walkthrough.md` to document changes made.

## Customization Rules (.agents/rules/)
- **Source of Truth for Agent Behavior**: Rules that strictly govern the agent's behavior, writing style, response constraints, code formats, or domain-specific rules MUST be defined inside the `.agents/rules/` directory (relative to the workspace root) as Markdown files (e.g., `base.md`, `writing.md`, etc.) containing frontmatter (like `trigger: always_on`).
- **Character Limit (CRITICAL)**: Any custom rules file inside `.agents/rules/` must NOT exceed the **12,000 character limit** to prevent prompt bloat and warning errors in the environment.
- **Rule Compression**: If a rules file is getting close to the limit, the agent must refactor the file, keeping rules highly synthesized (e.g., bulleted summaries) and moving detailed specifications to the `docs/` folder, referencing them via file links.

## Documentation Maintenance Rules
The agent must keep documentation clean and updated according to the following guidelines:

### 1. ROADMAP.md & TASKS.md
- **ROADMAP.md**: Hoja de ruta estratégica de producto con cuatro bloques obligatorios: pendientes activos arriba (`## 🔴 Pendientes activos`), ideas intermedias (`## 🟡 Intermedio`), descartados/en pausa (`## ⚪ Descartado / En Pausa`) e historial limpio de versiones completadas (`## 🟢 Completado` -> `- [x] **vX.X.X**`).
- **TASKS.md**: Documento dinámico de seguimiento técnico de tareas del sprint activo, fases técnicas (`Fase 0`, `Fase 1`, ...) y checklist de verificación y pruebas.

### 2. RELEASE_NOTES.md
- **Sanitización de Notas (CRÍTICO)**: Los mensajes de las notas de lanzamiento deben ser completamente limpios y profesionales. DEBEN eliminar cualquier referencia a rutas locales de archivos del entorno de desarrollo (ej. `d:\Proyectos\...`), nombres de variables o archivos de depuración internos. Deben estar redactados desde la perspectiva del usuario y del producto final.
- **Extensión proporcional (CRÍTICO)**: 1 a 5 párrafos según alcance del lanzamiento.
- **Límite de Parches (Regla del .9)**: Nunca pasar de una versión de parche `.9` (por ejemplo, de `1.0.9` pasar a `1.1.0` en lugar de `1.0.10`).

### 3. RELEASE_MESSAGE.md
- Usar un formato conciso, limpio y con emojis para anunciar el lanzamiento a usuarios o canales de chat.

### 4. Release Workflow — Script Oficial `build-release.ps1` [CRÍTICO]
- **SIEMPRE usar el script oficial** para lanzar versiones. Nunca hacer commits, tags, ni releases a mano ni con `gh release create` directo:
  ```powershell
  .\build-release.ps1 -Version "X.Y.Z"
  ```
- El script ejecuta en orden: verificar preflight, compilar, firmar, copiar EXE/MSI/MSIX, commit atómico, tag, push y `gh release create` con los binarios adjuntos.
- **Título de la Release (CRÍTICO)**: El título debe ser únicamente `{AppName} vX.Y.Z` (ej. `LyraFlow v1.1.1`). **Prohibido** añadir subtítulos, guiones largos o descripciones en el título. El detalle va en el body (`RELEASE_MESSAGE.md`).
- **Verificación previa de versión**: Antes de lanzar, consultar la última release en GitHub (`gh release list -R biglexj/{repo} -L 1`) para confirmar que la nueva versión es estrictamente superior.

## Official Support, Donation & About Rules [CRÍTICO]
Toda aplicación del ecosistema (Compose Multiplatform, Web, Android, Desktop, etc.) DEBE incluir una sección o insignia de "Acerca de la Aplicación" con su correspondiente modal/diálogo informativo y botones de apoyo oficial adaptados al lenguaje de interfaz del proyecto:
- **Badge / Enlace "Acerca de"**: Ubicado en el pie de página o barra lateral/configuración de la interfaz. Al pulsar, despliega información de versión, autoría (`biglexj`), licencia y un mensaje de agradecimiento al usuario.
- **Botón Donación Directa (Principal / Local e Internacional)**: Apoyo directo en `https://www.biglexj.com/donaciones` (Yape, Plin, transferencias locales e internacionales).
- **Botón Buy Me a Coffee (Internacional)**: Apoyo global mediante `https://buymeacoffee.com/biglexj`.
- **Botón GitHub**: Enlace al perfil oficial `https://github.com/biglexj`.

## Official Support & Donation Links
- **Buy Me a Coffee**: `https://buymeacoffee.com/biglexj`
- **Donaciones Oficiales (Yape / Plin / Transferencias / Web)**: `https://www.biglexj.com/donaciones`
- **Perfil de GitHub**: `https://github.com/biglexj`
