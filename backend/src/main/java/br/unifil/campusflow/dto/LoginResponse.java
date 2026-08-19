package br.unifil.campusflow.dto;

import br.unifil.campusflow.domain.Role;
import br.unifil.campusflow.domain.Usuario;

import java.util.List;

public record LoginResponse(
    String token,
    Long id,
    String nome,
    String email,
    Role role,
    boolean administrativo,
    List<CursoResponse> cursos
) {
    public static LoginResponse from(String token, Usuario u) {
        return new LoginResponse(
            token, u.getId(), u.getNome(), u.getEmail(), u.getRole(), u.ehAdministrativo(),
            u.getCursos().stream().map(CursoResponse::from).toList());
    }
}
