package com.lcyl.system.mapper;

import java.util.List;
import com.lcyl.system.domain.LcElderLeaveApproveRecord;

public interface LcElderLeaveApproveRecordMapper
{
    public LcElderLeaveApproveRecord selectLcElderLeaveApproveRecordById(Long id);
    public List<LcElderLeaveApproveRecord> selectLcElderLeaveApproveRecordList(LcElderLeaveApproveRecord lcElderLeaveApproveRecord);
    public int insertLcElderLeaveApproveRecord(LcElderLeaveApproveRecord lcElderLeaveApproveRecord);
    public int updateLcElderLeaveApproveRecord(LcElderLeaveApproveRecord lcElderLeaveApproveRecord);
    public int deleteLcElderLeaveApproveRecordById(Long id);
    public int deleteLcElderLeaveApproveRecordByIds(Long[] ids);
}
