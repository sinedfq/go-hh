$ErrorActionPreference='Stop'
$root=Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$global:LASTEXITCODE=0
function global:go { $global:OtpResetArguments=@($args) }
try{
 & (Join-Path $root 'scripts/otp-reset.ps1') -All
 if(($global:OtpResetArguments -join '|') -ne 'run|./cmd/otp-reset|--all'){throw '-All was not forwarded as one argument'}
 & (Join-Path $root 'scripts/otp-reset.ps1') -Phone '+79990000001'
 if(($global:OtpResetArguments -join '|') -ne 'run|./cmd/otp-reset|--phone|+79990000001'){throw '-Phone was not forwarded correctly'}
 Write-Output 'OTP reset argument checks passed (no database writes).'
}finally{Remove-Item Function:\go;Remove-Variable OtpResetArguments -Scope Global -ErrorAction SilentlyContinue}
