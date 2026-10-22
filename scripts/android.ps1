param(
    [ValidateSet('build', 'install', 'test', 'device-test', 'run')]
    [string]$Action = 'build',
    [string]$ApiBaseUrl = 'http://10.0.2.2:8080/'
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
Push-Location (Join-Path $projectRoot 'android')
try {
    & .\gradlew.bat @tasks "-PAPI_BASE_URL=$ApiBaseUrl" '--console=plain'
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $LASTEXITCODE" }
    if ($Action -eq 'run') {
        & $adb shell am start -n 'com.example.trudomix/.MainActivity'
        if ($LASTEXITCODE -ne 0) { throw 'Could not start the app. Check adb devices.' }
    }
} finally { Pop-Location }
