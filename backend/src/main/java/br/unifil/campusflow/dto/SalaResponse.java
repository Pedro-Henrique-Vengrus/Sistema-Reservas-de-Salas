package br.unifil.campusflow.dto;

import br.unifil.campusflow.domain.Curso;
import br.unifil.campusflow.domain.Sala;
import java.util.Set;
import java.util.stream.Collectors;

public record SalaResponse(
    Long id, String nome, String tipo, Integer capacidade,
    String andar, String situacao, Boolean ativo, Set<CursoResponse> cursos
) {
    public static SalaResponse from(Sala s) {
        Set<CursoResponse> cs = s.getCursos().stream()
            .map(CursoResponse::from)
            .collect(Collectors.toSet());
        return new SalaResponse(s.getId(), s.getNome(), s.getTipo(), s.getCapacidade(),
            s.getAndar(), s.getSituacao(), s.getAtivo(), cs);
    }
}
