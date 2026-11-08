$ErrorActionPreference='Stop'
$env:JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot'
$env:JAVA_TOOL_OPTIONS='-XX:ActiveProcessorCount=4 -Xmx768m -Djava.io.tmpdir=D:/Codex-Work/fraynix-workspace-edition095/build/tmp-tests'
$env:PATH=$env:JAVA_HOME+'/bin;'+$env:PATH
$active095=@(Get-CimInstance Win32_Process | Where-Object {$_.Name -eq 'java.exe' -and $_.CommandLine -like '*fraynix-workspace-edition095*' -and $_.CommandLine -like '*WorkspaceServer*'})
if($active095.Count){throw 'Close the isolated study before regression builds.'}
New-Item -ItemType Directory -Force build/tmp-tests | Out-Null
# Source-identical 094 originals exercise the unchanged native corruption boundaries.
python -c "import shutil,pathlib; r=pathlib.Path.cwd(); src=pathlib.Path('D:/Codex-Work/fraynix-workspace-edition094/build/ui094-study/project'); dst=r/'build/native-audit095'; assert not dst.exists(); dst.mkdir(); [shutil.copytree(pathlib.Path(chr(92)*2+'?'+chr(92)+str(src/n)),pathlib.Path(chr(92)*2+'?'+chr(92)+str(dst/n))) for n in ['world-procedures-079','persistent-worlds']]"
if($LASTEXITCODE){throw 'Native regression evidence copy failed.'}
$env:FRAYNIX_RECOVERY094_PROJECT='D:/Codex-Work/fraynix-workspace-edition095/build/native-audit095'
git rev-parse HEAD > build/regression095-source.txt
$classes=@('RecoveryForecast093Test','RecoveryAudit094Test','NativeCheckpoint094Test','WorldUpgrade094Test','AtomicCheckpoint054Test','WorldCheckpoint071Test','NativeEnvelope091Test','EnvelopeOutcome091Test','TaskCost080Test','TaskCost084Test','TaskCost089Test','TaskCost090Test','CalibrationCoverage083Test','RebuildCompatibility079Test','Procedure079Test','WorldProcedure079Test','ProofMemo078Test','WorldLedgerTest','Resources082Test')
$testArgs=@(':workspace-engine:test','--offline','--no-daemon','--max-workers=1','-x',':installDist')
foreach($class in $classes){$testArgs+=@('--tests',('fraymus.workspace.'+$class))}
& ./gradlew.bat @testArgs *> build/regression095.log
$exit095=$LASTEXITCODE
New-Item -ItemType Directory -Force build/regression095 | Out-Null
Copy-Item -Path workspace-engine/build/test-results/test/TEST-*.xml -Destination build/regression095/
if($exit095){throw 'Java regression failed; retain the original result.'}
Push-Location workspace-desktop
try { & npm.cmd test *> ../build/desktop095.log; if($LASTEXITCODE){throw 'Desktop regression failed.'}; node --test test/costs095.test.cjs *> ../build/cost-tests095.log; if($LASTEXITCODE){throw 'Cost arithmetic checks failed.'} } finally {Pop-Location}
Write-Output 'Focused Java and desktop regressions completed.'
