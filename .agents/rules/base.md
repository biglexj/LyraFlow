# Instrucciones del proyecto — LyraFlow

## Documentación Core — entrada compartida

Consultar primero [Documentación Core](../../../Docs/README.md) y seleccionar solo los módulos pertinentes. Los estándares compartidos se mantienen allí; este archivo conserva el alcance y los contratos específicos del proyecto.

- [Calidad y entrega proporcionales](../../../Docs/global/quality/quality-baseline.md): build para ajustes visuales pequeños; pruebas pertinentes para elementos, lógica o páginas nuevos y cambios grandes; revisión acotada para documentación.
- [Procesos según alcance](../../../Docs/global/documentation/process-workflow.md): no añadir fases, archivos ni nuevas aprobaciones por rutina a ajustes pequeños ya autorizados.
- [Diseño común](../../../Docs/global/design/design_system.md): contenido preciso, espacio útil, mínimo scroll innecesario, fondo global y presets compartidos, conservando la identidad del producto.
- [Diagnósticos reutilizables](../../../Docs/troubleshooting/README.md) y [promoción a Core](../../../Docs/global/documentation/core-promotion-workflow.md): documentar en Core los fallos confirmados o aprendizajes generalizables y enlazarlos desde el proyecto; mantener locales los detalles exclusivos.

En estos criterios comunes, aplicar las bases vigentes enlazadas en lugar de copias históricas de este archivo. Respetar la dirección actual de Biglex y los contratos específicos del producto; ante una contradicción, comprobar la implementación vigente y resolverla explícitamente. Las skills generales importadas son apoyo técnico, no una autoridad para redefinir estas reglas.

---

## 1. Alcance del proyecto

Aplicación de dictado por voz: captura con atajo global, transcribe con un proveedor de IA o con Whisper local e inserta el texto en la aplicación activa.

Stack vigente en `main`: **Rust + GPUI (`2.0.0`)** en la raíz (`src/`, `assets/`, `Cargo.toml`, `build.rs`), compilado con Cargo y operado mediante Mise (sección 4). La edición histórica Kotlin Multiplatform / Compose (`v1.1.9`) se conserva congelada únicamente en la rama **`v-kmp`**.

Módulos de Core que aplican a este proyecto. Consultar solo los pertinentes a la tarea:

- **Tipo y plataforma**: [Desktop App Standards](../../../Docs/types/desktop/desktop_app_standards.md), [instalador de Windows](../../../Docs/platforms/windows/installer.md), [instancia única](../../../Docs/features/single-instance/README.md), [estado de ventana](../../../Docs/types/desktop/window-state.md).
- **Funciones adoptadas**: [actualización integrada](../../../Docs/features/auto-updater/auto_updater.md), [instaladores](../../../Docs/features/installer/README.md), [modelos de IA](../../../Docs/features/ai-models/README.md), [soporte oficial y «Acerca de»](../../../Docs/features/official-support/official-support-baseline.md), [centro de feedback](../../../Docs/features/feedback-center/feedback_center.md), [design system](../../../Docs/features/design-system/design-system-baseline.md).
- **Interfaz**: [textos de interfaz](../../../Docs/global/design/interface-writing.md), [diseño común](../../../Docs/global/design/design_system.md) y [Aurora y Clásico](../../../Docs/global/design/app-colors.md).
- **Operación y publicación**: [comandos mediante Mise](../../../Docs/global/architecture/mise-command-standard.md), [scripts y alias](../../../Docs/global/architecture/command-scripts-standard.md), [organización de carpetas](../../../Docs/global/architecture/project-structure.md), [organización de assets](../../../Docs/global/architecture/asset-organization-standard.md), [base de lanzamientos](../../../Docs/global/releases/release-baseline.md), [protocolo de lanzamiento](../../../Docs/global/releases/protocolo_lanzamiento_actualizacion.md) y [release message para la web](../../../Docs/global/releases/web-release-message-standard.md).
- **Reglas de trabajo**: [reglas de trabajo del agente](../../../Docs/global/documentation/agent-working-rules.md), [estilo de comunicación](../../../Docs/global/agents/communication-style.md) y [gobernanza documental](../../../Docs/global/documentation/documentation-governance.md).

## 2. Contratos y decisiones locales

Estos contratos son exclusivos de LyraFlow; no se replican en Core.

