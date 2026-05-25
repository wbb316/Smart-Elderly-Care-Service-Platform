package com.lcyl.system.controller;

import java.util.List;
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
import com.lcyl.system.domain.LcElderLeave;
import com.lcyl.system.service.ILcElderLeaveService;
import com.lcyl.common.utils.poi.ExcelUtil;
import com.lcyl.common.core.page.TableDataInfo;

/**
 * 老人请假申请主Controller
 *
 * @author ruoyi
 * @date 2026-05-24
 */
@RestController
@RequestMapping("/system/leave")
public class LcElderLeaveController extends BaseController
{
    @Autowired
    private ILcElderLeaveService lcElderLeaveService;

    /**
     * 查询老人请假申请主列表
     */
    @PreAuthorize("@ss.hasPermi('system:leave:list')")
    @GetMapping("/list")
    public TableDataInfo list(LcElderLeave lcElderLeave)
    {
        startPage();
        List<LcElderLeave> list = lcElderLeaveService.selectLcElderLeaveList(lcElderLeave);
        return getDataTable(list);
    }

    /**
     * 导出老人请假申请主列表
     */
    @PreAuthorize("@ss.hasPermi('system:leave:export')")
    @Log(title = "老人请假申请主", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LcElderLeave lcElderLeave)
    {
        List<LcElderLeave> list = lcElderLeaveService.selectLcElderLeaveList(lcElderLeave);
        ExcelUtil<LcElderLeave> util = new ExcelUtil<LcElderLeave>(LcElderLeave.class);
        util.exportExcel(response, list, "老人请假申请主数据");
    }

    /**
     * 获取老人请假申请主详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:leave:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(lcElderLeaveService.selectLcElderLeaveById(id));
    }

    /**
     * 新增老人请假申请主
     */
    @PreAuthorize("@ss.hasPermi('system:leave:add')")
    @Log(title = "老人请假申请主", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody LcElderLeave lcElderLeave)
    {
        return toAjax(lcElderLeaveService.insertLcElderLeave(lcElderLeave));
    }

    /**
     * 修改老人请假申请主
     */
    @PreAuthorize("@ss.hasPermi('system:leave:edit')")
    @Log(title = "老人请假申请主", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody LcElderLeave lcElderLeave)
    {
        return toAjax(lcElderLeaveService.updateLcElderLeave(lcElderLeave));
    }

    /**
     * 删除老人请假申请主
     */
    @PreAuthorize("@ss.hasPermi('system:leave:remove')")
    @Log(title = "老人请假申请主", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(lcElderLeaveService.deleteLcElderLeaveByIds(ids));
    }
}
