package br.unifil.campusflow.dto;

import br.unifil.campusflow.domain.Curso;
import br.unifil.campusflow.domain.StatusRegistro;

public record CursoResponse(Long id, String nome, String sigla, StatusRegistro status) {
    public static CursoResponse from(Curso c) {
        return new CursoResponse(c.getId(), c.getNome(), c.getSigla(), c.getStatus());
    }
}
