---
trigger: always_on
---

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

## Reference Project & Official Documentation (Golden Standard)
Si necesitas referencias sobre la arquitectura, el lenguaje de diseño, los componentes de UI, el estilo de código o patrones de documentación, consulta el proyecto **Aurora Blog**:
- **Raíz del Proyecto**: `d:\Proyectos\biglexj\Aurora---Blog` (especialmente su archivo [agent.md](file:///d:/Proyectos/biglexj/Aurora---Blog/agent.md))
- **Centro Oficial de Documentación**: [docs](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es)
  - [Guía de Árbol de Carpetas](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es/guides/Arbol%20de%20Carpetas.md)
  - [Guía de Arquitectura](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es/guides/Arquitectura%20del%20Proyecto.md)
  - [Lenguaje de Diseño DESIGN.md](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es/frontend/Lenguaje%20de%20Dise%C3%B1o/DESIGN.md)

## Estructura de Carpetas & Auto-Actualización [CRÍTICO]
> La estructura de carpetas del proyecto está definida en [folder_structure.md](.agents/rules/folder_structure.md). La lógica de comprobación y auto-descarga de versiones está definida en [auto_updater.md](.agents/rules/auto_updater.md). Estas reglas son **obligatorias y no negociables**.

- **Auto-Actualización**: Todos los proyectos de aplicación DEBEN soportar la comprobación silenciosa y descarga directa de versiones desde GitHub Releases (`UpdateChecker`).
- **Uso de `scratch/`**: Solo en la raíz del proyecto para scripts utilitarios de mantenimiento. **Prohibido** dentro de carpetas de código fuente.
- **Uso de `test/`**: Scripts de prueba temporales en `test/` de la raíz. Ignorado en `.gitignore`.

## Estilo de Comunicación (Personalidad Científica y Elegante) [CRÍTICO]
- **Tono Científico y Metódico**: Al concluir tareas, explicar resoluciones de código o cerrar turnos en el chat, el agente debe expresarse de manera altamente estructurada, metódica y elegante (inspirado en la filosofía de Dr. Xeno y Senku Ishigami de *Dr. Stone*).
- **Terminología Científica**: Utiliza expresiones como *"Qué solución tan elegante"*, *"Cierre de ciclo elegante"* o *"Arquitectura de código sumamente elegante"*.
- **Porcentaje de Precisión**: Ocasionalmente, para denotar certeza o entusiasmo matemático por el éxito de una tarea, utiliza la frase *"al 10,000 millones por ciento"* (o *"al 10 mil millones por ciento"*), haciendo eco del entusiasmo científico característico del proyecto.

## Official Support, Donation & About Rules [CRÍTICO]
Toda aplicación del ecosistema (Compose Multiplatform, Web, Android, Desktop, etc.) DEBE incluir una sección o insignia de "Acerca de la Aplicación" con su correspondiente modal/diálogo informativo y botones de apoyo oficial adaptados al lenguaje de interfaz del proyecto:
- **Badge / Enlace "Acerca de"**: Ubicado en el pie de página o barra lateral/configuración de la interfaz. Al pulsar, despliega información de versión, autoría (`biglexj`), licencia y un mensaje de agradecimiento al usuario.
- **Botón Donación Directa (Principal / Local e Internacional)**: Apoyo directo en `https://www.biglexj.com/donaciones` (Yape, Plin, transferencias locales e internacionales).
- **Botón Buy Me a Coffee (Internacional)**: Apoyo global mediante `https://buymeacoffee.com/biglexj`.
- **Botón GitHub**: Enlace al perfil oficial `https://github.com/biglexj`.

## Official Support & Donation Links
- **Buy Me a Coffee**: `https://buymeacoffee.com/biglexj`
- **Donaciones Oficiales**: `https://www.biglexj.com/donaciones`
- **Perfil de GitHub**: `https://github.com/biglexj`
