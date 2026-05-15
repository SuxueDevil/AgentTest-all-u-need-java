package com.agenttest.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置 — 注册核心插件。
 * 当前仅注册分页插件，后续可扩展乐观锁、防全表更新等插件。
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 注册分页插件，指定 MySQL 方言。
     * 使用方式: Mapper 层调用 selectPage(new Page<>(page, size), wrapper) 即可分页。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
