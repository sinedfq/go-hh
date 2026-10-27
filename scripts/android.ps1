param(
    [ValidateSet('build', 'install', 'test', 'device-test', 'run')]
    [string]$Action = 'build',
    [string]$ApiBaseUrl = 'http://10.0.2.2:8080/',
    [ValidateRange(0,2100000000)][int]$BuildNumber = 0
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.cache\gradle'
$androidSdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $androidSdk 'platform-tools\adb.exe'
if (!$ApiBaseUrl.EndsWith('/')) { throw 'ApiBaseUrl must end with /' }
$tasks = @(switch ($Action) {
    'build' { @(':app:assembleDebug') }
    'install' { @(':app:installDebug') }
    'test' { @(':app:testDebugUnitTest') }
    'device-test' { @(':app:connectedDebugAndroidTest') }
    'run' { @(':app:installDebug') }
})
$packaging=$Action -in @('build','install','run')
if($packaging) {
    $counter=Join-Path $projectRoot '.cache/android-build-number.txt'
    New-Item -ItemType Directory -Force (Split-Path $counter) | Out-Null
    # An exclusive file handle prevents concurrent builds from reserving one number.
    $lease=[System.IO.File]::Open($counter,[System.IO.FileMode]::OpenOrCreate,[System.IO.FileAccess]::ReadWrite,[System.IO.FileShare]::None)
    try {
        $buffer=New-Object byte[] $lease.Length
        [void]$lease.Read($buffer,0,$buffer.Length)
        $text=[System.Text.Encoding]::UTF8.GetString($buffer).Trim()
        $previous=if($text){[int]$text}else{1}
        if(!$BuildNumber){$BuildNumber=$previous+1}
        if($BuildNumber -gt 2100000000){throw 'Android build number limit reached'}
        $next=[System.Text.Encoding]::UTF8.GetBytes([string][Math]::Max($previous,$BuildNumber))
        $lease.Position=0;$lease.SetLength(0);$lease.Write($next,0,$next.Length)
    }finally{$lease.Dispose()}
    Write-Host "Building Trudomix #$BuildNumber"
}
Push-Location (Join-Path $projectRoot 'android')
try {
    $gradleArgs=@($tasks)+@("-PAPI_BASE_URL=$ApiBaseUrl",'--console=plain')
    if($packaging){$gradleArgs+="-PBUILD_NUMBER=$BuildNumber"}
    & .\gradlew.bat @gradleArgs
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $LASTEXITCODE" }
    if($packaging) {
        $output=Join-Path $projectRoot 'android/app/build/outputs/apk/debug'
        $metadata=Get-Content -LiteralPath (Join-Path $output 'output-metadata.json') -Raw | ConvertFrom-Json
        $element=$metadata.elements | Select-Object -First 1
        if([int]$element.versionCode -ne $BuildNumber){throw 'APK build number does not match the reserved number'}
        $numbered=Join-Path $output ("Trudomix-{0}-build-{1:D4}-debug.apk" -f $element.versionName,$BuildNumber)
        Copy-Item -LiteralPath (Join-Path $output $element.outputFile) -Destination $numbered -Force
        Write-Host "APK #${BuildNumber}: $numbered"
    }
    if ($Action -eq 'run') {
        & $adb shell am start -n 'com.example.trudomix/.MainActivity'
        if ($LASTEXITCODE -ne 0) { throw 'Could not start the app. Check adb devices.' }
    }
} finally { Pop-Location }
