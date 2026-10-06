param()
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$obsolete = @(
    'app\src\main\java\ua\edu\cunl\tv\audio\MinuteSilenceMetronome.java',
    'app\src\main\res\raw\metronome_tick.wav',
    'tests\MetronomeContractTest.java'
)
foreach ($rel in $obsolete) {
    $f = Join-Path $root $rel
    if (Test-Path -LiteralPath $f) {
        Remove-Item -LiteralPath $f -Force
        Write-Host "Removed obsolete: $rel"
    }
}
$oldStubs = Join-Path $root 'tests\metronome_stubs'
if (Test-Path -LiteralPath $oldStubs) {
    Remove-Item -LiteralPath $oldStubs -Recurse -Force
    Write-Host 'Removed obsolete metronome test stubs'
}
Write-Host 'HOTFIX 2.7.0.4 prepared. Next: .\BUILD_AND_INSTALL.ps1 -BuildOnly'
