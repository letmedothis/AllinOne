@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
set "ROOT=%~dp0.."
set "SCOPE=%~1"
set "AUDIT_FAILED=0"
if "%SCOPE%"=="" set "SCOPE=production"
if not "%SCOPE%"=="production" if not "%SCOPE%"=="--all" (
    echo Usage: %~nx0 [--all]
    exit /b 2
)

call :audit "Luckysheet" "%ROOT%\allinone-luckysheet"
call :audit "application frontend" "%ROOT%\allinone-typescript"
exit /b %AUDIT_FAILED%

:audit
echo Auditing production dependencies: %~1
pushd %~2
call npm audit --omit=dev --audit-level=high || set "AUDIT_FAILED=1"
if "%SCOPE%"=="--all" (
    echo Auditing all dependencies: %~1
    call npm audit --audit-level=high || set "AUDIT_FAILED=1"
)
popd
goto :eof
