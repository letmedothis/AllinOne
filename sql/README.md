# 公开 SQL 使用说明

`ry_20260417.sql` 和 `jimureport.mysql5.7.create.sql` 包含可公开的初始化及示例数据，但不包含可直接复用的后台密码、数据源口令或分享令牌。

## 导入前

1. 在本地生成符合项目密码策略的 BCrypt 哈希，不要把明文或哈希提交到仓库。
2. 在执行 `ry_20260417.sql` 的同一 MySQL 会话中设置会话变量 `@bootstrap_password_bcrypt`，再执行脚本。
3. 如果未设置该变量，内置 `admin` 账号会写入无效占位值并保持停用；示例 `ry` 账号始终默认停用。

示意流程（哈希仅使用本地值）：

```sql
SET @bootstrap_password_bcrypt = '<LOCAL_BCRYPT_HASH>';
SOURCE sql/ry_20260417.sql;
```

脚本会为 `sys.user.initPassword` 生成一次性随机值。首次进入系统后，应在参数管理中按部署环境的密码策略重新配置该值。

## JimuReport 数据

- 示例报表和大屏定义会保留。
- 数据源 URL 仅保留本机示例地址，账号与口令均为空；导入后必须在受控环境中重新配置。
- `jimu_report_share` 不预置记录，历史分享 URL 和令牌不会随仓库公开。
- 联系方式统一替换为保留的示例值，不应被当作真实业务数据。

如果以后用新的上游数据脚本覆盖这两个文件，执行 `scripts/sanitize-public-sql.ps1` 后再提交，并检查 Git diff。该脚本只做确定性的联系方式替换和分享记录删除，不会执行 SQL。
