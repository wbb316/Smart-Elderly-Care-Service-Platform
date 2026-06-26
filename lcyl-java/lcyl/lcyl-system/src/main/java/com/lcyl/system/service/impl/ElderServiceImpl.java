package com.lcyl.system.service.impl;

import java.util.List;
import com.lcyl.common.utils.DateUtils;
import com.lcyl.common.utils.UserThreadLocal;
import com.lcyl.system.domain.dto.AddInfo;
import com.lcyl.system.domain.vo.BedVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional
    public int insertElder(Elder elder)
    {
        elder.setCreateTime(DateUtils.getNowDate());
        return elderMapper.insertElder(elder);
    }

    @Override
    @Transactional
    public int updateElder(Elder elder)
    {
        elder.setUpdateTime(DateUtils.getNowDate());
        return elderMapper.updateElder(elder);
    }

    @Override
    @Transactional
    public int insertInfo(AddInfo addInfo)
    {
        Long memberId = UserThreadLocal.getUserId();
        return elderMapper.insertInfo(addInfo, memberId);
    }

    @Override
    @Transactional
    public int insertInfo(AddInfo addInfo, Long memberId)
    {
        return elderMapper.insertInfo(addInfo, memberId);
    }

    @Override
    public List<BedVO> getElderBedList()
    {
        Long memberId = UserThreadLocal.getUserId();
        return elderMapper.selectElderBedList(memberId);
    }

    @Override
    @Transactional
    public int deleteElderInfoById(Long id)
    {
        return elderMapper.delElderInfoById(id);
    }

    @Override
    @Transactional
    public int deleteElderByIds(Long[] ids)
    {
        // 先清理老人与家属的关联关系
        for (Long id : ids) {
            elderMapper.delElderInfoById(id);
        }
        // 注意：contract、check_in_config、assign_nurse 等关联表暂未自动清理，需在 ElderController 中补充
        return elderMapper.deleteElderByIds(ids);
    }

    @Override
    @Transactional
    public int deleteElderById(Long id)
    {
        elderMapper.delElderInfoById(id);
        return elderMapper.deleteElderById(id);
    }
}
