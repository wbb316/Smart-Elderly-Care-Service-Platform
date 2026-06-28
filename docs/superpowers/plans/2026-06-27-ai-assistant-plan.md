# AI 智能助手 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在小程序"我的"页面中增加 AI 智能助手入口，家属可通过自然语言查询账单/订单/合同/请假等信息。

**Architecture:** 微信小程序聊天页面 → `POST /wxLogin/ai/ask` → `AiService`（查询数据库 + 组装上下文）→ 调用 DeepSeek API → 返回自然语言回答。会话历史存 Redis，TTL 30 分钟。

**Tech Stack:** Spring Boot + HttpClient + DeepSeek API (OpenAI 兼容格式) + Redis + 微信小程序原生开发

## Global Constraints

- DeepSeek API Key 通过环境变量 `DEEPSEEK_API_KEY` 注入
- 每轮对话只查当前登录用户绑定的老人的数据
- Redis 会话 TTL 为 1800 秒，最多存 6 轮对话
- DeepSeek model 使用 `deepseek-chat`，temperature 0.3，max_tokens 1000
- 所有异常必须降级返回中文提示，不透传原始异常

---

### Task 1: 后端 DeepSeek 配置 + AiService 实现

**Files:**
- Modify: `lcyl-java/lcyl/lcyl-admin/src/main/resources/application.yml`
- Create: `lcyl-java/lcyl/lcyl-admin/src/main/java/com/lcyl/web/service/AiService.java`
- Create: `lcyl-java/lcyl/lcyl-admin/src/main/java/com/lcyl/web/service/impl/AiServiceImpl.java`

**Interfaces:**
- Produces: `AiService.ask(String question, Long memberId, String sessionId) → String`

- [ ] **Step 1: 在 application.yml 添加 DeepSeek 配置**

在 `D:\WBB_JAVA\team\lcyl-java\lcyl\lcyl-admin\src/main/resources/application.yml` 末尾添加：

```yaml
# AI
deepseek:
  api-key: ${DEEPSEEK_API_KEY}
  model: deepseek-chat
  temperature: 0.3
  max-tokens: 1000
```

- [ ] **Step 2: 创建 AiService 接口**

创建文件 `D:\WBB_JAVA\team\lcyl-java\lcyl\lcyl-admin\src\main\java\com\lcyl\web\service\AiService.java`：

```java
package com.lcyl.web.service;

public interface AiService {
    String ask(String question, Long memberId, String sessionId);
}
```

- [ ] **Step 3: 创建 AiServiceImpl（含完整查询实现）**

创建文件 `D:\WBB_JAVA\team\lcyl-java\lcyl\lcyl-admin\src\main\java\com\lcyl\web\service\impl\AiServiceImpl.java`：

