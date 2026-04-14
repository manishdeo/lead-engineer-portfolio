package com.maplehub.analytics.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class ClickHouseConfig {

    @Bean
    public DataSource clickHouseDataSource(
            @org.springframework.beans.factory.annotation.Value("${clickhouse.url}") String url,
            @org.springframework.beans.factory.annotation.Value("${clickhouse.username:default}") String username,
            @org.springframework.beans.factory.annotation.Value("${clickhouse.password:}") String password) {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName("com.clickhouse.jdbc.ClickHouseDriver")
                .build();
    }

    @Bean
    public JdbcTemplate clickHouseJdbcTemplate(DataSource clickHouseDataSource) {
        return new JdbcTemplate(clickHouseDataSource);
    }
}
