# 微信小程序前端代码审计报告（lcyl-wx）

- 审计对象: D:\WBB_JAVA\team\lcyl-wx
- 审计范围: 38 个 .js（页面 + app.js + utils/request.js）、36 个 .wxml、app.json / project*.config.json / sitemap.json
- 审计方式: 逐文件通读 + 跨文件正则扫描（wx.request / setInterval / 存储 / 生命周期 / 绑定事件 / 数据绑定 / 图片引用），并对关键结论做了 Java 后端类型与接口的交叉验证（只用于确认前端类型假设，不重复报告后端问题）
- 本轮为只读审计，未修改任何源码文件，仅新增本报告
- 后端清单 docs/lcyl-java-code-issue-checklist.md 只覆盖 Java 侧，本报告不与其重叠；仅在必要时引用后端类型作为证据

## 结论摘要

共确认 **52 条问题：4 条🔴严重、9 条🟠高、25 条🟡中、14 条🔵低**（另附 6 条已核查但判定无问题的结论，避免误报）。清单索引：S1-S4 / H1-H9 / M1-M25 / L1-L14。

最严重的三类：
1. **环境与安全**：请求基址被硬编码为 http://localhost:8080（app.js:4、utils/request.js:4），微信正式环境不接受非 HTTPS、非白名单域名，等于线上不可用；同时有两处直接调用 wx.request 绕过统一封装（aiChat），头像上传 wx.uploadFile 未带 token。
2. **支付链路断点**：家人端账单页的"去支付"按钮因 String/Number 严格比较永远不会渲染（family/pages/mybill），该页的 goPay 成为死代码。
3. **重复请求与生命周期缺陷**：每个 Tab 每次 onShow 会把 refreshToken 请求发两次；7 个页面在冷启动时把首屏数据请求发两次；checkToken 网络失败被当作"登录过期"强制退出登录。

---

# 🔴 严重

## S1. 请求基址硬编码为 http://localhost:8080（线上不可用 + 明文 HTTP）

- 文件: lcyl-wx/app.js:4、lcyl-wx/utils/request.js:3-5
- 证据（app.js:4）:
    globalData: { baseUrl: 'http://localhost:8080', ... }
- 证据（utils/request.js:4）:
    return (app && app.globalData && app.globalData.baseUrl) || 'http://localhost:8080';
- 现象: 所有 wx.request 的 URL 由 buildUrl() 拼出，基址永远是 localhost 的明文 HTTP。真机/正式版无法访问；小程序正式环境强制 HTTPS 且要求域名在"request 合法域名"白名单内。project.private.config.json:6 的 "urlCheck": false 只在开发者工具里掩盖了这个问题。
- 根本原因: 没有任何环境区分（dev/prod）机制，地址写死在代码与兜底值里。
- 修复建议:
  1. 新增 lcyl-wx/utils/env.js，按 wx.getAccountInfoSync().miniProgram.envVersion（develop/trial/release）返回不同基址；
  2. app.js 只保留一份 baseUrl（删除 request.js:4 的 localhost 兜底，缺失时直接抛错而不是静默指向本机）；
  3. 正式基址必须为 https，并在微信后台配置 request 合法域名；把 project.private.config.json 中的 urlCheck:false 视为仅本地调试，不要提交到主干配置。

## S2. 直接调用 wx.request 绕过统一封装（无 401 处理 + 硬编码地址）

- 文件: lcyl-wx/my/pages/aiChat/aiChat.js:102-126（confirm）、:131-145（cancel）
- 证据（aiChat.js:102-106）:
    wx.request({
      url: 'http://localhost:8080/wxLogin/ai/confirm',
      method: 'POST',
      data: { sessionId: this.data.sessionId },
      header: { 'Authorization': app.globalData.token || wx.getStorageSync('token') },
- 现象: 全项目仅这两处直接调用 wx.request（其余 4 处都在 utils/request.js 内）。它们：
  1) 绕过了 request() 的 isTokenExpired/handleTokenExpired 统一 401 兜底（utils/request.js:118-125），token 过期时不会跳登录页，只会弹"操作失败"；
  2) 绕过了 buildUrl()，地址写死 localhost，切换环境时必然漏改；
  3) 文件内还存在重复的 getApp() 调用（aiChat.js:2 与 :101/:130），风格不一致。
- 根本原因: 这两个接口是后补的 AI 确认/取消功能，未复用统一封装。
- 修复建议: 改为 request({ url: '/wxLogin/ai/confirm', method: 'POST', data: { sessionId } }) 与 request({ url: '/wxLogin/ai/cancel', ... })；如确需特殊超时，给 request() 增加 options.timeout，而不是绕开它。

## S3. 头像上传 wx.uploadFile 未携带 token，绕过统一封装

