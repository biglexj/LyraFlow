# Script para generar ejecutable portable de LyraFlow
# Basado en el flujo de WinTTS
param(
    [string]$Configuration = "Release",
    [string]$Runtime = "win-x64",
    [switch]$SelfContained = $true,
    [switch]$SingleFile = $true
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$ProjectName = "LyraFlow"
$OutputDir = Join-Path $ProjectRoot "publish\exe"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  LyraFlow - Generador de Ejecutable Portable" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# Limpiar y Crear directorio de salida
if (Test-Path $OutputDir) { Remove-Item $OutputDir -Recurse -Force }
New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null

Write-Host "[1/2] Compilando y generando ejecutable..." -ForegroundColor Yellow
$projectFile = Join-Path $ProjectRoot "$ProjectName.csproj"

$publishArgs = @(
    "publish", $projectFile,
    "-c", $Configuration,
    "-r", $Runtime,
    "--output", $OutputDir,
    "--self-contained", "true",
    "-p:PublishSingleFile=true",
    "-p:IncludeNativeLibrariesForSelfExtract=true",
    "-p:PublishReadyToRun=true"
)

& dotnet @publishArgs

Write-Host ""
Write-Host "✅ Ejecutable generado en: $OutputDir" -ForegroundColor Green
