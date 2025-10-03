package com.matera.hackathon.controller;

import com.matera.hackathon.batch.AtualizaPorteClienteService;
import com.matera.hackathon.service.RefactorService;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/refactor")
public class RefactorController {

    private final AtualizaPorteClienteService atualizaPorteClienteService;

    public RefactorController(AtualizaPorteClienteService atualizaPorteClienteService) {
        this.atualizaPorteClienteService = atualizaPorteClienteService;
    }

    @PostMapping()
    public ResponseEntity<String> getRefactorData(@RequestParam String dataCliente) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        atualizaPorteClienteService.sBcAtualizaPorteCliente(LocalDate.parse(dataCliente));
        return new ResponseEntity<>(HttpStatus.OK);
    }
}