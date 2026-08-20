package br.unifil.campusflow.dto;

import br.unifil.campusflow.domain.Role;
import br.unifil.campusflow.domain.StatusRegistro;
import br.unifil.campusflow.domain.Usuario;

import java.util.Comparator;
import java.util.List;

public record UsuarioResponse(
    Long id,
    String nome,
    String email,
    Role role,
    StatusRegistro status,
    List<CursoResponse> cursos
) {
    public static UsuarioResponse from(Usuario u) {
        List<CursoResponse> cursos = u.getCursos().stream()
            .map(CursoResponse::from)
            .sorted(Comparator.comparing(CursoResponse::nome))
            .toList();
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getRole(), u.getStatus(), cursos);
    }
}
