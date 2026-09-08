<#
.SYNOPSIS
  在 Windows 上启动本地 AllinOne 后端与前端。

.DESCRIPTION
  默认连接 WSL 暴露到 localhost 的 MySQL 和 Redis：
  MySQL root/123456，Redis 密码 123456。
  首次运行会在缺少构建产物或依赖时自动安装/构建。

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File .\scripts\start-local-windows.ps1
#>

[CmdletBinding()]
param(
    [string]$DbUsername = 'root',
    [string]$DbPassword,
    [string]$RedisPassword,
    [string]$JwtSecret,
    [string]$DbHost = 'localhost',
    [int]$DbPort = 3306,
    [string]$RedisHost = 'localhost',
    [int]$RedisPort = 6379,
    [int]$ServerPort = 8080,
    [switch]$SkipInstall
)

$ErrorActionPreference = 'Stop'

function Test-TcpPort {
    param([string]$HostName, [int]$Port)

    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $connectTask = $client.ConnectAsync($HostName, $Port)
        if (-not $connectTask.Wait(1000)) {
            return $false
        }
        return $client.Connected
    }
    catch {
        return $false
    }
    finally {
        $client.Dispose()
    }
}

function Require-Command {
    param([string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "未找到 $Name，请先安装并配置到 PATH。"
    }
}

function Invoke-Npm {
    param([string]$WorkingDirectory, [string[]]$Arguments)

    Push-Location $WorkingDirectory
    try {
        & npm @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "npm $($Arguments -join ' ') 执行失败。"
        }
    }
    finally {
        Pop-Location
    }
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$adminRoot = Join-Path $projectRoot 'allinone-admin'
$frontendRoot = Join-Path $projectRoot 'allinone-typescript'
$luckysheetRoot = Join-Path $projectRoot 'allinone-luckysheet'
$jarPath = Join-Path $adminRoot 'target\allinone-admin.jar'
$logDirectory = Join-Path $projectRoot 'logs'
$mavenRepository = Join-Path $projectRoot '.m2\repository'

if ([string]::IsNullOrWhiteSpace($DbPassword)) {
    $DbPassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { '123456' }
}
if ([string]::IsNullOrWhiteSpace($RedisPassword)) {
    $RedisPassword = if ($env:REDIS_PASSWORD) { $env:REDIS_PASSWORD } else { '123456' }
}
if ([string]::IsNullOrWhiteSpace($JwtSecret)) {
    $JwtSecret = $env:JWT_SECRET
}
if ([string]::IsNullOrWhiteSpace($JwtSecret)) {
    $randomBytes = [byte[]]::new(48)
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($randomBytes)
    $JwtSecret = [Convert]::ToBase64String($randomBytes)
    Write-Host '未设置 JWT_SECRET，已为本次启动生成临时随机密钥；重启后旧登录令牌会失效。' -ForegroundColor Yellow
}

Require-Command java
Require-Command mvn
Require-Command node
Require-Command npm

if (-not (Test-TcpPort -HostName $DbHost -Port $DbPort)) {
    throw "无法连接 MySQL：$DbHost`:$DbPort。请确认 WSL MySQL 已启动并监听该端口。"
}
if (-not (Test-TcpPort -HostName $RedisHost -Port $RedisPort)) {
    throw "无法连接 Redis：$RedisHost`:$RedisPort。请确认 WSL Redis 已启动并监听该端口。"
}
if (Test-TcpPort -HostName 'localhost' -Port $ServerPort) {
    throw "端口 $ServerPort 已被占用。请先停止现有后端，或使用 -ServerPort 指定其他端口。"
}

New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null

if (-not (Test-Path $jarPath)) {
    Write-Host '未找到后端 JAR，正在打包后端…' -ForegroundColor Cyan
    Push-Location $projectRoot
    try {
        & mvn "-Dmaven.repo.local=$mavenRepository" -pl allinone-admin -am package -DskipTests
        if ($LASTEXITCODE -ne 0) {
            throw '后端打包失败。'
        }
    }
    finally {
        Pop-Location
    }
}

if (-not $SkipInstall -and -not (Test-Path (Join-Path $luckysheetRoot 'node_modules\.bin\vite.cmd'))) {
    Write-Host '正在安装 Luckysheet 依赖…' -ForegroundColor Cyan
    Invoke-Npm -WorkingDirectory $luckysheetRoot -Arguments @('ci', '--no-audit', '--no-fund')
}
if (-not (Test-Path (Join-Path $luckysheetRoot 'dist\index.html'))) {
    Write-Host '正在构建 Luckysheet 静态文件…' -ForegroundColor Cyan
    Invoke-Npm -WorkingDirectory $luckysheetRoot -Arguments @('run', 'build')
}
if (-not $SkipInstall -and -not (Test-Path (Join-Path $frontendRoot 'node_modules\.bin\vite.cmd'))) {
    Write-Host '正在安装前端依赖…' -ForegroundColor Cyan
    Invoke-Npm -WorkingDirectory $frontendRoot -Arguments @('ci', '--no-audit', '--no-fund')
}

$env:DB_USERNAME = $DbUsername
$env:DB_PASSWORD = $DbPassword
$env:DB_URL = "jdbc:mysql://${DbHost}:$DbPort/allinone?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8"
$env:REDIS_HOST = $RedisHost
$env:REDIS_PORT = $RedisPort
$env:REDIS_PASSWORD = $RedisPassword
$env:JWT_SECRET = $JwtSecret
$env:LOG_PATH = $logDirectory
$env:SERVER_PORT = $ServerPort
if ([string]::IsNullOrWhiteSpace($env:RUOYI_PROFILE)) {
    $env:RUOYI_PROFILE = Join-Path $projectRoot 'runtime\uploadPath'
}
New-Item -ItemType Directory -Force -Path $env:RUOYI_PROFILE | Out-Null

$backendStdout = Join-Path $logDirectory 'backend-stdout.log'
$backendStderr = Join-Path $logDirectory 'backend-stderr.log'
Remove-Item -Force -ErrorAction SilentlyContinue $backendStdout, $backendStderr

Write-Host '正在启动后端…' -ForegroundColor Cyan
$backend = Start-Process -FilePath java `
    -ArgumentList @('-Xms256m', '-Xmx1024m', '-XX:MetaspaceSize=128m', '-XX:MaxMetaspaceSize=512m', "-DLOG_PATH=$logDirectory", '-jar', $jarPath) `
    -WorkingDirectory $projectRoot `
    -RedirectStandardOutput $backendStdout `
    -RedirectStandardError $backendStderr `
    -PassThru

$ready = $false
for ($attempt = 1; $attempt -le 60; $attempt++) {
    Start-Sleep -Seconds 1
    if ($backend.HasExited) {
        Write-Host "后端已退出，错误日志：$backendStderr" -ForegroundColor Red
        Get-Content -Path $backendStderr -Tail 80 -ErrorAction SilentlyContinue
        $backendStdoutPath = Join-Path $logDirectory 'backend-stdout.log'
        $systemErrorPath = Join-Path $logDirectory 'sys-error.log'
        Write-Host "后端标准输出：$backendStdoutPath" -ForegroundColor DarkGray
        Get-Content -Path $backendStdoutPath -Tail 80 -ErrorAction SilentlyContinue
        if (Test-Path $systemErrorPath) {
            Write-Host "应用错误日志：$systemErrorPath" -ForegroundColor DarkGray
            Get-Content -Path $systemErrorPath -Tail 120 -ErrorAction SilentlyContinue
        }
        throw '后端启动失败。'
    }
    if (Test-TcpPort -HostName 'localhost' -Port $ServerPort) {
        $ready = $true
        break
    }
}
if (-not $ready) {
    throw "后端未能在 60 秒内监听端口 $ServerPort。请查看 $backendStderr。"
}

Write-Host "后端已启动：http://localhost:$ServerPort（进程 $($backend.Id)）" -ForegroundColor Green
Write-Host "后端日志：$logDirectory" -ForegroundColor DarkGray
Write-Host '正在启动前端；按 Ctrl+C 停止前端。后端会继续运行，可用 Stop-Process 停止上方进程号。' -ForegroundColor Green

Invoke-Npm -WorkingDirectory $frontendRoot -Arguments @('run', 'dev')
