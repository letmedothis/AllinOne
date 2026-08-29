#!/bin/sh
set -eu
APP_HOME="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$APP_HOME/allinone-admin/target/allinone-admin.jar"

if [ ! -f "$JAR" ]; then
    echo "[ERROR] $JAR not found. Run package.sh first." >&2
    exit 1
fi

# 默认按生产配置启动（上传路径/日志级别/swagger 均走 prod 安全默认值）；
# 本地开发需调试 SQL 时可显式：SPRING_PROFILES_ACTIVE=druid ./run.sh
: "${SPRING_PROFILES_ACTIVE:=druid,prod}"
export SPRING_PROFILES_ACTIVE

# DB_PASSWORD 无默认值，缺失时占位符解析失败会直接拒启，这里提前给出明确报错
: "${DB_PASSWORD:?请在环境变量中设置 DB_PASSWORD（本地 MySQL 密码，需已导入 sql/ 下的脚本）}"

# JWT_SECRET 无默认值；缺失时生成临时随机密钥兜底（重启后登录态失效，生产请显式设置）
if [ -z "${JWT_SECRET:-}" ]; then
    JWT_SECRET="$(head -c 48 /dev/urandom | base64 | tr -d '\n')"
    export JWT_SECRET
    echo "[WARN] JWT_SECRET 未设置，已生成临时密钥（重启后登录态失效，生产请显式设置）" >&2
fi

mkdir -p "$APP_HOME/logs"

echo "[INFO] Starting backend: $JAR (profiles: $SPRING_PROFILES_ACTIVE)"
JAVA_OPTS="-Xms256m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m -DLOG_PATH=$APP_HOME/logs"
exec java $JAVA_OPTS -jar "$JAR"
