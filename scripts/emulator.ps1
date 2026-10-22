param(
    [string]$Name,
    [switch]$Visible,
    [string]$SdkPath,
    [ValidateRange(1024, 16384)][int]$MemoryMb = 2560,
    [ValidateRange(1, 16)][int]$Cores = 2,
    [switch]$WaitForBoot,
    [ValidateRange(30, 900)][int]$BootTimeoutSeconds = 360
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$settingsFile = Join-Path $projectRoot 'android\emulator.local.json'
$settings = if (Test-Path -LiteralPath $settingsFile) { Get-Content -LiteralPath $settingsFile -Raw | ConvertFrom-Json } else { $null }
if (!$Name) { $Name = if ($settings.defaultAvd) { $settings.defaultAvd } else { 'Pixel_7' } }
if ($Name -notmatch '^[A-Za-z0-9._-]+$') { throw 'Invalid AVD name' }
$androidSdk = if ($SdkPath) { $SdkPath } elseif ($settings.sdkPath) { $settings.sdkPath } elseif ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$emulator = Join-Path $androidSdk 'emulator\emulator.exe'
$adb = Join-Path $androidSdk 'platform-tools\adb.exe'
if (!(Test-Path -LiteralPath $emulator)) { throw 'Android Emulator is missing. Install it through Android Studio SDK Manager.' }

function Find-AvdSerial {
    foreach ($line in @(& $adb devices)) {
        if ($line -match '^(emulator-\d+)\s+(device|offline)$') {
            $candidate = $Matches[1]
            try {
                $avdName = @(& $adb -s $candidate emu avd name 2>$null)
                if ($avdName -contains $Name) { return $candidate }
            } catch { }
        }
    }
}

$previousSdkRoot = $env:ANDROID_SDK_ROOT
$previousAndroidHome = $env:ANDROID_HOME
try {
    $env:ANDROID_SDK_ROOT = $androidSdk
    $env:ANDROID_HOME = $androidSdk
    $available = @(& $emulator -list-avds)
    if ($Name -notin $available) { throw "AVD '$Name' not found. Available: $($available -join ', ')" }
    $serial = Find-AvdSerial
    $emulatorProcess = $null
    if ($serial) {
        Write-Host "AVD '$Name' is already running ($serial)."
    } else {
        $logDirectory = Join-Path $projectRoot '.cache'
        New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
        $stdout = Join-Path $logDirectory "emulator-$Name.stdout.log"
        $stderr = Join-Path $logDirectory "emulator-$Name.stderr.log"
        $arguments = @('-avd', $Name, '-no-snapshot', '-no-audio', '-gpu', 'swiftshader_indirect', '-memory', "$MemoryMb", '-cores', "$Cores")
        if (!$Visible) { $arguments += '-no-window' }
        $startOptions = @{ FilePath = $emulator; ArgumentList = $arguments; PassThru = $true; RedirectStandardOutput = $stdout; RedirectStandardError = $stderr }
        if (!$Visible) { $startOptions.WindowStyle = 'Hidden' }
        $emulatorProcess = Start-Process @startOptions
        Write-Host "Starting '$Name', PID $($emulatorProcess.Id), SDK $androidSdk"
        Write-Host "Logs: $stdout and $stderr"
    }
    if ($WaitForBoot) {
        $deadline = (Get-Date).AddSeconds($BootTimeoutSeconds)
        $nextReconnect = Get-Date
        do {
            if ($emulatorProcess -and $emulatorProcess.HasExited) { throw "Emulator exited with code $($emulatorProcess.ExitCode). Check the logs above." }
            $serial = Find-AvdSerial
            if ($serial) {
                try {
                    $booted = & $adb -s $serial shell getprop sys.boot_completed 2>$null
                    if ($booted -eq '1') { Write-Host "Android is ready ($serial)."; return }
                } catch { }
                if ((Get-Date) -ge $nextReconnect) {
                    try { & $adb -s $serial reconnect 2>$null | Out-Null } catch { }
                    $nextReconnect = (Get-Date).AddSeconds(20)
                }
            }
            Start-Sleep -Seconds 3
        } while ((Get-Date) -lt $deadline)
        throw 'Android did not finish booting before the timeout. Check the emulator logs.'
    }
    Write-Host 'Wait for the Android home screen, then run .\scripts\android.ps1 -Action run'
} finally {
    $env:ANDROID_SDK_ROOT = $previousSdkRoot
    $env:ANDROID_HOME = $previousAndroidHome
}
