# Script para generar el paquete MSIX de LyraFlow usando MakeAppx
# Basado en el flujo de WinTTS
param(
    [string]$Configuration = "Release",
    [string]$SdkVersion = "10.0.22621.0" # Ajustar según el SDK instalado
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$ProjectName = "LyraFlow"
$OutputDir = Join-Path $ProjectRoot "publish\msix"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  LyraFlow - Generador de Paquetes MSIX" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# Buscar MakeAppx
$sdkBinRoot = "C:\Program Files (x86)\Windows Kits\10\bin"
if (Test-Path $sdkBinRoot) {
    $latestSdk = Get-ChildItem $sdkBinRoot -Directory | Where-Object { $_.Name -match "^10\." } | Sort-Object Name -Descending | Select-Object -First 1
    if ($latestSdk) {
        $sdkBinPath = Join-Path $latestSdk.FullName "x64"
        $makeAppxPath = Join-Path $sdkBinPath "makeappx.exe"
        $signToolPath = Join-Path $sdkBinPath "signtool.exe"
    }
}

if (-not $makeAppxPath -or -not (Test-Path $makeAppxPath)) {
    # Intento manual si falla la detección automática
    $fallbackPaths = @(
        "C:\Program Files (x86)\Windows Kits\10\bin\10.0.22621.0\x64\makeappx.exe",
        "C:\Program Files (x86)\Windows Kits\10\bin\10.0.19041.0\x64\makeappx.exe",
        "C:\Program Files (x86)\Windows Kits\10\App Certification Kit\makeappx.exe"
    )
    foreach ($fb in $fallbackPaths) {
        if (Test-Path $fb) {
            $makeAppxPath = $fb
            break
        }
    }
}

if (-not $makeAppxPath -or -not (Test-Path $makeAppxPath)) {
    Write-Host "❌ No se encontró makeappx.exe. Asegúrate de tener el Windows SDK instalado." -ForegroundColor Red
    exit 1
}

# Preparar carpetas
if (Test-Path $OutputDir) { Remove-Item $OutputDir -Recurse -Force }
$packageDir = Join-Path $OutputDir "package"
New-Item -ItemType Directory -Path $packageDir -Force | Out-Null

# 1. Publicar archivos
Write-Host "[1/3] Publicando archivos..." -ForegroundColor Yellow
dotnet publish "$ProjectRoot\$ProjectName.csproj" -c $Configuration -r win-x64 --output $packageDir --self-contained true

# 2. Preparar Manifiesto
Write-Host "[2/3] Preparando AppxManifest.xml..." -ForegroundColor Yellow
$manifestSource = Join-Path $ProjectRoot "Package.appxmanifest"
$manifestDest = Join-Path $packageDir "AppxManifest.xml"
$manifestContent = Get-Content $manifestSource -Raw
$manifestContent = $manifestContent -replace '\$targetnametoken\$', $ProjectName
$manifestContent | Set-Content $manifestDest -Encoding UTF8

# 3. Copiar Imágenes
Write-Host "[3/3] Copiando recursos visuales..." -ForegroundColor Yellow
Copy-Item -Path "$ProjectRoot\Image" -Destination $packageDir -Recurse -Force

# 4. Empaquetar
Write-Host "[4/4] Creando paquete .msix..." -ForegroundColor Yellow
$msixPath = Join-Path $OutputDir "$ProjectName.msix"
& $makeAppxPath pack /d $packageDir /p $msixPath /o

Write-Host ""
Write-Host "✅ Paquete MSIX generado en: $msixPath" -ForegroundColor Green
Write-Host "⚠️  Recuerda que para instalarlo debe estar firmado o usar modo dev." -ForegroundColor Yellow
