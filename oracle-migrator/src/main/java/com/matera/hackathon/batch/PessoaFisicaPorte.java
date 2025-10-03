package com.matera.hackathon.batch;

import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

public record PessoaFisicaPorte(
    Long idPessoa,
    BigDecimal vlrRendimentoMensal,
    String porteClienteAtual,
    String novoPorteCliente
) {
    public static class PessoaFisicaPorteRowMapper implements RowMapper<PessoaFisicaPorte> {
        @Override
        public PessoaFisicaPorte mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PessoaFisicaPorte(
                rs.getLong("id_pessoa"),
                rs.getBigDecimal("vlr_rendimento_mensal"),
                rs.getString("porte_cliente"),
                rs.getString("novo_porte")
            );
        }
    }
}

