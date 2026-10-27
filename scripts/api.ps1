param([switch]$RequirePush)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'otp-local.ps1')
$pushConfigPath = Join-Path $projectRoot '.cache\push.local.json'
if (Test-Path -LiteralPath $pushConfigPath) {
    $pushConfig = Get-Content -LiteralPath $pushConfigPath -Raw | ConvertFrom-Json
    if (!$pushConfig.projectId -or !$pushConfig.credentialsPath) { throw 'Invalid local push configuration.' }
    if (!(Test-Path -LiteralPath $pushConfig.credentialsPath -PathType Leaf)) { throw 'Firebase server credentials are missing. See docs/PUSH_SETUP.md.' }
    $env:FIREBASE_PROJECT_ID = $pushConfig.projectId
    $env:GOOGLE_APPLICATION_CREDENTIALS = $pushConfig.credentialsPath
    Write-Host "Firebase project: $($pushConfig.projectId)"
} elseif ($RequirePush -and (!$env:FIREBASE_PROJECT_ID -or !$env:GOOGLE_APPLICATION_CREDENTIALS)) {
    throw 'Push is not configured. See docs/PUSH_SETUP.md.'
}
Push-Location $projectRoot
try {
    $apiPort=if($env:HTTP_ADDR -match ':(\d+)$'){[int]$Matches[1]}else{8080}
    $listener=Get-NetTCPConnection -LocalPort $apiPort -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if($listener){
        try{$health=Invoke-RestMethod "http://127.0.0.1:$apiPort/health" -TimeoutSec 3}catch{$health=$null}
        if($health.status -eq 'ok'){
            if($RequirePush -and !$health.push_enabled){throw "API is already running (PID $($listener.OwningProcess)), but does not confirm Firebase readiness. Restart that API with -RequirePush."}
            Write-Host "API is already running on port $apiPort (PID $($listener.OwningProcess)). Reusing the running server; no second instance started."
            return
        }
        throw "Port $apiPort is occupied by PID $($listener.OwningProcess). Stop that process or set HTTP_ADDR (for example :8082)."
    }
    & go run ./cmd/api
    if ($LASTEXITCODE -ne 0) { throw "API exited with code $LASTEXITCODE" }
} finally { Pop-Location }
