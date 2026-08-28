#!/bin/sh
set -eu

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)

echo "[1/4] Installing locked Luckysheet dependencies"
cd "$PROJECT_ROOT/allinone-luckysheet"
npm ci

echo "[2/4] Building Luckysheet"
npm run build

echo "[3/4] Installing locked application dependencies"
cd "$PROJECT_ROOT/allinone-typescript"
npm ci

echo "[4/4] Building application frontend"
npm run build:prod
