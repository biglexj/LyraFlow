# Sprint v1.1.2 & Sincronización de Plantillas — Plan

- Estado: `APPROVED`
- Fecha: `2026-08-08`
- Proyecto: `LyraFlow`

## Objetivo

Completar las tareas pendientes del Sprint v1.1.2 (Fase 2 de Personalización del Instalador) y alinear la estructura del repositorio con las plantillas maestras de Core-Docs.

## Alcance

- **Incluye**:
  - Sincronización de `agent.md` y reglas locales en `.agents/rules/` con la Documentación Core.
  - Creación de la estructura del módulo `process/` y plantillas de proceso locales.
  - Migración y eliminación de `TASKS.md` de la raíz.
  - Reorganización de `ROADMAP.md` a la estructura de 4 bloques.
  - Implementar Sección 6 (Personalización y Localización del Instalador en `es-PE`/`es-ES`, banner e imagen de branding oficial y metadatos de copyright).
- **No incluye**:
  - Nuevas integraciones funcionales de audio o cambios en el motor de transcripción ajenos al empaquetado/instalador.

## Enfoque

1. Sincronizar las reglas del agente y la estructura de directorios en el repositorio.
2. Migrar las tareas del sprint v1.1.2 al flujo activo de procesos, eliminando el archivo de la raíz.
3. Actualizar `ROADMAP.md` al estándar de 4 bloques.
4. Implementar los detalles de personalización de la Sección 6 en la configuración del empaquetador del instalador.
5. Ejecutar la validación completa del proceso.

## Criterios de finalización

- [x] Reglas del agente sincronizadas y por debajo de 12k caracteres.
- [x] Carpeta `process/` y sus archivos creados correctamente.
- [x] Sin `TASKS.md` en la raíz del proyecto.
- [x] `ROADMAP.md` reorganizado con 4 bloques.
- [ ] Configuración del empaquetado del instalador (`build-release.ps1` o scripts de packaging) con la Sección 6 integrada.
- [ ] Validación del proceso registrada y firmada en `APPROVAL.md`.

## Autorización

- [x] Plan aprobado para ejecución.
