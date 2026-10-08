$ErrorActionPreference = "Stop"

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$secure = Read-Host "alerts.in.ua API token" -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)

try {
    $token = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)

    $headers = @{
        Authorization = "Bearer $token"
        Accept = "application/json"
    }

    $url = "https://api.alerts.in.ua/v1/iot/active_air_raid_alerts/81.json"

    Write-Host "GET $url"

    $response = Invoke-WebRequest `
        -UseBasicParsing `
        -Uri $url `
        -Headers $headers `
        -Method GET `
        -TimeoutSec 10

    $raw = $response.Content.Trim()
    $state = $raw.Trim('"')

    Write-Host "HTTP: $($response.StatusCode)"
    Write-Host "RAW:  $raw"

    switch ($state) {
        "A" { Write-Host "STATE: ACTIVE - air raid alert is active" }
        "P" { Write-Host "STATE: PARTIAL - partial alert; LyceumTV treats it as AIR_RAID" }
        "N" { Write-Host "STATE: NONE - no active air raid alert" }
        default { Write-Warning "Unknown API response: $raw" }
    }
}
finally {
    if ($ptr -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }

    Remove-Variable token -ErrorAction SilentlyContinue
}
