package br.unifil.campusflow.dto;

import jakarta.validation.constraints.*;
import java.util.Set;

public record SalaRequest(
    @NotBlank(message = "Nome da sala e obrigatorio")
    @Size(max = 100)
    String nome,

    @Size(max = 50)
    String tipo,

    @NotNull @PositiveOrZero
    Integer capacidade,

    @Size(max = 50)
    String andar,

    // IDs dos cursos que enxergam esta sala (visibilidade setorizada)
    Set<Long> cursoIds
) {}
