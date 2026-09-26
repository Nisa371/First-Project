# Windows PowerShell 5.1 / PowerShell 7. Run from any working directory.
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false

# Parse literal dotenv values; never execute or expand their contents.
function Read-BackendEnv {
    param([string]$Path)
    $values = @{}
    $lineNumber = 0
    foreach ($line in [IO.File]::ReadAllLines($Path)) {
        $lineNumber++
        if ($line -match '^\s*(#|$)') { continue }
        if ($line -notmatch '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
            throw "Invalid .env format at line $lineNumber; use KEY=value."
        }
        $key = $Matches[1]
        $value = $Matches[2]
        if ($value.StartsWith('"') -or $value.StartsWith("'")) {
            if ($value.Length -lt 2 -or $value[$value.Length - 1] -ne $value[0]) {
                throw "Unmatched quote in .env at line $lineNumber."
            }
            $value = $value.Substring(1, $value.Length - 2)
        }
        if ($value.IndexOf([char]0) -ge 0) {
            throw "Invalid null character in .env at line $lineNumber."
        }
        $values[$key] = $value
    }
    return $values
}

$backendExitCode = 1
$locationPushed = $false
try {
    $envFile = Join-Path $PSScriptRoot '.env'
    if (-not (Test-Path -LiteralPath $envFile -PathType Leaf)) {
        throw ".env was not found.`nCopy .env.example to .env and configure it first."
    }
    $config = Read-BackendEnv $envFile
    # Explicit .env entries are authoritative for this Windows launch.
    foreach ($key in $config.Keys) {
        [Environment]::SetEnvironmentVariable($key, $config[$key], 'Process')
    }
    foreach ($key in @('DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USERNAME', 'DB_PASSWORD')) {
        if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($key, 'Process'))) {
            throw "Required variable $key is missing or empty. Configure it in .env."
        }
    }
    $portNumber = 0
    if (-not [int]::TryParse($env:DB_PORT, [ref]$portNumber) -or $portNumber -lt 1 -or $portNumber -gt 65535) {
        throw 'DB_PORT must be a number between 1 and 65535.'
    }
    if ($env:DB_HOST -notmatch '^[A-Za-z0-9._:-]+$') { throw 'DB_HOST must be a hostname or IP address.' }
    if ($env:DB_NAME -cnotmatch '^[A-Za-z0-9_]{1,64}$') { throw 'DB_NAME must contain 1-64 letters, digits or underscores.' }
    if ($env:DB_USERNAME -eq 'root') { throw 'Use a dedicated DB_USERNAME, not root.' }
    if ($env:AI_PROVIDER -eq 'GEMINI' -and [string]::IsNullOrWhiteSpace($env:GEMINI_API_KEY)) {
        throw 'GEMINI_API_KEY is required when AI_PROVIDER=GEMINI. Configure it in .env.'
    }
    $wrapper = Join-Path $PSScriptRoot 'mvnw.cmd'
    if (-not (Test-Path -LiteralPath $wrapper -PathType Leaf)) { throw 'Maven wrapper mvnw.cmd was not found in backend/.' }
    if ($env:JAVA_HOME) {
        if (-not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe') -PathType Leaf)) {
            throw 'JAVA_HOME does not contain bin\java.exe. Set JAVA_HOME to your JDK 21 installation.'
        }
    } elseif (-not (Get-Command java.exe -CommandType Application -ErrorAction SilentlyContinue)) {
        throw 'Java was not found. Install JDK 21 and set JAVA_HOME or add its bin directory to PATH.'
    }
    Push-Location -LiteralPath $PSScriptRoot
    $locationPushed = $true
    Write-Host 'Starting Spring Boot...'
    # Keep Maven/Java attached to this console so Ctrl+C reaches the backend.
    & $wrapper spring-boot:run @args
    $backendExitCode = $LASTEXITCODE
} catch {
    [Console]::Error.WriteLine($_.Exception.Message)
} finally {
    if ($locationPushed) { Pop-Location }
}
exit $backendExitCode
