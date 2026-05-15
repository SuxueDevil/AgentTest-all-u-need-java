package com.agenttest.common.exception;

import com.agenttest.common.Response;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理 — 拦截三类异常并统一返回 Response 格式:
 * <ol>
 *   <li>{@link BusinessException} — 业务异常，原样返回其 code + message</li>
 *   <li>{@link MethodArgumentNotValidException} — 参数校验失败，提取字段错误信息</li>
 *   <li>{@link Exception} — 未预期的系统异常，返回 500 + 通用错误信息，记录完整堆栈</li>
 * </ol>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常 — 返回业务错误码和提示 */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Response<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Response.error(e.getCode(), e.getMessage());
    }

    /** 参数校验失败 — 拼接所有字段的校验错误信息 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Response.error(400, msg);
    }

    /** 兜底异常 — 记录完整堆栈，返回通用错误，避免敏感信息泄露 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Response<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: {} {}", request.getMethod(), request.getRequestURI(), e);
        return Response.error(500, "服务器内部错误");
    }
}
