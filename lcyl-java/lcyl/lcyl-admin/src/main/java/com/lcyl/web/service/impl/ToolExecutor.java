package com.lcyl.web.service.impl;

import com.lcyl.code.domain.ElderLeave;
import com.lcyl.code.domain.LcReservation;
import com.lcyl.code.domain.ServiceOrder;
import com.lcyl.code.domain.dto.WxRefundApplyDTO;
import com.lcyl.code.dto.ElderLeaveResubmitDto;
import com.lcyl.code.mapper.ElderLeaveMapper;
import com.lcyl.code.mapper.LcReservationMapper;
import com.lcyl.code.mapper.ServiceOrderMapper;
import com.lcyl.code.service.impl.ElderLeaveServiceImpl;
import com.lcyl.code.service.impl.WxLoginServiceImpl;
import com.lcyl.system.domain.Elder;
import com.lcyl.web.service.constant.AiConstants;
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
    @Autowired private LcReservationMapper lcReservationMapper;

    public boolean needsConfirm(String toolName) {
        return AiConstants.CONFIRM_TOOLS.contains(toolName);
    }

    public Object execute(String toolName, Map<String, Object> args, Long memberId) {
        log.info("执行工具: toolName={}, memberId={}, args={}", toolName, memberId, args);

        // 权限检查：获取当前用户绑定的老人 ID 集合
        Set<Long> boundElderIds = elderMapper.selectElderByMember(memberId)
                .stream().map(Elder::getId).collect(Collectors.toSet());

        switch (toolName) {
            case "cancelOrder":
                return cancelOrder(args, boundElderIds);
            case "applyRefund":
                return applyRefund(args, boundElderIds);
            case "cancelVisit":
                return cancelVisit(args, memberId);
            case "resubmitLeave":
                return resubmitLeave(args, boundElderIds);
            default:
                throw new IllegalArgumentException("未知工具: " + toolName);
        }
    }

    private Object cancelOrder(Map<String, Object> args, Set<Long> boundElderIds) {
        Long orderId;
        try {
            orderId = Long.valueOf(args.get("orderId").toString());
        } catch (Exception e) {
            return "参数错误：缺少有效的订单ID";
        }
        // 检查订单所属老人
        ServiceOrder order = serviceOrderMapper.selectServiceOrderById(orderId);
        if (order == null) return "订单不存在";
        if (!boundElderIds.contains(order.getElderId())) return "无权操作此订单";

        log.info("取消订单: orderId={}, elderId={}", orderId, order.getElderId());
        try {
            int result = wxLoginService.cancelOrder(orderId);
            if (result <= 0) return "取消失败";
            return formatResult("订单 " + orderId + " 已取消", "成功");
        } catch (Exception e) {
            log.error("取消订单失败: orderId={}", orderId, e);
            return "取消失败：" + e.getMessage();
        }
    }

    private Object applyRefund(Map<String, Object> args, Set<Long> boundElderIds) {
        Long orderId;
        try {
            orderId = Long.valueOf(args.get("orderId").toString());
        } catch (Exception e) {
            return "参数错误：缺少有效的订单ID";
        }
        String reason = (String) args.getOrDefault("reason", "");
        // 检查归属
        ServiceOrder order = serviceOrderMapper.selectServiceOrderById(orderId);
        if (order == null) return "订单不存在";
        if (!boundElderIds.contains(order.getElderId())) return "无权操作此订单";

        log.info("申请退款: orderId={}, elderId={}, reason={}", orderId, order.getElderId(), reason);
        WxRefundApplyDTO dto = new WxRefundApplyDTO();
        dto.setOrderId(orderId);
        dto.setRefundReason(reason);
        try {
            wxLoginService.applyRefund(dto);
            return formatResult("退款申请已提交", "成功");
        } catch (Exception e) {
            log.error("退款申请失败: orderId={}", orderId, e);
            return "退款申请失败：" + e.getMessage();
        }
    }

    private Object cancelVisit(Map<String, Object> args, Long memberId) {
        Long visitId;
        try {
            visitId = Long.valueOf(args.get("visitId").toString());
        } catch (Exception e) {
            return "参数错误：缺少有效的预约ID";
        }
        // 查询预约信息，检查老人归属
        LcReservation reservation = lcReservationMapper.selectLcReservationById(visitId);
        if (reservation == null) return "预约不存在";

        // 预约记录只有老人姓名（olderName），通过姓名匹配确认归属
        List<Elder> elders = elderMapper.selectElderByMember(memberId);
        boolean owned = elders.stream().anyMatch(e -> e.getName().equals(reservation.getOlderName()));
        if (!owned) return "无权操作此预约";

        log.info("取消预约: visitId={}, olderName={}", visitId, reservation.getOlderName());
        try {
            int result = wxLoginService.deleteVisit(visitId, false);
            if (result <= 0) return "取消失败";
            if (result == 2) return "今日取消次数已达上限，如需强制取消请联系管理员";
            return formatResult("预约已取消", "成功");
        } catch (Exception e) {
            log.error("取消预约失败: visitId={}", visitId, e);
            return "取消预约失败：" + e.getMessage();
        }
    }

    private Object resubmitLeave(Map<String, Object> args, Set<Long> boundElderIds) {
        Long leaveId;
        try {
            leaveId = Long.valueOf(args.get("leaveId").toString());
        } catch (Exception e) {
            return "参数错误：缺少有效的请假单ID";
        }
        // 检查请假单归属
        ElderLeave leave = elderLeaveMapper.selectElderLeaveById(leaveId);
        if (leave == null) return "请假单不存在";
        if (!boundElderIds.contains(leave.getElderId())) return "无权操作此请假单";

        log.info("重新提交请假单: leaveId={}, elderId={}", leaveId, leave.getElderId());
        ElderLeaveResubmitDto dto = new ElderLeaveResubmitDto();
        dto.setLeaveId(leaveId);
        try {
            int rows = elderLeaveService.resubmitLeave(dto);
            if (rows <= 0) return "提交失败：数据未更新";
            return formatResult("请假单已重新提交", "成功");
        } catch (Exception e) {
            log.error("重新提交请假单失败: leaveId={}", leaveId, e);
            return "提交失败：" + e.getMessage();
        }
    }

    private String formatResult(String msg, Object result) {
        return msg + "（" + (result != null ? result.toString() : "") + "）";
    }
}
