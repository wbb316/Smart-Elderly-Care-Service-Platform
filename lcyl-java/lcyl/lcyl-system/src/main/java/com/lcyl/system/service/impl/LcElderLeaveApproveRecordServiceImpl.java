package com.lcyl.system.service.impl;

import java.util.List;
import com.lcyl.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.lcyl.system.mapper.LcElderLeaveApproveRecordMapper;
import com.lcyl.system.domain.LcElderLeaveApproveRecord;
import com.lcyl.system.service.ILcElderLeaveApproveRecordService;

@Service
public class LcElderLeaveApproveRecordServiceImpl implements ILcElderLeaveApproveRecordService
{
    @Autowired
    private LcElderLeaveApproveRecordMapper lcElderLeaveApproveRecordMapper;

    @Override
    public LcElderLeaveApproveRecord selectLcElderLeaveApproveRecordById(Long id)
    {
        return lcElderLeaveApproveRecordMapper.selectLcElderLeaveApproveRecordById(id);
    }

    @Override
    public List<LcElderLeaveApproveRecord> selectLcElderLeaveApproveRecordList(LcElderLeaveApproveRecord lcElderLeaveApproveRecord)
    {
        return lcElderLeaveApproveRecordMapper.selectLcElderLeaveApproveRecordList(lcElderLeaveApproveRecord);
    }

    @Override
    public int insertLcElderLeaveApproveRecord(LcElderLeaveApproveRecord lcElderLeaveApproveRecord)
    {
        lcElderLeaveApproveRecord.setCreateTime(DateUtils.getNowDate());
        return lcElderLeaveApproveRecordMapper.insertLcElderLeaveApproveRecord(lcElderLeaveApproveRecord);
    }

    @Override
    public int updateLcElderLeaveApproveRecord(LcElderLeaveApproveRecord lcElderLeaveApproveRecord)
    {
        return lcElderLeaveApproveRecordMapper.updateLcElderLeaveApproveRecord(lcElderLeaveApproveRecord);
    }

    @Override
    public int deleteLcElderLeaveApproveRecordByIds(Long[] ids)
    {
        return lcElderLeaveApproveRecordMapper.deleteLcElderLeaveApproveRecordByIds(ids);
    }

    @Override
    public int deleteLcElderLeaveApproveRecordById(Long id)
    {
        return lcElderLeaveApproveRecordMapper.deleteLcElderLeaveApproveRecordById(id);
    }
}
