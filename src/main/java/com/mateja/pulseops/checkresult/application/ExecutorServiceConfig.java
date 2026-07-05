package com.mateja.pulseops.checkresult.application;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorServiceConfig {

    @Bean
    public ExecutorService checkExecutor()
    {
        return Executors.newFixedThreadPool(8);
    }
}