- 文件: lcyl-wx/my/pages/myProfile/myProfile.js:119-123
- 证据:
    wx.uploadFile({
      url: buildUrl('/upload'),
      filePath: filePath,
      name: 'file',
      success: (uploadRes) => { ... }
- 现象: 该调用没有 header，因此没有 Authorization，完全不走 request() 的 token 注入（utils/request.js:106-109）。
- 佐证（后端，仅作类型/配置证据）: lcyl-java/.../SecurityConfig.java:114 把 /upload 放进了 permitAll，即该上传入口对未登录请求开放。
- 影响: 客户端侧无凭据校验、服务端侧该路径放行，任意人可调用上传接口（配合后端未做类型/大小限制时可作为文件落地入口）。
- 根本原因: wx.uploadFile 与 wx.request 是两套 API，封装只覆盖了后者。
- 修复建议:
  1. 在 utils/request.js 中导出 uploadFile(options)，内部统一注入 Authorization 并复用 buildUrl；
  2. myProfile.js 改为调用该封装；
  3. 后端把 /upload 从 permitAll 移出（属后端清单范畴，此处仅提示联动）。

## S4. 家人端账单页"去支付"按钮永远不渲染（String 与 Number 严格比较）

- 文件: lcyl-wx/family/pages/mybill/mybill.wxml:50
- 证据:
    <button wx:if="{{item.tradeStatus === 0}}" class="btn pay-btn" bindtap="goPay" data-id="{{item.id}}">去支付</button>
- 交叉验证: 该页数据来自 lcyl-wx/family/pages/mybill/mybill.js:47-55 的 POST /wxLogin/list；后端 WxLoginController.java:272-275 返回 success(iBillService.selectBillList(bill))，BillServiceImpl.java:48 声明为 List<Bill>，Bill.java:63 的 tradeStatus 是 String（Jackson 序列化为 JSON 字符串 "0"）。
- 现象: "0" === 0 恒为 false，按钮永不显示；同一文件 :20-21 的状态文案用的是宽松比较 == 0 所以能正常显示"待支付"，页面上于是出现"状态是待支付、却没有去支付按钮"的自相矛盾。mybill.js:75-87 的 goPay 因此成为不可达的死代码。
- 影响: 家属无法从该账单页发起支付（只能绕到"查看明细 → myBillDetail"再支付），核心付费路径断裂。
- 根本原因: 同一份数据里混用了字符串状态与数字字面量，且该页未像 my/pages/myBill/myBill.js:80-92 那样先做 String(tradeStatus) 归一化。
- 修复建议: 统一以字符串比较：wx:if="{{item.tradeStatus === '0'}}"；更稳妥的做法是在页面 js 里映射出 status 字段（参考 myBill.js:79-108），wxml 只依赖映射后的枚举值。

---

# 🟠 高

## H1. checkToken 网络失败被当作"登录过期"，直接清 token 并跳登录页

- 文件: lcyl-wx/utils/request.js:88-91（慢路径 fail 分支）
- 证据:
    fail: function () {
      handleTokenExpired();
      reject(new Error('网络错误'));
    }
- 现象: verifyToken() 的慢路径只要 wx.request 失败（弱网、超时、DNS 抖动、后端重启）就执行 handleTokenExpired()（:142-149）→ 清空 globalData.token、removeStorageSync('token')、reLaunch 到登录页。
- 影响: 一次瞬时网络抖动就会把用户踢下线并丢失本地登录态，必须重新走微信一键登录。
- 根本原因: 把"请求失败"与"令牌失效"合并成同一条分支。
- 修复建议: fail 分支只 reject（或提示"网络异常，请重试"），不要清 token/跳登录；只有收到 401 或 code===401 时才调用 handleTokenExpired()。

## H2. 每次 Tab 切换发送两次 refreshToken 请求

- 文件: lcyl-wx/pages/family/family.js:66-70、lcyl-wx/pages/my/my.js:69-74、lcyl-wx/pages/servicePage/servicePage.js:28-36
- 证据（family.js:66-70）:
    onShow() {
      verifyToken().then(() => {
      refreshSession();
      this.getElderList();
      }).catch(() => {});
    },
- 证据（utils/request.js:64-72，verifyToken 的本地快路径）:
    if (payload.exp - nowSec > LOCAL_VERIFY_GRACE_SECONDS) {
      refreshSession();      // ← 快路径内部已经调了一次
      resolve();
- 现象: 三个 Tab 页在 onShow 里先 await verifyToken()（内部已触发一次 refreshSession），再显式调用 refreshSession()，同一次页面显示发出 2 个 GET /wxLogin/refreshToken。
- 影响: 每次切 Tab 多一个无效往返；三个高频 Tab 叠加后是稳定可观测的额外 QPS 与首屏延迟。
- 根本原因: refreshSession 的调用职责被写在两处（封装内的滑动续期 + 页面显式调用），页面侧未意识到封装已处理。
- 修复建议: 删除三个页面 onShow 里的显式 refreshSession() 调用，续期统一交给 verifyToken()；若希望续期与页面解耦，则把 refreshSession 从 verifyToken 中移出，只保留页面调用的一处。

## H3. 7 个页面冷启动时首屏数据请求重复发送一次

- 文件:
  - lcyl-wx/my/pages/myOrder/myOrder.js:26-35（onLoad 与 onShow 都调 getMyOrders）
  - lcyl-wx/my/pages/myOrderDetail/myOrderDetail.js:32-41（onLoad 与 onShow 都调 getOrderDetail）
  - lcyl-wx/my/pages/myBillDetail/myBillDetail.js:14-25（onLoad 与 onShow 都调 getBillDetail）
  - lcyl-wx/family/pages/leave/list/list.js:16-22（onLoad 与 onShow 都调 loadList）
  - lcyl-wx/home/pages/myService/myService.js:32-46（onLoad 与 onShow 都调 getMyOrders）
  - lcyl-wx/pages/home/home.js:27-29 与 :60-63（onLoad + onShow 都调 getBedType）
  - lcyl-wx/pages/servicePage/servicePage.js:20-36（onLoad + onShow 都调 getServiceList/getFamilyList）
- 证据（myOrder.js:26-35）:
    onLoad(options) { this.setData({ currentTab: options.tab || 'all' }); this.getMyOrders() },
    onShow() { this.getMyOrders() },
- 现象: 小程序首次进入页面会依次触发 onLoad → onShow，两者都发同一请求，首屏一定重复。
- 补充: servicePage.js:31 的防重入判断 if (this.data.originalList.length === 0) 无效——onLoad 的请求是异步的，onShow 执行时 originalList 仍为 []，两次请求都会发出。
- 影响: 首屏接口调用量翻倍；对 /wxLogin/myOrders、/wxLogin/getElderBedList 这类带聚合查询的接口会明显拖慢首屏。
- 根本原因: 没有区分"首次加载"与"回到页面刷新"。
- 修复建议: 统一用 onShow 触发 + 首次标记，例如：
    onLoad(options){ this._loaded=false; this.tab=options.tab },
    onShow(){ if(!this._loaded){ this._loaded=true; ...初始化 } this.getMyOrders() }
  或统一只保留 onShow（onLoad 只做参数解析），二选一，不要两处都发请求。

## H4. 我的合同：接口非 200 时 loading 永久为 true，页面卡在"加载中"

- 文件: lcyl-wx/my/pages/myContract/myContract.js:36-49
- 证据:
    getMyElderList() {
      this.setData({ loading: true })
      request({...}).then((resp) => {
        if (resp.data.code == 200) {
          let elderList = resp.data.data || []
          this.setData({ elderList })
          this.getAllContractForAllElders(elderList)   // 只有这里最终会 loading:false
        }
        // ← 没有 else，也没有 finally
      }).catch(() => { this.setData({ loading: false }) })
    }
- 现象: 业务返回 code != 200（例如未绑定老人、会话失效的业务码）时，既没有 setData({loading:false})，也没有任何提示，wxml 的 wx:if="{{loading}}" 分支（myContract.wxml:16-18）会一直显示"加载中..."，空状态分支（:21-24）永远走不到。
- 根本原因: 只有成功分支和 catch 分支负责关闭 loading，业务失败分支被遗漏。
- 修复建议: 把 setData({loading:false}) 放到 .finally() 里（该文件 getAllContractForAllElders 已经用了 finally，风格应统一），并在 code != 200 时给 wx.showToast 提示。

## H5. 我的合同：按合同逐条发请求（N+1）且无 catch，data 为空时直接抛错

- 文件: lcyl-wx/my/pages/myContract/myContract.js:52-69、:133-137
- 证据（:133-137）:
    list.forEach((item, index) => {
      if (item.elderId) {
        this.getElderPhoto(item.elderId, index)
      }
    })
- 证据（:55-59）:
    request({ url: `/wxLogin/bed/${elderId}`, method: 'GET' }).then((res) => {
      let photo = res.data.data.photo || ""      // data 为 null 时抛出 TypeError
- 现象: 合同列表每有一条就单独请求一次 /wxLogin/bed/{elderId}（N 条合同 = N 个串行/并发请求），并直接对 res.data.data 取属性；该 then 没有 .catch，一旦某条失败或 data 为 null，异常无人处理，对应合同的头像/房型信息静默丢失。
- 影响: 合同条数多时首屏请求数线性膨胀，是典型的接口雪崩点；失败无提示，用户看到空头像却不知原因。
- 修复建议:
  1. 让后端在 /wxLogin/contract/list 的返回里带 photo/roomCode/roomTypeName（最优），前端不再补请求；
  2. 若必须补，改为并发且有上限的批量拉取（Promise.all + 结果按 elderId 建索引一次性 setData，避免 N 次 setData 引发 N 次渲染）；
  3. 补 .catch 与空值保护：const d = res.data && res.data.data; if (!d) return;

## H6. 房型预定参数未解码且被二次编码，中文房型名与图片 URL 显示错误

- 文件: lcyl-wx/home/pages/roomConfirm/roomConfirm.js:12-20（接收端）、:60、:63（转发端）；发送端 lcyl-wx/home/pages/roomDetail/roomDetail.js:56-61
- 证据（roomDetail.js:56-61，已编码传出）:
    url: `/home/pages/roomConfirm/roomConfirm?roomTypeId=${roomInfo.id}&roomName=${encodeURIComponent(roomInfo.name)}&price=${roomInfo.price}&photo=${encodeURIComponent(roomInfo.photo || '')}&bookingDate=${encodeURIComponent(bookingDate)}`
- 证据（roomConfirm.js:12-19，原样收下，未 decode）:
    detail: { id: options.roomTypeId, name: options.roomName, price: options.price, photo: options.photo }
- 证据（roomConfirm.js:60、:63，再次编码转发）:
    &serviceName=${encodeURIComponent(data.detail.name)}  ... &imageUrl=${encodeURIComponent(data.detail.photo || "")}
- 现象: roomName/photo 传入时是编码串，roomConfirm 没有 decodeURIComponent（同页的 bookingDate 却解了，:19），页面上直接显示 %E5%AE%89...；转发到 payPage 时又 encodeURIComponent 一次，而 payPage.js:27-30 只 decode 一次，最终 payPage 拿到的仍是编码串。
- 影响: 房型预定确认页与支付页的房型名显示为乱码、房型图片 404。
- 根本原因: URL 参数编解码没有成对约定，同一页面内也不一致。
- 修复建议: 统一规则——传参一律 encodeURIComponent，接收端一律 decodeURIComponent（可抽成 utils/url.js 的 parseQuery）。最短修复：roomConfirm.js:15-17 改为 name: decodeURIComponent(options.roomName || ''), photo: decodeURIComponent(options.photo || '')，同时保持转发端编码，形成"编码一次、解码一次"的闭环。

## H7. 探访预约缺少必填校验，可提交空预约时间

- 文件: lcyl-wx/home/pages/eldervisit/eldervisit.js:158-186
- 证据:
    submitBooking() {
      const { name, phone, familyName, selectedDateTime } = this.data;
      // 手机号格式校验
      if (!/^1[3-9]\d{9}$/.test(phone)) { ... return; }
      request({ url: '/wxLogin/addvisitor', method: 'POST', data: { type: 1, name, phone, olderName: familyName, appointmentTime: selectedDateTime } })
- 对照: 同源的 customVisit.js:142-151 先做了 if (!name || !phone || !date || !selectedTime) 校验，探访页把这层校验删掉了。
- 现象: name 为空、selectedTime/selectedDateTime 为空（用户没点任何时间段）都能提交，appointmentTime 传空串给后端 /wxLogin/addvisitor。
- 影响: 后端落库一条无预约时间的记录，家属"我的预约"列表时间显示为无效日期（myApply.js:50-57 用 new Date(item.appointmentTime) 直接格式化）。
- 修复建议: 在 eldervisit.js:158 补上 customVisit.js:142-151 的同等校验（name/date/selectedTime 非空 + 时间已被选择），并复用同一份校验函数（见 L5 的重复代码建议）。

## H8. 提交订单可重复点击，产生重复订单

- 文件: lcyl-wx/servicePage/pages/orderConfirm/orderConfirm.js:31-38（缺少提交中标记）、lcyl-wx/servicePage/pages/orderConfirm/orderConfirm.wxml:35（按钮无 disabled）
- 证据（orderConfirm.wxml:35）:
    <button class="submit-btn" bindtap="submitOrder">提交订单</button>
- 证据（orderConfirm.js:38-47）:
    wx.showLoading({ title: "提交中..." })
    request({ url: "/wxLogin/createOrder", method: "POST", ... })
- 现象: showLoading 未加 mask:true（默认为 false，遮罩不拦截点击），按钮也没有 disabled 绑定，用户连点两次就会发出两次 createOrder。
- 对照: home/pages/registerElder/registerElder.js:48+（有 submitting 标记且 wxml:41 绑定了 disabled）、family/pages/leave/apply/apply.js:97（有 if (submitting) return）、my/pages/myProfile/myProfile.js:185-187（有 saving 判断）——说明项目里已有正确写法，只有下单链路漏了。
- 影响: 重复订单/重复扣款风险，属资金相关路径。
- 修复建议: 在 data 中加 submitting: false，submitOrder 首行 if (this.data.submitting) return; this.setData({submitting:true})，在 finally 里复位；wxml 按钮加 disabled="{{submitting}}"；同一问题也适用于 lcyl-wx/servicePage/pages/payPage/payPage.js:71-155 的 confirmPay（见 M10）。

## H9. 聊天页自动滚动无效：wx.pageScrollTo 无法滚动 scroll-view

- 文件: lcyl-wx/my/pages/aiChat/aiChat.js:148-152、lcyl-wx/my/pages/aiChat/aiChat.wxml:2
- 证据（aiChat.wxml:2）:
    <scroll-view class="msg-list" scroll-y scroll-with-animation upper-threshold="50" lower-threshold="50">
- 证据（aiChat.js:148-152）:
    scrollToBottom() {
      setTimeout(() => {
        wx.pageScrollTo({ scrollTop: 99999 });
      }, 100);
    }
- 现象: 消息列表在一个 scroll-view 内，整页并不滚动，wx.pageScrollTo 对 scroll-view 内部偏移无效；scroll-view 上也没有绑定 scroll-into-view / scroll-top。
- 影响: 发送消息后新回复出现在可视区之外，用户必须手动往下滑，是聊天页最直观的体验缺陷。
- 修复建议: 给 data 增加 scrollIntoView（或 scrollTop），给 scroll-view 绑定 scroll-into-view="{{scrollIntoView}}"，每条消息生成唯一 id（同时把 aiChat.wxml:3 的 wx:key="index" 换成 id），scrollToBottom() 时 setData 该值为最后一条消息的 id。

---

# 🟡 中

## M1. 我的预约：confirmCancel 调用不存在的方法，点击即 TypeError（当前为潜在缺陷）

- 文件: lcyl-wx/my/pages/myApply/myApply.js:223-254；触发点 lcyl-wx/my/pages/myApply/myApply.wxml:53
- 证据（myApply.js:223-254 节选）:
    confirmCancel() {
      const { currentCancelId, list } = this.data
      const newList = list.map(item => { ... })     // data.list 恒为 []（全页面从不写入）
      this.setData({ list: newList, showModal: false, currentCancelId: null })
      this.updateFilteredList()                     // ← 该方法在文件中不存在
      wx.showToast({ title: '已取消预约', icon: 'success' })
    }
- 现象: this.updateFilteredList 未定义，第 248 行必然抛 "is not a function"；且 data.list(:32) 从未被赋值（页面渲染的是 filteredList），即使方法存在也不会有任何效果。
- 当前可达性: showModal 只由 handleCancel(:189-214) 置 true，而 handleCancel 没有被任何 wxml 绑定（全项目扫描：wxml 只绑定了 showCancelConfirm/confirmCancel/closeModal），所以这条路径目前点不到——属于"一旦接上就崩"的潜在缺陷 + 死代码。
- 修复建议: 要么删除 handleCancel/confirmCancel/showModal/modalType 这套未接线的代码，要么补齐实现（改用 filteredList 并对后端确认后用 getFilteredList() 重新拉取），不要在本地伪造取消结果。

## M2. 我的预约：用 id % 2 决定弹窗文案（疑似演示代码残留）

- 文件: lcyl-wx/my/pages/myApply/myApply.js:189-214
- 证据:
    let modalType = 1
    if (id % 2 === 0) { modalType = 2 }
- 现象: 按记录主键的奇偶决定显示"取消3次后不可预约"还是"今日已不可预约"，与真实业务规则（应由后端返回的取消次数/是否可预约决定）无关；注释里也自承"这里模拟两种不同提示"。
- 影响: 一旦该弹窗被接线，用户会看到与自身状态不符的规则提示。
- 修复建议: 删除该函数（改用已实现的 showCancelConfirm + doCancel，:151-187 是走真实接口的正确实现）。

## M3. 我的预约：列表请求无 catch，失败时静默且提示误导

- 文件: lcyl-wx/my/pages/myApply/myApply.js:108-124
- 证据:
    getFilteredList() {
      const status = this.data.currentTab
      request({ url: '/wxLogin/selectVisitInfo', method: 'POST', data: { status } })
        .then((res) => { if (res.data.code === 200) { ... this.setData({ filteredList: formattedList }) } })
      // 没有 .catch，也没有 else
    }
- 现象: 网络失败或非 200 时无任何提示，filteredList 保持旧值或空数组，wxml:41 会显示"暂无预约记录"——把"加载失败"伪装成"没有数据"。
- 修复建议: 补 .catch 与 else 分支，分别给出"网络异常，请重试"和 res.data.msg 提示；可加 loading 状态区分三种态。

## M4. 家人端账单：用 null 作为"全部"标签的哨兵值，经 data-* 往返后判断失效

- 文件: lcyl-wx/family/pages/mybill/mybill.js:5-13、:43-45；lcyl-wx/family/pages/mybill/mybill.wxml:9
- 证据（mybill.js:5-13）:
    tabs: [ { name: '全部', tradeStatus: null }, { name: '待支付', tradeStatus: 0 }, ... ]
- 证据（mybill.wxml:9）:
    data-tradestatus="{{item.tradeStatus}}"
- 证据（mybill.js:42-45）:
    let postData = { elderId };
    if (tradeStatus !== null) { postData.tradeStatus = tradeStatus; }
- 现象: data-* 属性在渲染/取值过程中会被字符串化，null 往返后不是 null（为空串或 undefined），因此 ! == null 判定为真，切到"全部"时仍会把一个空值/未定义值带进 postData.tradeStatus。前端无法保证其被 JSON 序列化时剔除，导致"全部"要么被后端当成空条件过滤、要么依赖引擎行为。
- 影响: "全部"标签可能查出空列表或与预期不一致的结果，属于不确定行为。
- 修复建议: 不要用 null 做哨兵。方案一：全部标签用 tradeStatus: '' 并在 js 中显式判断 if (tradeStatus !== '' && tradeStatus !== null && tradeStatus !== undefined)。方案二（更稳）：让 wxml 只传标签下标 data-index，由 js 用 this.data.tabs[index].tradeStatus 取值，绕开 data-* 的类型转换。

## M5. 家人端账单：跳转支付页只传 2 个参数，支付页金额为空

- 文件: lcyl-wx/family/pages/mybill/mybill.js:82-86
- 证据:
    url: `/servicePage/pages/payPage/payPage?sourceType=bill&billId=${bill.id}&totalPrice=${bill.payableAmount || 0}`
- 对照: lcyl-wx/my/pages/myBill/myBill.js:149-158 同样的"去支付"传了 serviceName/price/familyName/serviceTime 全套参数。
- 现象: payPage 的 serviceName/price/familyName/serviceTime 全为空串；payPage.wxml:3 的 ¥{{totalPrice}} 能显示，但其余上下文信息缺失；payPage.js:32-33 的 decodeURIComponent('') 也让 familyName/serviceTime 为空。
- 备注: 由于 S4，该按钮目前根本点不到，本条是在修复 S4 后必须一起修的问题。
- 修复建议: 与 myBill.js 保持一致补齐参数（serviceName=账单标题、price/totalPrice=payableAmount、familyName=elderName、serviceTime=payDeadline 的格式化值）。

## M6. 家人端账单：payDeadline 未做格式化直接渲染

- 文件: lcyl-wx/family/pages/mybill/mybill.wxml:39
- 证据:
    <text>支付截止时间：{{item.payDeadline}}</text>
- 交叉验证: 该页数据源 /wxLogin/list 返回的是裸实体 List<Bill>（BillServiceImpl.java:48）；Bill.java:68 的 payDeadline 是 java.util.Date 且没有 @JsonFormat 注解（对照 BaseEntity.java:28 的 createTime 有 @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")），全仓库 yml/properties 中也查不到 spring.jackson.date-format（0 命中）。
- 现象: createTime 会被格式化成 "yyyy-MM-dd HH:mm:ss"，而 payDeadline 走 Jackson 默认日期序列化，页面直接显示未加工的日期值（带 T 与时区的 ISO 串，或时间戳，取决于序列化特性配置）——同一行上下两个时间格式不一致，且不可读。
- 佐证: 项目其它页面都显式格式化该字段（如 my/pages/myBill/myBill.js:102 走 formatDateTime），说明前端本就知道它不可直接展示。
- 修复建议: 二选一——后端给 Bill.payDeadline 加 @JsonFormat（推荐），或前端在 mybill.js 里对数据做 map 归一化后再 setData（参考 myBill.js:79-108）。

## M7. 合同详情页存在重复的 onLoad 定义，第一份为死代码

- 文件: lcyl-wx/my/pages/contractDetails/contractDetails.js:15-17 与 :29-43
- 证据:
    onLoad(options) {          // 第 15 行，空实现
    },
    onReady() { ... },
    onLoad(options) {          // 第 29 行，同一个 key 又定义一次
      const roomCode = options.roomCode
      ...
    }
- 现象: 同一个对象字面量里 onLoad 出现两次，后者覆盖前者，前者是永不执行的死代码；在严格模式/ESLint(no-dupe-keys) 下会直接报错，也让代码阅读者误判入口。
- 修复建议: 删除 :15-17（以及紧随其后的空 onReady 可按需保留），只保留 :29-43 的实现。

## M8. 服务页 onPullDownRefresh 永远不会触发，且固定延时关闭刷新动画

- 文件: lcyl-wx/pages/servicePage/servicePage.js:172-175；配置 lcyl-wx/pages/servicePage/servicePage.json
- 证据（servicePage.js:172-175）:
    onPullDownRefresh() {
      this.getServiceList()
      setTimeout(() => wx.stopPullDownRefresh(), 1000)
    }
- 证据: 全项目 36 个页面 json 中没有任何一个声明 enablePullDownRefresh: true（全局扫描 "enablePullDownRefresh" 命中 0）。
- 现象: 该回调是死代码（下拉刷新被禁用，回调不会执行）；即使开启，它在请求尚未返回时就固定 1 秒后关闭动画，慢请求下会出现"刷新条已收起但数据还在变"。
- 修复建议: 在 servicePage.json 中加 "enablePullDownRefresh": true，并把 wx.stopPullDownRefresh() 放到 getServiceList 的 finally 里（getServiceList 目前返回 Promise，直接链式 .finally 即可）。

## M9. 支付页把定时器句柄写入 data，并每秒 setData 一次

- 文件: lcyl-wx/servicePage/pages/payPage/payPage.js:19、:40-58、:60-69
- 证据:
    data: { ..., timer: null },
    const timer = setInterval(() => { total -= 1; this.updateTime(total); if (total <= 0) { clearInterval(this.data.timer); ... } }, 1000);
    this.setData({ timer });
- 现象:
  1) 定时器句柄（非序列化对象）放进 data 并 setData 到视图层，是不必要的跨线程传输；
  2) 每秒一次 setData({countDown})，一分钟 60 次渲染通信，属于典型的高频 setData 反模式；
  3) startCountDown(900) 对所有 sourceType 都固定 15 分钟，账单（bill）并没有 15 分钟支付窗口，超时后 payPage.wxml:17 的按钮会被永久禁用（isTimeout 无法恢复），账单也点不了支付。
- 修复建议: 定时器句柄挂在 this 上（this.timer）而非 data；倒计时改为整秒节流（如每 5 秒 setData 一次，或只在整分变化时更新）；账单场景不启动倒计时（if (this.data.sourceType === 'bill') return;），或由后端返回真实截止时间驱动。

## M10. 支付页缺少防重复提交

- 文件: lcyl-wx/servicePage/pages/payPage/payPage.js:71-155
- 证据:
    confirmPay() {
      if (this.data.isTimeout) { ... return; }
      ...
      wx.showLoading({ title: "支付中..." });     // 未加 mask
      request({ url: payUrl, method: "POST" })
      ...
      setTimeout(() => { wx.redirectTo/navigateTo(...) }, 1000);   // :115 成功后仍有 1 秒窗口
- 现象: 支付请求发出到 1 秒后跳转之间，按钮仍可点击，可重复发出 payOrder/payBill/payRoomBooking（资金接口）。
- 修复建议: 增加 paying 标记（进入时置 true，finally 复位）并给 showLoading 加 mask: true；wxml 按钮绑定 disabled="{{isTimeout || paying}}"。

## M11. 服务详情页拉全量列表找单条，且找不到时 detail 为 undefined 会抛错

- 文件: lcyl-wx/servicePage/pages/serviceDetail/serviceDetail.js:37-47、:114
- 证据（:37-47）:
    request({ url: "/wxLogin/item/list" }).then((res) => {
      let list = res.data.rows || []
      let current = list.find(item => item.id == this.data.id)
      this.setData({ detail: current })          // 未找到 → undefined
    })
- 证据（:113-114）:
    url: `...orderConfirm?serviceId=${detail.id}&serviceName=${detail.itemName}...`    // detail 为 undefined 时抛 TypeError
- 现象: 为拿一个项目详情而拉取完整服务列表（列表可能很大），且未命中时没有兜底；同时该 then 没有 .catch。
- 修复建议: 后端已有 GET /wxLogin/item/{id}（WxLoginController.java:155-158），直接改调该接口；对未找到的情况 setData({detail:{}}) 并提示"服务不存在"后 navigateBack，避免 undefined 参与拼接。

## M12. 下单参数中 imageUrl 未编码而其它参数已编码

- 文件: lcyl-wx/pages/servicePage/servicePage.js:168、lcyl-wx/servicePage/pages/serviceDetail/serviceDetail.js:114
- 证据（servicePage.js:167-169）:
    url: `/servicePage/pages/orderConfirm/orderConfirm?serviceId=${selectedService.id}&serviceName=${selectedService.itemName}&price=${selectedService.price}&unit=${selectedService.unit}&familyId=${familyId}&familyName=${familyName}&serviceTime=${encodeURIComponent(fullTime)}&imageUrl=${selectedService.imageUrl}`
- 现象: imageUrl 直接拼接；若它是含 ?/&/= 的完整 URL（如带 OSS 签名的地址），会把参数截断，serviceName/familyName 等同样未编码的中文也可能在部分机型上出现解析异常。
- 修复建议: 所有字符串参数统一 encodeURIComponent（接收端 orderConfirm.js:12-25 已有部分 decode，注意补齐 imageUrl 的 decode）；更彻底的做法是改用 eventChannel 或 globalData 传递对象，避免长 URL 拼参（项目里 roomSuccess 已经用 globalData 传参，可复用该模式）。

## M13. 统一封装把 token 附加到任意以 http 开头的绝对地址

- 文件: lcyl-wx/utils/request.js:106-111
- 证据:
    var token = app.globalData.token || wx.getStorageSync('token');
    if (token) { header['Authorization'] = token; }
    var url = options.url && options.url.startsWith('http') ? options.url : buildUrl(options.url);
- 现象: 只要调用方传入绝对 URL，JWT 就会随 Authorization 头发给该地址。当前唯一的绝对 URL 调用点是 aiChat 里写死的 localhost（S2），暂无对外泄漏；但这是封装层的安全隐患，一旦后续有人传入第三方地址就会静默泄露登录态。
- 修复建议: 绝对 URL 一律拒绝（或仅允许白名单域名），把 token 注入限制在 buildUrl 生成的同源地址上。

## M14. 以"500 + msg 含'登录'"判定登录过期过于宽松

- 文件: lcyl-wx/utils/request.js:133-140
- 证据:
    if (res.data.code === 500 && res.data.msg && res.data.msg.indexOf('登录') !== -1) return true;
- 现象: 任何返回 500 且文案里带"登录"的业务错误（例如"该手机号未登录过""请重新登录后再试"之类的提示语）都会被判为登录过期，触发 handleTokenExpired() 清 token 并跳登录页。
- 修复建议: 只认 HTTP 401 与业务 code===401；确需兼容老接口时改为精确匹配错误码常量，不要用中文子串包含判断。

## M15. 健康数据页标题里的老人姓名永远为空

- 文件: lcyl-wx/family/pages/healthData/healthData.wxml:12、lcyl-wx/family/pages/healthData/healthData.js:4-5、:13-16
- 证据（wxml:12）:
    <view wx:elif="{{noData}}" class="empty">{{elderName}}暂无健康数据</view>
- 证据（js:4-5, 13-16）: data 里声明了 elderName: ''，onLoad 只 setData 了 elderId，全文件再无赋值。
- 现象: 无数据时页面显示"暂无健康数据"，姓名位置空缺；同时 elderId 明明已在 URL 中传入（family.js:48），却完全可以用来展示姓名。
- 修复建议: 由跳转方带上 elderName（family.wxml:19 的 data-id 旁再加 data-name），或调用后端老人信息接口补齐；至少不要把空变量拼进文案。

## M16. 聊天页消息项使用 wx:key="index"，且确认条把 wx:if 与 wx:for 写在同一元素上

- 文件: lcyl-wx/my/pages/aiChat/aiChat.wxml:3、:13
- 证据（:3）: <view wx:for="{{messages}}" wx:key="index" ...>
- 证据（:13）: <view wx:if="{{item.needsConfirm}}" class="confirm-bar" wx:for="{{messages}}" wx:for-index="">
- 现象:
  1) wx:key="index" 在列表只追加的聊天场景下会导致节点复用错乱（配合 :2 的 scroll-with-animation 更明显），应用消息唯一 id；
  2) wx:for 与 wx:if 写在同一元素且 wx:for-index="" 为空字符串，是明确的误用（官方文档要求拆分为 block/wx:for + 内层 wx:if）。若历史消息中存在两条及以上带 needsConfirm 的记录，会渲染出多组"确认执行/取消"按钮，而 confirmAction()（aiChat.js:94-97）只认最后一条，点前面的按钮会对错误的会话执行确认。
- 修复建议: 把 :13 改为 <block wx:for="{{messages}}" wx:key="id"><view wx:if="{{item.needsConfirm}}" ...>；消息对象补充唯一 id（后端历史消息若没有，可在格式化时补 uuid/时间戳）；同时把 :3 的 wx:key 换成 id。

## M17. AI 会话 ID 中的 memberId 永远是空值

- 文件: lcyl-wx/my/pages/aiChat/aiChat.js:15-17；佐证 lcyl-wx/pages/index/index.js:31-42
- 证据（aiChat.js:15-17）:
    const memberId = wx.getStorageSync('memberId') || '';
    const sessionId = `member_${memberId}_${today}`;
- 现象: 全项目对 memberId 只有这一处读取（grep 命中 2 行，均为本文件），登录成功时（index.js:36）只写了 token，从未写入 memberId，因此 sessionId 恒为 "member__YYYYMMDD"——所有用户共用同一个会话键。
- 影响: 后端 getHistoryMessages/ask 仍按登录态 memberId 过滤（WxLoginController.java:478-500），所以不构成跨用户数据泄漏；但前端设计的"按成员+按天"会话隔离完全失效，且同一天内重装/换设备后历史无法与预期一致。
- 修复建议: 二选一——登录成功时 wx.setStorageSync('memberId', resultData.memberId)（需后端在登录响应中返回该字段）；或直接删掉 memberId 拼接，把会话键交给后端按登录态生成，前端不再自行构造。

## M18. userInfo 只写不读，登出也不清理

- 文件: lcyl-wx/my/pages/myProfile/myProfile.js:230-234；lcyl-wx/pages/my/my.js:22-28
- 证据（myProfile.js:230-234）:
    wx.setStorageSync('userInfo', { name: name.trim(), avatar, gender })
- 证据: 全项目 "userInfo" 仅此一处（只写不读）。
- 现象: 保存资料后写入的本地缓存没有任何消费方，属于无效状态；同时 logout()（my.js:22-28）只清 token，不清 userInfo/memberId，也没有二次确认。
- 修复建议: 要么删除该写入，要么在"我的"页用本地缓存做首屏占位（再被接口数据覆盖）；logout 一并清除 userInfo/memberId 并加 wx.showModal 确认。

## M19. 消息已读状态直接原地修改 this.data 并回传同一引用

- 文件: lcyl-wx/my/pages/notification/notification.js:83-95（markRead）、:105-109（markAllRead）
- 证据（:91-94）:
    if (res.data && res.data.code === 200) {
      item.isRead = 1;
      this.setData({ list: this.data.list });
    }
- 现象: 先直接改 this.data.list 里的元素，再用同一数组引用 setData。这种写法依赖框架不做等值短路才生效，属于官方明确不推荐的用法；一旦框架或基础库做 diff 优化，界面就不会更新。
- 修复建议: 用不可变更新：const list = this.data.list.map(i => i.id === id ? { ...i, isRead: 1 } : i); this.setData({ list });（markAllRead 已是 map 写法，保持一致即可）。

## M20. 家人端账单把 URL 里的 elderId 直接当作查询条件（缺少归属校验）

- 文件: lcyl-wx/family/pages/mybill/mybill.js:16-25、:36-50
- 证据:
    onLoad(options) { if (options.id) { this.setData({ elderId: options.id }); this.getBillList(...) } },
    request({ url: '/wxLogin/list', method: 'POST', data: postData })   // postData.elderId 来自 URL
- 交叉验证（仅作事实说明，后端授权问题归 Java 清单）: WxLoginController.java:272-275 的 /wxLogin/list 直接把请求体的 elderId 交给 selectBillList(Bill)，控制器内没有做 memberId 归属校验（对比同文件 :503-511 的 healthData 有明确的归属校验）。
- 现象: 前端把导航参数当作可信输入直接上送；只要改一个 id，后端就会按该 elderId 查询账单。前端这一侧的问题在于：从 family.js:33-36 跳转时未夹带任何身份校验信息，也没有对"退款/支付"动作做二次归属确认。
- 修复建议: 前端在进入该页前校验 elderId 是否在当前家属的 getElderBedList 结果中（失败即 toast + navigateBack）；同时推动后端在 /wxLogin/list 内按登录态收敛 elderId 范围。

## M21. 房间/服务图片地址在多个页面未做空值与编码保护（散点）

- 文件: lcyl-wx/home/pages/roomDetail/roomDetail.js:28-31、lcyl-wx/pages/servicePage/servicePage.js:168、lcyl-wx/home/pages/myService/myService.js:95
- 证据（roomDetail.js:28-31）:
    if (res.data.code === 200) {
      this.setData({ roomInfo: res.data.data })
      wx.setNavigationBarTitle({ title: res.data.data.name })     // data 为 null 时抛 TypeError
    }
- 证据（myService.js:95）: image: '/images/head.png'（无视后端返回的 item.projectImage，而 my/pages/myOrder/myOrder.js:75 用的是 item.projectImage）
- 现象: 详情页对 data 为 null 无保护（异常被 :32-33 的空 catch 吞掉，页面停留空白）；两个"我的服务/我的订单"页对同一字段取不同的兜底策略，列表封面不一致。
- 修复建议: 统一 const d = res.data && res.data.data; if (!d) { toast; navigateBack; return }；封面统一用 item.projectImage || '/images/head.png'。

## M22. 同一 Tab 内的"我的服务"(home) 与"我的订单"(my) 实现重复且已产生分歧

- 文件: lcyl-wx/home/pages/myService/myService.js:61-266 与 lcyl-wx/my/pages/myOrder/myOrder.js:39-245
- 证据: 两个文件的 mapOrderItem/getOrderStatusMeta/formatDateTime/formatMoney/filterOrders/cancelOrder/applyRefund/goPay 逐段几乎一致，仅订单封面兜底（:95 vs :75）和 Tab 切换实现有细微差别。
- 影响: 任何状态映射或退款规则的改动都要改两处，已经出现不一致；后续极易漏改。
- 修复建议: 抽出 utils/order.js（映射 + 格式化）与 components/order-list 组件，两页共用；短期至少把状态映射表合并为单一来源。

## M23. 全局错误处理为空，运行时异常无任何上报

- 文件: lcyl-wx/app.js:32-34
- 证据:
    onError: function (msg) {
    }
- 现象: 页面脚本错误被静默吞掉；结合项目里大量的空 catch（如 home/pages/roomDetail/roomDetail.js:32-33、home/pages/eldervisit/eldervisit.js:243 等），线上问题无法定位。
- 修复建议: 在 onError 中至少 console.error + 上报（wx.reportEvent/wx.getRealtimeLogManager），空 catch 改为记录错误或给出用户提示。

## M24. 登录流程无防重入，wx.login 的 code 被重复消费

- 文件: lcyl-wx/pages/index/index.js:9-64；lcyl-wx/pages/index/index.wxml:8
- 证据: getPhoneNumber 直接进入 wx.login → request('/wxLogin/phoneLogin')，没有 loading/disabled 状态。
- 现象: 用户连点"微信一键登录"会并发发起两次 wx.login + phoneLogin；微信 code 是一次性的，第二次通常返回失败，可能覆盖第一次成功的结果或弹出"登录失败"。
- 修复建议: 增加 loggingIn 标记 + 按钮 disabled，逻辑与 registerElder/submitLeave 的 submitting 写法保持一致。

## M25. 服务列表搜索：每次输入都 trim、且对未做空值/大小写保护的字段做 includes

- 文件: lcyl-wx/pages/servicePage/servicePage.js:38-51
- 证据:
    onSearchInput(e) { this.setData({ searchKey: e.detail.value.trim() }); this.doSearch() },
    const filtered = originalList.filter(i => i.itemName.includes(searchKey))
- 现象: 1) 每次输入都 trim，用户无法在关键词中间输入空格；2) i.itemName 为 null/undefined 时 includes 抛错，整页白屏；3) 搜索区分大小写且不做空数组兜底（originalList 为空时 filter 结果为空，无提示）。
- 修复建议: 保存原始输入、比较时再 trim/转小写；filter 内写 (i.itemName || '').toLowerCase().includes(key)；对空结果给出"未找到相关服务"。

