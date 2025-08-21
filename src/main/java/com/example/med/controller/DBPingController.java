package com.example.med.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DBPingController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/db/ping")
    public String ping() {
        Integer v = jdbcTemplate.queryForObject("SELECT 1 FROM dual", Integer.class);
        return "DB OK: " + v;
    }
}