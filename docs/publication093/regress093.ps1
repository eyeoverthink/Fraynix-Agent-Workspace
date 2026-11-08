$ErrorActionPreference='Stop'
$env:JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot'
$env:JAVA_TOOL_OPTIONS='-XX:ActiveProcessorCount=4 -Xmx768m -Djava.io.tmpdir=D:/Codex-Work/fraynix-workspace-edition093/build/tmp-tests'
$env:PATH=$env:JAVA_HOME+'/bin;'+$env:PATH
$running=@(Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -like '*fraynix-workspace-edition093*' -and $_.CommandLine -like '*WorkspaceServer*' })
if($running.Count){throw 'Close the isolated study before regression builds.'}
python -c "import shutil,pathlib,json; r=pathlib.Path.cwd(); src=r/'build/ui093-study/project'; dst=r/'build/native-audit093'; assert not dst.exists(); dst.mkdir(); [shutil.copytree(src/n,dst/n) for n in ['world-procedures-079','persistent-worlds']]"
if($LASTEXITCODE){throw 'Native evidence copy failed.'}
$env:FRAYNIX_RECOVERY093_PROJECT='D:/Codex-Work/fraynix-workspace-edition093/build/native-audit093'
git rev-parse HEAD > build/regression093-source.txt
$classes=@('RecoveryForecast093Test','RecoveryAudit093Test','NativeEnvelope091Test','EnvelopeOutcome091Test','TaskCost090Test','RebuildCompatibility079Test','Procedure079Test','WorldProcedure079Test','ProofMemo078Test','WorldLedgerTest','Resources082Test')
$testArgs=@(':workspace-engine:test','--offline','--no-daemon','--max-workers=1','-x',':installDist')
foreach($class in $classes){$testArgs+=@('--tests',('fraymus.workspace.'+$class))}
& ./gradlew.bat @testArgs *> build/regression093.log
New-Item -ItemType Directory -Force build/regression093 | Out-Null
Copy-Item -Path workspace-engine/build/test-results/test/TEST-*.xml -Destination build/regression093/
if($LASTEXITCODE){throw 'Focused Java regression failed; preserve original log.'}
Push-Location workspace-desktop
try { & npm.cmd test *> ../build/desktop093.log; if($LASTEXITCODE){throw 'Desktop regression failed.'} } finally {Pop-Location}
Write-Output 'Focused Java and desktop regression complete.'
