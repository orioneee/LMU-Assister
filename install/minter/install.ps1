param(
    [switch]$SkipElevate,
    [string]$LogPath
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
try {
    [System.Net.ServicePointManager]::SecurityProtocol =
        [System.Net.ServicePointManager]::SecurityProtocol -bor [System.Net.SecurityProtocolType]::Tls12
} catch {
}

$Repo = "orioneee/LMU-Assister"
$InstallerUrl = "https://raw.githubusercontent.com/$Repo/main/install/minter/install.ps1"
$MainClass = "com.orioooneee.lmuasister.minter.MainKt"
$Script:InstallLogPath = if (-not [string]::IsNullOrWhiteSpace($LogPath)) {
    $LogPath
} elseif (-not [string]::IsNullOrWhiteSpace($env:LMU_MINTER_LOG)) {
    $env:LMU_MINTER_LOG
} else {
    $null
}

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

function Add-InstallLogLine {
    param([string]$Line)

    if ([string]::IsNullOrWhiteSpace($Script:InstallLogPath)) {
        return
    }

    try {
        $Parent = Split-Path -Parent $Script:InstallLogPath
        if (-not [string]::IsNullOrWhiteSpace($Parent)) {
            New-Item -ItemType Directory -Force -Path $Parent | Out-Null
        }
        Add-Content -Encoding UTF8 -Path $Script:InstallLogPath -Value $Line
    } catch {
    }
}

function Write-InstallProgress {
    param(
        [int]$Percent,
        [string]$Message
    )

    $ClampedPercent = [Math]::Max(0, [Math]::Min(100, $Percent))
    $Remaining = 100 - $ClampedPercent
    $Line = "[{0,3}% | {1,3}% left] {2}" -f $ClampedPercent, $Remaining, $Message
    Write-Host $Line
    Add-InstallLogLine $Line
}

function Write-InstallFailure {
    param([System.Management.Automation.ErrorRecord]$ErrorRecord)

    $Message = if ($ErrorRecord.Exception.Message) { $ErrorRecord.Exception.Message } else { $ErrorRecord.ToString() }
    $Line = "[ERROR] $Message"
    Write-Host $Line -ForegroundColor Red
    Add-InstallLogLine $Line
    Add-InstallLogLine ($ErrorRecord | Out-String)
}

function Show-InstallLogTail {
    param(
        [string]$Path,
        [int]$Count = 40
    )

    if (-not (Test-Path $Path)) {
        return
    }

    Write-Host ""
    Write-Host "Last installer log lines:"
    Get-Content -Path $Path -Tail $Count -ErrorAction SilentlyContinue | ForEach-Object {
        Write-Host $_
    }
}

function Wait-ElevatedInstaller {
    param(
        [System.Diagnostics.Process]$Process,
        [string]$Path
    )

    $Printed = 0
    while (-not $Process.HasExited) {
        if (Test-Path $Path) {
            $Lines = @(Get-Content -Path $Path -ErrorAction SilentlyContinue)
            for ($Index = $Printed; $Index -lt $Lines.Count; $Index++) {
                Write-Host $Lines[$Index]
            }
            $Printed = $Lines.Count
        }
        Start-Sleep -Milliseconds 500
        $Process.Refresh()
    }

    if (Test-Path $Path) {
        $Lines = @(Get-Content -Path $Path -ErrorAction SilentlyContinue)
        for ($Index = $Printed; $Index -lt $Lines.Count; $Index++) {
            Write-Host $Lines[$Index]
        }
    }
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

    $ElevatedLogPath = Join-Path ([System.IO.Path]::GetTempPath()) ("lmu-minter-install-" + [System.Guid]::NewGuid().ToString("N") + ".log")
    Write-InstallProgress 1 "Requesting administrator rights"

    $Bootstrap = $null
    if ($PSCommandPath) {
        $ArgumentList = "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`" -SkipElevate -LogPath `"$ElevatedLogPath`""
    } else {
        $Bootstrap = Join-Path ([System.IO.Path]::GetTempPath()) ("lmu-minter-install-" + [System.Guid]::NewGuid().ToString("N") + ".ps1")
        $Lines = @('$ErrorActionPreference = "Stop"')
        $Lines += 'try { [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor [System.Net.SecurityProtocolType]::Tls12 } catch {}'
        $Lines += ('$env:LMU_MINTER_LOG = {0}' -f (ConvertTo-PowerShellLiteral $ElevatedLogPath))
        foreach ($Name in @("LMU_MINTER_VERSION", "LMU_MINTER_PORT", "LMU_MINTER_HOME", "LMU_MINTER_NO_ELEVATE")) {
            $Value = [System.Environment]::GetEnvironmentVariable($Name, "Process")
            if (-not [string]::IsNullOrWhiteSpace($Value)) {
                $Lines += ('Set-Item -Path {0} -Value {1}' -f (ConvertTo-PowerShellLiteral "Env:$Name"), (ConvertTo-PowerShellLiteral $Value))
            }
        }
        $Lines += 'try {'
        $Lines += ('    $Installer = Invoke-RestMethod -UseBasicParsing -Uri {0}' -f (ConvertTo-PowerShellLiteral $InstallerUrl))
        $Lines += '    Invoke-Expression $Installer'
        $Lines += '    exit 0'
        $Lines += '} catch {'
        $Lines += '    Add-Content -Encoding UTF8 -Path $env:LMU_MINTER_LOG -Value ("[ERROR] " + $_.Exception.Message)'
        $Lines += '    Add-Content -Encoding UTF8 -Path $env:LMU_MINTER_LOG -Value ($_ | Out-String)'
        $Lines += '    exit 1'
        $Lines += '}'
        Set-Content -Encoding ASCII -Path $Bootstrap -Value $Lines
        $ArgumentList = "-NoProfile -ExecutionPolicy Bypass -File `"$Bootstrap`""
    }

    try {
        $PowerShell = (Get-Command powershell.exe -ErrorAction SilentlyContinue).Source
        if ([string]::IsNullOrWhiteSpace($PowerShell)) {
            $PowerShell = "powershell.exe"
        }
        $Process = Start-Process -FilePath $PowerShell -ArgumentList $ArgumentList -Verb RunAs -PassThru
        Wait-ElevatedInstaller -Process $Process -Path $ElevatedLogPath
        if ($null -ne $Process.ExitCode -and $Process.ExitCode -ne 0) {
            Write-Host ""
            Write-Host "Elevated installer failed with exit code $($Process.ExitCode). Full log: $ElevatedLogPath" -ForegroundColor Red
            Show-InstallLogTail -Path $ElevatedLogPath
            exit $Process.ExitCode
        }
        exit 0
    } catch {
        Write-InstallFailure $_
        Write-Host "Administrator prompt failed or was cancelled." -ForegroundColor Red
        Write-Host "Installer log: $ElevatedLogPath"
        exit 1
    } finally {
        if ($Bootstrap -and (Test-Path $Bootstrap)) {
            Remove-Item $Bootstrap -Force -ErrorAction SilentlyContinue
        }
    }
}

function Invoke-DownloadFile {
    param(
        [string]$Uri,
        [string]$OutFile,
        [int]$StartPercent,
        [int]$EndPercent,
        [string]$Label
    )

    Write-InstallProgress $StartPercent "Downloading $Label"

    $Request = [System.Net.HttpWebRequest]::Create($Uri)
    $Request.UserAgent = "LMU-Minter-Installer"
    $Response = $Request.GetResponse()
    try {
        $Total = $Response.ContentLength
        $InputStream = $Response.GetResponseStream()
        $OutputStream = [System.IO.File]::Create($OutFile)
        try {
            $Buffer = New-Object byte[] 1048576
            $ReadTotal = [int64]0
            $LastShownPercent = -1

            while (($Read = $InputStream.Read($Buffer, 0, $Buffer.Length)) -gt 0) {
                $OutputStream.Write($Buffer, 0, $Read)
                $ReadTotal += $Read

                if ($Total -gt 0) {
                    $DownloadRatio = [double]$ReadTotal / [double]$Total
                    $StepPercent = $StartPercent + [int]([Math]::Floor($DownloadRatio * ($EndPercent - $StartPercent)))
                    $StepPercent = [Math]::Min($EndPercent, $StepPercent)
                    if ($StepPercent -gt $LastShownPercent) {
                        Write-InstallProgress $StepPercent ("Downloading $Label ({0:P0})" -f $DownloadRatio)
                        $LastShownPercent = $StepPercent
                    }
                }
            }
        } finally {
            $OutputStream.Close()
            $InputStream.Close()
        }
    } finally {
        $Response.Close()
    }

    Write-InstallProgress $EndPercent "Downloaded $Label"
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

        Write-InstallProgress 60 "Stopping existing LMU Minter process $($Process.ProcessId)"
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
    Write-InstallProgress 3 "Preparing installer workspace"
    New-Item -ItemType Directory -Force -Path $TempDir | Out-Null
    $Archive = Join-Path $TempDir $Artifact
    $ExtractDir = Join-Path $TempDir "extract"

    Invoke-DownloadFile -Uri $Url -OutFile $Archive -StartPercent 5 -EndPercent 35 -Label $Artifact

    Write-InstallProgress 40 "Extracting $Artifact"
    Expand-Archive -Path $Archive -DestinationPath $ExtractDir -Force
    $SourceDir = Join-Path $ExtractDir "lmu-minter-$Platform"
    if (-not (Test-Path $SourceDir)) {
        throw "Archive layout is invalid: $SourceDir not found"
    }

    Write-InstallProgress 50 "Installing to $InstallDir"
    $ExistingTask = Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
    if ($ExistingTask) {
        Write-InstallProgress 55 "Stopping existing scheduled task"
        Stop-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
        Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false -ErrorAction SilentlyContinue
    }

    Write-InstallProgress 60 "Stopping old minter processes"
    Stop-ExistingMinterProcesses -Path $InstallDir
    Write-InstallProgress 65 "Replacing installed files"
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

    Write-InstallProgress 80 "Registering hidden startup task"
    Register-ScheduledTask `
        -TaskName $TaskName `
        -Action $Action `
        -Trigger $Trigger `
        -Principal $Principal `
        -Settings $Settings `
        -Description "Local Steam sign-in helper for LMU Assister Web." `
        -Force | Out-Null

    Write-InstallProgress 88 "Starting LMU Minter"
    Start-ScheduledTask -TaskName $TaskName

    $HealthUrl = "http://127.0.0.1:$Port/health"
    for ($i = 0; $i -lt 20; $i++) {
        try {
            $Response = Invoke-WebRequest -UseBasicParsing -Uri $HealthUrl -TimeoutSec 2
            if ($Response.StatusCode -eq 200) {
                Write-InstallProgress 100 "LMU Minter is installed and running: $HealthUrl"
                exit 0
            }
        } catch {
        }
        $HealthPercent = 90 + [Math]::Min(9, [int][Math]::Floor(($i + 1) / 20 * 9))
        Write-InstallProgress $HealthPercent "Waiting for health check at $HealthUrl"
        Start-Sleep -Seconds 1
    }

    Write-InstallProgress 100 "LMU Minter is installed, but health check did not answer yet: $HealthUrl"
} catch {
    Write-InstallFailure $_
    exit 1
} finally {
    if (Test-Path $TempDir) {
        Remove-Item $TempDir -Recurse -Force
    }
}
