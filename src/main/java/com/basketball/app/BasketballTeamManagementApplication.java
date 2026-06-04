package com.basketball.app;

import com.basketball.app.config.CorsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CorsConfig.class)
public class BasketballTeamManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasketballTeamManagementApplication.class, args);
    }
}

