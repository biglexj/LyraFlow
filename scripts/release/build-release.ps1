param(
    [string]$Version,
    [string]$ReleaseNotesFile = "RELEASE_MESSAGE.md",
    [switch]$LocalOnly,
    [switch]$SkipTests,
    [switch]$SkipBuild,
    [switch]$SkipSigning
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
. (Join-Path $root "scripts\release\ReleaseTools.ps1")

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
if (-not (Test-Path -LiteralPath $releaseNotesPath)) {
    throw "No se encontró el archivo de notas: $releaseNotesPath"
}
if (-not $LocalOnly -and $SkipSigning) {
    throw "Una publicación oficial no puede usar -SkipSigning."
}

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
}

if ($Version -ne $currentVersion) {
    $newCode = [int]$codeMatch.Groups[1].Value + 1
    $properties = $properties `
        -replace '(?m)^(lyraflow\.versionName=)\d+\.\d+\.\d+$', "`${1}$Version" `
        -replace '(?m)^(lyraflow\.versionCode=)\d+$', "`${1}$newCode"
    Set-Content -LiteralPath $propertiesPath -Value $properties -Encoding UTF8 -NoNewline

    $appVersionFile = Join-Path $root "composeApp\src\commonMain\kotlin\com\biglexj\lyraflow\core\config\AppVersion.kt"
    if (Test-Path -LiteralPath $appVersionFile) {
        $appVersionContent = Get-Content -LiteralPath $appVersionFile -Raw -Encoding UTF8
        $appVersionContent = $appVersionContent -replace 'const val CURRENT = ".*?"', "const val CURRENT = `"$Version`""
        Set-Content -LiteralPath $appVersionFile -Value $appVersionContent -Encoding UTF8 -NoNewline
    }

    $mainWxs = Join-Path $root "package\windows\main.wxs"
    if (Test-Path -LiteralPath $mainWxs) {
        $wxsContent = Get-Content -LiteralPath $mainWxs -Raw -Encoding UTF8
        $wxsContent = $wxsContent -replace 'Version="[^"]+"', "Version=`"$Version`""
        Set-Content -LiteralPath $mainWxs -Value $wxsContent -Encoding UTF8 -NoNewline
    }
    Write-Host "[1/7] Versión actualizada a $Version ($newCode)." -ForegroundColor Yellow
} else {
    Write-Host "[1/7] Versión centralizada confirmada: $Version." -ForegroundColor Yellow
}

$jdk = Get-FullJdk
$signTool = if ($SkipSigning) { $null } else { Get-WindowsSdkTool "signtool.exe" }
$env:JAVA_HOME = $jdk

if (-not $SkipBuild) {
    Write-Host "[2/7] Compilando y ejecutando verificaciones..." -ForegroundColor Yellow
    $tasks = @(
        ":composeApp:createDistributable",
        ":composeApp:packageExe"
    )
    if (-not $SkipTests) { $tasks = @(":composeApp:desktopTest") + $tasks }
    $gradleArguments = @("-Dorg.gradle.java.home=$jdk", "--no-configuration-cache") + $tasks
    Invoke-Checked -Executable (Join-Path $root "gradlew.bat") -ArgumentList $gradleArguments
} else {
    Write-Host "[2/7] Build omitido; se usarán binarios existentes." -ForegroundColor DarkYellow
}

Write-Host "[3/7] Preparando instalador EXE..." -ForegroundColor Yellow
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
Copy-Item -LiteralPath (Join-Path $root "composeApp\build\compose\binaries\main\exe\LyraFlow-$Version.exe") -Destination $exe
(Get-Item -LiteralPath $exe).IsReadOnly = $false

Write-Host "[4/7] Firmando y verificando artefactos..." -ForegroundColor Yellow
if (-not $SkipSigning) {
    $certificate = Join-Path $root "LyraFlow_Dev_Certificate.pfx"
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
Get-ChildItem -LiteralPath $output -File |
    Where-Object Extension -In '.exe' |
    Get-FileHash -Algorithm SHA256 |
    ForEach-Object { "{0}  {1}" -f $_.Hash.ToLowerInvariant(), (Split-Path $_.Path -Leaf) } |
    Set-Content -LiteralPath $hashPath -Encoding UTF8

Write-Host "[5/7] Artefactos verificados:" -ForegroundColor Yellow
Get-ChildItem -LiteralPath $output -File | ForEach-Object {
    Write-Host "  $($_.Name) ($([math]::Round($_.Length / 1MB, 2)) MB)" -ForegroundColor Gray
}
if ($LocalOnly) {
    Write-Host "Build local terminado. Git y GitHub fueron omitidos." -ForegroundColor Green
    exit 0
}

Write-Host "[6/7] Creando commit, tag y push atómico..." -ForegroundColor Yellow
Push-Location $root
try {
    Invoke-Checked git @("add", "-A")
    Invoke-Checked git @("diff", "--cached", "--check")
    & git diff --cached --quiet
    if ($LASTEXITCODE -ne 0) {
        Invoke-Checked git @("commit", "-m", "release: LyraFlow $tag")
    }
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

Write-Host "Release publicada: https://github.com/$repository/releases/tag/$tag" -ForegroundColor Green
