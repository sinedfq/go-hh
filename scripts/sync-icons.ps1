# Import the approved originals and generate Android launcher density resources.
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
Add-Type -AssemblyName System.Drawing
function Save-LauncherPng($source,$target,[int]$size,[int]$padding=0) {
    $bitmap=[Drawing.Bitmap]::new($size,$size,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics=[Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.Clear([Drawing.Color]::Transparent)
        $graphics.CompositingMode=[Drawing.Drawing2D.CompositingMode]::SourceCopy
        $graphics.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $graphics.DrawImage($source,[Drawing.Rectangle]::new($padding,$padding,$size-2*$padding,$size-2*$padding))
        $bitmap.Save($target,[Drawing.Imaging.ImageFormat]::Png)
    } finally { $graphics.Dispose();$bitmap.Dispose() }
}
$night=[Drawing.Bitmap]::new((Join-Path $root 'icons/Night_Wave_2048.png'))
$light=[Drawing.Bitmap]::new((Join-Path $root 'icons/White_Wave_2048.png'))
try {
    foreach($entry in @(@('night',$night),@('light',$light))) {
        Save-LauncherPng $entry[1] (Join-Path $root "android/app/src/main/res/drawable-nodpi/trudomix_wave_$($entry[0]).png") 512
    }
    foreach($density in @(@('mdpi',1),@('hdpi',1.5),@('xhdpi',2),@('xxhdpi',3),@('xxxhdpi',4))) {
        $name=$density[0];$scale=[double]$density[1]
        $mipmap=Join-Path $root "android/app/src/main/res/mipmap-$name"
        foreach($resource in @('ic_launcher','ic_launcher_round','ic_launcher_night')) {
            Save-LauncherPng $night (Join-Path $mipmap "$resource.png") ([int](48*$scale))
        }
        Save-LauncherPng $light (Join-Path $mipmap 'ic_launcher_light.png') ([int](48*$scale))
        $drawable=Join-Path $root "android/app/src/main/res/drawable-$name/trudomix_wave_launcher.png"
        Save-LauncherPng $night $drawable ([int](108*$scale)) ([int](16*$scale))
    }
} finally { $night.Dispose();$light.Dispose() }
Write-Host 'Updated launcher densities from the same transparent originals.'

& node (Join-Path $PSScriptRoot 'sync-wave-icons.mjs')
if ($LASTEXITCODE -ne 0) { throw 'Wave branding import failed' }
