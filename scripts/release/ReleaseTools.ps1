Set-StrictMode -Version Latest

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

function Get-FullJdk {
    $candidates = @(
        $env:JAVA_HOME,
        "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot",
        "C:\Program Files\Android\Android Studio\jbr"
    ) | Where-Object {
        $_ -and (Test-Path -LiteralPath (Join-Path $_ "bin\jpackage.exe"))
    }

    $jdk = $candidates | Select-Object -First 1
    if (-not $jdk) {
        throw "Configura JAVA_HOME con un JDK completo que incluya jpackage.exe."
    }
    return $jdk
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
    if ([int]$Matches[3] -gt 9) {
        throw "La regla del .9 impide publicar el parche '$Version'."
    }
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
        if ($LASTEXITCODE -ne 0 -or $branch -ne "main") {
            throw "La publicación exige la rama main; rama actual: '$branch'."
        }

        $origin = (& git remote get-url origin).Trim()
        if ($LASTEXITCODE -ne 0 -or $origin -notmatch "github\.com[/:]$([regex]::Escape($Repository))(\.git)?$") {
            throw "El remoto origin no corresponde a ${Repository}: '$origin'."
        }

        Invoke-Checked git @("fetch", "origin", "main", "--tags")
        $localHead = (& git rev-parse HEAD).Trim()
        $remoteHead = (& git rev-parse origin/main).Trim()
        if ($localHead -ne $remoteHead) {
            throw "main debe estar sincronizada con origin/main antes de publicar."
        }

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

function Assert-SignedArtifact {
    param(
        [Parameter(Mandatory)] [string]$Path,
        [Parameter(Mandatory)] [string]$Publisher
    )

    $signature = Get-AuthenticodeSignature -LiteralPath $Path
    if (-not $signature.SignerCertificate -or $signature.SignerCertificate.Subject -ne $Publisher) {
        throw "La firma de '$Path' no corresponde a $Publisher."
    }
}
