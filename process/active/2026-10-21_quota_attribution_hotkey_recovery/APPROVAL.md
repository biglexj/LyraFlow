# Aprobación del Proceso: Atribución de Cuota y Recuperación del Atajo Global

- **Estado**: Validación técnica superada · pendiente la instrucción de lanzamiento
- **Fecha**: 2026-10-21
- **Aprobador**: biglexj (solicitud de nueva versión con los dos defectos reportados)

## Criterios de Aceptación

1. El aviso de cuota agotada identifica al proveedor y al modelo seleccionados, no a un proveedor fijo.
2. La cuota agotada de un proveedor o modelo no bloquea a otra seleccion: al cambiar se reintenta la nube.
3. El atajo global de dictado sigue respondiendo tras agotarse la cuota y tras cambiar de modelo, proveedor o API key.
4. El estado de transcripcion nunca queda atascado sin peticion viva que lo respalde.
5. Suite de pruebas unitarias en verde e instalador de la version generado.

## Alcance autorizado

Correccion de los dos defectos reportados y preparacion de la version 1.1.9 con
las notas de lanzamiento correspondientes. La publicacion en GitHub requiere la
instruccion explicita de lanzamiento.

## Cierre técnico

Las comprobaciones obligatorias están validadas (suite 51/51 e instalador firmado
generado). El proceso permanece en `active` hasta que Biglex instruya el
lanzamiento; en ese momento se fusiona `preview` a `main`, se etiqueta y se
publica, y la carpeta pasa a `process/completed/2026/`.
