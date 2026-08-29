@echo off
chcp 65001 >nul
set "JAR=%~dp0..\allinone-admin\target\allinone-admin.jar"

if not exist "%JAR%" (
    echo [ERROR] %JAR% not found. Run package.bat first.
    pause
    exit /b 1
)

echo [INFO] Starting backend: %JAR%
set JAVA_OPTS=-Xms256m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m
java -jar %JAVA_OPTS% "%JAR%"
pause
