@echo off
chcp 65001 >nul
cd /d "%~dp0.."

echo [INFO] Cleaning all Maven module targets...
call mvn clean
if errorlevel 1 goto :fail

echo [INFO] Clean finished.
pause
exit /b 0

:fail
echo [ERROR] Maven clean failed.
pause
exit /b 1
