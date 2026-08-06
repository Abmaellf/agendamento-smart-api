package com.agendamento.smart.controller.info;

import com.agendamento.smart.service.ProjectInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/build-info")
public class BuildInfoController {

    private final ObjectProvider<BuildProperties> buildProperties;
    private final ProjectInfoService projectInfoService;

    @GetMapping("/version")
    public String getVersion() {
        BuildProperties properties = buildProperties.getIfAvailable();
        return properties != null ? properties.getVersion() : projectInfoService.getProjectVersion();
    }
}
