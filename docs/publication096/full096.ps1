$ErrorActionPreference='Stop'
Set-Location 'D:/Codex-Work/fraynix-workspace-edition096'
$env:JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot'
$env:PATH="$env:JAVA_HOME/bin;$env:PATH"
$env:JAVA_TOOL_OPTIONS='-XX:ActiveProcessorCount=4 -Xmx768m'
Get-ChildItem Env: | Where-Object {$_.Name -match '^FRAYNIX.*(RESUME|PROJECT)$'} | ForEach-Object {Remove-Item -LiteralPath ('Env:'+ $_.Name)}
New-Item -ItemType Directory -Force build/full096-native,build/full096-results | Out-Null
$env:TEMP=(Resolve-Path build/full096-native).Path
$env:TMP=$env:TEMP
$source096=(git rev-parse HEAD)
@{source=$source096;startedAt=(Get-Date -Format o);command='gradlew :check :distZip --offline --no-daemon --max-workers=1 --console=plain';freshNative=$true;temp=$env:TEMP;resumeVariables=@(Get-ChildItem Env: | Where-Object {$_.Name -match '^FRAYNIX.*(RESUME|PROJECT)$'} | Select-Object -ExpandProperty Name)} | ConvertTo-Json | Set-Content build/full096-run.json -Encoding utf8
& ./gradlew.bat :check :distZip --offline --no-daemon --max-workers=1 --console=plain *> build/full096.log
$exit096=$LASTEXITCODE
Get-ChildItem -Path . -Filter 'TEST-*.xml' -Recurse -File | Where-Object {$_.FullName -match '[\\/]build[\\/]test-results[\\/]test[\\/]'} | ForEach-Object {
 $relative096=[IO.Path]::GetRelativePath((Get-Location).Path,$_.FullName)
 $destination096=Join-Path 'build/full096-results' $relative096
 New-Item -ItemType Directory -Force (Split-Path -Parent $destination096) | Out-Null
 Copy-Item -LiteralPath $_.FullName -Destination $destination096
}
@{source=$source096;finishedAt=(Get-Date -Format o);exit=$exit096} | ConvertTo-Json | Set-Content build/full096-finished.json -Encoding utf8
Get-Content build/full096.log -Tail 30
exit $exit096
