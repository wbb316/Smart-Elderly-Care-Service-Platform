package com.lcyl.system.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import com.lcyl.code.domain.CheckIn;
import com.lcyl.code.domain.CheckInConfigg;
import com.lcyl.code.domain.ElderRegistration;
import com.lcyl.code.mapper.AssignNurseMapper;
import org.springframework.transaction.support.TransactionTemplate;
import com.lcyl.code.mapper.CheckInConfiggMapper;
import com.lcyl.code.mapper.CheckInMapper;
import com.lcyl.code.service.IElderRegistrationService;
import com.lcyl.common.exception.ServiceException;
import com.lcyl.system.domain.Contract;
import com.lcyl.system.domain.dto.AddInfo;
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

    @Autowired
    private IElderRegistrationService elderRegistrationService;

    @Autowired
    private TransactionTemplate transactionTemplate;

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
    @PreAuthorize("@ss.hasPermi('system:elder:query')")
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
    @PreAuthorize("@ss.hasPermi('system:elder:query')")
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

        Contract contract = contractMapper.selectContractByElderId(id);
        CheckIn checkIn = checkInMapper.selectCheckInById(id);
        List<String> assignNurse = assignNurseMapper.selectNurseByElderId(id);
        String string = checkInMapper.getlevelName(id);

        Map<String, Object> result = new HashMap<>();
        result.put("bedNo", checkInConfig.getBedNo());
        result.put("contract", contract != null ? contract.getContractNo() : null);
        result.put("counselor", checkIn != null ? checkIn.getCounselor() : null);
        result.put("nursingLevel", string);
        result.put("nurseNames", assignNurse);
        return success(result);
    }

    /**
     * 查询待审核的老人注册列表
     */
    @PreAuthorize("@ss.hasPermi('system:elder:list')")
    @GetMapping("/pendingRegistrations")
    public TableDataInfo pendingRegistrations(ElderRegistration elderRegistration) {
        startPage();
        if (elderRegistration.getStatus() == null) {
            elderRegistration.setStatus("1");
        }
        List<ElderRegistration> list = elderRegistrationService.selectElderRegistrationList(elderRegistration);
        return getDataTable(list);
    }

    /**
     * 审核老人注册
     * @param id 注册ID
     * @param body 包含 action（approve/reject）和 remark
     */
    @PreAuthorize("@ss.hasPermi('system:elder:edit')")
    @Log(title = "老人注册审核", businessType = BusinessType.UPDATE)
    @PostMapping("/auditRegistration/{id}")
    public AjaxResult auditRegistration(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String action = body.get("action");
        ElderRegistration registration = elderRegistrationService.selectElderRegistrationById(id);
        if (registration == null) {
            return error("注册记录不存在");
        }
        if (!"1".equals(registration.getStatus())) {
            return error("该记录已审核，请勿重复操作");
        }

        if ("approve".equals(action)) {
            // 通过：创建 elder 记录并绑定家属（事务保护）
            transactionTemplate.execute(status -> {
                Elder elder = new Elder();
                elder.setName(registration.getName());
                elder.setIdCardNo(registration.getIdCardNo());
                elder.setAge(registration.getAge());
                elder.setSex(registration.getSex());
                elder.setPhone(registration.getPhone());
                elder.setImage(registration.getImage());
                elder.setRemark(registration.getRemark());
                elder.setStatus(1L); // 启用
                elderService.insertElder(elder);

                // 绑定老人和家属关系
                AddInfo addInfo = new AddInfo();
                addInfo.setElderId(elder.getId());
                addInfo.setRelation(registration.getRelation() != null ? registration.getRelation() : "家属");
                elderService.insertInfo(addInfo, registration.getMemberId());

                // 更新注册状态
                registration.setStatus("2");
                elderRegistrationService.updateElderRegistration(registration);
                return null;
            });
        } else if ("reject".equals(action)) {
            registration.setStatus("3");
            elderRegistrationService.updateElderRegistration(registration);
        } else {
            return error("操作类型错误，请使用 approve 或 reject");
        }

        return success("操作成功");
    }
}
