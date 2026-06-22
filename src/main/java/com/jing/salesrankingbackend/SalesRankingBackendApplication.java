package com.jing.salesrankingbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.jing.salesrankingbackend.mapper")
@EnableScheduling
public class SalesRankingBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesRankingBackendApplication.class, args);
    }

}
