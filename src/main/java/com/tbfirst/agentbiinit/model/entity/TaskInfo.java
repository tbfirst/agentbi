package com.tbfirst.agentbiinit.model.entity;

import com.alibaba.dashscope.common.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务信息实体类
 */
@Data
@Builder
@NoArgsConstructor   // ← Jackson 需要这个无参构造
@AllArgsConstructor  // ← @Builder 需要这个配合全参构造
public class TaskInfo {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 文件指纹（用于幂等）
     */
    private String fingerprint;

    /**
     * 任务状态：PENDING(待处理)、RUNNING(执行中)、SUCCESS(成功)、FAILED(失败)
     */
    private TaskStatus status;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 对话记忆ID
     */
    private String memoryId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 执行结果（JSON字符串，SUCCESS时填充）
     */
    private String result;

    /**
     * 错误信息（FAILED时填充）
     */
    private String errorMsg;
}
