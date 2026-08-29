<p align="center">
	<img alt="logo" src="https://oscimg.oschina.net/oscnet/up-d3d0a9303e11d522a06cd263f3079027715.png">
</p>
<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">AllinOne — 企业级报表管理系统</h1>
<h4 align="center">基于 Spring Boot 3 + Vue 3 + TypeScript 的一体化报表解决方案</h4>
<p align="center">
	<a href="https://gitee.com/y_project/RuoYi-Vue"><img src="https://img.shields.io/badge/RuoYi-v3.9.2-brightgreen.svg"></a>
	<a href="https://github.com/jeecgboot/jimureport"><img src="https://img.shields.io/badge/JimuReport-v2.5.0-blue.svg"></a>
	<a href="https://github.com/mengshukeji/Luckysheet"><img src="https://img.shields.io/badge/Luckysheet-2.1.13-orange.svg"></a>
	<a href="https://github.com/yangzongzhuan/RuoYi-Vue/blob/master/LICENSE"><img src="https://img.shields.io/github/license/mashape/apistatus.svg"></a>
</p>

---

## 📋 项目简介

**AllinOne** 是在 [RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue) 快速开发框架基础上构建的 **企业级报表管理系统**，以"**All in One, Report Everything**"为理念，将系统管理、数据收集、报表展示、数据可视化分析整合于一体。

项目将若依强大的 RBAC 权限体系作为底座，在此基础上深度集成三大核心能力：

| 能力 | 技术方案 | 用途 |
|------|---------|------|
| 📊 **数据收集** | Luckysheet 在线电子表格 | 类 Excel 的在线数据填报、采集、协同编辑 |
| 📈 **报表展示** | JimuReport 积木报表 | 复杂报表设计、打印、套打、多 Sheet 报表 |
| 📉 **数据可视化** | JimuBI + ECharts | 数据大屏、仪表盘、图表分析、自助 BI |

---

## 🎯 产品定位

- **目标用户**：企业内部管理人员、业务运营人员、数据分析人员
- **核心价值**：统一数据收集入口 + 灵活报表配置 + 可视化决策分析
- **部署方式**：单体部署，开箱即用
- **交付形态**：全功能开源，支持私有化部署

---

## 🧩 功能架构

### 第一层：系统基础（RuoYi 底座）

| 模块 | 说明 |
|------|------|
| 用户管理 | 系统操作者配置与维护 |
| 部门管理 | 组织机构树结构，支持数据权限 |
| 岗位管理 | 用户所属职务配置 |
| 菜单管理 | 系统菜单、操作权限、按钮权限标识 |
| 角色管理 | 角色菜单权限分配、数据范围权限划分 |
| 字典管理 | 常用固定数据维护 |
| 参数管理 | 系统动态参数配置 |
| 通知公告 | 系统通知公告发布维护 |
| 操作日志 | 正常及异常操作日志记录查询 |
| 登录日志 | 登录日志（含异常）记录查询 |
| 定时任务 | 在线添加/修改/删除任务调度 |
| 代码生成 | ⭐ 一键生成前后端 CRUD 代码 |
| 系统接口 | 自动生成 API 接口文档（SpringDoc） |

### 第二层：数据收集（Luckysheet）

| 功能 | 说明 |
|------|------|
| 在线表格编辑 | 类 Excel 的全功能电子表格 |
| 数据填报模板 | 自定义模板的数据在线填报 |
| 数据回写 | 填报数据直接写入业务数据库 |
| 公式与计算 | 支持 Excel 公式自动计算 |
| 导入/导出 | Excel 文件导入、导出 |
| 多 Sheet 管理 | 多标签页数据组织 |

### 第三层：报表分析（JimuReport + JimuBI）

| 功能 | 说明 |
|------|------|
| 报表设计器 | 分组报表、交叉报表、明细表、主子报表等 |
| 图形报表 | 柱形图、折线图、饼图、散点图、雷达图等 |
| 数据大屏 | 拖拽式大屏设计，28+ 种 ECharts 图表 |
| 仪表盘 | 24 列栅格布局，PC/手机双模式 |
| 数据填报 | 校验规则、批量导入、移动端填报 |
| 打印导出 | 自定义打印、套打、PDF/Word/Excel 导出 |

