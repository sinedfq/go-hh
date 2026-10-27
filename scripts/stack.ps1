param([ValidateSet('up','down','status','logs')][string]$Action='up')
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'otp-local.ps1')
$composeArgs=@('compose','-f',(Join-Path $projectRoot 'docker-compose.yml'))
if(Test-Path -LiteralPath (Join-Path $projectRoot '.cache/firebase-service-account.json')) {
    $composeArgs+=@('-f',(Join-Path $projectRoot 'docker-compose.firebase.yml'))
    $localPush=Join-Path $projectRoot '.cache/push.local.json'
    if(Test-Path -LiteralPath $localPush) {
        $config=Get-Content -LiteralPath $localPush -Raw|ConvertFrom-Json
        if($config.projectId){$env:FIREBASE_PROJECT_ID=$config.projectId}
    }
}
Push-Location $projectRoot
try {
    if($env:OS -eq 'Windows_NT') {
        if($Action -eq 'up') {
            $apiContainer=& docker @composeArgs ps -aq api
            if($LASTEXITCODE -ne 0){throw 'Cannot inspect existing Docker stack'}
            if($apiContainer) {
                $existing=(& docker inspect $apiContainer|ConvertFrom-Json)[0]
                if(($existing.Mounts|Where-Object Destination -eq '/app/uploads').Type -eq 'volume') {
                    & docker @composeArgs cp 'api:/app/uploads/.' (Join-Path $projectRoot 'uploads')
                    if($LASTEXITCODE -ne 0){throw 'Cannot preserve existing Docker uploads'}
                }
            }
        }
        $composeArgs+=@('-f',(Join-Path $projectRoot 'docker-compose.windows.yml'))
    }
    switch($Action) {
        'up' { & docker @composeArgs up -d --build --wait --wait-timeout 120 }
        'down' { & docker @composeArgs down }
        'status' { & docker @composeArgs ps }
        'logs' { & docker @composeArgs logs --tail 100 -f api frontend migrate }
    }
    if($LASTEXITCODE -ne 0){throw "Docker Compose exited with code $LASTEXITCODE"}
    if($Action -eq 'up') { Write-Host 'Trudomix is running. Default website: http://localhost:3000; API: http://localhost:8081. Port settings: .env.' }
}finally{Pop-Location}
