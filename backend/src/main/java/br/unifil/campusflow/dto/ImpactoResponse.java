package br.unifil.campusflow.dto;

import java.util.List;
import java.util.Map;

/**
 * Previa do efeito colateral de inativar/excluir um cadastro.
 * Alimenta o dialogo de confirmacao do painel administrativo.
 */
public record ImpactoResponse(
    Long id,
    String nome,
    long reservasFuturasAtivas,
    long reservasTotais,
    long usuariosVinculados,
    boolean podeInativarSemForcar,
    boolean podeExcluirFisicamente,
    String bloqueio,
    List<ReservaResponse> amostraReservas
) {
    public Map<String, Object> comoDetalhes() {
        return Map.of(
            "id", id,
            "nome", nome,
            "reservasFuturasAtivas", reservasFuturasAtivas,
            "reservasTotais", reservasTotais,
            "usuariosVinculados", usuariosVinculados,
            "podeExcluirFisicamente", podeExcluirFisicamente
        );
    }
}
