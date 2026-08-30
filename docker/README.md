# Docker 部署

一条命令拉起 MySQL + Redis + 后端(allinone-admin) + 前端(Nginx) 四个容器，构建、数据库初始化、健康检查全部自动化，**不需要本机安装 Maven/Node/MySQL**。

## 快速开始

```bash
git clone <本仓库> allinone && cd allinone

# 一键部署（自动生成含随机密钥的 .env，构建镜像，启动并等待就绪）
scripts/docker-deploy.sh        # Windows: scripts\docker-deploy.bat
```

看到 `部署完成 ✔` 后访问 `http://localhost/`（端口可在 `.env` 的 `HTTP_PORT` 调整）。**管理员账号默认停用**（本项目公开 SQL 不携带默认密码，admin/admin123 不可用），启用方式见下节。

## 管理员账号启用（重要）

启用方式二选一：

**方式一（推荐）：首次启动前在 `.env` 声明密码哈希**

```bash
# 1. 生成本地密码的 BCrypt 哈希（任选其一）：
python3 -c "import bcrypt;print(bcrypt.hashpw(b'你的密码', bcrypt.gensalt()).decode())"
# 或：htpasswd -bnBC 10 "" '你的密码' | tr -d ':\n'
# 或使用任意 BCrypt 工具（$2a$/$2b$/$2y$ 前缀均可）

# 2. 写入 .env（须在数据库首次初始化前）：
echo "ADMIN_PASSWORD_BCRYPT=<上一步的哈希>" >> .env

# 3. 正常执行部署，数据卷首次初始化时会以该哈希启用 admin
scripts/docker-deploy.sh
```

**方式二：部署后手动启用**

```bash
docker compose exec mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" allinone -e \
  "UPDATE sys_user SET password='\''<BCrypt哈希>'\'', status='\''0'\'' WHERE user_name='\''admin'\'';"'
```

> ⚠️ `ADMIN_PASSWORD_BCRYPT` 只在**数据卷首次初始化**时生效（`docker-entrypoint-initdb.d` 仅执行一次）。
> 已初始化过的环境请用方式二；登录后请立即在「个人中心」修改密码。

脚本做了三件事：

1. 首次运行自动生成 `.env`（随机数据库密码 + JWT 密钥，已置为 600 权限）；
2. `docker compose up -d --build` 构建并启动四个容器；
3. 轮询后端健康检查（登录页公开接口 `/captchaImage` 可访问即视为就绪）。

不想用脚本时的等价手动流程：

```bash
cp .env.example .env    # 至少修改 MYSQL_ROOT_PASSWORD 与 JWT_SECRET
docker compose up -d --build
docker compose ps       # 等待 backend 状态为 healthy
```

## 架构说明

| 容器 | 镜像 | 说明 |
|------|------|------|
| frontend | allinone/frontend（nginx:1.27-alpine） | 托管 `allinone-typescript` 构建产物（含 Luckysheet 静态资源），`/prod-api/` 反代到 backend:8080 |
| backend | allinone/backend（eclipse-temurin:17-jre） | `druid,prod` profile 运行，敏感配置全部经环境变量注入 |
| mysql | mysql:8.0 | 首次启动自动按序导入 `sql/` 建库脚本（见 `docker/mysql/init/01-init.sh`） |
| redis | redis:7-alpine | 会话/缓存，密码随 `.env` 的 `REDIS_PASSWORD` 自动生效 |

数据库、Redis 数据、上传文件分别落在 `mysql-data`、`redis-data`、`upload-data` 三个命名卷，升级镜像不丢数据。

## 构建细节（排查问题时看）

- **后端**：`docker/Dockerfile.backend` Maven 多阶段构建，先拷各模块 pom 做依赖层缓存，再整体打包，产物 `allinone-admin/target/allinone-admin.jar`；运行镜像装了 curl 仅用于健康检查，非 root 用户运行。`allinone-luckysheet` 是纯 Node 工程，不参与 Maven 打包。
- **前端**：`docker/Dockerfile.frontend` 复刻 `scripts/build-frontend.sh` 顺序——先在容器内构建 Luckysheet（`npm run build`），再构建应用（`npm run build:prod`）；vite 插件收尾时自动把 Luckysheet 产物拷进 `dist/luckysheet/`。构建阶段使用 `node:20-bookworm-slim`（glibc）而非 alpine，因为 vite/esbuild 的平台二进制只锁了 linux-x64 变体。
- **数据库初始化**仅在全空数据卷首次启动时执行一次；之后再启动不会重复导入。导入会话自动放宽 `sql_mode`（JimuReport 历史示例数据含超长列/零日期写法，MySQL 8 默认严格模式会中断初始化；仅导入连接受影响，不影响运行期数据库行为）。

## 常用运维

```bash
docker compose ps                          # 查看容器状态/健康
docker compose logs -f backend             # 后端日志
docker compose up -d --build backend       # 改代码后只重建后端
docker compose down                        # 停止（保留数据）
docker compose down -v                     # 停止并清空数据卷（重新初始化数据库）
docker compose exec mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" allinone' > backup.sql
```

## 注意事项

- `.env` 含随机密钥且已被 `.gitignore` 忽略；迁移到新机器时重新生成即可，但**数据库密码更换后需同步 `docker compose down -v` 重置数据卷**（旧卷内 root 密码不会跟随变更）。
- JVM 内存经 `.env` 的 `JAVA_OPTS` 调整（如 `-Xms512m -Xmx2g`）。
- JimuReport 设计器默认关闭，需要时在 `.env`/compose 的 backend 环境中追加 `JIMUREPORT_UI_ENABLE=true`。
- 数据库仅容器网络内可达；需要宿主机直连时放开 compose 中 mysql 的 `ports` 注释。
- 内网环境无法访问 Docker Hub 时，把 Dockerfile/compose 里的基础镜像（`maven:3.9-eclipse-temurin-17`、`node:20-bookworm-slim`、`nginx:1.27-alpine`、`mysql:8.0`、`redis:7-alpine`）换成私有 registry 地址，Maven/npm 依赖可配置镜像源参数后放入 Dockerfile。
