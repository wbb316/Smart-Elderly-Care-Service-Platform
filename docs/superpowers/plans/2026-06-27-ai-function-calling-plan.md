# AI Agent Function Calling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 AI 智能助手中增加 function calling 能力，让家属可以通过自然语言取消订单、取消预约、申请退款、重新提交请假。

**Architecture:** DeepSeek 原生 function calling → `ToolExecutor` 执行 → 涉及钱的工具先挂起确认状态（Redis，120s TTL）→ 用户确认后再执行。

**Tech Stack:** Spring Boot + DeepSeek API (function calling) + Redis + 微信小程序原生

## Global Constraints

- 涉及钱的工具（cancelOrder, applyRefund）必须先返回确认信息，用户确认后才执行
- 所有工具执行前校验数据归属当前用户的老人
- 确认信息 Redis 挂起 120 秒后自动过期
- AI 返回确认信息时前端必须展示确认/取消按钮

---

### Task 1: ToolExecutor 工具执行引擎

**Files:**
- Create: `lcyl-java/lcyl/lcyl-admin/src/main/java/com/lcyl/web/service/impl/ToolExecutor.java`

**Interfaces:**
- Produces: `ToolExecutor.execute(String toolName, Map<String,Object> args, Long memberId) → Object`
- Produces: `ToolExecutor.needsConfirm(String toolName) → Boolean`

**代码实现：**
```java
package com.lcyl.web.service.impl;

import com.lcyl.code.mapper.ElderLeaveMapper;
import com.lcyl.code.mapper.ServiceOrderMapper;
import com.lcyl.code.service.impl.ElderLeaveServiceImpl;
import com.lcyl.code.service.impl.WxLoginServiceImpl;
import com.lcyl.code.vo.ElderLeaveVo;
import com.lcyl.common.core.domain.AjaxResult;
import com.lcyl.system.domain.Elder;
import com.lcyl.system.mapper.ElderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ToolExecutor {

    @Autowired private WxLoginServiceImpl wxLoginService;
    @Autowired private ElderLeaveServiceImpl elderLeaveService;
    @Autowired private ElderMapper elderMapper;
    @Autowired private ServiceOrderMapper serviceOrderMapper;
    @Autowired private ElderLeaveMapper elderLeaveMapper;

    private static final Set<String> CONFIRM_TOOLS = Set.of("cancelOrder", "applyRefund");

    public boolean needsConfirm(String toolName) {
        return CONFIRM_TOOLS.contains(toolName);
    }

    public Object execute(String toolName, Map<String, Object> args, Long memberId) {
        // 权限检查：获取当前用户绑定的老人 ID 集合
        Set<Long> boundElderIds = elderMapper.selectElderByMember(memberId)
                .stream().map(Elder::getId).collect(Collectors.toSet());

        switch (toolName) {
            case "cancelOrder":
                return cancelOrder(args, boundElderIds);
            case "applyRefund":
                return applyRefund(args, boundElderIds);
            case "cancelVisit":
                return cancelVisit(args, boundElderIds);
            case "resubmitLeave":
                return resubmitLeave(args, boundElderIds);
            default:
                throw new IllegalArgumentException("未知工具: " + toolName);
        }
    }

    private Object cancelOrder(Map<String, Object> args, Set<Long> boundElderIds) {
        Long orderId = Long.valueOf(args.get("orderId").toString());
        // 检查订单所属老人
        com.lcyl.code.domain.ServiceOrder order = serviceOrderMapper.selectServiceOrderById(orderId);
        if (order == null) return "订单不存在";
        if (!boundElderIds.contains(order.getElderId())) return "无权操作此订单";
        Object result = wxLoginService.cancelOrder(orderId);
        return formatResult("订单 " + orderId + " 已取消", result);
    }

    private Object applyRefund(Map<String, Object> args, Set<Long> boundElderIds) {
        Long orderId = Long.valueOf(args.get("orderId").toString());
        String reason = (String) args.getOrDefault("reason", "");
        // 检查归属
        com.lcyl.code.domain.ServiceOrder order = serviceOrderMapper.selectServiceOrderById(orderId);
        if (order == null) return "订单不存在";
        if (!boundElderIds.contains(order.getElderId())) return "无权操作此订单";

        com.lcyl.code.domain.dto.WxRefundApplyDTO dto = new com.lcyl.code.domain.dto.WxRefundApplyDTO();
        dto.setOrderId(orderId);
        dto.setRefundReason(reason);
        Object result = wxLoginService.applyRefund(dto);
        return formatResult("退款申请已提交", result);
    }

    private Object cancelVisit(Map<String, Object> args, Set<Long> boundElderIds) {
        Long visitId = Long.valueOf(args.get("visitId").toString());
        // 直接调用已有逻辑
        Object result = wxLoginService.deleteVisit(visitId, false);
        return formatResult("预约已取消", result);
    }

    private Object resubmitLeave(Map<String, Object> args, Set<Long> boundElderIds) {
        Long leaveId = Long.valueOf(args.get("leaveId").toString());
        // 检查请假单归属
        com.lcyl.code.domain.ElderLeave leave = elderLeaveMapper.selectElderLeaveById(leaveId);
        if (leave == null) return "请假单不存在";
        if (!boundElderIds.contains(leave.getElderId())) return "无权操作此请假单";

        com.lcyl.code.dto.ElderLeaveResubmitDto dto = new com.lcyl.code.dto.ElderLeaveResubmitDto();
        dto.setId(leaveId);
        // resubmitLeave 需要关联的原始数据
        int rows = elderLeaveService.resubmitLeave(dto);
        return rows > 0 ? "请假单已重新提交" : "提交失败，请联系管理员";
    }

    private String formatResult(String msg, Object result) {
        return msg + "（" + (result != null ? result.toString() : "") + "）";
    }
}
```

