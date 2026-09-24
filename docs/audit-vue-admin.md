# 智慧养老平台（lcyl）Vue3 管理后台前端代码审计报告

- 审计对象：D:\WBB_JAVA\team\lcyl-vue\lcyl\（源码 src/、.env.*、vite.config.ts、tsconfig.json、package.json）
- 技术栈：Vue 3.5 + TypeScript(strict) + Element Plus 2.13 + Pinia 3 + Vite 6 + vue-router 4
- 审计方式：全量静态阅读 + 跨文件交叉验证；对关键结论做了可复现验证（用项目内 @vue/compiler-dom 实测模板编译、用 node 实测语法解析）
- 边界：未审计 node_modules/；本次为只读审计，未修改被测项目任何文件

## 结论概览

| 级别 | 数量 | 主要问题类型 |
| --- | --- | --- |
| 🔴 严重 | 7 | 两处语法错误导致构建失败、口令密文落 Cookie、路由权限校验可绕过、敏感页面无权限暴露、医疗监护页假数据、财务金额写死 |
| 🟠 高 | 33 | 响应体双重解包致功能必失败、Vue2 残留语法、事件/实例泄漏、loading 与遮罩永久卡死（系统性）、只读/权限形同虚设、字段错位致业务数据错误 |
| 🟡 中 | 33 | computed 内改状态、props 被改写、千分位/时区/枚举不一致、N+1 请求、空表单与死规则、金额与状态判断脆弱 |
| 🔵 低 | 14 | 死代码与空文件、第三方内部路径导入、未使用工具函数、外链缺 noopener、iframe 内嵌后端控制台 |

重点结论：本项目的风险不是单点漏洞，而是**「演示/生成态代码直接进入生产」+「全项目统一的错误处理缺失」**。后者表现最明显：全库按行统计，loading/遮罩复位几乎都写在 .then 里而没有 .catch/.finally，接口异常即整页卡死或永久遮罩，涉及 30+ 个页面。

---

## 🔴 严重问题

### 1. src/views/checkin/approve.vue:268-274 —— 残留代码块造成语法错误，该文件无法解析
- 问题描述：try 块内 submitLoading.value = true 之后，残留了一段没有函数调用的孤立对象字面量片段，第 274 行的 }) 没有与之配对的左括号，整个 SFC 的 script 无法解析。该组件被 src/views/checkin/index.vue 引用，属构建期/路由加载期致命错误。已用 node 对同类片段做解析验证：报 Unexpected token（12 个 checkin SFC 中仅此文件解析失败）。
- 根本原因：删除了旧审批接口调用（如 approveCheckin( 开头那行），只删掉函数名与括号，留下了中间参数行与闭括号。
- 修复建议：删除第 270-274 行残留片段（第 277 行的 approveCheckin(...) 才是真正的接口调用），并给构建流程加 syntax/type 检查门禁。
- 代码证据：

        submitLoading.value = true

          checkInId: currentCheckInId.value,
          approveResult: approveForm.approveResult,
          approveRemark: approveForm.approveRemark
        })

