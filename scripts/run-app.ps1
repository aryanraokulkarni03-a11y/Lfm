$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

$securePassword = Read-Host -Prompt 'Enter the MySQL password for campus_app' -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
$appExitCode = 1

try {
    $env:CAMPUS_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    Push-Location $projectRoot
    try {
        & (Join-Path $projectRoot 'mvnw.cmd') javafx:run
        $appExitCode = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    Remove-Item Env:CAMPUS_DB_PASSWORD -ErrorAction SilentlyContinue
}

exit $appExitCode
