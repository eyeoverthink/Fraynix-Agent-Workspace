param([string]$JavaPath)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$dist = Join-Path $repo 'build\install\Fraynix-Studio'
if (!(Test-Path -LiteralPath (Join-Path $dist 'lib'))) { throw 'Run installDist first.' }
$java = if ($JavaPath) { $JavaPath } else { (Get-Command java -ErrorAction Stop).Source }
$workspace = Join-Path $repo 'build\native-factory-smoke'
$classpath = Join-Path $dist 'lib\*'
$identities = @()
for ($iteration = 1; $iteration -le 2; $iteration++) {
    $stdout = Join-Path $repo "build\native-factory-$iteration.stdout.log"
    $stderr = Join-Path $repo "build\native-factory-$iteration.stderr.log"
    $arguments = @('--enable-native-access=ALL-UNNAMED','-ea',('"-Dfraymus.factoryWorkspace=' + $workspace + '"'),'-cp',('"' + $classpath + '"'),'Main','--factory-smoke-exit')
    $process = Start-Process -FilePath $java -ArgumentList $arguments -WorkingDirectory $dist -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
    # Retain the OS handle before exit so Windows PowerShell can retrieve the exit code.
    $null = $process.Handle
    if (!$process.WaitForExit(45000)) {
        Stop-Process -Id $process.Id -ErrorAction SilentlyContinue
        throw "Native smoke run $iteration timed out"
    }
    $process.WaitForExit()
    $output = Get-Content -LiteralPath $stdout -Raw
    if ($process.ExitCode -ne 0 -or $output -notmatch 'FACTORY_UI_SMOKE_PASSED.*identitySet=([^\r\n]+)') {
        Get-Content -LiteralPath $stderr -Tail 20
        throw "Native smoke run $iteration failed (exit=$($process.ExitCode))"
    }
    $identities += $Matches[1]
    if ($output -notmatch 'FACTORY_PHYSICS_INSPECTOR_RENDERED') { throw 'Native physics inspector did not render after restore' }
    if ($iteration -eq 2 -and $output -notmatch 'FACTORY_AUTORESTORE') { throw 'Second launch did not automatically restore' }
    Write-Output "Native editor run $iteration passed"
}
if ($identities[0] -ne $identities[1]) { throw 'Persistent scene identities changed across native process restart' }
[ordered]@{NativeRuns=2;AutomaticRestore=$true;StableIdentities=$true;IdentitySet=$identities[0];Workspace=$workspace} |
    ConvertTo-Json | Set-Content -LiteralPath (Join-Path $repo 'build\native-factory-result.json')
Write-Output 'NATIVE_FACTORY_RESTART_PASSED'
