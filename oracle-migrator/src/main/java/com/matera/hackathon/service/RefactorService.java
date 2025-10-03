package com.matera.hackathon.service;

import org.springframework.stereotype.Service;

@Service
public class RefactorService {

    public String obterDadosRefatorados() {
        // Lógica de negócio e acesso ao banco de dados (Repository)
        // Usaria o Oracle para buscar dados com a nova estrutura de tabelas
        return "Dados Refatorados (Service) - Conectando ao Oracle...";
    }
}