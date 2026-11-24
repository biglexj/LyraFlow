<#
.SYNOPSIS
    Sincronización y generación reproducible de iconos derivados de LyraFlow conforme a Core.
.DESCRIPTION
    Lee la fuente canónica 'assets/branding/icons/icon-transparent.png' y genera los recursos
    derivados del branding nativo, de barra de título y del instalador oficial en Windows,
    conservando el canal alfa transparente en las superficies del sistema.
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

Write-Host "═════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  LyraFlow — Sincronización de Iconos de Branding" -ForegroundColor Cyan
Write-Host "═════════════════════════════════════════════════" -ForegroundColor Cyan

# 1. assets/branding/icons/icon.png
$brandingIcon = Join-Path $root "assets\branding\icons\icon.png"
Copy-Item -LiteralPath $masterIcon -Destination $brandingIcon -Force
Write-Host "✔ assets\branding\icons\icon.png actualizado (transparente)." -ForegroundColor Green

# 2. assets/branding/icons/app_icon.ico
$appIco = Join-Path $root "assets\branding\icons\app_icon.ico"
$magickCmd = Get-Command magick -ErrorAction SilentlyContinue
if ($magickCmd) {
    & magick "$masterIcon" -define icon:auto-resize=256,128,64,48,32,24,16 "$appIco"
    Write-Host "✔ app_icon.ico generado con ImageMagick (256,128,64,48,32,24,16 - canal alfa puro)." -ForegroundColor Green
} else {
    Write-Warning "ImageMagick no encontrado en PATH; usando generación nativa .NET..."
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
    $fs = [System.IO.File]::Open($appIco, [System.IO.FileMode]::Create)
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

# 3. Icono del instalador oficial Inno Setup.
$installerResources = Join-Path $root "packaging\windows"
New-Item -ItemType Directory -Path $installerResources -Force | Out-Null
Copy-Item -LiteralPath $appIco -Destination (Join-Path $installerResources "LyraFlow.ico") -Force
Write-Host "✔ packaging\windows\LyraFlow.ico sincronizado." -ForegroundColor Green

# 4. Distintivo con fondo blanco redondeado para el icono superior de barra de título.
$brandingDir = Join-Path $root "assets\branding"
New-Item -ItemType Directory -Path $brandingDir -Force | Out-Null
$badgeOut = Join-Path $brandingDir "titlebar-icon-badge.png"
$badgeSize = 64
$badgeBmp = New-Object System.Drawing.Bitmap($badgeSize, $badgeSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$bg = [System.Drawing.Graphics]::FromImage($badgeBmp)
$bg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$bg.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$bg.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$bg.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
$bg.Clear([System.Drawing.Color]::Transparent)

$path = New-Object System.Drawing.Drawing2D.GraphicsPath
$rect = New-Object System.Drawing.RectangleF(2, 2, 60, 60)
$d = 24
$path.AddArc($rect.X, $rect.Y, $d, $d, 180, 90)
$path.AddArc($rect.Right - $d, $rect.Y, $d, $d, 270, 90)
$path.AddArc($rect.Right - $d, $rect.Bottom - $d, $d, $d, 0, 90)
$path.AddArc($rect.X, $rect.Bottom - $d, $d, $d, 90, 90)
$path.CloseFigure()

$fillBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 255, 255))
$borderPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 203, 213, 225), 2)
$bg.FillPath($fillBrush, $path)
$bg.DrawPath($borderPen, $path)

$srcIcon = [System.Drawing.Bitmap]::FromFile($masterIcon)
$bg.DrawImage($srcIcon, 6, 6, 52, 52)

$srcIcon.Dispose()
$borderPen.Dispose()
$fillBrush.Dispose()
$path.Dispose()
$bg.Dispose()
$badgeBmp.Save($badgeOut, [System.Drawing.Imaging.ImageFormat]::Png)
$badgeBmp.Dispose()
Write-Host "✔ assets\branding\titlebar-icon-badge.png generado." -ForegroundColor Green
