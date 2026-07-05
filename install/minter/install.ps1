param(
    [switch]$SkipElevate
)

$ErrorActionPreference = "Stop"

$Repo = "orioneee/LMU-Assister"
$InstallerUrl = "https://raw.githubusercontent.com/$Repo/main/install/minter/install.ps1"
$MainClass = "com.orioooneee.lmuasister.minter.MainKt"

function Test-IsAdministrator {
    $Identity = [System.Security.Principal.WindowsIdentity]::GetCurrent()
    $Principal = [System.Security.Principal.WindowsPrincipal]::new($Identity)
    return $Principal.IsInRole([System.Security.Principal.WindowsBuiltInRole]::Administrator)
}

function ConvertTo-PowerShellLiteral {
    param([AllowNull()][string]$Value)

    if ($null -eq $Value) {
        return "''"
    }
    return "'" + $Value.Replace("'", "''") + "'"
}

function Invoke-ElevatedInstaller {
    if ($env:LMU_MINTER_NO_ELEVATE -eq "1") {
        return
    }
    if (Test-IsAdministrator) {
        return
    }
    if ($SkipElevate) {
        throw "Administrator rights are required to install and register the LMU Minter scheduled task."
    }

    Write-Host "Requesting administrator rights to install LMU Minter..."

    $Bootstrap = $null
    if ($PSCommandPath) {
        $ArgumentList = "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`" -SkipElevate"
    } else {
        $Bootstrap = Join-Path ([System.IO.Path]::GetTempPath()) ("lmu-minter-install-" + [System.Guid]::NewGuid().ToString("N") + ".ps1")
        $Lines = @('$ErrorActionPreference = "Stop"')
        foreach ($Name in @("LMU_MINTER_VERSION", "LMU_MINTER_PORT", "LMU_MINTER_HOME", "LMU_MINTER_NO_ELEVATE")) {
            $Value = [System.Environment]::GetEnvironmentVariable($Name, "Process")
            if (-not [string]::IsNullOrWhiteSpace($Value)) {
                $Lines += ('Set-Item -Path {0} -Value {1}' -f (ConvertTo-PowerShellLiteral "Env:$Name"), (ConvertTo-PowerShellLiteral $Value))
            }
        }
        $Lines += ('irm {0} | iex' -f (ConvertTo-PowerShellLiteral $InstallerUrl))
        Set-Content -Encoding ASCII -Path $Bootstrap -Value $Lines
        $ArgumentList = "-NoProfile -ExecutionPolicy Bypass -File `"$Bootstrap`""
    }

    try {
        $PowerShell = (Get-Command powershell.exe -ErrorAction SilentlyContinue).Source
        if ([string]::IsNullOrWhiteSpace($PowerShell)) {
            $PowerShell = "powershell.exe"
        }
        $Process = Start-Process -FilePath $PowerShell -ArgumentList $ArgumentList -Verb RunAs -Wait -PassThru
        if ($null -ne $Process.ExitCode -and $Process.ExitCode -ne 0) {
            exit $Process.ExitCode
        }
        exit 0
    } finally {
        if ($Bootstrap -and (Test-Path $Bootstrap)) {
            Remove-Item $Bootstrap -Force -ErrorAction SilentlyContinue
        }
    }
}

function Stop-ExistingMinterProcesses {
    param([string]$Path)

    $FullPath = [System.IO.Path]::GetFullPath($Path).TrimEnd([System.IO.Path]::DirectorySeparatorChar)
    $Processes = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue)
    foreach ($Process in $Processes) {
        if ($Process.ProcessId -eq $PID -or [string]::IsNullOrWhiteSpace($Process.CommandLine)) {
            continue
        }
        if ($Process.CommandLine.IndexOf($FullPath, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
            continue
        }

        Write-Host "Stopping existing LMU Minter process $($Process.ProcessId)"
        Stop-Process -Id $Process.ProcessId -Force -ErrorAction SilentlyContinue
    }
}

function Remove-InstallDirectory {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        return
    }

    for ($Attempt = 1; $Attempt -le 5; $Attempt++) {
        try {
            Remove-Item $Path -Recurse -Force
            return
        } catch {
            if ($Attempt -eq 5) {
                throw
            }
            Start-Sleep -Seconds 1
        }
    }
}

Invoke-ElevatedInstaller

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
        Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false -ErrorAction SilentlyContinue
    }

    Stop-ExistingMinterProcesses -Path $InstallDir
    Remove-InstallDirectory -Path $InstallDir
    New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
    Copy-Item -Path (Join-Path $SourceDir "*") -Destination $InstallDir -Recurse -Force

    $JavaLauncher = Join-Path $InstallDir "runtime\bin\javaw.exe"
    if (-not (Test-Path $JavaLauncher)) {
        throw "Hidden Java launcher not found: $JavaLauncher"
    }

    $Classpath = Join-Path $InstallDir "lib\*"
    $TaskUser = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
    $Action = New-ScheduledTaskAction `
        -Execute $JavaLauncher `
        -Argument "-cp `"$Classpath`" $MainClass $Port" `
        -WorkingDirectory $InstallDir
    $Trigger = New-ScheduledTaskTrigger -AtLogOn
    $Principal = New-ScheduledTaskPrincipal `
        -UserId $TaskUser `
        -LogonType Interactive `
        -RunLevel LeastPrivilege
    $Settings = New-ScheduledTaskSettingsSet `
        -AllowStartIfOnBatteries `
        -Hidden `
        -StartWhenAvailable `
        -RestartCount 3 `
        -RestartInterval (New-TimeSpan -Minutes 1)

    Register-ScheduledTask `
        -TaskName $TaskName `
        -Action $Action `
        -Trigger $Trigger `
        -Principal $Principal `
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
