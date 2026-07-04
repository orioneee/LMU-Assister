param(
    [Parameter(Mandatory = $true)]
    [string]$Platform,

    [Parameter(Mandatory = $true)]
    [string]$Archive,

    [string]$DistDir = "jvm-minter\build\install\jvm-minter"
)

$ErrorActionPreference = "Stop"

$MainClass = "com.orioooneee.lmuasister.minter.MainKt"
$ArtifactName = "lmu-minter-$Platform"
$ReleaseDir = "build\minter-release"
$WorkDir = Join-Path $ReleaseDir $ArtifactName

if (-not $env:JAVA_HOME) {
    $JavaCommand = Get-Command java -ErrorAction SilentlyContinue
    if (-not $JavaCommand) {
        throw "JAVA_HOME is not set and java was not found on PATH"
    }
    $JavaHomeLine = (& $JavaCommand.Source -XshowSettings:properties -version 2>&1 | Select-String "java.home =").Line
    $env:JAVA_HOME = ($JavaHomeLine -split "=", 2)[1].Trim()
}

if (Test-Path $WorkDir) {
    Remove-Item $WorkDir -Recurse -Force
}

New-Item -ItemType Directory -Force -Path "$WorkDir\bin" | Out-Null
New-Item -ItemType Directory -Force -Path "$WorkDir\lib" | Out-Null
Copy-Item "$DistDir\lib\*" "$WorkDir\lib" -Recurse -Force

$Jars = Get-ChildItem "$WorkDir\lib" -Filter "*.jar" | ForEach-Object { $_.FullName }
$JdepsOutput = & "$env:JAVA_HOME\bin\jdeps.exe" `
    --multi-release 21 `
    --ignore-missing-deps `
    --recursive `
    --print-module-deps `
    --class-path "$WorkDir\lib\*" `
    @Jars 2>$null

$Modules = ($JdepsOutput | Select-Object -Last 1).Trim()
if (-not $Modules -or $Modules.StartsWith("Warning:")) {
    $Modules = "java.base,java.instrument,java.management,java.naming,java.sql,jdk.httpserver,jdk.unsupported"
}

foreach ($Module in @("jdk.crypto.ec", "java.security.jgss")) {
    if (",${Modules}," -notlike "*,${Module},*") {
        $Modules = "$Modules,$Module"
    }
}

& "$env:JAVA_HOME\bin\jlink.exe" `
    --add-modules $Modules `
    --strip-debug `
    --no-header-files `
    --no-man-pages `
    --compress=2 `
    --output "$WorkDir\runtime"

@"
@echo off
set APP_HOME=%~dp0..
"%APP_HOME%\runtime\bin\java.exe" -cp "%APP_HOME%\lib\*" $MainClass %*
"@ | Set-Content -Encoding ASCII "$WorkDir\bin\lmu-minter.bat"

@"
LMU Assister JVM Minter

Run:
  bin\lmu-minter.bat

The runtime directory is bundled. Java does not need to be installed separately.
"@ | Set-Content -Encoding ASCII "$WorkDir\README.txt"

if ($Archive -ne "zip") {
    throw "Unsupported archive type: $Archive"
}

$ArchivePath = "$ReleaseDir\$ArtifactName.zip"
if (Test-Path $ArchivePath) {
    Remove-Item $ArchivePath -Force
}
Compress-Archive -Path $WorkDir -DestinationPath $ArchivePath
