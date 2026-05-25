package com.lcyl.system.service.impl;

import java.util.List;
import com.lcyl.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.lcyl.system.mapper.LcElderLeaveMapper;
import com.lcyl.system.domain.LcElderLeave;
import com.lcyl.system.service.ILcElderLeaveService;

/**
 * 老人请假申请主Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-24
 */
@Service
public class LcElderLeaveServiceImpl implements ILcElderLeaveService
{
    @Autowired
    private LcElderLeaveMapper lcElderLeaveMapper;

    /**
     * 查询老人请假申请主
     *
     * @param id 老人请假申请主主键
     * @return 老人请假申请主
     */
    @Override
    public LcElderLeave selectLcElderLeaveById(Long id)
    {
        return lcElderLeaveMapper.selectLcElderLeaveById(id);
    }

    /**
     * 查询老人请假申请主列表
     *
     * @param lcElderLeave 老人请假申请主
     * @return 老人请假申请主
     */
    @Override
    public List<LcElderLeave> selectLcElderLeaveList(LcElderLeave lcElderLeave)
    {
        return lcElderLeaveMapper.selectLcElderLeaveList(lcElderLeave);
    }

    /**
     * 新增老人请假申请主
     *
     * @param lcElderLeave 老人请假申请主
     * @return 结果
     */
    @Override
    public int insertLcElderLeave(LcElderLeave lcElderLeave)
    {
        lcElderLeave.setCreateTime(DateUtils.getNowDate());
        return lcElderLeaveMapper.insertLcElderLeave(lcElderLeave);
    }

    /**
     * 修改老人请假申请主
     *
     * @param lcElderLeave 老人请假申请主
     * @return 结果
     */
    @Override
    public int updateLcElderLeave(LcElderLeave lcElderLeave)
    {
        lcElderLeave.setUpdateTime(DateUtils.getNowDate());
        return lcElderLeaveMapper.updateLcElderLeave(lcElderLeave);
    }

    /**
     * 批量删除老人请假申请主
     *
     * @param ids 需要删除的老人请假申请主主键
     * @return 结果
     */
    @Override
    public int deleteLcElderLeaveByIds(Long[] ids)
    {
        return lcElderLeaveMapper.deleteLcElderLeaveByIds(ids);
    }

    /**
     * 删除老人请假申请主信息
     *
     * @param id 老人请假申请主主键
     * @return 结果
     */
    @Override
    public int deleteLcElderLeaveById(Long id)
    {
        return lcElderLeaveMapper.deleteLcElderLeaveById(id);
    }
}
