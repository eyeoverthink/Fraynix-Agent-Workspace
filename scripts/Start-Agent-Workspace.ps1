param([switch]$PrepareOnly,[switch]$Offline)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
if (!(Get-Command node.exe -ErrorAction SilentlyContinue) -or !(Get-Command java.exe -ErrorAction SilentlyContinue)) {
    throw 'Install Node.js LTS and JDK 21, then run this launcher again.'
}
Push-Location -LiteralPath $repo
$taskPreviousOffline=$env:FRAYNIX_WORKSPACE_OFFLINE
try {
    if ($Offline) { $env:FRAYNIX_WORKSPACE_OFFLINE='1' }
    # A closed interface may still have a live Java owner. Never rebuild its loaded installation.
    $taskOwnerLive042=$false
    $taskProject042=if($env:FRAYNIX_WORKSPACE_PROJECT){[IO.Path]::GetFullPath($env:FRAYNIX_WORKSPACE_PROJECT)}else{Join-Path ([Environment]::GetFolderPath('UserProfile')) '.fraynix-agent-workspace/projects/default'}
    $taskDescriptor042=Join-Path $taskProject042 'runtime-042/host.json'
    if(Test-Path -LiteralPath $taskDescriptor042){
        if((Get-Item -LiteralPath $taskDescriptor042).Length -gt 16384){throw 'Java owner record needs inspection; no build attempted.'}
        $taskOwner042=[IO.File]::ReadAllText($taskDescriptor042)|ConvertFrom-Json
        if($taskOwner042.protocol -ne 'fraynix-agent-host-042-v1' -or $taskOwner042.origin -notmatch '^http://127\.0\.0\.1:[1-9][0-9]{0,4}$' -or $taskOwner042.token -notmatch '^[a-f0-9]{64}$' -or [IO.Path]::GetFullPath($taskOwner042.project) -ne $taskProject042){throw 'Java owner record does not match this project; no build attempted.'}
        try{$taskState042=Invoke-RestMethod -Uri ($taskOwner042.origin+'/api/agent042/state') -Method Post -Headers @{Authorization=('Bearer '+$taskOwner042.token)} -ContentType 'application/json' -Body '{}' -TimeoutSec 3;if($taskState042.bootId -ne $taskOwner042.bootId -or $taskState042.agentId -ne $taskOwner042.agentId -or $taskState042.pid -ne $taskOwner042.pid){throw 'Owner identity mismatch'};$taskOwnerLive042=$true}
        catch{if(Get-Process -Id $taskOwner042.pid -ErrorAction SilentlyContinue){throw 'The Java owner is alive but could not be verified. Stop it through its app before building; no build attempted.'}}
    }
    if($taskOwnerLive042 -and $PrepareOnly){throw 'Java is still running. Use Stop Java & close app before preparing an updated build; no build attempted.'}
    if($taskOwnerLive042){Write-Host 'Reconnecting to the continuing Java agent. Its running installation will not be rebuilt.'}
    else {
    # Incremental build refreshes an existing installation after Git updates.
    $taskBuildArgs=@('prepareGameRuntime115',':workspace-engine:installDist'); if ($Offline) { $taskBuildArgs+='--offline' }
    & (Join-Path $repo 'gradlew.bat') @taskBuildArgs
    if ($LASTEXITCODE -ne 0) { throw 'Workspace engine build failed; see the Gradle diagnostics above.' }

    }
    $desktop = Join-Path $repo 'workspace-desktop'
    Push-Location -LiteralPath $desktop
    try {
        $lockHash = (Get-FileHash -LiteralPath 'package-lock.json' -Algorithm SHA256).Hash
        $stamp = Join-Path $desktop 'node_modules/.fraynix-lock-sha256'
        $previous = if (Test-Path -LiteralPath $stamp) { [IO.File]::ReadAllText($stamp).Trim() } else { '' }
        & node.exe (Join-Path $repo 'scripts/Verify-Desktop-Cache.cjs') $desktop
        $taskCacheValid=$LASTEXITCODE -eq 0
        if ($previous -ne $lockHash -or !$taskCacheValid) {
            # A missing stamp must not replace a valid installation used by an open app.
            if (!$taskCacheValid) {
                if ($Offline) { throw 'Offline setup needs matching cached desktop dependencies. Run once without -Offline to prepare them; no package download was attempted.' }
                $taskElectronBinary=Join-Path $desktop 'node_modules/electron/dist/electron.exe'
                $taskRunning=Get-Process electron -ErrorAction SilentlyContinue | Where-Object { $_.Path -eq $taskElectronBinary }
                if ($taskRunning) { throw 'Desktop dependencies need preparation, but this Workspace is still running. Close it before setup; no dependency replacement was attempted.' }
                & npm.cmd ci --no-audit --no-fund
                if ($LASTEXITCODE -ne 0) { throw 'Desktop setup failed; inspect the npm diagnostics.' }
                & node.exe (Join-Path $repo 'scripts/Verify-Desktop-Cache.cjs') $desktop
                if ($LASTEXITCODE -ne 0) { throw 'Installed desktop dependencies could not be verified.' }
            }
            [IO.File]::WriteAllText($stamp, $lockHash, [Text.UTF8Encoding]::new($false))
        }
        if (!$PrepareOnly) {
            & node.exe 'node_modules/electron/cli.js' '.'
            if ($LASTEXITCODE -ne 0) { throw "Workspace exited with code $LASTEXITCODE." }
        }
    } finally { Pop-Location }
} finally { $env:FRAYNIX_WORKSPACE_OFFLINE=$taskPreviousOffline; Pop-Location }
