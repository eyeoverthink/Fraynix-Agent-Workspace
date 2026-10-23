param(
    [string]$JavaPath,
    [string]$PrimaryModel = 'fraynix-3b:latest',
    [string]$SecondaryModel = 'fraymus-oracle:latest'
)
$ErrorActionPreference = 'Stop'
if (!$JavaPath) { $JavaPath = (Get-Command java -ErrorAction Stop).Source }
$repo = Split-Path -Parent $PSScriptRoot
$dist = Join-Path $repo 'build\install\Fraynix-Studio'
$workspace = Join-Path $repo 'build\native-studio-smoke'
if (!(Test-Path -LiteralPath (Join-Path $dist 'lib'))) { throw 'Run installDist first.' }
if (!(Test-Path -LiteralPath $JavaPath)) { throw 'Set -JavaPath to a local Java 17+ executable.' }
$identities = @()
$models = @($PrimaryModel, $SecondaryModel)
foreach ($iteration in 0..1) {
    $journal = Join-Path $workspace 'chat\exchanges.jsonl'
    $before = if (Test-Path -LiteralPath $journal) { @(Get-Content -LiteralPath $journal | Where-Object { $_.Trim() }).Count } else { 0 }
    $stdout = Join-Path $repo "build\native-studio-$iteration.stdout.log"
    $stderr = Join-Path $repo "build\native-studio-$iteration.stderr.log"
    $arguments = @('--enable-native-access=ALL-UNNAMED', ('"-Dfraymus.factoryWorkspace=' + $workspace + '"'),
        '-Dfraymus.studioSmoke=true', ('"-Dfraymus.smokeModel=' + $models[$iteration] + '"'),
        ('"-Dfraymus.studioCapture=' + (Join-Path $repo "build\native-studio-$iteration.png") + '"'),
        '-cp', '"lib/*"', 'Main', '--factory-smoke-exit')
    $process = Start-Process -FilePath $JavaPath -ArgumentList $arguments -WorkingDirectory $dist -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
    $null = $process.Handle
    $deadline = [DateTime]::UtcNow.AddSeconds(200)
    while (!$process.WaitForExit(10000)) {
        if ([DateTime]::UtcNow -gt $deadline) {
            Stop-Process -Id $process.Id -ErrorAction SilentlyContinue
            throw 'Owned native studio test timed out'
        }
    }
    $process.WaitForExit()
    $output = Get-Content -LiteralPath $stdout -Raw
    if ($process.ExitCode -ne 0 -or $output -notmatch 'STUDIO_UI_SMOKE_PASSED') {
        Get-Content -LiteralPath $stderr -Tail 25
        throw "Native studio run $iteration failed (exit=$($process.ExitCode))"
    }
    if ($output -notmatch 'tabs=\[Chat, Create, Models, Kernel, Tools, Jobs\].*skill=world.inspect skillJournalIndex=\d+') {
        throw 'Native tools tab or journaled live-world inspection was not verified'
    }
    if ($output -notmatch 'FACTORY_UI_SMOKE_PASSED.*identitySet=([^\r\n]+)') { throw 'Missing scene identity verification' }
    $identities += $Matches[1]
    $records = @(Get-Content -LiteralPath $journal | Where-Object { $_.Trim() } | ForEach-Object { $_ | ConvertFrom-Json })
    if ($records.Count -ne $before + 1 -or !$records[-1].success -or $records[-1].model -ne $models[$iteration]) { throw 'Completed chat not persisted exactly once' }
    if ($iteration -eq 1 -and $output -notmatch 'FACTORY_AUTORESTORE') { throw 'Second run did not restore the scene' }
    Write-Output "Native studio run $iteration passed: $($models[$iteration]) -> $($records[-1].endpoint); saved exchanges=$($records.Count)"
}
if ($identities[0] -ne $identities[1]) { throw 'Persistent identities changed across restart' }
Write-Output 'NATIVE_STUDIO_RESTART_PASSED: all Fraynix tabs, actual local inference, chat persistence, stable scene and physics'