### 2. src/views/system/role/index.vue:463 —— 返回类型注解被误写进函数体，语法错误，构建失败
- 问题描述：function getDeptTree(roleId: number) { Promise<RoleDeptTreeResult> —— 冒号漏写，类型注解落到了函数体第一行。它是 a > return 形式，属于非法语法（已实测：PARSE FAIL: Unexpected token 'return'）。该文件由 import.meta.glob 纳入构建图，会导致打包失败或该路由模块加载失败。
- 根本原因：TypeScript 迁移时把 : Promise<X> 的冒号写丢并放到了 { 之后。
- 修复建议：改为 function getDeptTree(roleId: number): Promise<RoleDeptTreeResult> {。
- 代码证据：

    function getDeptTree(roleId: number) { Promise<RoleDeptTreeResult>
      return deptTreeSelect(roleId).then(response => {

### 3. src/views/login.vue:120-129 —— 「记住密码」把用户口令写入前端 Cookie（长期有效、JS 可读）
- 问题描述：勾选记住密码后，用户名、RSA 加密后的口令、rememberMe 全部写入 Cookie，有效期 30 天，secure 硬编码为 true。Cookie 无 httpOnly，任何 XSS、浏览器插件、本地取证脚本均可读取；该密文对后端等价于口令，拿到即可复用。另外 secure:true 在 http 部署下会被浏览器直接丢弃，导致功能静默失效。
- 根本原因：把「凭据材料」而非「登录态」持久化到前端可读存储；secure 未按 location.protocol 判定（对比 src/utils/auth.ts:10 的正确写法 const isSecure = location.protocol === 'https:'）。
- 修复建议：禁止前端持久化任何形式的密码（含密文）；「记住我」改为后端下发 httpOnly + Secure + SameSite 的长期会话/刷新令牌；如仅需记住账号，只保留用户名，且 secure 按协议动态判断。
- 代码证据：

    Cookies.set("password", encrypt(loginForm.value.password), { expires: 30, secure: true, sameSite: 'Strict' })

### 4. src/permission.ts:46-62 —— 路由权限校验在首次进入/硬刷新时被绕过
- 问题描述：首次加载或刷新动态路由页面时，to 是在 addRoute 之前解析的，其 matched 指向常量路由里的全捕获 404 路由（无 permissions），因此 hasRoutePermission(to) 恒返回 true；直到 next({ ...to, replace: true }) 触发第二次导航，路由才真正匹配。用户只要直接访问深层 URL 或刷新页面，即可跳过页面级 permissions 校验拿到组件（配合第 5 条的 permissions: [] 页面，等于完全开放）。
- 根本原因：在路由表变更后仍用变更前解析出的 to 做权限判定，权限判定时机与路由解析时机不一致；hasRoutePermission 只读 permissions，从不读 roles。
- 修复建议：addRoute 之后用 router.resolve(to.fullPath) 重新解析再校验，或把校验统一放到第二次守卫执行分支；对 to.matched.length === 0 / 落 404 的情况显式拒绝。
- 代码证据：

        accessRoutes.forEach((route: any) => { if (!isHttp(route.path)) router.addRoute(route) })
        if (!hasRoutePermission(to)) {   // to 仍是 addRoute 之前的旧匹配（404 通配）

### 5. src/router/index.ts:78 / 110 / 124 / 139 / 166 / 228 —— 敏感业务页面以 permissions: [] 注册在常量路由，任何登录用户可直达
- 问题描述：/checkin/*、/system/checkin/list（入住任务中心）、/system/my-apply、/system/my-to（我的待办）、/code/checkout-apply/* 全部写入 constantRoutes（启动即注册，不经过后端菜单授权），并注释「明确声明不需要权限检查」。其中 finishment（费用清算完成）、checkoutApply（发起退住）permissions 为空。配合 src/permission.ts:23-24 的 if (perms && perms.length > 0)，空数组等于跳过校验。
- 根本原因：用「常量路由 + 空权限数组」承载业务流程页，权限模型与业务敏感度脱节。
- 修复建议：流程页统一由后端 getRouters 动态下发并携带真实权限点；确需常量注册的，至少在守卫中按角色白名单硬校验。
- 代码证据：

      permissions: [],  // 明确声明不需要权限检查
      ...
      { path:'finishment', name: 'Finishment', permissions: [], meta: { title: '清算成功' } },

### 6. src/views/system/SmartBed/index.vue:88-101（及 14）—— 智能床位医疗监护页整页是硬编码假数据
- 问题描述：楼层、房间、老人姓名、心率、呼吸率、离床次数、报警状态全部写死在页面（注释直言「2/3楼为模拟数据」），无任何 API 调用、无 loading、无错误处理、无权限指令；报警红点写死为 v-if="item === '1楼'"，与真实报警数据无关：1 楼永远显示报警、2/3 楼真报警也不显示。护理人员看到的生命体征与报警是伪造的。
- 根本原因：Demo/原型静态数据被当作业务页面提交，未接入数据模型与接口。
- 修复建议：删除 floorData 硬编码，接入真实楼层/房间/床位/体征接口（含 loading、错误兜底、轮询刷新），报警状态由真实字段驱动，并补 v-hasPermi。
- 代码证据：

    // 楼层数据（1楼为你现有数据，2/3楼为模拟数据）
    const floorData = ref({
      '1楼': [ { roomCode: '101', doorStatus: '开启', temp: '26', ... }

### 7. src/views/code/checkout/billApproval.vue:337-338（checkoutApproval.vue:354-355、billAdjustment.vue:434 同）—— 服务天数写死 12，欠费与退款金额由假常量算出
- 问题描述：退住结算页把「本月天数 30 / 服务天数 12」硬编码，totalArrears、finalRefundAmount 等金额全部基于这两个常量计算，财务审批人看到的是与真实账期无关的金额。
- 根本原因：页面从账单页复制而来，未接入真实账单周期与退住日期。
- 修复建议：由后端返回的账单周期与退住日期计算，或直接展示后端下发的金额，前端不重算金额。
- 代码证据：

      refundForm.actualDays = 30
      refundForm.refundDays = 12

---

## 🟠 高优先级问题

### 8. src/views/tool/build/RightPanel.vue:76 与 :80 —— 模板中调用 Vue 3 已删除的 $set，交互即抛异常
- 问题描述：el-checkbox-group 的「至少应选/最多可选」输入框绑定 @input="$set(activeData, 'min', ...)"。Vue 3 无 $set，运行时 _ctx.$set 为 undefined，用户一输入即 TypeError: $set is not a function，属性面板后续操作中断。
- 根本原因：从 Vue2 版 RuoYi 表单生成器迁移时未替换 $set（Vue3 的 reactive/ref 本身可追踪增删属性）。
- 修复建议：改为 @input="activeData.min = $event || undefined"，或抽具名方法 setField('min', $event)。
- 代码证据：

            <el-input-number :value="activeData.min" :min="0" placeholder="至少应选"
              @input="$set(activeData, 'min', $event ? $event : undefined)" />

### 9. src/views/tool/build/RightPanel.vue:432-434、439-441 —— el-radio-button 的 label 与 value 混写，配置被写入非法枚举
- 问题描述：<el-radio-button label="large" value="较大" /> 同时给了两个属性。Element Plus 2.13 中 value 才是绑定值、label 是显示文本，于是 formConf.size 被写入「较大/默认/较小」，formConf.labelPosition 被写入「左对齐/右对齐/顶部对齐」，而 build/index.vue 的 :size/:label-position 只接受 large/default/small 与 left/right/top —— 设置静默失效，按钮上显示的却是 large/left 等英文键名。
- 根本原因：旧 API（label 即值）与新 API（value 即值）混写，语义互斥必有一半失效。
- 修复建议：统一为 <el-radio-button value="large">较大</el-radio-button>；src/views/index.vue:84-86 的 label="today" 同类问题一并改为 value。
- 代码证据：

              <el-radio-button label="large" value="较大" />
              <el-radio-button label="default" value="默认" />
              <el-radio-button label="small" value="较小" />

### 10. src/components/Editor/index.vue:194-195 —— 响应体被二次解包，富文本粘贴图片 100% 失败
- 问题描述：src/utils/request.ts:106-107 的响应拦截器已 return Promise.resolve(res.data)，即 request.post() 的返回值就是业务体 { code, fileName }。此处又取 res.data，得到 undefined，传入 handleUploadSuccess 后第一行 res.code 立即抛 TypeError，被 catch 吞掉并提示「图片上传失败」。工具栏按钮走 el-upload，所以问题被掩盖。
- 根本原因：同一份响应存在两套解包约定（拦截器已解包 vs 调用点再取 .data），调用点的 TS 类型标注 { data: UploadFileResult } 与真实返回值不符，编译期无法发现。
- 修复建议：改为 handleUploadSuccess(res as unknown as UploadFileResult, file)，并把 request 返回类型声明为业务体（axios 实例加泛型），杜绝同类错误。
- 代码证据：

    request.post("/common/upload", formData, { headers: { "Content-Type": "multipart/form-data" } }).then((res: { data: UploadFileResult }) => {
      handleUploadSuccess(res.data as UploadFileResult, file)

### 11. src/plugins/download.ts:16-21、34-39、52-57 —— 通用下载插件三处双重解包，$download.zip 必失败
- 问题描述：blobValidate(res.data) 传入 undefined，而 blobValidate（src/utils/ruoyi.ts:230-232）实现是 return data.type !== 'application/json'，访问 undefined.type 直接抛 TypeError；name()/resource() 还访问了根本不存在的 res.headers。调用点 src/views/tool/gen/index.vue:228 的代码生成 ZIP 下载在当前代码下必定失败。
- 根本原因：沿用旧版若依（拦截器未解包时代）的写法，迁移后未同步修改。
- 修复建议：blobValidate(res) / new Blob([res])；移除 res.headers 依赖；blobValidate 增加 if (!(data instanceof Blob)) return false 防御。
- 代码证据：

        const isBlob = blobValidate(res.data)   // res 已是 Blob，res.data 为 undefined

### 12. src/components/DictTag/index.vue:22 —— Vue3 已移除的过滤器语法，字典回退值恒显示 0
- 问题描述：{{ unmatchArray | handleArray }} 是 Vue2 过滤器写法。Vue3 把 | 当作按位或运算符（已实测编译产物为 _toDisplayString(_ctx.unmatchArray | _ctx.handleArray)，无编译报错），[] | 函数 求值为 0，未匹配字典值时页面显示字符 0。此外 unmatch 是 computed 却在内部写 unmatchArray.value = [] 并 push，属渲染期改状态。
- 根本原因：从若依 Vue2 版本迁移未把过滤器改为方法调用；副作用被塞进 computed。
- 修复建议：模板改为 {{ handleArray(unmatchArray) }}；unmatch 改为纯计算，赋值移到 watch 中。
- 代码证据：

    <template v-if="unmatch && showValue">
      {{ unmatchArray | handleArray }}

### 13. src/views/index.vue:625-630、733-736 与 src/views/monitor/cache/index.vue:124-127 —— 匿名 resize 监听无法移除，卸载后仍调用已 dispose 的 ECharts
- 问题描述：三处都用匿名箭头函数注册 window.addEventListener('resize', ...)，无处保存引用、无 removeEventListener；而 onUnmounted（index.vue:739-742）只 dispose 图表。组件卸载后监听器仍存活，窗口一缩放就调用已 dispose 的实例 resize()，抛异常并泄漏（闭包持有整个组件作用域）；index.vue 的 c1/c2/c3 三个实例甚至没有纳入 myCharts 统一 dispose。
- 根本原因：注册与注销不对称（匿名函数无法反注册），生命周期清理只覆盖图表资源、未覆盖 DOM 事件。
- 修复建议：抽出命名函数 onResize，onMounted 注册、onBeforeUnmount/onUnmounted 移除；或统一用 @vueuse/core 的 useEventListener（自动清理）。
- 代码证据：

      window.addEventListener('resize', () => {
        mainChart?.resize(); c1?.resize(); c2?.resize(); c3?.resize()
      })
      ...
      onUnmounted(() => { myCharts.forEach(c => c.dispose()); mainChart?.dispose() })

### 14. src/store/modules/permission.ts:35-54 —— generateRoutes 无 reject 分支，接口失败让路由守卫永久挂起
- 问题描述：返回的 Promise 只有 resolve，内部 getRouters() 也没有 .catch。菜单接口 4xx/5xx/超时后 Promise 既不 resolve 也不 reject，src/permission.ts:50 的 next() 永不执行：NProgress 卡住、页面白屏，且因 isRelogin.show = true（request.ts:85）401 弹窗被抑制，用户只能手动改地址。
- 根本原因：手工包装 Promise 漏掉失败路径，与「等待型」守卫组合形成无超时挂起。
- 修复建议：getRouters().then(...).catch(err => reject(err))，守卫端统一 next('/401') 或登出跳登录，并加失败提示。
- 代码证据：

        generateRoutes(roles?: any[]): Promise<any[]> {
          return new Promise(resolve => {
            getRouters().then(res => { ...; resolve(rewriteRoutes) })

### 15. src/utils/request.ts:96、105 —— 拦截器用字符串 reject，丢失错误对象与堆栈
- 问题描述：Promise.reject('无效的会话，或者会话已过期，请重新登录。') 与 Promise.reject('error') 抛出的是字符串而非 Error，上层 catch(err) 拿不到 message 与堆栈，提示为空、难以定位。
- 根本原因：沿用若依旧版写法。
- 修复建议：统一 Promise.reject(new Error(msg)) 并携带 code（自定义 ApiError）。
- 代码证据：

          return Promise.reject('无效的会话，或者会话已过期，请重新登录。')

### 16. src/views/register.vue:137-142 —— 漏写 .value，验证码开关判断恒为真
- 问题描述：captchaEnabled 是 ref，此处 if (captchaEnabled) 判断的是 ref 对象本身，永远真值；注册失败时无论后端是否开启验证码都会重新请求验证码图片。同文件 149 行与 login.vue:143 都是正确写法 captchaEnabled.value。
- 根本原因：script 区块中 ref 不自动解包，编写时漏写，ref 恒真使错误被静默。
- 修复建议：改为 if (captchaEnabled.value)。
- 代码证据：

        }).catch(() => {
          loading.value = false
          if (captchaEnabled) { getCode() }

### 17. src/utils/jsencrypt.ts:13-17 + src/views/login.vue:161-170 —— decrypt 恒返回 false，密码回填被写成布尔值
- 问题描述：decrypt 被改造为打印告警并 return false，但登录页仍按 string 契约使用：password: password === undefined ? loginForm.value.password : decrypt(password)。只要浏览器残留历史 password Cookie，密码字段就被赋成布尔 false（TS 上 string 字段收到 boolean），输入框为空、required 校验失败；encrypt 的返回类型 string | false 也被直接 Cookies.set。
- 根本原因：禁用了能力但保留调用点，接口契约与实际返回不一致且无类型收窄。
- 修复建议：删除 decrypt 及其调用，getCookie 只回填用户名；encrypt 结果判空后再写 Cookie。
- 代码证据：

    export function decrypt(txt: string): string | false {
      console.warn('前端不应持有私钥，解密功能已禁用')
      return false

### 18. src/store/modules/settings.ts:10 —— localStorage 脏数据导致应用启动白屏
- 问题描述：模块顶层直接 JSON.parse(localStorage.getItem('layout-setting') || '{}')，无 try/catch。该键只要被写成非 JSON（旧版本格式、手工改值、存储截断），模块加载即抛 SyntaxError，main.ts 引入 store 失败，整个后台无法启动。另外 storageSetting.navType === undefined ? navType : ... 对 null 值不生效，会把 null 当配置使用。
- 根本原因：把不受信任的持久化数据当配置源直接反序列化，失败路径未兜底。
- 修复建议：try/catch 降级为 {}（并做字段白名单/类型校验），或改用 useStorage 等自带容错的封装。
- 代码证据：

    const storageSetting = JSON.parse(localStorage.getItem('layout-setting') || '{}') || {}

### 19. src/utils/index.ts:232-236 —— deepClone 判空条件写反，传 null 抛 TypeError
- 问题描述：if (!source && typeof source !== 'object') throw 用的是 &&，正确语义应为「值为空 或 不是对象」。null 满足 !source 但 typeof null === 'object'，于是跳过守卫，随后读 null.constructor 抛 TypeError；0/'' /false 又会被误判为参数错误直接抛异常。
- 根本原因：布尔运算符误用 + typeof null 经典陷阱无防御。
- 修复建议：if (source === null || typeof source !== 'object') { ... }，并对数组/Date 分支处理。
- 代码证据：

    export function deepClone<T>(source: T): T {
      if (!source && typeof source !== 'object') { throw new Error('error arguments') }

### 20. 全项目系统性缺陷：loading / 全局遮罩复位写在 .then 内，无 .catch/.finally（30+ 处）
- 问题描述：列表与详情请求统一写成 loading.value = true 前置、loading.value = false 只在 .then 内，接口异常时 v-loading 遮罩或全局 ElLoading 永久覆盖，页面不可用且无错误反馈。已逐文件核对到具体行：
  - system 模块（22 处）：src/views/system/user/index.vue:308、role/index.vue:305、menu/index.vue:327、dict/index.vue:220、dict/data.vue:250、dept/index.vue:179、config/index.vue:205、post/index.vue:184、item/index.vue:242、level/index.vue:213、elder/index.vue:219、bed/index.vue:133、device/index.vue:154、notice/index.vue:197、type/index.vue:218、contract/index.vue:389、leave/index.vue:503、refund/index.vue:377、record/index.vue:281、role/authUser.vue:120、user/authRole.vue:114、system/checkin/index.vue:315
  - code 模块（6 处）：src/views/code/item/index.vue:137、payment/index.vue:223、plan/index.vue:385、reservation/index.vue:330、arrival/index.vue:162、checkout/index.vue:195
  - monitor/tool（8 处）：monitor/operlog/index.vue:235、logininfor/index.vue:157、job/index.vue:328、job/log.vue:206、online/index.vue:78、cache/list.vue:183 与 206、tool/gen/index.vue:200
  - 全局遮罩不关闭：src/views/monitor/cache/index.vue:76-79、src/views/monitor/server/index.vue:178-183（proxy.$modal.closeLoading() 在 .then 内且无 catch）
- 根本原因：全项目未统一封装请求/表格 loading 生命周期，复位与成功回调强耦合。
- 修复建议：统一改 .finally(() => loading.value = false) 或抽 useTableList/useFetch 组合式函数；全局遮罩用 try{...}finally{ closeLoading() }。
- 代码证据：

          loading.value = true
          listUser(proxy.addDateRange(queryParams.value, dateRange.value)).then(res => {
            loading.value = false   // 失败分支缺失
            userList.value = res.rows

### 21. src/views/code/arrival/index.vue:195 —— 使用未导入的 ElMessage，失败分支直接 ReferenceError
- 问题描述：该文件使用了 ElMessage，但既未 import 也未自动导入。已核实 vite/plugins/auto-import.ts:5-9 只配置 vue/vue-router/pinia，根 auto-imports.d.ts 中无任何 element-plus 声明。走到该分支即抛 ElMessage is not defined，错误提示被新的异常取代。
- 根本原因：依赖了并不存在的全局自动导入；仅在接口失败时触达，漏测。
- 修复建议：显式 import { ElMessage } from 'element-plus'，或把 element-plus 加入 unplugin-auto-import。同类风险应全局排查未导入即使用的 API。
- 代码证据：

        ElMessage.error('筛选访问类别失败，请重试');

### 22. src/views/code/checkout/applyApprovalAdmin.vue:329-330 —— 养老顾问与护理员字段互换
- 问题描述：data.counselor 被赋值为 resp.data.nursingName，data.nurse 被赋值为 resp.data.counselor，两项内容互换。同文件对比 src/views/code/checkout/billAdjustment.vue:472-473 的正确写法（counselor ← res.data.counselor、nurse ← res.data.nursingName）可见明显错位，审批页显示的责任人与实际不符。
- 根本原因：复制粘贴时字段名未同步。
- 修复建议：data.counselor = resp.data.counselor；data.nurse = resp.data.nursingName。
- 代码证据：

          data.counselor = resp.data.nursingName;
          data.nurse = resp.data.counselor;

### 23. src/views/code/checkout/finalSettlement.vue:254 —— res.counselor 少写 .data，顾问字段恒为空
- 问题描述：同一段赋值中护理员用 res.data.nursingName，顾问却用 res.counselor（没有 .data），永远取不到值，页面显示空白。整个文件为 JS 写法，TS 未拦住。
- 根本原因：属性路径笔误。
- 修复建议：改为 res.data.counselor。
- 代码证据：

      data.counselor = res.counselor || ''
      data.nurse = res.data.nursingName || ''

### 24. src/views/code/checkout/contractTerm.vue:202 —— 预览合同读取的是从未赋值的 data.contractUrl，「查看合同」必然提示无文件
- 问题描述：合同地址实际保存在 form.businessData.contractUrl（第 117 行声明、231 行提交），而 previewContract 读的是 data.contractUrl —— data 对象（125-129 行）根本没有该字段，值恒为 undefined，点击「查看合同」永远走 ElMessage.warning('无合同文件')。
- 根本原因：状态对象名混用（data 与 form.businessData）。
- 修复建议：改为 form.businessData.contractUrl，并统一合同信息的状态载体。
- 代码证据：

    const previewContract = () => {
      data.contractUrl ? window.open(data.contractUrl, '_blank') : ElMessage.warning('无合同文件')

### 25. src/views/code/checkout/index.vue:4-19 —— 「单据编号」与「姓名」绑定同一字段，编号搜索失效
- 问题描述：两个查询输入框都 v-model="queryParams.name" 且 prop="name"，互相覆盖，按单据编号检索实际按姓名检索；queryParams 中本来就有 retreatCode 字段。
- 根本原因：复制表单项时未改绑定。
- 修复建议：单据编号绑定 queryParams.retreatCode，prop 同步修改。
- 代码证据：

      <el-form-item label="姓名" prop="name">
        <el-input v-model="queryParams.name"

### 26. src/views/code/checkout/applyApproval.vue:38-47 —— 整页硬编码假审批记录，无任何接口调用
- 问题描述：操作记录中的「顾廷烨/盛明兰/2048-10-15 09:00:00」全部写死在模板中，页面不读路由参数、不请求后端，上线即展示伪造的审批流水。
- 根本原因：原型/演示页被当作正式页面提交。
- 修复建议：接入真实流程历史接口，或在路由与菜单中下线该页面。
- 代码证据：

    <p class="log-desc">发起申请-申请退住<br><span class="log-operator">顾廷烨（已发起）</span></p>
    <span class="log-time">2048-10-15 09:00:00</span>

### 27. 上传请求缺鉴权头（3 处）—— el-upload 绕过 axios 拦截器且未带上 Authorization
- 文件：src/views/code/nursingItem/index.vue:136-142、src/views/code/nursingTask/index.vue:163-169、src/views/system/type/index.vue:92-96
- 问题描述：这些 el-upload 只配了 :action="VITE_APP_BASE_API + '/upload'"，没有 :headers（nursingItem 全文件 grep 无 headers），el-upload 自行发 XHR 绕过 axios 拦截器，后端校验 token 时必然 401；对比同项目 bill/index.vue:189、recharge/index.vue:131、user/index.vue:259 都显式注入了 Authorization。
- 根本原因：漏抄项目既有写法，上传请求未纳入统一鉴权通道。
- 修复建议：统一封装上传（action + headers 由 request 实例提供）或改用 :http-request 走 request；服务端同时做类型/大小/来源校验。
- 代码证据：

        <el-upload class="avatar-uploader" :action="uploadUrl" :show-file-list="false"
          :on-success="handleAvatarSuccess" :before-upload="beforeAvatarUpload">

### 28. src/views/system/checkin/index.vue:315（同类：src/views/checkin/index.vue 相关流程页）—— 审批/配置/签约动作页无权限指令，且只读协议不统一导致只读页仍可提交
- 问题描述：（a）/checkin、/system/checkin 两条路由显式 permissions: []，approve/config/sign/evaluate 组件内没有任何 v-hasPermi，任何登录用户拿到 businessId/taskId 即可打开审批、配置、签约页并发起请求；（b）src/views/system/checkin/list.vue:246、255 传的是 readonly: true，而 src/views/checkin/index.vue:141 只判断 route.query.mode === 'view'，从不读 readonly（全项目仅 evaluate.vue:394 自己读了），因此「查看」入口下 approve/config/sign 仍显示并可点击提交；approve.vue:174 声明了 viewMode 却全程未使用。审批与签约不可逆。
- 根本原因：动作页与查询页混用同一无权限路由；只读协议有三套（mode / readonly / prop）且多数子组件未消费。
- 修复建议：统一只读判定（index.vue 合并 mode==='view' || readonly==='true' 后下发 viewMode），动作页隐藏提交按钮并补 v-hasPermi，后端必须二次鉴权。
- 代码证据：

    src/views/system/checkin/list.vue:246   readonly: true
    src/views/checkin/index.vue:141         if (route.query.mode === 'view') {
    src/views/system/checkin/index.vue:315  listCheckin({ ...queryParams.value, flowStatus: 5, status: 2 }).then(response => {

### 29. src/views/checkin/evaluate.vue:31 —— el-form 缺少 :model，健康评估必填校验空转
- 问题描述：healthFormRef 只绑了 :rules="healthRules"，没有 :model="healthForm"。Element Plus 取值来自 form.model[prop]，model 为空时 fieldValue 恒为 undefined，疾病诊断/跌倒史等 required 规则无法反映真实输入（要么校验永不通过卡住流程，要么完全空转）。同项目 config.vue:22 是标准写法。
- 根本原因：漏写 :model 绑定。
- 修复建议：<el-form ... :model="healthForm" :rules="healthRules">。
- 代码证据：

    <el-form label-width="140px" class="eval-form" ref="healthFormRef" :rules="healthRules" :disabled="isReadOnly">

### 30. src/views/checkin/evaluate.vue:750 —— 评估参数被双层包裹，落库结构畸形（下游被迫双重解包）
- 问题描述：evaluateCheckin(id, { evaluation: evaluationJson })，而 src/api/system/checkin.js:114 内部已经是 data: { evaluation }，最终请求体为 {"evaluation":{"evaluation":"..."}}。这正是 detail.vue:635-640 与 approve.vue:367-370 需要判断 evaluation.evaluation != null 再 parse 一次的原因：写端出错、读端打补丁。
- 根本原因：调用方多包了一层对象。
- 修复建议：改为 evaluateCheckin(id, evaluationJson)，使请求体为 { evaluation: "<json>" }，并移除下游的兜底解包。
- 代码证据：

        const res = await evaluateCheckin(currentCheckInId.value, { evaluation: evaluationJson })
        // api/system/checkin.js:114  data: { evaluation }

### 31. src/views/checkin/config.vue:534 —— 用「床位 id」匹配「房型 id」，床位费永远带不出且被判超限
- 问题描述：roomTypeList 来自房型接口，r 是床位行，rt.id === r.id 比较两个不同实体的主键，命中率≈0，于是走 else 把 bedCost 与 originalBedFee 置 0；随后 watch(configForm.bedCost)（559-569 行）用 originalBedFee*0.9 判断，用户手填任何大于 0 的床位费都会被判「超出/低于原费用10%」而无法提交。
- 根本原因：关联字段用错（应为 r.roomTypeId 或按房型名匹配），且未匹配时把原费用写成 0。
- 修复建议：按 rt.id === r.roomTypeId 或 rt.typeName === r.roomTypeName 匹配；未匹配时不写 originalBedFee，或提示用户。
- 代码证据：

      const roomType = roomTypeList.value.find(rt => rt.id === r.id)
      if (roomType && roomType.price != null) { ... } else { originalBedFee.value = 0; configForm.bedCost = 0 }

### 32. src/views/checkin/approve.vue:221-222 —— 性别映射与写入端相反，审批人看到的性别是错的
- 问题描述：apply.vue:363 由身份证第 17 位推导 gender：'1' 男 / '0' 女，apply.vue:31-32 单选、detail.vue:434-437、sign.vue:202-204 也一致为 1=男；只有 approve.vue 的 sexLabel 写成 '0'→男、'1'→女，types.ts:27 注释同样是反的。审批页 337 行用该函数渲染性别。
- 根本原因：多份重复实现、没有公共字典，approve 那份写反。
- 修复建议：抽公共 genderLabel(v)（1=男，0=女）并替换 approve.vue 与 types.ts 的反向定义；reapply.vue:54 的 label="2" 女 也需统一为 0。
- 代码证据：

      if (v === '0') return '男'
      if (v === '1') return '女'

### 33. src/views/checkin/detail.vue:514 / approve.vue:405 / evaluate.vue:595 —— 同一评估结果三套互斥的等级阈值，同一老人三页结论不同
- 问题描述：自理能力总分到能力等级/护理等级的换算三处各写一遍且互相矛盾：detail 判 <=20 能力完好；approve 判 0→能力完好、<=20 轻度失能；evaluate 判 <=10 能力完好、11-20 轻度。护理等级更冲突：approve 0→特级护理，detail <=20→无需护理，evaluate 0→无需护理、41-50→特级护理。直接影响收费与照护配置。
- 根本原因：评分规则无单一来源，散落在 3 个组件内（approve.vue:401 与 434 甚至同文件内重复计算两次总分）。
- 修复建议：抽 @/utils/assessment（唯一实现 + 单测），三处统一调用。
- 代码证据：

    detail.vue:514     if (totalScore <= 20) return '能力完好'
    approve.vue:436    checkInInfo.value.nursingLevel = '特级护理'
    evaluate.vue:595   if (score <= 10) return '能力完好'

### 34. src/views/system/floor/index.vue:521 —— addBed 不判断返回码，失败仍提示「新增床位成功」
- 问题描述：同文件 updateBed/updateRoom 都判断 res.code === 200，这里 await addBed(bedData) 的结果被丢弃，后端报错也提示成功并关闭弹窗，形成假成功。
- 根本原因：漏写结果校验。
- 修复建议：const res = await addBed(bedData); if (res.code !== 200) return ElMessage.error(res.msg)。
- 代码证据：

        await addBed(bedData);
        ElMessage.success("新增床位成功");

### 35. src/views/system/floor/index.vue:334 —— 姓名为空时静默把用户选择的床位状态改成「空闲」
- 问题描述：保存床位时若姓名为空，不报错不提示，直接把 bedStatus 改成 '0' 提交，用户在弹窗里选的「已入住/请假中」被无声覆盖，老人从床位数据中消失，仅留下 console.error。
- 根本原因：用副作用赋值代替校验（应校验「非空闲必须填写老人姓名」）。
- 修复建议：bedStatus != '0' 且姓名为空时 ElMessage.warning 并中断提交。
- 代码证据：

        const name = (editBedForm.name || '').trim();
        if (!name) editBedForm.bedStatus = '0';

### 36. src/views/system/floor/index.vue:387（及 509-521）—— 楼层切换无竞态控制，且床位容量校验口径与展示口径不一致
- 问题描述：（a）handleFloorChange 连续点击会并发多次 getFloorRoomBed，先发后到的响应无条件写入 roomList，出现高亮的楼层与展示数据不一致；（b）展示用过滤掉占位床位的 validRoomList（262 行 filter(bed => bed.bedId != null)），容量判断却用原始 room.beds?.length（509 行），把空占位床计入，未满也提示「已达上限」无法新增；（c）511 行 let max = 4 为默认，房型名不含「单人/双人/四人」就按 4 张放行，且 room.roomTypeName.includes 在字段为 null 时抛 TypeError。
- 根本原因：无请求版本号；同一数据两套口径；房型用字符串包含映射。
- 修复建议：维护 requestId 丢弃过期响应；校验统一用过滤后的 beds；房型改 roomTypeId 精确映射并加空值保护。
- 代码证据：

        const current = room.beds?.length || 0;   // 未过滤占位床
        let max = 4;
        if (room.roomTypeName.includes("单人")) max = 1;

### 37. src/views/system/user/index.vue:193（及 451-467）—— 导入用户上传无 on-error，isUploading 卡死后无法再选文件
- 问题描述：el-upload 只绑了 on-progress/on-success/on-change/on-remove；isUploading = true 在 handleFileUploadProgress 设置、仅在 handleFileSuccess 复位。上传失败（非 2xx/超时/后端异常）后 :disabled="upload.isUploading" 恒为 true，用户无法再次选择文件，只能刷新。
- 根本原因：只处理成功分支。
- 修复建议：绑定 :on-error 并在其中复位 isUploading 并提示。
- 代码证据：

      <el-upload ref="uploadRef" ... :disabled="upload.isUploading" :on-progress="handleFileUploadProgress" :on-success="handleFileSuccess" ...>

### 38. src/views/system/user/index.vue:424 —— 重置密码后把明文新口令回显到页面提示
- 问题描述：resetUserPwd 成功后 proxy.$modal.msgSuccess("修改成功，新密码是：" + value)，明文口令出现在 UI 中，可被肩窥/截图/日志采集插件记录。
- 根本原因：为「方便告知」把口令回显。
- 修复建议：只提示「重置成功」，交付走一次性凭据或强制首次登录改密。
- 代码证据：

        proxy.$modal.msgSuccess("修改成功，新密码是：" + value)

### 39. src/views/system/menu/index.vue:413 —— await 一个不返回 Promise 的函数，等待是空操作
- 问题描述：getTreeselect（335-342 行）内部只有 listMenu().then(...)，无 async 无 return，返回 undefined；handleUpdate 里 await getTreeselect() 立即继续，随后打开的「修改菜单」弹窗中 menuOptions 仍为空，上级菜单树空白/父级无法回显。
- 根本原因：把「发起请求」当成「等待完成」。
- 修复建议：改为 return listMenu().then(...) 或显式串联。
- 代码证据：

    async function handleUpdate(row: SysMenu) {
      reset()
      await getTreeselect()

### 40. src/views/system/user/index.vue:303、320 —— 模板 ref 未判空 + JSON.parse(JSON.stringify(undefined)) 抛错
- 问题描述：（a）watch(deptName) 中 proxy.$refs["deptTreeRef"].filter(val) 无可选链，切换 showSearch/keep-alive 复用而 ref 为空时抛 Cannot read properties of undefined；354 行 setCurrentKey 同样。（b）getDeptTree 中 JSON.parse(JSON.stringify(response.data)) 未判 data 存在，data 为 undefined 时 JSON.parse(undefined) 抛 SyntaxError，部门树初始化中断，且无 .catch。
- 根本原因：模板 ref 缺存在性保护；把深拷贝当成无条件安全（JSON.stringify(undefined) 返回 undefined）。
- 修复建议：proxy.$refs["deptTreeRef"]?.filter(val)；const raw = response.data ?? [] 后再拷贝，并补 catch/finally。
- 代码证据：

    watch(deptName, (val: string) => { proxy.$refs["deptTreeRef"].filter(val) })
    enabledDeptOptions.value = filterDisabledDept(JSON.parse(JSON.stringify(response.data)))

### 41. src/views/system/refund/index.vue:271（及 367）—— 逻辑删除标志 delFlag 暴露给前端且必填
- 问题描述：新增/修改弹窗可直接填写「删除标志(0存在 2删除)」，rules 还要求必填，提交时 form 整体回传，客户端可自行把资金记录标记为已删除或改回存在，绕过逻辑删除语义。src/views/system/leave/index.vue:404 同构。
- 根本原因：代码生成器把实体字段无差别暴露到表单与 rules，逻辑删除字段未由后端独占。
- 修复建议：前端移除该表单项与规则，后端忽略入参 delFlag。
- 代码证据：

      <el-form-item label="删除标志(0存在 2删除)" prop="delFlag">
        <el-input v-model="form.delFlag" placeholder="请输入删除标志(0存在 2删除)" />

### 42. src/views/system/refund/index.vue:513-549 —— 资金操作无防重、失败静默
- 问题描述：handleRefundSuccess 只靠 $modal.confirm，按钮与弹窗无 loading/禁用态，双击可并发发出两次 refundSuccess(row.id)（重复退款/重复回调）；submitRefundFail 的 refundFail(...).then() 完全没有 .catch，失败时静默且弹窗不关闭。
- 根本原因：资金类操作按普通 CRUD 写法实现，无提交锁与幂等。
- 修复建议：加 submitting 标志禁用按钮、补 catch 与失败提示，后端加幂等键。
- 代码证据：

          .then(() => refundSuccess(row.id!))
          .then(() => { proxy.$modal.msgSuccess("操作成功"); getList() })
          .catch((e) => { console.error(e) })

### 43. src/views/system/type/index.vue:92-96（同类：src/views/system/elder/index.vue:110、dict/index.vue:148）—— 表单/表格字段与校验错配
- 问题描述：（a）type/index.vue 的 el-upload 未传 headers（见第 27 条），且 on-success 不判 code，photo 可能被写成 undefined；（b）elder/index.vue:110 label="欠费金额" prop="age"、label="支付截止时间" prop="sex" 字段错位，搜索项（20 行）与表单（145 行）也把欠费金额绑在 age 上；（c）dict/index.vue:148 的 el-form-item prop="dictName" 而输入框 v-model="form.dictType"，dictType 的 required 永不触发，字典类型可提交为空。
- 根本原因：复制模板后只改文案未同步字段/prop。
- 修复建议：逐列核对 label↔prop↔v-model，并做一次全量表单契约核对。
- 代码证据：

          <el-table-column label="欠费金额" align="center" prop="age" />
          <el-form-item prop="dictName">   <!-- 但 v-model="form.dictType" -->

### 44. src/views/tool/gen/index.vue:192-194 —— 重置表单名字写错（静默失效）+ pageNum 被写成 NaN
- 问题描述：onActivated 调用 proxy.resetForm("queryForm")，但模板里的表单 ref 是 queryRef（第 3 行；同文件 255 行是正确写法），重置永远无效；同段还把 route.query.pageNum 直接 Number 后赋给分页参数，URL 上没有该参数时得到 NaN，请求带 pageNum=NaN 发出。
- 根本原因：复制其它页面的调用名未核对 ref 名；对 query 参数做无兜底 Number 转换。
- 修复建议：改成 proxy.resetForm("queryRef")；queryParams.value.pageNum = Number(route.query.pageNum) || 1。
- 代码证据：

        queryParams.value.pageNum = Number(route.query.pageNum)
        proxy.resetForm("queryForm")

### 45. src/views/tool/gen/editTable.vue:200-214 —— 用全局选择器抢 DOM 且 Sortable 实例不销毁
- 问题描述：onMounted 里 document.querySelector('.el-table__body > tbody') 没有 nextTick、没有限定组件容器，取到的是全文档第一个匹配的 tbody（可能是别的表格），el-table 未渲染时静默绑定失败；创建的 Sortable 无 destroy()，反复进入编辑页累积实例与 DOM 监听。
- 根本原因：全局选择器 + 忽略第三方实例生命周期。
- 修复建议：await nextTick() 并用 ref 限定容器；保存实例，onBeforeUnmount 中 destroy()。
- 代码证据：

      const element = document.querySelector('.el-table__body > tbody')
      Sortable.create(element as HTMLElement, { handle: ".allowDrag", ... })

### 46. src/views/tool/build/RightPanel.vue:632 与 IconsDialog.vue:23-31 —— 跨组件实例误用与未声明 props
- 问题描述：（a）++proxy.idGlobal 中的 proxy 来自 getCurrentInstance()，指向 RightPanel 自身，而 idGlobal 定义在父组件 build/index.vue:123，自增读到 undefined → 写入 NaN，父组件 id 计数器完全没动，新增/复制节点可能 formId/renderKey 重复；（b）父组件通过 :current="activeData[currentIconModel]" 传值，但 IconsDialog 没有 defineProps，current 落进 attrs 从未使用，当前图标永不回显高亮。
- 根本原因：Vue2 的 $parent/隐式 props 思路在 script setup 中失效。
- 修复建议：id 由父组件通过 props/emit 分配；IconsDialog 补 defineProps({ current: String }) 并 watch 同步 active。
- 代码证据：

    function addTreeItem(): void { ++proxy.idGlobal; ... }
    const active = ref<string>('')   // IconsDialog 未声明 current

### 47. 15 个页面组件的 name 全为 "Config"，keep-alive 缓存与组件名匹配失效
- 文件：src/views/system/config/index.vue:167、refund/index.vue:308、item/index.vue:182、level/index.vue:158、leave/index.vue:418、record/index.vue:230、elder/index.vue:171、contract/index.vue:318、code/item/index.vue:101、code/plan/index.vue:265、code/payment/index.vue:172、code/arrival/index.vue:101、code/reservation/index.vue:145、code/checkout/index.vue:141、code/nursingLevel/index.vue:173
- 问题描述：这些页面都以 config/index.vue 为模板复制，组件 name 未改。src/layout/components/AppMain.vue:5 的 keep-alive :include="tagsViewStore.cachedViews" 用的是路由 name（tagsView.ts:49-53 推入 view.name），与组件 name 不一致时该页不会被缓存（切页即丢状态），同名则可能命中同一缓存条目造成串页。
- 根本原因：模板复制未改组件 name，且项目未校验 name 唯一性。
- 修复建议：每个页面改为与路由一致的唯一 name；可加构建期/ESLint 检查重复 name。
- 代码证据：

    <script setup lang="ts" name="Config">   // 15 个页面重复

---

## 🟡 中优先级问题

### 48. src/views/checkin/index.vue:141 与 apply.vue:466 —— 只读模式直接 return，导致「查看详情」表单全空
- 问题描述：success.vue:59 会以 mode:'view' 跳转，apply.vue 一旦 props.viewMode 为真就在 onMounted 首行 return，basicForm/familyList/imagePaths 永远不加载，只读页显示空白表单；其后 catch(e){} 为空，任何加载异常被静默吞掉。
- 根本原因：把「只读」误写成「不加载」。
- 修复建议：删除提前 return（仅禁用输入），catch 至少打印/提示。
- 代码证据：

    onMounted(async () => {
      if (props.viewMode) return
      ...
      } catch (e) { }

### 49. src/views/checkin/apply.vue:171（及三处上传）—— 上传地址硬编码 /dev-api，生产环境必然 404
- 问题描述：三处 el-upload action="/dev-api/common/upload" 写死开发前缀，.env.production 的 VITE_APP_BASE_API='/prod-api'，打包后该路径无代理、无网关前缀。组件内的 baseApi（apply.vue:262）只用于图片回显。
- 根本原因：硬编码环境前缀，未使用 import.meta.env.VITE_APP_BASE_API 或项目 FileUpload 组件。
- 修复建议：改用 VITE_APP_BASE_API 拼接或统一走 @/components/FileUpload。
- 代码证据：

                  action="/dev-api/common/upload"

### 50. src/views/checkin/apply.vue:493-516、533-537 —— 校验函数定义后从未调用，必填项形同虚设
- 问题描述：validateFamily 全文件仅出现 1 次（未使用），handleNext 的 family 分支直接 activeTab='upload' 并提示「保存成功」；validateUpload 恒返回 true 且调用点被注释。模板中三处「*一寸照片/身份证人像面/身份国徽面」完全不受约束。
- 根本原因：校验写好后未接入流程。
- 修复建议：family 分支补 if (!validateFamily()) return；upload 分支校验三个文件非空。
- 代码证据：

      } else if (activeTab.value === 'family') {
        // 验证家属信息
        activeTab.value = 'upload'

### 51. src/views/checkin/apply.vue:325-329 —— 下拉选项混入测试与不当数据
- 问题描述：民族列表含「张梦族」，政治面貌含「sb张梦」，宗教含「张教」，学历含「张梦」。这些会出现在生产表单并写入业务数据，其中「sb」属不当内容。
- 根本原因：开发期临时测试项未清理。
- 修复建议：删除这些项，改为后端字典驱动。
- 代码证据：

      "珞巴族","基诺族","张梦族"
      '群众','中共党员',...,'sb张梦'

### 52. src/views/checkin/reapply.vue:65 —— 使用 Element UI 的 picker-options，禁用未来日期不生效
- 问题描述：Element Plus 已改为 disabled-date（同项目 config.vue:31、71 是正确写法）。picker-options 在 EP 中被忽略，disabledDate 永不执行，出生日期可选未来日期。
- 根本原因：从旧版组件复制未适配 EP。
- 修复建议：改 :disabled-date="pickerOptions.disabledDate" 或直接写方法。
- 代码证据：

                    :picker-options="pickerOptions"

### 53. src/views/checkin/detail.vue:172 与 evaluate.vue:273 —— mentalForm.cognitive.length 无空值保护
- 问题描述：两处模板直接取 .length，而 detail.vue:708、evaluate.vue:882 用 Object.assign(mentalForm, xxx.mentalForm) 覆盖，一旦后端旧数据缺 cognitive 或为 null，渲染期抛 Cannot read properties of undefined (reading 'length')，整页渲染失败（evaluate.vue:644 的提交校验同理）。
- 根本原因：假设后端字段完整，未用可选链/默认值合并。
- 修复建议：(mentalForm.cognitive || []).length；Object.assign 改为带默认值的字段级赋值。
- 代码证据：

                    {{ mentalForm.cognitive.length > 0 ? '已评估' : '未评估' }}

### 54. src/views/checkin/reapply.vue:54 与 approve.vue:221 —— 性别编码 2=女 与全局 0=女 冲突
- 问题描述：apply.vue:31-32、detail.vue:434-437、sign.vue:202-204 都是 1=男/0=女，reapply 用 label="2" 女 且 handleIdCardBlur 只算年龄不推导性别，重新申请会把 gender='2' 写回后端，下游详情/签约按 0=女 解析会显示为「-」。
- 根本原因：同一字典多份实现。
- 修复建议：改为 label="0"，并复用公共性别字典。
- 代码证据：

                  <el-radio-group v-model="basicForm.gender">
                    <el-radio label="1">男</el-radio>
                    <el-radio label="2">女</el-radio>

### 55. src/views/checkin/reapply.vue:432-435 —— 表单校验失败被当成提交失败，且错误信息为空
- 问题描述：Promise.all([...validate()]) 的 rejection（校验不通过的对象）落进同一个 catch，弹出「重新申请提交失败：未知错误」，用户看不到哪一页哪一项不合格；用了 error.message（校验错误对象没有该字段）。
- 根本原因：校验异常与请求异常共用 catch。
- 修复建议：先 await validate().catch(() => false) 提前返回并提示页签，或在 catch 里区分数组/Error。
- 代码证据：

      ElMessage.error('重新申请提交失败：' + (error.message || '未知错误'))

### 56. src/views/checkin/apply.vue:548 与 reapply.vue:415 —— 家属信息两套存储结构，重新申请后家属信息丢失
- 问题描述：apply 写入 reviewInfo.familyList（detail.vue:602 也只读该路径），reapply 提交到 otherApplyInfo.familyForm（并只从该路径读取）。两者互不兼容：重新申请时看到的家属信息为空，提交后详情页也读不到新填的家属。
- 根本原因：首次申请/重新申请两条链路使用不同 schema，无公共 DTO。
- 修复建议：统一字段路径，并在 detail/approve/reapply 共用解析函数。
- 代码证据：

    apply.vue:548    familyList: familyList.value,
    reapply.vue:415  familyForm: { ...familyForm },

### 57. src/views/checkin/config.vue:280、200-201、597 —— 死规则、重复列与错误字段名
- 问题描述：（a）configRules 声明 6 条 required 但 submitConfig 只做手写 if 判断，configFormRef.validate() 从未调用，且 configRules.bedId 对应的 configForm.bedId 不存在（床位在 selectedBed），启用即恒失败；（b）账单表格「类型」与「费用项目」两列都是 prop="type"，「费用项目」列重复显示类型值；（c）账单预览取 find(...)?.name，而下拉用的是 levelName（第 41 行），恒 undefined → 护理项名称永远显示兜底值「护理」。
- 根本原因：表单校验写一半改成手工校验；复制列/字段未同步。
- 修复建议：绑定 bedId 并调用 validate（或删除无效规则与 ref）；「费用项目」列改 prop="name"；统一用 levelName。
- 代码证据：

        <el-table-column label="类型" prop="type" width="100" />
        <el-table-column label="费用项目" prop="type" width="100" />   // 重复

### 58. src/views/checkin/config.vue:407-409 与 695-699 —— 回填丢失床位选择；楼层/房间/床位重复全量加载
- 问题描述：（a）回填只 Object.assign(configForm, res.data.configInfo)，不含 selectedBed，重开页面「入住床位」为空、床位费回到旧值，重选床位会再次触发第 31 条的错误价格逻辑覆盖原费用；（b）onMounted 已调 loadFloors()，openBedSelect() 又调 loadFloors+loadRooms+loadFreeBeds，三个接口都是 pageSize: 500 全量拉取后前端过滤，首屏就多发 2 个 500 条请求。
- 根本原因：床位用局部状态承载未纳入回填；无按楼层查询接口，加载策略粗放。
- 修复建议：从 configInfo 还原 selectedBed 且回填时不覆盖费用；按 floorId/roomId 服务端查询，去掉弹窗前的预热请求。
- 代码证据：

          if (res.data.configInfo) { Object.assign(configForm, res.data.configInfo) }
          ...
        onMounted(() => { loadCheckIn(); loadNursingLevels(); loadRoomTypes(); loadFloors() })

### 59. src/views/checkin/config.vue:674 与 apply.vue:576 —— 关键提交无 loading/防重，可重复提交
- 问题描述：submitCheckInConfig、applyCheckin、reapplyCheckin 的按钮没有 loading 与禁用（对比 evaluate.vue:340、reapply.vue:202、approve.vue:115 都有 :loading），config 确认后请求期间可再次点击，导致重复占床/重复建单。
- 根本原因：提交态未纳入 UI 状态。
- 修复建议：加 submitting ref，:loading="submitting"，finally 复位。
- 代码证据：

                <el-button type="primary" @click="submitConfig">提交</el-button>

### 60. src/views/checkin/detail.vue:691-704 与 652 —— 用题干文本匹配回填得分；medications.map 未过滤空元素
- 问题描述：（a）detail.vue 内置 110 行硬编码题库（307-416，与 evaluate.vue:440-549 完全重复），再用 String(q.question).trim() 建 Map 匹配后端题面，题面一字之差（空格/标点/版本）即匹配失败，score 保持 null → calculateAbilityScore 返回 0 → 详情页显示「0 分/能力完好」，与真实评估不符且无提示；（b）evaluation.healthForm.medications 中只要有一个 null，med.name 抛 TypeError，被 728 行的大 try 捕获后提示「解析评估信息失败」，后续能力评估、精神状态、报告全部不再填充（页面大面积空白）。
- 根本原因：题库双份维护且以文本为关联键；数组元素未过滤、容错粒度太粗（一个大 try 包住所有解析）。
- 修复建议：用稳定题目 code/id 作为匹配键，题库抽公共常量；.filter(Boolean).map(...)，各区块解析拆成独立 try。
- 代码证据：

                healthForm.medication = evaluation.healthForm.medications.map(med => `${med.name} (${med.dosage}, ${med.method})`).join('; ')
                const key = String(q.question || '').trim()

### 61. src/views/checkin/evaluate.vue:586-589、617 与 840 —— 未作答按 0 分计入；单层 JSON.parse 无法还原双层数据
- 问题描述：（a）item.score || 0 把 null 当 0，getAbilityLevel 在 score<=10 返回「能力完好」、getNursingLevel 在 0 分返回「无需护理」，用户只填健康页就切到「评估报告」会看到「能力完好/无需护理」的错误结论；（b）evaluate 这里只 JSON.parse 一次，而 detail/approve 需要二次解析（第 30 条），双层编码的历史数据解析后得到字符串，populateFormData 内部所有分支静默不成立，已保存的评估在评估页显示为空。
- 根本原因：未区分「未作答(null)」与「0 分」；读端容错策略不统一。
- 修复建议：有 score===null 时返回「未评估」并阻止出报告；复用统一的 parseJsonValue（apply.vue:404 已有实现）。
- 代码证据：

      return total + (item.score || 0)
      ...
            const evalData = JSON.parse(res.data.evaluation);

### 62. src/views/system/floor/index.vue:379、397、511（及 262）- —— 定时器无清理、不判 res.code、房型/容量判断脆弱
- 问题描述：（a）379 行 setTimeout(() => handleFloorChange(), 50) 串联首次加载，全文件无 onUnmounted/clearTimeout，组件 50ms 内卸载时回调仍执行；（b）397 行只读 res.data?.[0]?.rooms，不判 res.code，接口失败时静默显示「暂无房间数据」；（c）见第 36 条与第 63 条。
- 根本原因：用定时器代替 nextTick/显式串联且无生命周期清理；成功结构当成必然。
- 修复建议：改 nextTick/watch(floorList) 并清理定时器；先判 res.code === 200 再取数据。
- 代码证据：

            activeFloor.value = floorList.value[0].id + "";
            setTimeout(() => handleFloorChange(), 50);

### 63. src/views/system/user/index.vue:245、516、560-561 —— initPassword 先使用后赋值，新增用户密码框初始为空
- 问题描述：const initPassword = ref(undefined)（245 行），handleAdd（516 行）读取它时 onMounted（560 行）发起的 getConfigKey("sys.user.initPassword") 可能尚未返回（且该赋值在 561 行），新增用户时密码框为空；同时这是「看起来可用实际是死值」的隐患。
- 根本原因：异步初始化与使用时机未对齐。
- 修复建议：改成 await getConfigKey 后再打开弹窗，或在 reset() 中显式留空并提示默认口令。
- 代码证据：

        form.value.password = initPassword.value   // 561 行才赋值

### 64. src/views/system/user/profile/userAvatar.vue:152 与 userInfo.vue:51 —— ref/reactive 混用写错对象 + 直接改写 props
- 问题描述：（a）模板 v-if="visible" 控制 vue-cropper，closeDialog 却写 options.visible = false（只给 reactive 加了个无关属性），visible 永为 true，弹窗关闭后裁剪实例不销毁；（b）userInfo.vue 保存成功后 props.user.phonenumber = form.value.phonenumber 原地改写父组件对象，父状态被静默篡改。
- 根本原因：变量名近似写错；缺少 emit/回调机制。
- 修复建议：visible.value = false 并重置 options；改 emit('update', ...) 或由父组件重新拉取。
- 代码证据：

    function closeDialog() { options.img = userStore.avatar; options.visible = false }
    props.user.phonenumber = form.value.phonenumber

### 65. src/views/system/user/authRole.vue:112 与 dict/data.vue:366、monitor/job/log.vue:275 —— 路由参数恒真/恒假判断导致 NaN 请求与 loading 卡死
- 问题描述：（a）authRole：const userId = route.params && Number(route.params.userId)，参数缺失时为 NaN → if 为假，初始化整体跳过，loading 保持 true，页面永久骨架（getAuthRole 也无 catch）；（b）dict/data：getTypes(route.params && Number(route.params.dictId))，route.params 恒为对象（恒真），缺参数时请求 /system/dict/type/NaN；（c）job/log：jobId !== undefined && jobId != 0 —— Number() 结果永远不是 undefined，NaN != 0 也为真，会以 getJob(NaN) 发请求。
- 根本原因：用逻辑与/undefined 做数字有效性判断，未用 Number.isFinite。
- 修复建议：统一 const id = Number(route.params?.x); if (!Number.isFinite(id)) { loading.value = false; 提示返回 }。
- 代码证据：

    const userId = route.params && Number(route.params.userId)   // authRole.vue:112
    if (jobId !== undefined && jobId != 0) {                     // job/log.vue:275

### 66. src/views/system/item/index.vue:226（及 level:197、contract:370、elder:203）—— rules 声明必填但表单无对应控件
- 问题描述：规则里写了 status 必填，弹窗表单却没有 prop="status" 的 el-form-item（表格却展示该列），规则永不触发；reset() 把 status 置 null，新增记录状态为空。contract 的 status 同样只在列表展示、表单无录入项。
- 根本原因：生成模板裁剪了字段但未同步裁剪 rules。
- 修复建议：补状态控件（el-select + 枚举）或删除死规则；状态由后端默认值维护。
- 代码证据：

        status: [ { required: true, message: "状态：0-启用，1-禁用不能为空", trigger: "change" } ],

### 67. src/views/system/menu/index.vue:475-480 —— 直接访问 el-tree 内部 store，无判空
- 问题描述：menuRef.value.store.nodesMap[treeList[i].id].expanded = value（dept 分支 480 行同）依赖组件私有实现，树未渲染/节点异步加载/element-plus 升级时抛 Cannot read properties of undefined。
- 根本原因：依赖私有 API 且缺可选链。
- 修复建议：改用公开能力（default-expand-all 或递归设置 expanded），至少全部加 ?.。
- 代码证据：

          menuRef.value.store.nodesMap[treeList[i].id].expanded = value

### 68. src/views/system/role/selectUser.vue:29、98 —— @row-click 与复选框自身选中冲突
- 问题描述：表格同时绑 @row-click="clickRow" 与 selection-change，clickRow 对任意行 toggleRowSelection；点击复选框单元格时选中与 toggle 各执行一次，勾选被立刻取消/随机反转，handleSelectUser 取到的 userIds 不可靠。
- 根本原因：行点击与复选框点击事件叠加，未区分触发列。
- 修复建议：row-click 中判断 column.type !== 'selection' 再 toggle，或移除 row-click。
- 代码证据：

    function clickRow(row: SysUser) { proxy.$refs["refTable"].toggleRowSelection(row) }

### 69. src/views/system/leave/index.vue:234、src/views/system/contract/index.vue:186、config/index.vue:104、monitor/operlog/index.vue:169 —— PII 与敏感参数明文展示并进入导出链路
- 问题描述：（a）请假列表明文展示老人身份证号与手机号，合同页明文展示丙方手机号且可编辑；（b）系统参数页原样渲染 configValue 并参与 xlsx 导出，参数表常存初始密码/密钥；（c）操作日志详情直接展示 operParam 与 jsonResult（登录、改密、新增用户入参常含明文密码/token），同页还可整表导出。
- 根本原因：生成器按数据库字段逐个渲染，未区分 PII/敏感字段；日志明细无脱敏。
- 修复建议：默认掩码（110***********1234 / 138****8888），按 key 白名单处理参数展示与导出，后端对 password/token/secret 打码。
- 代码证据：

          <el-table-column label="老人身份证号" align="center" prop="elderIdCard" />
          <el-table-column label="参数键值" align="center" prop="configValue" :show-overflow-tooltip="true" />

### 70. src/views/code/nursingTask/index.vue:159 与 273/376 —— value-format 大小写错误与 UTC 时间当本地时间
- 问题描述：（a）159 行 value-format="yyyy-MM-dd HH:mm:ss" 使用小写 yyyy（本仓库其余 10 处均为大写 YYYY，该文件 31、192 行也是大写），小写令牌会产出含字面 yyyy 与英文星期名的脏值提交后端；（b）273、376 行用 new Date().toISOString() 作为执行时间默认值（UTC），中国时区比本地早 8 小时，且截断秒后直接提交。
- 根本原因：手写格式串时大小写混用；用 ISO(UTC) 当本地时间格式化。
- 修复建议：统一 YYYY-MM-DD HH:mm:ss；用 dayjs().format('YYYY-MM-DD HH:mm:ss')（文件已引入 dayjs）。
- 代码证据：

            value-format="yyyy-MM-dd HH:mm:ss"
            executeTime: new Date().toISOString().slice(0, 19).replace('T', ' '),

### 71. src/views/code/leave/index.vue:317 —— 返回登记时间经 toISOString 转 UTC 后提交
- 问题描述：用户选的是本地时间，parseDateTimeString 得到本地 Date 再 toISOString()，后端按字面存储则实际返回时间整体差 8 小时，直接影响实际请假天数。
- 根本原因：本地时间误用 UTC 序列化；同文件其它展示走 parseTime，风格不统一。
- 修复建议：直接提交 returnForm.actualReturnTime（已是 YYYY-MM-DD HH:mm:ss）。
- 代码证据：

      const actualReturnTime = parseDateTimeString(returnForm.actualReturnTime).toISOString()

### 72. src/views/code/recharge/index.vue:196 —— useDict 返回的是 ref，Array.isArray 恒为 false，状态字典永不渲染
- 问题描述：hasRechargeStatusDict 初始即 false 且无 watch，模板 v-if="hasRechargeStatusDict" 永远走 else，状态列只显示裸值。已核实 src/utils/dict.ts:22 返回 toRefs(res.value)，字典是 ref，Array.isArray(ref) 必为 false；且字典异步加载，setup 期求值本身即错。
- 根本原因：误判 useDict 的返回形态；同步求值替代响应式求值。
- 修复建议：const hasRechargeStatusDict = computed(() => Array.isArray(lc_recharge_status.value) && lc_recharge_status.value.length > 0)。
- 代码证据：

    const hasRechargeStatusDict = ref<boolean>(Array.isArray(lc_recharge_status) && lc_recharge_status.length > 0)

### 73. src/views/code/checkout/billApproval.vue:209-211（checkoutApproval、applyApprovalAdmin 同）—— 审批单选默认值无法回显，类型漂移
- 问题描述：approvalForm.approvalResult 初始为数字 1，而 <el-radio label="1"> 的 value 是字符串，初始无任何项被选中，用户必须手动点；点后值变字符串，action 类型随之漂移。label="number" 是无效属性（EP 中 label 是显示文本/旧值）。
- 根本原因：用已废弃的 label 承载值，数字/字符串混用。
- 修复建议：改用 <el-radio :value="1">，删除 label="number"。
- 代码证据：

      <el-radio-group v-model="approvalForm.approvalResult" label="number">
        <el-radio label="1">审批通过</el-radio>

### 74. src/views/code/item/index.vue:88-97 与 payment/index.vue:159-168 —— 新增/修改弹窗是空表单
- 问题描述：el-form 内没有任何 el-form-item，rules 却写了必填项；用户点「新增」后无法录入任何字段，提交的是 reset() 的全 null 对象。
- 根本原因：字段被删空但 submitForm/reset/rules 保留，属半成品组件。
- 修复建议：补齐字段，或删除该弹窗与相关按钮/接口调用。
- 代码证据：

        <el-form ref="itemRef" :model="form" :rules="rules" label-width="80px">
        </el-form>

### 75. N+1 逐行请求（4 处）
- 文件：src/views/code/leave/index.vue:197-200、src/views/code/leave/todo.vue:82-90、src/views/code/nursingLevel/index.vue:255-262、src/views/code/nursingItem/index.vue:309-319
- 问题描述：列表每行再发一次详情/引用校验请求，leave/todo 与 nursingLevel 还是串行 await，页大小 10~100 时接口数成倍放大；失败被静默吞掉。
- 根本原因：后端列表接口未返回展示所需字段，前端逐行补齐。
- 修复建议：列表接口下发字段，或提供批量接口（如 checkReferenced({ids})）一次返回；至少改并发并统一 loading。
- 代码证据：

        await Promise.all(needFillRows.map(async (item: ElderLeave) => {
          try { const response = await getLeave(item.id!)

### 76. src/views/code/reservation/index.vue:148-149 —— 导入第三方库内部实现路径且完全未使用
- 问题描述：import { el } from "element-plus/es/locales.mjs"、import { log } from "echarts/types/src/util/log.js" 引用第三方内部文件，未使用却进入依赖图；内部路径随时可能在升级后消失，属构建期定时炸弹。
- 根本原因：编辑器自动补全误插入，lint 未拦住未使用导入。
- 修复建议：删除这两行；开启 no-unused-vars。
- 代码证据：

    import { el } from "element-plus/es/locales.mjs"
    import { log } from "echarts/types/src/util/log.js"

### 77. src/views/code/reservation/index.vue:226-249 —— 到院提交未 await、无 catch，先关弹窗再提示成功
- 问题描述：updateArrivalTime(...).then(...) 未 await，紧接着就关闭弹窗；失败时既不回滚也无提示，Promise 未 catch 产生未处理拒绝。currentRow 还是 ref(null)，读 .id 类型不安全。
- 根本原因：把 then 链写在 try 里误以为被 try 捕获（异步拒绝不会）。
- 修复建议：await 后再关弹窗并刷新；currentRow 用 ref<LcReservation | null>(null) 并判空。
- 代码证据：

      updateArrivalTime({ id: currentRow.value.id, visitTime: visitForm.visitTime }).then(res => {

### 78. src/views/code/arrival/index.vue:131-132 —— rules 为空对象，表单校验形同虚设
- 问题描述：data.rules = {}，而 submitForm 仍走 validate，来访人姓名/手机号/来访时间/访问类别都能为空提交（同结构的 reservation 至少还有状态字典）。
- 根本原因：规则未填。
- 修复建议：补必填规则，手机号加 pattern（项目已有 src/utils/validate 可复用）。
- 代码证据：

      } as ArrivalQueryParams,
      rules: {
      }

### 79. 审批页 taskId/businessId 直接取自 URL 且前端不校验归属（越权风险）
- 文件：src/views/code/checkout/applyApprovalAdmin.vue:285-286、billAdjustment.vue:305-306、billApproval.vue:265-266、checkoutApproval.vue:259-260、contractTerm.vue:108-109、finalSettlement.vue:146-147
- 问题描述：一律 const taskId = route.query.taskId 后直接 completeTask，前端不校验该任务是否属于当前用户/当前节点；若后端未做任务归属与节点权限校验，篡改 URL 参数即可替他人完成审批。另 applyApprovalAdmin.vue:383-390 的 businessData.updateBy 由前端提交，可伪造操作人。
- 根本原因：敏感标识由客户端提供且无二次确认；操作人字段由前端决定。
- 修复建议：提交前用任务详情接口校验归属；updateBy 由后端从登录态取，前端不传。
- 代码证据：

    const taskId = route.query.taskId;
    const bussinessId = route.query.businessId;

### 80. src/views/code/checkout/billAdjustment.vue:111 与 421-423、plan/index.vue:519-521、nursingTask/index.vue:362 —— 计数、过滤与校验条件写错
- 问题描述：（a）billAdjustment:111 待办数用 arrearsList.length + 1 拼魔法数字，列表为空时 el-empty 说「暂无历史欠费账单」而待办显示 1；（b）billAdjustment:421-423 等处 arrearsList 只保留 tradeStatus=='0'，取月度金额却用未过滤的 data[0].billAmount，第一条不是待支付时金额与列表不一致（局部 const data 遮蔽外层 reactive data）；（c）plan:519-521 重名校验只在 if (!form.value.id) 时执行，修改时不校验唯一性；（d）nursingTask:362 等 await cancelFormRef.value?.validate() 位于 try 之外、无 catch，校验失败产生 Unhandled Rejection 且无用户提示。
- 根本原因：魔法数字与过滤口径不一致；条件写反；promise 形式 validate 未 catch。
- 修复建议：计数由数据源长度决定；过滤结果统一复用；去掉 plan 的 if；validate().catch(() => false) 后提前 return。
- 代码证据：

            待办：<span class="badge-text">{{ arrearsList.length + 1 }}</span>
        if (data.length > 0) { refundForm.billAmount = data[0].billAmount || 0 }
        const confirmCancel = async () => { await cancelFormRef.value?.validate()

### 81. src/views/code/checkout/bill/detail.vue:223-224 与 orders/detail.vue:56-63、279 —— 支付信息建模错误与硬编码猜测
- 问题描述：（a）bill/detail 只取 paymentRows[0] 渲染一笔支付，分次支付的账单丢失其余记录（orders/detail.vue:279 又取最后一条，两处口径相反）；（b）orders/detail.vue:56-63 只要 tradeStatus ∈{1,2,3,4} 就显示「线上支付/微信」，金额直接取订单金额，未接入支付记录接口（bill/detail 已用 listBillPayment）。
- 根本原因：把支付记录当单条建模；用状态推断替代真实支付数据。
- 修复建议：用表格渲染全部支付记录；调用 listBillPayment 取真实渠道与实付金额。
- 代码证据：

      paymentRecord.value = paymentRows.length > 0 ? paymentRows[0] : null
      {{ ["1", "2", "3", "4"].includes(payDisplayStatus) ? "微信" : "-" }}

### 82. src/views/code/recharge/index.vue:124-126、218-222 与 orders/index.vue:331-334 —— 资金表单校验与幂等缺失
- 问题描述：（a）充值金额是普通 el-input，rules 只有 required，非数字（abc、1e5）可直接提交，且「确定」按钮请求期间不禁用，网络慢时连点会重复入账；（b）生成费用账单成功后不 getList()、无提交锁，可对同一订单反复生成多张账单。
- 根本原因：金额字段沿用文本输入框；关键资金操作无 submitting 状态。
- 修复建议：改 el-input-number（precision=2, min=0.01）或加正则；提交期间禁用按钮；成功后刷新列表。
- 代码证据：

        rechargeAmount: [{ required: true, message: "请输入充值金额", trigger: "blur" }]
        .then(() => { proxy.$modal.msgSuccess("费用账单生成成功") })

### 83. src/views/code/checkout/checkoutApply.vue:226-239 —— 老人详情请求存在竞态，快速切换会串数据
- 问题描述：await elderDetail(elderId) 返回后无差别覆盖 form 字段，连续切换老人或网络乱序时旧响应覆盖新选择，导致床位/合同/护理员与实际老人不符。
- 根本原因：无请求序号/取消机制。
- 修复建议：加自增序号比对（if (seq !== current) return）或 AbortController。
- 代码证据：

      const resp = await elderDetail(elderId);
      if (resp.data) { form.bedNo = resp.data.bedNo;

### 84. src/views/code/checkout/finishment.vue:163、202 与 190-192 —— 空 catch 吞异常，并用默认值伪造财务事实
- 问题描述：（a）catch(e){} 把账单加载失败与页面初始化失败全部吃掉，页面只呈现空白（0 元、无记录）；（b）后端没返回就展示「银行转账」「退住费用已清算完成」，把未知包装成事实。
- 根本原因：为消除控制台报错直接吞异常；用 || 兜底成有业务语义的文案。
- 修复建议：catch 中打印并提示、加错误占位；空值显示「-」，不要兜底成结论。
- 代码证据：

      refundForm.refundMethod = billRes.data.tradingChannel || '银行转账'
      }catch(e){}

### 85. src/views/code/nursingItem/index.vue:258-260 与 nursingTask:302-305 —— 上传回调不判业务码，失败时把错误体写进图片地址
- 问题描述：response.data || response.url 未校验 response.code === 200，网关返回 401/500 JSON 时会把错误对象字段当作 URL 存入 imageUrl，页面出现坏图且无提示（对比 bill/index.vue:433-440 的正确写法）。
- 根本原因：只处理成功结构。
- 修复建议：判 code，失败时 msgError 并清空。
- 代码证据：

        form.value.imageUrl = response.data || response.url;
        ElMessage.success("上传成功");

### 86. src/views/code/refund/index.vue:275-277（arrival:247、payment:292、reservation:390、item:197 同）—— 详情赋值无兜底，data 为 null 时渲染报错
- 问题描述：form.value = response.data 后立即打开弹窗，若 data 为 null，模板访问 form.refundNo 在渲染期抛 Cannot read properties of null。
- 根本原因：直接把响应赋给 ref，无默认对象。
- 修复建议：form.value = response.data || {}，或加 v-if 守卫。
- 代码证据：

        form.value = response.data
        open.value = true

### 87. src/views/code/item/index.vue:198、208 与 responsible/index.vue:136、139 —— 字段类型不确定且未判空
- 问题描述：（a）serviceContent 在 reset() 是数组、详情是逗号字符串故 .split(",")、提交又 .join(",")，某条记录为 null 时 .split 抛错导致弹窗打不开；（b）responsible 依赖 item.nurseNames.split(' ') 假设后端返回字符串，改数组即 TypeError（列表整块渲染失败）；parseInt(a.code) 在 code 非纯数字时得 NaN，排序不确定。
- 根本原因：同一字段建模不一致，前端用 any 承接接口类型。
- 修复建议：统一按数组建模并 (x || []) 处理；Array.isArray 兜底 + localeCompare 排序。
- 代码证据：

        form.value.serviceContent = form.value.serviceContent.split(",")
        nurseNames: item.nurseNames ? item.nurseNames.split(' ').filter(Boolean) : []

### 88. src/views/index.vue:441、386、417-421 与 548/563/577 —— 首页图表竞态、失败态渲染与串行无保护 await
- 问题描述：（a）refreshChart 里 if (isLoading.value) return 直接丢弃并发刷新，快速切页签后 activeTab 已变但图表仍是上一个页签的数据且不自愈；（b）loadCountData 无论成功失败都在 finally 调 renderAllCharts()，接口失败时画出一堆全 0 饼图；renderAllCharts 对同一 DOM 反复 echarts.init 且未复用/销毁（会告警并泄漏），chartRefs 函数 ref 只累积不清空；（c）老人等级/年龄性别/服务能力三个互不依赖的接口串行 await 且无 try/catch，任一失败即中断 onMounted 后续（含 resize 注册）并产生未处理拒绝。
- 根本原因：用布尔值做单飞去重而不记录最新意图；渲染时机放在 finally；把可并行的请求写成瀑布式且无异常分支。
- 修复建议：用自增请求序号或 AbortController；仅在成功分支渲染，用 echarts.getInstanceByDom(el) || echarts.init(el)；改 Promise.all/allSettled 并包 try/catch。
- 代码证据：

    async function refreshChart() { if (isLoading.value) return; isLoading.value = true
      } finally { renderAllCharts() }
      const d1 = await getNursingLevelStat()

### 89. src/views/index.vue:224、281、402-405 与 675-682 —— 首页展示明文手机号、假图表数据与全量拉取
- 问题描述：（a）预约卡片明文渲染预约人完整手机号；（b）todoList.value = res.data 无兜底，data 为 null 时 todoList.length/v-for 抛错导致待办卡片白屏；（c）「服务单数量」饼图总数与计划内/计划外（24/10/14）全部写死；（d）listReservation({}) 不带日期/分页参数，全量拉到浏览器再按 7 天在前端过滤。
- 根本原因：看板复用列表接口；缺少脱敏与默认值兜底；占位假数据未回收。
- 修复建议：手机号掩码，按日期区间请求，res.data ?? [] 兜底，接入真实统计接口。
- 代码证据：

    <span>预约人：{{ item.name }} | 手机号：{{ item.phone }}</span>
    { total: 24, data: [ { name: '计划内', value: 10, ... }

### 90. src/views/tool/build/index.vue:147-151 —— $modal.confirm 无 catch，取消操作产生未处理拒绝
- 问题描述：清空组件用 .then() 而没有 .catch，Element Plus MessageBox 在用户点取消时 reject('cancel')，每次取消都抛未处理拒绝（控制台报错、前端监控误报）；工具内大量 .catch((e) => console.error(e)) 也把「用户取消」当错误打印。
- 根本原因：把 confirm 当纯 then 链使用，忽略取消路径。
- 修复建议：加 .catch(() => {})，或 const ok = await ElMessageBox.confirm(...).catch(() => false); if (!ok) return。
- 代码证据：

      proxy.$modal.confirm('确定要清空所有组件吗？', '提示', { type: 'warning' }).then(() => {

### 91. src/views/monitor/cache/index.vue:82、101、131 —— setup 顶层发请求 + 非空断言掩盖 DOM 未挂载风险
- 问题描述：echarts.init(commandstats.value!, "macarons") 用 ! 掩盖可能为 null 的 ref；getList() 在 setup 顶层（131 行）调用，只在 then 里初始化图表，一旦响应早于挂载返回，echarts.init(null) 抛错，resize 监听与图表都不会创建；图表实例也没有 onUnmounted dispose。
- 根本原因：把依赖 DOM 的初始化放在网络回调里，用 ! 消除类型告警而非消除风险。
- 修复建议：放到 onMounted/nextTick 后并加 if (!el) return；卸载时 dispose。
- 代码证据：

        const commandstatsIntance = echarts.init(commandstats.value!, "macarons")

### 92. src/views/monitor/cache/list.vue:192-195、220-223 与 118-125、252-256 —— 假成功提示与破坏性操作无权限指令
- 问题描述：（a）refreshCacheNames 先调异步 getCacheNames()（未 await）紧接着弹「刷新缓存列表成功」，失败时仍提示成功，与第 20 条的 loading 卡死叠加造成误判；（b）「清理全部」及各键删除按钮没有任何 v-hasPermi（该文件 grep 为 0，而同目录 online/operlog/job 都有），任何能打开缓存列表的用户可一键清空 Redis。
- 根本原因：异步调用当同步用；破坏性操作未做按钮级授权。
- 修复建议：await 后再提示并加 catch；补 v-hasPermi=['monitor:cache:remove']，后端同步校验。
- 代码证据：

    function refreshCacheNames(): void { getCacheNames(); proxy.$modal.msgSuccess("刷新缓存列表成功") }
              @click="handleClearCacheAll()"

### 93. src/views/monitor/cache/list.vue:180 与 src/views/tool/gen/editTable.vue:136 —— 表格高度只在 setup 计算一次
- 问题描述：window.innerHeight - 200 / document.documentElement.scrollHeight - 245 在组件创建时求值并固化为 px，窗口或侧边栏尺寸变化后不再更新，出现留白或内容截断。
- 根本原因：把一次性窗口测量当成响应式值。
- 修复建议：用 CSS calc(100vh - Npx)，或监听 resize（并清理监听）。
- 代码证据：

    const tableHeight = ref<number>(window.innerHeight - 200)

### 94. src/views/tool/gen/genInfoForm.vue:266-274、307 与 basicInfoForm.vue:34-39 —— 直接改写 props 内的对象，且 Object 默认值为 null
- 问题描述：子组件写 props.info.subTableFkName = ""、props.info.tplWebType = ...，模板也 v-model="info.tplCategory" 直接改父对象，父状态被静默修改；info 的 default: null 在父组件未传时使 info.tplCategory 渲染即报错。
- 根本原因：沿用 Vue2「props 传对象随便改」的习惯；默认值写法错误。
- 修复建议：改 emit('update:info', {...}) + v-model:info；default 改 () => ({})。
- 代码证据：

      if (props.info) { props.info.subTableFkName = "" }

### 95. src/views/tool/gen/importTable.vue:79-81 与 basicInfoForm.vue:6、34-39 —— $refs 无判空、请求无 catch、文案复制错误
- 问题描述：（a）proxy.$refs.table.toggleRowSelection(row) 未判空，表格未渲染时抛 Cannot read properties of undefined；getList 只有 then 没有 catch，失败时列表静默为空；（b）表名称输入框 placeholder 写成「请输入仓库名称」。
- 根本原因：refs 字符串取值无保护；复制模板未校正文案。
- 修复建议：改 proxy.$refs.table?.toggleRowSelection(row)；补 catch；placeholder 改「请输入表名称」。
- 代码证据：

        proxy.$refs.table.toggleRowSelection(row)
        <el-input placeholder="请输入仓库名称" v-model="info.tableName" />

### 96. src/views/error/404.vue:29-31 与 401.vue —— computed 包装常量；401 无历史时返回无响应
- 问题描述：message 的 computed 内部返回固定字符串，无任何响应式依赖，属误用 computed；401.vue 只用 $router.go(-1)，无历史记录时点击返回无反应。
- 根本原因：模板变量改成常量时留下 computed 外壳；返回逻辑未兜底。
- 修复建议：直接 const message = '找不到网页！'；401 在 window.history.length <= 1 时降级 router.push('/')。
- 代码证据：

    const message = computed(() => { return '找不到网页！' })

---

## 🔵 低优先级问题

### 97. 死代码、空文件与备份文件
- src/components/Checkout/elderInfo.vue 为 0 字节空文件；src/types/index-bak.ts（第 4 行是非法语法 ....）、src/types/api/index-bak.ts 为备份文件（前者被 tsconfig include 命中）。
- src/views/code/checkout/index.vue:294-295 的 submitForm 使用模板中不存在的 proxy.$refs["checkoutRef"]，永不可达；checkout/index.vue:267-275 handleApproveDetail 无模板引用；leave/index.vue:340-361 为整段注释代码；arrival/reservation 的 ids/single/multiple/handleDelete/handleExport 无对应按钮。
- src/views/monitor/job/index.vue:383-397 handleCommand 无任何调用点。
- 未使用导入：approve.vue:154 computed、approve.vue:159 formatDate、checkin/index.vue:34 watch、reapply.vue:211 computed、system/checkin/index.vue:251 delCheckin、monitor/job/log.vue:177 SysJob、code/reservation/index.vue:148-149（第三方内部路径）。
- 修复建议：删除空文件/备份/死代码；开启 eslint no-unused-vars 与 ts-prune 类检查。

### 98. TypeScript 严格模式下的既存类型/语法问题，且没有 type-check 脚本兜底
- tsconfig.json:15 strict: true 且 include 覆盖 src/**/*.ts，但：src/types/index-bak.ts:4 是非法语法；src/api/index.ts:11、45、54 参数隐式 any（TS7006）；package.json:8-13 只有 dev/build，没有 type-check 脚本，因此这些错误在本地与 CI 都不会暴露。
- 修复建议：删除备份文件、补参数类型、增加 "type-check": "vue-tsc --noEmit" 并在 CI 强制执行。

### 99. 外部链接与 iframe 安全基线缺失
- 8 处 window.open（src/views/code/checkout/finishment.vue:206、finalSettlement.vue:302、contractTerm.vue:202、checkoutApproval.vue:420、billApproval.vue:380、billAdjustment.vue:518、applyApprovalAdmin.vue:363、src/views/code/checkout/finishment.vue:87 的 a[target=_blank]）以及 src/components/FileUpload/index.vue:32 的 el-link target="_blank"，都未校验协议、未加 noopener/noreferrer，被打开页面可通过 window.opener 反向控制后台窗口。项目里已有 safeOpenUrl（src/utils/index.ts:6-12，仅允许 http/https）但全项目零调用。
- src/views/monitor/druid/index.vue:10、src/views/tool/swagger/index.vue:8 通过 src/components/iFrame/index.vue 内嵌后端 Druid 控制台与 Swagger UI；iFrame 组件对 src 无白名单、无 sandbox 属性。
- 修复建议：统一改用 safeOpenUrl 并补 noopener,noreferrer；el-link 加 rel；iFrame 加 src 白名单与 sandbox；生产禁用 Druid/Swagger 页面。

### 100. src/components/iFrame/index.vue:27-29 —— 覆盖全局 window.onresize 且不清理
- 问题描述：直接 window.onresize = function 会顶掉其它模块的 onresize 处理器，组件卸载后不恢复；高度只在挂载时计算一次，字符串里还多了个分号（+ "px;"）。
- 修复建议：改 addEventListener，onUnmounted 移除；去掉分号。

### 101. 事件监听/第三方实例未在卸载时清理
- src/layout/components/TagsView/ScrollPane.vue:21-26 addEventListener 带 capture=true，removeEventListener 未带 capture → 监听移除失败；同文件 62-73 visitedViews.value[currentIndex - 1].path 在 currentIndex 为 0/-1 时读 undefined.path 抛 TypeError。
- src/views/system/refund/index.vue 等弹窗表单内：src/components/FileUpload/index.vue:223-239、ImageUpload/index.vue:242-257 的 Sortable.create 实例从未 destroy()；src/components/Editor/index.vue:130 quill.root 的 paste 监听未移除。
- src/layout/components/TagsView/index.vue:73-79 body click 监听只在 visible 变 false 时移除，组件在菜单打开时被卸载会残留。
- 修复建议：统一在 onBeforeUnmount/onUnmounted 中反注册与 destroy；移除 capture 参数保持对称。

### 102. 上传组件在 setup 阶段快照 token，重新登录后上传 401
- src/components/Editor/index.vue:41-43、src/components/FileUpload/index.vue:103、src/components/ImageUpload/index.vue:113、src/views/code/bill/index.vue:281、src/views/code/recharge/index.vue:194 都在 setup 时求值 Authorization: "Bearer " + getToken()；token 变化后仍发旧值，token 为空时还会发 "Bearer null"。
- 修复建议：改为 computed/getter 在请求前取最新 token，或统一走 request 实例。

### 103. src/store/modules/tagsView.ts:180-183、206-209 —— findIndex 返回 -1 被当作下标使用，误删 iframe 页签
- 问题描述：const fi = this.iframeViews.findIndex(...); this.iframeViews.splice(fi, 1)，找不到时 fi === -1，splice(-1, 1) 会删除最后一个元素，关闭左侧/右侧页签会误删无关记录。
- 修复建议：if (fi > -1) 再 splice。

### 104. src/store/modules/dict.ts:18-21 —— 判空条件写错（&& 恒假）
- 问题描述：if (_key == null && _key == "") 一个值不可能同时等于两者，条件恒为假，本意应为 ||；当前靠循环兜底侥幸不出错。
- 修复建议：改为 if (_key == null || _key === '')。

### 105. src/layout/components/Sidebar/SidebarItem.vue:86-99 —— 无保护的 JSON.parse 与 title.length
- 问题描述：JSON.parse(routeQuery) 未包 try/catch，后端菜单 query 字段写错一个字符即让侧边栏渲染抛错白屏；hasTitle(title: string) 直接取 title.length，而调用点（第 7、15 行）在 meta 存在但 title 缺失时传入 undefined。
- 修复建议：JSON.parse 包 try/catch 并忽略异常；hasTitle(title?: string) 用 (title || '').length。

### 106. src/components/RightToolbar/index.vue:109-120、144-164 —— 直接修改 props 对象（单向数据流被破坏）
- 问题描述：props.columns[key].visible = event 等 6 处直接改写入参对象；父组件传入非响应式对象时不刷新，显隐列状态与实际表格不一致，且 Vue 开发环境会告警。
- 修复建议：emit('update:columns', ...) 或 v-model 交给父组件更新。

### 107. src/layout/components/Settings/index.vue:179 与 TopNav/index.vue:98-113、77-95 —— setTimeout 传调用结果；computed 中执行副作用
- 问题描述：（a）setTimeout(proxy.$modal.closeLoading(), 1000) 第一个参数是 closeLoading() 的执行结果（undefined），遮罩在调用瞬间即关闭，1 秒延迟完全失效（同文件 185-187 行是正确写法）；（b）TopNav 的 childrenMenus computed 往 routers 子路由写 parentPath/path，activeMenu computed 内部调用 activeRoutes 进而 setSidebarRouters/toggleSideBarHide 修改状态，属读取状态又写状态的典型反模式，易触发 Maximum recursive updates 或侧边栏状态错乱。
- 修复建议：setTimeout(() => proxy.$modal.closeLoading(), 1000)；把导航联动移到 watch/watchEffect，路由规范化在 generateRoutes 一次完成。

### 108. src/components/HeaderSearch/index.vue:162-170 —— fuse 未判空，未初始化时输入关键字抛错
- 问题描述：fuse.value.search(query) 无判空；fuse 只在 watch(searchPool) 触发时初始化，若用户早于路由数据打开搜索框并输入，会抛 Cannot read properties of undefined (reading 'search')。
- 修复建议：fuse.value ? fuse.value.search(query) : []，或在 onMounted 直接 initFuse 并去掉对 watch 的依赖。

### 109. src/utils/request.ts:42-67 —— 防重复提交把请求体明文写入 sessionStorage，且大小计算 API 误用
- 问题描述：所有 POST/PUT 都把 JSON.stringify(config.data) 存入 sessionStorage.sessionObj，老人/账单/审批表单中的身份证、手机号、金额等会明文留在会话存储；第 47 行 Object.keys(JSON.stringify(requestObj)).length 是把 Object.keys 用在字符串上（返回下标数组，长度恰好≈字符数），写法错误且对多字节字符低估大小。
- 修复建议：只存 url + 请求体哈希 + 时间戳，不落原文；大小判定改 new Blob([json]).size；文件上传类请求跳过该逻辑。

### 110. src/utils/auth.ts:3-12 —— Token 存于非 httpOnly Cookie（XSS 可读）
- 问题描述：Admin-Token 存在 JS 可读 Cookie（sameSite: 'Strict'、按协议设 secure 的做法本身是正确的），一旦出现 XSS 可直接读取并复用。本次审计未发现 v-html（全库 0 处），富文本统一用 quill 的 contenteditable，src/utils/index.ts:162-167 的 html2Text 用 DOMParser + textContent（安全），因此当前 XSS 面较小，但第三方组件漏洞仍可能引入 XSS。
- 修复建议：敏感系统改为 httpOnly Cookie 或短期 access token + refresh token；用 ESLint 规则 vue/no-v-html 固化「禁止 v-html」。

### 111. 分页与列表规模（保留观察项）
- 全库 <el-table 出现 679 次，而分页组件仅 46 次。src/views/system/checkin/list.vue:14（待办/候选/已完成三个表格）与 src/views/code/leave/todo.vue 等真实列表页未接分页，且 api/system/checkin.js:47-85 的对应接口不接收 pageNum/pageSize，数据量增长后一次性渲染成百上千行。
- 修复建议：列表接口补分页，前端统一接 Pagination 或 el-table-v2 虚拟滚动。

### 112. src/views/system/refund/index.vue:179、188 与 type/index.vue:197 —— 状态判断与上传结果的脆弱写法
- 问题描述：（a）v-if="scope.row.refundStatus === '0'" 使用严格相等，若后端序列化为 number 0 则退款按钮永不渲染；（b）type/index.vue on-success 不判 code，只要 HTTP 200 就提示上传成功并把 response.data || response.url 写入 photo，后端 code=500 时 photo 为 undefined。
- 修复建议：统一状态类型或 String(row.refundStatus) === '0'；上传回调先判 code。

---

## 已核查、未发现问题的项（避免误报，供复核）

- 全库无 v-html / innerHTML / document.write / eval / new Function，无 DOM 型 XSS 注入点；html2Text 使用 DOMParser + textContent（安全）。
- .env.development / .env.production / .env.staging 中不含任何密钥、账号或内部地址，VITE_APP_BASE_API 为相对路径（/dev-api、/prod-api、/stage-api）。
- 前端源码中未发现硬编码的口令、API Key、私钥（jsencrypt.ts:3-4 的 RSA 公钥非敏感信息，但 decrypt 已废弃，见第 17 条）。
- isToken: false 仅出现在登录/注册/验证码三处（src/api/login.ts:15、28、56），不存在对业务接口关闭鉴权的写法。
- 未发现 console.log 输出敏感数据；未发现 localStorage/sessionStorage 存放令牌或密码（仅 layout-setting、sidebarStatus、size、sessionObj 用途，见第 109 条风险）。

---

## 修复优先级建议（按投入产出比）

1. 立即修：第 1、2 条语法错误（否则无法构建/加载）；第 3 条口令落 Cookie；第 4、5、6 条权限模型。
2. 一个迭代内：第 20 条 loading/finally 统一封装（覆盖 30+ 页面，收益最大）、第 10、11 条响应解包统一、第 12 条 DictTag、第 13 条监听清理、第 14 条 generateRoutes 失败分支。
3. 数据正确性：第 7、22、23、24、25、43、70、71 条——这些会直接产生错误金额、错误人员、错误时间与非法查询条件。
4. 工程治理：接入 vue-tsc type-check + ESLint(no-unused-vars、vue/no-v-html、vue/require-prop-types) + 组件 name 唯一性检查（第 47 条）+ 删除死代码（第 97 条）。
