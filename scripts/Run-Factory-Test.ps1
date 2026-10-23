param([switch]$BuildOnly, [switch]$Bundled)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$arguments = @('--offline','--no-daemon','--console=plain',':check',':runFactoryDemo',':installDist')
if ($Bundled) { $arguments += '-PfraynixRoot=bundled' }
Push-Location -LiteralPath $repo
try {
    & (Join-Path $repo 'gradlew.bat') @arguments
    if ($LASTEXITCODE -ne 0) { throw "Factory verification failed: $LASTEXITCODE" }
    if (!$BuildOnly) {
        $dist = Join-Path $repo 'build\install\Fraynix-Studio'
        & (Join-Path $dist 'Launch-Fraynix-Studio.bat')
        if ($LASTEXITCODE -ne 0) { throw "Editor exited: $LASTEXITCODE" }
    }
} finally { Pop-Location }
