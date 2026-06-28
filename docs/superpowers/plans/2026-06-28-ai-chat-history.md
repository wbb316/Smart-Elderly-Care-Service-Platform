# AI 聊天历史恢复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 退出 AI 聊天页面再进入时自动恢复历史对话，历史最多保留 30 轮。

**Architecture:** 后端从 Redis 读取已有会话消息返回前端，前端 `onLoad` 时调用。

---

### Task 1: 后端 AiService 加 getHistory 方法

**Files:**
- Modify: `lcyl-common/.../AiService.java`
- Modify: `lcyl-admin/.../AiServiceImpl.java`

AiService 接口新增：
```java
List<Map<String, Object>> getHistoryMessages(String sessionId);
```

AiServiceImpl 实现：
```java
@Override
public List<Map<String, Object>> getHistoryMessages(String sessionId) {
    List<Map<String, Object>> messages = getHistory(sessionId);
    // 去掉 system prompt，只返回 user/assistant 消息
    List<Map<String, Object>> result = new ArrayList<>();
    for (Map<String, Object> msg : messages) {
        String role = (String) msg.get("role");
        if ("user".equals(role) || "assistant".equals(role)) {
            Map<String, Object> clean = new HashMap<>();
            clean.put("role", role);
            clean.put("content", msg.get("content"));
            result.add(clean);
        }
    }
    return result;
}
```

同时修改 `saveHistory`，裁剪逻辑改为保留 system + 最多 61 条（1 system + 60 条对话 = 30 轮）：

```java
private void saveHistory(String sessionId, List<Map<String, Object>> msgs) {
    if (msgs.size() > 61) {
        List<Map<String, Object>> system = msgs.subList(0, 1);
        List<Map<String, Object>> recent = msgs.subList(msgs.size() - 60, msgs.size());
        List<Map<String, Object>> pruned = new ArrayList<>(system);
        pruned.addAll(recent);
        redisCache.setCacheObject(SESSION_PREFIX + sessionId, pruned, SESSION_TTL, TimeUnit.SECONDS);
    } else {
        redisCache.setCacheObject(SESSION_PREFIX + sessionId, msgs, SESSION_TTL, TimeUnit.SECONDS);
    }
}
```

### Task 2: 后端 WxLoginController 加 /ai/history 端点

**Files:**
- Modify: `lcyl-code/.../WxLoginController.java`

```java
@GetMapping("/ai/history")
public AjaxResult getHistory(@RequestParam String sessionId) {
    if (com.lcyl.common.utils.StringUtils.isEmpty(sessionId)) {
        return AjaxResult.success(new ArrayList<>());
    }
    return AjaxResult.success(aiService.getHistoryMessages(sessionId));
}
```

### Task 3: 前端 onLoad 恢复历史

**Files:**
- Modify: `lcyl-wx/my/pages/aiChat/aiChat.js`

`onLoad` 改为：

```javascript
onLoad() {
    const memberId = wx.getStorageSync('memberId') || '';
    const today = new Date().toISOString().slice(0, 10).replace(/-/g, '');
    const sessionId = `member_${memberId}_${today}`;
    this.setData({ sessionId });

    // 恢复历史消息
    verifyToken().then(() => {
        request({
            url: '/wxLogin/ai/history',
            method: 'GET',
            data: { sessionId }
        }).then((res) => {
            if (res.data && res.data.code === 200 && Array.isArray(res.data.data) && res.data.data.length > 0) {
                this.setData({ messages: res.data.data });
            }
        });
    }).catch(() => {});
}
```
