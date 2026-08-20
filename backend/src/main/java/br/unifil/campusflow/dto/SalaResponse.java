package br.unifil.campusflow.dto;

import br.unifil.campusflow.domain.Sala;
import br.unifil.campusflow.domain.StatusRegistro;
import br.unifil.campusflow.domain.TipoAmbiente;

import java.util.Comparator;
import java.util.List;

public record SalaResponse(
    Long id, String nome, String codigo, TipoAmbiente tipo, String tipoRotulo,
    Integer capacidade, String andar, StatusRegistro status, List<CursoResponse> cursos
) {
    public static SalaResponse from(Sala s) {
        List<CursoResponse> cs = s.getCursos().stream()
            .map(CursoResponse::from)
            .sorted(Comparator.comparing(CursoResponse::nome))
            .toList();
        TipoAmbiente tipo = s.getTipo();
        return new SalaResponse(s.getId(), s.getNome(), s.getCodigo(), tipo,
            tipo != null ? tipo.getRotulo() : null,
            s.getCapacidade(), s.getAndar(), s.getStatus(), cs);
    }
}
