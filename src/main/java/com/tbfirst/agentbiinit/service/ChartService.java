package com.tbfirst.agentbiinit.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.tbfirst.agentbiinit.model.dto.chart.ChartResultResponse;
import com.tbfirst.agentbiinit.model.dto.chart.FileTaskStatusResponse;
import com.tbfirst.agentbiinit.model.dto.chart.GenerateChartByAiRequest;
import com.tbfirst.agentbiinit.model.entity.ChartEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

/**
* @author tbfirst
* @description 智能 bi 图表信息表服务
*/
public interface ChartService extends IService<ChartEntity> {

     /**
     * 根据用户上传的图表数据生成图表,先对文件进行多重校验，之后才使用阿里云oss 存储用户文件
     * 【rabbitmq 异步调用 ai，异步线程池处理文件解析】
     */
    String genChartByAiMQApache2(MultipartFile multipartFile, GenerateChartByAiRequest generateChartByAiRequest, HttpServletRequest request);

    /**
     * 查询文件解析任务状态
     */
    FileTaskStatusResponse getFileTaskStatus(String fileTaskId);

    /**
     * 根据指纹获取图表结果
     */
    ChartResultResponse getChartResultByFingerprint(String fingerprint);
}
