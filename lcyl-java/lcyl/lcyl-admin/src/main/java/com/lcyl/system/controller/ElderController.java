package com.lcyl.system.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import com.lcyl.code.domain.CheckIn;
import com.lcyl.code.domain.CheckInConfigg;
import com.lcyl.code.mapper.AssignNurseMapper;
import com.lcyl.code.mapper.CheckInConfiggMapper;
import com.lcyl.code.mapper.CheckInMapper;
import com.lcyl.common.exception.ServiceException;
import com.lcyl.system.domain.Contract;
import com.lcyl.system.mapper.ContractMapper;
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
import com.lcyl.system.domain.Elder;
import com.lcyl.system.service.IElderService;
import com.lcyl.common.utils.poi.ExcelUtil;
import com.lcyl.common.core.page.TableDataInfo;

/**
 * 老人Controller
 *
 * @author ruoyi
 * @date 2026-05-23
 */
@RestController
@RequestMapping("/system/elder")
public class ElderController extends BaseController
{
    @Autowired
    private IElderService elderService;

    /**
     * 查询老人列表
     */
    @PreAuthorize("@ss.hasPermi('system:elder:list')")
    @GetMapping("/list")
    public TableDataInfo list(Elder elder)
    {
        startPage();
        List<Elder> list = elderService.selectElderList(elder);
        return getDataTable(list);
    }

    /**
     * 导出老人列表
     */
    @PreAuthorize("@ss.hasPermi('system:elder:export')")
    @Log(title = "老人", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Elder elder)
    {
        List<Elder> list = elderService.selectElderList(elder);
        ExcelUtil<Elder> util = new ExcelUtil<Elder>(Elder.class);
        util.exportExcel(response, list, "老人数据");
    }

    /**
     * 获取老人详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:elder:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(elderService.selectElderById(id));
    }

    /**
     * 新增老人
     */
    @PreAuthorize("@ss.hasPermi('system:elder:add')")
    @Log(title = "老人", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Elder elder)
    {
        return toAjax(elderService.insertElder(elder));
    }

    /**
     * 修改老人
     */
    @PreAuthorize("@ss.hasPermi('system:elder:edit')")
    @Log(title = "老人", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Elder elder)
    {
        return toAjax(elderService.updateElder(elder));
    }

    /**
     * 删除老人
     */
    @PreAuthorize("@ss.hasPermi('system:elder:remove')")
    @Log(title = "老人", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(elderService.deleteElderByIds(ids));
    }

    /**
     * 获取所有老人列表（不分页）
     */
    @GetMapping("/getElderList")
    public AjaxResult getElderList() {
        List<Elder> elderList = elderService.selectAllElder();
        return success(elderList);
    }

    @Autowired
    private CheckInConfiggMapper checkInConfigMapper;
    @Autowired
    private ContractMapper contractMapper;
    @Autowired
    private AssignNurseMapper assignNurseMapper;
    @Autowired
    private CheckInMapper checkInMapper;

    /**
     * 获取老人完整信息
     */
    @GetMapping("/fullInfo/{id}")
    public AjaxResult getElderFullInfo(@PathVariable Long id) {
        Elder elder = elderService.selectElderById(id);
        if (elder == null) {
            return error("老人不存在");
        }

        CheckInConfigg checkInConfig = checkInConfigMapper.selectCheckInConfigById(id);
        if (checkInConfig == null) {
            throw new ServiceException("未找到老人的入住配置信息，请先办理入住");
        }

        Contract contract = contractMapper.selectContractById(id);
        CheckIn checkIn = checkInMapper.selectCheckInById(id);
        List<String> assignNurse = assignNurseMapper.selectNurseByElderId(id);
        String string = checkInMapper.getlevelName(id);

        Map<String, Object> result = new HashMap<>();
        result.put("bedNo", checkInConfig.getBedNo());
        result.put("contract", contract.getContractNo());
        result.put("counselor", checkIn.getCounselor());
        result.put("nursingLevel", string);
        result.put("nurseNames", assignNurse);
        return success(result);
    }
}
