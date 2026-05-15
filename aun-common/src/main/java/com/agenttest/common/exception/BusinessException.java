package com.agenttest.common.exception;

/**
 * 业务异常 — 由业务代码主动抛出，携带错误码。
 * 被 {@link GlobalExceptionHandler} 拦截后转为统一 Response 格式返回前端。
 *
 * 使用示例:
 * <pre>
 *   throw new BusinessException(404, "Agent不存在");
 *   throw new BusinessException("操作失败");  // code 默认为 500
 * </pre>
 */
public class BusinessException extends RuntimeException {

    /** 业务错误码，如 400 参数错误、404 资源不存在 */
    private final int code;

    /**
     * @param code    业务错误码
     * @param message 错误描述
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 使用默认错误码 500
     * @param message 错误描述
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public int getCode() { return code; }
}
