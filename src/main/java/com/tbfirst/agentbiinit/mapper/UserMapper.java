package com.tbfirst.agentbiinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tbfirst.agentbiinit.model.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
