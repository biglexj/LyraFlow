# Validación — 2026-11-03

Estado: implementación y empaquetado locales verificados; aceptación funcional y publicación pendientes.

## Limpieza de raíz y entrada Mise

- Retirados los lanzadores raíz `dev.bat` y `build-release.ps1`. Mise, Gradle y package.json apuntan directamente al script canónico de release.
- Mise dispone de dev (incluido isolated/android), build, check (incluido desktop), test (incluido scripts), release local y publish explícito. Se rechazan variantes y opciones desconocidas.
- Certificado local reubicado en `.local/signing/` y directorio ignorado en Git; el pipeline también admite `LYRAFLOW_SIGNING_CERTIFICATE`. No se cambió el contenido del certificado.
- context.md retirado de la raíz; no se utiliza como system prompt en ejecución. Su contenido histórico permanece en Git. La copia documental propuesta también fue retirada en cambios concurrentes y no se recreó.
- Los restos .NET de bin/ y obj/ y el directorio package/ vacío se reubicaron en `temp/root-cleanup-2026-11-03/`. Una operación agrupada fue rechazada por la revisión automática sin un motivo específico; se optó por el traslado reversible, sin eliminar los restos ni cerrar procesos.
- `mise release --plan`, `mise dev isolated --plan`, `mise build --plan` y `mise publish github --plan` resuelven los comandos vigentes. No se ejecutó una publicación.
- `mise test scripts`: 4 pruebas aprobadas. `mise check desktop`: compilación aprobada. PowerShell sin errores de sintaxis y `git diff --check` aprobado.

La limpieza respeta los cambios concurrentes detectados en las reglas especializadas de `.agents/`; no se restauraron esos archivos retirados.

## Revisión posterior de Ajustes — 2026-11-03

- Escala en pantallas grandes: General y Modelos comparten una variante ampliada desde 1200 × 700 dp disponibles. Tipografía +15 %, títulos y etiquetas con mayor peso, tarjetas con relleno de 24 dp, mayor separación, chips de 44 dp y acciones de 48 dp, con contornos más gruesos. La variante compacta conserva sus tamaños y se respeta la escala de texto del sistema. Compilación de escritorio y revisión del diff aprobadas; pendiente revisión visual.

- Inicio: panel de dictado más alto, con crecimiento progresivo según el alto disponible en ventanas anchas. La animación pasa de 56 dp a 72–128 dp; botón de dictado de 52 dp a 56–60 dp, con mayor espacio vertical e icono ligeramente mayor en pantalla grande. El botón y el resultado quedan más abajo al maximizar. Compilación de escritorio aprobada; pendiente revisión visual.

- Ancho aprobado por Biglex en pantalla completa: retirado el máximo de 1160 dp del encabezado y del contenido de Ajustes. General y Modelos usan todo el ancho disponible con sus márgenes habituales y dos columnas; los campos de modelo, endpoint y clave también llenan su columna. Las pestañas siguen compactas a la izquierda. Compilación de escritorio aprobada; pendiente revisión visual.

- Prompts: Inteligente refuerza la separación de ideas/enfoques en párrafos; Voz original conserva palabras y orden, permite puntuación y párrafos y elimina la contradicción de «sin estructurar». La plantilla queda redactada en párrafos. Al cargar preferencias se actualizan las instrucciones integradas y únicamente la copia exacta de la plantilla literal anterior en Personalizado; los textos personalizados distintos se conservan íntegros. Compilación y 70 pruebas aprobadas (3 nuevas sobre persistencia/migración de prompts). No se evaluó un dictado real contra un proveedor; Whisper local sin refinamiento no interpreta estos system prompts.

- Ajuste sobre las capturas posteriores: retirado el resaltado hover de las filas de switches, conservando la activación por fila y teclado; eliminado el bloque duplicado de versión/actualizaciones al pie de Ajustes. Acerca de permanece en la navegación lateral/móvil y conserva su comprobación de actualizaciones. Compilación de escritorio aprobada.

- Ajuste posterior solicitado sobre la captura: selector de modelo, acciones Guardar/Ver/Escanear y lista de modelos movidos a una tarjeta debajo de Conexión en la columna izquierda. Transcripción empieza directamente por el modo y las instrucciones. `:composeApp:compileKotlinDesktop` aprobado; pendiente revisión visual.

