# P0 安全修复记录

> 修复日期：2026-08-16
> 依据：`docs/audit-summary.md` 的 P0 清单
> 验证：Java 全量编译通过；改动的 JS/Vue 文件语法检查全部通过

---

## 1. 修复 approve.vue 语法错误（构建阻断）

**文件**：`lcyl-vue/lcyl/src/views/checkin/approve.vue`

**问题**：第 270-274 行残留了一段孤立的对象字面量片段（删除旧接口调用时只删了函数名与左括号，
留下了参数和 `})`），导致整个 SFC 无法解析。该组件被 `checkin/index.vue` 引用，属构建期致命错误。

**修复**：删除残留片段。

**验证**：TypeScript 编译器 `transpileModule(reportDiagnostics)` 从 **12 个语法错误 → 0 个**。

---

## 2. JWT 密钥改为环境变量注入

**文件**：
- `lcyl-java/lcyl/lcyl-admin/src/main/resources/application.yml`
- `lcyl-java/lcyl/lcyl-framework/.../web/service/TokenService.java`

**问题**：密钥明文硬编码且已提交 git（原值 64 字符，此处已脱敏不再复述——它已在 git 历史中，
本次已改为环境变量注入并轮换，旧值作废），
任何拿到该字符串的人可自行签发任意用户 token。

**修复**：
```yaml
secret: ${TOKEN_SECRET:lcyl-dev-only-secret-please-set-TOKEN_SECRET-in-production}
```
并在 `TokenService` 加 `@PostConstruct` 校验：若仍为开发默认值，启动时打印醒目告警。

**验证**：Java 编译通过；YAML 结构校验通过。

**⚠️ 需要你做的事**：
```bash
# 生成新密钥
openssl rand -base64 48
# 设置环境变量（IDE Run Configuration 或系统环境变量）
TOKEN_SECRET=<上一步生成的值>
```
**注意**：密钥一旦更换，所有用旧密钥签发的 token 立即失效，**用户需要重新登录一次**（预期行为）。

---

## 3. /upload、/uploadContractPdf 移出匿名放行清单

**文件**：
- `SecurityConfig.java`（移除 permitAll）
- `FileController.java`（新增小程序专用上传端点）
- `lcyl-wx/my/pages/myProfile/myProfile.js`（改地址 + 带 token）
- `nursingItem/index.vue`、`system/type/index.vue`、`nursingTask/index.vue`（el-upload 补 token 头）

**问题**：两个上传接口在 Spring Security 层 permitAll，任何人无需登录即可上传文件；
配合 OSS 公共读形成"任意上传 + 公开下载"。

**修复方案**（关键点：两套 token 体系不能混用）：

| 调用方 | 原地址 | 现地址 | 鉴权方式 |
|--------|--------|--------|---------|
| 小程序 | `/upload` | `/wxLogin/upload` | 落在 `/wxLogin/**` 下，由 `UserInterceptor` 校验小程序 JWT |
| 管理端 el-upload（3 处） | `/upload` | 不变 | 补 `:headers="uploadHeaders"` 携带 `Bearer` 管理端 token |
| 管理端 axios（`api/code/checkout.ts`） | 不变 | 不变 | 走 `request` 封装，已自动带 token |

之所以给小程序单开端点：小程序用的是自己的 JWT（由 `UserInterceptor` 按 URL 前缀校验），
而 Spring Security 的 `anyRequest().authenticated()` 认的是管理端 token（JWT + Redis 会话）。
若直接套用同一路径，小程序上传会被 401 拒绝。

**验证**：Java 编译通过；3 个 Vue 文件与 1 个小程序文件语法检查通过。

---

## 4. 小程序 baseUrl 改为环境自适应

**文件**：
- `lcyl-wx/config.js`（**新增**）
- `lcyl-wx/app.js`
- `lcyl-wx/utils/request.js`
- `lcyl-wx/my/pages/aiChat/aiChat.js`

**问题**：请求基址硬编码 `http://localhost:8080`（明文 HTTP + localhost），
微信正式环境要求 HTTPS + 合法域名，上线即不可用；`aiChat.js` 还有两处直接硬编码。

**修复**：新增 `config.js`，按 `wx.getAccountInfoSync().miniProgram.envVersion` 自动选择：
```js
develop → http://localhost:8080        （开发者工具/预览）
trial   → https://test.example.com     （体验版，待替换）
release → https://api.example.com      （正式版，待替换）
```
另将 `aiChat.js` 两处硬编码改为 `buildUrl()`。

**验证**：5 个 JS 文件 `node --check` 全部通过；全项目已无残留的 `localhost:8080`
（仅 `config.js` 的 develop 分支保留，属预期）。

**⚠️ 需要你做的事**：替换 `config.js` 里 trial/release 的真实域名，
并在微信公众平台「开发管理 → 服务器域名」配置为 request 合法域名。

---

## 5. 删除疑似凭据注释

**文件**：`lcyl-wx/pages/home/home.wxml:18`

**问题**：源码里残留注释 `<!-- 4e39a465...（32 位十六进制，此处已脱敏） -->`，32 位 hex 形态，
与微信 AppSecret 格式一致。全项目搜索确认该字符串**未被任何代码使用**。

**修复**：删除该注释。

**⚠️ 需要你做的事**：确认它是否为曾经使用过的真实凭据。
- 若**是**（例如曾在微信公众平台生成过该 AppSecret）→ 请到公众平台**重置 AppSecret**
- 若**否**（仅是一串无意义字符）→ 无需处理

> 已确认微信登录凭据（`WX_APPID`/`WX_SECRET`）本身是通过 `System.getenv()` 注入的，未硬编码 ✓

---

## 改动文件清单（14 个）

| 文件 | 改动 |
|------|------|
| `lcyl-vue/.../checkin/approve.vue` | -5 行（删残留片段） |
| `application.yml` | 密钥改环境变量 |
| `TokenService.java` | +21 行（启动校验） |
| `SecurityConfig.java` | 移除 2 个放行路径 |
| `FileController.java` | +13 行（小程序上传端点） |
| `lcyl-wx/config.js` | **新增** |
| `lcyl-wx/app.js` | 使用环境配置 |
| `lcyl-wx/utils/request.js` | 兜底改配置 |
| `lcyl-wx/my/pages/myProfile/myProfile.js` | 上传改端点+token |
| `lcyl-wx/my/pages/aiChat/aiChat.js` | 去硬编码 |
| `lcyl-wx/pages/home/home.wxml` | 删凭据注释 |
| `nursingItem/index.vue` | el-upload 补 token |
| `system/type/index.vue` | el-upload 补 token |
| `nursingTask/index.vue` | el-upload 补 token |

---

## 回归测试建议

由于改动了鉴权链路，建议回归以下功能：

| 场景 | 预期 |
|------|------|
| 小程序「我的」→ 换头像 | 上传成功（走 `/wxLogin/upload`） |
| 管理端 护理项目/房型/护理任务 → 上传图片 | 上传成功（带 token） |
| 管理端 退住 → 上传解除合同 PDF | 上传成功（axios 封装） |
| **未登录**状态下直接 POST `/upload` | 返回 401（修复生效） |
| 管理端登录/退出 | 正常（密钥变更后需重新登录） |
