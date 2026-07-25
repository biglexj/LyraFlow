---
trigger: always_on
---

# 🚀 Regla y Sistema de Auto-Actualización desde GitHub Releases

> [!IMPORTANT]
> Todos los proyectos de aplicación (Android, Kotlin Multiplatform, Desktop) DEBEN incluir compatibilidad para la **comprobación automática y descarga directa de actualizaciones desde GitHub Releases**.

## Requerimientos Obligatorios

1. **Verificación Silenciosa al Iniciar**:
   - Al iniciar la aplicación, consultar la API de GitHub (`https://api.github.com/repos/{owner}/{repo}/releases/latest`) en segundo plano sin interrumpir al usuario.
   - **No mostrar ningún mensaje** si ya se está en la última versión (el check de inicio es transparente).
2. **Verificación Manual**:
   - Si el usuario pulsa "Buscar actualizaciones" (en Ajustes o en el diálogo Acerca de), mostrar el resultado siempre:
     - Hay nueva versión → mostrar el `UpdateBanner`.
     - Ya en la última versión → mostrar un **toast global flotante** (✅ "Estás en la última versión") que se auto-descarta a los 4 segundos con `AnimatedVisibility` + `fadeOut`.
3. **Comparación Semántica de Versiones**:
   - Comparar la versión de la app instalada contra el `tag_name` de GitHub usando el módulo `UpdateChecker.isNewerVersion`.
4. **Descarga e Instalación Directa en Android**:
   - En Android, utilizar `DownloadManager` para encolar la descarga del archivo `.apk` directamente en la carpeta de descargas del dispositivo.
   - Solicitar la instalación (`Intent.ACTION_VIEW` con `application/vnd.android.package-archive`).
5. **Enlace Directo o Navegador en Desktop**:
   - Redirigir al archivo instalador o a la página de la release de GitHub.
6. **Sanitización del `body` de GitHub Releases**:
   - El campo `body` de la API de GitHub viene en Markdown crudo con `\r\n`, `**negrita**`, `##Títulos`, etc.
   - **Siempre sanitizar** antes de mostrarlo en la UI con la función `sanitizeMarkdown()` (ver guía técnica `github_auto_updater_guide.md`).
   - Limitar a 2 líneas máximo con `overflow = TextOverflow.Ellipsis`.
7. **Interfaz No Intrusiva (Material Expressive)**:
   - Mostrar un banner de notificación de actualización (`UpdateBanner`) discreto solo cuando exista una versión superior, permitiendo al usuario "Actualizar" o "Ahora no".
