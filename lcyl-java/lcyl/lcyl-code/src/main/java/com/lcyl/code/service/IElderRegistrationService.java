package com.lcyl.code.service;

import com.lcyl.code.domain.ElderRegistration;
import java.util.List;

public interface IElderRegistrationService {
    ElderRegistration selectElderRegistrationById(Long id);

    List<ElderRegistration> selectElderRegistrationList(ElderRegistration elderRegistration);

    int insertElderRegistration(ElderRegistration elderRegistration);

    int updateElderRegistration(ElderRegistration elderRegistration);
}
