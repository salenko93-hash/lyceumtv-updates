param(
    [string]$Tv = "192.168.5.77:5555",
    [switch]$FreshInstall,
    [switch]$BuildOnly
)

$ErrorActionPreference = "Stop"

$project = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $project

$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME

$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"

if (!(Test-Path $adb)) {
    throw "adb.exe not found: $adb"
}

$jdkCandidates = @(
    "$env:USERPROFILE\.jdks\jbr-17.0.14",
    "$env:USERPROFILE\.jdks\jbr-17.0.13",
    "$env:ProgramFiles\Android\Android Studio\jbr"
)

$jdk = $null

foreach ($candidate in $jdkCandidates) {
    if (Test-Path "$candidate\bin\java.exe") {
        $versionText = (& cmd.exe /d /c "`"$candidate\bin\java.exe`" -version 2>&1") | Out-String

        if ($versionText -match '"17\.') {
            $jdk = $candidate
            break
        }
    }
}

if ($null -eq $jdk) {
    throw "JDK 17 not found. Install/use JDK 17 and update BUILD_AND_INSTALL.ps1."
}

$env:JAVA_HOME = $jdk
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Write-Host "JDK 17: $env:JAVA_HOME"

$gradleRoot = Join-Path $project ".gradle-local"
$gradleDir = Join-Path $gradleRoot "gradle-8.2"
$gradleBat = Join-Path $gradleDir "bin\gradle.bat"
$gradleZip = Join-Path $gradleRoot "gradle-8.2-bin.zip"
$gradleUrl = "https://services.gradle.org/distributions/gradle-8.2-bin.zip"

New-Item -ItemType Directory -Force $gradleRoot | Out-Null

function Test-ZipFile([string]$Path) {
    if (!(Test-Path $Path)) {
        return $false
    }

    try {
        Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue
        $archive = [System.IO.Compression.ZipFile]::OpenRead($Path)
        $count = $archive.Entries.Count
        $archive.Dispose()
        return $count -gt 0
    } catch {
        return $false
    }
}

if (!(Test-Path $gradleBat)) {
    if ((Test-Path $gradleZip) -and !(Test-ZipFile $gradleZip)) {
        Write-Host "Removing damaged Gradle ZIP..."
        Remove-Item -Force $gradleZip
    }

    if (!(Test-Path $gradleZip)) {
        Write-Host "Downloading Gradle 8.2..."

        & curl.exe -L --fail --retry 5 --retry-delay 3 `
            -o $gradleZip $gradleUrl

        if ($LASTEXITCODE -ne 0) {
            throw "Gradle download failed."
        }
    }

    if (!(Test-ZipFile $gradleZip)) {
        Remove-Item -Force $gradleZip -ErrorAction SilentlyContinue
        throw "Downloaded Gradle ZIP is damaged. Run BUILD_AND_INSTALL.ps1 again."
    }

    Write-Host "Extracting Gradle..."
    Remove-Item -Recurse -Force $gradleDir -ErrorAction SilentlyContinue
    Expand-Archive -Path $gradleZip -DestinationPath $gradleRoot -Force
}

if (!(Test-Path $gradleBat)) {
    throw "Gradle executable not found after extraction: $gradleBat"
}

Write-Host "== Building Lyceum TV 2.7.0.4 SILENCE MP3 UID 81 ONLY =="

& $gradleBat clean assembleDebug

if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed."
}

$apk = Join-Path $project "app\build\outputs\apk\debug\app-debug.apk"

if (!(Test-Path $apk)) {
    throw "APK not found: $apk"
}

if ($BuildOnly) {
    Write-Host "BUILD ONLY succeeded. APK: $apk"
    Write-Host "No files were installed on the TV."
    exit 0
}

Write-Host "== Connecting to TV $Tv =="

& $adb connect $Tv | Out-Host

$deviceLine = & $adb devices |
    Select-String ([regex]::Escape($Tv))

if ($null -eq $deviceLine -or $deviceLine.ToString() -notmatch "\sdevice$") {
    throw "TV $Tv is not connected/authorized."
}

if ($FreshInstall) {
    Write-Warning "FreshInstall removes LyceumTV local settings, including the encrypted API token."
    & $adb -s $Tv uninstall ua.edu.cunl.tv.debug | Out-Host
}

Write-Host "== Installing APK =="

& $adb -s $Tv install --no-streaming -r $apk | Out-Host

if ($LASTEXITCODE -ne 0) {
    throw "ADB install failed."
}

Write-Host "== Launching LyceumTV =="

& $adb -s $Tv shell am force-stop ua.edu.cunl.tv.debug
Start-Sleep -Milliseconds 600
& $adb -s $Tv shell am start -n ua.edu.cunl.tv.debug/ua.edu.cunl.tv.MainActivity | Out-Host

Write-Host ""
Write-Host "DONE: Lyceum TV 2.7.0.4 SILENCE MP3 UID 81 ONLY installed and launched on $Tv"
Write-Host "Open admin (hold OK) and enter the alerts.in.ua API token."
