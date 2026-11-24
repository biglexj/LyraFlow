Set-StrictMode -Version Latest

# --- Compatibilidad de modulos con Windows PowerShell 5.1 --------------------
# Si el PSModulePath apunta antes a la copia de PowerShell 7 (instalacion de
# la Store), Windows PowerShell resuelve Microsoft.PowerShell.Utility y
# Microsoft.PowerShell.Security a la version 7, que no carga en el host de
# .NET Framework: Get-FileHash y Get-AuthenticodeSignature dejan de existir
# y el script de release aborta al calcular las sumas o al verificar la firma.
# Se antepone la carpeta de modulos del propio Windows PowerShell.
if ($PSVersionTable.PSEdition -eq 'Desktop') {
    $systemModulePath = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\Modules'
    if ($env:PSModulePath -and (Test-Path -LiteralPath $systemModulePath)) {
        $segments = @($env:PSModulePath -split ';' | Where-Object { $_ -and $_ -ne $systemModulePath })
        $env:PSModulePath = (@($systemModulePath) + $segments) -join ';'
    }
}

function Invoke-Checked {
    param(
        [Parameter(Mandatory)] [string]$Executable,
        [Parameter(Mandatory)] [AllowEmptyString()] [string[]]$ArgumentList
    )

    & $Executable @ArgumentList
    if ($LASTEXITCODE -ne 0) {
        throw "El comando '$Executable' terminó con código $LASTEXITCODE."
    }
}

function Get-CargoManifestInfo {
    param([Parameter(Mandatory)] [string]$Root)

    $manifestPath = Join-Path $Root 'Cargo.toml'
    if (-not (Test-Path -LiteralPath $manifestPath)) {
        throw "No se encontró Cargo.toml en $Root."
    }
    $manifest = Get-Content -LiteralPath $manifestPath -Raw -Encoding UTF8
    $versionMatch = [regex]::Match($manifest, '(?m)^\[package\][\s\S]*?^version\s*=\s*"(\d+\.\d+\.\d+)"')
    $codeMatch = [regex]::Match($manifest, '(?m)^version_code\s*=\s*(\d+)')
    if (-not $versionMatch.Success) {
        throw "No se pudo leer version desde Cargo.toml."
    }
    if (-not $codeMatch.Success -or [int]$codeMatch.Groups[1].Value -le 0) {
        throw "No se pudo leer version_code positivo desde Cargo.toml ([package.metadata.lyraflow])."
    }
    return @{
        Version = $versionMatch.Groups[1].Value
        VersionCode = [int]$codeMatch.Groups[1].Value
    }
}

function Get-WindowsSdkTool {
    param([Parameter(Mandatory)] [string]$Name)

    $sdkRoot = "C:\Program Files (x86)\Windows Kits\10\bin"
    $sdk = Get-ChildItem -LiteralPath $sdkRoot -Directory |
        Where-Object Name -Match '^10\.' |
        Sort-Object { [version]$_.Name } -Descending |
        Select-Object -First 1
    if (-not $sdk) { throw "No se encontró Windows SDK." }

    $tool = Join-Path $sdk.FullName "x64\$Name"
    if (-not (Test-Path -LiteralPath $tool)) {
        throw "No se encontró $Name en Windows SDK."
    }
    return $tool
}

function Assert-SemanticVersion {
    param([Parameter(Mandatory)] [string]$Version)

    if ($Version -notmatch '^(\d+)\.(\d+)\.(\d+)$') {
        throw "La versión '$Version' no cumple el formato mayor.menor.parche."
    }
    # SemVer no limita cada segmento a un solo digito: 1.1.12 es una version
    # valida (Core-Docs: protocolo_lanzamiento_actualizacion.md y
    # web-release-message-standard.md). El salto se decide por el alcance
    # real del cambio, no por la cantidad de digitos.
}

