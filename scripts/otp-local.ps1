# Optional per-machine development configuration; never placed in Git.
$otpLocalPath=Join-Path (Split-Path -Parent $PSScriptRoot) '.cache\otp.local.json'
if((Test-Path -LiteralPath $otpLocalPath) -and !$env:OTP_MODE){
    $otpLocal=Get-Content -LiteralPath $otpLocalPath -Raw | ConvertFrom-Json
    if($otpLocal.mode -ne 'app_notification' -or !$otpLocal.secret -or $otpLocal.secret.Length -lt 32){throw 'Invalid local OTP configuration'}
    $env:APP_ENV='development'
    $env:OTP_MODE='app_notification'
    $env:OTP_SECRET=$otpLocal.secret
    Write-Host 'Temporary OTP delivery: app notifications (development only).'
}
