package br.unifil.campusflow.dto;

import br.unifil.campusflow.domain.PeriodoGrade;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PeriodoGradeResponse(
    Boolean aberto,
    String descricao,
    LocalDate inicioVigencia,
    LocalDate fimVigencia,
    String atualizadoPor,
    LocalDateTime dataModificacao
) {
    public static PeriodoGradeResponse from(PeriodoGrade p) {
        return new PeriodoGradeResponse(
            p.getAberto(), p.getDescricao(), p.getInicioVigencia(), p.getFimVigencia(),
            p.getAtualizadoPor() != null ? p.getAtualizadoPor().getNome() : null,
            p.getDataModificacao());
    }
}
