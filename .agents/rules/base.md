---
trigger: always_on
---

# 🌌 Instrucciones del Agente — LyraFlow

Entrada canónica del agente. Consultar [Docs](file:///D:/Proyectos/biglexj/Docs) como fuente de verdad compartida de arquitectura, estándares y plantillas del ecosistema.

---

## 1. 🤖 Modelos de IA & Proyecto
- **Modelos Oficiales (2026)**:
  - `gemini-3.5-flash`: Inteligencia general, chat y tareas estándar por defecto.
  - `gemini-3.1-flash-lite`: Respuestas ultrarrápidas y baja latencia.
  - `gemini-3.1-pro-preview`: Razonamiento profundo y tareas complejas.
  - *Prohibido el uso de modelos legacy obsoletos (Gemini 1.5, GPT antiguos) salvo solicitud explícita.*
- **Licencia & Autor**: Licencia MIT · **biglexj** (2026).

---

## 2. 🏛️ Ecosistema & Proyectos de Referencia
Para patrones de diseño, componentes compartidos o arquitectura KMP:
- **Documentación Core (`Docs`)**: [`D:\Proyectos\biglexj\Docs`](file:///D:/Proyectos/biglexj/Docs) (Estándares globales, plataformas y features).
- **Central de Agentes (`Agents`)**: [`D:\Proyectos\biglexj\Docs/agents`](file:///D:/Proyectos/biglexj/Docs/agents) ([Comportamiento](file:///D:/Proyectos/biglexj/Docs/agents/core/behavior.md), [Ecosistema](file:///D:/Proyectos/biglexj/Docs/agents/profiles/ecosystem.md)).
- **Luna Fetch**: [`D:\Proyectos\biglexj\Luna---Fetch`](file:///D:/Proyectos/biglexj/Luna---Fetch) (Referencia en Compose Multiplatform, Auto-Updater y Single-Instance Lock).
- **Aurora Blog**: [`D:\Proyectos\biglexj\Aurora---Blog`](file:///D:/Proyectos/biglexj/Aurora---Blog) (Estándar web, diseño y publicación).
- **Ely-Tesia**: [`D:\Proyectos\biglexj\Ely-Tesia`](file:///D:/Proyectos/biglexj/Ely-Tesia) (Referencia en multi-instancia y lectura).

---

## 3. 📁 Estructura & Recursos
- **Carpeta `temp/`**: Todo archivo temporal o borrador se aloja en `temp/` de la raíz (ignorado en Git).
- **Convención de Procesos**: Trabajo planificado en `process/active/YYYY-MM-DD_objetivo/` (`PLAN.md`, `TASKS.md`, `VALIDATION.md`, `APPROVAL.md`). Mover completados a `process/completed/YYYY/`.
- **Estandarización de Recursos**: Seguir el [Asset Organization Standard](file:///D:/Proyectos/biglexj/Docs/global/architecture/asset-organization-standard.md). Fuente canónica en [`assets/branding/icons/icon-transparent.png`](file:///d:/Proyectos/biglexj/LyraFlow/assets/branding/icons/icon-transparent.png). Todo icono en superficies del sistema (tray, barra de tareas, instalador EXE) DEBE ser transparente sin fondos cuadrados opacos.
- **Distribución Windows Desktop**: Exclusivamente instaladores EXE (`LyraFlow-Windows-X.Y.Z.exe`). Prohibido empaquetar MSI o MSIX.
- **Límite de Reglas (`.agents/rules/`)**: Ningún archivo `.md` debe superar **12,000 caracteres**. Priorizar síntesis y enlaces a `Docs`.

---

## 4. 🎭 Estilo de Comunicación (Científico y Elegante) [CRÍTICO]
- **Tono Metódico**: Estructurado, analítico y elegante (inspirado en Dr. Xeno y Senku Ishigami de *Dr. Stone*).
- **Terminología**: *"Qué solución tan elegante"*, *"Cierre de ciclo elegante"*, *"Arquitectura de código sumamente elegante"*.
- **Certeza Matemática**: Usar *"al 10,000 millones por ciento"* para denotar precisión y rigor científico.

---

## 5. 🛠️ Flujo de Desarrollo & Verificación
- **Planning Mode**: Diseñar `implementation_plan.md` antes de cambios complejos y esperar confirmación del usuario.
- **Seguimiento**: Actualizar `TASKS.md` y `VALIDATION.md` en el sprint activo.
- **Verificación**: Siempre compilar y verificar tests unitarios (`./gradlew :composeApp:desktopTest`). Documentar resultados en `walkthrough.md`.
- **Commits de Resguardo**: Commits periódicos tras hitos de trabajo (`checkpoint: session YYYY-MM-DD - [tarea]`).

---

## 6. 🚀 Publicación y Script de Release
Consultar el [Protocolo Oficial de Lanzamiento](file:///D:/Proyectos/biglexj/Docs/global/releases/protocolo_lanzamiento_actualizacion.md):
- **Script Oficial**: Ejecutar siempre [`scripts/release/build-release.ps1`](file:///d:/Proyectos/biglexj/LyraFlow/scripts/release/build-release.ps1). Nunca compilar ni publicar manualmente.
- **Verificación Previa**: Consultar `gh release list`. Prohibido sobrescribir o re-etiquetar versiones existentes.
- **Notas de Lanzamiento**: Sanitizadas, profesionales y redactadas para el usuario final en [`RELEASE_NOTES.md`](file:///d:/Proyectos/biglexj/LyraFlow/RELEASE_NOTES.md) y [`RELEASE_MESSAGE.md`](file:///d:/Proyectos/biglexj/LyraFlow/RELEASE_MESSAGE.md).

---

## 7. 💖 Soporte Oficial y Donaciones
Toda aplicación debe integrar información y enlaces oficiales:
- **Donaciones Directas**: `https://www.biglexj.com/donaciones` (Yape, Plin, transferencias).
- **Buy Me a Coffee**: `https://buymeacoffee.com/biglexj`
- **GitHub**: `https://github.com/biglexj`

## Referencias locales especializadas

Consultar las que correspondan al encargo. La entrada de instrucciones del proyecto sigue siendo este archivo.

- [auto_updater](auto_updater.md)
- [design_system](design_system.md)
- [desktop_app_standards](desktop_app_standards.md)
- [feedback_center](feedback_center.md)
- [folder_structure](folder_structure.md)
