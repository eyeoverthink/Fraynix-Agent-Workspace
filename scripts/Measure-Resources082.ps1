param([ValidateSet('pin','read','monitor')][string]$Action='read',[Parameter(Mandatory=$true)][int]$TargetPid,[int]$Processors=4,[Parameter(Mandatory=$true)][string]$Checkout,[string]$OutputFile,[string]$StopFile)
$ErrorActionPreference='Stop'
$root082=(Resolve-Path -LiteralPath $Checkout).Path
$info082=Get-CimInstance Win32_Process -Filter "ProcessId = $TargetPid"
if(!$info082 -or $info082.Name -ne 'java.exe' -or !$info082.CommandLine.Contains($root082) -or $info082.CommandLine -notmatch 'fraymus.workspace.(WorkspaceServer|AgentHost042)'){throw 'Only the Java owner from the explicit study checkout may be measured or pinned.'}
$process082=Get-Process -Id $TargetPid
if($Action -eq 'pin'){
 if($Processors -lt 1 -or $Processors -gt 4){throw 'One to four logical processors required'}
 [long]$available082=$process082.ProcessorAffinity.ToInt64();[long]$chosen082=0;$count082=0
 for($bit082=0;$bit082 -lt 63 -and $count082 -lt $Processors;$bit082++){
  [long]$value082=[long]1 -shl $bit082
  if(($available082 -band $value082) -ne 0){$chosen082=$chosen082 -bor $value082;$count082++}
 }
 if($count082 -ne $Processors){throw 'Insufficient allowed logical processors'}
 $process082.ProcessorAffinity=[IntPtr]$chosen082
}
function Sample082 {
 $process082.Refresh()
 [ordered]@{atUtc=[DateTime]::UtcNow.ToString('o');pid=$TargetPid;affinity=$process082.ProcessorAffinity.ToInt64().ToString();cpuMillis=$process082.TotalProcessorTime.TotalMilliseconds;workingSetBytes=$process082.WorkingSet64;privateBytes=$process082.PrivateMemorySize64;peakWorkingSetBytes=$process082.PeakWorkingSet64;handles=$process082.HandleCount;threads=$process082.Threads.Count;startUtc=$process082.StartTime.ToUniversalTime().ToString('o')} | ConvertTo-Json -Compress
}
if($Action -ne 'monitor'){Sample082;exit}
if(!$OutputFile -or !$StopFile -or (Test-Path -LiteralPath $OutputFile)){throw 'A fresh output path and a stop path are required'}
$encoding082=[Text.UTF8Encoding]::new($false)
$writer082=[IO.StreamWriter]::new($OutputFile,$false,$encoding082)
try{
 while(!(Test-Path -LiteralPath $StopFile)){
  $process082.Refresh();if($process082.HasExited){break}
  $writer082.WriteLine((Sample082));$writer082.Flush();Start-Sleep -Milliseconds 1000
 }
}finally{$writer082.Dispose()}
