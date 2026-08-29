#!/bin/sh
# Docker 一键部署：生成 .env（含随机密钥）-> 构建并启动全部容器 -> 等待就绪
# 用法：scripts/docker-deploy.sh [--no-build] ；环境变量随 docker-compose.yml 注入
set -eu

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$PROJECT_ROOT"

log() { echo "[deploy] $*"; }
fail() { echo "[deploy] 错误：$*" >&2; exit 1; }

command -v docker >/dev/null 2>&1 || fail "未检测到 docker，请先安装 Docker Engine 20+"
docker compose version >/dev/null 2>&1 || fail "未检测到 docker compose 插件，请安装 Docker Compose v2"

# 首次部署：生成 .env，数据库密码与 JWT 密钥取随机值
if [ ! -f .env ]; then
    log "未发现 .env，自动生成（含随机生成的数据库密码与 JWT 密钥）"
    gen_secret() {
        if command -v openssl >/dev/null 2>&1; then
            openssl rand -hex 24
        else
            head -c 24 /dev/urandom | od -An -tx1 | tr -d ' \n'
        fi
    }
    {
        echo "# 由 docker-deploy.sh 自动生成于 $(date '+%Y-%m-%d %H:%M:%S')"
        echo "MYSQL_ROOT_PASSWORD=$(gen_secret)"
        echo "REDIS_PASSWORD="
        echo "JWT_SECRET=$(gen_secret)$(gen_secret)"
        echo "HTTP_PORT=80"
        echo "JAVA_OPTS="
    } > .env
    chmod 600 .env
    log "已生成 .env，请妥善保管（含数据库密码与 JWT 密钥）"
fi

BUILD_FLAG="--build"
if [ "${1:-}" = "--no-build" ]; then
    BUILD_FLAG=""
    log "跳过镜像构建，使用已有镜像启动"
fi

log "构建并启动容器（首次构建需下载 Maven/npm 依赖，可能耗时 10 分钟以上）"
# shellcheck disable=SC2086
docker compose up -d $BUILD_FLAG

log "等待后端就绪（最长 6 分钟）"
i=0
while [ "$i" -lt 48 ]; do
    status=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{end}}' allinone-backend 2>/dev/null || echo unknown)
    case "$status" in
        healthy)
            port=$(grep -E '^HTTP_PORT=' .env | cut -d= -f2-)
            port=${port:-80}
            log "部署完成 ✔"
            echo ""
            echo "  访问地址：http://localhost:${port}/"
            echo "  默认账号：admin / admin123（登录后请立即修改）"
            echo "  常用命令：docker compose ps | logs -f backend | down"
            echo ""
            exit 0
            ;;
        unhealthy)
            fail "后端健康检查失败，查看日志：docker compose logs backend"
            ;;
    esac
    i=$((i + 1))
    sleep 8
done

fail "等待超时，请查看日志：docker compose logs backend"
