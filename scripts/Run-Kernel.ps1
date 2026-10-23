param(
    [switch]$Offline,
    [Parameter(ValueFromRemainingArguments=$true)][string[]]$KernelArguments
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
if (!$KernelArguments) { $KernelArguments = @('status', $repo) }
$buildArguments = @('--no-daemon','--console=plain','installDist')
if ($Offline) { $buildArguments += '--offline' }
Push-Location -LiteralPath $repo
try {
    & (Join-Path $repo 'gradlew.bat') @buildArguments
    if ($LASTEXITCODE -ne 0) { throw "Kernel build failed: $LASTEXITCODE" }
    $libraries = Join-Path $repo 'build\install\Fraynix-Studio\lib\*'
    & java -cp $libraries brain_architecture.kernel.OfflineKernelCli @KernelArguments
    if ($LASTEXITCODE -ne 0) { throw "Kernel command failed: $LASTEXITCODE" }
} finally { Pop-Location }
