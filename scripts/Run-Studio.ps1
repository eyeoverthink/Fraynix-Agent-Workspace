param([switch]$Offline, [switch]$BuildOnly)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$gradleArguments = @('--no-daemon', '--console=plain', 'installDist')
if ($Offline) { $gradleArguments += '--offline' }
Push-Location -LiteralPath $repoRoot
try {
    & (Join-Path $repoRoot 'gradlew.bat') @gradleArguments
    if ($LASTEXITCODE -ne 0) { throw "Build failed: $LASTEXITCODE" }
    if (!$BuildOnly) {
        $distribution = Join-Path $repoRoot 'build\install\Fraynix-Studio'
        Push-Location -LiteralPath $distribution
        try {
            & (Join-Path $distribution 'bin\Fraynix-Studio.bat')
            if ($LASTEXITCODE -ne 0) { throw "Studio exited: $LASTEXITCODE" }
        } finally { Pop-Location }
    }
} finally { Pop-Location }
