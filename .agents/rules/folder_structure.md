---
trigger: always_on
---

# 📁 Regla de Estructura de Carpetas — LyraFlow

> [!CAUTION]
> Esta regla es **CRÍTICA y no negociable**. Todo nuevo archivo, carpeta o módulo creado por el agente DEBE seguir esta convención.

## Estructura Raíz del Proyecto

```
LyraFlow/                           # Raíz del repositorio
├── .agents/rules/                  # Reglas del agente (base.md, folder_structure.md)
├── Core/                           # Lógica central del sistema
├── Services/                       # Servicios de negocio y comunicación
├── UI/                             # Vistas e interfaces organizadas por features
├── Docs/                           # Documentación técnica
├── scratch/                        # Scripts utilitarios de mantenimiento (solo en raíz)
├── test/                           # Scripts de prueba temporales (ignorado en .gitignore)
├── agent.md                        # Instrucciones principales del agente (raíz)
├── INSTRUCCIONES_AGENTE.md          # Archivo ejecutable directo para el agente (raíz)
├── ROADMAP.md                      # Plan de trabajo y prioridades
├── RELEASE_NOTES.md                # Historial de cambios por versión
└── README.md                       # Documentación pública del proyecto
```

## Reglas Estructurales Obligatorias
- **Uso de `scratch/`**: Solo en la raíz del proyecto para scripts utilitarios.
- **Límite de líneas**: Archivos de más de **400 líneas** deben dividirse en sub-módulos/componentes.
- **Centro Oficial de Documentación**: Para cualquier duda sobre arquitectura, consultar [d:\Proyectos\biglexj\Aurora---Blog\docs\es](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es).
