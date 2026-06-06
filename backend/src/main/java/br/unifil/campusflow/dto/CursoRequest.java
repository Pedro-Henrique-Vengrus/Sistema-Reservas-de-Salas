package br.unifil.campusflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CursoRequest(
    @NotBlank(message = "Nome do curso e obrigatorio")
    @Size(max = 100)
    String nome
) {}
