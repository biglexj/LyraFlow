# Validación

Prototipo renderizado el 2026-11-17 con Edge en modo headless, desde el archivo local.

- [x] `Docs/prototipos/acercada/index.html` carga sin errores: estructura, hoja de estilos y componente de contenido.
- [x] Las tres presentaciones se montan con el mismo contenido: ventana flotante, página de Ajustes y cabecera de producto.
- [x] Modo claro: fondo `#FAFFFD`, superficie `#F6FBF9`, superficie elevada `#EAF6F1`, texto `#000000`, acento `#00C9B9` con texto negro y superficies tonales en los botones de apoyo.
- [x] Modo oscuro: fondo `#020908`, superficie `#071512`, superficie elevada `#0B1D18`, texto `#FFFFFF`, acento `#00C9B9` con texto negro.
- [x] Estado «al día»: la tarjeta de actualizaciones muestra el estado, «Ver novedades» y «Buscar actualizaciones».
- [x] Estado «nueva versión»: la tarjeta indica la versión disponible y la instalada, y ofrece «Actualizar», la acción del actualizador interno de LyraFlow.
- [x] Enlace del repositorio con su icono, soporte oficial y página de la app apuntan a los destinos reales.
- [x] Ventana estrecha (520 px): la navegación de Ajustes pasa a fila, las columnas se apilan y los nombres se ajustan a varias líneas.
- [x] El icono procede de la exportación canónica del repositorio, sin copias.

Límites:

- El prototipo es web; la traslación a GPUI y Compose está documentada pero no implementada.
- No se ha medido el contraste con una herramienta de accesibilidad; los valores son los de Aurora.
- La aceptación visual de Biglex no está registrada.

No se han modificado aplicaciones publicadas: el cambio afecta solo al prototipo y a su documentación.

## Comprobación nativa — 2026-11-19

- `mise check gpui`: compilación estática correcta del nuevo diálogo.
- Pruebas existentes de `native/src/about.rs`, compiladas de forma aislada con rustc y la versión de Cargo 2.0.0: 3 correctas, 0 fallos. Comprueban datos del producto y destinos oficiales; no hacen solicitudes de red.
- `git diff --check`: sin errores de espacios.
- Apertura y revisión visual del diálogo: en curso.

No se ejecuta publicación ni instalación. La versión estable es la confirmación de Biglex; esta comprobación no verifica una release remota.

Resultado de la revisión nativa: `mise dev gpui` compiló y abrió LyraFlow. Se abrió «Acerca de» desde la navegación y se inspeccionó visualmente: cabecera, logo oficial, v2.0.0, Windows 64-bit, licencia MIT, autoría, dos vías de apoyo, enlaces propios y pie desplegable, sin texto de demo. Diálogo visible sin recortes en la ventana observada (1212 × 902 px). No se accionaron enlaces externos. El despliegue de la licencia, el modo claro y la ventana estrecha no se probaron visualmente en esta sesión. Se dejó el diálogo abierto para revisión de Biglex.

Ajuste solicitado por Biglex: «biglexj.com» apunta a https://biglexj.com/desarrollo/lyraflow, que reúne la ficha y las novedades. Se retiran los accesos separados «Página de LyraFlow» y «Ver novedades». `mise check gpui` y `mise dev gpui` correctos; diálogo abierto y fila de cuatro enlaces revisada visualmente. Destino comprobado en el código; no se navegó a la página externa.

Iconos solicitados para el «Acerca de»: GitHub en el perfil, globo en la página, enlace externo en el repositorio, reporte de errores, corazón en donaciones, taza en Buy Me a Coffee y escudo en licencia. SVG incrustados en LyraAssets, sin descargas en ejecución. `mise check gpui` y `mise dev gpui` correctos. Diálogo revisado visualmente en Clásico oscuro: los siete iconos son visibles y la fila de enlaces cabe en el ancho observado. Se conserva el destino de todos los enlaces. Diálogo dejado abierto.

Ajuste de relleno: Donaciones oficiales y Buy Me a Coffee usan el tinte de su color sobre la superficie, con hover del mismo tono. Regla añadida a Core. `mise check gpui` y `mise dev gpui` correctos; rellenos revisados visualmente en Clásico oscuro. El modo claro no se revisó visualmente en este ajuste.

## Actualizaciones y fondo modal — 2026-11-20

Encargo de Biglex: comprobación de actualizaciones entre identidad y apoyo, y fondo desenfocado para evitar los textos translúcidos y manchas de la interfaz posterior.

- `mise check gpui`: correcto.
- `mise test gpui`: 30 pruebas correctas, incluidas dos nuevas de comparación semántica de versiones y rechazo de datos inválidos/preliminares.
- Consulta real al iniciar y consulta manual observadas en Windows: instalada 2.0.0, última release estable pública devuelta por GitHub 1.1.9. Se informa que la instalada es posterior; no se propone downgrade ni se afirma que GitHub tenga publicada la 2.0.0.
- Se observó «Buscando actualizaciones…» y el retorno al resultado de la consulta real. No se descargó ni ejecutó ningún instalador.
- Desenfoque real comprobado en Clásico claro mediante captura del cliente de LyraFlow. Se redujo saturación y reforzó el velo para atenuar la franja del botón principal; fondo sin textos legibles. Logo y contenido del modal visibles; se dejó abierto para Biglex.
- Alternativa de fondo uniforme implementada para captura fallida y plataformas sin este método; no se forzó su validación. No se revisó visualmente el modo oscuro ni el cambio de tamaño con el modal abierto.
- La instantánea solo permanece en memoria mientras se usa el modal; no se escribe ni se transmite. Descarga, instalación y actualizador completo siguen pendientes.

La regla compartida se actualizó en Core, manteniendo allí el criterio visual y en native/README.md el mecanismo específico de Windows.
