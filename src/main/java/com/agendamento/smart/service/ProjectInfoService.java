package com.agendamento.smart.service;

import com.agendamento.smart.util.ProjectInfoProperties;
import org.springframework.stereotype.Service;

@Service
public class ProjectInfoService {
    private final ProjectInfoProperties projectInfoProperties;

    public ProjectInfoService(ProjectInfoProperties projectInfoProperties) {
        this.projectInfoProperties = projectInfoProperties;
    }

    public String getProjectVersion() {
        return projectInfoProperties.getVersion();
    }
}
