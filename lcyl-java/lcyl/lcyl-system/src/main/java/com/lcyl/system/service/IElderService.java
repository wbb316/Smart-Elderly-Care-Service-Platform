package com.lcyl.system.service;

import java.util.List;
import com.lcyl.system.domain.Elder;
import com.lcyl.system.domain.dto.AddInfo;
import com.lcyl.system.domain.vo.BedVO;

/**
 * 老人Service接口
 *
 * @author ruoyi
 * @date 2026-05-23
 */
public interface IElderService
{
    /**
     * 查询老人
     *
     * @param id 老人主键
     * @return 老人
     */
    public Elder selectElderById(Long id);

    /**
     * 查询老人列表
     *
     * @param elder 老人
     * @return 老人集合
     */
    public List<Elder> selectElderList(Elder elder);

    /**
     * 查询所有老人（不分页）
     */
    public List<Elder> selectAllElder();

    /**
     * 根据家属会员ID查询关联的老人
     */
    public List<Elder> selectElderByUser(Long memberId);

    /**
     * 新增老人
     *
     * @param elder 老人
     * @return 结果
     */
    public int insertElder(Elder elder);

    /**
     * 修改老人
     *
     * @param elder 老人
     * @return 结果
     */
    public int updateElder(Elder elder);

    /**
     * 新增老人-家属关联
     */
    public int insertInfo(AddInfo addInfo);

    /**
     * 查询老人床位列表（家属端）
     */
    public List<BedVO> getElderBedList();

    /**
     * 删除老人-家属关联
     */
    public int deleteElderInfoById(Long id);

    /**
     * 批量删除老人
     *
     * @param ids 需要删除的老人主键集合
     * @return 结果
     */
    public int deleteElderByIds(Long[] ids);

    /**
     * 删除老人信息
     *
     * @param id 老人主键
     * @return 结果
     */
    public int deleteElderById(Long id);
}
