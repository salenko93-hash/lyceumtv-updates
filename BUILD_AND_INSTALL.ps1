$ErrorActionPreference = "Stop"
$Project = $PSScriptRoot

# Parse optional arguments manually. This deliberately avoids a PowerShell
# [switch] parameter-binding edge case when a trailing slash is accidentally
# added after the script name. Supported: -BuildOnly and -Device SERIAL.
$BuildOnly = $false
$Device = ""
$ReplaceExisting = $false
for ($i = 0; $i -lt $args.Count; $i++) {
    $arg = [string]$args[$i]
    if ([string]::IsNullOrWhiteSpace($arg) -or $arg -eq "\") {
        continue
    }
    if ($arg -ieq "-BuildOnly") {
        $BuildOnly = $true
        continue
    }
    if ($arg -ieq "-Device" -and ($i + 1) -lt $args.Count) {
        $i++
        $Device = [string]$args[$i]
        continue
    }
    if ($arg -ieq "-ReplaceExisting") {
        $ReplaceExisting = $true
        continue
    }
}

function Get-JavaMajorVersion {
    param([Parameter(Mandatory=$true)][string]$JavaExe)

    if (-not (Test-Path -LiteralPath $JavaExe)) { return $null }

    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $JavaExe
    $psi.Arguments = "-version"
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.CreateNoWindow = $true

    $proc = New-Object System.Diagnostics.Process
    $proc.StartInfo = $psi
    try {
        [void]$proc.Start()
        $stdout = $proc.StandardOutput.ReadToEnd()
        $stderr = $proc.StandardError.ReadToEnd()
        $proc.WaitForExit()
    } catch {
        return $null
    } finally {
        if ($proc) { $proc.Dispose() }
    }

    $text = "$stdout`n$stderr"
    if ($text -match 'version\s+"(?<major>\d+)(?:\.|")') { return [int]$Matches['major'] }
    if ($text -match 'openjdk\s+(?<major>\d+)(?:\.|\s)') { return [int]$Matches['major'] }
    return $null
}

function Find-Jdk17 {
    $patterns = New-Object System.Collections.Generic.List[string]

    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) { $patterns.Add($env:JAVA_HOME) }
    $patterns.Add("$env:ProgramFiles\Microsoft\jdk-17*")
    $patterns.Add("$env:ProgramFiles\Eclipse Adoptium\jdk-17*")
    $patterns.Add("$env:ProgramFiles\Java\jdk-17*")
    $patterns.Add("$env:ProgramFiles\Zulu\zulu-17*")
    $patterns.Add("$env:ProgramFiles\Amazon Corretto\jdk17*")
    $patterns.Add("$env:LOCALAPPDATA\Programs\Eclipse Adoptium\jdk-17*")
    $patterns.Add("$env:LOCALAPPDATA\Programs\Microsoft\jdk-17*")
    $patterns.Add("$env:ProgramFiles\Android\Android Studio\jbr")

    foreach ($pattern in $patterns) {
        $matches = @(Get-Item -Path $pattern -ErrorAction SilentlyContinue)
        foreach ($item in ($matches | Sort-Object FullName -Descending)) {
            $jdkHome = $item.FullName
            $java = Join-Path $jdkHome "bin\java.exe"
            if ((Get-JavaMajorVersion -JavaExe $java) -eq 17) { return $jdkHome }
        }
    }
    return $null
}

$selectedJavaHome = Find-Jdk17
if (-not $selectedJavaHome) {
    Write-Host ""
    Write-Host "JDK 17 не знайдено." -ForegroundColor Red
    Write-Host "Встановіть Microsoft OpenJDK 17 командою:" -ForegroundColor Yellow
    Write-Host 'winget install --id Microsoft.OpenJDK.17 -e --source winget'
    Write-Host "Потім закрийте PowerShell, відкрийте знову і запустіть:"
    Write-Host '.\BUILD_AND_INSTALL.ps1'
    throw "JDK 17 required."
}

$env:JAVA_HOME = $selectedJavaHome
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$javaExe = Join-Path $env:JAVA_HOME "bin\java.exe"
$wrapperJar = Join-Path $Project "gradle\wrapper\gradle-wrapper.jar"

Write-Host "== JDK 17 selected ==" -ForegroundColor Green
Write-Host "JAVA_HOME=$env:JAVA_HOME"

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $javaExe
$psi.Arguments = "-version"
$psi.UseShellExecute = $false
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.CreateNoWindow = $true
$proc = New-Object System.Diagnostics.Process
$proc.StartInfo = $psi
[void]$proc.Start()
$javaOut = $proc.StandardOutput.ReadToEnd()
$javaErr = $proc.StandardError.ReadToEnd()
$proc.WaitForExit()
$proc.Dispose()
Write-Host ($javaOut + $javaErr).Trim()

if ((Get-JavaMajorVersion -JavaExe $javaExe) -ne 17) {
    throw "Internal check failed: selected Java is not JDK 17."
}
if (-not (Test-Path -LiteralPath $wrapperJar)) {
    throw "Gradle wrapper JAR not found: $wrapperJar"
}

Write-Host ""
Write-Host "== Gradle wrapper self-test =="
& $javaExe -cp $wrapperJar org.gradle.wrapper.GradleWrapperMain --wrapper-self-test
if ($LASTEXITCODE -ne 0) { throw "Gradle wrapper self-test failed." }

Write-Host ""
Write-Host "== Building tests + debug APK =="
# Run the wrapper directly through Java. This avoids cmd.exe/BAT encoding issues.
& $javaExe -cp $wrapperJar org.gradle.wrapper.GradleWrapperMain --no-daemon clean test :app:assembleDebug
if ($LASTEXITCODE -ne 0) { throw "Gradle build failed." }

$apk = Join-Path $Project "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path -LiteralPath $apk)) { throw "APK не знайдено: $apk" }

Write-Host ""
Write-Host "BUILD SUCCESSFUL" -ForegroundColor Green
Write-Host "APK: $apk"
if ($BuildOnly) { exit 0 }

$installer = Join-Path $Project "INSTALL_EXISTING_APK.ps1"
if (-not (Test-Path -LiteralPath $installer)) { throw "Installer helper not found: $installer" }

$installArgs = @('-ApkPath', $apk)
if (-not [string]::IsNullOrWhiteSpace($Device)) { $installArgs += @('-Device', $Device) }
if ($ReplaceExisting) { $installArgs += '-ReplaceExisting' }

& $installer @installArgs
$installCode = $LASTEXITCODE
if ($installCode -ne 0) {
    if ($installCode -eq 20) {
        Write-Host ""
        Write-Host "Build succeeded. Installation needs old-signature replacement; see command above." -ForegroundColor Yellow
    }
    exit $installCode
}
