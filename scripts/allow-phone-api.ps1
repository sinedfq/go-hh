#Requires -RunAsAdministrator
param(
    [string]$InterfaceAlias,
    [ValidateRange(1024, 65535)][int]$Port = 8080
)
$ErrorActionPreference = 'Stop'
$interfaces = @(Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway -and $_.NetAdapter.HardwareInterface -and (!$InterfaceAlias -or $_.InterfaceAlias -eq $InterfaceAlias) })
if ($interfaces.Count -ne 1) { throw 'Specify the local Wi-Fi interface using -InterfaceAlias. No firewall changes were made.' }
$network = $interfaces[0]
$address = @($network.IPv4Address | Where-Object { $_.IPAddress -notlike '169.254.*' })[0].IPAddress
if (!$address) { throw 'The selected interface has no IPv4 address.' }
$category = (Get-NetConnectionProfile -InterfaceIndex $network.InterfaceIndex).NetworkCategory.ToString()
$profile = if ($category -eq 'DomainAuthenticated') { 'Domain' } else { $category }
$ruleName = "Trudomix-API-WiFi-$Port"
$rule = Get-NetFirewallRule -Name $ruleName -ErrorAction SilentlyContinue
if ($rule) {
    Set-NetFirewallRule -Name $ruleName -InterfaceAlias $network.InterfaceAlias -Profile $profile -Enabled True -Action Allow
    $rule | Get-NetFirewallAddressFilter | Set-NetFirewallAddressFilter -LocalAddress $address -RemoteAddress LocalSubnet
} else {
    New-NetFirewallRule -Name $ruleName -DisplayName "Trudomix API: local network TCP $Port" -Direction Inbound -Action Allow -Protocol TCP -LocalPort $Port -LocalAddress $address -RemoteAddress LocalSubnet -InterfaceAlias $network.InterfaceAlias -Profile $profile | Out-Null
}
$message = "Allowed TCP $Port on $address ($($network.InterfaceAlias), $profile) from LocalSubnet only."
Write-Output $message
$projectRoot = Split-Path -Parent $PSScriptRoot
$logDirectory = Join-Path $projectRoot '.cache'
New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
$message | Set-Content -LiteralPath (Join-Path $logDirectory 'phone-firewall.txt') -Encoding UTF8
