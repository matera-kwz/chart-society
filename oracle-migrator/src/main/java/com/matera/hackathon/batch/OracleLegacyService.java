package com.matera.hackathon.batch;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Map;

@Service
public class OracleLegacyService {

    private final JdbcTemplate jdbcTemplate;

    public OracleLegacyService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void pSdLogDebug(String module, String procedure, String message, String mark) {
        // Assuming pSdLog.debug is a procedure. If it's a function, adjust accordingly.
        // This is a simplified example. A real implementation might use a StoredProcedure object
        // or a CallableStatementCreator for more complex parameter handling.
        String sql = "{call pSdLog.debug(?, ?, ?, ?)}";
        jdbcTemplate.update(sql, module, procedure, message, mark);
    }

    public void pSdMsgTrataExcecao(int sqlCode, String sqlErrm, String procedureName) {
        String sql = "{call pSdMsg.trataExcecao(?, ?, ?)}";
        jdbcTemplate.update(sql, sqlCode, sqlErrm, procedureName);
    }

    public String sSdBuscaUsuario() {
        // Assuming sSdBuscaUsuario is a function that returns a VARCHAR2
        String sql = "SELECT sSdBuscaUsuario FROM DUAL";
        return jdbcTemplate.queryForObject(sql, String.class);
    }

    public LocalDate sBcRoundDiaUtil(LocalDate date) {
        // Assuming sBcRoundDiaUtil is a function that returns a DATE
        String sql = "SELECT sBcRoundDiaUtil(?) FROM DUAL";
        return jdbcTemplate.queryForObject(sql, LocalDate.class, date);
    }

    public String sBcRetornaPorteClientePF(BigDecimal vlrRendimentoMensal, BigDecimal vnSalarioMinimo) {
        // Assuming sBcRetornaPorteClientePF is a function that returns a VARCHAR2
        String sql = "SELECT sBcRetornaPorteClientePF(?, ?) FROM DUAL";
        return jdbcTemplate.queryForObject(sql, String.class, vlrRendimentoMensal, vnSalarioMinimo);
    }

    // Example of how to call a stored procedure with output parameters if needed
    private static class GetSalarioMinimoProcedure extends StoredProcedure {
        public GetSalarioMinimoProcedure(DataSource dataSource) {
            super(dataSource, "bc_configuracao_pkg.get_salario_minimo"); // Assuming a package and procedure
            declareParameter(new SqlOutParameter("p_salario_minimo", Types.NUMERIC));
            compile();
        }

        public BigDecimal execute() {
            Map<String, Object> out = super.execute();
            return (BigDecimal) out.get("p_salario_minimo");
        }
    }
}

