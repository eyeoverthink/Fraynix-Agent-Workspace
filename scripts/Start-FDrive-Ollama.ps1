param(
    [Parameter(Mandatory=$true)][string]$ModelStore,
    [ValidateRange(1024,65535)][int]$Port = 11435,
    [string]$LoadTimeout = '15m'
)
$ErrorActionPreference = 'Stop'
$store = (Resolve-Path -LiteralPath $ModelStore).Path
$hasManifests = (Test-Path -LiteralPath (Join-Path $store 'manifests')) -or (Test-Path -LiteralPath (Join-Path $store 'manifests-v2'))
if (!$hasManifests -or !(Test-Path -LiteralPath (Join-Path $store 'blobs'))) {
    throw 'ModelStore must contain an existing Ollama manifests/ or manifests-v2/ directory and a blobs/ directory.'
}
$endpoint = "http://127.0.0.1:$Port"
try {
    $version = Invoke-RestMethod -Uri "$endpoint/api/version" -TimeoutSec 2
    Write-Output "Existing Ollama $($version.version) is available at $endpoint; its configuration was not changed."
    return
} catch { }
if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
    throw "Port $Port is occupied by a service that did not identify as Ollama."
}
$ollama = (Get-Command ollama -ErrorAction Stop).Source
$repo = Split-Path -Parent $PSScriptRoot
$logs = Join-Path $repo 'build\fdrive-ollama'
New-Item -ItemType Directory -Path $logs -Force | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$variables = @{ OLLAMA_HOST=$endpoint; OLLAMA_MODELS=$store; OLLAMA_LOAD_TIMEOUT=$LoadTimeout; OLLAMA_NUM_PARALLEL='1'; OLLAMA_MAX_LOADED_MODELS='1' }
$previous = @{}
try {
    foreach ($name in $variables.Keys) {
        $previous[$name] = [Environment]::GetEnvironmentVariable($name,'Process')
        [Environment]::SetEnvironmentVariable($name,$variables[$name],'Process')
    }
    $process = Start-Process -FilePath $ollama -ArgumentList 'serve' -WorkingDirectory $repo -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logs "$stamp.stdout.log") -RedirectStandardError (Join-Path $logs "$stamp.stderr.log")
} finally {
    foreach ($name in $previous.Keys) { [Environment]::SetEnvironmentVariable($name,$previous[$name],'Process') }
}
$deadline = [DateTime]::UtcNow.AddSeconds(20)
do {
    if ($process.HasExited) { throw "Ollama exited during startup. Check $logs." }
    try {
        $version = Invoke-RestMethod -Uri "$endpoint/api/version" -TimeoutSec 2
        Write-Output "Started Ollama $($version.version) at $endpoint (PID $($process.Id), load timeout $LoadTimeout, model store $store)."
        return
    } catch { Start-Sleep -Milliseconds 250 }
} while ([DateTime]::UtcNow -lt $deadline)
throw "Ollama did not become ready within 20 seconds. Check $logs; the process was left running."
