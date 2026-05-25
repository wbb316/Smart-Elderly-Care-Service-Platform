package com.lcyl.system.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.lcyl.common.annotation.Log;
import com.lcyl.common.core.controller.BaseController;
import com.lcyl.common.core.domain.AjaxResult;
import com.lcyl.common.enums.BusinessType;
import com.lcyl.code.domain.NursingItem;
import com.lcyl.code.service.ILcNursingItemService;
import com.lcyl.common.utils.poi.ExcelUtil;
import com.lcyl.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/system/item")
public class LcNursingItemController extends BaseController
{
    @Autowired
    private ILcNursingItemService lcNursingItemService;

    @PreAuthorize("@ss.hasPermi('system:item:list')")
    @GetMapping("/list")
    public TableDataInfo list(NursingItem nursingItem)
    {
        startPage();
        List<NursingItem> list = lcNursingItemService.selectLcNursingItemList(nursingItem);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('system:item:export')")
    @Log(title = "护理项目", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, NursingItem nursingItem)
    {
        List<NursingItem> list = lcNursingItemService.selectLcNursingItemList(nursingItem);
        ExcelUtil<NursingItem> util = new ExcelUtil<NursingItem>(NursingItem.class);
        util.exportExcel(response, list, "护理项目数据");
    }

    @PreAuthorize("@ss.hasPermi('system:item:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(lcNursingItemService.selectLcNursingItemById(id));
    }

    @PreAuthorize("@ss.hasPermi('system:item:add')")
    @Log(title = "护理项目", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody NursingItem nursingItem)
    {
        return toAjax(lcNursingItemService.insertLcNursingItem(nursingItem));
    }

    @PreAuthorize("@ss.hasPermi('system:item:edit')")
    @Log(title = "护理项目", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody NursingItem nursingItem)
    {
        return toAjax(lcNursingItemService.updateLcNursingItem(nursingItem));
    }

    @PreAuthorize("@ss.hasPermi('system:item:remove')")
    @Log(title = "护理项目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(lcNursingItemService.deleteLcNursingItemByIds(ids));
    }
}
