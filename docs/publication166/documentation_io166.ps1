$ErrorActionPreference = 'Stop'
$root = (Get-Location).Path
$script = Join-Path $root 'scripts/Verify-Documentation.ps1'
$tokens = $null
$errors = $null
$ast = [Management.Automation.Language.Parser]::ParseFile($script,[ref]$tokens,[ref]$errors)
if($errors.Count){throw 'Documentation script parse failure'}
$definition = $ast.Find({param($node) $node -is [Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Canonical-Hash'},$true)
. ([scriptblock]::Create($definition.Extent.Text))
$oldSource = (git show '63769d3d5:scripts/Verify-Documentation.ps1') -join "`n"
$oldAst = [Management.Automation.Language.Parser]::ParseInput($oldSource,[ref]$tokens,[ref]$errors)
$oldDefinition = $oldAst.Find({param($node) $node -is [Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Canonical-Hash'},$true)
. ([scriptblock]::Create($oldDefinition.Extent.Text.Replace('function Canonical-Hash','function Original-Canonical-Hash')))
$manifest = Get-Content 'docs/readmes/editions.json' -Raw -Encoding utf8 | ConvertFrom-Json
$entry = $manifest.editions | Where-Object number -eq 164
$sample = $entry.snapshotFiles | Where-Object kind -eq binary | Sort-Object { (Get-Item -LiteralPath $_.path).Length } -Descending | Select-Object -First 1
$file = Join-Path $root $sample.path
$rows = @()
foreach($mode in @('original','buffered','buffered','original')) {
    $timer = [Diagnostics.Stopwatch]::StartNew()
    $digest = if($mode -eq 'original'){Original-Canonical-Hash $file $true}else{Canonical-Hash $file $true}
    $timer.Stop()
    if($digest -ne $sample.sha256){throw "Published binary digest changed: $mode"}
    $rows += [ordered]@{mode=$mode;milliseconds=$timer.Elapsed.TotalMilliseconds;sha256=$digest}
}
$copy = Join-Path $root 'build/documentation166-changed.bin'
if(Test-Path -LiteralPath $copy){throw 'Refuse overwriting a prior test'}
[byte[]]$bytes = [IO.File]::ReadAllBytes($file)
$bytes[0] = $bytes[0] -bxor 1
[IO.File]::WriteAllBytes($copy,$bytes)
if((Canonical-Hash $copy $true) -eq $sample.sha256){throw 'Changed binary was accepted'}
if((Canonical-Hash $file $true) -ne $sample.sha256){throw 'Original evidence changed'}
$result = [ordered]@{passed=$true;source=$sample.path;bytes=(Get-Item -LiteralPath $file).Length;rows=$rows;changedCopyRefused=$true;originalUnchanged=$true;scope='Four alternating hashes of one published binary, before the full documentation pass. Same SHA-256 and sharing rule; only file buffering changes. Exposed local IO diagnostic, not a general performance guarantee.'}
$result | ConvertTo-Json -Depth 8 | Set-Content 'build/documentation-io166.json' -Encoding utf8
$result | ConvertTo-Json -Depth 8
