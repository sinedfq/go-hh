# Optional per-machine development configuration; never placed in Git.
$otpLocalPath=Join-Path (Split-Path -Parent $PSScriptRoot) '.cache\otp.local.json'
if(Test-Path -LiteralPath $otpLocalPath){
    $otpLocal=Get-Content -LiteralPath $otpLocalPath -Raw | ConvertFrom-Json
    # A terminal may retain the old local mode from the previous script run.
    $upgradeLocalMode=$env:OTP_MODE -eq 'app_notification' -and $otpLocal.mode -eq 'firebase' -and $env:OTP_SECRET -eq $otpLocal.secret
    if(!$env:OTP_MODE -or $upgradeLocalMode){
        if($otpLocal.mode -notin @('app_notification','firebase') -or !$otpLocal.secret -or $otpLocal.secret.Length -lt 32){throw 'Invalid local OTP configuration'}
        $env:APP_ENV='development'
        $env:OTP_MODE=$otpLocal.mode
        $env:OTP_SECRET=$otpLocal.secret
        Write-Host "Temporary OTP delivery: $($otpLocal.mode) (development only)."
    }
}
