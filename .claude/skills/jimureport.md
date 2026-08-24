---
name: jimureport
description: JimuReport 积木报表 — 免费的数据可视化报表工具，集成报表设计、大屏设计、仪表盘、打印填报
metadata:
  type: reference
---

# JimuReport (积木报表) 开发 Skill

> 🟡 **集成状态：部分集成** (Security 已放行，Maven 依赖待添加) | 关联：[Luckysheet](luckysheet.md) · [Warm-Flow](warm-flow.md) · [RuoYi-Vue](ruoyi-vue.md)

## 概述

积木报表（JimuReport）是一款**免费的数据可视化报表工具**，由 JeecgBoot 团队开发。集报表、打印、大屏、仪表盘于一体，采用类 Excel 拖拽式在线设计，与本项目 RuoYi-Vue 后端无缝集成。

**官方资源：**
- 文档: https://help.jimureport.com/
- GitHub: https://github.com/jeecgboot/jimureport

---

## 核心能力全景

| 模块 | 说明 |
|------|------|
| **JimuReport** | 传统复杂报表与打印设计 |
| **JimuBI** | 数据大屏与仪表盘可视化 |
| **数据填报** | 在线表格填报，数据回写数据库 |
| **AI 生成** | 一句话生成报表/大屏/仪表盘 |

---

## 技术集成

### 本项目的集成状态

> 🟡 **部分集成** — Security 已放行路径，等待 Maven 依赖和数据库初始化。

**已完成：**
- SecurityConfig 已放行 `/jmreport/**` 和 `/jmbi/**`

**待完成：**
- 在 `allinone-admin/pom.xml` 中添加 Maven 依赖
- 执行初始化 SQL 脚本
- 添加 `application.yml` 配置

**Maven 依赖（待添加）：**
```xml
<!-- JimuReport 报表引擎（Spring Boot 3.x 版本） -->
<dependency>
    <groupId>org.jeecgframework.jimureport</groupId>
    <artifactId>jimureport-spring-boot3-starter</artifactId>
    <version>2.5.0</version>
</dependency>
<!-- JimuBI 大屏（可选，如需大屏/仪表盘功能） -->
<dependency>
    <groupId>org.jeecgframework.jimureport</groupId>
    <artifactId>jimubi-spring-boot3-starter</artifactId>
    <version>2.5.0</version>
</dependency>
```

### 集成配置注意事项

1. **包扫描路径**：确保主启动类添加 JimuReport 的扫描包
   ```java
   @SpringBootApplication(scanBasePackages = {"org.jeecg", "com.allinone"})
   ```

2. **Security 放行**：在 Spring Security 配置中放行报表相关路径（注意使用 `requestMatchers` 而非 `antMatchers`，后者在 Spring Security 6.x 中已移除）
   ```java
   .requestMatchers("/jmreport/**").permitAll()
   ```

3. **访问地址**：集成后访问 `http://localhost:8080/jmreport/list`

---

## 报表设计器功能

### 数据源/数据集

| 类型 | 说明 |
|------|------|
| SQL 数据集 | 直接编写 SQL 查询，支持参数（`${参数名}`） |
| API 数据集 | 通过 HTTP 接口获取数据 |
| JSON 数据集 | 使用静态 JSON 数据 |
| 存储过程数据集 | 调用数据库存储过程 |
| 共享数据集 | 复用已定义的数据集 |
| 跨库数据源 | 支持多数据源、跨数据库取数 |

**SQL 数据集的参数传递示例：**
```sql
SELECT * FROM sys_user
WHERE 1=1
  AND dept_id = ${deptId}
  AND create_time >= '${startTime}'
ORDER BY create_time DESC
```

### 支持的报表类型

1. **分组报表** — 按字段分组汇总
2. **交叉报表** — 行列交叉统计（类透视表）
3. **明细报表** — 纯数据明细展示
4. **主子报表** — 主表+子表数据关联展示
5. **多表头报表** — 复杂表头合并
6. **预警报表** — 条件高亮/颜色预警
7. **数据钻取** — 点击跳转下钻明细
8. **多 Sheet 报表** — 单报表多标签页

### 图表类型

| 图表类型 | 具体图表 |
|---------|---------|
| 基础图表 | 柱形图、折线图、饼图、折柱混合图 |
| 高级图表 | 散点图、漏斗图、雷达图 |
| 特色图表 | 象形图、关系图 |
| 地图 | 中国地图、省份地图 |
| 仪表盘 | 仪表盘图 |

### 单元格功能

- 边框、字体、颜色、背景、对齐样式
- 文本自动换行/溢出/截断
- 单元格合并
- 单元格冻结（冻结行列）
- 函数支持（SUM、AVG、MAX、MIN、COUNT）
- 图片嵌入
- 超链接跳转

---

## 大屏设计器 (JimuBI)

### 特性
- 拖拽式设计，类 Word 风格操作
- 28+ 种 ECharts 图表组件
- 支持 3D 图表、地图组件
- 静态/动态数据源
- 地图组件类型：
  - 散点地图、飞线地图
  - 柱形地图、热力地图
  - 区域地图
- 装饰组件、文本组件、表格组件、视频组件
- 预览分享、密码保护、水印设置

### 大屏设计数据源配置
```sql
-- 示例：统计各部门用户数
SELECT d.dept_name, COUNT(u.user_id) as user_count
FROM sys_dept d
LEFT JOIN sys_user u ON d.dept_id = u.dept_id
GROUP BY d.dept_id, d.dept_name
ORDER BY user_count DESC
```

