# 🎯 LyraFlow — Roadmap

Plan de trabajo, objetivos y prioridades del proyecto.

## 🔴 Pendientes activos
- [ ] Personalización y Localización del Instalador de Escritorio (`.exe` / `.msi`) según **Sección 6 de Desktop App Standards** (pantallas e interfaz en español `es-PE`/`es-ES`, banner de branding oficial, icono personalizado en panel de programas de Windows).
- [ ] Validar manualmente endpoints multimodales de terceros configurados por el usuario en Windows.
- [ ] Añadir adaptadores nativos únicamente cuando un proveedor no exponga audio mediante OpenAI-compatible.

## 🟡 Intermedio
- [ ] Integración avanzada con el Centro de Feedback oficial (`https://www.biglexj.com/feedback`) enviando metadatos completos por API JSON (`app`, `version`, `os`, `type`).
- [ ] Añadir perfiles de proveedor para conservar también modelos y endpoints recientes por servicio.
- [ ] Incorporar una prueba de integración opcional contra un endpoint configurado por el usuario.

## ⚪ Descartado / En Pausa
*(No hay tareas en esta categoría actualmente)*

## 🟢 Completado
- [x] **v1.1.7** (2026-09-04) — Renovación de identidad visual con nuevo icono estilizado y suave, erradicación del *Render Lifecycle Lock* en Windows 11 DWM (lazy status overlay y elevación nativa Win32), distribución exclusiva en ejecutables EXE para auto-actualización in-app de fricción cero y robustecimiento de sockets IPC.
- [x] **v1.1.6** (2026-08-30) — Estandarización de arquitectura de instancia única y reactivación según Core-Docs (aislamiento de canales `stable`/`dev`, eliminación de heurísticas y flags en producción, despacho IPC y restauración de foco en Windows) y reorganización modular de activos según el Asset Organization Standard.
- [x] **v1.1.5** (2026-08-16) — Escaneo dinámico de modelos multimodales desde APIs oficiales de Gemini y OpenAI con persistencia local en caché, botón 'Guardar modelo' con colapso visual, nuevo prompt maestro de edición estructurada, soporte ampliado de Drag & Drop para imágenes y audio, garantía estandarizada de Single-Instance Lock con restauración de foco en Windows, auto-expiración de cuotas (cooldown 60s), widget flotante de estado persistente y drenaje completo del búfer PCM.
- [x] **v1.1.4** (2026-08-04) — Calibración nativa de Whisper local (`-l es`, `-nt`, `-np`, `--prompt`), multithreading adaptativo, desactivación independiente de proveedores (Nube / Whisper) y simetría M3 Expressive en diálogos modales.
- [x] **v1.1.3** (2026-08-03) — Fallback inteligente de 3 intentos, conmutación autónoma a Whisper local ante cuota agotada (HTTP 429), selector interactivo de modelos Whisper, auto-actualización canónica con `UpdateModalDialog` e inmunidad a deadlocks de renderizado GPU.
- [x] **v1.1.2** (2026-08-02) — Cierre automático de proceso previo (`taskkill /f /im LyraFlow.exe`) antes de actualización en caliente, auto-reinicio automático in-app (`cmd.exe start /wait && start`), y sincronización con normas maestras de tamaño de archivos (800-1200 líneas).
- [x] **v1.1.1** (2026-08-02) — Dictado inteligente & transcripción multimodal con Drag & Drop, Pestañas de System Prompt (*Voz Original* vs *Inteligente*), Single-Instance Lock con listener socket ping y bypass dev (`isDev`), Auto-actualizador In-App silencioso con barra de progreso y reinicio en caliente, Persistencia de estado de ventana (Sección 5), Catálogo GPT-5.6 (`gpt-5.6-luna`, `gpt-5.6-terra`, `gpt-5.6-sol`) y Feedback Center.
- [x] In-App Silent Auto-Update con barra de progreso, descarga en segundo plano, instalación pasiva silenciosa y auto-reinicio automático del ejecutable recién instalado.
- [x] Persistencia de Estado de Ventana (Sección 5: ancho, alto, posición y estado de maximizado entre sesiones).
- [x] Single-Instance Lock inteligente con listener socket ping y bypass automático en entorno de desarrollo (`-Dlyraflow.dev=true`).
- [x] Pestañas de System Prompt (*Voz Original* / literal vs *Inteligente*) con migración reactiva al ingresar API Key por primera vez.
- [x] Catálogo actualizado de OpenAI a la familia GPT-5.6 (`gpt-5.6-luna`, `gpt-5.6-terra`, `gpt-5.6-sol`, `gpt-audio-5.6`) con auto-migración de claves guardadas.
- [x] Botón de Feedback Center e integración directa con GitHub Issues en modal "Acerca de".
- [x] **v1.0.9** (2026-07-27) — Detección dinámica de tema oscuro nativo en tiempo real y rediseño de apariencia Material 3 Expressive.
- [x] Hito inicial del proyecto
- [x] Actualizar iconos con fondo y esquinas redondeadas para la aplicación y la bandeja de sistema.
- [x] Separar el dominio de dictado de Gemini mediante un catálogo de proveedores multimodales.
- [x] Añadir adaptador OpenAI-compatible con endpoint y modelo editables e indicativo de compatibilidad experimental.
- [x] Migrar claves cifradas y preferencias para Gemini y proveedores compatibles.
