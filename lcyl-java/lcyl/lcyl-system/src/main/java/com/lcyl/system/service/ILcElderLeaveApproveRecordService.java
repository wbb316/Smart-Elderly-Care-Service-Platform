package com.lcyl.system.service;

import java.util.List;
import com.lcyl.system.domain.LcElderLeaveApproveRecord;

public interface ILcElderLeaveApproveRecordService
{
    public LcElderLeaveApproveRecord selectLcElderLeaveApproveRecordById(Long id);
    public List<LcElderLeaveApproveRecord> selectLcElderLeaveApproveRecordList(LcElderLeaveApproveRecord lcElderLeaveApproveRecord);
    public int insertLcElderLeaveApproveRecord(LcElderLeaveApproveRecord lcElderLeaveApproveRecord);
    public int updateLcElderLeaveApproveRecord(LcElderLeaveApproveRecord lcElderLeaveApproveRecord);
    public int deleteLcElderLeaveApproveRecordByIds(Long[] ids);
    public int deleteLcElderLeaveApproveRecordById(Long id);
}
