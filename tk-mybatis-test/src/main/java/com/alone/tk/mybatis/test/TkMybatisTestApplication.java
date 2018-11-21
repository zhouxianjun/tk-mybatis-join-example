package com.alone.tk.mybatis.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(basePackages = "com.alone.tk.mybatis.test.mapper")
public class TkMybatisTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TkMybatisTestApplication.class, args);
    }
}
