package com.goofly.gray.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import com.goofly.gray.core.annotation.EnableGrayConfig;

@SpringBootApplication
@EnableDiscoveryClient
@EnableGrayConfig
public class ProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }
}