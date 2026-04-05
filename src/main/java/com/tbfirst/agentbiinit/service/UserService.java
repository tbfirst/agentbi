package com.tbfirst.agentbiinit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tbfirst.agentbiinit.model.dto.user.UserLoginRequest;
import com.tbfirst.agentbiinit.model.dto.user.UserRegisterRequest;
import com.tbfirst.agentbiinit.model.entity.UserEntity;

public interface UserService extends IService<UserEntity> {
    /**
     * 用户注册
     * @param userRegisterRequest 用户注册请求
     * @return 用户ID
     */
    Long userRegister(UserRegisterRequest userRegisterRequest);
    /**
     * 用户登录
     * @param userLoginRequest 用户登录请求
     * @return 用户ID
     */
    Long userLogin(UserLoginRequest userLoginRequest);
    /**
     * 获取当前登录用户
     * @return 当前登录用户
     */
    UserEntity getLoginUser();
    /**
     * 用户退出登录
     * @return 是否退出成功
     */
    boolean userLogout();
}
