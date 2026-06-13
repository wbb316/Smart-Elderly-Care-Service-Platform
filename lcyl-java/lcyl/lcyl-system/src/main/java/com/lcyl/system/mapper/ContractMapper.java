package com.lcyl.system.mapper;

import java.util.List;
import com.lcyl.system.domain.Contract;

/**
 * 合同Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-23
 */
public interface ContractMapper 
{
    /**
     * 查询合同
     * 
     * @param id 合同主键
     * @return 合同
     */
    public Contract selectContractById(Long id);

    /**
     * 通过老人ID查询合同
     *
     * @param elderId 老人ID
     * @return 合同
     */
    public Contract selectContractByElderId(Long elderId);

    /**
     * 查询合同列表
     * 
     * @param contract 合同
     * @return 合同集合
     */
    public List<Contract> selectContractList(Contract contract);

    /**
     * 新增合同
     * 
     * @param contract 合同
     * @return 结果
     */
    public int insertContract(Contract contract);

    /**
     * 修改合同
     * 
     * @param contract 合同
     * @return 结果
     */
    public int updateContract(Contract contract);

    /**
     * 删除合同
     * 
     * @param id 合同主键
     * @return 结果
     */
    public int deleteContractById(Long id);

    /**
     * 批量删除合同
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteContractByIds(Long[] ids);

    void batchUpdateContractStatus();
}
