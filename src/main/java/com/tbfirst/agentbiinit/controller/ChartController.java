package com.tbfirst.agentbiinit.controller;

import com.tbfirst.agentbiinit.common.BaseResponse;
import com.tbfirst.agentbiinit.common.ResultUtils;
import com.tbfirst.agentbiinit.model.dto.chart.*;
import com.tbfirst.agentbiinit.service.ChartService;
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
    private ChartService chartService;
     /**
     * 根据用户上传的图表数据生成图表【异步调用】，注意返回的是一个任务 id，需要根据任务 id 轮询查询结果
     * rabbitmq 异步调用，异步线程池处理文件解析，先对文件进行多重校验，之后上传文件到阿里云 oss 存储
     */
    @PostMapping("/aiChartMQApache2")
    public BaseResponse<String> genChartByAiMQApache2(@RequestPart("multipartFile") MultipartFile multipartFile,
                                                    GenerateChartByAiRequest generateChartByAiRequest,
                                                    HttpServletRequest request) {

        return ResultUtils.success(chartService.genChartByAiMQApache2(multipartFile, generateChartByAiRequest, request));
    }
}
