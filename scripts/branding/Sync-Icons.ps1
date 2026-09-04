<#
.SYNOPSIS
    Sincronización y generación reproducible de iconos derivados de LyraFlow conforme a Core-Docs.
.DESCRIPTION
    Lee la fuente canónica 'assets/branding/icons/icon-transparent.png' y genera los recursos
    derivados para Compose Desktop (app_icon.ico, icon.png) y las superficies del sistema (Image/),
    garantizando 100% canal alfa sin fondos cuadrados opacos ni artefactos.
#>

[CmdletBinding()]
param(
    [switch]$Force
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

$masterIcon = Join-Path $root "assets\branding\icons\icon-transparent.png"
if (-not (Test-Path -LiteralPath $masterIcon)) {
    throw "No se encontró el icono maestro transparente en: $masterIcon"
}

Add-Type -AssemblyName System.Drawing

function Resize-ImagePng {
    param(
        [System.Drawing.Bitmap]$source,
        [int]$width,
        [int]$height,
        [string]$outputPath
    )
    $dest = New-Object System.Drawing.Bitmap($width, $height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($dest)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    # Calcular proporción para centrar en caso de aspect ratios no cuadrados
    $srcRatio = $source.Width / $source.Height
    $destRatio = $width / $height
    if ($srcRatio -gt $destRatio) {
        $drawW = $width
        $drawH = [int]($width / $srcRatio)
        $drawX = 0
        $drawY = [int](($height - $drawH) / 2)
    } else {
        $drawH = $height
        $drawW = [int]($height * $srcRatio)
        $drawX = [int](($width - $drawW) / 2)
        $drawY = 0
    }

    $g.DrawImage($source, $drawX, $drawY, $drawW, $drawH)
    $g.Dispose()

    $dest.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $dest.Dispose()
}

Write-Host "═════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  LyraFlow — Sincronización de Iconos de Branding" -ForegroundColor Cyan
Write-Host "═════════════════════════════════════════════════" -ForegroundColor Cyan

# 1. assets/branding/icons/icon.png
$brandingIcon = Join-Path $root "assets\branding\icons\icon.png"
Copy-Item -LiteralPath $masterIcon -Destination $brandingIcon -Force
Write-Host "✔ assets\branding\icons\icon.png actualizado (transparente)." -ForegroundColor Green

# 2. composeApp/src/desktopMain/resources/icon.png
$composePng = Join-Path $root "composeApp\src\desktopMain\resources\icon.png"
Copy-Item -LiteralPath $masterIcon -Destination $composePng -Force
Write-Host "✔ composeApp\src\desktopMain\resources\icon.png actualizado." -ForegroundColor Green

# 3. composeApp/src/desktopMain/resources/app_icon.ico
$composeIco = Join-Path $root "composeApp\src\desktopMain\resources\app_icon.ico"
$magickCmd = Get-Command magick -ErrorAction SilentlyContinue
if ($magickCmd) {
    & magick "$masterIcon" -define icon:auto-resize=256,128,64,48,32,24,16 "$composeIco"
    Write-Host "✔ app_icon.ico generado con ImageMagick (256,128,64,48,32,24,16 - canal alfa puro)." -ForegroundColor Green
} else {
    Write-Warning "ImageMagick no encontrado en PATH; usando generación nativa .NET..."
    # Fallback si no hay ImageMagick
    $bmp = [System.Drawing.Bitmap]::FromFile($masterIcon)
    $sizes = @(16, 24, 32, 48, 64, 128, 256)
    $pngStreams = @()
    foreach ($s in $sizes) {
        $dest = New-Object System.Drawing.Bitmap($s, $s, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $g = [System.Drawing.Graphics]::FromImage($dest)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $g.Clear([System.Drawing.Color]::Transparent)
        $g.DrawImage($bmp, 0, 0, $s, $s)
        $g.Dispose()
        $ms = New-Object System.IO.MemoryStream
        $dest.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
        $dest.Dispose()
        $pngStreams += ,@($s, $ms.ToArray())
        $ms.Dispose()
    }
    $bmp.Dispose()
    $headerSize = 6 + ($pngStreams.Count * 16)
    $offset = $headerSize
    $fs = [System.IO.File]::Open($composeIco, [System.IO.FileMode]::Create)
    $bw = New-Object System.IO.BinaryWriter($fs)
    $bw.Write([UInt16]0); $bw.Write([UInt16]1); $bw.Write([UInt16]$pngStreams.Count)
    foreach ($item in $pngStreams) {
        $s = $item[0]; $bytes = $item[1]
        $w = if ($s -ge 256) { [byte]0 } else { [byte]$s }
        $bw.Write($w); $bw.Write($w); $bw.Write([byte]0); $bw.Write([byte]0)
        $bw.Write([UInt16]1); $bw.Write([UInt16]32); $bw.Write([UInt32]$bytes.Length); $bw.Write([UInt32]$offset)
        $offset += $bytes.Length
    }
    foreach ($item in $pngStreams) { $bw.Write($item[1]) }
    $bw.Close(); $fs.Close()
    Write-Host "✔ app_icon.ico generado mediante .NET." -ForegroundColor Green
}

# 4. Iconos derivados de interfaz en composeApp/src/desktopMain/resources
$desktopResources = Join-Path $root "composeApp\src\desktopMain\resources"
if (-not (Test-Path -LiteralPath $desktopResources)) {
    New-Item -ItemType Directory -Path $desktopResources -Force | Out-Null
}

$sourceBmp = [System.Drawing.Bitmap]::FromFile($masterIcon)
Resize-ImagePng -source $sourceBmp -width 44 -height 44 -outputPath (Join-Path $desktopResources "Square44x44Logo.png")
Write-Host "✔ composeApp\src\desktopMain\resources\Square44x44Logo.png generado (44x44 transparente)." -ForegroundColor Green

Resize-ImagePng -source $sourceBmp -width 150 -height 150 -outputPath (Join-Path $desktopResources "Square150x150Logo.png")
Write-Host "✔ composeApp\src\desktopMain\resources\Square150x150Logo.png generado (150x150 transparente)." -ForegroundColor Green

Resize-ImagePng -source $sourceBmp -width 50 -height 50 -outputPath (Join-Path $desktopResources "StoreLogo.png")
Write-Host "✔ composeApp\src\desktopMain\resources\StoreLogo.png generado (50x50 transparente)." -ForegroundColor Green

Resize-ImagePng -source $sourceBmp -width 310 -height 150 -outputPath (Join-Path $desktopResources "Wide310x150Logo.png")
Write-Host "✔ composeApp\src\desktopMain\resources\Wide310x150Logo.png generado (310x150 transparente centrado)." -ForegroundColor Green
$sourceBmp.Dispose()

Write-Host "Todos los iconos fueron sincronizados y estandarizados con transparencia real en desktopMain/resources." -ForegroundColor Green