### 🔮 远期规划

| 能力 | 状态 | 说明 |
|------|:----:|------|
| **工作流引擎** | 🔜 规划中 | 集成 Warm-Flow 工作流引擎，7 张表轻量实现完整审批流转 |
| **数据填报→审批→归档** | 🔜 规划中 | Luckysheet 填报 + Warm-Flow 审批 + JimuReport 归档的完整链路 |

---

## 🛠️ 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|:----:|------|
| Spring Boot | 3.5.14 | 核心框架 |
| JDK | 17+ | 运行环境 |
| MyBatis | Spring Boot 3 Starter | ORM 框架 |
| Druid | 1.2.28 | 数据库连接池 |
| PageHelper | — | 分页插件 |
| Redis | — | 缓存 |
| JWT | — | 登录认证 |
| SpringDoc | 2.8.17 | API 文档 |
| Oshi | 7.3.0 | 服务器监控 |
| JimuReport | 2.5.0 | 报表/大屏/仪表盘 |
| Quartz | — | 定时任务 |
| MySQL | ≥ 5.7 | 数据库 |

### 前端

| 技术 | 版本 | 用途 |
|------|:----:|------|
| Vue | 3.5.26 | 前端框架 |
| Vite | 6.4.1 | 构建工具 |
| TypeScript | 5.6.3 | 类型系统 |
| Element Plus | 2.13.1 | UI 组件库 |
| Pinia | 3.0.4 | 状态管理 |
| Vue Router | 4.6.4 | 路由管理 |
| ECharts | 5.6.0 | 图表库 |
| Axios | 1.13.2 | HTTP 客户端 |
| Luckysheet | 2.1.13 | 在线电子表格（本地源码构建，allinone-luckysheet/）|
| @vueuse/core | 14.1.0 | 组合式工具集 |

---

## 📁 项目模块结构

```
allinone
├── allinone-admin              # 后台管理模块（Controller 层）
│   └── src/main/java/com/allinone/web/controller
├── allinone-common             # 公共工具模块
│   └── src/main/java/com/allinone/common
├── allinone-framework          # 核心框架配置
│   └── src/main/java/com/allinone/framework
│       ├── config              # Security、Redis、Swagger 等配置
│       ├── security            # Spring Security + JWT 认证
│       └── web.service         # 权限服务
├── allinone-generator          # 代码生成器模块
│   └── src/main/java/com/allinone/generator
├── allinone-quartz             # 定时任务模块（Quartz）
│   └── src/main/java/com/allinone/quartz
├── allinone-system             # 系统业务模块（Service + Mapper）
│   └── src/main/java/com/allinone/system
├── allinone-collect            # 数据填报模块（独立子模块）
│   └── src/main/java/com/allinone/collect
│       ├── controller          # 填报控制器
│       ├── domain              # 填报领域模型（5 张表）
│       ├── mapper              # 填报 Mapper
│       └── service             # 填报业务逻辑
├── allinone-report             # 报表配置模块（独立子模块）
│   └── src/main/java/com/allinone/report
│       ├── controller          # 报表控制器
│       ├── domain              # 报表领域模型（2 张表）
│       ├── mapper              # 报表 Mapper
│       └── service             # 报表业务逻辑
├── allinone-typescript         # 前端项目（Vue 3 + TypeScript）
│   ├── src
│   │   ├── api                 # API 调用封装
│   │   ├── components          # 公共组件
│   │   ├── composables         # 组合式函数
│   │   ├── store               # Pinia 状态管理
│   │   ├── utils               # 工具函数
│   │   └── views               # 页面组件
│   └── vite.config.ts          # Vite 构建配置
├── sql                         # SQL 脚本
├── bin                         # 部署脚本
└── doc                         # 项目文档
```

---

## ⚡ 快速开始

### 环境要求

- JDK ≥ 17
- MySQL ≥ 5.7
- Maven ≥ 3.6.3
- Node.js ≥ 18
- Redis ≥ 3.0
- npm ≥ 10

