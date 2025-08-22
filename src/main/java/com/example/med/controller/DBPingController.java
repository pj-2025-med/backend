package com.example.med.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;

@RestController
public class DBPingController {

    private final JdbcTemplate primaryJdbcTemplate;
    private final JdbcTemplate secondJdbcTemplate;

    @Autowired
    public DBPingController(
            @Qualifier("primaryDataSource") DataSource primaryDataSource,
            @Qualifier("secondDataSource") DataSource secondDataSource
    ) {
        this.primaryJdbcTemplate = new JdbcTemplate(primaryDataSource);
        this.secondJdbcTemplate = new JdbcTemplate(secondDataSource);
    }

    @GetMapping("/db/ping/primary")
    public String pingPrimary() {
        try {
            Integer v = primaryJdbcTemplate.queryForObject("SELECT 1 FROM dual", Integer.class);
            return "Primary DB OK: " + v;
        } catch (Exception e) {
            return "Primary DB Error: " + e.getMessage();
        }
    }

    @GetMapping("/db/ping/secondary")
    public String pingSecondary() {
        try {
            Integer v = secondJdbcTemplate.queryForObject("SELECT 1 FROM dual", Integer.class);
            return "Secondary DB OK: " + v;
        } catch (Exception e) {
            return "Secondary DB Error: " + e.getMessage();
        }
    }
}
