package com.agent.springbootmcpstream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.sms.docqamcphost"})
public class SpringBootMcpStreamApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootMcpStreamApplication.class, args);
    }

}
