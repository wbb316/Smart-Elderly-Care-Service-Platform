package com.lcyl.system.service.impl;

import java.util.List;
import com.lcyl.common.exception.ServiceException;
import com.lcyl.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lcyl.system.mapper.LcServiceOrderRefundMapper;
import com.lcyl.system.domain.LcServiceOrderRefund;
import com.lcyl.system.service.ILcServiceOrderRefundService;

/**
 * 服务订单退款Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-30
 */
@Service
public class LcServiceOrderRefundServiceImpl implements ILcServiceOrderRefundService 
{
    @Autowired
    private LcServiceOrderRefundMapper lcServiceOrderRefundMapper;

    /**
     * 查询服务订单退款
     * 
     * @param id 服务订单退款主键
     * @return 服务订单退款
     */
    @Override
    public LcServiceOrderRefund selectLcServiceOrderRefundById(Long id)
    {
        return lcServiceOrderRefundMapper.selectLcServiceOrderRefundById(id);
    }

    /**
     * 查询服务订单退款列表
     * 
     * @param lcServiceOrderRefund 服务订单退款
     * @return 服务订单退款
     */
    @Override
    public List<LcServiceOrderRefund> selectLcServiceOrderRefundList(LcServiceOrderRefund lcServiceOrderRefund)
    {
        return lcServiceOrderRefundMapper.selectLcServiceOrderRefundList(lcServiceOrderRefund);
    }

    /**
     * 新增服务订单退款
     * 
     * @param lcServiceOrderRefund 服务订单退款
     * @return 结果
     */
    @Override
    public int insertLcServiceOrderRefund(LcServiceOrderRefund lcServiceOrderRefund)
    {
        lcServiceOrderRefund.setCreateTime(DateUtils.getNowDate());
        return lcServiceOrderRefundMapper.insertLcServiceOrderRefund(lcServiceOrderRefund);
    }

    /**
     * 修改服务订单退款
     * 
     * @param lcServiceOrderRefund 服务订单退款
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateLcServiceOrderRefund(LcServiceOrderRefund lcServiceOrderRefund)
    {
        // 查询修改前的退款状态
        LcServiceOrderRefund oldRefund = lcServiceOrderRefundMapper.selectLcServiceOrderRefundById(lcServiceOrderRefund.getId());

        lcServiceOrderRefund.setUpdateTime(DateUtils.getNowDate());
        int rows = lcServiceOrderRefundMapper.updateLcServiceOrderRefund(lcServiceOrderRefund);

        // 如果退款状态发生了变更，同步订单和账单
        if (oldRefund != null && lcServiceOrderRefund.getRefundStatus() != null
                && !lcServiceOrderRefund.getRefundStatus().equals(oldRefund.getRefundStatus()))
        {
            String orderNo = oldRefund.getOrderNo();
            if ("1".equals(lcServiceOrderRefund.getRefundStatus()))
            {
                // 改为退款成功 → 同步订单(已关闭+退款成功) + 账单(已退款)
                lcServiceOrderRefundMapper.updateOrderStatusByOrderNo(orderNo, "5", "3");
                lcServiceOrderRefundMapper.updateBillTradeStatusByOrderNo(orderNo, "3");
            }
            else if ("2".equals(lcServiceOrderRefund.getRefundStatus()))
            {
                // 改为退款失败 → 同步订单(退款失败) + 恢复账单(已支付)
                lcServiceOrderRefundMapper.updateOrderStatusByOrderNo(orderNo, null, "4");
                lcServiceOrderRefundMapper.updateBillTradeStatusByOrderNo(orderNo, "1");
            }
        }

        return rows;
    }

    /**
     * 批量删除服务订单退款
     * 
     * @param ids 需要删除的服务订单退款主键
     * @return 结果
     */
    @Override
    public int deleteLcServiceOrderRefundByIds(Long[] ids)
    {
        return lcServiceOrderRefundMapper.deleteLcServiceOrderRefundByIds(ids);
    }

    /**
     * 删除服务订单退款信息
     * 
     * @param id 服务订单退款主键
     * @return 结果
     */
    @Override
    public int deleteLcServiceOrderRefundById(Long id)
    {
        return lcServiceOrderRefundMapper.deleteLcServiceOrderRefundById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int refundSuccess(Long id)
    {
        LcServiceOrderRefund refund = lcServiceOrderRefundMapper.selectLcServiceOrderRefundById(id);
        if (refund == null)
        {
            throw new ServiceException("退款记录不存在");
        }
        if (!"0".equals(refund.getRefundStatus()))
        {
            throw new ServiceException("当前退款记录不是处理中状态");
        }

        refund.setRefundStatus("1");
        refund.setRefundTime(DateUtils.getNowDate());
        refund.setFailCode(null);
        refund.setFailReason(null);
        refund.setUpdateTime(DateUtils.getNowDate());
        int rows = lcServiceOrderRefundMapper.updateLcServiceOrderRefund(refund);

        // 同步订单状态：已关闭+退款成功
        lcServiceOrderRefundMapper.updateOrderStatusByOrderNo(refund.getOrderNo(), "5", "3");

        // 同步账单状态：已退款
        lcServiceOrderRefundMapper.updateBillTradeStatusByOrderNo(refund.getOrderNo(), "3");

        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int refundFail(Long id, String failCode, String failReason)
    {
        LcServiceOrderRefund refund = lcServiceOrderRefundMapper.selectLcServiceOrderRefundById(id);
        if (refund == null)
        {
            throw new ServiceException("退款记录不存在");
        }
        if (!"0".equals(refund.getRefundStatus()))
        {
            throw new ServiceException("当前退款记录不是处理中状态");
        }

        refund.setRefundStatus("2");
        refund.setFailCode(failCode);
        refund.setFailReason(failReason);
        refund.setUpdateTime(DateUtils.getNowDate());
        int rows = lcServiceOrderRefundMapper.updateLcServiceOrderRefund(refund);

        // 同步订单状态：退款失败（orderStatus不变，仅改tradeStatus）
        lcServiceOrderRefundMapper.updateOrderStatusByOrderNo(refund.getOrderNo(), null, "4");

        // 恢复账单状态：已支付
        lcServiceOrderRefundMapper.updateBillTradeStatusByOrderNo(refund.getOrderNo(), "1");

        return rows;
    }
}
