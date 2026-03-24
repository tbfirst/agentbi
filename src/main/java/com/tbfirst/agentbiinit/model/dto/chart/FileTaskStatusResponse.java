package com.tbfirst.agentbiinit.model.dto.chart;

import lombok.Data;

@Data
public class FileTaskStatusResponse {
    private String fileTaskId;
    private String status;
    private String fingerprint;
    private String csvUrl;
    private String errorMsg;
    private Integer parseTimeMs;
    private Long fileSize;
    private Long csvSize;
}
