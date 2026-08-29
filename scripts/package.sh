#!/bin/sh
set -eu
cd "$(dirname "$0")/.."

echo "[INFO] Packaging backend (skipping tests)..."
mvn clean package -Dmaven.test.skip=true
echo "[INFO] Package finished. Artifact: allinone-admin/target/allinone-admin.jar"
