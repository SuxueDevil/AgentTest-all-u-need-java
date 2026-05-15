package com.agenttest.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求日志拦截器 — 记录每个 HTTP 请求的路径、Controller 方法和耗时。
 * <p>
 * 日志格式：→ GET /api/agents/3 | AgentController.detail<br>
 *           ← 200 (23ms)
 */
public class RequestLogInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLogInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        if (handler instanceof HandlerMethod hm) {
            String controller = hm.getBeanType().getSimpleName();
            String method = hm.getMethod().getName();
            String path = request.getRequestURI();
            if (request.getQueryString() != null) {
                path += "?" + request.getQueryString();
            }
            request.setAttribute("_start", System.currentTimeMillis());
            log.info("----------------------------------------------------------------------");
            log.info("→ {} {} | {}.{}", request.getMethod(), path, controller, method);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (handler instanceof HandlerMethod) {
            Long start = (Long) request.getAttribute("_start");
            long elapsed = start != null ? System.currentTimeMillis() - start : 0;
            log.info("← {} ({}ms)", response.getStatus(), elapsed);
        }
    }
}
