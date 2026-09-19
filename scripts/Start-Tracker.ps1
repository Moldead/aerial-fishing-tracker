param([switch]$BuildOnly)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$portableJava = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools/jdk17') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
if ($portableJava) {
    $env:JAVA_HOME = $portableJava.FullName
}
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/javac.exe'))) {
    throw 'Set JAVA_HOME to a JDK 17 directory, then run this script again. See README.md.'
}
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.tools/gradle-cache'
Push-Location -LiteralPath $projectRoot
try {
    if ($BuildOnly) {
        & .\gradlew.bat clean test build --console=plain --no-daemon
    } else {
        & .\gradlew.bat run --console=plain --no-daemon
    }
    if ($LASTEXITCODE -ne 0) { throw "Gradle exited with code $LASTEXITCODE. See the output above." }
} finally {
    Pop-Location
}
