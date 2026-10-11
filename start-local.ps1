$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (-not (Test-Path -LiteralPath '.env')) { throw 'Create .env from .env.example first.' }
Get-Content -LiteralPath '.env' | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2], 'Process') }
}
$focusJdk = Join-Path $env:TEMP 'focusguard-jdk21\jdk-21.0.12.1+1'
if (Test-Path -LiteralPath $focusJdk) { $env:JAVA_HOME = $focusJdk }
if (-not $env:JAVA_HOME) { throw 'Set JAVA_HOME to a Java 21 JDK.' }
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
& .\mvnw.cmd -q -DskipTests package
if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' }
& "$env:JAVA_HOME\bin\java.exe" -jar 'target\FocusGuard-Server-0.0.1-SNAPSHOT.jar'