### 后端启动

```bash
# 1. 创建数据库并按顺序导入 SQL（必须用 SOURCE 方式，详见 doc/SQL_EXECUTION_ORDER.md）
mysql -uroot -p
mysql> create database allinone default charset utf8mb4;
mysql> use allinone;
mysql> SET @bootstrap_password_bcrypt = '<你的admin密码BCrypt哈希>';  -- 见下方"管理员账号"说明
mysql> SOURCE sql/ry_20260417.sql;
mysql> SOURCE sql/quartz.sql;
mysql> SOURCE sql/allinone_biz.sql;
mysql> SOURCE sql/jimureport.mysql5.7.create.sql;
mysql> SOURCE sql/allinone_biz_update.sql;   -- work_report 系列表，缺它报表功能不可用
mysql> SOURCE sql/allinone_menu.sql;         -- 业务菜单，缺它前端无业务入口

# 2. 设置必需的环境变量（配置文件中无默认值，缺失会启动失败）
export DB_PASSWORD='<MySQL密码>'
export JWT_SECRET='<JWT签名密钥>'
export DRUID_LOGIN_PASSWORD='<Druid监控台密码>'

# 3. 启动后端
mvn clean install -DskipTests
cd allinone-admin
mvn spring-boot:run
```

> **管理员账号：** 本仓库的 SQL 脚本已脱敏，**不存在默认密码**。`admin` 账号的密码由导入
> `ry_20260417.sql` 时的会话变量 `@bootstrap_password_bcrypt` 决定（BCrypt 哈希，需自行生成）；
> 未设置该变量时 admin 处于停用状态，无法登录。详见 [sql/README.md](sql/README.md)。

### 前端启动

```bash
cd allinone-luckysheet
npm ci
npm run build
cd ../allinone-typescript
npm ci
npm run dev
```

> **关于 Luckysheet：** 主前端通过本地 `file:` 依赖引用 Luckysheet，首次启动前必须先构建：
> ```bash
> cd allinone-luckysheet
> npm ci
> npm run build    # 将 src/ 编译输出到 dist/
> cd ../allinone-typescript
> npm ci
> npm run dev
> ```
> 生产构建可在项目根目录直接执行 `./scripts/build-frontend.sh`；该脚本会按锁文件安装依赖，先构建 Luckysheet，再构建主前端。

前端生产依赖安全审计可执行 `./scripts/audit-frontend.sh`，高危或严重漏洞会返回非零退出码，可直接作为 CI 发布门禁。执行 `./scripts/audit-frontend.sh --all` 可额外检查完整构建工具链；旧工具链问题单独治理，不阻塞生产依赖门禁。依赖升级应逐项回归，禁止使用 `npm audit fix --force` 批量跨主版本修复。

访问地址：`http://localhost:80`（登录账号为你在导入 SQL 时通过 `@bootstrap_password_bcrypt` 设定的 admin 密码）

---

## 📖 使用文档

| 文档 | 说明 |
|------|------|
| **[使用手册](doc/使用手册.md)** | 面向业务用户的完整操作手册:登录、填报模板设计、在线填报、草稿与提交、报表查看,含全流程截图、权限速查与常见问题 FAQ |
| [报表集成指南](doc/REPORT_INTEGRATION_GUIDE.md) | JimuReport/JimuBI 集成配置、数据源与票据鉴权说明 |
| [SQL 执行顺序](doc/SQL_EXECUTION_ORDER.md) | 初始化脚本的执行顺序与逐条说明 |
| [浏览器交互回归报告](doc/浏览器交互回归报告_2026-08-29.md) | 关键链路浏览器级回归验证记录(含已知限制) |

---

## 🧭 开发指南

本项目在 `.agents/skills/` 中提供以下通用 Agent Skill。它们采用 `<skill-name>/SKILL.md` 标准结构，可供支持 Agent Skills 规范的编码代理发现和加载：

