# AI Agent Function Calling 设计规格说明书

> 在现有 AI 智能助手基础上增加 DeepSeek function calling 能力，让家属可以通过自然语言直接执行操作。

**版本:** v1.0  
**更新日期:** 2026-06-27

---

## 1. 概述

基于现有 `AiServiceImpl` 的问答能力，增加工具执行能力。AI 不再只是"回答"，而是可以"操作"。

### 1.1 支持的 Tools（v1）

| Tool | 说明 | 涉及退款 | 需要确认 |
|------|------|---------|---------|
| `cancelOrder` | 取消护理服务订单 | 是 | 是 |
| `applyRefund` | 申请退款 | 是 | 是 |
| `cancelVisit` | 取消探访/参观预约 | 否 | 否 |
| `resubmitLeave` | 重新提交被驳回的请假 | 否 | 否 |

### 1.2 非目标（v1 不做）
- 修改/删除数据库记录以外的操作
- 跨老人操作（只能操作自己绑定的老人的数据）
- 多步骤工具的自动编排

---

## 2. 架构设计

### 2.1 核心流程

```
用户 → "帮我把订单 123 取消掉"
  → AiServiceImpl.ask()
    ├─ 1. 查用户上下文 + 数据
    ├─ 2. 调 DeepSeek（含 tools 定义）
    │     AI 返回: {tool_calls: [{name:"cancelOrder", args:{orderId:123}}]}
    ├─ 3. ToolExecutor.execute(toolName, args, memberId)
    │     ├─ 需要确认? → 返回 pending_confirm
    │     └─ 不需要? → 直接执行 → 返回结果
    ├─ 4. 把结果回传给 DeepSeek（tool_result）
    ├─ 5. DeepSeek 生成自然语言回答
    └─ 6. 返回用户
```

### 2.2 确认流程（涉及钱的场景）

```
用户 → "帮我把订单 123 取消掉"
  → AI 识别需要 cancelOrder
  → ToolExecutor 检测到该 tool "需要确认"
  → 返回特殊响应: {type:"confirm", tool:"cancelOrder", args:{orderId:123}, summary:"订单 123 金额 ¥100，取消后将自动退款"}
  → 前端弹确认框: "订单 123 金额 ¥100，确定要取消吗？"
  → 用户点"确定"
  → 前端传 {confirm: true, sessionId}
  → AiServiceImpl 执行 tool → 调 DeepSeek → 返回结果
```

---

## 3. 详细设计

### 3.1 AiServiceImpl 改造

现有 `ask()` 方法增加 function calling 分支：

```java
public String ask(String question, Long memberId, String sessionId) {
    // ... 现有逻辑：查用户上下文 + 数据 ...
    
    if (检测到 pending confirm) {
        // 执行已确认的 tool
        Object result = toolExecutor.execute(confirmRequest);
        messages.add(buildToolResultMessage(confirmRequest.getToolName(), result));
    } else {
        // 首次提问，调 DeepSeek 含 tools
        DeepSeekResponse response = callDeepSeekWithTools(messages, getToolDefinitions());
        
        if (response.hasToolCalls()) {
            ToolCall toolCall = response.getToolCalls().get(0);
            
            if (toolExecutor.needsConfirm(toolCall.getName())) {
                // 需要用户确认 → 挂起会话，返回确认信息
                savePendingConfirm(sessionId, toolCall);
                return formatConfirmResponse(toolCall); // 前端显示确认框
            } else {
                // 直接执行
                Object result = toolExecutor.execute(toolCall.getName(), toolCall.getArgs(), memberId);
                messages.add(buildToolResultMessage(toolCall.getName(), result));
                // 再次调 DeepSeek 生成最终回答
                String finalReply = callDeepSeek(messages);
                saveHistory(sessionId, messages);
                return finalReply;
            }
        }
    }
}
```

### 3.2 ToolExecutor

```java
@Component
public class ToolExecutor {
    
    public boolean needsConfirm(String toolName) {
        return "cancelOrder".equals(toolName) || "applyRefund".equals(toolName);
    }
    
    public Object execute(String toolName, Map<String, Object> args, Long memberId) {
        switch (toolName) {
            case "cancelOrder":
                return cancelOrder(Long.valueOf(args.get("orderId").toString()), memberId);
            case "applyRefund":
                return applyRefund(args, memberId);
            case "cancelVisit":
                return cancelVisit(Long.valueOf(args.get("visitId").toString()), memberId);
            case "resubmitLeave":
                return resubmitLeave(args, memberId);
            default:
                throw new IllegalArgumentException("未知工具: " + toolName);
        }
    }
}
```

