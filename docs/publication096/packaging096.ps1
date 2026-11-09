$ErrorActionPreference='Stop'
Set-Location 'D:/Codex-Work/fraynix-workspace-edition096'
if (-not (Get-Content build/coverage096.json -Raw | ConvertFrom-Json).passed) { throw 'Method coverage must be verified before packaging' }
$env:JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot'
$env:PATH="$env:JAVA_HOME/bin;$env:PATH"
$env:JAVA_TOOL_OPTIONS='-XX:ActiveProcessorCount=4 -Xmx768m'
$source096=(git rev-parse HEAD)
@{source=$source096;startedAt=(Get-Date -Format o);command='gradlew :verifySourceIsolation :workspace-engine:installDist :distZip --offline --no-daemon --max-workers=1 --console=plain --rerun-tasks'} | ConvertTo-Json | Set-Content build/packaging096-run.json -Encoding utf8
& ./gradlew.bat :verifySourceIsolation :workspace-engine:installDist :distZip --offline --no-daemon --max-workers=1 --console=plain --rerun-tasks *> build/packaging096.log
$exit096=$LASTEXITCODE
@{source=$source096;finishedAt=(Get-Date -Format o);exit=$exit096;scope='Separate source-isolation, installed engine and complete archive packaging after corrected method verification.'} | ConvertTo-Json | Set-Content build/packaging096-finished.json -Encoding utf8
Get-Content build/packaging096.log -Tail 25
exit $exit096
