param(
    [string]$Tv = "192.168.5.77:5555"
)

$ErrorActionPreference = "Stop"
$Package = "ua.edu.cunl.tv.debug"
$Activity = "ua.edu.cunl.tv.MainActivity"
$Pending = "files/pending_alerts_token.txt"

function Find-AndroidSdk {
    $candidates = @(
        $env:ANDROID_SDK_ROOT,
        $env:ANDROID_HOME,
        "$env:LOCALAPPDATA\Android\Sdk"
    ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }

    foreach ($candidate in $candidates) {
        $candidateAdb = Join-Path $candidate "platform-tools\adb.exe"
        if (Test-Path $candidateAdb) { return $candidate }
    }
    throw "Android SDK/platform-tools/adb.exe not found."
}

function Invoke-RemoteShell {
    param(
        [Parameter(Mandatory=$true)][string]$Command
    )
    $output = & $adb -s $Tv shell $Command 2>&1
    $code = $LASTEXITCODE
    return [pscustomobject]@{
        ExitCode = $code
        Output = ($output -join "`n")
    }
}

function Invoke-RemoteShellWithStdin {
    param(
        [Parameter(Mandatory=$true)][string]$Command,
        [Parameter(Mandatory=$true)][string]$InputText
    )

    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $adb
    $psi.Arguments = "-s `"$Tv`" shell `"$Command`""
    $psi.UseShellExecute = $false
    $psi.RedirectStandardInput = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.CreateNoWindow = $true

    $proc = New-Object System.Diagnostics.Process
    $proc.StartInfo = $psi

    if (-not $proc.Start()) {
        throw "Unable to start adb."
    }

    # Write() deliberately adds no newline. The token is not placed in adb arguments.
    $proc.StandardInput.Write($InputText)
    $proc.StandardInput.Close()

    $stdout = $proc.StandardOutput.ReadToEnd()
    $stderr = $proc.StandardError.ReadToEnd()
    $proc.WaitForExit()

    return [pscustomobject]@{
        ExitCode = $proc.ExitCode
        StdOut = $stdout
        StdErr = $stderr
    }
}

$Sdk = Find-AndroidSdk
$adb = Join-Path $Sdk "platform-tools\adb.exe"

Write-Host "Connecting to TV $Tv ..."
& $adb connect $Tv | Out-Host
if ($LASTEXITCODE -ne 0) { throw "adb connect failed." }

$device = & $adb devices | Select-String ("^" + [regex]::Escape($Tv) + "\s+device$")
if ($null -eq $device) {
    throw "TV $Tv is not connected/authorized."
}

# Verify run-as independently. The APK must be the debuggable package.
$probe = Invoke-RemoteShell "run-as $Package sh -c 'id >/dev/null && mkdir -p files'"
if ($probe.ExitCode -ne 0) {
    Write-Host $probe.Output
    throw "run-as failed. Install the debug APK first and verify package $Package."
}

$secure = Read-Host "Введіть alerts.in.ua API token" -AsSecureString
$bstr = [IntPtr]::Zero
$plain = $null

try {
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)

    if ([string]::IsNullOrWhiteSpace($plain)) {
        throw "Token is empty."
    }
    if ([Text.Encoding]::UTF8.GetByteCount($plain) -gt 4000) {
        throw "Token is unexpectedly long."
    }

    # IMPORTANT: all redirection happens inside run-as on the TV.
    $remoteWrite = "run-as $Package sh -c 'umask 077; mkdir -p files; cat > $Pending; chmod 600 $Pending'"
    $write = Invoke-RemoteShellWithStdin -Command $remoteWrite -InputText $plain

    if ($write.ExitCode -ne 0) {
        if (-not [string]::IsNullOrWhiteSpace($write.StdErr)) {
            Write-Host $write.StdErr
        }
        throw "Unable to write the private token hand-off file."
    }

    $check = Invoke-RemoteShell "run-as $Package sh -c 'test -s $Pending && echo PRESENT || echo MISSING'"
    if (($check.ExitCode -ne 0) -or ($check.Output -notmatch "PRESENT")) {
        Write-Host $check.Output
        throw "Token hand-off file was not created in the app sandbox."
    }
}
finally {
    $plain = $null
    if ($bstr -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

Write-Host "Restarting LyceumTV so it can import and encrypt the token ..."
& $adb -s $Tv shell am force-stop $Package | Out-Null
& $adb -s $Tv shell am start -n "$Package/$Activity" | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw "Unable to start LyceumTV."
}

Start-Sleep -Seconds 3

$pendingCheck = Invoke-RemoteShell "run-as $Package sh -c 'if [ -f $Pending ]; then echo PRESENT; else echo DELETED; fi'"
$securePrefs = Invoke-RemoteShell "run-as $Package sh -c 'if [ -f shared_prefs/alerts_token_secure_v1.xml ]; then cat shared_prefs/alerts_token_secure_v1.xml; fi'"

if (($pendingCheck.Output -match "DELETED") -and
    ($securePrefs.Output -match 'name="iv"') -and
    ($securePrefs.Output -match 'name="data"')) {

    Write-Host "OK: token imported. Plaintext hand-off deleted; encrypted token is stored by Android Keystore." -ForegroundColor Green
    Write-Host "LyceumTV can now query alerts.in.ua." -ForegroundColor Green
} else {
    Write-Warning "Token import could not be confirmed automatically."
    Write-Host "Pending status: $($pendingCheck.Output)"
    Write-Host "Collect log with:"
    Write-Host "& `"$adb`" -s `"$Tv`" logcat -d | Select-String `"LyceumTokenSetup|AndroidRuntime`""
}
