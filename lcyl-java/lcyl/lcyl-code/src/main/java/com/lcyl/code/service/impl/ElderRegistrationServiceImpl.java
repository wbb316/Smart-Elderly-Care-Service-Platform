package com.lcyl.code.service.impl;

import com.lcyl.code.domain.ElderRegistration;
import com.lcyl.code.mapper.ElderRegistrationMapper;
import com.lcyl.code.service.IElderRegistrationService;
import com.lcyl.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ElderRegistrationServiceImpl implements IElderRegistrationService {

    @Autowired
    private ElderRegistrationMapper elderRegistrationMapper;

    @Override
    public ElderRegistration selectElderRegistrationById(Long id) {
        return elderRegistrationMapper.selectElderRegistrationById(id);
    }

    @Override
    public List<ElderRegistration> selectElderRegistrationList(ElderRegistration elderRegistration) {
        return elderRegistrationMapper.selectElderRegistrationList(elderRegistration);
    }

    @Override
    @Transactional
    public int insertElderRegistration(ElderRegistration elderRegistration) {
        elderRegistration.setCreateTime(DateUtils.getNowDate());
        return elderRegistrationMapper.insertElderRegistration(elderRegistration);
    }

    @Override
    @Transactional
    public int updateElderRegistration(ElderRegistration elderRegistration) {
        elderRegistration.setUpdateTime(DateUtils.getNowDate());
        return elderRegistrationMapper.updateElderRegistration(elderRegistration);
    }
}
