# Personalización del instalador EXE (branding, español e «Iniciar LyraFlow») — Plan

- Estado: `APPROVED`
- Fecha: `2026-10-21`
- Proyecto: `LyraFlow`

> **Actualización de alcance (2026-10-26).** La distribución oficial de la 1.1.9 se realiza con el **EXE autónomo de Inno Setup** (`packaging/windows/`), que sustituye al instalador MSI de jpackage + WiX. El trabajo de este proceso (plantilla WiX propia, localización `es-es`, branding y «Iniciar LyraFlow al terminar») **se conserva como camino MSI no publicado** y sus comprobaciones V01 a V07 siguen siendo válidas para ese camino. Las comprobaciones manuales (V08 a V10) pasan a apuntar al artefacto que se publica.

## Objetivo

Que el instalador EXE de Windows se muestre en español, con la identidad visual de LyraFlow y con la opción de iniciar la aplicación al terminar la instalación.

## Alcance

- Incluye: plantilla WiX propia (`package/windows/main.wxs`), localización es-es, bitmaps e icono de marca, diálogo final con «Iniciar LyraFlow al terminar», cierre de la aplicación en ejecución antes de reemplazar la versión anterior, traducción del aviso «la carpeta ya existe», tarea de Gradle propia y su integración en el script de release.
- No incluye: publicación de la release en GitHub, cambios en la lógica de la aplicación, firma y etiquetado (siguen siendo paso aparte del script de release).

## Enfoque

1. Sustituir la plantilla WiX por defecto de jpackage, que era la que llegaba realmente al instalador (el plugin de Compose limpia su carpeta de recursos antes de invocar jpackage).
2. Reejecutar jpackage con los mismos argumentos del plugin pero con `--resource-dir` propio, reutilizando la imagen de aplicación que el plugin deja en `compose/binaries/main/app` como `--input`.
3. Declarar la identidad visual mediante `WixVariable` (`WixUIBannerBmp`, `WixUIDialogBmp`) y `ARPPRODUCTICON`, que son los mecanismos reales que usa la WixUIExtension.
4. Ofrecer «Iniciar LyraFlow» mediante la casilla opcional del `ExitDialog` de WixUI (`WIXUI_EXITDIALOGOPTIONALTEXT` / `WIXUI_EXITDIALOGOPTIONALCHECKBOXTEXT`) y un evento `DoAction` sobre el botón «Finalizar».
5. Cerrar la aplicación en ejecución antes de `RemoveExistingProducts` para evitar el fallo «ended prematurely» al actualizar.

## Criterios de finalización

- [x] El MSI generado declara `ProductLanguage = 1034` y los textos propios en español.
- [x] El MSI incrusta los bitmaps de marca (`WixUI_Bmp_Banner`, `WixUI_Bmp_Dialog`) y el icono `LyraFlowIcon`.
- [x] El MSI contiene el evento `DoAction` que lanza `LyraFlow.exe` cuando la casilla está marcada.
- [x] La tarea `:composeApp:packageLyraFlowInstaller` produce el EXE final y el script de release la utiliza.
- [ ] Comprobación manual en la interfaz de Windows del flujo completo de instalación (pendiente de ejecutar el instalador en la máquina).

## Autorización

- [x] Plan aprobado para ejecución.
