[CmdletBinding()]
param(
    [switch]$Stop,
    [switch]$NoBrowser
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$logs = Join-Path $root "logs\dev"
$stateFile = Join-Path $logs "processes.json"
$compose = Join-Path $root "compose.yml"
$containers = @(
    @{ service = "mysql"; name = "travel-mind-mysql" },
    @{ service = "redis"; name = "travel-mind-redis" },
    @{ service = "qdrant"; name = "travel-mind-qdrant" }
)

function Require-Command([string]$Name, [string]$ScoopPackage) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "缺少 $Name，请先执行：scoop install $ScoopPackage"
    }
}

function Test-Url([string]$Url) {
    try {
        $null = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 10
        return $true
    } catch {
        return $false
    }
}

function Stop-AppProcess([int]$ProcessId, [int]$Port) {
    $pids = @($ProcessId)
    if ($Port) {
        $pids += (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue).OwningProcess
    }
    $pids | Where-Object { $_ } | Sort-Object -Unique | ForEach-Object {
        Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue
    }
}

function Stop-TrackedProcesses {
    if (-not (Test-Path $stateFile)) { return }
    $processes = @(Get-Content $stateFile -Raw | ConvertFrom-Json)
    foreach ($entry in $processes) {
        Stop-AppProcess $entry.pid $entry.port
        Write-Host "已停止 $($entry.name)"
    }
    Remove-Item $stateFile -Force
}

function Start-DataContainer([string]$Service, [string]$Name) {
    & docker container inspect $Name *> $null
    if ($LASTEXITCODE -eq 0) {
        & docker start $Name *> $null
    } else {
        & docker compose -f $compose up -d $Service
        if ($LASTEXITCODE -ne 0) { throw "$Service 容器创建失败" }
    }

    $deadline = (Get-Date).AddSeconds(90)
    while ((Get-Date) -lt $deadline) {
        $status = & docker inspect --format "{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}" $Name
        if ($status -in @("healthy", "running")) {
            Write-Host "$Service 已启动"
            return
        }
        Start-Sleep -Seconds 1
    }
    throw "$Service 容器启动超时"
}

function Start-AppProcess(
    [string]$Name,
    [string]$Command,
    [string[]]$Arguments,
    [string]$WorkingDirectory,
    [string]$HealthUrl,
    [int]$Port
) {
    if (Test-Url $HealthUrl) {
        Write-Host "$Name 已在运行"
        return $null
    }
    if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) {
        throw "$Name 无法启动：端口 $Port 已被其他进程占用"
    }

    $process = Start-Process -FilePath $Command -ArgumentList $Arguments `
        -WorkingDirectory $WorkingDirectory -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logs "$Name.out.log") `
        -RedirectStandardError (Join-Path $logs "$Name.err.log")

    $deadline = (Get-Date).AddSeconds(120)
    while ((Get-Date) -lt $deadline) {
        if ($process.HasExited) {
            Stop-AppProcess $process.Id $Port
            throw "$Name 启动失败，请查看 logs/dev/$Name.err.log"
        }
        if (Test-Url $HealthUrl) {
            Write-Host "$Name 已启动"
            return [pscustomobject]@{ name = $Name; pid = $process.Id; port = $Port }
        }
        Start-Sleep -Seconds 1
    }
    Stop-AppProcess $process.Id $Port
    throw "$Name 启动超时，请查看 logs/dev/$Name.err.log"
}

Require-Command "docker" "docker"

if ($Stop) {
    Stop-TrackedProcesses
    foreach ($container in $containers) {
        & docker stop $container.name *> $null
    }
    Write-Host "Travel Mind 已停止"
    return
}

Require-Command "java" "temurin17-jdk"
Require-Command "mvn" "maven"
Require-Command "node" "nodejs-lts"
Require-Command "npm" "nodejs-lts"

New-Item -ItemType Directory -Path $logs -Force | Out-Null
Stop-TrackedProcesses

if (-not (Test-Path (Join-Path $root ".env"))) {
    Copy-Item (Join-Path $root ".env.example") (Join-Path $root ".env")
    Write-Host "已从 .env.example 创建本地 .env"
}

foreach ($container in $containers) {
    Start-DataContainer $container.service $container.name
}

$backendReady = Test-Url "http://localhost:8080/health"
$backendArguments = @()
if (-not $backendReady) {
    & mvn -q -pl app -am package "-DskipTests"
    if ($LASTEXITCODE -ne 0) { throw "Java 后端构建失败" }
    $jar = Get-ChildItem (Join-Path $root "app\target") -Filter "*.jar" |
        Where-Object Name -NotLike "*.original" |
        Select-Object -First 1
    if (-not $jar) { throw "未找到后端可执行 JAR" }
    $backendArguments = @("-jar", $jar.FullName)
}

$frontend = Join-Path $root "frontend"
$frontendReady = Test-Url "http://localhost:5173"
if (-not $frontendReady -and -not (Test-Path (Join-Path $frontend "node_modules"))) {
    & npm ci --prefix $frontend
    if ($LASTEXITCODE -ne 0) { throw "前端依赖安装失败" }
}

$pythonRoot = Join-Path $root "python-ai"
$python = Join-Path $pythonRoot ".venv\Scripts\python.exe"
$pythonReady = Test-Url "http://localhost:19080/health"
if (-not $pythonReady -and -not (Test-Path $python)) {
    Require-Command "python" "python"
    & python -m venv (Join-Path $pythonRoot ".venv")
    if ($LASTEXITCODE -ne 0) { throw "Python 虚拟环境创建失败" }
    & $python -m pip install -r (Join-Path $pythonRoot "requirements.txt")
    if ($LASTEXITCODE -ne 0) { throw "Python 依赖安装失败" }
}

$processes = @()
try {
    $started = Start-AppProcess -Name "backend" -Command "java" -Arguments $backendArguments `
        -WorkingDirectory $root -HealthUrl "http://localhost:8080/health" -Port 8080
    if ($started) { $processes += $started }
    $started = Start-AppProcess -Name "python-ai" -Command $python `
        -Arguments @("-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "19080") `
        -WorkingDirectory $pythonRoot -HealthUrl "http://localhost:19080/health" -Port 19080
    if ($started) { $processes += $started }
    $vite = Join-Path $frontend "node_modules\vite\bin\vite.js"
    $started = Start-AppProcess -Name "frontend" -Command "node" -Arguments @($vite, "--host", "0.0.0.0") `
        -WorkingDirectory $frontend -HealthUrl "http://localhost:5173" -Port 5173
    if ($started) { $processes += $started }
} catch {
    foreach ($entry in $processes) {
        Stop-AppProcess $entry.pid $entry.port
    }
    throw
}

if ($processes.Count -gt 0) {
    $processes | ConvertTo-Json | Set-Content $stateFile -Encoding utf8
}

Write-Host ""
Write-Host "Travel Mind 已就绪"
Write-Host "用户端：http://localhost:5173/"
Write-Host "管理端：http://localhost:5173/admin"
Write-Host "停止服务：.\start.ps1 -Stop"

if (-not $NoBrowser) {
    Start-Process "http://localhost:5173/"
}
