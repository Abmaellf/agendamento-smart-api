package com.agendamento.smart.controller.info;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/build-info")
public class BuildInfoController {


    private final BuildProperties buildProperties;

    @GetMapping("/version")
    public String getVersion() {
        return buildProperties.getVersion();
    }
}
