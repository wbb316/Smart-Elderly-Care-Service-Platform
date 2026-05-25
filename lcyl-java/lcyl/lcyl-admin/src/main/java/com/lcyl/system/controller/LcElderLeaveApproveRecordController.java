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
import com.lcyl.system.domain.LcElderLeaveApproveRecord;
import com.lcyl.system.service.ILcElderLeaveApproveRecordService;
import com.lcyl.common.utils.poi.ExcelUtil;
import com.lcyl.common.core.page.TableDataInfo;

/**
 * 老人请假审批记录Controller
 *
 * @author ruoyi
 * @date 2026-05-24
 */
@RestController
@RequestMapping("/system/record")
public class LcElderLeaveApproveRecordController extends BaseController
{
    @Autowired
    private ILcElderLeaveApproveRecordService lcElderLeaveApproveRecordService;

    @PreAuthorize("@ss.hasPermi('system:record:list')")
    @GetMapping("/list")
    public TableDataInfo list(LcElderLeaveApproveRecord lcElderLeaveApproveRecord)
    {
        startPage();
        List<LcElderLeaveApproveRecord> list = lcElderLeaveApproveRecordService.selectLcElderLeaveApproveRecordList(lcElderLeaveApproveRecord);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('system:record:export')")
    @Log(title = "老人请假审批记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LcElderLeaveApproveRecord lcElderLeaveApproveRecord)
    {
        List<LcElderLeaveApproveRecord> list = lcElderLeaveApproveRecordService.selectLcElderLeaveApproveRecordList(lcElderLeaveApproveRecord);
        ExcelUtil<LcElderLeaveApproveRecord> util = new ExcelUtil<LcElderLeaveApproveRecord>(LcElderLeaveApproveRecord.class);
        util.exportExcel(response, list, "老人请假审批记录数据");
    }

    @PreAuthorize("@ss.hasPermi('system:record:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(lcElderLeaveApproveRecordService.selectLcElderLeaveApproveRecordById(id));
    }

    @PreAuthorize("@ss.hasPermi('system:record:add')")
    @Log(title = "老人请假审批记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody LcElderLeaveApproveRecord lcElderLeaveApproveRecord)
    {
        return toAjax(lcElderLeaveApproveRecordService.insertLcElderLeaveApproveRecord(lcElderLeaveApproveRecord));
    }

    @PreAuthorize("@ss.hasPermi('system:record:edit')")
    @Log(title = "老人请假审批记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody LcElderLeaveApproveRecord lcElderLeaveApproveRecord)
    {
        return toAjax(lcElderLeaveApproveRecordService.updateLcElderLeaveApproveRecord(lcElderLeaveApproveRecord));
    }

    @PreAuthorize("@ss.hasPermi('system:record:remove')")
    @Log(title = "老人请假审批记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(lcElderLeaveApproveRecordService.deleteLcElderLeaveApproveRecordByIds(ids));
    }
}
