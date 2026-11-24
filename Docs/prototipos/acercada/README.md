# Componente «Acerca de» del ecosistema Biglex

Prototipo del «Acerca de» compartido por las aplicaciones del ecosistema, con tres
presentaciones del mismo contenido. La referencia visual es el «Acerca de» de Prisma
Upscaler; la estructura y los tokens son los de Core.

## Ver el prototipo

```powershell
start Docs/prototipos/acercada/index.html
```

Parámetros de la URL, útiles para revisar capturas: `?tema=claro|oscuro` y
`?estado=aldia|nueva`.

## Presentaciones

| Presentación | Marco | Cuándo usarla |
| --- | --- | --- |
| Ventana flotante | Ventana propia con barra de título | Cuando la vista se abre aparte y necesita todo el ancho. |
| Dentro de Ajustes | Página de la navegación de Ajustes, con cabecera sencilla | Cuando la app ya tiene Ajustes y el «Acerca de» es una sección más. |
| Cabecera de producto | La misma página con la identidad sobre una superficie Aurora y halo turquesa detrás del icono | **Propuesta principal**: da más peso a la identidad sin cambiar el contenido. |

Las tres montan el mismo componente de contenido: solo cambia la cabecera y el marco.

## Contenido compartido

1. **Identidad.** Icono real, nombre, versión instalada y canal cuando la app lo use.
2. **Actualizaciones.** Tarjeta a todo el ancho con el estado actual, «Ver novedades» y
   «Buscar actualizaciones». Cuando se confirma una versión nueva, indica cuál es y
   ofrece la acción del mecanismo real de la app: «Actualizar» si el actualizador es
   interno, «Descargar» si el usuario debe obtener el instalador.
3. **Dos columnas.** Izquierda, «Ayuda y proyecto»: soporte oficial, repositorio de
   GitHub con su icono y página de la app (la ficha en BiglexJ.com o «Desarrollo →
   Apps» si todavía no hay ficha). Derecha, «Apoyar el desarrollo»: dos botones de igual
   peso visual, uno debajo del otro, «Apoyar en BiglexJ.com» y «Cómprame un café».
4. **Pie.** «Información técnica» desplegable con compilación, plataforma, canal,
   versión y repositorio. En una app pública, «Créditos» visible al pie; en una app
   privada, «Licencia de uso» directamente.

En ventanas estrechas los nombres se ajustan a varias líneas y las columnas se apilan
cuando ya no se pueden leer cómodamente.

## Tokens

Usa los tokens Aurora de Core (`biglex-app-base`, versión `1.0.0`). En oscuro: fondo
`#020908`, superficie `#071512`, superficie elevada `#0B1D18`, texto `#FFFFFF`. En
claro: fondo `#FAFFFD`, superficie `#F6FBF9`, superficie elevada `#EAF6F1`, texto
`#000000`. El botón principal usa `#00C9B9` con texto e icono negros, esquinas
moderadas y sin contorno; al pasar el cursor se aclara a `#40DFD1`. Los dos botones de
apoyo usan superficies tonales.

## Datos

`acercada.js` mantiene un único objeto `PRODUCTO` con los datos de la app: nombre,
versión, canal, lema, plataforma, repositorio, ficha, soporte, café, autoría, licencia y
si la app es pública. Cambian por aplicación; la estructura no.

Los destinos son constantes del ecosistema y salen de la [base de soporte
official](../../../../Docs/features/official-support/official-support-baseline.md):

| Destino | Enlace |
| --- | --- |
| Donaciones oficiales | `https://www.biglexj.com/donaciones` |
| Buy Me a Coffee | `https://buymeacoffee.com/biglexj` |
| Perfil de GitHub | `https://github.com/biglexj` |
| Ficha de la app | `https://biglexj.com/desarrollo/<id>` |

El icono se toma de la exportación canónica del repositorio
(`assets/branding/icons/icon-transparent.png`), sin copias.

## Llevarlo a una app nativa

La estructura es la misma; solo cambian los componentes:

- **Rust y GPUI**: una vista con la tarjeta de actualizaciones, las dos columnas y el
  pie; el «Actualizar» llama al actualizador de la app y el pie despliega la
  información técnica al pulsar.
- **Compose Multiplatform**: `Column` y `Row` con la misma jerarquía, `FilterChip` o
  botones con `surfaceVariant` para el apoyo y un `AnimatedVisibility` para la
  información técnica.

En ambos casos el estado de la tarjeta de actualizaciones lo aporta la app: la demo
GPUI no incluye actualizador, así que solo muestra el estado y «Ver novedades».

## Relación con Core

El estándar de este componente es la [base de soporte oficial y «Acerca de»](../../../../Docs/features/official-support/official-support-baseline.md)
de Core, con la jerarquía de la vista y los destinos oficiales. La promoción de este
prototipo a Core como componente compartido queda pendiente de decisión.
