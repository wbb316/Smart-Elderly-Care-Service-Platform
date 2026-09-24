# P3 治理项记录

> 日期：2026-08-16
> 原则：**能安全执行的就执行；不可逆或需要业务决策的只出方案**
> 验证：vue-tsc 实测、MySQL 实测行数、精确 grep 核实引用

---

## 完成项

| # | 项目 | 状态 |
|---|------|------|
| 1 | 索引迁移脚本纳入版本管理 | ✅ 已暂存（未提交） |
| 2 | 加 type-check 脚本 + 清理孤立文件 | ✅ 脚本已加、文件已清理（但**不能作为阻断门禁**，见下） |
| 3 | 死表清理 | 📄 脚本已生成，**未执行**（需你确认） |
| 4 | 前端写死常量 | 📄 已定位，未改（需业务决策） |
| 5 | vue 类型遮蔽问题 | 📄 已诊断（含实验数据），**未改**（避免无边界变更） |

---

## 1. 索引迁移脚本入库

`lcyl-java/lcyl/sql/20260802_index_migration.sql` 原本是未跟踪状态（`??`），但**已经执行到数据库**。
新环境部署不会执行它 → 12 条索引缺失导致性能问题复现。

已执行 `git add`（状态 `A`）。**未替你 commit** —— 你工作区还有 60+ 个未提交改动，
我不便替你决定提交范围。

---

## 2. 构建门禁：脚本已加，但**当前无法作为阻断门禁**

### 已做
- `package.json` 增加 `"type-check": "vue-tsc --noEmit"`（vue-tsc 2.1.10 已装）
- 清理 3 个孤立文件（均为备份/空文件，无人引用）：
  - `src/types/index-bak.ts`（含非法语法 `....`，文件自身注释就写着"追加好后此文件可删除"）
  - `src/types/api/index-bak.ts`
  - `src/components/Checkout/elderInfo.vue`（0 字节，无任何 import）

### 但：清理后暴露出 **185 个类型错误**

原因：`index-bak.ts` 里的非法语法 `....` 会让 TypeScript **语法分析阶段就中止**，
之后的语义检查全部跳过。删掉它之后，完整的类型检查才真正跑起来，暴露出 185 个既有错误。

**注意：这些错误与本次清理无关，是原本就存在的**（被语法错误掩盖了）。按错误码分布：

| 错误码 | 数量 | 含义 |
|--------|------|------|
| TS7006 | 88 | 参数隐式 any |
| TS2305 | 37 | 模块没有该导出成员 |
| TS2709 | 16 | 不能用作命名空间 |
| TS2339 | 13 | 属性不存在 |
| TS2345 | 8 | 参数类型不匹配 |
| 其他 | 23 | |

**结论**：`npm run type-check` 现在可以跑，但**会报 185 个错误，不能接成 CI 阻断门禁**。
建议把它作为「类型债务的度量工具」，先分批清零，再考虑设为门禁。

> 另注：`vite build` 走 esbuild，**只擦除类型不做检查**，所以 185 个错误不影响构建与运行。

---

## 3. 死表清理：脚本已生成，请确认后执行

`lcyl-java/lcyl/sql/20260816_dead_table_cleanup.sql`

这 9 张表在本项目代码中**无任何 SQL 引用**（已用精确 `(from|into|update|join)\s+表名` 核实），实测行数：

| 表 | 行数 | 大小 | 风险 |
|----|------|------|------|
| `lc_retreat_copy1` | 0 | 16 KB | 低（空表，且是手工复制的副本） |
| `member` | 0 | 16 KB | 低（与 `lc_member` 重复） |
| `sys_notice_read` | 0 | 32 KB | 低 |
| `device` | 12 | 48 KB | 低 |
| `nursing_project` | 12 | 32 KB | 低 |
| `nursing_project_plan` | 24 | 16 KB | 低 |
| `accraditation_record` | 94 | 48 KB | 中（表名疑似拼写错误） |
| `nursing_task` | 425 | 64 KB | 中（可能有历史数据） |
| **`device_data`** | **59,801** | **16.9 MB** | **高** ⚠️ |

### ⚠️ 为什么我没有直接执行
**本项目代码不引用这些表，不代表没有外部系统在用。**
尤其 `device_data`（6 万行 IoT 设备数据）**很可能是外部设备/服务持续写入的**——
我的 grep 只能证明「本仓库的 SQL 里没引用」，无法排除外部写入方。

