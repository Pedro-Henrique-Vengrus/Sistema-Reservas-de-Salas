package br.unifil.campusflow.dto;

import br.unifil.campusflow.domain.TipoAmbiente;
import jakarta.validation.constraints.*;

import java.util.Set;

public record SalaRequest(
    @NotBlank(message = "Nome do ambiente e obrigatorio")
    @Size(max = 100)
    String nome,

    @Size(max = 30)
    String codigo,

    @NotNull(message = "Tipo do ambiente e obrigatorio")
    TipoAmbiente tipo,

    @NotNull @PositiveOrZero
    Integer capacidade,

    @Size(max = 50)
    String andar,

    // IDs dos cursos que enxergam este ambiente (visibilidade setorizada)
    @NotEmpty(message = "Vincule ao menos um curso ao ambiente")
    Set<Long> cursoIds
) {}