- Orden aprobado por Biglex: dos pestañas y dos columnas. General agrupa atajos, comportamiento, apariencia/historial y una fila de Acerca de. Modelos separa conexión y transcripción.
- Se reducen rellenos y jerarquías repetidas; los selectores y acciones envuelven su contenido sin desbordar las columnas. Dos columnas a partir de 720 dp disponibles; una por debajo. Las pestañas quedan alineadas a la izquierda.
- Se muestran las instrucciones personalizadas solo en su modo y las funciones experimentales mediante despliegue. La captura de atajos, claves, proveedores y preferencias conserva sus callbacks.
- `:composeApp:compileKotlinDesktop` y `:composeApp:desktopTest` aprobados; 67 pruebas, sin fallos ni errores. `git diff --check` aprobado.
- Pendiente revisión visual de esta distribución en la app. El instalador y SHA-256 registrados debajo corresponden a la base anterior a este rediseño; no incluyen todavía estos cambios de Ajustes. Debe ejecutarse el pipeline oficial al preparar el siguiente artefacto.

## Evidencia de la base anterior al rediseño

- Pipeline oficial: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/release/build-release.ps1 -LocalOnly`, terminado con código 0.
- Escritorio: 67 pruebas, 0 fallos, 0 errores. Incluye 5 casos del respaldo de Whisper, 4 de persistencia del historial, 2 de argumentos del instalador y recuperación del dictado cuando el historial falla.
- Scripts: `bun test scripts/tests`, 3 pruebas aprobadas. Se comprueban modificaciones con timestamps iguales, rechazo de cambios en el payload, modo plan sin despacho de publicación y verificación de escritura de Aurora con HTTP sustituido por un mock local.
- PowerShell: scripts parseados sin errores; `git diff --check` aprobado.
- Inno Setup: scripts compilados e invariantes de la plantilla compartida aprobadas. La plantilla compartida se conserva sin cambios; las adaptaciones son locales.
- Imagen: `Assert-AppImageIdentity` aprobado para el código y todos los archivos empaquetados.
- Instalador: `release/LyraFlow-Windows-1.1.9.exe`, 59.918.440 bytes; ProductVersion 1.1.9.
- SHA-256: `4da179f71e042545d8efa24fe363ee141ea9bd5269e6892b0d4bed0ca61a6454`, coincide con SHA256SUMS.txt.
- Firma presente: CN=biglexj. Windows devuelve UnknownError al evaluar confianza; no se acredita confianza de una CA pública ni timestamp.
- `publish github --check` detecta y rechaza los cambios locales sin confirmar; no crea tags, commits, instalaciones ni escrituras remotas.

No realizado: instalación/migración sobre la 1.1.8 pública, reinicio de la app instalada, micrófono y atajos reales, instalación de Whisper desde la UI, lector de pantalla ni publicación. No se ha desinstalado la copia personal de Biglex para obtener esta evidencia.

La publicación de Aurora solo tiene pruebas locales con mock. Requiere LYRAFLOW_AURORA_APP_ID y AURORA_RELEASE_TOKEN del entorno autorizado; no se buscaron credenciales ni se escribió en Aurora/GitHub.

Durante la prueba de scripts se detectó y corrigió que Windows PowerShell 5.1 serializaba Get-Content con metadatos en vez de un string JSON limpio. Las notas se leen con File.ReadAllText y se verifica su conservación Unicode.

## Resultado local

LyraFlow 1.1.9 tiene un único flujo activo de instalación, basado en Inno Setup, con gestión explícita del MSI anterior y relanzamiento desde el destino instalado. La publicación exige -Publish; por defecto los comandos de release solo preparan la entrega local.

El historial de escritorio se guarda en disco y se filtra por el plazo seleccionado al cargar, guardar y durante la ejecución. El valor predeterminado de 24 horas permanece. El borrado/desactivado elimina el archivo de historial; no se crean copias fuera de su retención. Un fallo del disco no invalida una transcripción terminada.

Whisper intenta los respaldos tras errores de transporte. Se retiraron el actualizador duplicado, MSIX y la personalización WiX; el generador de iconos apunta al instalador vigente. El campo de ruta del instalador vuelve a ser editable y navegable; la acción de eliminar una transcripción tiene una etiqueta accesible.

Evidencia y limitaciones: [VALIDATION.md](VALIDATION.md). La base está preparada localmente; no se declara aceptada la actualización real ni se ha publicado.

Revisión posterior de Ajustes: distribución compacta en dos columnas desde 720 dp, pestañas alineadas a la izquierda y contenido agrupado por uso. General sigue el orden dictado → comportamiento → preferencias; Modelos separa conexión y transcripción. Compilación y 67 pruebas aprobadas. La revisión visual y la incorporación de este rediseño al instalador permanecen pendientes.
