---
trigger: always_on
---

# 📁 Regla de Estructura de Carpetas — LyraFlow

> [!CAUTION]
> Esta regla es **CRÍTICA y no negociable**. Todo nuevo archivo, carpeta o módulo creado por el agente DEBE seguir esta convención. Violar esta estructura es inaceptable y debe ser corregido inmediatamente.

## Estructura Raíz del Proyecto

```
LyraFlow/                           # Raíz del repositorio
├── .agents/rules/                  # Reglas del agente (base.md, folder_structure.md, ...)
├── Core/                           # Lógica central del sistema
├── Services/                       # Servicios de negocio y comunicación
├── UI/                             # Vistas e interfaces organizadas por features
├── Docs/                           # Documentación técnica y guías del proyecto
├── scratch/                        # Scripts utilitarios de mantenimiento (solo raíz)
├── test/                           # Scripts de prueba temporales (ignorado en .gitignore)
├── agent.md                        # Instrucciones principales del agente (raíz)
├── ROADMAP.md                      # Plan de trabajo y prioridades
├── RELEASE_NOTES.md                # Historial de cambios por versión
├── RELEASE_MESSAGE.md              # Mensaje de anuncio del último lanzamiento
└── README.md                       # Documentación pública del proyecto
```

## Reglas de Nomenclatura [CRÍTICO]

| Elemento | Convención | Ejemplo |
|---|---|---|
| Carpetas de feature | `PascalCase` o `camelCase` según stack | `MusicFeature/`, `music/` |
| Archivos de componente | `PascalCase` + sufijo de tipo | `HomeScreen.kt`, `ResultCard.kt` |
| Archivos de modelo / data classes | `PascalCase` | `TranscriptionRequest`, `AppPreferences` |
| Constantes | `SCREAMING_SNAKE_CASE` | `DEFAULT_SYSTEM_PROMPT` |
| Variables / funciones | `camelCase` | `processDictation()`, `systemPromptMode` |

## Reglas Estructurales Obligatorias

### ✅ PERMITIDO
- Usar `test/` en la raíz para scripts temporales de prueba.
- Usar `scratch/` en la raíz para scripts de mantenimiento, organizados en subcategorías.

### ❌ PROHIBIDO — VIOLACIONES COMUNES A EVITAR
- **Nunca** crear carpetas `scratch/` dentro de carpetas de código fuente (`src/`, `composeApp/`).
- **Nunca** colocar archivos de lógica de negocio directamente en la raíz sin una estructura semántica.
- **Nunca** duplicar componentes: si ya existe en componentes compartidos, importarlo; no copiarlo.

## Regla de Crecimiento de Archivos

Como buena práctica, se debe **evitar normalmente que un archivo supere las 800 - 900 líneas**. El límite máximo permitido es de **1000 a 1200 líneas** (pudiendo llegar excepcionalmente hasta **1220 líneas**). Los archivos que superen las **1200 - 1220 líneas** son **deuda técnica activa** y el agente DEBE proponer su división en sub-componentes y registrarlo en el ROADMAP como tarea de refactorización pendiente.
