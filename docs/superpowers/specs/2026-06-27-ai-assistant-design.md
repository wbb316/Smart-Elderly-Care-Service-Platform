# AI 智能助手 — 设计规格说明书

> 为微信小程序家属端提供 AI 问答功能，基于 DeepSeek API。

**版本:** v1.0  
**状态:** 草稿  
**更新日期:** 2026-06-27

---

## 1. 概述

在小程序"我的"页面中增加 AI 智能助手入口，家属可通过自然语言查询老人的账单、订单、合同、请假等信息。后端基于 DeepSeek API 实现语义理解和数据检索。

### 1.1 目标
- 家属无需记忆菜单层级，直接说话就能查到想看的
- 降低学习成本，提升信息获取效率

### 1.2 非目标（v1 不做）
- 健康数据 AI 解读和分析（方案 1）
- 语音输入（后续可加）
- 主动推送（后续可加）

---

## 2. 架构设计

### 2.1 组件关系

```
小程序聊天页面
    ↓ POST /wxLogin/ai/ask { question, sessionId }
WxLoginController.aiAsk()
    ↓
AiService.ask(question, memberId, sessionId)
    ├─ 1. 解析用户绑定的老人列表（从 DB）
    ├─ 2. 根据 question 确定需要什么数据
    ├─ 3. 查询相关数据（账单/订单/合同/请假）
    ├─ 4. 组装 System Prompt + 上下文数据
    ├─ 5. 获取对话历史（Redis）
    ├─ 6. 调用 DeepSeek API
    └─ 7. 保存对话历史（Redis）
    ↓
返回回答文本
```

### 2.2 数据流

```
用户: "我妈妈上个月花了多少钱"
  → 后端查当前用户绑定的老人列表
  → 发现老人"张三"
  → 查张三上个月的账单汇总
  → 构建 Prompt:
      System: 你是养老院智能助手...当前用户: xxx, 绑定老人: 张三...
      本月账单: xxx 元, 明细: ...
  → 请求 DeepSeek
  → 返回: "张三上个月总共消费了 ¥2,350.00，其中..."
```

### 2.3 DeepSeek API

使用 DeepSeek 官方兼容 OpenAI 的 API：

```
POST https://api.deepseek.com/chat/completions
Authorization: Bearer {API_KEY}

{
  "model": "deepseek-chat",
  "messages": [
    {"role": "system", "content": "..."},
    {"role": "user", "content": "..."}
  ],
  "temperature": 0.3,
  "max_tokens": 1000
}
```

响应：
```json
{
  "choices": [{
    "message": {"role": "assistant", "content": "..."}
  }]
}
```

---

## 3. 详细设计

### 3.1 后端

#### AiService.java

```java
public interface AiService {
    String ask(String question, Long memberId, String sessionId);
}
```

**实现逻辑 AiServiceImpl.java：**

1. **getUserContext(memberId)** — 查 `lc_member` 表获取用户绑定的老人列表
2. **detectIntent(question)** — 根据关键词判断意图：账单、订单、合同、请假、其他
3. **queryData(intent, elders)** — 根据意图查对应数据：
   - 账单 → 查当月/上月 `bill` 表
   - 订单 → 查 `service_order` 表
   - 合同 → 查 `contract` 表
   - 请假 → 查 `elder_leave` 表
4. **buildPrompt(context, data)** — 组装 system prompt
5. **getHistory(sessionId)** — 从 Redis 取最近 6 轮对话
6. **callDeepSeek(messages)** — HTTP 调用 DeepSeek API
7. **saveHistory(sessionId, messages)** — 存回 Redis（TTL: 30 分钟）

#### DeepSeek 配置

```yaml
deepseek:
  api-key: ${DEEPSEEK_API_KEY}
  model: deepseek-chat
  temperature: 0.3
  max-tokens: 1000
```

通过环境变量 `DEEPSEEK_API_KEY` 注入，不硬编码。

#### System Prompt

```
你是乐康养老院的智能助手，你的名字叫"小乐"。
当前用户的信息：
- 姓名：{userName}
- 手机号：{userPhone}

绑定的老人信息：
{elderInfo}

当前可查询的数据：
{currentData}

回答要求：
1. 优先从上面提供的真实数据中回答，数据中有的信息直接给出
2. 如果用户问的问题不在可查询范围内，礼貌告知暂时无法回答
3. 使用温暖、亲切的语气
4. 回答简洁明了，可以适当使用表情符号
5. 涉及金额时注明明细
```

### 3.2 前端

#### 入口

在小程序"我的"页面右上角加一个 AI 图标按钮。

#### 页面

新建 `my/pages/aiChat/aiChat` 页面：

- 顶部：导航栏 + 标题"智能助手"
- 中间：消息列表（用户消息靠右，AI 消息靠左）
- 底部：输入框 + 发送按钮
- 加载状态：AI 回答时显示打字中动画

#### 请求格式

```javascript
request({
  url: '/wxLogin/ai/ask',
  method: 'POST',
  data: {
    question: '我妈妈上个月花了多少钱',
    sessionId: this.data.sessionId
  }
})
```

#### 响应格式

```json
{
  "code": 200,
  "data": "张三上个月总共消费了 ¥2,350.00..."
}
```

### 3.3 会话管理

- `sessionId` 用 `memberId` + 日期生成，如 `member_1001_20260627`
- Redis key: `ai:session:{sessionId}`
- TTL: 1800 秒（30 分钟）
- 存储格式：JSON 数组，最多 6 轮（12 条消息）

### 3.4 错误处理

| 场景 | 前端表现 | 后端处理 |
|------|---------|---------|
| DeepSeek API 超时 | "AI 暂时忙不过来，请稍后再试" | 返回 fallback 消息 |
| 无绑定老人 | "您还没有绑定老人，请先在「家人」页面添加" | 提前返回 |
| API Key 无效 | "服务暂不可用" | 日志告警 |
| 网络异常 | "网络异常" | 502 响应 |

---

## 4. 文件清单

### 后端新增
```
lcyl-code/src/main/java/com/lcyl/code/service/AiService.java
lcyl-code/src/main/java/com/lcyl/code/service/impl/AiServiceImpl.java
lcyl-code/src/main/resources/application.yml（加 deepseek 配置）
```

### 后端修改
```
lcyl-code/src/main/java/com/lcyl/code/controller/WxLoginController.java（加 /ai/ask 端点）
```

### 前端新增
```
lcyl-wx/my/pages/aiChat/aiChat.js
lcyl-wx/my/pages/aiChat/aiChat.json
lcyl-wx/my/pages/aiChat/aiChat.wxml
lcyl-wx/my/pages/aiChat/aiChat.wxss
```

### 前端修改
```
lcyl-wx/pages/my/my.js（加 AI 助手入口按钮）
lcyl-wx/pages/my/my.wxml（加 AI 助手入口）
lcyl-wx/app.json（注册 aiChat 页面）
```

---

## 5. 边界情况

- **未登录用户**点击 AI 助手 → 拦截器返回 401 → 前端跳转登录
- **未绑定老人的用户** → AI 返回"请先绑定老人"
- **多老人场景** → AI 能分辨"我妈妈"和"我爸爸"分别对应哪个老人
- **非问题输入** → AI 可处理简单问候/闲聊
- **敏感信息** → 限制 AI 只能查当前用户绑定的老人的数据
