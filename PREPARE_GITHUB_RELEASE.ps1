param(
    [Parameter(Mandatory=$true)][string]$Apk,
    [Parameter(Mandatory=$true)][int]$VersionCode,
    [string]$OutputDir = ".\github-release-assets"
)
$ErrorActionPreference = "Stop"
if ($VersionCode -le 53) {
    throw "For installed LyceumTV 2.7.0.13, the upgrade APK must have versionCode greater than 53."
}
if (-not (Test-Path -LiteralPath $Apk)) {
    throw "APK not found: $Apk"
}
New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
$out = Join-Path (Resolve-Path $OutputDir).Path "LyceumTV.apk"
Copy-Item -LiteralPath $Apk -Destination $out -Force
$sha = (Get-FileHash -Algorithm SHA256 -LiteralPath $out).Hash.ToLowerInvariant()
[System.IO.File]::WriteAllText(
    "$out.sha256",
    "$sha  LyceumTV.apk`n",
    [System.Text.Encoding]::ASCII
)
Write-Host "Upload these two files as GitHub Release assets:"
Write-Host $out
Write-Host "$out.sha256"
Write-Host "Add this exact line to release description:"
Write-Host "versionCode=$VersionCode"
Write-Warning "The actual APK package name, signer, and versionCode must match an in-place upgrade."
