param(
    [string]$InstallDir = "$PSScriptRoot\.dotnet",
    [switch]$UseUserLocalDotnet = $true
)

$ErrorActionPreference = "Stop"

function Test-Command($Name) {
    return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Get-DotnetCommand {
    if (Test-Command "dotnet") {
        return "dotnet"
    }

    $localDotnet = Join-Path $InstallDir "dotnet.exe"
    if (Test-Path $localDotnet) {
        return $localDotnet
    }

    return $null
}

function Install-DotnetUserLocal {
    New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
    $installer = Join-Path $env:TEMP "dotnet-install.ps1"
    Invoke-WebRequest -Uri "https://dot.net/v1/dotnet-install.ps1" -OutFile $installer
    & powershell -NoProfile -ExecutionPolicy Bypass -File $installer -Channel 8.0 -InstallDir $InstallDir
}

$dotnet = Get-DotnetCommand
if ($null -eq $dotnet) {
    if (-not $UseUserLocalDotnet) {
        throw "dotnet was not found. Install .NET 8 SDK or run this script with -UseUserLocalDotnet."
    }

    Write-Host "dotnet was not found. Installing .NET SDK locally to $InstallDir ..."
    Install-DotnetUserLocal
    $dotnet = Get-DotnetCommand
}

if ($null -eq $dotnet) {
    throw "Unable to locate dotnet after installation."
}

Write-Host "Using dotnet: $dotnet"
& $dotnet --version

$project = Join-Path $PSScriptRoot "KmpPrinterBleWindows.csproj"
& $dotnet publish $project -c Release -r win-x64 --self-contained false

$output = Join-Path $PSScriptRoot "bin\Release\net8.0-windows10.0.19041.0\win-x64\publish\kmp-printer-ble-windows.exe"
Write-Host ""
Write-Host "BLE helper built:"
Write-Host $output
Write-Host ""
Write-Host "Use this path in PrinterConfig.bleBridgeCommand or add the publish folder to PATH."
