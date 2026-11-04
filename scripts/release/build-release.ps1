param(
    [string]$Version,
    [string]$ReleaseNotesFile = "RELEASE_MESSAGE.md",
    [switch]$LocalOnly,
    [switch]$Publish,
    [switch]$SkipTests,
    [switch]$SkipBuild,
    [switch]$SkipSigning,
    [ValidateSet("github", "all")] [string]$Channel = "all"
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
. (Join-Path $root "scripts\release\ReleaseTools.ps1")
. (Join-Path $root "scripts\packaging\AppImageIdentity.ps1")
. (Join-Path $root "scripts\release\AuroraRelease.ps1")
if ($LocalOnly -and $Publish) { throw 'No combines -LocalOnly y -Publish.' }
$LocalOnly = -not $Publish

$repository = "biglexj/LyraFlow"
$propertiesPath = Join-Path $root "gradle.properties"
$releaseNotesPath = Join-Path $root $ReleaseNotesFile
$properties = Get-Content -LiteralPath $propertiesPath -Raw -Encoding UTF8
$versionMatch = [regex]::Match($properties, '(?m)^lyraflow\.versionName=(\d+\.\d+\.\d+)')
$codeMatch = [regex]::Match($properties, '(?m)^lyraflow\.versionCode=(\d+)')
if (-not $versionMatch.Success -or -not $codeMatch.Success) {
    throw "No se pudo leer la versión centralizada de LyraFlow."
}

$currentVersion = $versionMatch.Groups[1].Value
if (-not $Version) { $Version = $currentVersion }
Assert-SemanticVersion $Version
if ($Version -ne $currentVersion) { throw 'Actualiza y revisa las fuentes de versión antes de empaquetar una versión distinta.' }
$packageVersion = (Get-Content -LiteralPath (Join-Path $root 'package.json') -Raw | ConvertFrom-Json).version
$appVersionContent = Get-Content -LiteralPath (Join-Path $root 'composeApp\src\commonMain\kotlin\com\biglexj\lyraflow\core\config\AppVersion.kt') -Raw
if ($packageVersion -ne $Version -or $appVersionContent -notmatch ('const val CURRENT = "' + [regex]::Escape($Version) + '"')) {
    throw 'Las versiones de package.json, AppVersion.kt y gradle.properties no coinciden.'
}
if (-not (Test-Path -LiteralPath $releaseNotesPath)) {
    throw "No se encontró el archivo de notas: $releaseNotesPath"
}
if (-not $LocalOnly -and $SkipSigning) {
    throw "Una publicación oficial no puede usar -SkipSigning."
}
if ($Publish -and ($SkipBuild -or $SkipTests)) { throw 'Una publicación debe compilar y ejecutar las pruebas del código que va a distribuir.' }

$tag = "v$Version"
$output = Join-Path $root "release"
$artifactNames = @(
    "LyraFlow-Windows-$Version.exe"
)

Write-Host ""
Write-Host "══════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "  LyraFlow — Release $tag" -ForegroundColor Magenta
Write-Host "══════════════════════════════════════════" -ForegroundColor Magenta

if (-not $LocalOnly) {
    Assert-PublishPreflight -Root $root -Repository $repository -Tag $tag
    if ($Channel -eq 'all') { Assert-AuroraReleasePreflight -Root $root -Version $Version }
}

Write-Host "[1/7] Versión centralizada confirmada: $Version." -ForegroundColor Yellow

$jdk = Get-FullJdk
$signTool = if ($SkipSigning) { $null } else { Get-WindowsSdkTool "signtool.exe" }
$env:JAVA_HOME = $jdk

if (-not $SkipBuild) {
    Write-Host "[2/7] Compilando y ejecutando verificaciones..." -ForegroundColor Yellow
    $tasks = @(
        ":composeApp:createDistributable"
    )
    if (-not $SkipTests) { $tasks = @(":composeApp:desktopTest") + $tasks }
    $gradleArguments = @("-Dorg.gradle.java.home=$jdk", "--no-configuration-cache") + $tasks
    Invoke-Checked -Executable (Join-Path $root "gradlew.bat") -ArgumentList $gradleArguments
    Write-AppImageIdentity -Root $root
} else {
    Write-Host "[2/7] Build omitido; se usarán binarios existentes." -ForegroundColor DarkYellow
}

Write-Host "[3/7] Generando el instalador EXE (Inno Setup)..." -ForegroundColor Yellow
Assert-AppImageIdentity -Root $root
New-Item -ItemType Directory -Path $output -Force | Out-Null
$resolvedRoot = [IO.Path]::GetFullPath($root).TrimEnd('\') + '\'
$resolvedOutput = [IO.Path]::GetFullPath($output).TrimEnd('\') + '\'
if (-not $resolvedOutput.StartsWith($resolvedRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw "La salida resuelta quedó fuera del proyecto."
}
Get-ChildItem -LiteralPath $output -Recurse -ErrorAction SilentlyContinue | ForEach-Object { if (-not $_.PSIsContainer) { $_.IsReadOnly = $false } }
try {
    Get-ChildItem -LiteralPath $output -File -ErrorAction SilentlyContinue | Remove-Item -Force
} catch [System.UnauthorizedAccessException] {
    Get-Process | Where-Object { try { $_.Path -and $_.Path.StartsWith($output, [StringComparison]::OrdinalIgnoreCase) } catch { $false } } | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 600
    Get-ChildItem -LiteralPath $output -File -ErrorAction SilentlyContinue | Remove-Item -Force
}
Get-ChildItem -LiteralPath $output -Directory -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force

$exe = Join-Path $output $artifactNames[0]
# El artefacto que se publica es el instalador EXE de Inno Setup. La
# distribucion (imagen de aplicacion) ya la genero Gradle en el paso [2/7],
# asi que el empaquetado no vuelve a compilar la app.
$installerBuilder = Join-Path $root "scripts\packaging\Build-Installer.ps1"
if (-not (Test-Path -LiteralPath $installerBuilder)) {
    throw "No se encontró el generador del instalador: $installerBuilder"
}
$builtInstaller = & $installerBuilder -Version $Version -SkipBuild -OutputDirectory $output | Select-Object -Last 1
if (-not $builtInstaller -or -not (Test-Path -LiteralPath $builtInstaller)) {
    throw "Inno Setup no produjo el instalador esperado en $output."
}
(Get-Item -LiteralPath $exe).IsReadOnly = $false

Write-Host "[4/7] Firmando y verificando artefactos..." -ForegroundColor Yellow
if (-not $SkipSigning) {
    $certificate = if ($env:LYRAFLOW_SIGNING_CERTIFICATE) { $env:LYRAFLOW_SIGNING_CERTIFICATE } else {
        Join-Path $root '.local\signing\LyraFlow_Dev_Certificate.pfx'
    }
    if (-not (Test-Path -LiteralPath $certificate)) { throw "Falta el certificado de firma local." }
    
    $maxAttempts = 5
    $signed = $false
    for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
        try {
            Start-Sleep -Milliseconds 600
            Invoke-Checked $signTool @("sign", "/fd", "SHA256", "/f", $certificate, $exe)
            $signed = $true
            break
        } catch {
            if ($attempt -lt $maxAttempts) {
                Write-Host "Reintento de firma ($attempt/$maxAttempts) tras espera de I/O..." -ForegroundColor DarkYellow
                Start-Sleep -Seconds 2
            } else {
                throw $_
            }
        }
    }
    Assert-SignedArtifact -Path $exe -Publisher "CN=biglexj"
}

$hashPath = Join-Path $output "SHA256SUMS.txt"
# Solo los binarios publicables entran en el fichero de sumas; el propio
# SHA256SUMS.txt queda fuera. Se escribe en UTF-8 sin BOM y con finales de
# linea LF: Set-Content -Encoding UTF8 de Windows PowerShell anade un BOM que
# hace que "sha256sum -c" (GNU) rechace el fichero como mal formado.
$hashLines = @(
    Get-ChildItem -LiteralPath $output -File |
        Where-Object Extension -In '.exe' |
        Get-FileHash -Algorithm SHA256 |
        ForEach-Object { "{0}  {1}" -f $_.Hash.ToLowerInvariant(), (Split-Path $_.Path -Leaf) }
)
[System.IO.File]::WriteAllText($hashPath, (($hashLines -join "`n") + "`n"), (New-Object System.Text.UTF8Encoding($false)))

Write-Host "[5/7] Artefactos verificados:" -ForegroundColor Yellow
Get-ChildItem -LiteralPath $output -File | ForEach-Object {
    Write-Host "  $($_.Name) ($([math]::Round($_.Length / 1MB, 2)) MB)" -ForegroundColor Gray
}
if ($LocalOnly) {
    Write-Host "Build local terminado. Git y GitHub fueron omitidos." -ForegroundColor Green
    exit 0
}

Write-Host "[6/7] Creando tag y push atómico del código revisado..." -ForegroundColor Yellow
Push-Location $root
try {
    if ((& git status --porcelain)) { throw 'El build modificó archivos versionados. Revisa y confirma los cambios antes de publicar.' }
    Invoke-Checked git @("tag", "-a", $tag, "-m", "LyraFlow $tag")
    Invoke-Checked git @("push", "--atomic", "origin", "HEAD:main", "refs/tags/$tag")
} finally {
    Pop-Location
}

Write-Host "[7/7] Creando GitHub Release..." -ForegroundColor Yellow
$assets = @($exe, $hashPath)
Invoke-Checked gh (@(
    "release", "create", $tag
) + $assets + @(
    "--repo", $repository,
    "--verify-tag",
    "--title", "LyraFlow $tag",
    "--notes-file", $releaseNotesPath
))

# Verificar el binario remoto antes de sincronizar su ficha.
Assert-PublishedRelease -Root $root -Repository $repository -Tag $tag -Installer $exe -NotesPath $releaseNotesPath
if ($Channel -eq 'all') {
    Publish-AuroraRelease -Root $root -Version $Version -NotesPath $releaseNotesPath -Installer $exe
}

Write-Host "Release publicada: https://github.com/$repository/releases/tag/$tag" -ForegroundColor Green
