; Configuración por aplicación de LyraFlow (Biglex Modern 2.0).
; Mantener MyAppId estable entre versiones: una versión nueva debe actualizar
; la anterior en el mismo sitio y no crear una instalación paralela.
;
; MyAppId NO es el UpgradeCode del MSI de WiX: Windows Installer no reconoce
; una actualización por compartir GUID. La coexistencia MSI/EXE está
; documentada en MIGRATION.md.
#ifndef MyAppName
  #define MyAppName "LyraFlow"
#endif
#ifndef MyAppId
  #define MyAppId "Biglex.LyraFlow.Desktop"
#endif
#ifndef MyAppDirectory
  #define MyAppDirectory "LyraFlow"
#endif
#ifndef MyAppExe
  #define MyAppExe "LyraFlow.exe"
#endif
#ifndef MyAppIcon
  #define MyAppIcon "LyraFlow.ico"
#endif
#ifndef MyAppTheme
  #define MyAppTheme "dynamic"
#endif

#ifndef MyAppSubtitle
  #define MyAppSubtitle "Tu voz, convertida en texto."
#endif
