#!/bin/sh
set -eu
JAR="$(cd "$(dirname "$0")/.." && pwd)/allinone-admin/target/allinone-admin.jar"

if [ ! -f "$JAR" ]; then
    echo "[ERROR] $JAR not found. Run package.sh first." >&2
    exit 1
fi

echo "[INFO] Starting backend: $JAR"
JAVA_OPTS="-Xms256m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m"
exec java $JAVA_OPTS -jar "$JAR"
