# Migración de MSI a Inno Setup

La versión 1.1.9 incorpora una migración del MSI anterior, identificado por el UpgradeCode `{55EE1E98-1BD5-4D89-990F-27756B85820C}`.
El instalador consulta Windows Installer, conserva la carpeta existente y retira los productos relacionados antes de copiar la aplicación nueva. No deduce productos por nombre ni elimina carpetas arbitrarias.
Si Windows Installer no puede retirar una instalación anterior, el proceso se detiene y muestra el código del fallo y la alternativa de desinstalar desde Configuración > Aplicaciones. No continúa creando otra copia de la aplicación.

Las instalaciones nuevas usan `%LOCALAPPDATA%\Programs\LyraFlow`. Una instalación migrada conserva su ubicación anterior; Inno Setup recuerda esa ubicación para las siguientes actualizaciones mediante el AppId estable `Biglex.LyraFlow.Desktop`.
El MSI histórico y el AppId nuevo son identidades distintas: conservar el directorio no sustituye la retirada del MSI.

La actualización iniciada desde la app nueva usa los parámetros de Inno Setup y deja al instalador el relanzamiento del ejecutable correcto. El actualizador público de 1.1.8 todavía puede abrir el asistente, porque sus parámetros antiguos no se pueden cambiar retroactivamente.

## Comprobación funcional pendiente

En un usuario de Windows de pruebas o VM, instalar el EXE público de 1.1.8, conservar un ajuste conocido y ejecutar el nuevo instalador. Comprobar una sola entrada en Aplicaciones, la carpeta elegida, el arranque de 1.1.9, las preferencias y el dictado. Repetir desde una instalación de Inno Setup y con un fallo de Windows Installer. Guardar los logs de instalación.
La compilación del script no acredita esta prueba.

El historial nuevo vive en el directorio de datos del usuario, separado del directorio de programas; su borrado y retención se gestionan desde Ajustes. Las preferencias y modelos de Whisper no se eliminan de forma recursiva durante la migración.
