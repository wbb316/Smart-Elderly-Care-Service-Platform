# 全项目代码审计总汇总

> 审计日期：2026-08-16
> 范围：Java 后端 + 微信小程序 + Vue 管理端
> 明细报告：`audit-backend-verification.md`、`audit-wx-miniprogram.md`、`audit-vue-admin.md`

---

## 一、总量

| 范围 | 严重 | 高 | 中 | 低 | 小计 |
|------|------|----|----|----|------|
| Java 后端（未修部分） | 4 | 4 | - | - | 8 |
| 微信小程序 | 4 | 9 | 25 | 14 | 52 |
| Vue 管理端 | 7 | 40 | 49 | 16 | **112** |
| **合计** | **15** | **53** | **74** | **30** | **172** |

> 说明：Vue 报告正文分节实测为 7/40/49/16（编号 1-112 连续）；其概览表写的
> 33/33/14 与正文不一致，此处以正文分节为准。
> Java 后端的 8 项是从既有清单 83 项中核验出的**仍未修复**部分，另有 3 类新发现。

---

## 二、🔴 严重问题（15 项）

### 认证与凭据（4）

| # | 位置 | 问题 |
|---|------|------|
| 1 | `application.yml:106` | JWT 密钥明文硬编码且已提交 git，可伪造任意用户 token |
| 2 | `login.vue:120-129` | "记住密码"把 RSA 密文口令写入 30 天期、JS 可读 Cookie，等同于明文凭据；`secure:true` 在 http 下静默失效 |
| 3 | `utils/jsencrypt.ts:13-17` + `login.vue:161-170` | `decrypt` 恒返回 `false`，回填时把密码字段写成布尔 false |
| 4 | `lcyl-wx/utils/request.js:106-111` | token 会被附加到任意 http 开头的绝对地址，可被诱导外发 |

### 权限校验失效（5）

| # | 位置 | 问题 |
|---|------|------|
| 5 | `SecurityConfig.java:114` | `/upload`、`/uploadContractPdf` 在 permitAll 清单，无需登录即可上传 |
| 6 | `permission.ts:46-62` + `router/index.ts:51-55` | 首次进入/硬刷新时权限校验被绕过（`to` 在 addRoute 前解析，matched 落 404 通配路由） |
| 7 | `router/index.ts:78,110,124,138,166,228` | 6 处 `permissions: []`（退住/清算/审批/我的待办）注册在常量路由，空数组 = 跳过校验 |
| 8 | `router/index.ts:180-220` | 路由用后端**角色 KEY**（`nurse_leader` 等）当权限点，与守卫比较的串不在同一命名空间 |
| 9 | `lcyl-wx/my/pages/myProfile/myProfile.js:119-122` | `wx.uploadFile` 未带 token，配合后端 permitAll → 任意人可上传 |

### 构建阻断（1）

| # | 位置 | 问题 |
|---|------|------|
| 10 | `checkin/approve.vue:268-274` | **语法错误，文件无法解析**：try 块内残留孤立对象字面量片段，第 274 行 `})` 无配对左括号。**已用 TypeScript 编译器实测：报 12 个语法错误**。该组件被 `checkin/index.vue` 引用，属构建期致命错误 |

### 资金与数据失真（4）

| # | 位置 | 问题 |
|---|------|------|
| 11 | `WxLoginServiceImpl.java:310,391,417` | 支付是模拟实现（`"WX"+时间戳+随机数`），客户端可免费把订单标记已支付 |
| 12 | `OssServiceImpl.java` | 合同 PDF OSS ACL = PublicRead，含身份证的合同公开可下载 |
| 13 | `views/system/SmartBed/index.vue` | 医疗监护页**整页硬编码假数据**（假心率、报警红点写死"1 楼"） |
| 14 | `views/code/checkout/billApproval.vue:337-338` 等 | 退住结算**服务天数写死 12**，欠费与退款金额由假常量算出 |

### 线上不可用（1）

| # | 位置 | 问题 |
|---|------|------|
| 15 | `lcyl-wx/app.js:4` + `utils/request.js:4` + `aiChat.js:102-126,131-145` | 请求基址硬编码 `http://localhost:8080`（明文 HTTP + localhost），微信正式环境要求 HTTPS + 合法域名；aiChat 两处还直接 `wx.request` 绕过统一封装的 401 兜底 |

---

## 三、🟠 高危（53 项，择要）

| 领域 | 代表问题 |
|------|---------|
| **Vue·系统性错误处理缺失** | loading/遮罩复位几乎都写在 `.then` 内、无 `.catch/.finally`，接口异常即整页永久卡死，**涉及 30+ 个页面** |
| Vue·响应体双重解包 | 富文本粘贴上传（`Editor/index.vue:194`）、ZIP 下载（`plugins/download.ts:16-57`）**必然失败**（拦截器已解包仍取 `.data`） |
| Vue·Vue2 迁移遗留 | `DictTag/index.vue:22` 用 Vue2 过滤器语法 → `[] \| fn` 求值为 0，字典回退值恒显示 "0"（已用 `@vue/compiler-dom` 实测） |
| Vue·启动即白屏 | `settings.ts:10` 模块顶层 `JSON.parse(localStorage)` 无 try/catch |
| Vue·守卫永久挂起 | `permission.ts` 的 `generateRoutes` 无 reject 分支；菜单接口失败则白屏且 401 弹窗被抑制 |
| Vue·keep-alive 失效 | 15 个页面组件 `name` 全为 "Config" |
| Vue·资源泄漏 | 匿名 resize 监听无法注销并调用已 dispose 的 echarts；ScrollPane 注册/注销 capture 不一致 |
| 小程序·弱网即掉线 | `request.js:88-91` checkToken 网络失败被当作"登录过期"→ 清 token 强制跳登录 |
| 小程序·重复请求 | 每个 Tab `onShow` 发两次 refreshToken；7 个页面冷启动首屏请求发两次 |
| 小程序·功能不可用 | `mybill.wxml:50` String 与 Number 严格比较 → "去支付"按钮永不渲染；`myContract.js:36-49` loading 永久 true；`roomConfirm.js:15-17` 参数漏 decode 致中文乱码/图片 404 |
| 小程序·可重复下单 | `orderConfirm.js:31-38` 无 submitting 标记、按钮未 disabled |
| 后端·并发资金 | `BillMapper.updateBill` 无条件更新 → 重复支付、重复冲抵欠费 |
| 后端·数据一致性 | `submitConfig` 无条件 insertBalance；`BedController.add` 调 insertBed 两次 |
| Vue·未导入 API | `arrival/index.vue:195` 使用未导入的 `ElMessage`（auto-import 仅覆盖 vue/router/pinia） |