一旦重命名/删除，外部写入方会直接报错。

### 脚本策略（可逆优先）
脚本用 `RENAME TABLE ... TO zzz_deprecated_*` 而**不是 `DROP`**：
- 重命名可随时改回，`DROP` 不可逆
- `device_data` 那行**默认注释掉**，需你确认写入方后手动放开
- 脚本末尾附「观察 2 周后再 DROP」的注释模板

**请先确认**：`device_data`/`nursing_task` 是否有项目外的读写方。确认无影响后再执行。

---

## 4. 前端写死常量：已定位，未改

`src/views/code/checkout/billApproval.vue:335-338`

```js
refundForm.billNo = 'ZD' + new Date().getFullYear() + String(new Date().getMonth()+1).padStart(2,'0') + '01015000001'
refundForm.billMonth = new Date().getFullYear() + "-" + String(new Date().getMonth()+1).padStart(2,'0')
refundForm.actualDays = 30
refundForm.refundDays = 12
```

- 单号尾部 `01015000001` 是魔数拼接
- `actualDays = 30` / `refundDays = 12` 是写死的天数 → **欠费与退款金额按假常量算出**

**为什么没改**：正确值应来自后端（入住天数、已消费天数）。
改成什么取决于业务口径（按自然月？按实际入住日？），属业务决策。
另外审计提到的 `SmartBed/index.vue` 整页假心率/假报警同理——需要真实数据源。

---

## 5. vue 类型遮蔽：已诊断，**故意未改**

### 问题
`src/types/global.d.ts` 是**全局脚本文件**（无顶层 import/export），
其中第 23-25 行：

```ts
declare module 'vue' {
  interface ComponentInternalInstance { proxy: any }
}
```

在脚本文件里，`declare module 'vue'` 会被 TypeScript 当作**重新声明整个 vue 模块**
（而不是模块扩展），于是 vue 的真实类型全部丢失 ——
这就是 `createApp`/`App` 等报"has no exported member"的原因。

同文件里还有 `declare module 'axios'`、`declare module 'element-plus'`、
`declare module '@vueup/vue-quill'` 这类**光声明无内容**的写法，同样会把对应库的类型抹成 any。

### 实验数据
我做了可回退的实验：把 vue 扩展拆到独立模块文件（`export {}` + 扩展声明）后：

| 状态 | 错误数 | vue 模块错误 |
|------|-------|------------|
| 修复前 | 185 | 53 |
| 修复后 | **760** | 0 |

vue 相关错误清零了，但总数从 185 涨到 **760** ——
因为 vue 类型恢复后，TypeScript 才真正开始检查 `.vue` 组件，
暴露出组件里原本被 `any` 掩盖的类型问题（`system/type/index.vue` 单个文件 51 个）。

### 为什么回退
- 这是**无边界的状态变更**：一旦启用，你正在开发的 61 个未提交文件会立刻"亮起"760 个错误
- 修复它本身是**正确方向**，但需要配套的类型清理计划，不适合塞进一轮治理改动
- 已验证回退后错误数精确回到 185，未留痕迹

**建议**：若要做类型清理，把它作为独立项目：
1. 先修 `global.d.ts` 的 vue/axios/element-plus 遮蔽（恢复真实类型）
2. 再按文件分批清零 760 个错误
3. 清零后再把 `type-check` 设为 CI 门禁

---

## P3 改动文件清单

| 文件 | 改动 |
|------|------|
| `lcyl-java/lcyl/sql/20260802_index_migration.sql` | **已暂存**（`git add`） |
| `lcyl-java/lcyl/sql/20260816_dead_table_cleanup.sql` | 新增（脚本，未执行） |
| `lcyl-vue/lcyl/package.json` | 加 `type-check` 脚本 |
| `lcyl-vue/lcyl/src/types/index-bak.ts` | 删除 |
| `lcyl-vue/lcyl/src/types/api/index-bak.ts` | 删除 |
| `lcyl-vue/lcyl/src/components/Checkout/elderInfo.vue` | 删除（0 字节空文件） |

---

## 需要你决策的三件事

| # | 事项 | 需要你确认什么 |
|---|------|--------------|
| 1 | 执行死表清理脚本 | `device_data`/`nursing_task` 是否有项目外的读写方 |
| 2 | 写死常量（结算天数/单号） | 正确口径（天数怎么算、单号怎么生成） |
| 3 | 类型清理（185→0 或先修 vue 遮蔽变 760 再清零） | 是否立项、优先级 |
