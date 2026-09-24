# AiServiceImpl 代码解读

> 文件：`lcyl-java/lcyl/lcyl-admin/src/main/java/com/lcyl/web/service/impl/AiServiceImpl.java`（692 行，23 个方法）
> 配套：`ToolExecutor.java`（真正执行操作的类）

---

## 一、先记住三句话

1. **它不让 AI 自己查数据库**。整个类的思路是「后端先把数据查好 → 塞进 System Prompt → AI 只负责理解和表达」。
2. **所有 queryXxx() 返回的是中文字符串**，不是对象。因为它们的结果要拼进 Prompt 给 AI 读。
3. **所有 formatXxx() 是把状态码翻译成中文**。数据库存 `"0"`/`"1"`，AI 看不懂，必须翻译成「待支付」「已支付」。

---

## 二、方法总览（23 个）

| 分组 | 方法 | 行号 | 作用 |
|------|------|------|------|
| ① 类的零件 | 字段 + @Autowired/@Value | 42-60 | 注入 Mapper、Redis、配置 |
| ② 入口 | `ask()` | 64-132 | 【最重要】用户提问的总流程 |
| | `executeConfirmed()` | 136-164 | 用户点「确认执行」后真正操作 |
| | `clearPendingConfirm()` | 166-169 | 用户点「取消」，删待确认记录 |
| | `hasPendingConfirm()` | 171-174 | 查有没有待确认操作 |
| | `getHistoryMessages()` | 677-691 | 给小程序恢复聊天记录 |
| ③ 工具调用 | `handleToolCalls()` | 181-254 | 处理 AI 发来的工具调用 |
| | `buildTools()` | 259-359 | 拼 4 个工具的 JSON 定义 |
| | `buildConfirmSummary()` | 364-388 | 拼「确定要…吗？」文案 |
| ④ 查数据 | `getUserContext()` | 392-405 | 查会员 + 绑定老人 |
| | `queryBills()` | 407-435 | 查账单 → 拼文字 |
| | `queryOrders()` | 437-458 | 查订单 → 拼文字 |
| | `queryContracts()` | 460-482 | 查合同 → 拼文字 |
| | `queryLeaves()` | 484-504 | 查请假 → 拼文字 |
| ⑤ 支撑 | `formatBillStatus()` 等 4 个 | 508-547 | 状态码翻译成中文 |
| | `buildSystemPrompt()` | 551-565 | 拼最终 System Prompt |
| | `callDeepSeek()` | 574-612 | 发 HTTP 请求给 DeepSeek |
| | `buildRequestBody()` | 617-650 | 组装请求 JSON |
| | `getHistory()` / `saveHistory()` | 655-671 | 读写 Redis 会话 |

---

## 三、核心数据结构：messages

**这是理解一切的钥匙**。它就是一个聊天记录数组：

```json
[
  { "role": "system",    "content": "你是乐康养老院的智能助手…【账单信息】…" },
  { "role": "user",      "content": "我妈妈这个月花了多少钱" },
  { "role": "assistant", "content": "您妈妈这个月一共花了 3200 元…" },
  { "role": "user",      "content": "那上个月呢" }
]
```

`role` 只有 4 种取值：

| role | 含义 |
|------|------|
| `system` | 给 AI 的人设 + 背景资料（含真实业务数据） |
| `user` | 用户说的话 |
| `assistant` | AI 之前说的话（或「我要调工具」） |
| `tool` | 工具执行的结果（回喂给 AI） |

---

## 四、ask() 的 7 个步骤

| 步骤 | 行号 | 做什么 |
|------|------|--------|
| 1 | 68-71 | `getUserContext()` 查会员+绑定老人；一个老人都没绑定就直接返回提示（不调 AI） |
| 2 | 74-77 | 预查 4 类数据，各自拼成中文字符串 |
| 3 | 80 | `buildSystemPrompt()` 把上面所有信息拼成 System Prompt |
| 4 | 83-89 | `getHistory()` 从 Redis 取历史；**只有首次对话才插入 system 消息** |
| 5 | 91-94 | 把用户这次的问题作为 `user` 消息追加 |
| 6 | 97-113 | `buildTools()` + `callDeepSeek()`；判断返回的是文本还是工具调用 |
| 7 | 115-127 | 纯文本则存历史并返回 |

兜底：整个方法包在 `try...catch` 里，任何异常都返回一句友好提示，避免用户看到「系统错误」。

---

## 五、两个分支：文本回复 vs 工具调用

```
DeepSeek 返回
   ├─ content 有值、无 tool_calls  →  直接返回这段文字
   └─ 有 tool_calls                →  handleToolCalls()
                                        ├─ 需确认（cancelOrder / applyRefund）
                                        │    → 存 Redis pending（120秒）
                                        │    → 返回 "[CONFIRM]确定要…吗？"
                                        │    → 小程序弹按钮
                                        │         ├─ 确认 → executeConfirmed() → ToolExecutor
                                        │         └─ 取消 → clearPendingConfirm()
                                        └─ 直接执行（cancelVisit / resubmitLeave）
                                             → ToolExecutor.execute()
                                             → 再调一次 DeepSeek（不带 tools）转成人话
```

