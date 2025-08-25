package com.agendamento.smart.infra.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//@Configuration
public class WebConfiguration {
//    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry
                        .addMapping("http://localhost:3000")
                        .allowedOrigins("http://localhost:3000/")
                        .allowedHeaders("http://localhost:3000/")
                        .allowedMethods("http://localhost:3000/");
            }
        };
    }
}
