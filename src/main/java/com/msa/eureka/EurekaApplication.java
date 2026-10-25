package com.msa.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/** 서비스 등록 정보를 보관하고 peer 서버와 복제하는 Eureka 서버입니다. */
@EnableEurekaServer
@SpringBootApplication
public class EurekaApplication {
    // HTTP 등록 API와 관리 대시보드를 함께 시작합니다.
    public static void main(String[] args) {
        SpringApplication.run(EurekaApplication.class, args);
    }
}
