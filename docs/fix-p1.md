# P1 修复记录

> 修复日期：2026-08-16
> 原则：**先核实再动**；确认不是问题的就不改；改动可能造成回归的改用零风险方案
> 验证：Java 编译通过；4 个改动的 Vue/TS 文件语法检查全部通过；关键结论用运行时实测

---

## 核实结论总览

| # | 审计发现 | 核实结果 | 处理 |
|---|---------|---------|------|
| 1 | Vue 路由权限可绕过 | ⚠️ **是真问题，但根因与审计说的不同** | ✅ 已修 |
| 2 | 前端响应体双重解包 | ✅ **确认为真**（上传/下载必失败） | ✅ 已修 |
| 3 | 合同 PDF OSS 公共读 | ❌ **审计归因有误**（那行是死代码，bucket 也不公开） | ⛔ 不改 |
| 4 | 支付 mock 需隔离 | ⚠️ 是真限制，但审计建议的方案会**弄坏功能** | 🔶 改为加启动告警 |

---

## 1. Vue 路由权限：真问题，但根因不同

### 审计的说法
"首次进入/硬刷新时 `to` 在 `addRoute` 之前解析，matched 落到 404 通配路由，权限判定恒为 true"。

### 实测发现（根因更严重）
我用项目自带的 **vue-router 4 实测**：**路由配置顶层的自定义字段会被丢弃**。

```
修复前 matched 记录上的字段:
   permissions=undefined  meta={}
   permissions=undefined  meta={"title":"护理组长审批"}
```

而权限校验读的是 `record.permissions || record.meta?.permissions` ——
两个位置都是 `undefined`，于是 `if (perms && perms.length > 0)` 恒为假，**整个校验直接跳过**。

也就是说：**前端路由权限校验从来就没有生效过**，与是否硬刷新无关。
（审计说的"第二次导航"确实会重新校验，但校验函数本身读不到权限，所以照样放行。）

### 另一个隐藏缺陷
即便权限能被读到，原实现用 `userPerms.includes(p)` 精确匹配，
超级管理员的 `*:*:*` 不包含 `code:checkout:query` 这类具体权限串 → **会把超管挡在门外**。

### 处理
1. **`permission.ts`**：改读 `record.meta?.permissions`，并让 `*:*:*` 视为拥有全部权限
2. **`router/index.ts`**：把 6 个路由的权限声明从顶层移入 `meta`，并把值从
   `nurse_leader`/`legal_staff`/`settleman_staff`/`vice_dean` 改为 **`code:checkout:query`**
   （与后端 `RetreatController` 的 `@ss.hasPermi('code:checkout:query')` 对齐）

### 为什么改成 `code:checkout:query` 而不是保留原值
核实数据库发现：
- `sys_menu` 里 **0 条** `code:*` 权限
- 实际角色只有 `admin`/`common`/`approver`，**没有任何角色拥有 `code:checkout:*`**
- 因此这些接口实际只有 `user_id=1`（拿到 `*:*:*`）能访问

原来的 `nurse_leader` 等角色 key 在系统里**根本不存在**（`sys_role.role_key` 只有 admin/common/approver），
所以那两个值都是永远匹配不上的。改成与后端一致的 `code:checkout:query` 后，
前端与后端判定**完全一致**：

| 用户 | 修复前（前端） | 修复后（前端） | 后端实际 |
|------|--------------|--------------|---------|
| admin(user_id=1) | 放行 | 放行 | 允许 |
| ry(common) | 放行 | 拦截 | 403 |
| zhuguan(approver) | 放行 | 拦截 | 403 |

**无回归**：被前端拦截的用户，其接口本来就返回 403。

> ⚠️ **遗留事项**：若希望非 admin 角色使用退住/审批模块，需要在 `sys_menu` 中补建
> `code:checkout:*` 权限点并分配给对应角色（属数据配置，建议归到 P3）。


### 重要：另有 5 处顶层 `permissions` 是**生效的**，未做改动

`router/index.ts` 的 `dynamicRoutes`（第 242/256/270/284/298 行）同样在顶层写了 `permissions`
（`system:user:edit`、`system:role:edit`、`system:dict:list`、`monitor:job:list`、`tool:gen:edit`），
看起来是同一个 bug。但核实后发现**它们走的是另一条路径，本来就没问题**：

```ts
// store/modules/permission.ts:100
export function filterDynamicRoutes(routes: any[]): any[] {
  routes.forEach(route => {
    if (route.permissions) {
      if (auth.hasPermiOr(route.permissions)) { res.push(route) }   // ← 读原始配置对象
    }
```

`filterDynamicRoutes` 的入参是**从 router 导入的原始配置对象数组**（不是 vue-router 归一化后的 matched 记录），
所以 `route.permissions` 在这里**能正常读到**；不匹配的路由根本不会被 `router.addRoute` 注册。
且 `auth.hasPermiOr` → `authPermission` 里正确实现了 `*:*:*` 通配符（第 8 行）。

**两条路径的区别**：

| 路由分组 | 权限过滤机制 | 读得到 permissions 吗 | 结论 |
|---------|------------|---------------------|------|
| `dynamicRoutes`（5 处） | `filterDynamicRoutes`（读原始配置对象） | ✅ 能 | **本来就生效，未改动** |
| `constantRoutes` 的子路由（6 处 checkout 审批页） | `hasRoutePermission`（读 matched 记录） | ❌ 被 vue-router 丢弃 | **已修复** |

