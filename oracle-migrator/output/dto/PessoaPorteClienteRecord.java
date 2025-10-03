package dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PessoaPorteClienteRecord(
    Long idPessoa,
    BigDecimal vlrRendimentoMensal,
    String porteAnterior,
    String novoPorte,
    LocalDate dataBase,
    String usuario
) {
    public PessoaPorteClienteRecord withUsuario(String usuario) {
        return new PessoaPorteClienteRecord(idPessoa, vlrRendimentoMensal, porteAnterior, novoPorte, dataBase, usuario);
    }
}
