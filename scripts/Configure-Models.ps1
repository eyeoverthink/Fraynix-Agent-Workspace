param(
    [string[]]$Endpoints = @('http://127.0.0.1:11434'),
    [string]$CatalogPath = (Join-Path $HOME '.fraynix-studio\models\local-model-catalog.json')
)
$ErrorActionPreference = 'Stop'
$stores = [System.Collections.Generic.List[object]]::new()
$models = [System.Collections.Generic.List[object]]::new()
$seen = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
$previous = if (Test-Path -LiteralPath $CatalogPath) { Get-Content -LiteralPath $CatalogPath -Raw | ConvertFrom-Json } else { $null }
foreach ($endpointInput in $Endpoints) {
    $endpoint = $endpointInput.TrimEnd('/')
    $uri = [uri]$endpoint
    if ($uri.Scheme -ne 'http' -or $uri.Host -notin @('127.0.0.1','localhost','[::1]','::1') -or $uri.UserInfo -or $uri.Query -or $uri.Fragment -or $uri.AbsolutePath -ne '/') {
        throw "Only loopback Ollama HTTP endpoints are supported: $endpoint"
    }
    try {
        $tags = (Invoke-RestMethod "$endpoint/api/tags" -TimeoutSec 10).models
        $stores.Add([ordered]@{endpoint=$endpoint;online=$true})
    } catch {
        $stores.Add([ordered]@{endpoint=$endpoint;online=$false})
        # Preserve known mappings while a configured store is temporarily offline.
        foreach ($row in @($previous.models | Where-Object { $_.endpoint -eq $endpoint })) {
            if ($row -and $seen.Add($row.name)) { $models.Add($row) }
        }
        Write-Warning "Store unavailable; retained known mappings: $endpoint"
        continue
    }
    foreach ($tag in $tags) {
        if ($tag.remote_host -or $tag.name -match 'cloud' -or !$seen.Add($tag.name)) { continue }
        $models.Add([ordered]@{
            name=$tag.name;status='LOCAL_INSTALLED';endpoint=$endpoint;family=$tag.details.family
            parameterSize=$tag.details.parameter_size;quantization=$tag.details.quantization_level
            sizeBytes=$tag.size;contextLength=$tag.details.context_length;capabilities=@($tag.capabilities)
        })
    }
}
$parent = Split-Path -Parent ([IO.Path]::GetFullPath($CatalogPath))
New-Item -ItemType Directory -Path $parent -Force | Out-Null
$temporary = Join-Path $parent ('catalog-' + [guid]::NewGuid().ToString('N') + '.tmp')
$json = [ordered]@{observedAt=[DateTime]::UtcNow.ToString('o');stores=$stores.ToArray();models=$models.ToArray()} | ConvertTo-Json -Depth 8
[IO.File]::WriteAllText($temporary, $json, [Text.UTF8Encoding]::new($false))
Move-Item -LiteralPath $temporary -Destination $CatalogPath -Force
Write-Output "Configured $($models.Count) exact local model names across $($stores.Count) store(s). Catalog: $CatalogPath"
