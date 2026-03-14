package com.tbfirst.agentbiinit.model.enums;

import lombok.Getter;

/**
 * 存储在 redis 中的 ai 分析图表的对话信息的 key 标识
 */
@Getter
public enum AiRedisEnum {

    CHART_RESULT("chart:result:"),
    CHART_HISTORY("chart:history:"),
    CHART_LOCK("chart:lock:"),
    SYSTEM_PROMPT("system:prompt"),
    CHART_TASK("chart:task:"),
    FILE_TASK("file:task:"),
    CHART_LIMIT("chart_limit:");

    private final String value;

    AiRedisEnum(String value) {
        this.value = value;
    }

}
