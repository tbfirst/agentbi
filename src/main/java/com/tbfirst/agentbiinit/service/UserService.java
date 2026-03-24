package com.tbfirst.agentbiinit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tbfirst.agentbiinit.model.dto.user.UserLoginRequest;
import com.tbfirst.agentbiinit.model.dto.user.UserRegisterRequest;
import com.tbfirst.agentbiinit.model.entity.UserEntity;

public interface UserService extends IService<UserEntity> {
    Long userRegister(UserRegisterRequest userRegisterRequest);
    Long userLogin(UserLoginRequest userLoginRequest);
    UserEntity getLoginUser();
    boolean userLogout();
}
