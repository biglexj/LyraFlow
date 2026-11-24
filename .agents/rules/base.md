---
trigger: always_on
---

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

Aplicación de dictado por voz: captura con atajo global, transcribe con un proveedor de IA o con Whisper local e inserta el texto en la aplicación activa. Escritorio Windows y Linux, más base compartida compilable para Android.

Stack: Kotlin Multiplatform y Compose Multiplatform. Gradle es el motor de compilación; Mise es la entrada principal de comandos (sección 4).

La aplicación nativa Rust + GPUI vive en `native/` y declara la versión `2.0.0`, confirmada por Biglex como estable el 2026-11-19. Se opera mediante `mise dev|build|check|test gpui`; su alcance y límites de empaquetado están en [`native/README.md`](../../native/README.md). Los identificadores heredados con `gpui-demo` se conservan por compatibilidad de datos y comandos; no deben presentarse como estado del producto. Compose permanece en el repositorio.

Módulos de Core que aplican a este proyecto. Consultar solo los pertinentes a la tarea:

- **Tipo y plataforma**: [Desktop App Standards](../../../Docs/types/desktop/desktop_app_standards.md), [instalador de Windows](../../../Docs/platforms/windows/installer.md), [instancia única](../../../Docs/features/single-instance/README.md), [estado de ventana](../../../Docs/types/desktop/window-state.md).
- **Funciones adoptadas**: [actualización integrada](../../../Docs/features/auto-updater/auto_updater.md), [instaladores](../../../Docs/features/installer/README.md), [modelos de IA](../../../Docs/features/ai-models/README.md), [soporte oficial y «Acerca de»](../../../Docs/features/official-support/official-support-baseline.md), [centro de feedback](../../../Docs/features/feedback-center/feedback_center.md), [design system](../../../Docs/features/design-system/design-system-baseline.md).
- **Interfaz**: [textos de interfaz](../../../Docs/global/design/interface-writing.md), [diseño común](../../../Docs/global/design/design_system.md) y [Aurora y Clásico](../../../Docs/global/design/app-colors.md).
- **Operación y publicación**: [comandos mediante Mise](../../../Docs/global/architecture/mise-command-standard.md), [scripts y alias](../../../Docs/global/architecture/command-scripts-standard.md), [organización de carpetas](../../../Docs/global/architecture/project-structure.md), [organización de assets](../../../Docs/global/architecture/asset-organization-standard.md), [base de lanzamientos](../../../Docs/global/releases/release-baseline.md), [protocolo de lanzamiento](../../../Docs/global/releases/protocolo_lanzamiento_actualizacion.md) y [release message para la web](../../../Docs/global/releases/web-release-message-standard.md).
- **Reglas de trabajo**: [reglas de trabajo del agente](../../../Docs/global/documentation/agent-working-rules.md), [estilo de comunicación](../../../Docs/global/agents/communication-style.md) y [gobernanza documental](../../../Docs/global/documentation/documentation-governance.md).

## 2. Contratos y decisiones locales

Estos contratos son exclusivos de LyraFlow; no se replican en Core.

- **Atajos globales**: `Ctrl + Espacio` alterna grabar/detener y `Ctrl + Alt + Espacio` cancela el dictado en curso sin transcribirlo. Ambos se editan en Ajustes, no pueden coincidir entre sí y vuelven al valor por defecto ante colisión. Fuente: [`KeyboardShortcut.kt`](../../composeApp/src/commonMain/kotlin/com/biglexj/lyraflow/core/hotkey/KeyboardShortcut.kt).
- **Transcripción**: proveedores `Gemini`, `OpenAI / compatible` (endpoint y modelo editables) y `MiniMax` (transcriptor `asr-1.0` con refinamiento posterior), más `Whisper local` por sidecar. Las claves se cifran con DPAPI por proveedor en Windows y se leen de `LYRAFLOW_API_KEY`, `GEMINI_API_KEY`, `OPENAI_API_KEY` o `MINIMAX_API_KEY`.
- **Canales de instancia única**: `stable` usa el puerto `49281`, `dev` el `49283` y las pruebas el `49285`. Varias instancias simultáneas solo con `lyraflow.allowMultipleInstances=true` (`mise dev isolated`).
- **Distribución**: un único instalador ejecutable Inno Setup por versión (`LyraFlow-Windows-X.Y.Z.exe`), instalación por usuario sin elevación y actualización en el mismo directorio. No se empaquetan MSI ni MSIX.
- **Versionado**: fuente única en `gradle.properties` (`lyraflow.versionName` y `lyraflow.versionCode`), propagada a `package.json`, `AppVersion.kt` y al instalador. El pipeline de release aborta si los tres no coinciden.
- **Identidad visual**: la fuente canónica es [`assets/branding/icons/icon-transparent.png`](../../assets/branding/icons/icon-transparent.png) y los derivados los genera `scripts/branding/Sync-Icons.ps1`. Los iconos en superficies del sistema son transparentes, sin fondo cuadrado opaco.
- **Apariencia de la demo nativa**: la demo de `native/` ofrece **Aurora** (tokens globales de Core, versión `1.0.0`, copiados en [`native/assets/tokens/biglex-app-base.json`](../../native/assets/tokens/biglex-app-base.json)) y **Clásico** (la paleta propia de LyraFlow), con los modos Automático, Claro y Oscuro y la preferencia persistida en un archivo propio de la demo. La aplicación de Compose mantiene Clásico hasta que la nativa la sustituya. Su «Acerca de» sigue la base de soporte oficial de Core.
- **Empaquetado e instalación**: el detalle operativo vive en [`Docs/packaging.md`](../../Docs/packaging.md) y el flujo vigente en `packaging/windows/`.

## 3. Procesos del proyecto

El trabajo planificado vive en `process/active/YYYY-MM-DD_objetivo/` con `PLAN.md`, `TASKS.md`, `VALIDATION.md` y `APPROVAL.md`, según el [flujo de procesos de Core](../../../Docs/global/documentation/process-workflow.md). Los moldes están en `process/templates/`, con la adaptación local de los módulos de Core. `ROADMAP.md` es la fuente de pendientes generales; no existe un `TASKS.md` en la raíz.

Los commits de resguardo y el límite de tamaño de este archivo se rigen por las [reglas de trabajo del agente](../../../Docs/global/documentation/agent-working-rules.md); no se replican aquí.

## 4. Entrada de comandos

Mise es la entrada principal: `mise dev`, `mise build`, `mise check`, `mise test`, `mise release` y `mise publish`.

- `release` prepara el instalador local y **no publica** nada.
- `publish` es la única vía de publicación y requiere autorización explícita; el canal `github` omite la sincronización con Aurora.
- `--plan` muestra el comando o el plan sin ejecutar procesos, sin escribir y sin consultar la red.
- `--check` (solo en `publish`) verifica requisitos locales y remotos en modo de solo lectura.

El contrato completo está en el [estándar de comandos mediante Mise](../../../Docs/global/architecture/mise-command-standard.md). `mise.toml` y `scripts/mise-task.ts` son su implementación local: el despachador resuelve la raíz desde la ubicación del script y no contiene rutas absolutas al disco local.

`gradlew` y `gradlew.bat` se conservan como wrappers técnicos de Gradle. Los alias de `package.json` y las tareas de Gradle delegan en `scripts/`; no se añaden lanzadores propios en la raíz.

## 5. Excepciones

Ninguna. LyraFlow adopta el estilo de comunicación, los enlaces de soporte oficial, la política de ramas y el flujo de versiones definidos en Core; este archivo solo los enlaza y conserva los contratos exclusivos del producto.
