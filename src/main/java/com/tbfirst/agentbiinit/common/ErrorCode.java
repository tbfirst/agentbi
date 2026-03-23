package com.tbfirst.agentbiinit.common;

/**
 * 自定义错误码
 *
 * @author tbfirst
 */
public enum ErrorCode {
    SYSTEM_ERROR(50000, "系统内部异常"),
    FILE_SCAN_ERROR(40501, "文件扫描错误"),
    TOO_MANY_REQUESTS(42900, "请求频率过快，请稍后重试"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN(40100, "未登录"),
    NO_AUTH(40101, "无权限"),
    OPERATION_ERROR(40200, "操作失败"),
    USER_NOT_FOUND(40400, "用户不存在");
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
