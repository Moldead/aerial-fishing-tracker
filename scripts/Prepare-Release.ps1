param()
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$releaseRoot = Join-Path $root 'release'
New-Item -ItemType Directory -Force -Path $releaseRoot | Out-Null
$destination = Join-Path $releaseRoot ('aerial-fishing-tracker-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $destination | Out-Null
# Explicit allowlist: exclude local clients, credentials, diagnostics and tracing data.
$files = @('.gitignore', 'README.md', 'LICENSE', 'NOTICE', 'build.gradle', 'settings.gradle',
    'gradlew', 'gradlew.bat', 'runelite-plugin.properties', 'Start-Tracker.cmd')
foreach ($file in $files) {
    Copy-Item -LiteralPath (Join-Path $root $file) -Destination $destination
}
foreach ($directory in @('src', 'gradle', 'docs')) {
    Copy-Item -LiteralPath (Join-Path $root $directory) -Destination $destination -Recurse
}
New-Item -ItemType Directory -Path (Join-Path $destination 'scripts') | Out-Null
foreach ($file in @('Start-Tracker.ps1', 'Prepare-Release.ps1')) {
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot $file) -Destination (Join-Path $destination 'scripts')
}
Compress-Archive -LiteralPath $destination -DestinationPath ($destination + '.zip')
Write-Output $destination
