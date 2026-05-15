package com.agenttest.common;

/**
 * 统一响应体 — 所有 Controller 接口返回此格式。
 * <pre>
 * 成功: { "code": 200, "message": "success", "data": {...} }
 * 失败: { "code": 400, "message": "参数错误", "data": null }
 * </pre>
 *
 * @param <T> data 字段的类型
 */
public class Response<T> {

    /** HTTP 状态码，200 表示成功 */
    private int code;
    /** 提示信息 */
    private String message;
    /** 业务数据，可为 null */
    private T data;

    /** 私有构造，通过静态工厂方法创建实例 */
    private Response(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功响应（带数据） */
    public static <T> Response<T> success(T data) {
        return new Response<>(200, "success", data);
    }

    /** 成功响应（无数据） */
    public static <T> Response<T> success() {
        return new Response<>(200, "success", null);
    }

    /** 错误响应 */
    public static <T> Response<T> error(int code, String message) {
        return new Response<>(code, message, null);
    }

    /** 错误响应（带数据，用于部分成功场景） */
    public static <T> Response<T> error(int code, String message, T data) {
        return new Response<>(code, message, data);
    }

    // ==================== getters / setters ====================

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
