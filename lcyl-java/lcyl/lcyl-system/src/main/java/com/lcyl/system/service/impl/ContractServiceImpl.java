package com.lcyl.system.service.impl;

import java.util.List;
import java.util.Map;

import com.lcyl.common.utils.DateUtils;
import com.lcyl.common.utils.StringUtils;
import com.lcyl.system.domain.Contract;
import com.lcyl.system.domain.Elder;
import com.lcyl.system.mapper.ContractMapper;
import com.lcyl.system.mapper.ElderMapper;
import com.lcyl.system.service.IContractService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 合同Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-23
 */
@Service
public class ContractServiceImpl implements IContractService
{
    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private ElderMapper elderMapper;

    @Override
    public Contract selectContractById(Long id)
    {
        return contractMapper.selectContractById(id);
    }

    @Override
    public List<Contract> selectContractList(Contract contract)
    {
        Map<String, Object> params = contract.getParams();
        if (params != null) {
            String beginTimeStr = (String) params.get("beginTime");
            String endTimeStr = (String) params.get("endTime");
            if (beginTimeStr != null) {
                contract.setCreateTime(DateUtils.parseDate(beginTimeStr));
            }
            if (endTimeStr != null) {
                contract.setEndTime(DateUtils.parseDate(endTimeStr));
            }
        }

        String elderName = contract.getElderName();
        if (StringUtils.isNotBlank(elderName)) {
            Elder queryElder = new Elder();
            queryElder.setName(elderName);
            List<Elder> elderList = elderMapper.selectElderList(queryElder);
            if (CollectionUtils.isNotEmpty(elderList)) {
                contract.setElderId(elderList.get(0).getId());
            } else {
                contract.setElderId(null);
            }
        }
        return contractMapper.selectContractList(contract);
    }

    @Override
    @Transactional
    public int insertContract(Contract contract)
    {
        contract.setCreateTime(DateUtils.getNowDate());
        return contractMapper.insertContract(contract);
    }

    @Override
    @Transactional
    public int updateContract(Contract contract)
    {
        contract.setUpdateTime(DateUtils.getNowDate());
        return contractMapper.updateContract(contract);
    }

    @Override
    @Transactional
    public void batchUpdateContractStatus()
    {
        contractMapper.batchUpdateContractStatus();
    }

    @Override
    @Transactional
    public int deleteContractByIds(Long[] ids)
    {
        return contractMapper.deleteContractByIds(ids);
    }

    @Override
    @Transactional
    public int deleteContractById(Long id)
    {
        return contractMapper.deleteContractById(id);
    }
}
