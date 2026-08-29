#!/bin/sh
set -eu
cd "$(dirname "$0")/.."

echo "[INFO] Cleaning all Maven module targets..."
mvn clean
echo "[INFO] Clean finished."
