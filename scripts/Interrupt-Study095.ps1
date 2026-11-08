param([Parameter(Mandatory=$true)][int]$TargetPid,[Parameter(Mandatory=$true)][int]$ExpectedParentPid,[Parameter(Mandatory=$true)][string]$ExpectedStartUtc,[Parameter(Mandatory=$true)][string]$Checkout,[Parameter(Mandatory=$true)][string]$Project)
$ErrorActionPreference='Stop'
$root095=(Resolve-Path -LiteralPath $Checkout).Path
$project095=(Resolve-Path -LiteralPath $Project).Path
$allowed095=[IO.Path]::GetFullPath((Join-Path $root095 'build/ui095-study'))
$relative095=[IO.Path]::GetRelativePath($allowed095,$project095)
if($relative095 -notmatch '^task-[0-9]{2}[\\/]project$'){throw 'Only an isolated095 numbered study project is allowed.'}
if($TargetPid -le 0 -or $ExpectedParentPid -le 0){throw 'Explicit positive process identities required.'}
$info095=Get-CimInstance Win32_Process -Filter "ProcessId = $TargetPid"
if(!$info095 -or $info095.Name -ne 'java.exe' -or $info095.ParentProcessId -ne $ExpectedParentPid -or !$info095.CommandLine.Contains($root095) -or $info095.CommandLine -notmatch 'fraymus.workspace.WorkspaceServer'){throw 'Target is not the explicitly owned isolated Java server.'}
$process095=Get-Process -Id $TargetPid
$start095=$process095.StartTime.ToUniversalTime().ToString('o')
if([DateTime]::Parse($start095).Ticks -ne [DateTime]::Parse($ExpectedStartUtc).Ticks){throw 'Process start identity changed.'}
$proof095=[ordered]@{pid=$TargetPid;parentPid=$ExpectedParentPid;startUtc=$start095;project=$project095;checkout=$root095;executable=$info095.ExecutablePath;commandLine=$info095.CommandLine;beforeKillUtc=[DateTime]::UtcNow.ToString('o');method='Stop-Process -Id exact-owned-PID -Force';gracefulShutdownRequested=$false}
Stop-Process -Id $TargetPid -Force
if(!$process095.WaitForExit(10000)){throw 'Owned process did not exit within bound.'}
$proof095['exited']=$process095.HasExited
$proof095['afterKillUtc']=[DateTime]::UtcNow.ToString('o')
$proof095 | ConvertTo-Json -Compress