---

# 🔵 低

## L1. app.json 的 tabBar 背景色缺少井号，配置值非法

- 文件: lcyl-wx/app.json:19
- 证据: "backgroundColor": "F7F6FB"
- 现象: WeChat 配色字段要求 #RRGGBB 格式，缺 # 会被判为非法值（该色不生效，开发者工具会告警）。
- 修复建议: 改为 "#F7F6FB"。

## L2. 服务页日期选择器的结束日期绑定了一个不存在的变量

- 文件: lcyl-wx/pages/servicePage/servicePage.wxml:54（变量在 lcyl-wx/pages/servicePage/servicePage.js:4-18 的 data 中不存在）
- 证据: <picker mode="date" value="{{serviceDate}}" start="{{nowDate}}" end="{{endDate}}" ...>
- 现象: endDate 未定义 → end 为空，用户可以选择任意远的日期（serviceDetail.js:24 有 endDate: "2030-12-31"，本页漏了）。
- 修复建议: 在 servicePage.js 的 data 中补 endDate: '2030-12-31'（或与 serviceDetail 共用同一常量）。

## L3. 空状态图片路径不存在

- 文件: lcyl-wx/family/pages/family.wxml:27
- 证据: <image class="empty-icon" src="/images/icon-emotion.png"></image>
- 验证: 全项目图片共 61 个（png/jpg），无 icon-emotion.png。
- 现象: 未绑定家人时显示破损图片。
- 修复建议: 改为已存在的空状态图（如 /images/zwnr2x.png 或 /images/wuwang.png），或在 images 下补该资源。

