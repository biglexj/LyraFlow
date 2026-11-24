Set-StrictMode -Version Latest
. (Join-Path $PSScriptRoot 'ReleaseTools.ps1')

function Get-AuroraReleasePayload {
    param([string]$Root, [string]$Version, [string]$NotesPath, [string]$Installer)
    if (-not $env:LYRAFLOW_AURORA_APP_ID) { throw 'Falta LYRAFLOW_AURORA_APP_ID: usa el identificador real de la ficha de LyraFlow.' }
    $info = Get-CargoManifestInfo -Root $Root
    $code = $info.VersionCode
    if ($code -le 0) { throw 'Falta version_code positivo en Cargo.toml.' }
    return @{
        appId = $env:LYRAFLOW_AURORA_APP_ID
        versionName = $Version
        versionCode = [int]$code
        releaseNotes = if ($NotesPath) { [IO.File]::ReadAllText($NotesPath, [Text.Encoding]::UTF8) } else { '' }
        downloadUrl = "https://github.com/biglexj/LyraFlow/releases/download/v$Version/LyraFlow-Windows-$Version.exe"
        sha256Checksum = if ($Installer) { (Get-FileHash -LiteralPath $Installer -Algorithm SHA256).Hash.ToLowerInvariant() } else { '' }
        status = 'published'
    }
}

function Get-AuroraReleaseHistory {
    if (-not $env:AURORA_RELEASE_TOKEN) { throw 'Falta AURORA_RELEASE_TOKEN en el entorno seguro de publicación.' }
    $uri = 'https://www.biglexj.com/api/admin/developer-app-releases?appId=' + [uri]::EscapeDataString($env:LYRAFLOW_AURORA_APP_ID)
    $response = Invoke-RestMethod -Uri $uri -Headers @{ Authorization = "Bearer $env:AURORA_RELEASE_TOKEN" } -TimeoutSec 30 -MaximumRedirection 0
    if ($response.success -ne $true -or $null -eq $response.releases) { throw 'Respuesta de historial de Aurora inválida.' }
    return @($response.releases)
}

function Assert-AuroraReleasePreflight {
    param([string]$Root, [string]$Version)
    $payload = Get-AuroraReleasePayload -Root $Root -Version $Version
    foreach ($row in (Get-AuroraReleaseHistory)) {
        $rowCode = if ($row.PSObject.Properties.Name -contains 'versionCode') { $row.versionCode } else { $row.version_code }
        $rowVersion = if ($row.PSObject.Properties.Name -contains 'versionName') { $row.versionName } else { $row.version_name }
        if ([int]$rowCode -ge $payload.versionCode -or [version]$rowVersion -ge [version]$Version) {
            throw 'Aurora ya registra esta versión o una superior. No se puede sobrescribir.'
        }
    }
}

function Publish-AuroraRelease {
    param([string]$Root, [string]$Version, [string]$NotesPath, [string]$Installer)
    $payload = Get-AuroraReleasePayload @PSBoundParameters
    $json = $payload | ConvertTo-Json -Depth 5
    $body = [Text.Encoding]::UTF8.GetBytes($json)
    Invoke-RestMethod -Uri 'https://www.biglexj.com/api/admin/developer-app-releases' -Method Post -ContentType 'application/json; charset=utf-8' `
        -Headers @{ Authorization = "Bearer $env:AURORA_RELEASE_TOKEN" } -Body $body -TimeoutSec 30 -MaximumRedirection 0 | Out-Null
    $rows = @(Get-AuroraReleaseHistory | Where-Object {
        ($_.PSObject.Properties.Name -contains 'version_name' -and $_.version_name -eq $Version) -or
        ($_.PSObject.Properties.Name -contains 'versionName' -and $_.versionName -eq $Version)
    })
    if ($rows.Count -ne 1) { throw 'GitHub publicado; Aurora no quedó verificada. Revisar el destino sin recrear la release.' }
    $row = $rows[0]
    foreach ($field in @(@('releaseNotes','release_notes'), @('versionCode','version_code'), @('downloadUrl','download_url'), @('sha256Checksum','sha256_checksum'), @('status','status'))) {
        $key = if ($row.PSObject.Properties.Name -contains $field[0]) { $field[0] } else { $field[1] }
        if ($row.$key -cne $payload[$field[0]]) { throw "Aurora no coincide con el artefacto publicado ($($field[0]))." }
    }
    Write-Host 'Release Message y descarga verificados en Aurora.' -ForegroundColor Green
}
