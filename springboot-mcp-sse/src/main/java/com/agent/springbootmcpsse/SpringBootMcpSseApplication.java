package com.agent.springbootmcpsse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.sms.docqamcphost"})
public class SpringBootMcpSseApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootMcpSseApplication.class, args);
    }

}
