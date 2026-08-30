#!/bin/bash
# MySQL 容器首次初始化时执行（docker-entrypoint-initdb.d 按文件名顺序调用）
# 按依赖顺序导入 sql/ 目录下的建库脚本
set -e

DB_NAME="${MYSQL_DATABASE:-allinone}"
SQL_DIR="/sql"

echo ">>> 初始化数据库 $DB_NAME"
for f in ry_20260417.sql quartz.sql jimureport.mysql5.7.create.sql allinone_biz.sql allinone_biz_update.sql allinone_menu.sql; do
    if [ -f "$SQL_DIR/$f" ]; then
        echo ">>> 导入 $f"
        mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$DB_NAME" < "$SQL_DIR/$f"
    else
        echo ">>> 跳过（不存在）：$f"
    fi
done
echo ">>> 数据库初始化完成"