## L4. home 分包里的 myOrder 是空白占位页，仍被打包

- 文件: lcyl-wx/home/pages/myOrder/myOrder.js:1-66、lcyl-wx/home/pages/myOrder/myOrder.wxml:1-2；声明于 lcyl-wx/app.json:63
- 证据（wxml 全文）: <!--home/pages/myOrder/myOrder.wxml--><text>home/pages/myOrder/myOrder.wxml</text>
- 现象: 整页只有占位文本，所有生命周期为空；真正使用的是 my/pages/myOrder。属于死页面，浪费分包体积并容易误导后续维护者。
- 修复建议: 确认无入口引用后删除该页面并从 app.json 的 home 分包中移除。

## L5. 探访预约与参观预约两份页面代码近乎整体复制

- 文件: lcyl-wx/home/pages/eldervisit/eldervisit.js 与 lcyl-wx/home/pages/customVisit/customVisit.js（后者 215 行，前者 266 行，checkTimeDisable/formatDate/selectTime/switchPeriod/onDateChange 基本逐行相同）
- 现象: 复制导致行为分歧——eldervisit 的提交校验被删（H7）、eldervisit 多出 getElder 接口与家人姓名输入（:227-245）。
- 修复建议: 抽成公共 mixin/behavior（如 utils/visitForm.js），两页只保留差异字段。

