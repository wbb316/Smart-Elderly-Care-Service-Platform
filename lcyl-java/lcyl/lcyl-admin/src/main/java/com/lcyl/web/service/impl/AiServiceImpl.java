package com.lcyl.web.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.lcyl.code.domain.Bill;
import com.lcyl.code.domain.Member;
import com.lcyl.code.domain.ServiceOrder;
import com.lcyl.code.dto.ElderLeaveDto;
import com.lcyl.code.mapper.BillMapper;
import com.lcyl.code.mapper.ElderLeaveMapper;
import com.lcyl.code.mapper.MemberMapper;
import com.lcyl.code.mapper.ServiceOrderMapper;
import com.lcyl.code.vo.ElderLeaveVo;
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
    @Autowired private ToolExecutor toolExecutor;

    @Value("${deepseek.api-key}")       private String apiKey;
    @Value("${deepseek.model}")         private String model;
    @Value("${deepseek.temperature}")   private double temperature;
    @Value("${deepseek.max-tokens}")    private int maxTokens;

    private static final String DEEPSEEK_URL = "https://api.deepseek.com/chat/completions";
    private static final String SESSION_PREFIX = "ai:session:";
    private static final String PENDING_PREFIX = "ai:pending:";
    private static final int SESSION_TTL = 1800;
    private static final int PENDING_TTL = 120;

    // ==================== 主入口 ====================

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
            List<Map<String, Object>> messages = getHistory(sessionId);
            if (messages.isEmpty()) {
                Map<String, Object> sysMsg = new HashMap<String, Object>();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
                messages.add(sysMsg);
            }

            Map<String, Object> userMsg = new HashMap<String, Object>();
            userMsg.put("role", "user");
            userMsg.put("content", question);
            messages.add(userMsg);

            // 5. 构建 tools 并调 DeepSeek
            JSONArray tools = buildTools();
            String rawResponse = callDeepSeek(messages, tools);
            JSONObject jsonObj = JSON.parseObject(rawResponse);
            JSONObject messageObj = jsonObj.getJSONArray("choices")
                                           .getJSONObject(0)
                                           .getJSONObject("message");

            // 6. 检查 tool_calls
            JSONArray toolCalls = messageObj.getJSONArray("tool_calls");
            if (toolCalls != null && !toolCalls.isEmpty()) {
                return handleToolCalls(toolCalls, messages, sessionId, memberId);
            }

            // 7. 正常文本回复
            String reply = messageObj.getString("content");
            if (reply == null || reply.trim().isEmpty()) {
                reply = "你好呀！我是小乐，你的智能助手 😊\n有什么可以帮助你的吗？";
            }

            Map<String, Object> asstMsg = new HashMap<String, Object>();
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

    // ==================== 确认执行相关 ====================

    @Override
    public String executeConfirmed(String sessionId) {
        String pendingKey = PENDING_PREFIX + sessionId;
        @SuppressWarnings("unchecked")
        Map<String, Object> pendingInfo = (Map<String, Object>) redisCache.getCacheObject(pendingKey);
        if (pendingInfo == null) {
            return "操作已过期，请重新发起";
        }

        String toolName = (String) pendingInfo.get("toolName");
        @SuppressWarnings("unchecked")
        Map<String, Object> args = (Map<String, Object>) pendingInfo.get("args");
        Object memberIdObj = pendingInfo.get("memberId");
        Long memberId = memberIdObj instanceof Number ? ((Number) memberIdObj).longValue() : null;
        if (memberId == null) {
            return "用户信息异常，请重新操作";
        }

        // 清除 pending 记录
        redisCache.deleteObject(pendingKey);

        try {
            Object result = toolExecutor.execute(toolName, args, memberId);
            return result != null ? result.toString() : "操作完成";
        } catch (Exception e) {
            log.error("执行确认操作失败: toolName={}", toolName, e);
            return "操作执行失败：" + e.getMessage();
        }
    }

    @Override
    public void clearPendingConfirm(String sessionId) {
        redisCache.deleteObject(PENDING_PREFIX + sessionId);
    }

    @Override
    public boolean hasPendingConfirm(String sessionId) {
        return redisCache.getCacheObject(PENDING_PREFIX + sessionId) != null;
    }

    // ==================== Function Calling ====================

    /**
     * 处理 DeepSeek 返回的 tool_calls
     */
    private String handleToolCalls(JSONArray toolCalls, List<Map<String, Object>> messages,
                                    String sessionId, Long memberId) throws Exception {
        JSONObject firstCall = toolCalls.getJSONObject(0);
        String toolCallId = firstCall.getString("id");
        JSONObject function = firstCall.getJSONObject("function");
        if (function == null) {
            return "AI 响应格式异常，请重试";
        }
        String toolName = function.getString("name");
        String argsStr = function.getString("arguments");

        // 解析参数
        Map<String, Object> args = new HashMap<String, Object>();
        if (argsStr != null && !argsStr.isEmpty()) {
            JSONObject argsJson = JSON.parseObject(argsStr);
            if (argsJson != null) {
                for (String key : argsJson.keySet()) {
                    args.put(key, argsJson.get(key));
                }
            }
        }

        // 添加 assistant 消息（含 tool_calls）到历史
        Map<String, Object> asstMsg = new HashMap<String, Object>();
        asstMsg.put("role", "assistant");
        asstMsg.put("tool_calls", toolCalls);
        messages.add(asstMsg);

        if (toolExecutor.needsConfirm(toolName)) {
            // ---- 需要用户确认 ----
            String pendingKey = PENDING_PREFIX + sessionId;
            Map<String, Object> pendingInfo = new HashMap<String, Object>();
            pendingInfo.put("toolName", toolName);
            pendingInfo.put("args", args);
            pendingInfo.put("memberId", memberId);
            pendingInfo.put("toolCallId", toolCallId);
            redisCache.setCacheObject(pendingKey, pendingInfo, PENDING_TTL, TimeUnit.SECONDS);

            // 移除刚添加的 assistant 消息（操作尚未确认，暂不记入历史）
            messages.remove(messages.size() - 1);

            return buildConfirmSummary(toolName, args);
        } else {
            // ---- 直接执行 ----
            Object result = toolExecutor.execute(toolName, args, memberId);
            String resultStr = result != null ? result.toString() : "操作完成";

            // 添加 tool 结果消息
            Map<String, Object> toolMsg = new HashMap<String, Object>();
            toolMsg.put("role", "tool");
            toolMsg.put("tool_call_id", toolCallId);
            toolMsg.put("content", resultStr);
            messages.add(toolMsg);

            // 再次调用 DeepSeek（不带 tools），获取自然语言回复
            String secondRaw = callDeepSeek(messages, null);
            JSONObject secondJson = JSON.parseObject(secondRaw);
            JSONObject secondMsg = secondJson.getJSONArray("choices")
                                             .getJSONObject(0)
                                             .getJSONObject("message");
            String reply = secondMsg.getString("content");
            if (reply == null) reply = "";

            // 添加最终 assistant 回复
            Map<String, Object> finalAsst = new HashMap<String, Object>();
            finalAsst.put("role", "assistant");
            finalAsst.put("content", reply);
            messages.add(finalAsst);
            saveHistory(sessionId, messages);

            return reply;
        }
    }

    /**
     * 构建 4 个工具的 JSON 定义
     */
    private JSONArray buildTools() {
        JSONArray tools = new JSONArray();

        // ---- cancelOrder ----
        {
            JSONObject tool = new JSONObject();
            tool.put("type", "function");
            JSONObject func = new JSONObject();
            func.put("name", "cancelOrder");
            func.put("description", "取消服务订单");
            JSONObject params = new JSONObject();
            params.put("type", "object");
            JSONObject props = new JSONObject();
            JSONObject orderIdProp = new JSONObject();
            orderIdProp.put("type", "integer");
            orderIdProp.put("description", "订单ID");
            props.put("orderId", orderIdProp);
            params.put("properties", props);
            JSONArray required = new JSONArray();
            required.add("orderId");
            params.put("required", required);
            func.put("parameters", params);
            tool.put("function", func);
            tools.add(tool);
        }

        // ---- applyRefund ----
        {
            JSONObject tool = new JSONObject();
            tool.put("type", "function");
            JSONObject func = new JSONObject();
            func.put("name", "applyRefund");
            func.put("description", "申请退款");
            JSONObject params = new JSONObject();
            params.put("type", "object");
            JSONObject props = new JSONObject();
            JSONObject orderIdProp = new JSONObject();
            orderIdProp.put("type", "integer");
            orderIdProp.put("description", "订单ID");
            props.put("orderId", orderIdProp);
            JSONObject reasonProp = new JSONObject();
            reasonProp.put("type", "string");
            reasonProp.put("description", "退款原因");
            props.put("reason", reasonProp);
            params.put("properties", props);
            JSONArray required = new JSONArray();
            required.add("orderId");
            params.put("required", required);
            func.put("parameters", params);
            tool.put("function", func);
            tools.add(tool);
        }

        // ---- cancelVisit ----
        {
            JSONObject tool = new JSONObject();
            tool.put("type", "function");
            JSONObject func = new JSONObject();
            func.put("name", "cancelVisit");
            func.put("description", "取消探访预约");
            JSONObject params = new JSONObject();
            params.put("type", "object");
            JSONObject props = new JSONObject();
            JSONObject visitIdProp = new JSONObject();
            visitIdProp.put("type", "integer");
            visitIdProp.put("description", "预约ID");
            props.put("visitId", visitIdProp);
            params.put("properties", props);
            JSONArray required = new JSONArray();
            required.add("visitId");
            params.put("required", required);
            func.put("parameters", params);
            tool.put("function", func);
            tools.add(tool);
        }

        // ---- resubmitLeave ----
        {
            JSONObject tool = new JSONObject();
            tool.put("type", "function");
            JSONObject func = new JSONObject();
            func.put("name", "resubmitLeave");
            func.put("description", "重新提交请假单");
            JSONObject params = new JSONObject();
            params.put("type", "object");
            JSONObject props = new JSONObject();
            JSONObject leaveIdProp = new JSONObject();
            leaveIdProp.put("type", "integer");
            leaveIdProp.put("description", "请假单ID");
            props.put("leaveId", leaveIdProp);
            params.put("properties", props);
            JSONArray required = new JSONArray();
            required.add("leaveId");
            params.put("required", required);
            func.put("parameters", params);
            tool.put("function", func);
            tools.add(tool);
        }

        return tools;
    }

    /**
     * 构建需要用户确认的提示消息
     */
    private String buildConfirmSummary(String toolName, Map<String, Object> args) {
        StringBuilder sb = new StringBuilder();
        sb.append("[CONFIRM]");
        switch (toolName) {
            case "cancelOrder":
                sb.append("确定要取消订单 #").append(args.get("orderId")).append(" 吗？");
                break;
            case "applyRefund":
                sb.append("确定要为订单 #").append(args.get("orderId")).append(" 申请退款吗？");
                Object reason = args.get("reason");
                if (reason != null && !reason.toString().isEmpty()) {
                    sb.append(" 退款原因：").append(reason);
                }
                break;
            case "cancelVisit":
                sb.append("确定要取消探访预约 #").append(args.get("visitId")).append(" 吗？");
                break;
            case "resubmitLeave":
                sb.append("确定要重新提交请假单 #").append(args.get("leaveId")).append(" 吗？");
                break;
            default:
                sb.append("确定要执行 ").append(toolName).append(" 操作吗？");
        }
        return sb.toString();
    }

    // ==================== 数据查询 ====================

    private String getUserContext(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) return null;

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
        List<Elder> elders = elderMapper.selectElderByMember(memberId);
        if (elders == null || elders.isEmpty()) return "暂无";

        StringBuilder sb = new StringBuilder();
        for (Elder elder : elders) {
            Bill param = new Bill();
            param.setElderId(elder.getId());
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

        List<ElderLeaveVo> allLeaves = elderLeaveMapper.selectElderLeaveList(new ElderLeaveDto());
        if (allLeaves == null || allLeaves.isEmpty()) return "暂无请假数据";

        Set<Long> elderIds = elders.stream().map(Elder::getId).collect(Collectors.toSet());
        StringBuilder sb = new StringBuilder();
        for (ElderLeaveVo l : allLeaves) {
            if (l.getElderId() != null && elderIds.contains(l.getElderId())) {
                sb.append("老人：").append(l.getElderName() != null ? l.getElderName() : "未知")
                  .append("，申请单号：").append(l.getOrderNo())
                  .append("，开始时间：").append(DateUtils.parseDateToStr("yyyy-MM-dd", l.getLeaveStartTime()))
                  .append("，预计返回：").append(DateUtils.parseDateToStr("yyyy-MM-dd", l.getPlannedReturnTime()))
                  .append("，状态：").append(formatLeaveStatus(l.getStatus()))
                  .append("；\n");
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
        return "你是乐康养老院的智能助手，名字叫\"小乐\"。\n\n"
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

    /**
     * 调用 DeepSeek API，返回原始 JSON 响应
     * @param messages 消息列表（支持含 tool_calls 的消息）
     * @param tools    工具定义 JSON (null 表示不传 tools)
     */
    private String callDeepSeek(List<Map<String, Object>> messages, JSONArray tools) throws Exception {
        List<Map<String, Object>> recent = new ArrayList<Map<String, Object>>();
        if (!messages.isEmpty() && "system".equals(messages.get(0).get("role"))) {
            recent.add(messages.get(0));
        }
        int start = Math.max(recent.size(), messages.size() - 4);
        for (int i = start; i < messages.size(); i++) {
            recent.add(messages.get(i));
        }

        String body = buildRequestBody(recent, tools);
        log.debug("DeepSeek request body: {}", body);

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
            java.io.InputStream errStream = conn.getErrorStream();
            String err = errStream != null ? org.apache.commons.io.IOUtils.toString(errStream, "UTF-8") : "no error body";
            log.error("DeepSeek API error: {} {}", code, err);
            conn.disconnect();
            throw new RuntimeException("DeepSeek API error: " + code + " - " + err);
        }

        String resp = org.apache.commons.io.IOUtils.toString(conn.getInputStream(), "UTF-8");
        conn.disconnect();
        return resp;
    }

    /**
     * 使用 fastjson2 构建请求体，支持 tool_calls 消息和 tools 定义
     */
    private String buildRequestBody(List<Map<String, Object>> messages, JSONArray tools) {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);

        JSONArray msgsArray = new JSONArray();
        for (Map<String, Object> m : messages) {
            JSONObject msgObj = new JSONObject();
            boolean hasToolCalls = false;
            for (Map.Entry<String, Object> entry : m.entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue();
                if ("tool_calls".equals(key) && val != null) {
                    hasToolCalls = true;
                }
                if (val != null) {
                    msgObj.put(key, val);
                }
            }
            // tool_calls 消息需要显式设置 content 为 null
            if (hasToolCalls) {
                msgObj.put("content", (Object) null);
            }
            msgsArray.add(msgObj);
        }
        body.put("messages", msgsArray);

        if (tools != null) {
            body.put("tools", tools);
        }

        return JSON.toJSONString(body, JSONWriter.Feature.WriteNulls);
    }

    // ==================== 会话管理 ====================

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getHistory(String sessionId) {
        Object o = redisCache.getCacheObject(SESSION_PREFIX + sessionId);
        return o instanceof List ? (List<Map<String, Object>>) o : new ArrayList<Map<String, Object>>();
    }

    private void saveHistory(String sessionId, List<Map<String, Object>> msgs) {
        // 保留 system + 最多 12 条（6 轮对话）
        if (msgs.size() > 13) {
            List<Map<String, Object>> system = msgs.subList(0, 1);
            List<Map<String, Object>> recent = msgs.subList(msgs.size() - 12, msgs.size());
            List<Map<String, Object>> pruned = new ArrayList<Map<String, Object>>(system);
            pruned.addAll(recent);
            redisCache.setCacheObject(SESSION_PREFIX + sessionId, pruned, SESSION_TTL, TimeUnit.SECONDS);
        } else {
            redisCache.setCacheObject(SESSION_PREFIX + sessionId, msgs, SESSION_TTL, TimeUnit.SECONDS);
        }
    }

    // ==================== 工具方法 ====================

}
