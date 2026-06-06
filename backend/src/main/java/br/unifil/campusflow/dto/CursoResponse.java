package br.unifil.campusflow.dto;

import br.unifil.campusflow.domain.Curso;

public record CursoResponse(Long id, String nome, Boolean ativo) {
    public static CursoResponse from(Curso c) {
        return new CursoResponse(c.getId(), c.getNome(), c.getAtivo());
    }
}
