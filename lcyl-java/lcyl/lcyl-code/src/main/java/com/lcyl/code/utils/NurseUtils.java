package com.lcyl.code.utils;

import com.lcyl.code.domain.Nurse;
import com.lcyl.code.service.INurseService;
import com.lcyl.common.core.domain.model.LoginUser;
import com.lcyl.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class NurseUtils {

    private final INurseService nurseService;

    @Autowired
    public NurseUtils(INurseService nurseService) {
        this.nurseService = nurseService;
    }

    public Nurse getCurrentNurse() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            throw new ServiceException("当前用户未登录");
        }
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof LoginUser)) {
            throw new ServiceException("当前用户认证信息异常");
        }
        LoginUser loginUser = (LoginUser) principal;
        Long userId = loginUser.getUserId();
        Nurse nurse = nurseService.selectNurseByUserId(userId);
        if (nurse == null) {
            throw new ServiceException("当前登录用户未绑定护理员信息");
        }
        return nurse;
    }
}