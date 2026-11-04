param([string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot))
$ErrorActionPreference = 'Stop'
$repo = [IO.Path]::GetFullPath($RepositoryRoot)
$issues = [System.Collections.Generic.List[string]]::new()
$anchors = @{}
$referenceCount = 0
$hashCount = 0
function Canonical-Hash([string]$Path,[bool]$Binary = $false) {
    $sha = [Security.Cryptography.SHA256]::Create()
    $stream = $null
    try {
        if($Binary){
            $stream = [IO.File]::OpenRead($Path)
            $digest = $sha.ComputeHash($stream)
        }else{
            [byte[]]$bytes = [Text.Encoding]::UTF8.GetBytes([IO.File]::ReadAllText($Path).Replace("`r`n","`n"))
            $digest = $sha.ComputeHash($bytes)
        }
        return ([BitConverter]::ToString($digest)).Replace('-','').ToLowerInvariant()
    } finally { if($stream){$stream.Dispose()}; $sha.Dispose() }
}
function Resolve-Local([string]$Origin,[string]$Target,[bool]$Vault = $false) {
    if($Target -match '^(https?|mailto|obsidian|data):'){return $null}
    $value = [Net.WebUtility]::HtmlDecode($Target.Trim('<','>'))
    $parts = $value.Split('#',2)
    $base = if($Vault){$repo}else{Split-Path -Parent $Origin}
    $path = if(!$parts[0]){$Origin}else{[IO.Path]::GetFullPath((Join-Path $base ([Uri]::UnescapeDataString($parts[0]))))}
    if($path -ne $repo -and !$path.StartsWith($repo+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){
        $issues.Add("Reference escapes repository: $Origin -> $Target");return $null
    }
    if(!(Test-Path -LiteralPath $path)){$issues.Add("Missing target: $Origin -> $Target");return $null}
    if($parts.Count -gt 1 -and $parts[1] -and [IO.Path]::GetExtension($path) -in '.md','.html'){
        if(!$anchors.ContainsKey($path)){
            $text=[IO.File]::ReadAllText($path)
            $set=[System.Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
            foreach($m in [regex]::Matches($text,'(?i)\bid=["'']([^"'']+)["'']')){[void]$set.Add($m.Groups[1].Value)}
            foreach($m in [regex]::Matches($text,'(?m)^#{1,6}\s+(.+?)\s*$')){
                $slug=($m.Groups[1].Value.ToLowerInvariant() -replace '[^\p{L}\p{N}\s_-]','') -replace '\s','-'
                [void]$set.Add($slug)
            }
            $anchors[$path]=$set
        }
        if(!$anchors[$path].Contains([Uri]::UnescapeDataString($parts[1]))){$issues.Add("Missing fragment: $Origin -> $Target")}
    }
    return $path
}
function Check-Wiki([string]$Origin,[string]$Text){
    foreach($m in [regex]::Matches($Text,'\[\[([^\]]+)\]\]')){
        $target=($m.Groups[1].Value.Split('|')[0]).Split('#')[0]
        if(!$target){continue}
        if(![IO.Path]::GetExtension($target)){$target+='.md'}
        $script:referenceCount++;$null=Resolve-Local $Origin $target $true
    }
}
$manifestPath=Join-Path $repo 'docs/readmes/editions.json'
$manifest=Get-Content -LiteralPath $manifestPath -Raw -Encoding UTF8 | ConvertFrom-Json
$ids=@($manifest.editions | ForEach-Object {[int]$_.number})
if(@($ids|Sort-Object -Unique).Count -ne $ids.Count){$issues.Add('Duplicate edition number')}
if(($ids|Measure-Object -Maximum).Maximum -ne [int]$manifest.latest){$issues.Add('Latest edition does not match highest registered number')}
for($i=0;$i -lt $ids.Count;$i++){if(($ids|Sort-Object)[$i] -ne $i+1){$issues.Add('Edition numbers must be sequential from 001')}}
foreach($edition in $manifest.editions){
    $p=Resolve-Local $manifestPath $edition.markdown $true
    if($p -and (Canonical-Hash $p) -ne $edition.markdownSha256){$issues.Add("Edition content changed: $($edition.markdown)")};$hashCount++
    foreach($asset in $edition.snapshotFiles){
        $p=Resolve-Local $manifestPath $asset.path $true
        if($p -and (Canonical-Hash $p ($asset.kind -eq 'binary')) -ne $asset.sha256){$issues.Add("Frozen snapshot changed: $($asset.path)")};$hashCount++
    }
}
$scope=@(Get-Item -LiteralPath (Join-Path $repo 'README.md'))+@(Get-ChildItem -LiteralPath $repo -Filter 'README-*.md' -File)
foreach($dir in 'docs/readmes','docs/obsidian'){$scope+=@(Get-ChildItem -LiteralPath (Join-Path $repo $dir) -Recurse -File | Where-Object {$_.Extension -in '.md','.html'})}
foreach($file in $scope){
    $text=[IO.File]::ReadAllText($file.FullName)
    if($file.Extension -eq '.md'){
        $plain=[regex]::Replace($text,'(?ms)^```.*?^```[^\r\n]*','')
        foreach($m in [regex]::Matches($plain,'!?\[[^\]\r\n]*\]\((?<target><[^>]+>|[^)\s]+)(?:\s+"[^"]*")?\)')){
            $referenceCount++;$null=Resolve-Local $file.FullName $m.Groups['target'].Value
        }
        if($file.FullName -match '[\\/]obsidian[\\/]'){Check-Wiki $file.FullName $plain}
    }else{
        foreach($m in [regex]::Matches($text,'(?i)(?:href|src)="([^"$]+)"')){$referenceCount++;$null=Resolve-Local $file.FullName $m.Groups[1].Value}
        if($text -match '<(?:script|link|img)\b[^>]+(?:src|href)="https?://'){$issues.Add("External asset dependency: $($file.FullName)")}
    }
}
$canvases=@(Get-ChildItem -LiteralPath (Join-Path $repo 'docs') -Filter '*.canvas' -Recurse -File)
foreach($file in $canvases){
    $c=Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8 | ConvertFrom-Json
    $nodes=[System.Collections.Generic.HashSet[string]]::new();$edges=[System.Collections.Generic.HashSet[string]]::new()
    foreach($n in $c.nodes){
        if(!$nodes.Add($n.id)){$issues.Add("Duplicate canvas node: $($file.Name) $($n.id)")}
        if($n.type -notin 'text','file','link','group' -or $n.width -le 0 -or $n.height -le 0){$issues.Add("Invalid canvas geometry/type: $($file.Name) $($n.id)")}
        if($n.type -eq 'file'){$referenceCount++;$null=Resolve-Local $file.FullName $n.file $true}
        if($n.type -eq 'text'){Check-Wiki $file.FullName $n.text}
    }
    foreach($e in $c.edges){if(!$edges.Add($e.id) -or !$nodes.Contains($e.fromNode) -or !$nodes.Contains($e.toNode)){$issues.Add("Invalid canvas edge: $($file.Name) $($e.id)")}}
}
foreach($edition in $manifest.editions){
    $html=[IO.File]::ReadAllText((Join-Path $repo $edition.companion))
    $dir=Join-Path $repo $edition.snapshotDirectory
    $map=Get-Content -LiteralPath (Join-Path $dir 'system-map.json') -Raw -Encoding UTF8 | ConvertFrom-Json
    $inventory=Get-Content -LiteralPath (Join-Path $dir 'file-inventory.json') -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach($pair in @(@('system-data',$map),@('inventory-data',$inventory))){
        $capture=[regex]::Match($html,'(?s)<script id="'+$pair[0]+'" type="application/json">(.*?)</script>')
        if(!$capture.Success){$issues.Add("Missing embedded dataset $($pair[0])");continue}
        $embedded=$capture.Groups[1].Value | ConvertFrom-Json
        if(($embedded|ConvertTo-Json -Depth 50 -Compress) -ne ($pair[1]|ConvertTo-Json -Depth 50 -Compress)){$issues.Add("Embedded dataset mismatch: $($pair[0])")}
    }
    foreach($m in $map.modules){foreach($key in 'source','note'){$referenceCount++;$null=Resolve-Local $manifestPath $m.$key $true}}
    foreach($p in @($map.screenshots)+@($map.evidence)){$referenceCount++;$null=Resolve-Local $manifestPath $p.path $true}
    foreach($f in $inventory){$null=Resolve-Local $manifestPath $f.path $true}
}
$report=[ordered]@{passed=($issues.Count -eq 0);editions=$ids.Count;documents=$scope.Count;canvases=$canvases.Count;references=$referenceCount;frozenHashes=$hashCount;issues=$issues.ToArray()}
$output=Join-Path $repo 'build/documentation-check.json';New-Item -ItemType Directory -Path (Split-Path -Parent $output) -Force | Out-Null
[IO.File]::WriteAllText($output,($report|ConvertTo-Json -Depth 7),[Text.UTF8Encoding]::new($false))
if($issues.Count){$issues|ForEach-Object{Write-Output $_};throw "Documentation verification failed: $($issues.Count) issue(s)"}
Write-Output "Documentation verified: $($ids.Count) edition(s), $($canvases.Count) canvases, $referenceCount references and $hashCount frozen hashes."
