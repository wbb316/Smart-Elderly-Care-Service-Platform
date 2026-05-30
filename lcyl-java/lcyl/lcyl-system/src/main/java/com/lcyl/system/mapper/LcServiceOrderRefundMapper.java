package com.lcyl.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.lcyl.system.domain.LcServiceOrderRefund;

/**
 * 服务订单退款Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-30
 */
public interface LcServiceOrderRefundMapper 
{
    /**
     * 查询服务订单退款
     * 
     * @param id 服务订单退款主键
     * @return 服务订单退款
     */
    public LcServiceOrderRefund selectLcServiceOrderRefundById(Long id);

    /**
     * 查询服务订单退款列表
     * 
     * @param lcServiceOrderRefund 服务订单退款
     * @return 服务订单退款集合
     */
    public List<LcServiceOrderRefund> selectLcServiceOrderRefundList(LcServiceOrderRefund lcServiceOrderRefund);

    /**
     * 新增服务订单退款
     * 
     * @param lcServiceOrderRefund 服务订单退款
     * @return 结果
     */
    public int insertLcServiceOrderRefund(LcServiceOrderRefund lcServiceOrderRefund);

    /**
     * 修改服务订单退款
     * 
     * @param lcServiceOrderRefund 服务订单退款
     * @return 结果
     */
    public int updateLcServiceOrderRefund(LcServiceOrderRefund lcServiceOrderRefund);

    /**
     * 删除服务订单退款
     * 
     * @param id 服务订单退款主键
     * @return 结果
     */
    public int deleteLcServiceOrderRefundById(Long id);

    /**
     * 批量删除服务订单退款
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLcServiceOrderRefundByIds(Long[] ids);

    public int updateOrderStatusByOrderNo(@Param("orderNo") String orderNo, @Param("orderStatus") String orderStatus, @Param("tradeStatus") String tradeStatus);

    public int updateBillTradeStatusByOrderNo(@Param("orderNo") String orderNo, @Param("tradeStatus") String tradeStatus);
}