---

## 四、跨模块的系统性根因（5 个模式）

这 172 项不是零散的，背后是 5 个反复出现的模式：

### 1. 从若依旧版复制粘贴后未适配
- Vue 响应体双重解包（拦截器已解包 vs 调用点再取 `.data`）
- `DictTag` 的 Vue2 过滤器语法
- `register.vue:137` 漏写 `.value`

### 2. "演示/生成态代码"直接进入生产
- `SmartBed/index.vue` 整页假医疗数据
- `billApproval.vue` 服务天数写死 12 → 金额失真
- 支付仍是 mock（`"WX"+时间戳`）
- 源码里残留疑似凭据注释（`home.wxml:18`）

### 3. 权限模型前后端不一致
- 后端：JWT 密钥可伪造 + `/upload` 放行
- 前端：常量路由 `permissions: []` + 角色 KEY 当权限点 + 刷新时绕过
- 结果：**多层"看着有校验"，实际都能绕过**——这种虚假安全感比没有校验更危险

### 4. 缺统一封装的纪律
- 小程序：2 处直接 `wx.request`、1 处 `wx.uploadFile` 绕过 `request.js`
- Vue：同一份响应存在两套解包约定
- Vue：全项目 loading 复位不写 `.catch/.finally`

### 5. 数据库与环境治理缺失
- 9 张死表（`device_data` 6 万行、`lc_retreat_copy1` 手工副本）
- 索引迁移脚本未纳入 git（但已执行到库）
- Redis 未运行
- 无 `type-check` 脚本、无构建期语法门禁（所以 #10 的语法错误能留存）

---

## 五、核验记录（重要：原报告有 1 处误报）

审计结论我做了抽样实测，结果如下：

| 结论 | 核验方式 | 结果 |
|------|---------|------|
| `approve.vue:268-274` 语法错误 | TypeScript 编译器 `transpileModule(reportDiagnostics)` | ✅ **确认**，报 12 个语法错误 |
| `role/index.vue:463` 语法错误 | 同上 | ❌ **误报**。TS 把它解析为 `<RoleDeptTreeResult>return ...` 类型断言，编译产物是 `Promise;`（空语句）+ 正常 `return`，**函数照常工作**。真实缺陷只是丢了返回类型标注（类型检查失效），不影响构建与运行 |
| 索引迁移已执行 | 直连 MySQL 查 information_schema | ✅ 12 条索引均在 |
| `mybill.wxml:50` 按钮永不渲染 | 查 `Bill.java:63` 确认 `tradeStatus` 是 `String` | ✅ `"0" === 0` 恒 false |
| `zhuguan`/`buzhang` 审批人 | 查 `sys_user` | ⚠️ 原清单判断有误：user_id 100/101 **真实存在**，流程可正常流转 |
| 9 张死表 | information_schema + 精确 SQL 引用 grep | ✅ 确认（`device_data` 59801 行零引用） |
| `permissions: []` 6 处 | 直接数源码 | ✅ 确认 |
| `DictTag` 过滤器语法 | 读源码 | ✅ 确认 |

**结论：112 条 Vue 发现中，至少 1 条（#2 语法错误里的 role/index.vue）为误报。报告其余结论抽样未见问题，可信度较高。**

---

## 六、建议处理顺序

| 阶段 | 内容 | 说明 |
|------|------|------|
| **P0（1 天）** | ① 修 `approve.vue` 语法错误（否则前端构建不过）② JWT 密钥改环境变量+轮换 ③ `/upload` 移出放行清单 ④ 小程序 baseUrl 配置化 ⑤ 删除/确认 `home.wxml:18` 疑似凭据 | 低风险小改动，收益最高 |
| **P1（3 天）** | Vue 路由权限（`permissions:[]` + 角色 KEY 两处）· 前端双重解包修复 · 合同 PDF 改私有+签名 URL · 支付加 `@Profile("demo")` 隔离 | 中等 |
| **P2（1 周）** | 小程序"去支付"按钮 · checkToken 弱网容错 · 去重复请求 · 下单防重 · 后端 updateBill 条件更新 / insertBalance 幂等 / BedController 去重 · 全项目 loading 补 `.catch/.finally` | 需回归测试 |
| **P3（计划）** | 死表清理 · 索引脚本入库 · Redis 常驻 · 加 `type-check` 与构建门禁 · 若依旧版代码全面排查 · 清理 SmartBed 假数据与写死常量 | 治理类 |
