$ErrorActionPreference = "Stop"
$Project = $PSScriptRoot
$PackageName = "ua.edu.cunl.lyceummobile"

# Manual argument parser keeps this script tolerant of an accidental trailing slash.
$Device = ""
$ApkPath = Join-Path $Project "app\build\outputs\apk\debug\app-debug.apk"
$ReplaceExisting = $false
for ($i = 0; $i -lt $args.Count; $i++) {
    $arg = [string]$args[$i]
    if ([string]::IsNullOrWhiteSpace($arg) -or $arg -eq "\") { continue }
    if ($arg -ieq "-Device" -and ($i + 1) -lt $args.Count) {
        $i++; $Device = [string]$args[$i]; continue
    }
    if ($arg -ieq "-ApkPath" -and ($i + 1) -lt $args.Count) {
        $i++; $ApkPath = [string]$args[$i]; continue
    }
    if ($arg -ieq "-ReplaceExisting") { $ReplaceExisting = $true; continue }
}

function Find-Adb {
    $sdk = $env:ANDROID_SDK_ROOT
    if ([string]::IsNullOrWhiteSpace($sdk)) { $sdk = $env:ANDROID_HOME }
    if ([string]::IsNullOrWhiteSpace($sdk)) {
        $candidate = Join-Path $env:LOCALAPPDATA "Android\Sdk"
        if (Test-Path -LiteralPath $candidate) { $sdk = $candidate }
    }
    if ([string]::IsNullOrWhiteSpace($sdk) -or -not (Test-Path -LiteralPath $sdk)) {
        throw "Android SDK not found. Install Android Studio / Platform Tools."
    }
    $adbExe = Join-Path $sdk "platform-tools\adb.exe"
    if (-not (Test-Path -LiteralPath $adbExe)) { throw "adb.exe not found: $adbExe" }
    return $adbExe
}

function Invoke-AdbCapture {
    param([Parameter(Mandatory=$true)][string[]]$AdbArgs)
    $out = & $script:adb @AdbArgs 2>&1
    $code = $LASTEXITCODE
    $text = (($out | ForEach-Object { [string]$_ }) -join "`n").Trim()
    return [pscustomobject]@{ Code = $code; Text = $text }
}

function Show-InstallHelp {
    param([string]$Text)
    Write-Host ""
    Write-Host "ADB installation failed." -ForegroundColor Red
    Write-Host $Text
    Write-Host ""
    if ($Text -match 'INSTALL_FAILED_USER_RESTRICTED|USER_RESTRICTED|canceled by user|cancelled by user') {
        Write-Host "HyperOS/Android blocked USB installation." -ForegroundColor Yellow
        Write-Host "1. Keep the phone unlocked and watch for an Install/Allow confirmation on the phone."
        Write-Host "2. Developer options: USB debugging = ON."
        Write-Host "3. If your HyperOS build has Install via USB / USB debugging (Security settings), enable it."
        Write-Host "4. Reconnect USB, accept the RSA prompt, then run INSTALL_EXISTING_APK.ps1 again."
    } elseif ($Text -match 'INSTALL_FAILED_INSUFFICIENT_STORAGE') {
        Write-Host "Not enough free storage on the phone." -ForegroundColor Yellow
        Write-Host "Free some storage and retry."
    } elseif ($Text -match 'INSTALL_FAILED_VERSION_DOWNGRADE') {
        Write-Host "A newer LyceumMobile is already installed." -ForegroundColor Yellow
        Write-Host "This installer already uses -d. If Android still rejects it, uninstall the newer LyceumMobile first."
    } elseif ($Text -match 'INSTALL_FAILED_OLDER_SDK') {
        Write-Host "The Android version is older than the app minimum SDK." -ForegroundColor Yellow
    } elseif ($Text -match 'INSTALL_FAILED_NO_MATCHING_ABIS') {
        Write-Host "The APK ABI is incompatible with this device." -ForegroundColor Yellow
    } else {
        Write-Host "The complete ADB error is shown above. Copy it if installation still fails." -ForegroundColor Yellow
    }
}

if (-not (Test-Path -LiteralPath $ApkPath)) {
    throw "APK not found: $ApkPath`nRun BUILD_AND_INSTALL.ps1 first, or pass -ApkPath <file>."
}
$ApkPath = (Resolve-Path -LiteralPath $ApkPath).Path
$script:adb = Find-Adb

Write-Host "== ADB device check ==" -ForegroundColor Cyan
& $adb start-server | Out-Null
$deviceLines = & $adb devices -l
Write-Host ($deviceLines -join "`n")

$records = @()
foreach ($line in $deviceLines) {
    if ($line -match '^([^\s]+)\s+(device|unauthorized|offline)(?:\s|$)') {
        $records += [pscustomobject]@{ Serial=$Matches[1]; State=$Matches[2] }
    }
}

$serial = ""
if (-not [string]::IsNullOrWhiteSpace($Device)) {
    $match = @($records | Where-Object { $_.Serial -eq $Device })
    if ($match.Count -eq 0) { throw "Device $Device is not listed by adb." }
    if ($match[0].State -ne 'device') { throw "Device $Device state is $($match[0].State). Unlock phone and accept USB debugging/RSA prompt." }
    $serial = $Device
} else {
    $ready = @($records | Where-Object { $_.State -eq 'device' })
    if ($ready.Count -eq 0) {
        $blocked = @($records | Where-Object { $_.State -ne 'device' })
        if ($blocked.Count -gt 0) { throw "Android device is connected but not authorized/online. Unlock phone and accept the USB debugging RSA prompt." }
        throw "No Android device found. Connect Redmi by USB and enable USB debugging."
    }
    if ($ready.Count -gt 1) { throw "Multiple Android devices are connected. Run: .\INSTALL_EXISTING_APK.ps1 -Device SERIAL" }
    $serial = $ready[0].Serial
}

$prefix = @('-s', $serial)
$model = Invoke-AdbCapture -AdbArgs ($prefix + @('shell','getprop','ro.product.model'))
$android = Invoke-AdbCapture -AdbArgs ($prefix + @('shell','getprop','ro.build.version.release'))
$hyper = Invoke-AdbCapture -AdbArgs ($prefix + @('shell','getprop','ro.mi.os.version.name'))
Write-Host ""
Write-Host "Target device: $serial" -ForegroundColor Green
if ($model.Text) { Write-Host "Model: $($model.Text)" }
if ($android.Text) { Write-Host "Android: $($android.Text)" }
if ($hyper.Text) { Write-Host "HyperOS: $($hyper.Text)" }

$existing = Invoke-AdbCapture -AdbArgs ($prefix + @('shell','pm','path',$PackageName))
if ($existing.Code -eq 0 -and $existing.Text -match '^package:') {
    Write-Host "Existing LyceumMobile installation detected." -ForegroundColor Yellow
    $pkgInfo = Invoke-AdbCapture -AdbArgs ($prefix + @('shell','dumpsys','package',$PackageName))
    $ver = ($pkgInfo.Text -split "`n" | Where-Object { $_ -match 'versionName=|versionCode=' } | Select-Object -First 3) -join ' | '
    if ($ver) { Write-Host $ver.Trim() }
}

function Try-Install {
    param([switch]$NoStreaming)
    $a = $prefix + @('install')
    if ($NoStreaming) { $a += '--no-streaming' }
    $a += @('-r','-d',$ApkPath)
    $r = Invoke-AdbCapture -AdbArgs $a
    if ($r.Text) { Write-Host $r.Text }
    return $r
}

Write-Host ""
Write-Host "== Installing APK ==" -ForegroundColor Cyan
$result = Try-Install
if ($result.Code -ne 0 -and $result.Text -notmatch 'UPDATE_INCOMPATIBLE|signatures do not match|INSTALL_FAILED_USER_RESTRICTED|INSTALL_FAILED_INSUFFICIENT_STORAGE|INSTALL_FAILED_VERSION_DOWNGRADE|INSTALL_FAILED_OLDER_SDK|INSTALL_FAILED_NO_MATCHING_ABIS') {
    Write-Host ""
    Write-Host "Retrying with ADB non-streaming install..." -ForegroundColor Yellow
    $result = Try-Install -NoStreaming
}

if ($result.Code -ne 0 -and $result.Text -match 'INSTALL_FAILED_UPDATE_INCOMPATIBLE|UPDATE_INCOMPATIBLE|signatures do not match|signature.*mismatch') {
    Write-Host ""
    Write-Host "Old LyceumMobile has a different signing key." -ForegroundColor Yellow
    Write-Host "Android cannot update an app when the package name is the same but the signature differs."
    if (-not $ReplaceExisting) {
        Write-Host ""
        Write-Host "To replace it automatically (this removes old LyceumMobile app data/settings), run:" -ForegroundColor Yellow
        Write-Host '.\INSTALL_EXISTING_APK.ps1 -ReplaceExisting'
        exit 20
    }
    Write-Host "Removing old package $PackageName ..." -ForegroundColor Yellow
    $un = Invoke-AdbCapture -AdbArgs ($prefix + @('uninstall',$PackageName))
    if ($un.Text) { Write-Host $un.Text }
    if ($un.Code -ne 0) { throw "Could not uninstall old LyceumMobile." }
    $result = Try-Install -NoStreaming
}

if ($result.Code -ne 0) {
    Show-InstallHelp -Text $result.Text
    exit $result.Code
}

Write-Host ""
Write-Host "INSTALL SUCCESSFUL" -ForegroundColor Green
Write-Host "== Launching LyceumMobile =="
$launch = Invoke-AdbCapture -AdbArgs ($prefix + @('shell','am','start','-n',"$PackageName/.MainActivity"))
if ($launch.Text) { Write-Host $launch.Text }
if ($launch.Code -ne 0) { throw "Application launch failed." }
Write-Host "DONE" -ForegroundColor Green
