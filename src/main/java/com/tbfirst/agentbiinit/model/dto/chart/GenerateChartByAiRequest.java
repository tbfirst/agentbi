package com.tbfirst.agentbiinit.model.dto.chart;

import lombok.Data;

@Data
public class GenerateChartByAiRequest {
    // 图表类型直接通过 @RequestPart("file") MultipartFile multipartFile传入，不需要单独定义

    /**
     * 图表名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表类型
     */
    private String chartType;

    private static final long serialVersionUID = 1L;
}
