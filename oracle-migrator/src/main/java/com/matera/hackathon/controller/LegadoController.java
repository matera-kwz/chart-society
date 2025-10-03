package com.matera.hackathon.controller;

import com.matera.hackathon.service.LegadoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/legado")
public class LegadoController {

    private final LegadoService legadoService;

    public LegadoController(LegadoService legadoService) {
        this.legadoService = legadoService;
    }

    @PostMapping()
    public ResponseEntity<String> getLegadoData(@RequestParam String dataCliente) {
        String dados = legadoService.call(dataCliente);
        return new ResponseEntity<>(dados, HttpStatus.OK);
    }
}