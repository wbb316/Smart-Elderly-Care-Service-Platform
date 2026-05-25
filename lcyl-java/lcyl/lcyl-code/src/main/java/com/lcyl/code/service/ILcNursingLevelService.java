package com.lcyl.code.service;

import java.util.List;
import com.lcyl.code.domain.LcNursingLevel;

public interface ILcNursingLevelService
{
    public LcNursingLevel selectLcNursingLevelById(Long id);
    public List<LcNursingLevel> selectLcNursingLevelList(LcNursingLevel lcNursingLevel);
    public int insertLcNursingLevel(LcNursingLevel lcNursingLevel);
    public int updateLcNursingLevel(LcNursingLevel lcNursingLevel);
    public int deleteLcNursingLevelByIds(Long[] ids);
    public int deleteLcNursingLevelById(Long id);
}
