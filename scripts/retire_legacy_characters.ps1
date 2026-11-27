$ErrorActionPreference='Stop'
$repo='G:\ChatGPT Projects\Forests of Hell\ForestsOfHellSource'
$assets=Join-Path $repo 'godot\assets'
$archive='G:\ChatGPT Projects\Forests of Hell\Master Asset Library\Retired Character Assets\before-v0.3.176'
New-Item -ItemType Directory -Force -Path $archive | Out-Null
$keep=@('human_adventurer.glb','goblin_warrior.glb','fire_beetle.glb','humanoid_mixamo_animation_library.glb','pine.glb','broadleaf.glb','boulder.glb')
$tracked=@{}
foreach($rel in @(& git -C $repo ls-files 'godot/assets/*.glb')){
    $tracked[$rel.Replace('\','/')]= $true
}
$manifest=New-Object 'System.Collections.Generic.List[object]'
$toRetire=@(Get-ChildItem $assets -File | Where-Object {
    ($_.Extension -eq '.glb' -and $keep -notcontains $_.Name) -or
    ($_.Extension -eq '.blend' -or $_.Name -like '*.blend1' -or $_.Name -like '*.blend2') -or
    ($_.Extension -eq '.png' -and $_.Name -like 'katie*')
})
foreach($f in $toRetire){
    $rel='godot/assets/'+$f.Name
    $dest=Join-Path $archive $f.Name
    if(-not(Test-Path $dest)){Copy-Item -LiteralPath $f.FullName -Destination $dest}
    $hash=(Get-FileHash -LiteralPath $f.FullName -Algorithm SHA256).Hash
    $archiveHash=(Get-FileHash -LiteralPath $dest -Algorithm SHA256).Hash
    if($hash -ne $archiveHash){throw "SHA256 differs from archive for $($f.Name)"}
    $isTracked=$tracked.ContainsKey($rel)
    $manifest.Add([pscustomobject]@{name=$f.Name;bytes=[long]$f.Length;sha256=$hash;tracked=$isTracked})
    if($isTracked){& git -C $repo rm -f -- $rel | Out-Null; if($LASTEXITCODE -ne 0){throw "git rm failed $rel"}}
    else{Remove-Item -LiteralPath $f.FullName -Force}
}
foreach($f in @(Get-ChildItem $assets -File -Filter '*.import')){
    $name=$f.Name.Substring(0,$f.Name.Length-7)
    if($keep -notcontains $name -and -not(Test-Path (Join-Path $assets $name))){
        Remove-Item -LiteralPath $f.FullName -Force
    }
}
$manifest|ConvertTo-Json -Depth 5|Set-Content -Path (Join-Path $archive 'retired-asset-manifest.json') -Encoding UTF8
Write-Output ('RETIRED_FILES='+$manifest.Count)
Write-Output ('RETIRED_BYTES='+[long](($manifest|Measure-Object bytes -Sum).Sum))
Write-Output ('RETIRED_TRACKED='+@($manifest|Where-Object tracked).Count)
Write-Output ('REMAINING_GLBS='+((Get-ChildItem $assets -File -Filter '*.glb' | Select-Object -ExpandProperty Name) -join ', '))
Write-Output ('REMAINING_ASSET_BYTES='+[long]((Get-ChildItem $assets -File | Measure-Object Length -Sum).Sum))
Write-Output ('ARCHIVE='+$archive)
