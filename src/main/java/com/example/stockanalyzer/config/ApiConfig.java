package com.example.stockanalyzer.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "api")
public class ApiConfig {
    private Boolean isSandBoxMode;

    @Value("${bot.nameBot}")
    String botName;

    @Value("${bot.tokenBot}")
    String token;
}
