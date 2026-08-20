package br.unifil.campusflow.dto;

import br.unifil.campusflow.domain.Role;
import jakarta.validation.constraints.*;

import java.util.Set;

public record UsuarioRequest(
    @NotBlank(message = "Nome e obrigatorio")
    @Size(max = 255)
    String nome,

    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email invalido")
    String email,

    // Obrigatoria na criacao; na edicao, em branco mantem a senha atual
    @Size(min = 3, max = 100, message = "A senha deve ter ao menos 3 caracteres")
    String senha,

    @NotNull(message = "Perfil e obrigatorio")
    Role role,

    Set<Long> cursoIds
) {}
