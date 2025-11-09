package com.agendamento.smart.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DebugController {

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping("/debug/db")
    public Map<String, Object> db() {
        return jdbc.queryForMap("SELECT DATABASE()");
    }
}