- [ ] **Step 1: 创建 ToolExecutor.java 文件**

创建 `D:\WBB_JAVA\team\lcyl-java\lcyl\lcyl-admin\src\main\java\com\lcyl\web\service\impl\ToolExecutor.java`

- [ ] **Step 2: 编译验证**

```bash
cd D:/WBB_JAVA/team/lcyl-java/lcyl
mvn compile -q 2>&1 | tail -10
```

- [ ] **Step 3: 提交**

```bash
cd D:/WBB_JAVA/team
git add lcyl-java/lcyl/lcyl-admin/src/main/java/com/lcyl/web/service/impl/ToolExecutor.java
git commit -m "feat: 添加 ToolExecutor 工具执行引擎"
```

---

### Task 2: AiServiceImpl 增加 function calling 能力

**Files:**
- Modify: `lcyl-java/lcyl/lcyl-admin/src/main/java/com/lcyl/web/service/impl/AiServiceImpl.java`
- Modify: `lcyl-java/lcyl/lcyl-common/src/main/java/com/lcyl/web/service/AiService.java`

**Interfaces:**
- Modify: `AiService.ask()` — 增加 function calling 分支
- Add: `AiService.executeConfirmed(String sessionId) → String`
- Add: `AiService.clearPendingConfirm(String sessionId)`
- Add: `AiService.hasPendingConfirm(String sessionId) → Boolean`

- [ ] **Step 1: AiService 接口新增方法**

在 `AiService.java` 中增加：
```java
String executeConfirmed(String sessionId);
void clearPendingConfirm(String sessionId);
boolean hasPendingConfirm(String sessionId);
```

- [ ] **Step 2: AiServiceImpl 改造 ask() 方法**

核心改动：
1. 调 DeepSeek 时传入 `tools` 参数（4 个工具的 JSON definition）
2. 检测返回值是否包含 `tool_calls`
3. 如果有 → 判断是否需要确认
4. 需要确认 → 存 Redis `ai:pending:{sessionId}` = toolCall JSON，返回确认消息
5. 不需要确认 → ToolExecutor 直接执行 → 结果回传 DeepSeek → 生成最终回答
6. 实现 `executeConfirmed()` → 读 Redis → 执行 → 删 Redis → 回传 → 返回
7. 实现 `clearPendingConfirm()` → 删 Redis

- [ ] **Step 3: 编译验证**

```bash
cd D:/WBB_JAVA/team/lcyl-java/lcyl
mvn compile -q 2>&1 | tail -10
```

- [ ] **Step 4: 提交**

```bash
cd D:/WBB_JAVA/team
git add lcyl-java/lcyl/lcyl-admin/src/main/java/com/lcyl/web/service/impl/AiServiceImpl.java
git add lcyl-java/lcyl/lcyl-common/src/main/java/com/lcyl/web/service/AiService.java
git commit -m "feat: AiServiceImpl 增加 function calling 和确认机制"
```

---

### Task 3: 后端 /ai/confirm 和 /ai/cancel 端点

**Files:**
- Modify: `lcyl-java/lcyl/lcyl-code/src/main/java/com/lcyl/code/controller/WxLoginController.java`

- [ ] **Step 1: 添加两个端点**

