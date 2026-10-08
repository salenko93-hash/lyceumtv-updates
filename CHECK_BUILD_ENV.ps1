$ErrorActionPreference = "Continue"

Write-Host "== Java =="
java -version

Write-Host "`n== Android SDK =="
$roots = @($env:ANDROID_SDK_ROOT, $env:ANDROID_HOME, "$env:LOCALAPPDATA\Android\Sdk") |
    Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
$roots | ForEach-Object { Write-Host $_ }

Write-Host "`n== compileSdk 34 =="
foreach ($root in $roots) {
    $p = Join-Path $root "platforms\android-34\android.jar"
    if (Test-Path $p) { Write-Host "FOUND: $p" -ForegroundColor Green }
}

Write-Host "`n== Required production files =="
$required = @(
    "app\src\main\assets\schedule_numerator.json",
    "app\src\main\assets\schedule_denominator.json",
    "app\src\main\assets\shelter_numerator.json",
    "app\src\main\assets\shelter_denominator.json",
    "app\src\main\res\raw\minute_silence.mp3",
    "app\src\main\res\raw\trivoga.mp3"
)
foreach ($f in $required) {
    if (Test-Path $f) { Write-Host "FOUND: $f" -ForegroundColor Green }
    else { Write-Host "MISSING: $f" -ForegroundColor Yellow }
}
