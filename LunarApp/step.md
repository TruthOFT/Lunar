# 开发记录

## 2026-04-21

- 修改 `app/src/main/java/com/lunar/MainActivity.kt`
  - 将默认示例页面替换为底部导航结构。
  - 新增底部导航：排盘、记录、课程。
  - 新增排盘页面表单：姓名、起盘方式、出生时间、性别、是否保存、开始排盘按钮。
  - 优化出生时间选择框点击区域，点击后弹出日期选择器。
  - 修复 `matchParentSize` 在当前作用域下无法解析的问题。
  - 调整出生时间选择框点击层，恢复为 `Box` 作用域内的 `matchParentSize` 写法。
  - 将底部导航第三项由课程改为我的。
  - 新增我的页面未登录状态、模拟登录按钮和登录后的基础用户信息展示。
  - 调整我的页面登录流程：点击登录进入登录页，登录成功后返回我的页面展示基本信息。
  - 接入 `/api/bazi/example` 示例接口，点击开始排盘后请求接口、解析返回值并展示到排盘页。
  - 设置底部导航图标 `tint = Color.Unspecified`，保留 vector/svg 原始颜色。
  - 将排盘请求由 `/api/bazi/example` GET 改为 `/api/bazi/calculate` POST，并按表单值组装 `name`、`sex`、`solar` 参数。
- 修改 `app/src/main/AndroidManifest.xml`
  - 新增网络权限。
  - 允许开发环境 HTTP 明文接口请求。
- 新增 `step.md`，记录本次代码改动。
