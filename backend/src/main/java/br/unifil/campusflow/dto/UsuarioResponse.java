package br.unifil.campusflow.dto;

import br.unifil.campusflow.domain.Usuario;

public record UsuarioResponse(
    Long id,
    String nome,
    String email,
    String role
) {
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getRole().name());
    }
}
