# Preserve launcher IDs and legacy PNG fallbacks, then import the supplied SVG vectors.
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$utf8=[Text.UTF8Encoding]::new($false)
$iconsFile=Join-Path $root 'android/app/src/main/java/com/example/trudomix/data/local/AppIcons.kt'
$source=[IO.File]::ReadAllText($iconsFile)
$legacy=@{1='01/navy';2='02/white';3='03/black';4='04/blue';5='01/coral';6='02/cream';7='04/navy';8='05/blue';9='06/black';10='06/white';11='07/navy';12='03/coral';13='07/black';14='08/white';15='08/violet';16='09/cream';17='07/coral';18='09/coral';19='04/black';20='04/white';21='01/violet';22='03/mint';23='06/navy';24='08/coral'}
$legacyResources=@{}
foreach($match in [regex]::Matches($source,'AppIcon\((\d+),\s*"[^"]+",\s*R.drawable.(\w+)(?:,[^\r\n]*)?\)')){if([int]$match.Groups[1].Value -le 24){$legacyResources[[int]$match.Groups[1].Value]=$match.Groups[2].Value}}
$designs=@('Классика','Светлая классика','Волна','Синяя T','Светлая синяя T','Лента','Самолёт','Светлый самолёт','Значок')
$colors=[ordered]@{black='тёмная';blue='голубая';coral='коралловая';cream='кремовая';mint='мятная';navy='ночная';violet='фиолетовая';white='белая'}
$entries=[Collections.Generic.List[object]]::new();$nextId=101
for($design=1;$design -le 9;$design++){
    $designCode='{0:00}' -f $design
    foreach($color in $colors.Keys){
        $pair="$designCode/$color"
        $oldId=@($legacy.Keys|Where-Object{$legacy[$_] -eq $pair})
        $id=if($oldId.Count){[int]$oldId[0]}else{$nextId;$nextId++}
        $asset="brand_${designCode}_${color}"
        $resource=if($oldId.Count){$legacyResources[$id]}else{"launcher_$id"}
        $title=if($id -eq 9){'Тёмные волны T'}else{"$($designs[$design-1]) · $($colors[$color])"}
        $inputFile=Join-Path $root "icons/icon_$designCode/$color/icon_432.png"
        if(!(Test-Path -LiteralPath $inputFile)){throw "Missing icon: $inputFile"}
        Copy-Item -LiteralPath $inputFile -Destination (Join-Path $root "android/app/src/main/res/drawable-nodpi/$asset.png") -Force
        [IO.File]::WriteAllText((Join-Path $root "android/app/src/main/res/drawable/$resource.xml"),"<bitmap xmlns:android=`"http://schemas.android.com/apk/res/android`" android:src=`"@drawable/$asset`" android:gravity=`"fill`" />`n",$utf8)
        $entries.Add([pscustomobject]@{id=$id;title=$title;resource=$resource;design=$design;color=$color})
    }
}
$list=($entries|Sort-Object @{Expression={if($_.id -eq 9){0}else{1}}},id|ForEach-Object{"        AppIcon($($_.id), `"$($_.title)`", R.drawable.$($_.resource), design=$($_.design), color=`"$($_.color)`")"}) -join ",`n"
$source=[regex]::Replace($source,'(?s)    val choices = listOf\(.*?\n    \)','    val choices = listOf('+"`n"+$list+"`n    )")
[IO.File]::WriteAllText($iconsFile,$source,$utf8)
$manifestFile=Join-Path $root 'android/app/src/main/AndroidManifest.xml'
$manifest=[IO.File]::ReadAllText($manifestFile)
$manifest=[regex]::Replace($manifest,'(?m)^\s*<activity-alias[^\n]*android:name=".Launcher\d+"[^\n]*\n','')
$aliases=($entries|Sort-Object id|ForEach-Object{$enabled=if($_.id -eq 9){'true'}else{'false'};"        <activity-alias android:name=`".Launcher$($_.id)`" android:targetActivity=`".MainActivity`" android:enabled=`"$enabled`" android:exported=`"true`" android:icon=`"@drawable/$($_.resource)`" android:label=`"@string/app_name`"><intent-filter><action android:name=`"android.intent.action.MAIN`"/><category android:name=`"android.intent.category.LAUNCHER`"/></intent-filter></activity-alias>"}) -join "`n"
$manifest=$manifest.Replace('</application>',"$aliases`n    </application>")
[IO.File]::WriteAllText($manifestFile,$manifest,$utf8)
$defaultIcon=Join-Path $root 'icons/icon_06/black/icon_512.png'
foreach($relative in @('frontend/public/brand-dark-waves.png','frontend/public/brand-trudomix.png','android/app/src/main/res/drawable-nodpi/brand_dark_waves.png','android/app/src/main/res/drawable-nodpi/trudomix_logo.png')){Copy-Item -LiteralPath $defaultIcon -Destination (Join-Path $root $relative) -Force}
Copy-Item -LiteralPath (Join-Path $root 'icons/icon_06/transparent/icon_512.png') -Destination (Join-Path $root 'android/app/src/main/res/drawable-nodpi/brand_transparent.png') -Force
Write-Host "Updated $($entries.Count) launcher choices, brand images and manifest aliases."

& node (Join-Path $PSScriptRoot "sync-svg-icons.mjs")
if ($LASTEXITCODE -ne 0) { throw "SVG import failed" }
