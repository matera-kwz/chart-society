package service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class OracleMessageService {
    private final JdbcTemplate jdbcTemplate;

    public OracleMessageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void trataExcecao(Exception e, String procName) {
        // Call pSdMsg.trataExcecao Oracle procedure
        jdbcTemplate.update("CALL pSdMsg_trataExcecao(?, ?, ?)",
            e instanceof java.sql.SQLException ? ((java.sql.SQLException) e).getErrorCode() : -1,
            e.getMessage(),
            procName);
        throw new OracleBusinessException(e.getMessage(), e);
    }

    public static class OracleBusinessException extends RuntimeException {
        public OracleBusinessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
