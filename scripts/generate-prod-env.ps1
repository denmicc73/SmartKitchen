$ErrorActionPreference = "Stop"

function New-Password {
    param([int] $Bytes = 24)

    $random = [byte[]]::new($Bytes)
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($random)
    return [Convert]::ToBase64String($random).TrimEnd("=") -replace "[+/]", "_"
}

$envPath = Join-Path (Get-Location) ".env"

if (Test-Path $envPath) {
    Write-Host ".env ya existe. No se ha sobrescrito."
    exit 0
}

$dbRootPass = New-Password
$dbPass = New-Password
$adminPass = New-Password

@"
DB_ROOT_PASS=$dbRootPass
DB_NAME=smartkitchen
DB_USER=smartkitchen
DB_PASS=$dbPass

ADMIN_USERNAME=admin
ADMIN_PASSWORD=$adminPass

ANTHROPIC_API_KEY=
"@ | Set-Content -Path $envPath -Encoding UTF8

Write-Host ".env creado."
Write-Host "Usuario admin: admin"
Write-Host "Contrasena admin: $adminPass"
Write-Host "Guarda esta contrasena; solo se muestra ahora."
