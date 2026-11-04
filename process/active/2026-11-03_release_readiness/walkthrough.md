# Resultado local

LyraFlow 1.1.9 tiene un único flujo activo de instalación, basado en Inno Setup, con gestión explícita del MSI anterior y relanzamiento desde el destino instalado. La publicación exige -Publish; por defecto los comandos de release solo preparan la entrega local.

El historial de escritorio se guarda en disco y se filtra por el plazo seleccionado al cargar, guardar y durante la ejecución. El valor predeterminado de 24 horas permanece. El borrado/desactivado elimina el archivo de historial; no se crean copias fuera de su retención. Un fallo del disco no invalida una transcripción terminada.

Whisper intenta los respaldos tras errores de transporte. Se retiraron el actualizador duplicado, MSIX y la personalización WiX; el generador de iconos apunta al instalador vigente. El campo de ruta del instalador vuelve a ser editable y navegable; la acción de eliminar una transcripción tiene una etiqueta accesible.

Evidencia y limitaciones: [VALIDATION.md](VALIDATION.md). La base está preparada localmente; no se declara aceptada la actualización real ni se ha publicado.

Revisión posterior de Ajustes: distribución compacta en dos columnas desde 720 dp, pestañas alineadas a la izquierda y contenido agrupado por uso. General sigue el orden dictado → comportamiento → preferencias; Modelos separa conexión y transcripción. Compilación y 67 pruebas aprobadas. La revisión visual y la incorporación de este rediseño al instalador permanecen pendientes.