## L6. 养老院联系电话是占位号码，且可直接拨出

- 文件: lcyl-wx/home/pages/aboutInfo/aboutInfo.js:7、:86-98
- 证据: phoneNumber: '0371-000-0000'
- 现象: 用户点击"联系客服"会拨打这个无效号码。
- 修复建议: 换成真实客服电话，或从后端配置/客服消息（open-type="contact"）获取。

## L7. 合同正文里硬编码机构名称与地址

- 文件: lcyl-wx/my/pages/contractDetails/contractDetails.wxml:7、:11
- 证据: <text class="highlight"> 中州养老机构 </text> 与 <text class="highlight"> 北京市昌平区西三旗街道135号 </text>
- 现象: 具有法律文本外观的页面写死了机构名称与地址，与项目实际机构（乐康/绿城养老）不一致，且机构信息变更需改代码。
- 修复建议: 由后端返回或抽到统一配置中渲染，避免在法律文本里硬编码。

## L8. home.wxml 里残留一段疑似密钥/ID 的注释

- 文件: lcyl-wx/pages/home/home.wxml:18
- 证据: `<!-- 4e39a465...（32 位十六进制，此处已脱敏） -->`
- 现象: 32 位十六进制串被注释遗留在首页模板中，形似 appId/密钥/会话 ID，无法判断是否为真实凭据。
- 修复建议: 确认来源后删除；若确为凭据，按密钥泄漏流程处理（作废并轮换）。

