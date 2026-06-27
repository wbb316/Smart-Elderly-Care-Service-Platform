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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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

    private static final Set<String> CONFIRM_TOOLS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("cancelOrder", "applyRefund")));

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
        dto.setLeaveId(leaveId);
        // resubmitLeave 需要关联的原始数据
        int rows = elderLeaveService.resubmitLeave(dto);
        return rows > 0 ? "请假单已重新提交" : "提交失败，请联系管理员";
    }

    private String formatResult(String msg, Object result) {
        return msg + "（" + (result != null ? result.toString() : "") + "）";
    }
}
