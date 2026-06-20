package com.lunar.lunar_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@MapperScan("com.lunar.lunar_backend.mapper")
@EnableAsync
@SpringBootApplication
public class LunarBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(LunarBackendApplication.class, args);
    }

}
