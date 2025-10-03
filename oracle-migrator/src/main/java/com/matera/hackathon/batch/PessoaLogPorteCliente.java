package com.matera.hackathon.batch;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PessoaLogPorteCliente(
    Long idPessoa,
    LocalDate dataBase,
    LocalDateTime dataEntrada,
    String usuario,
    String porteAnterior,
    String porteAtual
) {
    public static class PessoaLogPorteClienteRowMapper implements RowMapper<PessoaLogPorteCliente> {
        @Override
        public PessoaLogPorteCliente mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PessoaLogPorteCliente(
                rs.getLong("id_pessoa"),
                rs.getDate("data_base").toLocalDate(),
                rs.getTimestamp("data_entrada").toLocalDateTime(),
                rs.getString("usuario"),
                rs.getString("porte_anterior"),
                rs.getString("porte_atual")
            );
        }
    }
}

