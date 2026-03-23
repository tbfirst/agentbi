package com.tbfirst.agentbiinit.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件解析任务信息实体类
 */
@Data
@Builder
@NoArgsConstructor   // ← Jackson 需要这个无参构造
@AllArgsConstructor  // ← @Builder 需要这个配合全参构造
@TableName("file_task_info")  // ← 指定表名
public class FileTaskInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 文件解析任务ID
     */
    private String fileTaskId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 文件指纹
     */
    private String fingerprint;

    /**
     * 原始文件 OSS 的存储 URL 地址
     */
    private String originalUrl;

    /**
     * 解析后的 CSV 文件 OSS 的存储 URL 地址
     */
    private String csvUrl;

    /**
     * 任务状态：PENDING(待处理)、RUNNING(执行中)、SUCCEEDED(成功)、FAILED(失败)
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 原始文件大小
     */
    private Long fileSize;

    /**
     * CSV 文件大小
     */
    private Long csvSize;

    /**
     * 解析耗时（毫秒）
     */
    private Integer parseTimeMs;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
