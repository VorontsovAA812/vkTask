package com.vorontsov.task.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tarantool.TarantoolClient;
import org.tarantool.TarantoolClientConfig;
import org.tarantool.TarantoolClientImpl;

@Configuration
public class TarantoolConfig {

    @Bean
    public TarantoolClient tarantoolClient() {
        TarantoolClientConfig config = new TarantoolClientConfig();
        config.username = "guest";
        config.password = "";

        return new TarantoolClientImpl("127.0.0.1", config);
    }
}