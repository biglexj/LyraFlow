# Personalización del instalador EXE (branding, español e «Iniciar LyraFlow») — Tareas

- Estado: `IN_PROGRESS`
- Fecha: `2026-10-21`
- Proyecto: `LyraFlow`

## Tareas

- [x] T01 — Diagnosticar por qué el branding (`banner.bmp`, `dialog.bmp`, `icon.ico`) nunca llegaba al instalador.
  - Hallazgo: el plugin de Compose limpia `compose/build/compose/tmp/resources` dentro de `prepareWorkingDir` y luego invoca jpackage con `--resource-dir` a esa carpeta, por lo que la plantilla de `package/windows` nunca se usaba.
- [x] T02 — Reemplazar la plantilla WiX por una compatible con jpackage (`main.wxs`, `overrides.wxi`, `resource.wxl`).
- [x] T03 — Crear la tarea `:composeApp:packageLyraFlowInstaller`, que reejecuta jpackage con `--resource-dir` propio.
  - El plugin borra su carpeta de entrada, así que la tarea reutiliza la imagen de aplicación (`compose/binaries/main/app/LyraFlow`) como `--input` y `--runtime-image`.
- [x] T04 — Aplicar la identidad visual: `WixVariable` para los bitmaps, `ARPPRODUCTICON` para el icono y textos propios en es-es.
- [x] T05 — Añadir la opción «Iniciar LyraFlow al terminar» con la casilla opcional del `ExitDialog` y un evento `DoAction`.
- [x] T06 — Añadir el cierre de la aplicación en ejecución (`taskkill`) antes de `RemoveExistingProducts`.
- [x] T07 — Traducir el aviso «la carpeta ya existe» (`InstallDirNotEmptyDlg.wxs`).
- [x] T08 — Integrar la tarea nueva en `scripts/release/build-release.ps1`.
- [x] T09 — Verificar el MSI generado (idioma, icono, bitmaps, acciones y eventos).
- [ ] T10 — Comprobación manual del flujo completo en la interfaz de Windows.

## Notas técnicas

- La sintaxis de WiX 3 no admite `Type=` ni `Attributes=` en `CustomAction` y `Control`: las acciones ejecutables se declaran con `ExeCommand` + `Directory` y las condiciones van en el elemento de la secuencia.
- Los diálogos propios se programan con `<Show Dialog="..."/>` y los eventos sobre controles existentes con `<Publish Dialog="..." Control="..." Event="DoAction" .../>` dentro de un `<UI>`.
- `Installed` queda vacío tras la instalación, así que la condición `NOT Installed` de los controles opcionales del `ExitDialog` se cumple y la casilla es visible.
- El parser del fichero de argumentos de jpackage trata `\` como escape: las rutas se escriben con las barras duplicadas.

## Bloqueos

- La comprobación manual (T10) requiere el escritorio interactivo y no interferir con otras instalaciones de Windows Installer en curso.
