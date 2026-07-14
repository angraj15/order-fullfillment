package com.example.orderfulfilment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(scanBasePackages = "com.example.orderfulfilment")
@EnableRetry
public class OrderFulfilmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderFulfilmentApplication.class, args);
    }
}
