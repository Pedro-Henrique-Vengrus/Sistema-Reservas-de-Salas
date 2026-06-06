package br.unifil.campusflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PropostaRequest(
    @NotNull Long reservaOrigemId,
    @NotBlank(message = "A justificativa e obrigatoria")
    @Size(max = 500)
    String justificativa
) {}
