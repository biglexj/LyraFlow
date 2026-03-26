# Script para crear un certificado nuevo y firmar el paquete MSIX de LyraFlow
# Autor: BiglexJ (Adaptado para LyraFlow)
# NOTA: Este script requiere permisos de administrador

param(
    [string]$Publisher = "CN=biglexj",
    [string]$Password = "",
    [string]$CertName = "LyraFlow_Dev_Certificate"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  LyraFlow - Crear Certificado y Firmar MSIX" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# 1. Verificar administrador
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host "❌ Este script requiere permisos de administrador." -ForegroundColor Red
    exit 1
}

# 2. Buscar signtool
$sdkBinRoot = "C:\Program Files (x86)\Windows Kits\10\bin"
$latestSdk = Get-ChildItem $sdkBinRoot -Directory | Where-Object { $_.Name -match "^10\." } | Sort-Object Name -Descending | Select-Object -First 1
$signToolPath = Join-Path $latestSdk.FullName "x64\signtool.exe"

if (-not (Test-Path $signToolPath)) {
    Write-Host "❌ No se encontró signtool.exe" -ForegroundColor Red
    exit 1
}

# Rutas
$certPath = Join-Path $ProjectRoot "$CertName.pfx"
$msixPath = Join-Path $ProjectRoot "publish\msix\LyraFlow.msix"

if (-not (Test-Path $msixPath)) {
    Write-Host "❌ No se encontró el paquete MSIX. Ejecuta primero build-msix.ps1" -ForegroundColor Red
    exit 1
}

# Paso 1: Crear Certificado
Write-Host "[1/2] Creando certificado autofirmado..." -ForegroundColor Yellow
$cert = New-SelfSignedCertificate `
    -Type Custom `
    -Subject $Publisher `
    -KeyUsage DigitalSignature `
    -FriendlyName "LyraFlow Dev Cert" `
    -CertStoreLocation "Cert:\CurrentUser\My" `
    -TextExtension @("2.5.29.37={text}1.3.6.1.5.5.7.3.3", "2.5.29.19={text}")

$securePassword = New-Object System.Security.SecureString
Export-PfxCertificate -Cert $cert -FilePath $certPath -Password $securePassword | Out-Null
Write-Host "✅ Certificado exportado a: $certPath" -ForegroundColor Green

# Paso 2: Firmar
Write-Host "[2/2] Firmando paquete MSIX..." -ForegroundColor Yellow
& $signToolPath sign /fd SHA256 /a /f $certPath /p "" $msixPath
Write-Host "✅ Paquete firmado exitosamente" -ForegroundColor Green

# Paso 3: Instalar en Trusted Root
Write-Host "[3/3] Instalando en Almacén de Confianza..." -ForegroundColor Yellow
Import-PfxCertificate -FilePath $certPath -CertStoreLocation Cert:\LocalMachine\Root -Password $securePassword | Out-Null
Write-Host "✅ Certificado instalado. ¡Ya puedes instalar el MSIX!" -ForegroundColor Green
