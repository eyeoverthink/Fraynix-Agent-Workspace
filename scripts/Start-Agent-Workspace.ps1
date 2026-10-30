param([switch]$PrepareOnly)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
if (!(Get-Command node.exe -ErrorAction SilentlyContinue) -or !(Get-Command java.exe -ErrorAction SilentlyContinue)) {
    throw 'Install Node.js LTS and JDK 21, then run this launcher again.'
}
Push-Location -LiteralPath $repo
try {
    # Incremental build refreshes an existing installation after Git updates.
    & (Join-Path $repo 'gradlew.bat') :workspace-engine:installDist
    if ($LASTEXITCODE -ne 0) { throw 'Workspace engine build failed; see the Gradle diagnostics above.' }
    if (!(Test-Path -LiteralPath (Join-Path $repo 'build/install/Fraynix-Studio/lib'))) {
        & (Join-Path $repo 'gradlew.bat') installDist
        if ($LASTEXITCODE -ne 0) { throw 'Game Lab preparation failed.' }
    }
    $desktop = Join-Path $repo 'workspace-desktop'
    Push-Location -LiteralPath $desktop
    try {
        $lockHash = (Get-FileHash -LiteralPath 'package-lock.json' -Algorithm SHA256).Hash
        $stamp = Join-Path $desktop 'node_modules/.fraynix-lock-sha256'
        $previous = if (Test-Path -LiteralPath $stamp) { [IO.File]::ReadAllText($stamp).Trim() } else { '' }
        if ($previous -ne $lockHash -or !(Test-Path -LiteralPath 'node_modules/electron/cli.js') -or !(Test-Path -LiteralPath 'node_modules/three/package.json')) {
            & npm.cmd ci --no-audit --no-fund
            if ($LASTEXITCODE -ne 0) { throw 'Desktop setup failed. Close an existing workspace if its dependency files are locked, then retry.' }
            [IO.File]::WriteAllText($stamp, $lockHash, [Text.UTF8Encoding]::new($false))
        }
        if (!$PrepareOnly) {
            & node.exe 'node_modules/electron/cli.js' '.'
            if ($LASTEXITCODE -ne 0) { throw "Workspace exited with code $LASTEXITCODE." }
        }
    } finally { Pop-Location }
} finally { Pop-Location }
