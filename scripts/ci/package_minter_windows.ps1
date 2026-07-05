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
$FallbackModules = "java.base,java.instrument,java.management,java.naming,java.sql,jdk.httpserver,jdk.unsupported"

if (-not $env:JAVA_HOME) {
    $JavaCommand = Get-Command java -ErrorAction SilentlyContinue
    if (-not $JavaCommand) {
        throw "JAVA_HOME is not set and java was not found on PATH"
    }
    $JavaHomeLine = (& $JavaCommand.Source -XshowSettings:properties -version 2>&1 | Select-String "java.home =").Line
    if (-not $JavaHomeLine) {
        throw "Could not resolve java.home from java on PATH"
    }
    $JavaHomeParts = $JavaHomeLine -split "=", 2
    if ($JavaHomeParts.Count -lt 2 -or -not $JavaHomeParts[1]) {
        throw "Could not parse java.home from java on PATH"
    }
    $env:JAVA_HOME = $JavaHomeParts[1].Trim()
}

if (Test-Path $WorkDir) {
    Remove-Item $WorkDir -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $ReleaseDir | Out-Null
New-Item -ItemType Directory -Force -Path "$WorkDir\bin" | Out-Null
New-Item -ItemType Directory -Force -Path "$WorkDir\lib" | Out-Null
Copy-Item "$DistDir\lib\*" "$WorkDir\lib" -Recurse -Force

$Jars = @(Get-ChildItem "$WorkDir\lib" -Filter "*.jar" | ForEach-Object { $_.FullName })
if ($Jars.Count -eq 0) {
    throw "No jars found in $WorkDir\lib"
}

$JdepsOutput = @(& "$env:JAVA_HOME\bin\jdeps.exe" `
    --multi-release 21 `
    --ignore-missing-deps `
    --recursive `
    --print-module-deps `
    --class-path "$WorkDir\lib\*" `
    @Jars 2>$null)

$ModulesLine = @(
    $JdepsOutput |
        ForEach-Object { if ($null -ne $_) { $_.ToString().Trim() } } |
        Where-Object { $_ }
) | Select-Object -Last 1
$Modules = if ($ModulesLine) { $ModulesLine.ToString().Trim() -replace "\s", "" } else { "" }
if (-not $Modules -or $Modules.StartsWith("Warning:")) {
    $Modules = $FallbackModules
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
Set fso = CreateObject("Scripting.FileSystemObject")
Set shell = CreateObject("WScript.Shell")

appHome = fso.GetParentFolderName(fso.GetParentFolderName(WScript.ScriptFullName))
javaw = fso.BuildPath(appHome, "runtime\bin\javaw.exe")
classpath = fso.BuildPath(appHome, "lib\*")
args = ""

For Each arg In WScript.Arguments
    args = args & " " & Quote(arg)
Next

shell.CurrentDirectory = appHome
shell.Run Quote(javaw) & " -cp " & Quote(classpath) & " $MainClass" & args, 0, False

Function Quote(value)
    Quote = Chr(34) & Replace(value, Chr(34), Chr(34) & Chr(34)) & Chr(34)
End Function
"@ | Set-Content -Encoding ASCII "$WorkDir\bin\lmu-minter.vbs"

@"
LMU Assister JVM Minter

Run:
  wscript bin\lmu-minter.vbs

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
