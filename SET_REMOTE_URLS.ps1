param(
    [string]$GoogleAppsScriptUrl,
    [string]$GithubManifestUrl,
    [string]$Device
)

$ErrorActionPreference = "Stop"
$PackageName = "ua.edu.cunl.lyceummobile"
$ActivityName = "$PackageName/.MainActivity"

function Find-Adb {
    $candidates = @()
    if ($env:ANDROID_HOME) {
        $candidates += (Join-Path $env:ANDROID_HOME "platform-tools\adb.exe")
    }
    if ($env:ANDROID_SDK_ROOT) {
        $candidates += (Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe")
    }
    if ($env:LOCALAPPDATA) {
        $candidates += (Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe")
    }
    try {
        $fromPath = (Get-Command adb.exe -ErrorAction Stop).Source
        if ($fromPath) { $candidates += $fromPath }
    } catch {}

    foreach ($candidate in $candidates | Select-Object -Unique) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) {
            return $candidate
        }
    }
    throw "adb.exe not found. Install Android SDK Platform-Tools or open Android Studio once."
}

function To-Base64Utf8([string]$Value) {
    return [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($Value))
}

function Read-HttpsUrl([string]$Prompt, [string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) {
        $Value = Read-Host $Prompt
    }
    $Value = $Value.Trim()
    if (-not $Value.StartsWith("https://", [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "URL must start with https://"
    }
    return $Value
}

$adb = Find-Adb
Write-Host "ADB: $adb" -ForegroundColor Cyan

$GoogleAppsScriptUrl = Read-HttpsUrl "Paste Google Apps Script HTTPS /exec URL" $GoogleAppsScriptUrl
$GithubManifestUrl = Read-HttpsUrl "Paste GitHub Raw content_manifest.json URL" $GithubManifestUrl

if ($GoogleAppsScriptUrl -notmatch '^https://script\.google\.com/macros/s/.+/exec(?:\?.*)?$') {
    throw "Google Apps Script URL must look like https://script.google.com/macros/s/.../exec"
}
if ($GithubManifestUrl -notmatch '^https://raw\.githubusercontent\.com/.+/content_manifest\.json(?:\?.*)?$') {
    throw "GitHub URL must be a Raw URL from raw.githubusercontent.com ending with content_manifest.json"
}

& $adb start-server | Out-Null
$deviceLines = @(& $adb devices) | Select-Object -Skip 1 | Where-Object { $_ -match '\tdevice$' }
$serials = @($deviceLines | ForEach-Object { ($_ -split '\t')[0].Trim() })

if ($Device) {
    if ($serials -notcontains $Device) {
        throw "Requested device '$Device' is not connected/authorized. Connected: $($serials -join ', ')"
    }
    $serial = $Device
} elseif ($serials.Count -eq 1) {
    $serial = $serials[0]
} elseif ($serials.Count -eq 0) {
    throw "No authorized Android device found. Connect the Redmi by USB, enable USB debugging, and accept the RSA prompt."
} else {
    Write-Host "Multiple Android devices found:" -ForegroundColor Yellow
    for ($i = 0; $i -lt $serials.Count; $i++) {
        Write-Host "  [$($i + 1)] $($serials[$i])"
    }
    $choice = [int](Read-Host "Select device number")
    if ($choice -lt 1 -or $choice -gt $serials.Count) { throw "Invalid device selection." }
    $serial = $serials[$choice - 1]
}

$model = (& $adb -s $serial shell getprop ro.product.model 2>$null).Trim()
Write-Host "Device: $serial  $model" -ForegroundColor Green

$packageCheck = & $adb -s $serial shell pm path $PackageName 2>&1
if ($LASTEXITCODE -ne 0 -or ($packageCheck -join "`n") -notmatch '^package:') {
    throw "LyceumMobile is not installed on this device. Install v9 first, then run this script again."
}

$packageDump = @(& $adb -s $serial shell dumpsys package $PackageName 2>$null)
$versionLine = ($packageDump | Where-Object { $_ -match 'versionCode=' } | Select-Object -First 1)
$versionCode = 0
if ($versionLine -match 'versionCode=(\d+)') {
    $versionCode = [int]$Matches[1]
}
if ($versionCode -lt 9) {
    throw "Installed LyceumMobile is too old (versionCode=$versionCode). Install v9 or newer first."
}
Write-Host "LyceumMobile versionCode: $versionCode" -ForegroundColor Green

$googleB64 = To-Base64Utf8 $GoogleAppsScriptUrl
$githubB64 = To-Base64Utf8 $GithubManifestUrl

Write-Host "Sending URLs to LyceumMobile..." -ForegroundColor Cyan
$result = & $adb -s $serial shell am start `
    -n $ActivityName `
    --es pc_google_url_b64 $googleB64 `
    --es pc_github_manifest_url_b64 $githubB64 `
    --es pc_source GITHUB 2>&1
$exit = $LASTEXITCODE
$result | ForEach-Object { Write-Host $_ }

if ($exit -ne 0 -or ($result -join "`n") -match 'Error:|Exception') {
    throw "Could not send configuration to LyceumMobile."
}

Write-Host "" 
Write-Host "OK. URLs were sent to LyceumMobile." -ForegroundColor Green
Write-Host "Google Apps Script: $GoogleAppsScriptUrl"
Write-Host "GitHub Raw manifest: $GithubManifestUrl"
Write-Host "Source selected in the app: GITHUB"
Write-Host "The app should open on the Redmi and refresh its data automatically."
