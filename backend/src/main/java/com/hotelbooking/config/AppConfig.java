package com.hotelbooking.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public AppConfig(@Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * The clock defines what "today" means. It is a bean so tests can replace it
     * and so the business timezone is configurable instead of hidden in the JVM default.
     */
    @Bean
    public Clock clock(@Value("${app.timezone}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS");
    }
}
