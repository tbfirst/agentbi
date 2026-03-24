package com.tbfirst.agentbiinit.controller;

import com.tbfirst.agentbiinit.common.BaseResponse;
import com.tbfirst.agentbiinit.common.ResultUtils;
import com.tbfirst.agentbiinit.model.dto.chart.*;
import com.tbfirst.agentbiinit.service.ChartService;
import com.tbfirst.agentbiinit.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author tbfirst
 * 图表接口
 *
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private UserService userService;
    @Resource
    private ChartService chartService;

    @PostMapping("/aiChartMQApache2")
    public BaseResponse<ChartResultResponse> genChartByAiMQApache2(@RequestPart("multipartFile") MultipartFile multipartFile,
                                                    GenerateChartByAiRequest generateChartByAiRequest,
                                                    HttpServletRequest request) {
        userService.getLoginUser();
        return ResultUtils.success(chartService.genChartByAiMQApache2(multipartFile, generateChartByAiRequest, request));
    }

    @GetMapping("/fileTask/{fileTaskId}")
    public BaseResponse<FileTaskStatusResponse> getFileTaskStatus(@PathVariable String fileTaskId) {
        userService.getLoginUser();
        return ResultUtils.success(chartService.getFileTaskStatus(fileTaskId));
    }

    @GetMapping("/result/{fingerprint}")
    public BaseResponse<ChartResultResponse> getChartResult(@PathVariable String fingerprint) {
        userService.getLoginUser();
        return ResultUtils.success(chartService.getChartResultByFingerprint(fingerprint));
    }
}
