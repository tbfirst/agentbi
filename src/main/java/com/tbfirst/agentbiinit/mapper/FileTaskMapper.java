package com.tbfirst.agentbiinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tbfirst.agentbiinit.model.entity.FileTaskInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
* @description 针对表【file_task(文件任务表)】的数据库操作Mapper
*/
@Mapper
public interface FileTaskMapper extends BaseMapper<FileTaskInfo> {
    // 继承了 BaseMapper，基本的 CRUD 方法（insert, updateById, selectById 等）都已经自带，不需要写 XML。

    /**
     * 根据 fileTaskId 查询（MyBatis-Plus 自带方法不够用时的自定义 SQL）
     */
    @Select("SELECT * FROM file_task_info WHERE file_task_id = #{fileTaskId}")
    FileTaskInfo selectByFileTaskId(@Param("fileTaskId") String fileTaskId);

    // todo 调用ai服务返回了任务ID，需要根据任务ID查询任务状态为SUCCEEDED，之后通过数据库中该条数据包含的指纹，去redis中查找信息
}




