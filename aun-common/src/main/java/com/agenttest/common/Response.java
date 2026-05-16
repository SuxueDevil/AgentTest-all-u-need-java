package com.agenttest.common;

import lombok.Data;

/**
 * 统一响应体 — 所有 Controller 接口返回此格式。
 * <pre>
 * 成功: { "code": 200, "message": "success", "data": {...} }
 * 失败: { "code": 400, "message": "参数错误", "data": null }
 * </pre>
 *
 * @param <T> data 字段的类型
 */
@Data
public class Response<T> {

    /** HTTP 状态码，200 表示成功 */
    private int code;
    /** 提示信息 */
    private String message;
    /** 业务数据，可为 null */
    private T data;

    /**
     * 私有构造，通过静态工厂方法创建实例。
     *
     * @param code    HTTP 状态码
     * @param message 提示信息
     * @param data    业务数据
     */
    private Response(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（带数据）。
     *
     * @param data 业务数据
     * @param <T>  data 字段的类型
     * @return code=200, message="success" 的 Response
     */
    public static <T> Response<T> success(T data) {
        return new Response<>(200, "success", data);
    }

    /**
     * 成功响应（无数据）。
     *
     * @param <T> data 字段的类型
     * @return code=200, message="success", data=null 的 Response
     */
    public static <T> Response<T> success() {
        return new Response<>(200, "success", null);
    }

    /**
     * 错误响应（无数据）。
     *
     * @param code    业务错误码，如 400 / 404 / 500
     * @param message 错误提示信息
     * @param <T>     data 字段的类型
     * @return 含错误码和提示的 Response
     */
    public static <T> Response<T> error(int code, String message) {
        return new Response<>(code, message, null);
    }

    /**
     * 错误响应（带数据，用于部分成功场景，如批量操作返回失败明细）。
     *
     * @param code    业务错误码
     * @param message 错误提示信息
     * @param data    附属数据（如失败明细）
     * @param <T>     data 字段的类型
     * @return 含错误码、提示和数据的 Response
     */
    public static <T> Response<T> error(int code, String message, T data) {
        return new Response<>(code, message, data);
    }
}
