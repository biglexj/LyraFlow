# Sprint v1.1.2 & Sincronización de Plantillas — Tareas

- Estado: `IN_PROGRESS`

## Ejecución

### Fase 1: Sincronización e Infraestructura (COMPLETADO)
- [x] T01 — Implementar cierre automático de proceso previo y cadena de auto-reinicio en `DesktopAutoUpdater.kt`.
- [x] T02 — Ejecutar el script `init_project_docs.py` para generar la estructura de procesos.
- [x] T03 — Sincronizar y actualizar `agent.md` y las reglas en `.agents/rules/` con las plantillas maestras.
- [x] T04 — Reorganizar `ROADMAP.md` al estándar de 4 bloques.
- [x] T05 — Eliminar `TASKS.md` de la raíz del proyecto.
- [x] T06 — Mover planes viejos en `plan/` a `process/archive/2026/` y eliminar la carpeta `plan/` de la raíz.
- [x] T07 — Corregir bug de múltiples instancias (remover flag dev de jvmArgs globales en `build.gradle.kts` e inyectarlo en `run`).
- [x] T08 — Registrar el Thunderbolt en la Documentación Core en `D:\Proyectos\biglexj\Core-Docs\troubleshooting\`.

### Fase 2: Pendientes de Plantillas y Deuda Técnica (PENDIENTE)
- [ ] T09 — Implementar **Sección 6 (Personalización y Localización del Instalador)**: Configurar interfaz en español (`es-PE`/`es-ES`), banner e imagen de branding oficial y metadatos de copyright `biglexj` en los scripts de empaquetado (`scripts/packaging/` o `build-release.ps1`).

Las pruebas no se documentan aquí. Deben registrarse en `VALIDATION.md`.
