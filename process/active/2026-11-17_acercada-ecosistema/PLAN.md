# «Acerca de» del ecosistema

Objetivo: definir el componente «Acerca de» compartido por las aplicaciones del ecosistema Biglex y preparar sus tres presentaciones, con la estructura y los tokens de Core.

Origen: encargo de Biglex (2026-11-17) para diseñar la vista «Acerca de» de las aplicaciones del ecosistema, usando Prisma como ejemplo visual.

Fuentes normativas: [base de soporte oficial y «Acerca de»](../../../../Docs/features/official-support/official-support-baseline.md), [Aurora y Clásico](../../../../Docs/global/design/app-colors.md), [textos de interfaz](../../../../Docs/global/design/interface-writing.md) y [organización de proyectos](../../../../Docs/global/architecture/project-structure.md).

## Alcance

- Un componente de contenido compartido con esta distribución, idéntica en las tres presentaciones:
  1. **Identidad**: icono real, nombre, versión instalada y canal cuando la app lo use.
  2. **Actualizaciones**: tarjeta a todo el ancho con el estado actual, «Ver novedades» y «Buscar actualizaciones»; si se confirma una versión nueva, se indica cuál es y se ofrece la acción del mecanismo real («Actualizar» o «Descargar»).
  3. **Dos columnas**: «Ayuda y proyecto» (soporte oficial, repositorio de GitHub con su icono, página de la app) y «Apoyar el desarrollo» (dos botones de igual peso, «Apoyar en BiglexJ.com» y «Cómprame un café»).
  4. **Pie**: «Información técnica» desplegable (compilación, plataforma, canal, versión, repositorio); «Créditos» en apps públicas y «Licencia de uso» en las privadas.
- Tres presentaciones del mismo componente: ventana flotante, página de Ajustes y cabecera de producto sobre superficie Aurora, que es la propuesta principal.
- Tokens Aurora de Core en claro y oscuro, botón principal `#00C9B9` con texto e icono negros y hover `#40DFD1`, y botones de apoyo sobre superficies tonales.
- Comportamiento en ventanas estrechas: los nombres se ajustan a varias líneas y las columnas se apilan cuando ya no se leen cómodamente.

## Entregable

Prototipo web en [`Docs/prototipos/acercada/`](../../Docs/prototipos/acercada/): HTML para la estructura, CSS Grid para las columnas y SVG para iconos, con un único componente de contenido y tres montajes. Los datos salen de la configuración real del producto y de la base de soporte oficial.

## Fuera de alcance

- Cambiar el «Acerca de» de las aplicaciones ya publicadas: cada app adopta el componente cuando lo migra o lo revisa, con su propio criterio.
- La promoción del prototipo a Core como componente compartido: requiere decisión y sigue el [flujo de promoción](../../../../Docs/global/documentation/core-promotion-workflow.md).
- El actualizador de la demo GPUI, que no existe; su «Acerca de» muestra el estado y «Ver novedades».

Criterios de finalización: prototipo navegable con las tres presentaciones y los datos reales, revisión visual en claro y oscuro, comportamiento en ventana estrecha y documento de traslación a apps nativas.

## Adopción en LyraFlow nativo — 2026-11-19

Biglex confirma que la base nativa ya pasó a estable y solicita un «Acerca de» similar a la referencia de Prisma Upscaler, con el logo propio de LyraFlow. Este encargo autoriza la adopción en native/, sin cambiar otras aplicaciones ni publicar una versión.

Se conserva la versión 2.0.0 declarada en Cargo.toml y los identificadores/rutas heredados para evitar cambios de datos. El diálogo muestra la identidad de LyraFlow, sus destinos oficiales y la licencia MIT real. Los temas Aurora y Clásico conservan sus tokens.
