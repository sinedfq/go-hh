param([switch]$RequirePush)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
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
    & go run ./cmd/api
    if ($LASTEXITCODE -ne 0) { throw "API exited with code $LASTEXITCODE" }
} finally { Pop-Location }
