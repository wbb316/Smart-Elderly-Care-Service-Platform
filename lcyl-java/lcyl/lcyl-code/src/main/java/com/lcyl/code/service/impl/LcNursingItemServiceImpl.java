package com.lcyl.code.service.impl;

import java.util.List;
import com.lcyl.common.utils.DateUtils;
import com.lcyl.code.domain.NursingItem;
import com.lcyl.code.mapper.NursingItemMapper;
import com.lcyl.code.service.ILcNursingItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LcNursingItemServiceImpl implements ILcNursingItemService
{
    @Autowired
    private NursingItemMapper nursingItemMapper;

    @Override
    public NursingItem selectLcNursingItemById(Long id)
    {
        return nursingItemMapper.selectNursingItemById(id);
    }

    @Override
    public List<NursingItem> selectLcNursingItemList(NursingItem nursingItem)
    {
        return nursingItemMapper.selectNursingItemList(nursingItem);
    }

    @Override
    public int insertLcNursingItem(NursingItem nursingItem)
    {
        nursingItem.setCreateTime(DateUtils.getNowDate());
        return nursingItemMapper.insertNursingItem(nursingItem);
    }

    @Override
    public int updateLcNursingItem(NursingItem nursingItem)
    {
        nursingItem.setUpdateTime(DateUtils.getNowDate());
        return nursingItemMapper.updateNursingItem(nursingItem);
    }

    @Override
    public int deleteLcNursingItemByIds(Long[] ids)
    {
        return nursingItemMapper.deleteNursingItemByIds(ids);
    }

    @Override
    public int deleteLcNursingItemById(Long id)
    {
        return nursingItemMapper.deleteNursingItemById(id);
    }
}
