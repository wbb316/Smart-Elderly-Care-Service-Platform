# P2 修复记录

> 修复日期：2026-08-16
> 依据：`docs/audit-summary.md` 的 P2 清单
> 验证：Java 编译通过（`BedController.class` 重新生成）；9 个改动的 JS 文件 `node --check` 全部通过

---

## 一、核实结果：2 项审计发现**已经被修过了**

在动手前我逐项核实，发现两项其实早已修复，避免了重复劳动：

| 审计发现 | 核实结果 |
|---------|---------|
| `BillMapper.updateBill` 无条件更新 → payBill 并发重复支付 | ✅ **已修**。`BillMapper` 里已存在带条件的 `updatePaidBill`（`where id=#{id} and trade_status='0' and del_flag='0'`），且 `BillServiceImpl.payBill:289` 与 `WxLoginServiceImpl:430` 都已改用它，并校验 `rows <= 0` 时抛"账单已被支付或状态已变更" |
| `CheckInConfigServiceImpl.submitConfig` 无条件 insertBalance | ✅ **已修**。现在是 `if (existingBalance != null) updateElderBalance else insertBalance` 的先查后写 |

**仍存在的问题（本次修复）**：

- `BedController.add` 确实仍调用 `insertBed` 两次（下面第 4 项）
- `cancelBill` 仍用无条件 `updateBill`，但其竞态后果仅是"重复置为已关闭"，风险远低于支付，暂未改（见文末）

---

## 二、本次修复的 7 项

### 1. 小程序账单页「去支付」按钮永不渲染 🔴

**文件**：`lcyl-wx/family/pages/mybill/mybill.wxml:50`

**问题**：`wx:if="{{item.tradeStatus === 0}}"` —— 后端 `Bill.tradeStatus` 是 **String**（`Bill.java:63`），
`"0" === 0` 恒为 false，按钮永不渲染，`goPay` 成为死代码。
（同文件第 20 行用 `== 0` 松散比较所以状态文案正常，界面自相矛盾。）

**修复**：改为 `item.tradeStatus === '0'`。

**验证**：全量扫描小程序所有 wxml 的 `=== 数字` 比较，确认其余均为 `.length === 0`（Number，正确）
或已核实过的 `LcRoomType.status`（`int`，正确）。

---

### 2. 后端 BedController 重复插入床位 🟠

**文件**：`lcyl-code/.../controller/BedController.java:91`

**问题**：原实现把"校验"和"新增"当成两个动作，**各调用一次** `insertBed`：

```java
int checkResult = bedService.insertBed(bed);   // 注释说是"校验"，但它会真插入
if (checkResult == 1) { ...return 已满... }
try {
    bedService.insertBed(bed);                  // 又插一次 → 重复两条床位
```

`BedServiceImpl.insertBed`（:150）的契约是：**未满就插入并返回 0，已满返回 1**。

**修复**：合并为一次调用，按返回值分支。

**验证**：Java 编译通过，`BedController.class` 已重新生成。

---

### 3. 小程序 checkToken 弱网误判掉线 🟠

**文件**：`lcyl-wx/utils/request.js`

**问题**：`wx.request` 的 `fail`（断网、超时）也调用 `handleTokenExpired()` ——
清 token 并强制 `reLaunch` 到登录页。弱网抖动就把用户踢下线。
另外 `success` 里只要不是 200 就判过期，服务端 5xx 也会误清会话。

**修复**：
- `fail`：不再清 token、不跳登录页，仅 reject（交由调用方决定是否重试）
- `success` 非 200：改用已有的 `isTokenExpired()` 判断，**只有明确的 401/令牌无效**才清会话

"过期必跳登录"的语义不变——后端 `UserInterceptor` 对每个真实请求仍会校验，
数据请求返回 401 时 `request()` 里的 `isTokenExpired` 照样触发跳登录（兜底仍在）。

---

### 4. 小程序重复请求（refreshToken 翻倍 + 首屏请求两次）

**文件**：`utils/request.js` + 7 个页面

**问题 A**：每个 Tab 的 `onShow` 发两次 `refreshToken`
（`family.js` / `my.js` / `servicePage.js` 都显式调 `refreshSession()`，
而 `verifyToken` 的快路径里也调了一次）。

**修复 A**：移除快路径里的 `refreshSession()`。页面本来就会显式刷新，
由页面决定何时刷新更符合原有语义，且**只需改 1 个文件、不动任何页面**。

