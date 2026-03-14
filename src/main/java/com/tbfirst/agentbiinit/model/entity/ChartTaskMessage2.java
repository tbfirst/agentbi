package com.tbfirst.agentbiinit.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 消息实体类优化1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartTaskMessage2 implements Serializable {

    private String taskId;
    private String fingerprint;
    private String userId;
    private String memoryId;

    // 任务参数从原来的 csvData 改为 csvUrl
    private String csvUrl;
    private String goal;
    private String chartType;
    private String name;

    private long timestamp;  // 用于超时检测
}
