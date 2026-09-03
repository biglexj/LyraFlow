---
trigger: always_on
---

# Agent Instructions - LyraFlow

## AI Models (CRITICAL)
Always use next-generation platform models. Never use legacy models (Gemini 1.5, old GPT) unless explicitly requested.
- `gemini-3.5-flash` (Default chat/intelligence / Smart)
- `gemini-3.1-flash-lite` (Fast responses / Flash)
- `gemini-3.1-pro-preview` (Deep reasoning / Complex tasks / Pro)

## Project License & Author
- **License**: MIT
- **Author**: biglexj (2026)

## Proyectos de Referencia & Red de Agentes [CRÍTICO]
- **Central de Agentes (`Agents`)**: `d:\Proyectos\biglexj\Agents` ([00 - CORE.md](file:///d:/Proyectos/biglexj/Agents/Core/00%20-%20CORE.md), [03 - ECOSISTEMA.md](file:///d:/Proyectos/biglexj/Agents/Core/03%20-%20ECOSISTEMA.md)).
- **Documentación Core (`Core-Docs`)**: `D:\Proyectos\biglexj\Core-Docs` (Fuente oficial de plantillas, estándares y arquitectura).
- **Aurora Blog (Estándar Web & Docs)**: `d:\Proyectos\biglexj\Aurora---Blog` ([DESIGN.md](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es/frontend/Lenguaje%20de%20Dise%C3%B1o/DESIGN.md)).
- **Luna Fetch (Estándar Auto-Updater, Single-Instance Lock & KMP)**: `d:\Proyectos\biglexj\Luna---Fetch` ([agent.md](file:///d:/Proyectos/biglexj/Luna---Fetch/agent.md)).
- **LyraFlow (Estándar Transcripción & Asistente IA)**: `d:\Proyectos\biglexj\LyraFlow` ([agent.md](file:///d:/Proyectos/biglexj/LyraFlow/agent.md)).
- **Ely-Tesia (Estándar Multi-instancia y Lectura)**: `d:\Proyectos\biglexj\Ely-Tesia` ([agent.md](file:///d:/Proyectos/biglexj/Ely-Tesia/agent.md)).

## Estructura de Carpetas & Lenguaje de Diseño [CRÍTICO]
- **Sincronización Core-Docs**: Consultar `D:\Proyectos\biglexj\Core-Docs` al iniciar sesión. No propagar bibliotecas incompatibles.
- **`temp/`**: Archivos temporales o borradores en la raíz (ignorado en `.gitignore`).
- **Convención de Procesos**: Trabajo planificado vive en `process/active/YYYY-MM-DD_objetivo/` con `PLAN.md`, `TASKS.md`, `VALIDATION.md` y `APPROVAL.md`. Sin `TASKS.md` en raíz.
- **Sistema de Diseño (Material Expressive)**: UI con tokens M3 Expressive, colores tonales, contenedores elevados y micro-animaciones.
- **Auto-Actualización & Sanitización**: Comprobación silenciosa de GitHub Releases (`UpdateChecker`). Sanitizar notas con `sanitizeReleaseNotes`. Si la versión ya está al día en comprobación manual, mostrar Toast flotante centrado superior de 4 segundos.
- **Distribución Exclusiva en EXE (Windows Desktop)**: A partir de v1.1.6+, LyraFlow genera, distribuye y actualiza **únicamente instaladores EXE** (`LyraFlow-Windows-X.Y.Z.exe`). Prohibido empaquetar o lanzar MSI o MSIX.
- **Iconos Adaptativos Android**: Dos capas (`ic_launcher.xml`): fondo sólido tema (`#0F172A`) y foreground transparente. Cero anillos blancos.
- **`scratch/` y `test/`**: Solo en raíz para mantenimiento y scripts temporales. Prohibidos dentro de `src/` o código fuente.

## Estilo de Comunicación (Científico y Elegante) [CRÍTICO]
- **Tono Metódico**: Estructurado, analítico y elegante (inspirado en Dr. Xeno y Senku Ishigami de *Dr. Stone*).
- **Terminología**: *"Qué solución tan elegante"*, *"Cierre de ciclo elegante"*, *"Arquitectura de código sumamente elegante"*.
- **Porcentaje de Precisión**: Usar *"al 10,000 millones por ciento"* para denotar certeza y entusiasmo matemático.

## Development Workflow & Planning (CRITICAL)
- **Planning Mode**: Crear `implementation_plan.md` y esperar aprobación del usuario antes de cambios complejos.
- **Seguimiento**: `TASKS.md` del proceso activo para ejecución y `VALIDATION.md` para comprobaciones.
- **Checkpoint Commit Protocol**: Commits periódicos de resguardo (`checkpoint: session YYYY-MM-DD - [tarea]`) al picar código tras un hito o versión.
- **Verification**: Siempre verificar builds y tests unitarios. Documentar en `walkthrough.md`.

## Customization Rules (.agents/rules/)
- **Fuente de Verdad**: Reglas Markdown en `.agents/rules/` con frontmatter (`trigger: always_on`).
- **Límite de Caracteres (CRÍTICO)**: Ningún archivo en `.agents/rules/` debe superar **12,000 caracteres**. Sintetizar y enlazar a `docs/` si es necesario.

## Documentación y Lanzamientos

### 1. ROADMAP.md y procesos
- **ROADMAP.md**: 4 bloques (`🔴 Pendientes activos`, `🟡 Intermedio`, `⚪ Descartado / En Pausa`, `🟢 Completado`).
- **Cierre**: Proceso completado va a `process/completed/YYYY/`; cancelado a `process/archive/YYYY/`.

### 2. RELEASE_NOTES.md & Script de Release [CRÍTICO]
- **Verificación en GitHub ("Lanzar actualización")**:
  1. Consultar tags/releases en GitHub (`gh release list`).
  2. Si la versión local ya fue publicada, es **OBLIGATORIO** incrementar versión PATCH (`versionName` y `versionCode`).
  3. **Prohibición de Sobrescritura**: NUNCA sobreescribir ni re-etiquetar tags existentes.
  4. **Script Oficial de Release**: Ejecutar siempre `d:\Proyectos\biglexj\LyraFlow\build-release.ps1`. Compila solo EXE, firma, genera SHA256SUMS.txt y publica atómicamente.
- **Sanitización de Notas**: Limpias, sin rutas locales, sin variables ni logs internos. Redactadas para el usuario final.
- **Extensión**: 1 a 5 párrafos según magnitud del cambio.
- **Regla del .9**: Nunca pasar de `.9` (de `1.0.9` saltar a `1.1.0`).

### 3. RELEASE_MESSAGE.md
- Formato conciso con emojis, título `LyraFlow vX.Y.Z`, resumen y viñetas destacadas.

## Soporte Oficial, Donaciones y Acerca de [CRÍTICO]
- **Modal "Acerca de"**: Versión, autoría (`biglexj`), licencia y agradecimiento.
- **Donaciones Directas**: `https://www.biglexj.com/donaciones` (Yape, Plin, transferencias).
- **Buy Me a Coffee**: `https://buymeacoffee.com/biglexj`
- **GitHub**: `https://github.com/biglexj`
