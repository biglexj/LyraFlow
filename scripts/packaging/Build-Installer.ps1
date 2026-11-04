<#
.SYNOPSIS
    Genera el instalador EXE de LyraFlow para Windows con Inno Setup.

.DESCRIPTION
    Sustituye al empaquetado WiX/MSI de jpackage. El flujo es:

      1. Resuelve la version del proyecto (gradle.properties -> lyraflow.versionName).
      2. Reescribe packaging\windows\version.iss con esa version.
      3. Genera la distribucion de la app (imagen de aplicacion de jpackage)
         y la copia a build\inno-staging\app, que es la que empaqueta el .iss.
      4. Compila packaging\windows\setup.iss con ISCC.exe y deja el
         instalador en la carpeta de release.

    La identidad de actualizacion (AppId) vive en setup.iss y es estable
    entre versiones: una version nueva actualiza la anterior en el mismo
    directorio en vez de instalar en paralelo.

.PARAMETER Version
    Version concreta a empaquetar. Por defecto, la de gradle.properties.

.PARAMETER SkipBuild
    No ejecuta Gradle: reutiliza la distribucion ya generada.

.PARAMETER OutputDirectory
    Carpeta donde se deja el instalador. Por defecto, la carpeta release del
    proyecto.

.PARAMETER IsccPath
    Ruta a ISCC.exe. Por defecto se busca subiendo desde este script hasta
    <workspace>\Scripts\tools\InnoSetup\ISCC.exe y, si no, en el PATH.

.EXAMPLE
    .\scripts\packaging\Build-Installer.ps1
#>
param(
    [string]$Version,
    [switch]$SkipBuild,
    [string]$OutputDirectory,
    [string]$IsccPath = ''
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
. (Join-Path $root "scripts\release\ReleaseTools.ps1")
. (Join-Path $root "scripts\packaging\AppImageIdentity.ps1")

$packagingDir = Join-Path $root "packaging\windows"
$setupScript = Join-Path $packagingDir "setup.iss"
$versionScript = Join-Path $packagingDir "version.iss"
$stagingDir = Join-Path $root "build\inno-staging\app"
$appImageDir = Join-Path $root "composeApp\build\compose\binaries\main\app\LyraFlow"
if (-not $OutputDirectory) { $OutputDirectory = Join-Path $root "release" }

# --- Localizador de ISCC -----------------------------------------------------
# Localiza ISCC.exe sin rutas absolutas: primero la copia del workspace,
# subiendo desde este script; despues el PATH del sistema.
function Resolve-Iscc {
    $dir = $PSScriptRoot
    for ($i = 0; $i -lt 8; $i++) {
        $candidate = Join-Path $dir 'Scripts\tools\InnoSetup\ISCC.exe'
        if (Test-Path -LiteralPath $candidate) { return $candidate }
        $parent = Split-Path -Parent $dir
        if (-not $parent -or $parent -eq $dir) { break }
        $dir = $parent
    }
    $onPath = Get-Command ISCC.exe -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }
    return $null
}

