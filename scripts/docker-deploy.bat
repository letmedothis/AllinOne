@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

rem Docker 一键部署：生成 .env（含随机密钥）-> 构建并启动全部容器 -> 等待就绪
rem 用法：scripts\docker-deploy.bat [--no-build]

set "PROJECT_ROOT=%~dp0.."
cd /d "%PROJECT_ROOT%"

where docker >nul 2>nul || (echo [deploy] 错误：未检测到 docker，请先安装 Docker Desktop & exit /b 1)
docker compose version >nul 2>nul || (echo [deploy] 错误：未检测到 docker compose 插件 & exit /b 1)

if not exist .env (
    echo [deploy] 未发现 .env，自动生成（含随机数据库密码与 JWT 密钥）
    for /f %%a in ('powershell -NoProfile -Command "[guid]::NewGuid().ToString('N').Substring(0,32)"') do set "MYSQL_PW=%%a"
    for /f %%a in ('powershell -NoProfile -Command "[guid]::NewGuid().ToString('N').Substring(0,32) + [guid]::NewGuid().ToString('N').Substring(0,32)"') do set "JWT_SEC=%%a"
    > .env (
        echo # 由 docker-deploy.bat 自动生成
        echo MYSQL_ROOT_PASSWORD=!MYSQL_PW!
        echo REDIS_PASSWORD=
        echo JWT_SECRET=!JWT_SEC!
        echo HTTP_PORT=80
        echo JAVA_OPTS=
    )
    echo [deploy] 已生成 .env，请妥善保管
)

set "BUILD_FLAG=--build"
if "%~1"=="--no-build" (
    set "BUILD_FLAG="
    echo [deploy] 跳过镜像构建，使用已有镜像启动
)

echo [deploy] 构建并启动容器（首次构建可能耗时 10 分钟以上）
docker compose up -d %BUILD_FLAG%
if errorlevel 1 exit /b 1

echo [deploy] 等待后端就绪（最长 6 分钟）
set /a tries=0
:waitloop
for /f %%h in ('docker inspect --format "{{if .State.Health}}{{.State.Health.Status}}{{end}}" allinone-backend 2^>nul') do set "STATUS=%%h"
if "%STATUS%"=="healthy" goto deployed
if "%STATUS%"=="unhealthy" (
    echo [deploy] 错误：后端健康检查失败，查看日志：docker compose logs backend
    exit /b 1
)
set /a tries+=1
if %tries% geq 48 (
    echo [deploy] 错误：等待超时，查看日志：docker compose logs backend
    exit /b 1
)
timeout /t 8 /nobreak >nul
goto waitloop

:deployed
for /f "tokens=1,* delims==" %%a in ('findstr /b "HTTP_PORT=" .env') do set "HTTP_PORT=%%b"
if "%HTTP_PORT%"=="" set "HTTP_PORT=80"
echo [deploy] 部署完成 ✔
echo   访问地址：http://localhost:%HTTP_PORT%/
echo   默认账号：admin / admin123（登录后请立即修改）
echo   常用命令：docker compose ps ^| logs -f backend ^| down
exit /b 0
