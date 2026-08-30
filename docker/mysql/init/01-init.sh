#!/bin/bash
# MySQL 容器首次初始化时执行（docker-entrypoint-initdb.d 按文件名顺序调用）
# 按依赖顺序导入 sql/ 目录下的建库脚本
set -e

DB_NAME="${MYSQL_DATABASE:-allinone}"
SQL_DIR="/sql"

# ry_20260417.sql 依赖会话变量 @bootstrap_password_bcrypt 设置 admin 初始密码。
# Docker 部署时由 .env 的 ADMIN_PASSWORD_BCRYPT（BCrypt 哈希）注入；
# 未设置时 admin 保持停用（与单体部署的脱敏策略一致），设置方法见 docker/README.md。
# 导入会话放宽 sql_mode：JimuReport 历史示例数据存在超长列/零日期等宽松写法，
# MySQL 8 默认严格模式会中断初始化；仅导入连接受影响，不影响运行期数据库行为。
LENIENT_MODE_SQL="SET SESSION sql_mode='NO_ENGINE_SUBSTITUTION'"
BOOTSTRAP_ARGS=(--init-command="$LENIENT_MODE_SQL")
if [ -n "$ADMIN_PASSWORD_BCRYPT" ]; then
    echo ">>> 检测到 ADMIN_PASSWORD_BCRYPT，将以该哈希启用 admin 账号"
    BOOTSTRAP_ARGS=(--init-command="$LENIENT_MODE_SQL; SET @bootstrap_password_bcrypt='$ADMIN_PASSWORD_BCRYPT'")
else
    echo ">>> 未设置 ADMIN_PASSWORD_BCRYPT，admin 账号将保持停用（启用方法见 docker/README.md）"
fi

echo ">>> 初始化数据库 $DB_NAME"
for f in ry_20260417.sql quartz.sql jimureport.mysql5.7.create.sql allinone_biz.sql allinone_biz_update.sql allinone_menu.sql; do
    if [ -f "$SQL_DIR/$f" ]; then
        echo ">>> 导入 $f"
        mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "${BOOTSTRAP_ARGS[@]}" "$DB_NAME" < "$SQL_DIR/$f"
    else
        echo ">>> 跳过（不存在）：$f"
    fi
done
echo ">>> 数据库初始化完成"
