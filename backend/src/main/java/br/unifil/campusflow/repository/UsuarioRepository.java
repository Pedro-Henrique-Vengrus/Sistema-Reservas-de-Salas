package br.unifil.campusflow.repository;

import br.unifil.campusflow.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    // Usuarios que podem receber uma reserva feita pelo Admin (professores/coordenadores ativos, sem contar Admin/Gestor)
    @Query("SELECT u FROM Usuario u WHERE u.ativo = true AND u.role NOT IN ('ADMIN', 'GESTOR') ORDER BY u.nome")
    List<Usuario> findElegiveisParaReserva();
}