- **Atajos globales**: `Ctrl + Espacio` alterna grabar/detener y `Ctrl + Alt + Espacio` cancela el dictado en curso sin transcribirlo. Ambos se editan en Ajustes, no pueden coincidir entre sí y vuelven al valor por defecto ante colisión. Fuente: [`src/platform_win.rs`](../../src/platform_win.rs).
- **Transcripción**: proveedores `Gemini`, `OpenAI` (endpoint y modelo editables para endpoints compatibles) y `MiniMax` (transcriptor `asr-1.0` con refinamiento posterior), más `Whisper local` por sidecar. No se mantienen listas de modelos fijadas para Gemini ni OpenAI: se descubren dinámicamente (`src/scanner.rs`). Las claves se cifran con DPAPI por proveedor en Windows y se leen de `LYRAFLOW_API_KEY`, `GEMINI_API_KEY`, `OPENAI_API_KEY` o `MINIMAX_API_KEY`.
- **Rutas de datos y migración**: la edición `2.0.0` persiste configuración, historial y secretos en `%LOCALAPPDATA%\LyraFlow` (y equivalentes XDG en Linux), con lectura de respaldo desde `LyraFlow-gpui-demo` para conservar datos de pruebas previas.
- **Canales de instancia única**: `stable` usa el puerto `49281`, `dev` el `49283` y las pruebas el `49285`.
- **Distribución**: un único instalador ejecutable Inno Setup por versión (`LyraFlow-Windows-X.Y.Z.exe`), instalación por usuario sin elevación y actualización en el mismo directorio (`MyAppExe = LyraFlow.exe`). No se empaquetan MSI ni MSIX.
- **Versionado**: fuente única en `Cargo.toml` (`[package] version` y `[package.metadata.lyraflow] version_code`), sincronizada con `package.json` y `packaging/windows/version.iss`. El pipeline de release aborta si `Cargo.toml` y `package.json` no coinciden.
- **Identidad visual**: la fuente canónica es [`assets/branding/icons/icon-transparent.png`](../../assets/branding/icons/icon-transparent.png) y los derivados (`icon.png`, `app_icon.ico`, `titlebar-icon-badge.png`, `packaging/windows/LyraFlow.ico`) los genera `scripts/branding/Sync-Icons.ps1`. Los iconos en superficies del sistema son transparentes; solo el icono pequeño de barra de título (`ICON_SMALL`) añade fondo blanco redondeado para contraste.
- **Apariencia**: ofrece **Aurora** (tokens globales de Core, versión `1.0.0`, en [`assets/tokens/biglex-app-base.json`](../../assets/tokens/biglex-app-base.json)) y **Clásico** (paleta propia de LyraFlow), con los modos Automático, Claro y Oscuro. Su «Acerca de» sigue la base de soporte oficial de Core.
- **Empaquetado e instalación**: el detalle operativo vive en [`Docs/packaging.md`](../../Docs/packaging.md) y el flujo vigente en `packaging/windows/`.

## 3. Procesos del proyecto

El trabajo planificado vive en `process/active/YYYY-MM-DD_objetivo/` con `PLAN.md`, `TASKS.md`, `VALIDATION.md` y `APPROVAL.md`, según el [flujo de procesos de Core](../../../Docs/global/documentation/process-workflow.md). Los moldes están en `process/templates/`, con la adaptación local de los módulos de Core. `ROADMAP.md` es la fuente de pendientes generales; no existe un `TASKS.md` en la raíz.

Los commits de resguardo y el límite de tamaño de este archivo se rigen por las [reglas de trabajo del agente](../../../Docs/global/documentation/agent-working-rules.md); no se replican aquí.

## 4. Entrada de comandos

Mise es la entrada principal: `mise dev`, `mise build`, `mise check`, `mise test`, `mise release` y `mise publish`.

- `release` prepara el instalador local (`target/release/LyraFlow.exe` → `release/LyraFlow-Windows-X.Y.Z.exe`) y **no publica** nada.
- `publish` es la única vía de publicación y requiere autorización explícita; el canal `github` omite la sincronización con Aurora.
- `--plan` muestra el comando o el plan sin ejecutar procesos, sin escribir y sin consultar la red.
- `--check` (solo en `publish`) verifica requisitos locales y remotos en modo de solo lectura.

El contrato completo está en el [estándar de comandos mediante Mise](../../../Docs/global/architecture/mise-command-standard.md). `mise.toml` y `scripts/mise-task.ts` son su implementación local: el despachador resuelve la raíz desde la ubicación del script y no contiene rutas absolutas al disco local.

Los alias de `package.json` delegan en Cargo y `scripts/`; no se añaden lanzadores propios en la raíz.

## 5. Excepciones

Ninguna. LyraFlow adopta el estilo de comunicación, los enlaces de soporte oficial, la política de ramas (`main` activa `2.0.0`, `v-kmp` histórica `1.1.9`) y el flujo de versiones definidos en Core.
