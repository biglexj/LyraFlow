; ============================================================================
; setup.iss - Instalador de Windows de LyraFlow (Inno Setup 6.7+)
; ----------------------------------------------------------------------------
; Basado en la plantilla compartida Biglex Modern 2.0. Copia autocontenida:
;   - BiglexModernUI.iss : presentacion y validacion de la entrada
;   - identity.iss        : nombre, AppId, ejecutable, carpeta, icono y tema
;   - version.iss         : version real, la regenera el script de build
;
; Sustituye al instalador WiX/MSI de jpackage: un EXE autonomo, instalacion
; por usuario sin UAC y una sola pantalla de entrada (logo, nombre, bajada,
; ubicacion editable con selector de carpeta y un unico boton principal).
; Despues: progreso y pantalla de exito, con los controles nativos.
;
; La distribucion de la app (imagen de aplicacion de jpackage: lanzador +
; app\ + runtime\) la prepara scripts\packaging\Build-Installer.ps1 en
; build\inno-staging\app antes de compilar este script.
;
; Compilar:
;   & "D:\Proyectos\biglexj\Scripts\tools\InnoSetup\ISCC.exe" setup.iss /O<release>
; ============================================================================

#include "version.iss"
#include "identity.iss"

[Setup]
; --- IDENTIDAD DE ACTUALIZACION: unica y estable por app -------------------
; MyAppId vive en identity.iss y no cambia entre versiones: una version nueva
; actualiza la anterior en la misma carpeta en lugar de instalar en paralelo.
; No es el UpgradeCode del MSI de WiX (ver MIGRATION.md).
AppId={#MyAppId}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher=Biglex
AppPublisherURL=https://biglexj.com
AppSupportURL=https://github.com/biglexj/LyraFlow
DefaultDirName={code:GetInstallDirectory}
DefaultGroupName={#MyAppName}
; --- Pantallas: solo la nuestra + progreso + exito --------------------------
DisableWelcomePage=yes
DisableDirPage=yes
DisableProgramGroupPage=yes
DisableReadyPage=yes
OutputDir=Output
; El nombre del artefacto lo fija el contrato de release del proyecto:
; LyraFlow-Windows-X.Y.Z.exe
OutputBaseFilename=LyraFlow-Windows-{#MyAppVersion}
SetupIconFile={#MyAppIcon}
UninstallDisplayIcon={app}\{#MyAppExe}
UninstallDisplayName={#MyAppName}
Compression=lzma2/max
SolidCompression=yes
ShowLanguageDialog=no
; --- Identidad visual moderna + tema claro/oscuro automatico (IS 6.6+) ---
WizardStyle=modern {#MyAppTheme}
#if MyAppTheme == "dark"
WizardBackColor=#15191C
WizardImageBackColor=#15191C
#else
WizardBackColor=#F7F9FA
WizardImageBackColor=clWhite
#endif
WizardBackColorDynamicDark=#15191C
WizardImageBackColorDynamicDark=#15191C
; --- Instalacion por usuario (sin privilegios de administrador) -------------
; PrivilegesRequiredOverridesAllowed se deja vacio a proposito: abrir el
; dialogo "Seleccion del Modo de Instalacion" anade una pantalla extra.
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
; --- Comportamiento --------------------------------------------------------
CloseApplications=force
RestartApplications=no
SetupLogging=yes
VersionInfoDescription={#MyAppName} instalador
VersionInfoProductName={#MyAppName}
VersionInfoCompany=Biglex
; Sin esta linea el bloque de version del EXE queda vacio: la version real
; que se ve en el Explorador de Windows y en "Agregar o quitar programas".
VersionInfoVersion={#MyAppVersion}

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Messages]
spanish.FinishedHeadingLabel=Instalación completada
spanish.FinishedLabel={#MyAppName} se instaló correctamente.
spanish.FinishedLabelNoIcons={#MyAppName} se instaló correctamente.
spanish.ClickFinish=Pulsa Finalizar para cerrar el instalador.

[CustomMessages]
spanish.RunApp=Ejecutar {#MyAppName}

[Files]
; Extraído explícitamente por la UI; al principio para evitar descomprimir payload solid.
Source: "{#MyAppIcon}"; Flags: dontcopy
; La distribucion real de la app (imagen de aplicacion de jpackage).
Source: "..\..\build\inno-staging\app\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExe}"; WorkingDir: "{app}"
Name: "{userdesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExe}"; WorkingDir: "{app}"

[Run]
; Lanza la app via el shell del escritorio: heredar el contexto del instalador
; hace que el proceso falle con 0xc00004bc. explorer.exe vive en {win}, no en {sys}.
Filename: "{win}\explorer.exe"; Parameters: """{app}\{#MyAppExe}"""; Description: "{cm:RunApp}"; WorkingDir: "{app}"; Flags: nowait postinstall skipifsilent; Check: not RestartAppRequested
Filename: "{win}\explorer.exe"; Parameters: """{app}\{#MyAppExe}"""; WorkingDir: "{app}"; Flags: nowait; Check: RestartAppRequested

; Presentacion de la pantalla de entrada (Biglex Modern 2.0).
#include "BiglexModernUI.iss"
#include "LegacyMigration.iss"
#include "Accessibility.iss"

[Code]
{ Cierra la app si esta corriendo: cierre suave, espera y cierre forzado. }
procedure StopRunningApp();
var
  ResultCode: Integer;
begin
  Exec(ExpandConstant('{sys}\taskkill.exe'), '/IM {#MyAppExe} /T', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Sleep(500);
  Exec(ExpandConstant('{sys}\taskkill.exe'), '/F /IM {#MyAppExe} /T', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Log('Cierre de instancias de {#MyAppExe} en ejecucion completado.');
end;

function InitializeSetup(): Boolean;
begin
  Result := DiscoverLegacyProducts;
  if not Result then
    SuppressibleMsgBox('No se pudo comprobar la instalación anterior de LyraFlow. Revisa el registro del instalador.', mbError, MB_OK, IDOK);
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
begin
  Result := '';
  NeedsRestart := False;
  StopRunningApp;
  Result := RemoveLegacyProducts(NeedsRestart);
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usUninstall then
  begin
    StopRunningApp;
  end;
end;
procedure InitializeWizard();
begin
  ModernInitialize;
  InitializeAccessibleControls;
end;

procedure CurPageChanged(CurPageID: Integer);
begin
  ModernPageChanged(CurPageID);
end;

function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := ModernNextButtonClick(CurPageID);
end;

[Code]
procedure CancelButtonClick(CurPageID: Integer; var Cancel, Confirm: Boolean);
begin
  ModernCancelButtonClick(CurPageID, Cancel, Confirm);
end;
