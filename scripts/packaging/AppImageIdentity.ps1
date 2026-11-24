Set-StrictMode -Version Latest
. (Join-Path $PSScriptRoot '..\release\ReleaseTools.ps1')

function Get-AppImageIdentity {
    param([Parameter(Mandatory)] [string]$Root)
    $stage = Join-Path $Root 'build\inno-staging\app'
    $sourcePaths = @('src', 'assets', 'build.rs', 'Cargo.toml', 'Cargo.lock')
    $sources = @($sourcePaths | ForEach-Object {
        $path = Join-Path $Root $_
        if (Test-Path -LiteralPath $path -PathType Container) { Get-ChildItem -LiteralPath $path -File -Recurse }
        elseif (Test-Path -LiteralPath $path) { Get-Item -LiteralPath $path }
    } | Sort-Object FullName | ForEach-Object {
        '{0} {1}' -f $_.FullName.Substring($Root.Length + 1), (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
    })
    if (-not (Test-Path -LiteralPath (Join-Path $stage 'LyraFlow.exe'))) {
        throw 'Falta el binario preparado en build\inno-staging\app\LyraFlow.exe. Ejecuta release:local sin -SkipBuild.'
    }
    $artifacts = @(Get-ChildItem -LiteralPath $stage -File -Recurse | Sort-Object FullName | ForEach-Object {
        '{0} {1}' -f $_.FullName.Substring($stage.Length + 1), (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
    })
    return @{ sources = $sources; artifacts = $artifacts }
}

function Write-AppImageIdentity {
    param([Parameter(Mandatory)] [string]$Root)
    $identity = Get-AppImageIdentity -Root $Root
    $buildDir = Join-Path $Root 'build'
    New-Item -ItemType Directory -Path $buildDir -Force | Out-Null
    $path = Join-Path $buildDir 'app-image-identity.json'
    $identity | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $path -Encoding UTF8
}

function Assert-AppImageIdentity {
    param([Parameter(Mandatory)] [string]$Root)
    $path = Join-Path $Root 'build\app-image-identity.json'
    if (-not (Test-Path -LiteralPath $path)) {
        throw 'La imagen no tiene evidencia del build. Ejecuta release:local sin -SkipBuild.'
    }
    $expected = Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
    $actual = Get-AppImageIdentity -Root $Root
    foreach ($kind in @('sources', 'artifacts')) {
        if (Compare-Object @($expected.$kind) @($actual[$kind])) {
            throw "La imagen está obsoleta o fue modificada ($kind). Ejecuta release:local sin -SkipBuild."
        }
    }
}