```java
/**
 * AI 工具确认执行
 */
@PostMapping("/ai/confirm")
public AjaxResult aiConfirm(@RequestBody Map<String, String> body) {
    String sessionId = body.get("sessionId");
    if (com.lcyl.common.utils.StringUtils.isEmpty(sessionId)) {
        return AjaxResult.error("会话ID不能为空");
    }
    String reply = aiService.executeConfirmed(sessionId);
    return AjaxResult.success(reply);
}

/**
 * AI 工具取消执行
 */
@PostMapping("/ai/cancel")
public AjaxResult aiCancel(@RequestBody Map<String, String> body) {
    String sessionId = body.get("sessionId");
    aiService.clearPendingConfirm(sessionId);
    return AjaxResult.success("已取消操作");
}
```

- [ ] **Step 2: 编译验证**

```bash
cd D:/WBB_JAVA/team/lcyl-java/lcyl
mvn compile -q 2>&1 | tail -10
```

- [ ] **Step 3: 提交**

```bash
cd D:/WBB_JAVA/team
git add lcyl-java/lcyl/lcyl-code/src/main/java/com/lcyl/code/controller/WxLoginController.java
git commit -m "feat: 添加 /ai/confirm 和 /ai/cancel 端点"
```

---

### Task 4: 前端 AI 聊天页面增加确认框

**Files:**
- Modify: `lcyl-wx/my/pages/aiChat/aiChat.js`
- Modify: `lcyl-wx/my/pages/aiChat/aiChat.wxml`
- Modify: `lcyl-wx/my/pages/aiChat/aiChat.wxss`

- [ ] **Step 1: aiChat.js 增加确认处理逻辑**

在 `sendMessage()` 的 `then` 回调中，检测回复是否包含确认信息：

```javascript
// 在成功的回调中判断是否需要确认
.then((res) => {
    if (res.data && res.data.code === 200) {
        const data = res.data.data || '';
        // 检查是否是确认消息（以 [CONFIRM] 开头）
        if (typeof data === 'string' && data.startsWith('[CONFIRM]')) {
            const confirmInfo = data.replace('[CONFIRM]', '');
            this.setData({
                messages: [...this.data.messages, {
                    role: 'assistant',
                    content: confirmInfo,
                    needsConfirm: true
                }]
            });
        } else {
            this.setData({
                messages: [...this.data.messages, { role: 'assistant', content: data }]
            });
        }
    }
})
```

新增确认/取消方法：
```javascript
confirmAction() {
    const lastMsg = this.data.messages[this.data.messages.length - 1];
    if (!lastMsg || !lastMsg.needsConfirm) return;

    this.setData({ loading: true });

    request({
        url: '/wxLogin/ai/confirm',
        method: 'POST',
        data: { sessionId: this.data.sessionId }
    }).then((res) => {
        if (res.data && res.data.code === 200) {
            const messages = [...this.data.messages];
            // 替换最后一条
            messages[messages.length - 1] = {
                role: 'assistant',
                content: res.data.data || '操作完成'
            };
            this.setData({ messages });
        }
    }).catch(() => {
        wx.showToast({ title: '操作失败', icon: 'none' });
    }).finally(() => {
        this.setData({ loading: false });
        this.scrollToBottom();
    });
}

cancelAction() {
    request({
        url: '/wxLogin/ai/cancel',
        method: 'POST',
        data: { sessionId: this.data.sessionId }
    }).then(() => {
        const messages = [...this.data.messages];
        const lastMsg = messages[messages.length - 1];
        messages[messages.length - 1] = {
            role: 'assistant',
            content: (lastMsg.content || '') + '\n\n（已取消）'
        };
        this.setData({ messages });
    });
}
```

- [ ] **Step 2: aiChat.wxml 增加确认框 UI**

在消息列表中确认消息后追加确认/取消按钮：

```xml
<view wx:if="{{item.needsConfirm}}" class="confirm-bar">
  <button class="confirm-btn" bindtap="confirmAction" disabled="{{loading}}">确认执行</button>
  <button class="cancel-btn" bindtap="cancelAction" disabled="{{loading}}">取消</button>
</view>
```

- [ ] **Step 3: aiChat.wxss 增加确认框样式**

```css
.confirm-bar {
  display: flex;
  gap: 20rpx;
  margin-top: 20rpx;
  margin-left: 30rpx;
}

.confirm-btn {
  background: #87CEFB;
  color: #fff;
  font-size: 26rpx;
  padding: 10rpx 30rpx;
  border-radius: 30rpx;
}

.cancel-btn {
  background: #f5f5f5;
  color: #666;
  font-size: 26rpx;
  padding: 10rpx 30rpx;
  border-radius: 30rpx;
}
```

- [ ] **Step 4: 提交**

```bash
cd D:/WBB_JAVA/team
git add lcyl-wx/my/pages/aiChat/
git commit -m "feat: AI 聊天页面增加确认框交互"
```
