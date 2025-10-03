package service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class OracleWrapperService {
    private final JdbcTemplate jdbcTemplate;

    public OracleWrapperService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void logDebug(String schema, String proc, String message) {
        // Call pSdLog.debug Oracle procedure
        jdbcTemplate.update("CALL pSdLog_debug(?, ?, ?)", schema, proc, message);
    }

    public Configuracao getConfiguracao() {
        return jdbcTemplate.queryForObject(
            "SELECT NVL(ind_atualiza_porte_cliente, 'N') AS indAtualizaPorteCliente, salario_minimo FROM bc_configuracao",
            (rs, rowNum) -> new Configuracao(
                rs.getString("indAtualizaPorteCliente"),
                rs.getDouble("salario_minimo")
            )
        );
    }

    public LocalDate roundDiaUtil(LocalDate date) {
        // Call sBcRoundDiaUtil Oracle function
        return jdbcTemplate.queryForObject("SELECT sBcRoundDiaUtil(?) FROM dual", LocalDate.class, date);
    }

    public String getUsuario() {
        // Call sSdBuscaUsuario Oracle function
        return jdbcTemplate.queryForObject("SELECT sSdBuscaUsuario FROM dual", String.class);
    }

    public record Configuracao(String indAtualizaPorteCliente, Double salarioMinimo) {}
}
