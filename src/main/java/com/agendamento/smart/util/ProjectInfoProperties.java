package com.agendamento.smart.util;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

    @Configuration
    @ConfigurationProperties(prefix = "project")
    public class ProjectInfoProperties {

        private String version;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
