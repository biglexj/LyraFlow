# Base de lanzamiento 1.1.9

Autorizado por Biglex el 2026-11-03: corregir lo pendiente y retirar flujos antiguos para dejar la base preparada. No publicar.

1. Unificar la instalación de actualizaciones con Inno Setup, conservar la ruta existente y gestionar la migración del MSI anterior.
2. Retirar MSIX/WiX del flujo activo, mantener alias coherentes y comprobar la procedencia de la imagen antes de empaquetar.
3. Recuperar las descargas alternativas de Whisper cuando falle la red y probarlas sin depender de servicios externos.
4. Revisar accesibilidad de controles y navegación por teclado. Conservar la identidad visual.
5. Persistir el historial entre reinicios respetando las 24 horas seleccionadas y el borrado/desactivado.
6. Ejecutar pruebas de escritorio, comprobaciones de scripts y empaquetado local. Registrar por separado las pruebas interactivas y de migración que no se hayan realizado.
