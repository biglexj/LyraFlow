[Code]
{ Solo productos MSI de LyraFlow identificados por el UpgradeCode histórico. }
var
  LegacyProducts: TArrayOfString;
  LegacyDirectory: String;

function LyraMsiEnumRelatedProducts(UpgradeCode: String; Reserved, Index: Cardinal;
  ProductCode: String): Cardinal;
  external 'MsiEnumRelatedProductsW@msi.dll stdcall';
function LyraMsiGetProductInfo(ProductCode, PropertyName, Value: String;
  var ValueLength: Cardinal): Cardinal;
  external 'MsiGetProductInfoW@msi.dll stdcall';

function DiscoverLegacyProducts(): Boolean;
var
  Index, Status, BufferLength: Cardinal;
  ProductCode, Location: String;
begin
  Result := False;
  Index := 0;
  SetArrayLength(LegacyProducts, 0);
  LegacyDirectory := '';
  while True do
  begin
    ProductCode := StringOfChar(#0, 39);
    Status := LyraMsiEnumRelatedProducts('{55EE1E98-1BD5-4D89-990F-27756B85820C}', 0, Index, ProductCode);
    if Status = 259 then Break; { ERROR_NO_MORE_ITEMS }
    if Status <> 0 then
    begin
      Log(Format('No se pudo consultar el MSI anterior (código %d).', [Status]));
      Exit;
    end;
    SetLength(ProductCode, 38);
    SetArrayLength(LegacyProducts, Index + 1);
    LegacyProducts[Index] := ProductCode;
    BufferLength := 32768;
    Location := StringOfChar(#0, BufferLength);
    if LyraMsiGetProductInfo(ProductCode, 'InstallLocation', Location, BufferLength) = 0 then
    begin
      SetLength(Location, BufferLength);
      if FileExists(AddBackslash(Location) + '{#MyAppExe}') then
        LegacyDirectory := Location;
    end;
    Index := Index + 1;
  end;
  if (GetArrayLength(LegacyProducts) > 0) and (LegacyDirectory = '') and
    FileExists(ExpandConstant('{localappdata}\LyraFlow\{#MyAppExe}')) then
    LegacyDirectory := ExpandConstant('{localappdata}\LyraFlow');
  Result := True;
end;

function GetInstallDirectory(Param: String): String;
begin
  { Preservar el destino del actualizador de la 1.1.8; Inno recordará esta ruta. }
  if LegacyDirectory <> '' then Result := LegacyDirectory
  else Result := ExpandConstant('{localappdata}\Programs\{#MyAppDirectory}');
end;

function RemoveLegacyProducts(var NeedsRestart: Boolean): String;
var
  Index, ExitCode: Integer;
begin
  Result := '';
  for Index := 0 to GetArrayLength(LegacyProducts) - 1 do
  begin
    Log('Migrando MSI anterior: ' + LegacyProducts[Index]);
    if not Exec(ExpandConstant('{sys}\msiexec.exe'), '/x ' + LegacyProducts[Index] +
      ' /qn /norestart /L*v "' + ExpandConstant('{tmp}\LyraFlow-msi-migration.log') + '"',
      '', SW_HIDE, ewWaitUntilTerminated, ExitCode) then ExitCode := -1;
    if ExitCode = 3010 then NeedsRestart := True
    else if (ExitCode <> 0) and (ExitCode <> 1605) then
    begin
      Result := Format('No se pudo retirar la instalación anterior de LyraFlow (código %d). ' +
        'Desinstálala desde Configuración > Aplicaciones y vuelve a ejecutar este instalador.', [ExitCode]);
      Exit;
    end;
  end;
  SetArrayLength(LegacyProducts, 0);
end;

function RestartAppRequested(): Boolean;
var Index: Integer;
begin
  Result := False;
  for Index := 1 to ParamCount do
    if CompareText(ParamStr(Index), '/RESTARTAPP') = 0 then Result := True;
end;
