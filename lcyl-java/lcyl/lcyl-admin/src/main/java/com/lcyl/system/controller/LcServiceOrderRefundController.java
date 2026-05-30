package com.lcyl.system.controller;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.lcyl.common.annotation.Log;
import com.lcyl.common.core.controller.BaseController;
import com.lcyl.common.core.domain.AjaxResult;
import com.lcyl.common.enums.BusinessType;
import com.lcyl.system.domain.LcServiceOrderRefund;
import com.lcyl.system.service.ILcServiceOrderRefundService;
import com.lcyl.common.utils.poi.ExcelUtil;
import com.lcyl.common.core.page.TableDataInfo;

/**
 * 服务订单退款Controller
 * 
 * @author ruoyi
 * @date 2026-05-30
 */
@RestController
@RequestMapping("/system/refund")
public class LcServiceOrderRefundController extends BaseController
{
    @Autowired
    private ILcServiceOrderRefundService lcServiceOrderRefundService;

    /**
     * 查询服务订单退款列表
     */
    @PreAuthorize("@ss.hasPermi('system:refund:list')")
    @GetMapping("/list")
    public TableDataInfo list(LcServiceOrderRefund lcServiceOrderRefund)
    {
        startPage();
        List<LcServiceOrderRefund> list = lcServiceOrderRefundService.selectLcServiceOrderRefundList(lcServiceOrderRefund);
        return getDataTable(list);
    }

    /**
     * 导出服务订单退款列表
     */
    @PreAuthorize("@ss.hasPermi('system:refund:export')")
    @Log(title = "服务订单退款", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LcServiceOrderRefund lcServiceOrderRefund)
    {
        List<LcServiceOrderRefund> list = lcServiceOrderRefundService.selectLcServiceOrderRefundList(lcServiceOrderRefund);
        ExcelUtil<LcServiceOrderRefund> util = new ExcelUtil<LcServiceOrderRefund>(LcServiceOrderRefund.class);
        util.exportExcel(response, list, "服务订单退款数据");
    }

    /**
     * 获取服务订单退款详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:refund:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(lcServiceOrderRefundService.selectLcServiceOrderRefundById(id));
    }

    /**
     * 新增服务订单退款
     */
    @PreAuthorize("@ss.hasPermi('system:refund:add')")
    @Log(title = "服务订单退款", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody LcServiceOrderRefund lcServiceOrderRefund)
    {
        return toAjax(lcServiceOrderRefundService.insertLcServiceOrderRefund(lcServiceOrderRefund));
    }

    /**
     * 修改服务订单退款
     */
    @PreAuthorize("@ss.hasPermi('system:refund:edit')")
    @Log(title = "服务订单退款", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody LcServiceOrderRefund lcServiceOrderRefund)
    {
        return toAjax(lcServiceOrderRefundService.updateLcServiceOrderRefund(lcServiceOrderRefund));
    }

    /**
     * 删除服务订单退款
     */
    @PreAuthorize("@ss.hasPermi('system:refund:remove')")
    @Log(title = "服务订单退款", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(lcServiceOrderRefundService.deleteLcServiceOrderRefundByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('system:refund:edit')")
    @Log(title = "退款成功", businessType = BusinessType.UPDATE)
    @PostMapping("/success/{id}")
    public AjaxResult refundSuccess(@PathVariable Long id)
    {
        return toAjax(lcServiceOrderRefundService.refundSuccess(id));
    }

    @PreAuthorize("@ss.hasPermi('system:refund:edit')")
    @Log(title = "退款失败", businessType = BusinessType.UPDATE)
    @PostMapping("/fail")
    public AjaxResult refundFail(@RequestBody Map<String, String> params)
    {
        Long id = Long.valueOf(params.get("refundId"));
        String failCode = params.get("failCode");
        String failReason = params.get("failReason");
        return toAjax(lcServiceOrderRefundService.refundFail(id, failCode, failReason));
    }
}
