package com.lunar.lunar_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.lunar.lunar_backend.mapper")
@SpringBootApplication
public class LunarBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(LunarBackendApplication.class, args);
    }

}
