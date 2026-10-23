param(
    [string]$JavaPath,
    [string]$Model = 'fraynix-3b:latest'
)
$ErrorActionPreference = 'Stop'
if (!$JavaPath) { $JavaPath = (Get-Command java -ErrorAction Stop).Source }
$repo = Split-Path -Parent $PSScriptRoot
$dist = Join-Path $repo 'build\install\Fraynix-Studio'
$workspace = Join-Path $repo 'build\native-scene-agent'
if (!(Test-Path -LiteralPath (Join-Path $dist 'lib'))) { throw 'Run installDist first.' }
$phaseNumber = 0
foreach ($phase in @('propose','undo')) {
    $stdout = Join-Path $repo "build\native-scene-agent-$phase.stdout.log"
    $stderr = Join-Path $repo "build\native-scene-agent-$phase.stderr.log"
    $arguments = @('--enable-native-access=ALL-UNNAMED', ('"-Dfraymus.factoryWorkspace=' + $workspace + '"'),
        ('"-Dfraymus.sceneAgentSmoke=' + $phase + '"'), ('"-Dfraymus.smokeModel=' + $Model + '"'),
        ('"-Dfraymus.studioCapture=' + (Join-Path $repo 'build\native-scene-agent-preview.png') + '"'), '-cp', '"lib/*"', 'Main', '--factory-smoke-exit')
    $process = Start-Process -FilePath $JavaPath -ArgumentList $arguments -WorkingDirectory $dist -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
    $null = $process.Handle
    $deadline = [DateTime]::UtcNow.AddSeconds(200)
    while (!$process.WaitForExit(10000)) {
        if ([DateTime]::UtcNow -gt $deadline) {
            Stop-Process -Id $process.Id -ErrorAction SilentlyContinue
            throw 'Owned scene-agent native test timed out'
        }
    }
    $process.WaitForExit()
    $output = Get-Content -LiteralPath $stdout -Raw
    $expected = if ($phase -eq 'propose') { 'SCENE_AGENT_NATIVE_APPLY_PASSED' } else { 'SCENE_AGENT_NATIVE_UNDO_PASSED' }
    if ($process.ExitCode -ne 0 -or $output -notmatch $expected) {
        Get-Content -LiteralPath $stderr -Tail 30
        throw "Native scene-agent $phase failed (exit=$($process.ExitCode))"
    }
    if ($phase -eq 'undo' -and $output -notmatch 'FACTORY_AUTORESTORE') { throw 'Second process did not automatically restore the applied scene' }
    Write-Output "Native scene-agent $phase passed"
    $phaseNumber++
}
Write-Output 'NATIVE_SCENE_AGENT_PASSED: actual local proposal, detached validation, native apply/save/restore, durable undo after process restart'
