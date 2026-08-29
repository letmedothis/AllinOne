@echo off
chcp 65001 >nul
set "ROOT=%~dp0.."

echo [1/4] Installing locked Luckysheet dependencies
pushd "%ROOT%\allinone-luckysheet"
call npm ci || goto :fail
echo [2/4] Building Luckysheet
call npm run build || goto :fail
popd

echo [3/4] Installing locked application dependencies
pushd "%ROOT%\allinone-typescript"
call npm ci || goto :fail
echo [4/4] Building application frontend
call npm run build:prod || goto :fail
popd

echo [INFO] Frontend build finished.
exit /b 0

:fail
echo [ERROR] Frontend build failed.
popd
exit /b 1
