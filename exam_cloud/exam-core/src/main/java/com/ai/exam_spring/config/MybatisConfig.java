package com.ai.exam_spring.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 * <p>
 * 注册分页插件，支持 MySQL 分页查询。
 * 注册乐观锁插件，支持乐观锁。
 */
@Configuration
public class MybatisConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor plusInterceptor = new MybatisPlusInterceptor();
        plusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        plusInterceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return plusInterceptor;
    }
}
