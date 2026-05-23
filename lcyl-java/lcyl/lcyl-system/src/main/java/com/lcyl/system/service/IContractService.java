package com.lcyl.system.service;

import java.util.List;
import com.lcyl.system.domain.Contract;

/**
 * 合同Service接口
 *
 * @author ruoyi
 * @date 2026-05-23
 */
public interface IContractService
{
    public Contract selectContractById(Long id);

    public List<Contract> selectContractList(Contract contract);

    public int insertContract(Contract contract);

    public int updateContract(Contract contract);

    void batchUpdateContractStatus();

    public int deleteContractByIds(Long[] ids);

    public int deleteContractById(Long id);
}
