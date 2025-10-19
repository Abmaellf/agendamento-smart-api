package com.agendamento.smart.controller.info;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/build-info")
public class BuildInfoController {
    @Autowired
    private final BuildProperties buildProperties;

    public BuildInfoController(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping("/version")
    public String getVersion() {
        return buildProperties.getVersion();
    }
}
