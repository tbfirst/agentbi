package com.tbfirst.agentbiinit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tbfirst.agentbiinit.common.ErrorCode;
import com.tbfirst.agentbiinit.exception.BusinessException;
import com.tbfirst.agentbiinit.mapper.UserMapper;
import com.tbfirst.agentbiinit.model.dto.user.UserLoginRequest;
import com.tbfirst.agentbiinit.model.dto.user.UserRegisterRequest;
import com.tbfirst.agentbiinit.model.entity.UserEntity;
import com.tbfirst.agentbiinit.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Date;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    private static final String SALT = "tbfirst";

    @Resource
    private HttpServletRequest request;

    @Override
    public Long userRegister(UserRegisterRequest userRegisterRequest) {
        String username = userRegisterRequest.getUsername();
        String password = userRegisterRequest.getPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();

        if (StringUtils.isAnyBlank(username, password, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        if (username.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名过短");
        }

        if (password.length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短");
        }

        if (!password.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        synchronized (username.intern()) {
            QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", username);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
            }

            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());

            UserEntity user = new UserEntity();
            user.setUsername(username);
            user.setPassword(encryptPassword);
            user.setUserRole("user");
            user.setCreateTime(new Date());
            user.setUpdateTime(new Date());

            boolean result = this.save(user);
            if (!result) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }

            return user.getId();
        }
    }

    @Override
    public Long userLogin(UserLoginRequest userLoginRequest) {
        String username = userLoginRequest.getUsername();
        String password = userLoginRequest.getPassword();

        if (StringUtils.isAnyBlank(username, password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());

        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        queryWrapper.eq("password", encryptPassword);
        UserEntity user = this.baseMapper.selectOne(queryWrapper);

        if (user == null) {
            log.info("user login failed, username cannot match password");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名或密码错误");
        }

        request.getSession().setAttribute(USER_LOGIN_STATE, user);

        return user.getId();
    }

    public static final String USER_LOGIN_STATE = "userLoginState";

    @Override
    public UserEntity getLoginUser() {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }
        return (UserEntity) userObj;
    }

    @Override
    public boolean userLogout() {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }
}
