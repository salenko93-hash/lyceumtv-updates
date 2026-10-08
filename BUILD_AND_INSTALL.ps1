param(
    [string]$Tv = "192.168.5.77:5555",
    [switch]$FreshInstall,
    [switch]$ConfirmDataLoss,
    [switch]$BuildOnly
)

$ErrorActionPreference = "Stop"
$project = $PSScriptRoot
Set-Location $project

if ($FreshInstall -and -not $ConfirmDataLoss) {
    throw "-FreshInstall erases app settings/token. Re-run with -ConfirmDataLoss only if this is intentional."
}

function Find-AndroidSdk {
    $candidates = @($env:ANDROID_SDK_ROOT, $env:ANDROID_HOME, "$env:LOCALAPPDATA\Android\Sdk") |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    foreach ($candidate in $candidates) {
        if (Test-Path (Join-Path $candidate "platform-tools\adb.exe")) { return $candidate }
    }
    throw "Android SDK/platform-tools not found."
}

function Test-Jdk17([string]$JdkHome) {
    if ([string]::IsNullOrWhiteSpace($JdkHome)) { return $false }
    $java = Join-Path $JdkHome "bin\java.exe"
    if (!(Test-Path $java)) { return $false }
    $txt = (& cmd.exe /d /c "`"$java`" -version 2>&1") | Out-String
    return $txt -match '"17(\.|")'
}

function Find-Jdk17 {
    $candidates = @(
        $env:JAVA_HOME,
        "$env:ProgramFiles\Android\Android Studio\jbr",
        "$env:ProgramFiles\Java\jdk-17",
        "$env:ProgramFiles\Eclipse Adoptium\jdk-17*"
    )
    foreach ($candidate in $candidates) {
        if ([string]::IsNullOrWhiteSpace($candidate)) { continue }
        foreach ($item in @(Get-Item $candidate -ErrorAction SilentlyContinue | Sort-Object FullName -Descending)) {
            if ($item -and (Test-Jdk17 $item.FullName)) { return $item.FullName }
        }
        if ((Test-Path $candidate) -and (Test-Jdk17 $candidate)) { return $candidate }
    }
    throw "JDK 17 not found."
}

$env:ANDROID_HOME = Find-AndroidSdk
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:JAVA_HOME = Find-Jdk17
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$adb = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"

$gradleVersion = "8.2"
$local = Join-Path $project ".gradle-local"
$gradleDir = Join-Path $local "gradle-$gradleVersion"
$gradleBat = Join-Path $gradleDir "bin\gradle.bat"
$zip = Join-Path $local "gradle-$gradleVersion-bin.zip"
$url = "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip"
New-Item -ItemType Directory -Force $local | Out-Null

function Test-Zip([string]$Path) {
    if (!(Test-Path $Path)) { return $false }
    try {
        Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue
        $z = [System.IO.Compression.ZipFile]::OpenRead($Path)
        $ok = $z.Entries.Count -gt 0
        $z.Dispose()
        return $ok
    } catch { return $false }
}

if (!(Test-Path $gradleBat)) {
    if ((Test-Path $zip) -and !(Test-Zip $zip)) { Remove-Item -Force $zip }
    if (!(Test-Path $zip)) {
        & curl.exe -L --fail --retry 5 --retry-delay 3 -o $zip $url
        if ($LASTEXITCODE -ne 0) { throw "Gradle download failed." }
    }
    if (!(Test-Zip $zip)) { throw "Downloaded Gradle ZIP is damaged." }
    Expand-Archive -Path $zip -DestinationPath $local -Force
}

Write-Host "== Building LyceumTV 2.7.0.14 APPROVED TV PAGES =="
& $gradleBat --no-daemon clean assembleDebug
if ($LASTEXITCODE -ne 0) { throw "Gradle build failed." }

$apk = Join-Path $project "app\build\outputs\apk\debug\app-debug.apk"
if (!(Test-Path $apk)) { throw "APK not found: $apk" }
Write-Host "BUILD SUCCESSFUL"
Write-Host "APK: $apk"

if ($BuildOnly) { exit 0 }

function Connect-TV {
    & $adb connect $Tv | Out-Host
    Start-Sleep -Milliseconds 600
    $line = & $adb devices | Select-String ("^" + [regex]::Escape($Tv) + "\s+device$")
    if ($null -eq $line) { throw "TV $Tv not connected/authorized." }
}

Connect-TV
if ($FreshInstall) {
    Write-Warning "Removing installed app and all local data."
    & $adb -s $Tv uninstall ua.edu.cunl.tv.debug | Out-Host
    Connect-TV
}

$remote = "/data/local/tmp/lyceumtv_2709_pc_token.apk"
for ($i=1; $i -le 3; $i++) {
    & $adb -s $Tv push $apk $remote | Out-Host
    if ($LASTEXITCODE -eq 0) { break }
    if ($i -eq 3) { throw "ADB push failed." }
    & $adb disconnect $Tv | Out-Null
    Start-Sleep -Seconds 1
    Connect-TV
}
Connect-TV
& $adb -s $Tv shell pm install -r -t $remote | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw "pm install failed. Do not uninstall the old app if you need to preserve data/signing compatibility."
}
& $adb -s $Tv shell rm -f $remote | Out-Null
& $adb -s $Tv shell dumpsys package ua.edu.cunl.tv.debug |
    Select-String "versionCode=|versionName=" | Out-Host
& $adb -s $Tv shell am force-stop ua.edu.cunl.tv.debug
& $adb -s $Tv shell am start -n "ua.edu.cunl.tv.debug/ua.edu.cunl.tv.MainActivity" | Out-Host
