package com.agenttest;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.sql.DataSource;

/**
 * Agent 智能评测平台 — Spring Boot 启动入口。
 * <p>
 * MapperScan 扫描 com.agenttest.mapper 包下的所有 MyBatis Mapper 接口，
 * 自动生成代理实现并注册为 Spring Bean。
 * 自动装配会扫描 com.agenttest 及其子包下的 @Component / @Service / @Controller。
 */
@SpringBootApplication
@MapperScan("com.agenttest.mapper")
public class AgentTestApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AgentTestApplication.class);

    private final DataSource dataSource;

    /** 构造器注入 DataSource，用于预热连接池 */
    public AgentTestApplication(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static void main(String[] args) {
        SpringApplication.run(AgentTestApplication.class, args);
    }

    @Override
    public void run(String... args) {
        warmUpDataSource();
        log.info(
                "===================================\n"
                + "                                             \n"
                + "         Test-All-u-Need                     \n"
                + "         你想要的一切测试，启动成功               \n"
                + "               (=^･ω･^=)  ﾆｬ                  \n"
                + "                                              \n"
                + "========================================================================="
        );
    }

    /**
     * 预热 HikariCP 连接池 — 避免首个请求因远程 DB 建连而延迟数秒。
     */
    private void warmUpDataSource() {
        try (var conn = dataSource.getConnection()) {
            log.info("数据库连接池预热完成，DB: {}", conn.getMetaData().getURL());
        } catch (Exception e) {
            log.warn("数据库连接池预热失败: {}", e.getMessage());
        }
    }
}
