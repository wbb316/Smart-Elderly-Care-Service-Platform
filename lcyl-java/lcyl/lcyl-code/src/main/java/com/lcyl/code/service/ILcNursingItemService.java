package com.lcyl.code.service;

import java.util.List;
import com.lcyl.code.domain.NursingItem;

public interface ILcNursingItemService
{
    public NursingItem selectLcNursingItemById(Long id);
    public List<NursingItem> selectLcNursingItemList(NursingItem nursingItem);
    public int insertLcNursingItem(NursingItem nursingItem);
    public int updateLcNursingItem(NursingItem nursingItem);
    public int deleteLcNursingItemByIds(Long[] ids);
    public int deleteLcNursingItemById(Long id);
}
