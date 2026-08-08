package com.cybersocial;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.cybersocial.ai.AiAnalysisProperties;
import com.cybersocial.ai.AiServiceProperties;
import com.cybersocial.config.CorsProperties;
import com.cybersocial.security.jwt.JwtProperties;
import com.cybersocial.upload.CloudinaryProperties;

@EnableJpaAuditing
@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class, CloudinaryProperties.class, AiAnalysisProperties.class, AiServiceProperties.class})
public class CyberSocialBackendApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(CyberSocialBackendApplication.class, args);
    }
}