**问题 B**：7 个页面冷启动时首屏请求发两次（`onLoad` 与 `onShow` 都调加载函数）。
`onShow` 首次必定在 `onLoad` 之后触发，所以 `onLoad` 里那次是多余的。

**修复 B**：移除各页面 `onLoad` 中的加载调用，加载统一交给 `onShow`：

| 页面 | 改动 |
|------|------|
| `family/pages/leave/list/list.js` | 删除整个只调 `loadList` 的 `onLoad` |
| `my/pages/myOrder/myOrder.js` | 保留 setData，删除 `getMyOrders()` |
| `my/pages/myBillDetail/myBillDetail.js` | 保留 setData，删除 `getBillDetail()`（onShow 有 `if (this.data.id)` 守卫，setData 同步更新 data，安全） |
| `home/pages/myService/myService.js` | 保留 initNavBar + setData，删除 `getMyOrders()` |
| `pages/home/home.js` | 删除 `getBedType()` |
| `pages/servicePage/servicePage.js` | 保留 setData，删除列表请求（onShow 的 `originalList.length === 0` 守卫首次为真） |
| `my/pages/myOrderDetail/myOrderDetail.js` | 保留 `this.orderId = options.id`（实例属性同步可用），删除 `getOrderDetail()` |

**副作用**：页面从其他页返回时会照常刷新（`onShow` 仍在），行为不变。

---

### 5. 小程序下单可重复提交 🟠

**文件**：`servicePage/pages/orderConfirm/orderConfirm.js` + `.wxml`

**问题**：资金相关接口，无防重标记、按钮未 `disabled`、`showLoading` 未加 `mask`，连点产生多张订单。

**修复**：
- `data` 加 `submitting` 标记
- `submitOrder` 开头加 `if (this.data.submitting) return`
- `showLoading` 加 `mask: true`
- `.finally()` 中复位 `submitting`
- wxml 按钮加 `disabled="{{submitting}}"` 与文案切换

---

## 三、改动文件清单（12 个）

| 文件 | 改动 |
|------|------|
| `BedController.java` | 合并重复的 insertBed 调用 |
| `mybill.wxml` | 修 String 严格比较 |
| `utils/request.js` | 弱网容错 + 去重复刷新 |
| `leave/list/list.js` | 删冗余 onLoad |
| `myOrder.js` | 删冗余 onLoad 请求 |
| `myBillDetail.js` | 删冗余 onLoad 请求 |
| `myService.js` | 删冗余 onLoad 请求 |
| `home.js` | 删冗余 onLoad 请求 |
| `servicePage.js` | 删冗余 onLoad 请求 |
| `myOrderDetail.js` | 删冗余 onLoad 请求 |
| `orderConfirm.js` | 防重复提交 |
| `orderConfirm.wxml` | 按钮 disabled |

---

## 四、未处理项（说明原因）

| 项 | 为什么先不动 |
|----|-------------|
| `BillServiceImpl.cancelBill` 用无条件 `updateBill` | 后果仅为并发下重复置"已关闭"，无资金影响；改动需新增 mapper 方法，收益低风险中，建议连同 P3 的 SQL 治理一起做 |
| Vue 全项目 30+ 处 loading 无 `.catch/.finally` | 量大且分散，属机械式修改，建议单独立项 + 加 ESLint 规则约束，避免本次改动面过大难以回归 |
| Vue 路由权限、前端双重解包、合同 PDF ACL、支付隔离 | 属 P1 范围，尚未开始 |

---

## 五、回归测试建议

| 场景 | 预期 |
|------|------|
| 小程序 家人 → 账单列表 | 「去支付」按钮**正常出现**（待支付账单） |
| 小程序 断网后切页 | **不再被踢到登录页**，恢复网络后可正常加载 |
| 小程序 tab 来回切换 | 每次切换只发 **1 次** refreshToken（用开发者工具 Network 面板确认） |
| 小程序 冷启动各页面 | 首屏数据请求只发 **1 次** |
| 小程序 下单页快速连点「提交订单」 | 只生成 **1 张**订单，按钮显示"提交中..." |
| 管理端 床位管理 → 新增床位 | 只新增 **1 条**床位记录 |
| 管理端 床位管理 → 房型满员时新增 | 返回"已达上限"提示（行为不变） |
