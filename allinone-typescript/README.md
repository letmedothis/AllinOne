# allinone-typescript — AllinOne 前端模块

AllinOne 的前端工程，基于 RuoYi-Vue3-TypeScript（Vue 3 + TypeScript + Element Plus + Pinia + Vite 6）。
本项目说明以此为准；网上流传的 RuoYi 原版 README（在线体验/演示地址等）与本仓库无关。

## 常用命令

```bash
npm ci                  # 按锁文件安装依赖
npm run dev             # 开发模式（http://localhost:80，/prod-api、/jmreport 等已配置代理）
npm run build:prod      # 生产构建（vite build）
npm run test:contracts  # 前后端契约测试（node --test）
```

依赖本地 Luckysheet fork：`package.json` 以 `file:../allinone-luckysheet` 引用，
**首次启动前必须先构建 Luckysheet**（`cd ../allinone-luckysheet && npm ci && npm run build`）。
推荐从仓库根目录执行 `./scripts/build-frontend.sh`，会按正确顺序完成两者构建。

## 结构要点

- `src/views/collect/` — 数据填报：模板（列表/设计器）、我的填报（填报/详情/异步导出任务）、分类、字段映射
- `src/views/report/` — 报表管理：报表配置/分类、报表查看（ticket 内嵌）、大屏管理（卡片列表/全屏查看）
- `src/components/CollectSheet/` — Luckysheet 封装组件（只读模式、图片上传、字典下拉载体）
- `src/components/ReportFrame/` — JimuReport/JimuBI iframe 封装（防缓存、超时重试）
- `src/api/collect/`、`src/api/report/` — 后端接口封装（页面直调 API，不经 Pinia store）
- `vite.config.ts` — Luckysheet 静态资源插件（dev 挂载 `/luckysheet/`，构建后拷贝产物）、manualChunks 分包

## 与后端的契约

- 接口权限字符串与菜单种子见 `sql/allinone_menu.sql`；页面依赖 RuoYi 动态菜单路由，部署时需执行该脚本。
- 业务错误码 1001–1004（模板不存在/未发布/状态锁定/版本冲突）由后端经 `AjaxResult.code` 返回，拦截器统一提示。
