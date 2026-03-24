package com.tbfirst.agentbiinit.controller;

import com.tbfirst.agentbiinit.common.BaseResponse;
import com.tbfirst.agentbiinit.common.ResultUtils;
import com.tbfirst.agentbiinit.model.dto.user.UserLoginRequest;
import com.tbfirst.agentbiinit.model.dto.user.UserRegisterRequest;
import com.tbfirst.agentbiinit.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.tbfirst.agentbiinit.model.entity.UserEntity;



@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new RuntimeException("请求参数为空");
        }
        long userId = userService.userRegister(userRegisterRequest);
        return ResultUtils.success(userId);
    }

    @PostMapping("/login")
    public BaseResponse<Long> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new RuntimeException("请求参数为空");
        }
        long userId = userService.userLogin(userLoginRequest);
        return ResultUtils.success(userId);
    }

    @GetMapping("/current")
    public BaseResponse<UserEntity> getCurrentUser(HttpServletRequest request) {
        UserEntity user = userService.getLoginUser();
        return ResultUtils.success(user);
    }

    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        boolean result = userService.userLogout();
        return ResultUtils.success(result);
    }
}
