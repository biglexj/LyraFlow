<#
.SYNOPSIS
    Genera el instalador EXE de LyraFlow para Windows con Inno Setup.

.DESCRIPTION
    Flujo de empaquetado para LyraFlow (Rust + GPUI):
      1. Resuelve la version del proyecto desde Cargo.toml.
      2. Reescribe packaging\windows\version.iss con esa version.
      3. Compila target\release\LyraFlow.exe con Cargo (--locked --release) y
         lo prepara en build\inno-staging\app (salvo si se indica -SkipBuild).
      4. Verifica invariantes compartidas y compila packaging\windows\setup.iss
         con ISCC.exe dejando el instalador en release\.

.PARAMETER Version
    Version concreta a empaquetar. Por defecto, la de Cargo.toml.

.PARAMETER SkipBuild
    Reutiliza build\inno-staging\app sin ejecutar Cargo.

.PARAMETER OutputDirectory
    Carpeta donde se deja el instalador. Por defecto, release\.

.PARAMETER IsccPath
    Ruta a ISCC.exe. Por defecto se busca en <workspace>\Scripts\tools\InnoSetup\ISCC.exe o PATH.
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
$releaseExe = Join-Path $root "target\release\LyraFlow.exe"
if (-not $OutputDirectory) { $OutputDirectory = Join-Path $root "release" }

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

function Write-Fail {
    param([string]$Message)
    Write-Host "ERROR: $Message" -ForegroundColor Red
    throw $Message
}

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
$manifestInfo = Get-CargoManifestInfo -Root $root
$currentVersion = $manifestInfo.Version
if (-not $Version) { $Version = $currentVersion }
Assert-SemanticVersion $Version
if ($Version -ne $currentVersion) {
    throw 'La versión solicitada no coincide con Cargo.toml. Actualiza las fuentes de versión primero.'
}

Write-Host "[1/4] Version del instalador: $Version" -ForegroundColor Yellow

# --- 2. version.iss ---------------------------------------------------------
$versionContent = @(
    "; ---------------------------------------------------------------------------",
    "; version.iss - generada por scripts\packaging\Build-Installer.ps1 con la",
    "; version real del proyecto (Cargo.toml -> version).",
    "; No editar a mano: se reescribe en cada build.",
    "; ---------------------------------------------------------------------------",
    "#define MyAppVersion `"$Version`""
) -join "`r`n"
Set-Content -LiteralPath $versionScript -Value $versionContent -Encoding UTF8

# --- 3. Compilacion y staging del binario nativo ----------------------------
if (-not $SkipBuild) {
    Write-Host "[2/4] Compilando binario nativo Rust + GPUI (--locked --release)..." -ForegroundColor Yellow
    Invoke-Checked -Executable "cargo" -ArgumentList @("build", "--locked", "--release")
    if (-not (Test-Path -LiteralPath $releaseExe)) {
        throw "No se encontró el binario compilado en $releaseExe."
    }

    Write-Host "[3/4] Preparando el contenido del instalador..." -ForegroundColor Yellow
    if (Test-Path -LiteralPath $stagingDir) {
        $resolvedStage = [IO.Path]::GetFullPath($stagingDir)
        $expectedStage = [IO.Path]::GetFullPath((Join-Path $root 'build\inno-staging\app'))
        if ($resolvedStage -ne $expectedStage -or -not $resolvedStage.StartsWith($root + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Staging fuera del proyecto.' }
        Remove-Item -LiteralPath $resolvedStage -Recurse -Force
    }
    New-Item -ItemType Directory -Path $stagingDir -Force | Out-Null
    Copy-Item -LiteralPath $releaseExe -Destination (Join-Path $stagingDir "LyraFlow.exe") -Force
    Write-AppImageIdentity -Root $root
} else {
    Write-Host "[2/4] Build omitido: se reutiliza el staging existente." -ForegroundColor DarkYellow
    Write-Host "[3/4] Verificando staging existente..." -ForegroundColor Yellow
}

Assert-AppImageIdentity -Root $root

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
