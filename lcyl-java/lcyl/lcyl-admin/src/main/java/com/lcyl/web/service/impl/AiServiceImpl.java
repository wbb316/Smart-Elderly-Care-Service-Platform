package com.lcyl.web.service.impl;

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

        // 一次性查询所有请假记录，再按 elderId 过滤（ElderLeaveDto 无 elderId 字段）
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
            java.io.InputStream errStream = conn.getErrorStream();
            String err = errStream != null ? org.apache.commons.io.IOUtils.toString(errStream, "UTF-8") : "no error body";
            log.error("DeepSeek API error: {} {}", code, err);
            conn.disconnect();
            return "抱歉，我现在有点忙不过来，请稍后再试试吧 😊";
        }

        String resp = org.apache.commons.io.IOUtils.toString(conn.getInputStream(), "UTF-8");
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
        try {
            com.alibaba.fastjson2.JSONObject obj = com.alibaba.fastjson2.JSON.parseObject(json);
            return obj.getJSONArray("choices")
                     .getJSONObject(0)
                     .getJSONObject("message")
                     .getString("content");
        } catch (Exception e) {
            log.error("解析 DeepSeek 响应失败: {}", json, e);
            return null;
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ==================== 会话管理 ====================

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> getHistory(String sessionId) {
        Object o = redisCache.getCacheObject(SESSION_PREFIX + sessionId);
        return o instanceof List ? (List<Map<String, String>>) o : new ArrayList<>();
    }

    private void saveHistory(String sessionId, List<Map<String, String>> msgs) {
        // 保留 system + 最多 12 条（6 轮对话）
        if (msgs.size() > 13) {
            List<Map<String, String>> system = msgs.subList(0, 1);
            List<Map<String, String>> recent = msgs.subList(msgs.size() - 12, msgs.size());
            List<Map<String, String>> pruned = new ArrayList<>(system);
            pruned.addAll(recent);
            redisCache.setCacheObject(SESSION_PREFIX + sessionId, pruned, SESSION_TTL, TimeUnit.SECONDS);
        } else {
            redisCache.setCacheObject(SESSION_PREFIX + sessionId, msgs, SESSION_TTL, TimeUnit.SECONDS);
        }
    }
}
