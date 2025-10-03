package com.matera.hackathon.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Date;

@Service
public class LegadoService {

    private final JdbcTemplate jdbcTemplate;

    public LegadoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String call(String dataCliente) {
        LocalDate localDate = LocalDate.parse(dataCliente);
        Date sqlDate = java.sql.Date.valueOf(localDate);
        jdbcTemplate.update("call sBcAtualizaPorteCliente(?)", sqlDate);
        return "Success";
        }
}