param([Parameter(Mandatory=$true)][int]$TargetPid,[Parameter(Mandatory=$true)][int]$ExpectedParentPid,[Parameter(Mandatory=$true)][string]$ExpectedStartUtc,[Parameter(Mandatory=$true)][string]$Checkout,[Parameter(Mandatory=$true)][string]$Project)
$ErrorActionPreference='Stop'
$root097=(Resolve-Path -LiteralPath $Checkout).Path
$project097=(Resolve-Path -LiteralPath $Project).Path
$allowed097=[IO.Path]::GetFullPath((Join-Path $root097 'build/ui097-study'))
$relative097=[IO.Path]::GetRelativePath($allowed097,$project097)
if($relative097 -notmatch '^stream-[0-9]{2}[\\/]project$'){throw 'Only an isolated097 numbered study project is allowed.'}
if($TargetPid -le 0 -or $ExpectedParentPid -le 0){throw 'Explicit positive process identities required.'}
$info097=Get-CimInstance Win32_Process -Filter "ProcessId = $TargetPid"
if(!$info097 -or $info097.Name -ne 'java.exe' -or $info097.ParentProcessId -ne $ExpectedParentPid -or !$info097.CommandLine.Contains($root097) -or $info097.CommandLine -notmatch 'fraymus.workspace.WorkspaceServer'){throw 'Target is not the explicitly owned isolated Java server.'}
$process097=Get-Process -Id $TargetPid
$start097=$process097.StartTime.ToUniversalTime().ToString('o')
if([DateTime]::Parse($start097).Ticks -ne [DateTime]::Parse($ExpectedStartUtc).Ticks){throw 'Process start identity changed.'}
$proof097=[ordered]@{pid=$TargetPid;parentPid=$ExpectedParentPid;startUtc=$start097;project=$project097;checkout=$root097;executable=$info097.ExecutablePath;commandLine=$info097.CommandLine;beforeKillUtc=[DateTime]::UtcNow.ToString('o');method='Stop-Process -Id exact-owned-PID -Force';gracefulShutdownRequested=$false}
Stop-Process -Id $TargetPid -Force
if(!$process097.WaitForExit(10000)){throw 'Owned process did not exit within bound.'}
$proof097['exited']=$process097.HasExited
$proof097['afterKillUtc']=[DateTime]::UtcNow.ToString('o')
$proof097 | ConvertTo-Json -Compress
