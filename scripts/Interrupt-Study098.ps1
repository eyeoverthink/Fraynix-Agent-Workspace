param([Parameter(Mandatory=$true)][int]$TargetPid,[Parameter(Mandatory=$true)][int]$ExpectedParentPid,[Parameter(Mandatory=$true)][string]$ExpectedStartUtc,[Parameter(Mandatory=$true)][string]$Checkout,[Parameter(Mandatory=$true)][string]$Project)
$ErrorActionPreference='Stop'
$root098=(Resolve-Path -LiteralPath $Checkout).Path
$project098=(Resolve-Path -LiteralPath $Project).Path
$allowed098=[IO.Path]::GetFullPath((Join-Path $root098 'build/ui098-study'))
$relative098=[IO.Path]::GetRelativePath($allowed098,$project098)
if($relative098 -notmatch '^eval-long-recover-(simple|memory|adaptive)[\\/]project$'){throw 'Only an isolated098 recovery study project is allowed.'}
if($TargetPid -le 0 -or $ExpectedParentPid -le 0){throw 'Explicit positive process identities required.'}
$info098=Get-CimInstance Win32_Process -Filter "ProcessId = $TargetPid"
if(!$info098 -or $info098.Name -ne 'java.exe' -or $info098.ParentProcessId -ne $ExpectedParentPid -or !$info098.CommandLine.Contains($root098) -or $info098.CommandLine -notmatch 'fraymus.workspace.WorkspaceServer'){throw 'Target is not the explicitly owned isolated Java server.'}
$process098=Get-Process -Id $TargetPid
$start098=$process098.StartTime.ToUniversalTime().ToString('o')
if([DateTime]::Parse($start098).Ticks -ne [DateTime]::Parse($ExpectedStartUtc).Ticks){throw 'Process start identity changed.'}
$proof098=[ordered]@{pid=$TargetPid;parentPid=$ExpectedParentPid;startUtc=$start098;project=$project098;checkout=$root098;executable=$info098.ExecutablePath;commandLine=$info098.CommandLine;beforeKillUtc=[DateTime]::UtcNow.ToString('o');method='Stop-Process -Id exact-owned-PID -Force';gracefulShutdownRequested=$false}
Stop-Process -Id $TargetPid -Force
if(!$process098.WaitForExit(10000)){throw 'Owned process did not exit within bound.'}
$proof098['exited']=$process098.HasExited
$proof098['afterKillUtc']=[DateTime]::UtcNow.ToString('o')
$proof098 | ConvertTo-Json -Compress