function Assert-PublishPreflight {
    param(
        [Parameter(Mandatory)] [string]$Root,
        [Parameter(Mandatory)] [string]$Repository,
        [Parameter(Mandatory)] [string]$Tag
    )

    Push-Location $Root
    try {
        $branch = (& git branch --show-current).Trim()
        if ($LASTEXITCODE -ne 0 -or $branch -notin @('preview', 'main')) {
            throw "La publicación exige preview o main; rama actual: '$branch'."
        }
        if ((& git status --porcelain)) { throw 'Hay cambios sin confirmar. Revisa y confirma el código antes de publicar.' }

        $origin = (& git remote get-url origin).Trim()
        if ($LASTEXITCODE -ne 0 -or $origin -notmatch "github\.com[/:]$([regex]::Escape($Repository))(\.git)?$") {
            throw "El remoto origin no corresponde a ${Repository}: '$origin'."
        }

        Invoke-Checked git @("fetch", "origin", "main", "--tags")
        $localHead = (& git rev-parse HEAD).Trim()
        $remoteHead = (& git rev-parse origin/main).Trim()
        Invoke-Checked git @('merge-base', '--is-ancestor', $remoteHead, $localHead)

        & git rev-parse --quiet --verify "refs/tags/$Tag" *> $null
        if ($LASTEXITCODE -eq 0) { throw "El tag $Tag ya existe." }

        Invoke-Checked gh @("auth", "status")
        $releaseExists = $true
        $oldPreference = $ErrorActionPreference
        try {
            $ErrorActionPreference = "Continue"
            & gh release view $Tag --repo $Repository 2>$null | Out-Null
            if ($LASTEXITCODE -ne 0) { $releaseExists = $false }
        } finally {
            $ErrorActionPreference = $oldPreference
        }
        if ($releaseExists) { throw "El GitHub Release $Tag ya existe." }
    } finally {
        Pop-Location
    }
}

function Assert-PublishedRelease {
    param([string]$Root, [string]$Repository, [string]$Tag, [string]$Installer, [string]$NotesPath)
    $raw = & gh api "repos/$Repository/releases/tags/$Tag"
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo verificar la release remota.' }
    $release = $raw | ConvertFrom-Json
    if ($release.draft -or $release.prerelease -or $release.body.Trim() -ne (Get-Content -LiteralPath $NotesPath -Raw -Encoding UTF8).Trim()) {
        throw 'La release pública no coincide con las notas estables locales.'
    }
    $leaf = Split-Path $Installer -Leaf
    $asset = @($release.assets | Where-Object name -eq $leaf)
    if ($asset.Count -ne 1 -or $asset[0].size -ne (Get-Item -LiteralPath $Installer).Length) { throw 'El instalador remoto no coincide con el local.' }
    $verificationDir = Join-Path $Root ('temp\verify-release-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $verificationDir -Force | Out-Null
    Invoke-Checked gh @('release', 'download', $Tag, '--repo', $Repository, '--pattern', $leaf, '--dir', $verificationDir)
    if ((Get-FileHash -LiteralPath (Join-Path $verificationDir $leaf)).Hash -ne (Get-FileHash -LiteralPath $Installer).Hash) {
        throw 'La descarga pública no coincide con el SHA-256 local. No sincronizar Aurora.'
    }
}

function Import-SecurityModule {
    # El modulo Microsoft.PowerShell.Security no siempre se puede cargar por
    # nombre: si el PSModulePath apunta antes a la copia de PowerShell 7, sus
    # TypeData chocan con los tipos de .NET Framework y la importacion falla
    # con "Ya existe el miembro ...". En ese caso se carga la copia del
    # propio Windows PowerShell, que es la compatible con el host.
    try {
        Import-Module Microsoft.PowerShell.Security -ErrorAction Stop
        return
    } catch {
        $systemModule = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\Modules\Microsoft.PowerShell.Security\Microsoft.PowerShell.Security.psd1'
        if (-not (Test-Path -LiteralPath $systemModule)) {
            throw $_
        }
        Import-Module $systemModule -ErrorAction Stop
    }
}

function Assert-SignedArtifact {
    param(
        [Parameter(Mandatory)] [string]$Path,
        [Parameter(Mandatory)] [string]$Publisher
    )

    Import-SecurityModule
    $signature = Get-AuthenticodeSignature -LiteralPath $Path
    if (-not $signature.SignerCertificate -or $signature.SignerCertificate.Subject -ne $Publisher) {
        throw "La firma de '$Path' no corresponde a $Publisher."
    }
}
