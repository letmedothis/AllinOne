@echo off
chcp 65001 >nul
cd /d "%~dp0.."

echo [INFO] Packaging backend (skipping tests)...
call mvn clean package -Dmaven.test.skip=true
if errorlevel 1 goto :fail

echo [INFO] Package finished. Artifact: allinone-admin\target\allinone-admin.jar
pause
exit /b 0

:fail
echo [ERROR] Maven package failed.
pause
exit /b 1
