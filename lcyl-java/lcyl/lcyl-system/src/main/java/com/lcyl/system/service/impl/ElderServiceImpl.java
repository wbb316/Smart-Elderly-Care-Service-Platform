package com.lcyl.system.service.impl;

import java.util.List;
import com.lcyl.common.utils.DateUtils;
import com.lcyl.common.utils.UserThreadLocal;
import com.lcyl.system.domain.dto.AddInfo;
import com.lcyl.system.domain.vo.BedVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.lcyl.system.mapper.ElderMapper;
import com.lcyl.system.domain.Elder;
import com.lcyl.system.service.IElderService;

/**
 * 老人Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-23
 */
@Service
public class ElderServiceImpl implements IElderService
{
    @Autowired
    private ElderMapper elderMapper;

    @Override
    public Elder selectElderById(Long id)
    {
        return elderMapper.selectElderById(id);
    }

    @Override
    public List<Elder> selectElderList(Elder elder)
    {
        return elderMapper.selectElderList(elder);
    }

    @Override
    public List<Elder> selectAllElder()
    {
        return elderMapper.selectAllElderList();
    }

    @Override
    public List<Elder> selectElderByUser(Long memberId)
    {
        return elderMapper.selectElderByMember(memberId);
    }

    @Override
    public int insertElder(Elder elder)
    {
        elder.setCreateTime(DateUtils.getNowDate());
        return elderMapper.insertElder(elder);
    }

    @Override
    public int updateElder(Elder elder)
    {
        elder.setUpdateTime(DateUtils.getNowDate());
        return elderMapper.updateElder(elder);
    }

    @Override
    public int insertInfo(AddInfo addInfo)
    {
        Long memberId = UserThreadLocal.getUserId();
        return elderMapper.insertInfo(addInfo, memberId);
    }

    @Override
    public List<BedVO> getElderBedList()
    {
        Long memberId = UserThreadLocal.getUserId();
        return elderMapper.selectElderBedList(memberId);
    }

    @Override
    public int deleteElderInfoById(Long id)
    {
        return elderMapper.delElderInfoById(id);
    }

    @Override
    public int deleteElderByIds(Long[] ids)
    {
        return elderMapper.deleteElderByIds(ids);
    }

    @Override
    public int deleteElderById(Long id)
    {
        return elderMapper.deleteElderById(id);
    }
}