## L9. 房型详情页存在空 catchtap 与空 catch

- 文件: lcyl-wx/home/pages/roomDetail/roomDetail.wxml:27（catchtap=""）、lcyl-wx/home/pages/roomDetail/roomDetail.js:32-33（.catch(err => {})）
- 现象: 空处理器与空 catch 掩盖错误；:30 直接在 res.data.data.name 上取属性（见 M21）。
- 修复建议: 删除空的 catchtap（改用 catchtap="noop" 并定义 noop），catch 内补日志与用户提示。

## L10. 退出登录无确认、未清理本地用户态

- 文件: lcyl-wx/pages/my/my.js:22-28
- 证据:
    logout() { app.globalData.token = ''; wx.removeStorageSync('token'); wx.reLaunch({ url: "/pages/index/index" }) }
- 现象: 没有二次确认（"我的"页 wxml:112 是一整行可点区域，容易误触），也没有清理 userInfo/memberId（见 M18），未通知后端使会话失效（后端 refreshToken 会延长 Redis 会话，登出后旧 token 在服务端仍有效一段时间）。
- 修复建议: 加 wx.showModal 确认；清理全部本地登录态；如有登出接口则调用后再跳转。

## L11. utils/request.js 在模块顶层获取 app 实例，且缺失时静默指向 localhost

