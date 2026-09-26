# Windows PowerShell 5.1 / PowerShell 7. Local MySQL bootstrap only.
$ErrorActionPreference = 'Stop'
# Native exit codes are checked explicitly, including under PowerShell 7.
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

function Invoke-SetupMysql {
    param([string[]]$ClientArguments, [string]$Sql, [string]$FailureMessage)
    # Windows PowerShell treats redirected native stderr as ErrorRecords.
    $ErrorActionPreference = 'Continue'
    $output = $Sql | & $mysqlExe @ClientArguments 2>&1
    $code = $LASTEXITCODE
    if ($code -ne 0) {
        foreach ($entry in $output) {
            $diagnostic = [string]$entry
            # Preserve server diagnostics without exposing literal credentials.
            $diagnostic = $diagnostic.Replace($passwordSql, '[REDACTED]').Replace($dbPassword, '[REDACTED]')
            [Console]::Error.WriteLine($diagnostic)
        }
        throw $FailureMessage
    }
}

$oldMysqlPassword = [Environment]::GetEnvironmentVariable('MYSQL_PWD', 'Process')
$oldOutputEncoding = $OutputEncoding
try {
    $envFile = Join-Path $PSScriptRoot '.env'
    if (-not (Test-Path -LiteralPath $envFile -PathType Leaf)) {
        throw ".env was not found. From backend/, run Copy-Item .env.example .env and configure it first."
    }
    $config = Read-BackendEnv $envFile
    foreach ($key in @('DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USERNAME', 'DB_PASSWORD')) {
        if ([string]::IsNullOrWhiteSpace($config[$key])) { throw "Required variable $key is missing or empty in .env." }
    }
    if ($config['DB_URL'] -or $env:DB_URL) {
        throw 'Unset DB_URL for setup; configure DB_HOST/DB_PORT/DB_NAME instead.'
    }
    $dbHost = $config['DB_HOST']
    $dbPort = $config['DB_PORT']
    $dbName = $config['DB_NAME']
    $dbUser = $config['DB_USERNAME']
    $dbPassword = $config['DB_PASSWORD']
    if ($dbHost -notin @('localhost', '127.0.0.1') -or $dbPort -ne '3306') {
        throw 'Like setup-db.sh, this helper supports local MySQL on port 3306. Provision remote/custom-port servers manually.'
    }
    if ($dbName -cnotmatch '^[A-Za-z0-9_]{1,64}$' -or $dbUser -cnotmatch '^[A-Za-z0-9_]{1,32}$') {
        throw 'Use only letters, digits and underscores for DB_NAME (1-64) and DB_USERNAME (1-32).'
    }
    if ($dbName -in @('mysql', 'sys', 'information_schema', 'performance_schema')) {
        throw 'DB_NAME must name an application database, not a MySQL system database.'
    }
    if ($dbUser -eq 'root' -or $dbUser -like 'mysql_*') {
        throw 'DB_USERNAME must be a dedicated application user, not root or a MySQL system account.'
    }

    $mysqlExe = $null
    if ($env:MYSQL_EXE) {
        if (-not (Test-Path -LiteralPath $env:MYSQL_EXE -PathType Leaf)) {
            throw 'MYSQL_EXE must point to an existing mysql.exe.'
        }
        $mysqlExe = (Resolve-Path -LiteralPath $env:MYSQL_EXE).Path
    } else {
        $client = Get-Command mysql.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($client) { $mysqlExe = $client.Source }
        if (-not $mysqlExe) {
            foreach ($programDirectory in @($env:ProgramFiles, ${env:ProgramFiles(x86)})) {
                if (-not $programDirectory) { continue }
                $clients = Get-ChildItem -Path (Join-Path $programDirectory 'MySQL\MySQL Server *\bin\mysql.exe') -File -ErrorAction SilentlyContinue | Sort-Object FullName -Descending
                if ($clients) { $mysqlExe = ($clients | Select-Object -First 1).FullName; break }
            }
        }
    }
    if (-not $mysqlExe) {
        throw 'MySQL client was not found. Install MySQL Community Server, add mysql.exe to PATH, or set MYSQL_EXE to its full path.'
    }
    $adminUser = Read-Host 'MySQL administrative username [root]'
    if ([string]::IsNullOrWhiteSpace($adminUser)) { $adminUser = 'root' }
    if ($adminUser -eq $dbUser) { throw 'The administrative username must differ from DB_USERNAME.' }

    $passwordSql = $dbPassword.Replace("'", "''")
    # Escape underscores because MySQL database GRANT names are patterns.
    $grantDatabase = $dbName.Replace('_', '\_')
    $sql = @"
SET SESSION sql_mode = 'NO_BACKSLASH_ESCAPES';
CREATE DATABASE IF NOT EXISTS ``$dbName`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$dbUser'@'localhost' IDENTIFIED BY '$passwordSql';
ALTER USER '$dbUser'@'localhost' IDENTIFIED BY '$passwordSql';
GRANT ALL PRIVILEGES ON ``$grantDatabase``.* TO '$dbUser'@'localhost';
"@
    # No FLUSH PRIVILEGES is needed for CREATE/ALTER USER and GRANT.
    $OutputEncoding = New-Object System.Text.UTF8Encoding($false)
    # Ignore saved client/login credentials so verification uses this .env password.
    $connection = @('--no-defaults', '--no-login-paths', '--protocol=TCP', "--host=$dbHost", "--port=$dbPort", '--connect-timeout=10', '--default-character-set=utf8mb4', '--batch', '--binary-mode')
    [Environment]::SetEnvironmentVariable('MYSQL_PWD', $null, 'Process')
    Write-Host 'Preparing local MySQL. Enter the administrator password at the MySQL prompt.'
    Invoke-SetupMysql -ClientArguments ($connection + @("--user=$adminUser", '--password')) -Sql $sql -FailureMessage 'Database setup failed. Check the MySQL service, administrative credentials/privileges, and the server diagnostic above.'

    # Only the application password is passed transiently via the child environment,
    # never via argv or a credential file. Restore the previous value in finally.
    [Environment]::SetEnvironmentVariable('MYSQL_PWD', $dbPassword, 'Process')
    Invoke-SetupMysql -ClientArguments ($connection + @("--user=$dbUser", "--database=$dbName")) -Sql 'SELECT 1;' -FailureMessage 'Application connection failed. Check the MySQL service, account authentication, and database privileges.'
    Write-Host "Database setup completed successfully.`nDatabase: $dbName`nHost: ${dbHost}:$dbPort`nApplication user: $dbUser"
} catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
} finally {
    [Environment]::SetEnvironmentVariable('MYSQL_PWD', $oldMysqlPassword, 'Process')
    $OutputEncoding = $oldOutputEncoding
}
