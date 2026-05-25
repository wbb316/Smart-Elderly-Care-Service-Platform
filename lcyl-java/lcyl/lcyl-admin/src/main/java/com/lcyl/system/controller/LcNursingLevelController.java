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
import com.lcyl.code.domain.LcNursingLevel;
import com.lcyl.code.service.ILcNursingLevelService;
import com.lcyl.common.utils.poi.ExcelUtil;
import com.lcyl.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/system/level")
public class LcNursingLevelController extends BaseController
{
    @Autowired
    private ILcNursingLevelService lcNursingLevelService;

    @PreAuthorize("@ss.hasPermi('system:level:list')")
    @GetMapping("/list")
    public TableDataInfo list(LcNursingLevel lcNursingLevel)
    {
        startPage();
        List<LcNursingLevel> list = lcNursingLevelService.selectLcNursingLevelList(lcNursingLevel);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('system:level:export')")
    @Log(title = "护理等级", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LcNursingLevel lcNursingLevel)
    {
        List<LcNursingLevel> list = lcNursingLevelService.selectLcNursingLevelList(lcNursingLevel);
        ExcelUtil<LcNursingLevel> util = new ExcelUtil<LcNursingLevel>(LcNursingLevel.class);
        util.exportExcel(response, list, "护理等级数据");
    }

    @PreAuthorize("@ss.hasPermi('system:level:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(lcNursingLevelService.selectLcNursingLevelById(id));
    }

    @PreAuthorize("@ss.hasPermi('system:level:add')")
    @Log(title = "护理等级", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody LcNursingLevel lcNursingLevel)
    {
        return toAjax(lcNursingLevelService.insertLcNursingLevel(lcNursingLevel));
    }

    @PreAuthorize("@ss.hasPermi('system:level:edit')")
    @Log(title = "护理等级", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody LcNursingLevel lcNursingLevel)
    {
        return toAjax(lcNursingLevelService.updateLcNursingLevel(lcNursingLevel));
    }

    @PreAuthorize("@ss.hasPermi('system:level:remove')")
    @Log(title = "护理等级", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(lcNursingLevelService.deleteLcNursingLevelByIds(ids));
    }
}