- 文件: lcyl-wx/utils/request.js:1、:3-5
- 证据: const app = getApp(); 与 return (app && app.globalData && app.globalData.baseUrl) || 'http://localhost:8080';
- 现象: getApp() 在模块加载时执行，若该模块早于 App() 执行被 require，app 为 undefined；此时所有请求会静默打到 localhost 而不是报错，问题很难定位（与 S1 同一根因）。
- 修复建议: 改为在函数内部调用 getApp()（惰性获取），并删除 localhost 兜底，改为抛出明确错误。

## L12. app.js 生命周期为空，未做启动期初始化的必要处理

- 文件: lcyl-wx/app.js:18-27、:32-34
- 现象: onLaunch 只同步了 token，没有版本更新检查、没有全局错误上报（见 M23）、onShow/onHide 为空实现。
- 修复建议: 按需补充 wx.getUpdateManager 检查更新与日志上报；空生命周期可直接删除以减少噪音。

## L13. 我的服务页封面兜底与其他页面不一致

- 文件: lcyl-wx/home/pages/myService/myService.js:95
- 证据: image: '/images/head.png'（硬编码，忽略 item.projectImage）
- 对照: lcyl-wx/my/pages/myOrder/myOrder.js:75 使用 image: item.projectImage || '/images/head.png'。
- 现象: 同一个后端字段在"我的服务"里永远显示默认头像，用户看到的是与项目无关的老人头像图。
- 修复建议: 与 myOrder 保持一致，改回 item.projectImage || '/images/head.png'。