这 5 处的权限点也都真实存在并分配给了 `common` 角色（实测 `sys_role_menu`），
说明作者当初按 RuoYi 惯例写对了——只有后加的 checkout 页写在了会失效的位置。

---

---

## 2. 前端响应体双重解包：确认是真 bug

### 核实
`utils/request.ts:107` 响应拦截器 `return Promise.resolve(res.data)` ——
**返回的已经是业务体**。调用点再取 `.data` 必然得到 `undefined`。

### 影响
| 位置 | 后果 |
|------|------|
| `Editor/index.vue:194-195`（粘贴图片上传） | `handleUploadSuccess(undefined)` → 读 `res.code` 抛 TypeError → 提示"图片上传失败"，**粘贴上传 100% 失败**（工具栏按钮走 `el-upload` 所以看不出来） |
| `plugins/download.ts` 三处 | `blobValidate(undefined)` → 读 `undefined.type` 抛 TypeError → 下载失败。另 `res.headers` 在 Blob 上也不存在 |

### 处理
- **`Editor/index.vue`**：`handleUploadSuccess(res as UploadFileResult, file)`（`res` 就是业务体）
- **`plugins/download.ts`**：`blobValidate(res)` / `new Blob([res])`；
  文件名改用入参（`name`）或资源路径末段——因为拦截器已丢弃 headers，`res.headers` 取不到

> 全项目 `$download` 只有一个调用点：`views/tool/gen/index.vue:228`（导出代码包）。
> 另两个方法（`name`/`resource`）目前无调用方，但同样有问题，一并修好以免日后踩坑。

---

## 3. 合同 PDF OSS 公共读：审计归因有误，**不做改动**

### 审计的说法
"`OssServiceImpl.java:52` 的 `metadata.setObjectAcl(CannedAccessControlList.PublicRead)`
是专门为合同路径加的公读，导致 PDF 公开可下载。"

### 实测发现
```java
private String upload1(InputStream in, String filename) {
    ...
    PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, in);
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setObjectAcl(CannedAccessControlList.PublicRead);  // ← 创建了但从未使用
    ossClient.putObject(putObjectRequest);                      // ← 用的是没有 metadata 的 request
```

**`metadata` 从未传给 `putObjectRequest`，这行 ACL 设置完全不生效**（死代码）。
对象 ACL 取的是 bucket 默认值。

### 对外暴露面实测
对 bucket 根做匿名访问：**HTTP 403 `AccessDenied: The bucket you access does not belong to you`**
→ 匿名列举被拒，bucket 不是 public-read。

### 结论
**这段代码并不会让合同变公共读，无需改动。** 若强行"修"（例如把 metadata 挂到 request 上）
反而会把私有改成公共。

> 仍建议你在阿里云控制台确认一次 `sfzy-000` 这个 bucket 的读写权限设置，
> 以及历史上是否有个别对象被单独设过 public-read（代码层面无法验证）。
>
> 生产建议（属功能改造，未实施）：合同对象保持私有，读取时后端用
> `ossClient.generatePresignedUrl(...)` 生成短时签名链接返回。

---

## 4. 支付 mock：真限制，但**拒绝**审计建议的方案

### 核实
`WxLoginServiceImpl.payOrder`（:310）、`payBill`（:391）直接把订单/账单状态置为已支付，
不调用微信支付网关、无回调验签。客户端可自行把订单标记为已支付。

### 为什么**不**采用 `@Profile("demo")` 隔离
审计建议"用 `@Profile("demo")` 隔离 mock 支付"。核实后**不能这么做**：
项目里**没有真实的支付实现可以兜底**。一旦加上 `@Profile("demo")` 而该 profile 未激活，
`payOrder`/`payBill` 会直接不可用（Bean 不存在 → 启动或调用报错），**这是功能回归**。

### 处理：零行为变更的启动告警
在 `WxLoginServiceImpl` 加 `@PostConstruct`，启动时打印醒目提示：
当前支付为模拟实现、禁止用于生产、上线前必须替换。**不改变任何运行逻辑**。

这样部署的人一眼能看到这个假设，而不是误以为支付可用。

---

## 改动文件清单（5 个）

| 文件 | 改动 |
|------|------|
| `lcyl-vue/src/permission.ts` | 修通配符 + 只读 meta.permissions |
| `lcyl-vue/src/router/index.ts` | 6 个路由权限声明移入 meta，值与后端对齐 |
| `lcyl-vue/src/plugins/download.ts` | 3 处双重解包修复 |
| `lcyl-vue/src/components/Editor/index.vue` | 粘贴上传双重解包修复 |
| `lcyl-java/.../WxLoginServiceImpl.java` | +23 行启动告警（无逻辑变更） |

**未改动**：`OssServiceImpl.java`（核实后确认无问题）

---

## 回归测试建议

| 场景 | 预期 |
|------|------|
| admin 登录 → 打开退住审批相关页面 | 正常（超管不受影响） |
| zhuguan(approver) 登录 → 直接输入退住审批页 URL | 被拦到 /401（与接口 403 一致） |
| 富文本编辑器 → 粘贴图片 | 图片**正常插入**（原为失败） |
| 代码生成 → 导出代码包 | **正常下载 zip**（原为报错） |
| 后端启动日志 | 出现"[重要提醒] 支付功能当前为【模拟实现】" |
