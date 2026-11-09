$ErrorActionPreference='Stop'
Set-Location 'D:/Codex-Work/fraynix-workspace-edition096'
if ((Get-Content build/full096-finished.json -Raw | ConvertFrom-Json).exit -ne 1) { throw 'Review original complete run before correction' }
& python build/guard-correction096.py
if ($LASTEXITCODE -ne 0) { throw 'Unaccounted original failure; inspect before continuing' }
& python build/fix-payload096.py
if ($LASTEXITCODE -ne 0) { throw 'Exact test correction failed' }
& git diff --check
if ($LASTEXITCODE -ne 0) { throw 'Diff check failed' }
& git add workspace-engine/src/test/java/fraymus/workspace/Payload057Test.java workspace-engine/src/test/java/fraymus/workspace/Payload058PolicyTest.java workspace-engine/src/test/java/fraymus/workspace/Support059Test.java workspace-engine/src/test/java/fraymus/workspace/Support060Test.java workspace-desktop/test/verification096-integration.cjs docs/EDITION_096_VALIDATION_UPDATE.md
& git -c gc.auto=0 commit -m 'Align construction policy tests with validated model proposals and Java preparation'
if ($LASTEXITCODE -ne 0) { throw 'Commit failed' }
$env:JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot'
$env:PATH="$env:JAVA_HOME/bin;$env:PATH"
$env:JAVA_TOOL_OPTIONS='-XX:ActiveProcessorCount=4 -Xmx768m'
Get-ChildItem Env: | Where-Object {$_.Name -match '^FRAYNIX.*(RESUME|PROJECT)$'} | ForEach-Object {Remove-Item -LiteralPath ('Env:'+ $_.Name)}
New-Item -ItemType Directory -Force build/corrected096-native,build/corrected096-results | Out-Null
$env:TEMP=(Resolve-Path build/corrected096-native).Path
$env:TMP=$env:TEMP
$source096=(git rev-parse HEAD)
$selectors096=@('fraymus.workspace.DesignPolicy053Test','fraymus.workspace.ConstructionSchema096Test','fraymus.workspace.Domain044Test','fraymus.workspace.RegressionLock096Test','fraymus.workspace.Payload058PolicyTest','fraymus.workspace.Support059Test','fraymus.workspace.Support060Test','fraymus.workspace.Payload057Test.explicitChatPayloadRequestCannotBeDropped','fraymus.workspace.Payload057Test.payloadOnlyChatCannotInventUnrequestedRedesignSettings','fraymus.workspace.Payload057Test.protocolUsesSeparateRealMassesAndDeclaredPositions')
$args096=@(':workspace-engine:test','--offline','--no-daemon','--max-workers=1','--console=plain')
foreach ($selector096 in $selectors096) { $args096+=@('--tests',$selector096) }
@{source=$source096;startedAt=(Get-Date -Format o);selectors=$selectors096;scope='Focused follow-up only; default complete lifecycle has no exclusions.'} | ConvertTo-Json | Set-Content build/corrected096-run.json -Encoding utf8
& ./gradlew.bat @args096 *> build/corrected096.log
$exit096=$LASTEXITCODE
Copy-Item -Path workspace-engine/build/test-results/test/TEST-*.xml -Destination build/corrected096-results
@{source=$source096;finishedAt=(Get-Date -Format o);exit=$exit096} | ConvertTo-Json | Set-Content build/corrected096-finished.json -Encoding utf8
Get-Content build/corrected096.log -Tail 25
if ($exit096 -ne 0) { exit $exit096 }
& python build/coverage096.py
exit $LASTEXITCODE