## L14. 开发者工具关闭了域名校验，容易掩盖环境问题

- 文件: lcyl-wx/project.private.config.json:6
- 证据: "urlCheck": false
- 现象: 该开关让工具忽略 request 合法域名校验，配合硬编码的 http://localhost:8080（S1），本地一切正常而真机/体验版直接失败。
- 修复建议: 本地调试可保留，但需在提测前用体验版验证真实域名与 HTTPS，避免把 urlCheck:false 当作可依赖的行为。

---

# 附：已核查、确认无问题的点（避免误报）

1. Authorization 头格式与后端匹配：前端发送裸 token（utils/request.js:108，无 Bearer 前缀），后端 UserInterceptor.java:27 用 request.getHeader("authorization") 读取同一裸值，HTTP 头名大小写不敏感 —— 不是缺陷。
2. orderStatus / tradeStatus 的字符串比较：WxMyOrderVO.orderStatus 与 WxMyBillVO.tradeStatus 均为 String（lcyl-java/.../vo/WxMyOrderVO.java:17-18），因此 my/pages/myOrder/myOrder.js:89-106、my/pages/myOrderDetail/myOrderDetail.js:194-208 用 === '0' 比较是正确的；myBill.js:80 额外做 String() 归一化也正确。
3. roomDetail.wxml:13/22 的 roomInfo.status === 1：后端 LcRoomType.status 是原生 int（lcyl-java/.../LcRoomType.java:48），数字严格比较正确。
4. myBooking.wxml:22 的 item.status === '0'：后端 RoomBooking.status 为 String，比较正确（与 S4 的 mybill 不同，那里混用了数字）。
5. mybill.wxml:20/26 的状态与账单类型三元表达式用的是宽松 ==，配合 String 状态的 "0"/"1"/"2" 可正常工作 —— 风格不统一但不是缺陷。
6. refreshSession 不消费响应体：后端 /wxLogin/refreshToken（WxLoginController.java:133-141）只延长 Redis 会话 TTL 并返回 "ok"，不下发新 token，因此前端忽略返回值没有功能缺陷（仅 H2 的重复调用是问题）。

# 建议的修复优先级

1. 立即（发版前必须）：S1 基址/协议、S2 aiChat 绕过封装、S3 上传无 token、S4 账单支付按钮。
2. 本次迭代：H1 网络失败登出、H2/H3 重复请求、H4 loading 卡死、H5 N+1、H6 编解码、H7 空预约、H8 重复下单、H9 聊天滚动。
3. 下个迭代：中优先级全部（重点是 M1-M6 的业务正确性与 M9-M12 的支付/下单健壮性）。
4. 清理：低优先级与"附"中列出的死代码、重复实现、硬编码文案。