function Write-Step {
    param([string]$Message)
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Write-Fail {
    param([string]$Message)
    Write-Host "ERROR: $Message" -ForegroundColor Red
    throw $Message
}

# --- Invariantes de empaquetado (validador compartido) ---------------------
# La plantilla visual y el arranque post-instalacion son fuente unica del
# workspace. Si un .iss editado a mano o copiado de otra app los rompe, el
# instalador sale mal, asi que el validador compartido aborta el build.
function Resolve-SharedInnoTool {
    param([string]$Relative)
    $dir = $PSScriptRoot
    for ($i = 0; $i -lt 8; $i++) {
        $candidate = Join-Path $dir $Relative
        if (Test-Path -LiteralPath $candidate) { return $candidate }
        $parent = Split-Path -Parent $dir
        if (-not $parent -or $parent -eq $dir) { break }
        $dir = $parent
    }
    return $null
}

function Assert-PackagingInvariants {
    param([string]$SetupIss, [string]$PackagingDir)

    $sharedDir = Resolve-SharedInnoTool 'Scripts\tools\inno'
    if (-not $sharedDir) {
        Write-Host "AVISO: no se localizo Scripts\tools\inno en este workspace: se omite la verificacion de empaquetado." -ForegroundColor Yellow
        return
    }

    # Plantilla visual con fuente unica: el repo guarda lo que se compilo.
    $sharedUi = Join-Path $sharedDir 'BiglexModernUI.iss'
    $localUi = Join-Path $PackagingDir 'BiglexModernUI.iss'
    if ((Test-Path -LiteralPath $sharedUi) -and (Test-Path -LiteralPath $localUi)) {
        if ((Get-FileHash -LiteralPath $sharedUi).Hash -ne (Get-FileHash -LiteralPath $localUi).Hash) {
            throw 'La plantilla visual difiere de la compartida. Sincronízala y revisa el diff antes de empaquetar.'
        }
    }

    $checker = Join-Path $sharedDir 'Check-Packaging.ps1'
    if (-not (Test-Path -LiteralPath $checker)) {
        Write-Host "AVISO: no se localizo Check-Packaging.ps1: se omite la verificacion de empaquetado." -ForegroundColor Yellow
        return
    }
    $shell = Get-Command pwsh.exe -ErrorAction SilentlyContinue
    if (-not $shell) { $shell = Get-Command powershell.exe -ErrorAction SilentlyContinue }
    if (-not $shell) {
        Write-Host "AVISO: no hay pwsh.exe ni powershell.exe: se omite la verificacion de empaquetado." -ForegroundColor Yellow
        return
    }

    Write-Host "Verificando las invariantes de empaquetado (Check-Packaging.ps1)..." -ForegroundColor Cyan
    & $shell.Source -NoProfile -ExecutionPolicy Bypass -File $checker -SetupIss $SetupIss -Quiet
    if ($LASTEXITCODE -ne 0) {
        Write-Fail ("Las invariantes de empaquetado no se cumplen (Check-Packaging.ps1 devolvio $LASTEXITCODE). " +
                    "Revisa el mensaje del validador de arriba y corrige el setup.iss antes de compilar.")
    }
    Write-Host "Invariantes de empaquetado correctas." -ForegroundColor Green
}

# --- 1. Version -------------------------------------------------------------
$propertiesPath = Join-Path $root "gradle.properties"
$properties = Get-Content -LiteralPath $propertiesPath -Raw -Encoding UTF8
$versionMatch = [regex]::Match($properties, '(?m)^lyraflow\.versionName=(\d+\.\d+\.\d+)')
if (-not $versionMatch.Success) {
    throw "No se pudo leer lyraflow.versionName de $propertiesPath."
}
if (-not $Version) { $Version = $versionMatch.Groups[1].Value }
Assert-SemanticVersion $Version
if ($Version -ne $versionMatch.Groups[1].Value) {
    throw 'La versión solicitada no coincide con la imagen del proyecto. Actualiza y compila las fuentes de versión primero.'
}

Write-Host "[1/4] Version del instalador: $Version" -ForegroundColor Yellow

# --- 2. version.iss ---------------------------------------------------------
$versionContent = @(
    "; ---------------------------------------------------------------------------",
    "; version.iss - generada por scripts\packaging\Build-Installer.ps1 con la",
    "; version real del proyecto (gradle.properties -> lyraflow.versionName).",
    "; No editar a mano: se reescribe en cada build.",
    "; ---------------------------------------------------------------------------",
    "#define MyAppVersion `"$Version`""
) -join "`r`n"
Set-Content -LiteralPath $versionScript -Value $versionContent -Encoding UTF8

# --- 3. Distribucion de la app ---------------------------------------------
if (-not $SkipBuild) {
    Write-Host "[2/4] Generando la distribucion de la app (Gradle)..." -ForegroundColor Yellow
    $jdk = Get-FullJdk
    $gradleArguments = @("-Dorg.gradle.java.home=$jdk", "--no-configuration-cache", ":composeApp:createDistributable")
    Invoke-Checked -Executable (Join-Path $root "gradlew.bat") -ArgumentList $gradleArguments
    Write-AppImageIdentity -Root $root
} else {
    Write-Host "[2/4] Build omitido: se reutiliza la distribucion existente." -ForegroundColor DarkYellow
}

Assert-AppImageIdentity -Root $root

Write-Host "[3/4] Preparando el contenido del instalador..." -ForegroundColor Yellow
if (Test-Path -LiteralPath $stagingDir) {
    $resolvedStage = [IO.Path]::GetFullPath($stagingDir)
    $expectedStage = [IO.Path]::GetFullPath((Join-Path $root 'build\inno-staging\app'))
    if ($resolvedStage -ne $expectedStage -or -not $resolvedStage.StartsWith($root + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Staging fuera del proyecto.' }
    Remove-Item -LiteralPath $resolvedStage -Recurse -Force
}
New-Item -ItemType Directory -Path $stagingDir -Force | Out-Null

# .jpackage.xml es metadato del empaquetador de jpackage: el MSI lo guardaba
# como .package y la app no lo necesita en tiempo de ejecucion.
$excluded = @(".jpackage.xml")
Get-ChildItem -LiteralPath $appImageDir -Recurse -File | ForEach-Object {
    $relative = $_.FullName.Substring($appImageDir.Length).TrimStart('\')
    if ($excluded -contains $_.Name) { return }
    $target = Join-Path $stagingDir $relative
    New-Item -ItemType Directory -Path (Split-Path $target -Parent) -Force | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $target -Force
}

$stagedFiles = (Get-ChildItem -LiteralPath $stagingDir -Recurse -File).Count
Write-Host "      $stagedFiles ficheros preparados en $stagingDir" -ForegroundColor Gray

# --- 4. Compilacion del instalador -----------------------------------------
if (-not $IsccPath) { $IsccPath = Resolve-Iscc }
if (-not $IsccPath) { Write-Fail 'No se localizo ISCC.exe en Scripts\tools\InnoSetup ni en el PATH.' }
$iscc = $IsccPath
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
Write-Host "[4/4] Compilando el instalador con Inno Setup..." -ForegroundColor Yellow
Assert-PackagingInvariants -SetupIss $setupScript -PackagingDir $packagingDir
Invoke-Checked -Executable $iscc -ArgumentList @($setupScript, "/O$OutputDirectory")

$installer = Join-Path $OutputDirectory "LyraFlow-Windows-$Version.exe"
if (-not (Test-Path -LiteralPath $installer)) {
    throw "ISCC termino sin error pero no se encontro el instalador esperado: $installer"
}

Write-Host "Instalador generado: $installer ($([math]::Round((Get-Item -LiteralPath $installer).Length / 1MB, 2)) MB)" -ForegroundColor Green
Write-Output $installer
