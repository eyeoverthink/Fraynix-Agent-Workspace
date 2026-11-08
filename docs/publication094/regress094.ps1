$ErrorActionPreference='Stop'
$env:JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot'
$env:JAVA_TOOL_OPTIONS='-XX:ActiveProcessorCount=4 -Xmx768m -Djava.io.tmpdir=D:/Codex-Work/fraynix-workspace-edition094/build/tmp-tests'
$env:PATH=$env:JAVA_HOME+'/bin;'+$env:PATH
$running=@(Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -like '*fraynix-workspace-edition094*' -and $_.CommandLine -like '*WorkspaceServer*' })
if($running.Count){throw 'Close the isolated study before regression builds.'}
python -c "import shutil,pathlib,json; r=pathlib.Path.cwd(); src=r/'build/ui094-study/project'; dst=r/'build/native-audit094-longpaths'; assert not dst.exists(); dst.mkdir(); [shutil.copytree(pathlib.Path(chr(92)*2+'?'+chr(92)+str(src/n)),pathlib.Path(chr(92)*2+'?'+chr(92)+str(dst/n))) for n in ['world-procedures-079','persistent-worlds']]"
if($LASTEXITCODE){throw 'Native evidence copy failed.'}
$env:FRAYNIX_RECOVERY094_PROJECT='D:/Codex-Work/fraynix-workspace-edition094/build/native-audit094-longpaths'
git rev-parse HEAD > build/regression094-source.txt
$classes=@('RecoveryForecast093Test','RecoveryAudit094Test','NativeCheckpoint094Test','WorldUpgrade094Test','AtomicCheckpoint054Test','WorldCheckpoint071Test','NativeEnvelope091Test','EnvelopeOutcome091Test','TaskCost090Test','RebuildCompatibility079Test','Procedure079Test','WorldProcedure079Test','ProofMemo078Test','WorldLedgerTest','Resources082Test')
$testArgs=@(':workspace-engine:test','--offline','--no-daemon','--max-workers=1','-x',':installDist')
foreach($class in $classes){$testArgs+=@('--tests',('fraymus.workspace.'+$class))}
& ./gradlew.bat @testArgs *> build/regression094.log
New-Item -ItemType Directory -Force build/regression094 | Out-Null
Copy-Item -Path workspace-engine/build/test-results/test/TEST-*.xml -Destination build/regression094/
if($LASTEXITCODE){throw 'Focused Java regression failed; preserve original log.'}
Push-Location workspace-desktop
try { & npm.cmd test *> ../build/desktop094.log; if($LASTEXITCODE){throw 'Desktop regression failed.'} } finally {Pop-Location}
Write-Output 'Focused Java and desktop regression complete.'
