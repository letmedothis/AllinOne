# scripts 脚本目录

项目统一脚本目录（原 `bin/` 已合并至此）。同一任务同时提供 `.bat`（Windows）与 `.sh`（Linux/macOS）两种后缀，功能保持一致。

| 任务 | Windows | Linux / macOS | 说明 |
| --- | --- | --- | --- |
| 清理 Maven 构建产物 | `clean.bat` | `clean.sh` | 在项目根目录执行 `mvn clean` |
| 打包后端 | `package.bat` | `package.sh` | `mvn clean package -Dmaven.test.skip=true`，产物为 `allinone-admin/target/allinone-admin.jar` |
| 运行后端 | `run.bat` | `run.sh` | 以固定 JVM 参数运行 `allinone-admin.jar`，jar 不存在时会提示先执行打包 |
| 前端构建 | `build-frontend.bat` | `build-frontend.sh` | 按锁文件安装依赖，先构建 Luckysheet，再构建主前端 |
| 前端依赖审计 | `audit-frontend.bat` | `audit-frontend.sh` | 默认审计生产依赖，`--all` 额外审计开发依赖；高危漏洞返回非零退出码，可作 CI 门禁 |
| SQL 脱敏 | — | `sanitize-public-sql.ps1` | 仅 Windows PowerShell；对公开 SQL 文件做确定性联系方式替换与分享记录删除 |

注意事项：

- 所有脚本都基于自身位置定位项目根目录，可在任意工作目录下执行。
- `.bat` 脚本使用 UTF-8 输出（`chcp 65001`），失败时返回非零退出码。
- 部署环境的服务启停仍使用项目根目录的 `ry.sh` / `ry.bat`。
