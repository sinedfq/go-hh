param([string]$Phone,[switch]$All)
$ErrorActionPreference='Stop'
if((!$Phone -and !$All) -or ($Phone -and $All)){throw 'Specify -Phone or -All'}
. (Join-Path $PSScriptRoot 'otp-local.ps1')
$root=Split-Path -Parent $PSScriptRoot
Push-Location $root
try{
    [string[]]$arguments=@()
    if($All){$arguments+= '--all'}else{$arguments+=@('--phone',$Phone)}
    & go run ./cmd/otp-reset @arguments
    if($LASTEXITCODE -ne 0){throw 'OTP reset failed'}
}finally{Pop-Location}
