package com.agendamento.smart.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

    @Setter
    @Getter
    @Configuration
    @ConfigurationProperties(prefix = "project")
    public class ProjectInfoProperties {

        private String version;

    }
