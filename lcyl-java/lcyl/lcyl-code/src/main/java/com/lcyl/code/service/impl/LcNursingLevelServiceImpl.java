package com.lcyl.code.service.impl;

import java.util.List;
import com.lcyl.common.utils.DateUtils;
import com.lcyl.code.domain.LcNursingLevel;
import com.lcyl.code.mapper.LcNursingLevelMapper;
import com.lcyl.code.service.ILcNursingLevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LcNursingLevelServiceImpl implements ILcNursingLevelService
{
    @Autowired
    private LcNursingLevelMapper lcNursingLevelMapper;

    @Override
    public LcNursingLevel selectLcNursingLevelById(Long id)
    {
        return lcNursingLevelMapper.selectNursingLevelById(id);
    }

    @Override
    public List<LcNursingLevel> selectLcNursingLevelList(LcNursingLevel lcNursingLevel)
    {
        return lcNursingLevelMapper.selectNursingLevelList(lcNursingLevel);
    }

    @Override
    public int insertLcNursingLevel(LcNursingLevel lcNursingLevel)
    {
        lcNursingLevel.setCreateTime(DateUtils.getNowDate());
        return lcNursingLevelMapper.insertNursingLevel(lcNursingLevel);
    }

    @Override
    public int updateLcNursingLevel(LcNursingLevel lcNursingLevel)
    {
        lcNursingLevel.setUpdateTime(DateUtils.getNowDate());
        return lcNursingLevelMapper.updateNursingLevel(lcNursingLevel);
    }

    @Override
    public int deleteLcNursingLevelByIds(Long[] ids)
    {
        return lcNursingLevelMapper.deleteNursingLevelByIds(ids);
    }

    @Override
    public int deleteLcNursingLevelById(Long id)
    {
        return lcNursingLevelMapper.deleteNursingLevelById(id);
    }
}
