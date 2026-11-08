param([Parameter(Mandatory=$true)][int]$TargetPid,[Parameter(Mandatory=$true)][int]$ExpectedParentPid,[Parameter(Mandatory=$true)][string]$ExpectedStartUtc,[Parameter(Mandatory=$true)][string]$Checkout,[Parameter(Mandatory=$true)][string]$Project)
$ErrorActionPreference='Stop'
$root094=(Resolve-Path -LiteralPath $Checkout).Path
$project094=(Resolve-Path -LiteralPath $Project).Path
$allowed094=[IO.Path]::GetFullPath((Join-Path $root094 'build/ui094-study/project'))
if($project094 -ne $allowed094){throw 'Only the isolated094 study project is allowed.'}
if($TargetPid -le 0 -or $ExpectedParentPid -le 0){throw 'Explicit positive process identities required.'}
$info094=Get-CimInstance Win32_Process -Filter "ProcessId = $TargetPid"
if(!$info094 -or $info094.Name -ne 'java.exe' -or $info094.ParentProcessId -ne $ExpectedParentPid -or !$info094.CommandLine.Contains($root094) -or $info094.CommandLine -notmatch 'fraymus.workspace.WorkspaceServer'){throw 'Target is not the explicitly owned isolated Java server.'}
$process094=Get-Process -Id $TargetPid
$start094=$process094.StartTime.ToUniversalTime().ToString('o')
if([DateTime]::Parse($start094).Ticks -ne [DateTime]::Parse($ExpectedStartUtc).Ticks){throw 'Process start identity changed.'}
$proof094=[ordered]@{pid=$TargetPid;parentPid=$ExpectedParentPid;startUtc=$start094;project=$project094;checkout=$root094;executable=$info094.ExecutablePath;commandLine=$info094.CommandLine;beforeKillUtc=[DateTime]::UtcNow.ToString('o');method='Stop-Process -Id exact-owned-PID -Force';gracefulShutdownRequested=$false}
Stop-Process -Id $TargetPid -Force
if(!$process094.WaitForExit(10000)){throw 'Owned process did not exit within bound.'}
$proof094['exited']=$process094.HasExited
$proof094['afterKillUtc']=[DateTime]::UtcNow.ToString('o')
$proof094 | ConvertTo-Json -Compress
