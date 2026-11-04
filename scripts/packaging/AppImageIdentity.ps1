Set-StrictMode -Version Latest
. (Join-Path $PSScriptRoot '..\release\ReleaseTools.ps1')

function Get-AppImageIdentity {
    param([Parameter(Mandatory)] [string]$Root)
    $image = Join-Path $Root 'composeApp\build\compose\binaries\main\app\LyraFlow'
    $sourcePaths = @('composeApp\src', 'gradle', 'gradle.properties', 'build.gradle.kts', 'composeApp\build.gradle.kts', 'settings.gradle.kts')
    $sources = @($sourcePaths | ForEach-Object {
        $path = Join-Path $Root $_
        if (Test-Path -LiteralPath $path -PathType Container) { Get-ChildItem -LiteralPath $path -File -Recurse }
        elseif (Test-Path -LiteralPath $path) { Get-Item -LiteralPath $path }
    } | Sort-Object FullName | ForEach-Object {
        '{0} {1}' -f $_.FullName.Substring($Root.Length + 1), (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
    })
    if (-not (Test-Path -LiteralPath (Join-Path $image 'LyraFlow.exe'))) { throw 'Falta la imagen de aplicación. Ejecuta release:local sin -SkipBuild.' }
    $artifacts = @(Get-ChildItem -LiteralPath $image -File -Recurse | Sort-Object FullName | ForEach-Object {
        '{0} {1}' -f $_.FullName.Substring($image.Length + 1), (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
    })
    return @{ sources = $sources; artifacts = $artifacts }
}

function Write-AppImageIdentity {
    param([Parameter(Mandatory)] [string]$Root)
    $identity = Get-AppImageIdentity -Root $Root
    $path = Join-Path $Root 'composeApp\build\app-image-identity.json'
    $identity | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $path -Encoding UTF8
}

function Assert-AppImageIdentity {
    param([Parameter(Mandatory)] [string]$Root)
    $path = Join-Path $Root 'composeApp\build\app-image-identity.json'
    if (-not (Test-Path -LiteralPath $path)) { throw 'La imagen no tiene evidencia del build. Ejecuta release:local sin -SkipBuild.' }
    $expected = Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
    $actual = Get-AppImageIdentity -Root $Root
    foreach ($kind in @('sources', 'artifacts')) {
        if (Compare-Object @($expected.$kind) @($actual[$kind])) {
            throw "La imagen está obsoleta o fue modificada ($kind). Ejecuta release:local sin -SkipBuild."
        }
    }
}
