package com.tbfirst.agentbiinit.common;

/**
 * 自定义错误码
 *
 * @author tbfirst
 */
public enum ErrorCode {
    SYSTEM_ERROR(50000, "系统内部异常"),
    FILE_SCAN_ERROR(40501, "文件扫描错误"),
    TOO_MANY_REQUESTS(42900, "请求频率过快，请稍后重试");
    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
