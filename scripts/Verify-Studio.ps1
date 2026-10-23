$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $repo
try {
    & (Join-Path $repo 'gradlew.bat') --offline --no-daemon --console=plain check
    if ($LASTEXITCODE -ne 0) { throw "Studio verification failed: $LASTEXITCODE" }
} finally { Pop-Location }
