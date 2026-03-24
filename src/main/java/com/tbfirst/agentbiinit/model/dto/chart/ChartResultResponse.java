package com.tbfirst.agentbiinit.model.dto.chart;

import com.tbfirst.agentbiinit.model.vo.AiAnalysisVO;
import lombok.Data;

@Data
public class ChartResultResponse {
    private AiAnalysisVO.ChartConfig chartConfig;
    private String echartsCode;
    private String analysisResult;
    private String status;
    private String fingerprint;
    private String fileTaskId;
}
