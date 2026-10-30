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
    # Incremental build refreshes an existing installation after Git updates.
    $taskBuildArgs=@(':workspace-engine:installDist'); if ($Offline) { $taskBuildArgs+='--offline' }
    & (Join-Path $repo 'gradlew.bat') @taskBuildArgs
    if ($LASTEXITCODE -ne 0) { throw 'Workspace engine build failed; see the Gradle diagnostics above.' }
    if (!(Test-Path -LiteralPath (Join-Path $repo 'build/install/Fraynix-Studio/lib'))) {
        $taskGameArgs=@('installDist'); if ($Offline) { $taskGameArgs+='--offline' }
        & (Join-Path $repo 'gradlew.bat') @taskGameArgs
        if ($LASTEXITCODE -ne 0) { throw 'Game Lab preparation failed.' }
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
