$ErrorActionPreference = "Stop"

$Repo = "orioneee/LMU-Assister"
$Version = $env:LMU_MINTER_VERSION
if ([string]::IsNullOrWhiteSpace($Version)) {
    $Version = "latest"
}

$Port = 8787
if (-not [string]::IsNullOrWhiteSpace($env:LMU_MINTER_PORT)) {
    $Port = [int]$env:LMU_MINTER_PORT
}

$Platform = "windows-x64"
$Artifact = "lmu-minter-$Platform.zip"
if ($Version -eq "latest") {
    $Url = "https://github.com/$Repo/releases/latest/download/$Artifact"
} else {
    $Url = "https://github.com/$Repo/releases/download/$Version/$Artifact"
}

$InstallDir = $env:LMU_MINTER_HOME
if ([string]::IsNullOrWhiteSpace($InstallDir)) {
    $InstallDir = Join-Path $env:LOCALAPPDATA "LMU Assister\Minter"
}

$TaskName = "LMU Assister Minter"
$TempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("lmu-minter-" + [System.Guid]::NewGuid().ToString("N"))

try {
    New-Item -ItemType Directory -Force -Path $TempDir | Out-Null
    $Archive = Join-Path $TempDir $Artifact
    $ExtractDir = Join-Path $TempDir "extract"

    Write-Host "Downloading $Artifact"
    Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $Archive

    Expand-Archive -Path $Archive -DestinationPath $ExtractDir -Force
    $SourceDir = Join-Path $ExtractDir "lmu-minter-$Platform"
    if (-not (Test-Path $SourceDir)) {
        throw "Archive layout is invalid: $SourceDir not found"
    }

    Write-Host "Installing to $InstallDir"
    $ExistingTask = Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
    if ($ExistingTask) {
        Stop-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
    }

    if (Test-Path $InstallDir) {
        Remove-Item $InstallDir -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
    Copy-Item -Path (Join-Path $SourceDir "*") -Destination $InstallDir -Recurse -Force

    $Launcher = Join-Path $InstallDir "bin\lmu-minter.bat"
    if (-not (Test-Path $Launcher)) {
        throw "Launcher not found: $Launcher"
    }

    $Action = New-ScheduledTaskAction -Execute $Launcher -Argument "$Port" -WorkingDirectory $InstallDir
    $Trigger = New-ScheduledTaskTrigger -AtLogOn
    $Settings = New-ScheduledTaskSettingsSet `
        -AllowStartIfOnBatteries `
        -StartWhenAvailable `
        -RestartCount 3 `
        -RestartInterval (New-TimeSpan -Minutes 1)

    Register-ScheduledTask `
        -TaskName $TaskName `
        -Action $Action `
        -Trigger $Trigger `
        -Settings $Settings `
        -Description "Local Steam sign-in helper for LMU Assister Web." `
        -Force | Out-Null

    Start-ScheduledTask -TaskName $TaskName

    $HealthUrl = "http://127.0.0.1:$Port/health"
    for ($i = 0; $i -lt 20; $i++) {
        try {
            $Response = Invoke-WebRequest -UseBasicParsing -Uri $HealthUrl -TimeoutSec 2
            if ($Response.StatusCode -eq 200) {
                Write-Host "LMU Minter is installed and running: $HealthUrl"
                exit 0
            }
        } catch {
        }
        Start-Sleep -Seconds 1
    }

    Write-Host "LMU Minter is installed, but health check did not answer yet: $HealthUrl"
} finally {
    if (Test-Path $TempDir) {
        Remove-Item $TempDir -Recurse -Force
    }
}