### 三个容易懵的点

**Q1：为什么分支 A（需确认）要把刚加的消息删掉？（第 221 行）**
用户还没确认。若记进历史，下一轮 AI 会以为「已经取消了」，可能回答「您的订单已取消」，但用户点了取消其实什么都没发生。

**Q2：为什么分支 B（直接执行）要再调一次 DeepSeek？（第 237 行）**
第一次 AI 只说了「我要调工具」，`content` 是 null。执行完拿到结果（如「退款申请已提交」）后，得把结果喂回去，AI 才能组织成「已经帮您提交退款啦～」。

**Q3：`tool_call_id` 是干嘛的？**
身份证号。AI 一次可能调多个工具，靠 id 对应上「哪个结果属于哪次调用」。

---

## 六、Redis 里存了两样东西

| Key 格式 | 内容 | TTL | 用途 |
|---------|------|-----|------|
| `ai:session:{memberId}:{sessionId}` | 完整消息历史（含 system） | 1800 秒 | 多轮对话上下文 |
| `ai:pending:{memberId}:{sessionId}` | 待确认的工具名 + 参数 | **120 秒** | 等用户点确认，超时失效 |

裁剪规则：`saveHistory()` 最多保留 **1 条 system + 60 条对话**（30 轮）；`callDeepSeek()` 还会再裁一次，**只发 system + 最近 4 条**给 DeepSeek（省 token）。

---

## 七、安全设计（重要）

`executeConfirmed()` 第 150 行：

```java
if (!memberId.equals(pendingMemberId)) {
    return "操作已过期，请重新发起";
}
```

`sessionId` 是前端传的。如果 A 用户猜到/伪造了 B 用户的 sessionId，就能执行 B 的待确认操作。所以存 pending 时记下 memberId，执行时比对。**这是整个类里最重要的安全防线。**

另外 `ToolExecutor.execute()` 里还有一层：每个操作都校验「订单/请假单/预约是否属于该会员绑定的老人」，防止通过 AI 越权操作他人数据。

---

## 八、已知问题（待优化）

| # | 位置 | 问题 | 建议 |
|---|------|------|------|
| 1 | 第 84 行 | system 消息只在首次对话插入，后续用旧数据快照回答 | 每次用新 systemPrompt 覆盖 `messages[0]` |
| 2 | 第 488 行 | `queryLeaves()` 先查全表请假再内存过滤 | 给查询加 elderId 条件下推到数据库 |
| 3 | 第 184 行 | 只取 `toolCalls[0]`，AI 一次调多个工具会被丢弃 | 遍历处理，或明确告知模型一次只调一个 |
| 4 | 第 575-582 行 | 只带「system + 最近 4 条」，可能把 `tool_calls` 与其配对结果截断，导致 400 报错 | 裁剪时保证 tool_calls 与 tool 消息成对保留 |
| 5 | 第 259-359 行 | `buildTools()` 手拼 JSON 占 100 行 | 抽成静态 JSON 常量或用注解生成 |
| 6 | 全部 query 方法 | 每次提问都把 4 类数据全查一遍 | 按问题关键词只查相关的 1~2 类 |

---

## 九、阅读这类代码的通用套路

| 步骤 | 找什么 | 本类对应 |
|------|--------|---------|
| 1. 找入口 | `public` 且被 Controller 调的方法 | `ask()`、`executeConfirmed()` |
| 2. 找数据源 | 数据从哪来、怎么变成 AI 能读的文字 | `getUserContext()` + 4 个 `queryXxx()` |
| 3. 找协议 | 怎么和 AI 通信（消息格式、工具定义） | `buildSystemPrompt()`、`buildTools()`、`callDeepSeek()` |

其余都是**胶水代码**：格式化、拼字符串、读写缓存——逻辑简单，只是啰嗦。

---

## 十、关键补充：AI 为什么「知道」那 4 个工具

**AI 本身完全不知道你的系统有这些工具。** 是 `buildTools()` 每次生成定义、`buildRequestBody()`（第 645-647 行）塞进 HTTP 请求体的 `tools` 字段一起发过去的。

```java
if (tools != null) {
    body.put("tools", tools);   // 第 645-647 行
}
```

DeepSeek 的模型训练过这种协议（OpenAI Function Calling），看到 `tools` 字段就知道「原来我可以调这些函数」。

**它靠什么选工具？** 靠每个工具的 `description` 做语义匹配：
- `"取消服务订单"` → 用户说「退了吧」时不太匹配
- `"申请退款"` → **更匹配** ✅

所以 `description` 写得准不准，直接决定工具选得对不对。目前写得偏简略，遇到模糊表达可能误选，建议写详细些，例如：
`"申请退款：用户已支付但想退回款项时使用；如果只是未支付想放弃，请用 cancelOrder"`。

**第二次调用为什么故意不给工具？（第 237 行）**
`callDeepSeek(messages, null)` 传 null，`buildRequestBody` 里 `if (tools != null)` 就不放 `tools` 字段，等于告诉 DeepSeek「这次你没工具可用」，逼它只能用自然语言回答，避免它又去调工具陷入循环。
