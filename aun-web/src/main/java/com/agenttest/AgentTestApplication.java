package com.agenttest;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Agent 智能评测平台 — Spring Boot 启动入口。
 * <p>
 * MapperScan 扫描 com.agenttest.mapper 包下的所有 MyBatis Mapper 接口，
 * 自动生成代理实现并注册为 Spring Bean。
 * 自动装配会扫描 com.agenttest 及其子包下的 @Component / @Service / @Controller。
 */
@SpringBootApplication
@MapperScan("com.agenttest.mapper")
public class AgentTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentTestApplication.class, args);
    }
}