| Skill | 项目特有内容 | 适用场景 |
|-------|-------------|---------|
| [ruoyi-module-development](.agents/skills/ruoyi-module-development/SKILL.md) | 模块边界、权限、MyBatis、事务和验证约定 | 新增业务模块、CRUD、菜单权限 |
| [jimureport-integration](.agents/skills/jimureport-integration/SKILL.md) | JimuReport/JimuBI 2.5.0、一次性票据和嵌入链路 | 报表、大屏、鉴权和引擎配置 |
| [luckysheet-development](.agents/skills/luckysheet-development/SKILL.md) | 本地构建、工作簿快照、多 Sheet 和类型回写 | 在线填报、数据采集、Luckysheet 修改 |

Vue 3 和 Element Plus 的通用用法以官方文档为准；未集成的 Warm-Flow 不作为当前项目开发能力说明。

### 常见开发场景

#### 场景 1：新增业务模块
1. 建表（含 `create_by`, `create_time` 等基础字段）
2. 系统菜单 → 代码生成 → 导入表 → 编辑字段 → 生成代码
3. 解压到对应模块，重启刷新

#### 场景 2：创建数据填报页面
1. 使用 Luckysheet 创建在线表格模板
2. 配置数据回写映射到业务表
3. 通过若依菜单管理挂载到系统

#### 场景 3：设计数据大屏
1. 访问 `/jmreport/list` 进入报表设计器
2. 配置数据源（SQL/API）
3. 拖拽式设计大屏/仪表盘
4. 通过菜单管理嵌入系统

---

## 📐 部署

### 单体部署

```bash
# 后端打包（生产配置通过 druid,prod profile 激活）
cd allinone-admin
mvn clean package -DskipTests
nohup java -jar allinone-admin.jar --spring.profiles.active=druid,prod &

# 前端构建
cd allinone-typescript
npm run build:prod
# 将 dist 目录部署到 Nginx
```

### Nginx 配置要点（SPA 路由回退）

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    root /path/to/allinone-typescript/dist;
    index index.html;
    
    # Vue Router 回退
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # API 反向代理
    location /prod-api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    # 报表/大屏引擎路径代理（JimuReport、JimuBI、拖拽设计器）
    location /jmreport/ {
        proxy_pass http://localhost:8080;
    }
    location /jimubi/ {
        proxy_pass http://localhost:8080;
    }
    location /drag/ {
        proxy_pass http://localhost:8080;
    }
}
```

---


## 📋 初始化 SQL 说明

首次部署请按以下顺序执行 SQL 脚本：

| 步骤 | 脚本 | 来源 | 说明 |
|:----:|------|------|------|
| 1 | sql/ry_20260417.sql | RuoYi 官方 | 若依系统表（用户/角色/菜单等，默认无公开通用密码） |
| 2 | sql/quartz.sql | Quartz | 定时任务表 |
| 3 | sql/allinone_biz.sql | 本项目 | AllinOne 业务表（collect_*/report_*） |
| 4 | sql/jimureport.mysql5.7.create.sql | 本项目 | JimuReport + JimuBI 全部表（48 张，含已脱敏示例数据） |
| 5 | sql/allinone_biz_update.sql | 本项目 | work_report / work_report_sheet / work_report_cell / work_report_sheet_permission 增量表（**必须执行**，否则报表管理功能全部 500） |
| 6 | sql/allinone_menu.sql | 本项目 | 业务菜单与按钮权限（**必须执行**，否则前端无业务菜单入口） |

> 完整执行顺序与逐条说明见 [doc/SQL_EXECUTION_ORDER.md](doc/SQL_EXECUTION_ORDER.md)。
> 导入前必须先阅读 [SQL 公开与初始化说明](sql/README.md)。公开脚本已清除数据源口令和分享令牌，内置管理员也不再携带通用默认密码。

## 📄 开源协议

本项目基于 [MIT License](LICENSE) 开源，免费供个人及企业使用。

基于以下开源项目构建：
- [RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue) — MIT License
- [Element Plus](https://github.com/element-plus/element-plus) — MIT License
- [JimuReport](https://github.com/jeecgboot/jimureport) — Apache-2.0 License（免费商用）
- [Luckysheet](https://github.com/mengshukeji/Luckysheet) — MIT License