### 3.3 DeepSeek Tools 定义

```json
[
  {
    "type": "function",
    "function": {
      "name": "cancelOrder",
      "description": "取消护理服务订单。如果订单已支付，会自动发起退款。取消前需要用户确认。",
      "parameters": {
        "type": "object",
        "properties": {
          "orderId": {"type": "number", "description": "订单ID"}
        },
        "required": ["orderId"]
      }
    }
  },
  {
    "type": "function",
    "function": {
      "name": "applyRefund",
      "description": "为已支付的护理服务订单申请退款。需要用户确认。",
      "parameters": {
        "type": "object",
        "properties": {
          "orderId": {"type": "number", "description": "订单ID"},
          "reason": {"type": "string", "description": "退款原因"}
        },
        "required": ["orderId", "reason"]
      }
    }
  },
  {
    "type": "function",
    "function": {
      "name": "cancelVisit",
      "description": "取消探访预约或参观预约。不涉及费用，无需确认。",
      "parameters": {
        "type": "object",
        "properties": {
          "visitId": {"type": "number", "description": "预约ID"}
        },
        "required": ["visitId"]
      }
    }
  },
  {
    "type": "function",
    "function": {
      "name": "resubmitLeave",
      "description": "将已驳回的请假申请重新提交。",
      "parameters": {
        "type": "object",
        "properties": {
          "leaveId": {"type": "number", "description": "被驳回的请假单ID"}
        },
        "required": ["leaveId"]
      }
    }
  }
]
```

### 3.4 确认机制

**Redis 挂起确认状态：**
```
Key: ai:pending_confirm:{sessionId}
Value: {
  toolName: "cancelOrder",
  args: {"orderId": 123},
  createTime: 1687878000000
}
TTL: 120 秒
```

**前端交互：**
1. AI 返回 `{type:"confirm", summary:"订单 123 金额 ¥100，确定取消吗？"}`
2. 前端在聊天中显示确认框（"确定" / "取消"）
3. 用户点"确定" → 调用 `/wxLogin/ai/confirm` → 后端执行
4. 用户点"取消" → 调用 `/wxLogin/ai/cancel` → 清除 Redis

### 3.5 新增 API

```java
// WxLoginController

@PostMapping("/ai/confirm")
public AjaxResult aiConfirm(@RequestBody Map<String, String> body) {
    String sessionId = body.get("sessionId");
    String reply = aiService.executeConfirmed(sessionId);
    return AjaxResult.success(reply);
}

@PostMapping("/ai/cancel")
public AjaxResult aiCancel(@RequestBody Map<String, String> body) {
    String sessionId = body.get("sessionId");
    aiService.clearPendingConfirm(sessionId);
    return AjaxResult.success("已取消操作");
}
```

AiService 新增：
```java
// 执行已确认的工具
String executeConfirmed(String sessionId);

// 清除挂起的确认
void clearPendingConfirm(String sessionId);
```

### 3.6 权限控制

- 所有 tool 执行前检查：操作的数据是否属于当前用户绑定的老人
- `cancelOrder`：先查 `service_order` 的 `elder_id` → 再比对当前用户的老人列表
- 敏感信息（退款金额）由后端从数据库查询，不接受 AI 参数中的金额值

---

## 4. 文件清单

### 修改
```
lcyl-admin/.../AiServiceImpl.java           # 增加 function calling 分支
lcyl-admin/.../AiService.java              # 增加 executeConfirmed/clearPendingConfirm
```

### 新增
```
lcyl-admin/.../ToolExecutor.java           # 工具执行引擎
lcyl-code/.../WxLoginController.java       # /ai/confirm + /ai/cancel 端点
```

### 前端新增/修改
```
lcyl-wx/my/pages/aiChat/aiChat.js          # 确认框处理逻辑
lcyl-wx/my/pages/aiChat/aiChat.wxml        # 确认框 UI
```

---

## 5. 边界情况

| 场景 | 处理方式 |
|------|---------|
| 用户问"帮我的订单取消掉"但未指定哪个 | AI 反问"请问是哪个订单？" |
| 取消一个已取消/已退款的订单 | 后端返回错误，AI 用自然语言告知 |
| 确认超时（>120 秒）| 后端返回"确认已超时，请重新操作" |
| 试图操作非自己绑定的老人 | 执行前检查 elder_id 归属，拒绝 |
| DeepSeek 生成的参数格式不对 | 后端参数校验，返回具体错误信息 |
| 用户确认后又想反悔 | 告知"操作已完成，如需要退款请联系管理员" |