```java
package com.lcyl.web.service.impl;

import com.lcyl.code.domain.Bill;
import com.lcyl.code.domain.ElderLeave;
import com.lcyl.code.domain.Member;
import com.lcyl.code.domain.ServiceOrder;
import com.lcyl.code.mapper.BillMapper;
import com.lcyl.code.mapper.ElderLeaveMapper;
import com.lcyl.code.mapper.MemberMapper;
import com.lcyl.code.mapper.ServiceOrderMapper;
import com.lcyl.common.core.redis.RedisCache;
import com.lcyl.common.utils.DateUtils;
import com.lcyl.system.domain.Contract;
import com.lcyl.system.domain.Elder;
import com.lcyl.system.mapper.ContractMapper;
import com.lcyl.system.mapper.ElderMapper;
import com.lcyl.web.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    @Autowired private RedisCache redisCache;
    @Autowired private MemberMapper memberMapper;
    @Autowired private ElderMapper elderMapper;
    @Autowired private BillMapper billMapper;
    @Autowired private ServiceOrderMapper serviceOrderMapper;
    @Autowired private ContractMapper contractMapper;
    @Autowired private ElderLeaveMapper elderLeaveMapper;

    @Value("${deepseek.api-key}")       private String apiKey;
    @Value("${deepseek.model}")         private String model;
    @Value("${deepseek.temperature}")   private double temperature;
    @Value("${deepseek.max-tokens}")    private int maxTokens;

    private static final String DEEPSEEK_URL = "https://api.deepseek.com/chat/completions";
    private static final String SESSION_PREFIX = "ai:session:";
    private static final int SESSION_TTL = 1800;

    @Override
    public String ask(String question, Long memberId, String sessionId) {
        try {
            // 1. 用户上下文
            String userContext = getUserContext(memberId);
            if (userContext == null) {
                return "您还没有绑定老人，请先在「家人」页面添加哦 😊";
            }

            // 2. 查数据
            String billData = queryBills(memberId, question);
            String orderData = queryOrders(memberId, question);
            String contractData = queryContracts(memberId);
            String leaveData = queryLeaves(memberId, question);

            // 3. 组装 prompt
            String systemPrompt = buildSystemPrompt(userContext, billData, orderData, contractData, leaveData);

            // 4. 获取/初始化消息列表
            List<Map<String, String>> messages = getHistory(sessionId);
            if (messages.isEmpty()) {
                Map<String, String> sysMsg = new HashMap<>();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
                messages.add(sysMsg);
            }

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", question);
            messages.add(userMsg);

            // 5. 调 DeepSeek
            String reply = callDeepSeek(messages);

            // 6. 存历史
            Map<String, String> asstMsg = new HashMap<>();
            asstMsg.put("role", "assistant");
            asstMsg.put("content", reply);
            messages.add(asstMsg);
            saveHistory(sessionId, messages);

            return reply;
        } catch (Exception e) {
            log.error("AI 助手请求失败", e);
            return "抱歉，我现在有点忙不过来，请稍后再试试吧 😊";
        }
    }

    // ==================== 数据查询 ====================

    private String getUserContext(Long memberId) {
        Member member = memberMapper.selectMemberById(memberId);
        if (member == null) return null;

        Elder elderParam = new Elder();
        elderParam.setCreateBy(String.valueOf(memberId));
        List<Elder> elders = elderMapper.selectElderByMember(memberId);
        if (elders == null || elders.isEmpty()) return null;

        String elderInfo = elders.stream()
                .map(e -> e.getName() + (e.getBedNumber() != null ? "（床位 " + e.getBedNumber() + "）" : ""))
                .collect(Collectors.joining("、"));
        return "用户：" + (member.getName() != null ? member.getName() : "") +
               "（手机：" + (member.getPhone() != null ? member.getPhone() : "") + "）" +
               "\n绑定老人：" + elderInfo;
    }

    private String queryBills(Long memberId, String question) {
        // 通过 elder_member 关联查名下老人的账单
        List<Elder> elders = elderMapper.selectElderByMember(memberId);
        if (elders == null || elders.isEmpty()) return "暂无";

        StringBuilder sb = new StringBuilder();
        for (Elder elder : elders) {
            Bill param = new Bill();
            param.setElderId(elder.getId());
            // 判断是否问"本月"
            if (question.contains("本月") || question.contains("这个月")) {
                param.setBillMonth(new SimpleDateFormat("yyyy-MM").format(new Date()));
            } else if (question.contains("上月") || question.contains("上个月")) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, -1);
                param.setBillMonth(new SimpleDateFormat("yyyy-MM").format(cal.getTime()));
            }
            List<Bill> bills = billMapper.selectBillList(param);
            if (bills != null && !bills.isEmpty()) {
                for (Bill b : bills) {
                    sb.append("老人：").append(elder.getName())
                      .append("，账单号：").append(b.getBillNo())
                      .append("，月份：").append(b.getBillMonth())
                      .append("，金额：¥").append(b.getPayableAmount())
                      .append("，状态：").append(formatBillStatus(b.getTradeStatus()))
                      .append("；\n");
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : "暂无账单数据";
    }

    private String queryOrders(Long memberId, String question) {
        List<Elder> elders = elderMapper.selectElderByMember(memberId);
        if (elders == null || elders.isEmpty()) return "暂无";

        StringBuilder sb = new StringBuilder();
        for (Elder elder : elders) {
            ServiceOrder param = new ServiceOrder();
            param.setElderId(elder.getId());
            List<ServiceOrder> orders = serviceOrderMapper.selectServiceOrderList(param);
            if (orders != null && !orders.isEmpty()) {
                for (ServiceOrder o : orders) {
                    sb.append("老人：").append(elder.getName())
                      .append("，订单号：").append(o.getOrderNo())
                      .append("，项目：").append(o.getProjectName())
                      .append("，金额：¥").append(o.getOrderAmount())
                      .append("，状态：").append(formatOrderStatus(o.getOrderStatus()))
                      .append("；\n");
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : "暂无订单数据";
    }

    private String queryContracts(Long memberId) {
        List<Elder> elders = elderMapper.selectElderByMember(memberId);
        if (elders == null || elders.isEmpty()) return "暂无";

        StringBuilder sb = new StringBuilder();
        for (Elder elder : elders) {
            Contract param = new Contract();
            param.setElderId(elder.getId());
            List<Contract> contracts = contractMapper.selectContractList(param);
            if (contracts != null && !contracts.isEmpty()) {
                for (Contract c : contracts) {
                    sb.append("老人：").append(elder.getName())
                      .append("，合同编号：").append(c.getContractNo())
                      .append("，类型：").append(c.getName())
                      .append("，状态：").append(formatContractStatus(c.getStatus()))
                      .append("，有效期：").append(DateUtils.parseDateToStr("yyyy-MM-dd", c.getStartTime()))
                      .append(" 至 ").append(DateUtils.parseDateToStr("yyyy-MM-dd", c.getEndTime()))
                      .append("；\n");
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : "暂无合同数据";
    }

    private String queryLeaves(Long memberId, String question) {
        List<Elder> elders = elderMapper.selectElderByMember(memberId);
        if (elders == null || elders.isEmpty()) return "暂无";

        StringBuilder sb = new StringBuilder();
        for (Elder elder : elders) {
            ElderLeave param = new ElderLeave();
            param.setElderId(elder.getId());
            List<ElderLeave> leaves = elderLeaveMapper.selectElderLeaveList(param);
            if (leaves != null && !leaves.isEmpty()) {
                for (ElderLeave l : leaves) {
                    sb.append("老人：").append(elder.getName())
                      .append("，申请单号：").append(l.getOrderNo())
                      .append("，开始时间：").append(DateUtils.parseDateToStr("yyyy-MM-dd", l.getLeaveStartTime()))
                      .append("，预计返回：").append(DateUtils.parseDateToStr("yyyy-MM-dd", l.getPlannedReturnTime()))
                      .append("，状态：").append(formatLeaveStatus(l.getStatus()))
                      .append("；\n");
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : "暂无请假数据";
    }

    // ==================== 格式化 ====================

    private String formatBillStatus(String status) {
        if ("0".equals(status)) return "待支付";
        if ("1".equals(status)) return "已支付";
        if ("2".equals(status)) return "已关闭";
        if ("3".equals(status)) return "已退款";
        return status;
    }

    private String formatOrderStatus(String status) {
        if ("0".equals(status)) return "待支付";
        if ("1".equals(status)) return "待执行";
        if ("2".equals(status)) return "执行中";
        if ("3".equals(status)) return "已完成";
        if ("4".equals(status)) return "已退款";
        if ("5".equals(status)) return "已关闭";
        return status;
    }

    private String formatContractStatus(Long status) {
        if (status == null) return "未知";
        if (status == 0) return "未生效";
        if (status == 1) return "生效中";
        if (status == 2) return "已过期";
        if (status == 3) return "已作废";
        return String.valueOf(status);
    }

    private String formatLeaveStatus(String status) {
        switch (status) {
            case "draft": return "草稿";
            case "approving": return "审批中";
            case "approved": return "已批准";
            case "rejected": return "已驳回";
            case "revoked": return "已撤销";
            case "leaving": return "离院中";
            case "returned": return "已销假";
            case "timeout": return "超时未归";
            default: return status;
        }
    }

    // ==================== Prompt 构建 ====================

    private String buildSystemPrompt(String userContext, String bills, String orders,
                                      String contracts, String leaves) {
        return "你是乐康养老院的智能助手，名字叫"小乐"。\n\n"
                + "当前用户的信息：\n" + (userContext != null ? userContext : "未知")
                + "\n\n当前可查询的数据："
                + "\n【账单信息】" + (bills != null ? bills : "暂无")
                + "\n【订单信息】" + (orders != null ? orders : "暂无")
                + "\n【合同信息】" + (contracts != null ? contracts : "暂无")
                + "\n【请假信息】" + (leaves != null ? leaves : "暂无")
                + "\n\n回答要求："
                + "\n1. 优先从上面提供的真实数据中回答"
                + "\n2. 如果用户问的不在可查询范围内，礼貌告知暂时无法回答"
                + "\n3. 使用温暖、亲切的语气"
                + "\n4. 涉及金额时注明明细";
    }

    // ==================== DeepSeek API 调用 ====================

    private String callDeepSeek(List<Map<String, String>> messages) throws Exception {
        List<Map<String, String>> recent = new ArrayList<>();
        if (!messages.isEmpty() && "system".equals(messages.get(0).get("role"))) {
            recent.add(messages.get(0));
        }
        int start = Math.max(recent.size(), messages.size() - 4);
        for (int i = start; i < messages.size(); i++) {
            recent.add(messages.get(i));
        }

        String body = buildRequestBody(recent);
        URL url = new URL(DEEPSEEK_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            String err = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            log.error("DeepSeek API error: {} {}", code, err);
            return "抱歉，我现在有点忙不过来，请稍后再试试吧 😊";
        }

        String resp = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        conn.disconnect();
        return extractReply(resp);
    }

    private String buildRequestBody(List<Map<String, String>> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(model).append("\",");
        sb.append("\"temperature\":").append(temperature).append(",");
        sb.append("\"max_tokens\":").append(maxTokens).append(",");
        sb.append("\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            Map<String, String> m = messages.get(i);
            sb.append("{\"role\":\"").append(m.get("role"))
              .append("\",\"content\":\"").append(escapeJson(m.get("content"))).append("\"}");
            if (i < messages.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String extractReply(String json) {
        String key = "\"content\":\"";
        int start = json.indexOf(key);
        if (start < 0) return null;
        start += key.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"") : null;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    // ==================== 会话管理 ====================

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> getHistory(String sessionId) {
        Object o = redisCache.getCacheObject(SESSION_PREFIX + sessionId);
        return o instanceof List ? (List<Map<String, String>>) o : new ArrayList<>();
    }

    private void saveHistory(String sessionId, List<Map<String, String>> msgs) {
        redisCache.setCacheObject(SESSION_PREFIX + sessionId, msgs, SESSION_TTL, TimeUnit.SECONDS);
    }
}
```

    private String buildRequestBody(List<Map<String, String>> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(model).append("\",");
        sb.append("\"temperature\":").append(temperature).append(",");
        sb.append("\"max_tokens\":").append(maxTokens).append(",");
        sb.append("\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            Map<String, String> msg = messages.get(i);
            sb.append("{\"role\":\"").append(msg.get("role")).append("\",");
            sb.append("\"content\":\"").append(escapeJson(msg.get("content"))).append("\"}");
            if (i < messages.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String extractReply(String json) {
        // 简单 JSON 解析，不依赖第三方库
        String marker = "\"content\":\"";
        int start = json.indexOf(marker);
        if (start < 0) return null;
        start += marker.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"");
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> getHistory(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        Object cached = redisCache.getCacheObject(key);
        if (cached instanceof List) {
            return (List<Map<String, String>>) cached;
        }
        return new ArrayList<>();
    }

    private void saveHistory(String sessionId, List<Map<String, String>> messages) {
        String key = SESSION_PREFIX + sessionId;
        redisCache.setCacheObject(key, messages, SESSION_TTL, TimeUnit.SECONDS);
    }
}
```

注：`getUserContext`、`queryBills` 等方法需要注入对应的 Mapper/Service，具体注入哪个需根据项目实际表结构来，这里先留 TODO 实现。

- [ ] **Step 4: 编译验证**

```bash
cd D:/WBB_JAVA/team/lcyl-java/lcyl
mvn compile -q 2>&1 | tail -10
```
Expected: 无错误

- [ ] **Step 5: 提交**

```bash
cd D:/WBB_JAVA/team
git add lcyl-java/lcyl/lcyl-admin/src/main/resources/application.yml
git add lcyl-java/lcyl/lcyl-admin/src/main/java/com/lcyl/web/service/
git commit -m "feat: 添加 AiService 和 DeepSeek API 集成"
```

---

### Task 2: 后端 WxLoginController 添加 /ai/ask 端点

**Files:**
- Modify: `lcyl-java/lcyl/lcyl-code/src/main/java/com/lcyl/code/controller/WxLoginController.java`

**Interfaces:**
- Consumes: `AiService.ask(String, Long, String) → String` (from Task 1)
- Produces: `POST /wxLogin/ai/ask` endpoint

- [ ] **Step 1: 注入 AiService 并添加端点**

在 `WxLoginController` 中添加：

```java
@Autowired
private AiService aiService;

/**
 * AI 智能助手 - 家属端问答
 */
@PostMapping("/ai/ask")
public AjaxResult aiAsk(@RequestBody Map<String, String> body) {
    String question = body.get("question");
    if (StringUtils.isEmpty(question)) {
        return AjaxResult.error("请输入您的问题");
    }
    Long memberId = UserThreadLocal.getUserId();
    if (memberId == null) {
        return AjaxResult.error("用户未登录");
    }
    String sessionId = body.get("sessionId");
    if (StringUtils.isEmpty(sessionId)) {
        sessionId = "member_" + memberId + "_" + new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
    }
    String reply = aiService.ask(question, memberId, sessionId);
    return AjaxResult.success(reply);
}
```

- [ ] **Step 2: 编译验证**

```bash
cd D:/WBB_JAVA/team/lcyl-java/lcyl
mvn compile -q 2>&1 | tail -10
```
Expected: 无错误

- [ ] **Step 3: 提交**

```bash
cd D:/WBB_JAVA/team
git add lcyl-java/lcyl/lcyl-code/src/main/java/com/lcyl/code/controller/WxLoginController.java
git commit -m "feat: 添加 /wxLogin/ai/ask 接口"
```

---

### Task 3: 小程序 AI 聊天页面

**Files:**
- Create: `lcyl-wx/my/pages/aiChat/aiChat.js`
- Create: `lcyl-wx/my/pages/aiChat/aiChat.json`
- Create: `lcyl-wx/my/pages/aiChat/aiChat.wxml`
- Create: `lcyl-wx/my/pages/aiChat/aiChat.wxss`
- Create: `lcyl-wx/my/pages/aiChat/aiChat.wxss`

- [ ] **Step 1: 创建 aiChat.js**

创建 `D:\WBB_JAVA\team\lcyl-wx\my\pages\aiChat\aiChat.js`：

```javascript
const { request, verifyToken } = require('../../../utils/request');
const app = getApp();

Page({
  data: {
    sessionId: '',
    messages: [
      { role: 'assistant', content: '你好呀！我是小乐 😊\n你可以问我关于账单、订单、合同或请假的问题，比如：\n- "我妈妈这个月花了多少钱"\n- "爸爸请假的申请通过了吗"\n- "帮我看看合同信息"' }
    ],
    inputText: '',
    loading: false
  },

  onLoad() {
    const memberId = wx.getStorageSync('memberId') || '';
    const today = new Date().toISOString().slice(0, 10).replace(/-/g, '');
    this.setData({
      sessionId: `member_${memberId}_${today}`
    });
  },

  onInput(e) {
    this.setData({ inputText: e.detail.value });
  },

  sendMessage() {
    const { inputText, loading } = this.data;
    if (!inputText.trim() || loading) return;

    const userMsg = { role: 'user', content: inputText.trim() };
    const messages = [...this.data.messages, userMsg];
    this.setData({
      messages,
      inputText: '',
      loading: true
    });

    this.scrollToBottom();

    verifyToken().then(() => {
      request({
        url: '/wxLogin/ai/ask',
        method: 'POST',
        data: {
          question: userMsg.content,
          sessionId: this.data.sessionId
        }
      }).then((res) => {
        if (res.data && res.data.code === 200) {
          const reply = res.data.data || '抱歉，我没有理解您的意思 😅';
          this.setData({
            messages: [...this.data.messages, { role: 'assistant', content: reply }]
          });
        } else {
          this.setData({
            messages: [...this.data.messages, { role: 'assistant', content: res.data.msg || '服务出错了，请稍后再试' }]
          });
        }
      }).catch(() => {
        this.setData({
          messages: [...this.data.messages, { role: 'assistant', content: '网络异常，请稍后重试' }]
        });
      }).finally(() => {
        this.setData({ loading: false });
        this.scrollToBottom();
      });
    }).catch(() => {});
  },

  scrollToBottom() {
    setTimeout(() => {
      wx.pageScrollTo({ scrollTop: 99999 });
    }, 100);
  }
});
```

- [ ] **Step 2: 创建 aiChat.wxml**

创建 `D:\WBB_JAVA\team\lcyl-wx\my\pages\aiChat\aiChat.wxml`：

```xml
<view class="page">
  <scroll-view class="msg-list" scroll-y scroll-with-animation upper-threshold="50" lower-threshold="50">
    <view wx:for="{{messages}}" wx:key="index" class="msg-item {{item.role === 'user' ? 'msg-user' : 'msg-ai'}}">
      <view class="msg-content">{{item.content}}</view>
    </view>

    <view wx:if="{{loading}}" class="msg-item msg-ai">
      <view class="msg-content typing">
        <text class="dot">.</text><text class="dot">.</text><text class="dot">.</text>
      </view>
    </view>
  </scroll-view>

  <view class="input-bar">
    <input class="input" placeholder="输入您的问题..." value="{{inputText}}" bindinput="onInput" disabled="{{loading}}" />
    <button class="send-btn {{loading || !inputText.trim() ? 'disabled' : ''}}" bindtap="sendMessage">发送</button>
  </view>
</view>
```

- [ ] **Step 3: 创建 aiChat.wxss**

创建 `D:\WBB_JAVA\team\lcyl-wx\my\pages\aiChat\aiChat.wxss`：

```css
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f5f5f5;
}

.msg-list {
  flex: 1;
  padding: 30rpx;
  overflow-y: auto;
}

.msg-item {
  margin-bottom: 30rpx;
  display: flex;
}

.msg-user {
  justify-content: flex-end;
}

.msg-ai {
  justify-content: flex-start;
}

.msg-content {
  max-width: 70%;
  padding: 20rpx 30rpx;
  border-radius: 16rpx;
  font-size: 28rpx;
  line-height: 1.6;
  word-break: break-word;
}

.msg-user .msg-content {
  background: #87CEFB;
  color: #fff;
  border-bottom-right-radius: 4rpx;
}

.msg-ai .msg-content {
  background: #fff;
  color: #333;
  border-bottom-left-radius: 4rpx;
}

.typing .dot {
  animation: blink 1.4s infinite;
  font-size: 40rpx;
  font-weight: bold;
  margin-right: 4rpx;
}

.typing .dot:nth-child(2) { animation-delay: 0.2s; }
.typing .dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes blink {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}

.input-bar {
  display: flex;
  align-items: center;
  padding: 20rpx 30rpx;
  background: #fff;
  border-top: 2rpx solid #eee;
}

.input {
  flex: 1;
  height: 72rpx;
  background: #f5f5f5;
  border-radius: 36rpx;
  padding: 0 30rpx;
  font-size: 28rpx;
}

.send-btn {
  margin-left: 20rpx;
  width: 120rpx;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  background: #87CEFB;
  color: #fff;
  font-size: 28rpx;
  border-radius: 36rpx;
  padding: 0;
}

.send-btn.disabled {
  opacity: 0.5;
}
```

- [ ] **Step 4: 创建 aiChat.json**

创建 `D:\WBB_JAVA\team\lcyl-wx\my\pages\aiChat\aiChat.json`：

```json
{
  "navigationBarTitleText": "智能助手"
}
```

- [ ] **Step 5: 提交**

```bash
cd D:/WBB_JAVA/team
git add lcyl-wx/my/pages/aiChat/
git commit -m "feat: 添加 AI 聊天页面"
```

---

### Task 4: 小程序入口按钮 + 页面注册

**Files:**
- Modify: `lcyl-wx/pages/my/my.wxml`
- Modify: `lcyl-wx/pages/my/my.js`
- Modify: `lcyl-wx/app.json`

- [ ] **Step 1: 在 app.json 注册 aiChat 页面**

在 `my` 子包中添加：

```json
{
  "root": "my",
  "pages": [
    ...
    "pages/aiChat/aiChat"
  ]
}
```

- [ ] **Step 2: 在"我的"页面加 AI 入口**

在 `my.wxml` 的菜单列表中添加（放在"我的预约"前面）：

```xml
<navigator url="/my/pages/aiChat/aiChat" hover-class="none">
  <view class="menu-item">
    <text class="menu-text">🤖 智能助手</text>
    <text class="menu-arrow">›</text>
  </view>
</navigator>
```

- [ ] **Step 3: 提交**

```bash
cd D:/WBB_JAVA/team
git add lcyl-wx/pages/my/my.wxml lcyl-wx/pages/my/my.js lcyl-wx/app.json
git commit -m "feat: 我的页面添加 AI 智能助手入口"
```
