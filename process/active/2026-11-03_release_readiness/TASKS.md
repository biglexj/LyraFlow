# Tareas — base de lanzamiento

Autorización: solicitud de Biglex del 2026-11-03. Alcance local, sin publicación.

- [x] Limpiar lanzadores de raíz y consolidar Mise como entrada principal; reubicar contexto, firma local y salidas .NET antiguas, y verificar las tareas reales de Mise.

- [x] Unificar el actualizador y usar parámetros de Inno Setup sin cmd.
- [x] Implementar detección del MSI por UpgradeCode, retirada previa y conservación de ruta.
- [x] Retirar plantillas y script de MSIX/WiX del flujo activo; mantener el alias de empaquetado apuntando a Inno.
- [x] Verificar huellas de código e imagen; retirar ForceStale y la copia externa sin firmar.
- [x] Conectar publish al pipeline explícito, con comprobaciones de GitHub y adaptador de Aurora.
- [x] Recuperar respaldos de Whisper tras errores de transporte, con pruebas locales.
- [x] Persistir el historial al cerrar, respetar retención y desactivado; no invalidar el dictado por un fallo del disco.
- [x] Añadir etiqueta al botón de eliminar y navegación nativa por el campo de instalación.
- [x] Ejecutar pruebas y generar instalador local firmado con SHA-256.
- [x] Aplicar el orden de Ajustes aprobado: dos pestañas, dos columnas y controles compactos; compilar y ejecutar 67 pruebas.
- [ ] Revisar visualmente los Ajustes y regenerar el instalador para incorporar el rediseño.
- [ ] Probar migración desde 1.1.8 en un usuario/VM de pruebas y verificar el reinicio.
- [ ] Validar interacción con teclado y lector de pantalla; dictado y Whisper reales.
- [ ] Revisar y confirmar los cambios locales antes de publicar.
- [ ] Configurar/verificar ficha y credencial reales de Aurora durante el lanzamiento autorizado.

Los pendientes funcionales y de publicación mantienen este proceso en active.