---

## 仪表盘设计器

### 特性
- 24 列栅格布局
- PC/手机双模式预览
- 丰富的图表类型（南丁格尔玫瑰图、面积图、进度图等）
- 交互设置：
  - 联动过滤
  - 点击跳转
  - 数据钻取
  - 自定义 JS
- 数据源支持：SQL / API / JSON / WebSocket / 静态数据

---

## 数据填报

### 功能
- 在线 Excel 风格填报
- 数据直接回写数据库
- 字段校验规则配置
- 控件支持：下拉框、字典、日期选择器
- 批量导入 Excel 数据
- 移动端填报支持

### 配置示例
1. 创建填报报表
2. 绑定数据集（含主键）
3. 设置单元格为"可编辑"
4. 配置数据回写字段映射
5. 设置校验规则（必填、格式、唯一性等）
6. 发布后用户即可在线填报

---

## 导入导出

| 格式 | 支持情况 |
|------|---------|
| Excel 导入 | ✅ 支持 |
| Excel 导出 | ✅ 支持（含样式） |
| PDF 导出 | ✅ 支持 |
| Word 导出 | ✅ 支持 |
| 图片导出 | ✅ 支持 |
| 自定义打印 | ✅ 支持 |
| 套打（发票等） | ✅ 支持 |
| 分页打印 | ✅ 支持 |
| 证照打印 | ✅ 支持 |

---

## 与 RuoYi-Vue 项目集成实践

### 集成步骤总结

1. **Maven 依赖** — 已在项目 `pom.xml` 中添加
2. **初始化 SQL** — 执行 JimuReport 提供的 MySQL 建表脚本（`jeecg_report_*.sql`）
3. **配置文件** — 在 `application.yml` 中添加 JimuReport 配置
4. **包扫描** — 确保 `@SpringBootApplication` 扫描 `org.jeecg` 包
5. **权限放行** — 配置 Security 忽略或放行 `/jmreport/**` 路径
6. **验证访问** — 启动后访问 `http://localhost:8080/jmreport/list`

### 集成到若依菜单
1. 在系统管理 → 菜单管理添加菜单
2. 菜单类型选择"外链"或"内部组件"
3. 路由地址指向 `/jmreport/list` 或通过 iframe 嵌入

### 权限集成
若依的 Spring Security 权限体系与 JimuReport 集成：
- 需要实现 `JmReportTokenServiceI` 接口进行 Token 鉴权对接
- 注入 RuoYi 的 `TokenService` 调用 `getUsernameFromToken()` 和 `verifyToken()` 完成 JWT 校验
- iframe URL 中传递 Token 参数：`/jmreport/view/{id}?token={jwt}`
- 参考官方文档的 [Token 鉴权配置](https://help.jimureport.com/config/token/)
- 参考官方 [RuoYi 集成教程](https://help.jimureport.com/projectJoin/ruoyivue/)

---

## 常见问题

### 1. 报表页面 404
- 检查 Security 是否放行了 `/jmreport/**`（本项目已配置 ✅）
- 检查包扫描是否包含了 `org.jeecg`
- 检查是否执行了初始化 SQL

### 2. 数据源连接失败
- 检查数据源配置是否正确（URL、用户名、密码）
- 检查网络连通性
- 检查数据库驱动是否已添加

### 3. 权限校验不通过
- 检查 token 传递是否正确
- 检查是否在若依的菜单权限配置中添加了对应权限标识

### 4. 导出报错
- 检查 ECharts 导出依赖是否添加（`jimureport-echarts-starter`）
- 检查服务器字体配置

---

## 与本项目的数据关联

### collect_data_cell 表 — JimuReport 的数据桥梁

项目在数据提交时（`CollectDataServiceImpl.submitData()`）会自动将 Luckysheet 单元格数据写入 `collect_data_cell` 表，供 JimuReport SQL 数据集直接查询。

**表结构概览：**
```sql
collect_data_cell:
  data_id       BIGINT    -- 关联 collect_data.data_id
  template_id   BIGINT    -- 关联模板
  sheet_index   INT       -- Sheet 序号
  row_index     INT       -- 行号 (0-based)
  col_index     INT       -- 列号 (0-based)
  cell_text     VARCHAR   -- 单元格显示文本
  cell_type     VARCHAR   -- string/number/formula
```

**JimuReport SQL 数据集示例 — 查询某模板的所有填报数据：**
```sql
SELECT
  d.data_id,
  d.title        AS 数据标题,
  d.biz_status   AS 状态,
  c.row_index    AS 行,
  c.col_index    AS 列,
  c.cell_text    AS 值
FROM collect_data d
JOIN collect_data_cell c ON d.data_id = c.data_id
WHERE d.template_id = ${templateId}
  AND d.biz_status = 'submitted'
ORDER BY d.data_id, c.sheet_index, c.row_index, c.col_index
```

**透视表示例 — 行列交叉展示（适用于固定格式的填报模板）：**
```sql
SELECT
  d.data_id,
  MAX(CASE WHEN c.row_index=0 AND c.col_index=0 THEN c.cell_text END) AS 项目,
  MAX(CASE WHEN c.row_index=0 AND c.col_index=1 THEN c.cell_text END) AS 金额,
  MAX(CASE WHEN c.row_index=0 AND c.col_index=2 THEN c.cell_text END) AS 日期
FROM collect_data d
JOIN collect_data_cell c ON d.data_id = c.data_id
WHERE d.template_id = ${templateId}
GROUP BY d.data_id
```
