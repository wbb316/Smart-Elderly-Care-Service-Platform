package com.lcyl.system.mapper;

import java.util.List;
import com.lcyl.system.domain.LcElderLeave;

/**
 * 老人请假申请主Mapper接口
 *
 * @author ruoyi
 * @date 2026-05-24
 */
public interface LcElderLeaveMapper
{
    /**
     * 查询老人请假申请主
     *
     * @param id 老人请假申请主主键
     * @return 老人请假申请主
     */
    public LcElderLeave selectLcElderLeaveById(Long id);

    /**
     * 查询老人请假申请主列表
     *
     * @param lcElderLeave 老人请假申请主
     * @return 老人请假申请主集合
     */
    public List<LcElderLeave> selectLcElderLeaveList(LcElderLeave lcElderLeave);

    /**
     * 新增老人请假申请主
     *
     * @param lcElderLeave 老人请假申请主
     * @return 结果
     */
    public int insertLcElderLeave(LcElderLeave lcElderLeave);

    /**
     * 修改老人请假申请主
     *
     * @param lcElderLeave 老人请假申请主
     * @return 结果
     */
    public int updateLcElderLeave(LcElderLeave lcElderLeave);

    /**
     * 删除老人请假申请主
     *
     * @param id 老人请假申请主主键
     * @return 结果
     */
    public int deleteLcElderLeaveById(Long id);

    /**
     * 批量删除老人请假申请主
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLcElderLeaveByIds(Long[] ids);
}
