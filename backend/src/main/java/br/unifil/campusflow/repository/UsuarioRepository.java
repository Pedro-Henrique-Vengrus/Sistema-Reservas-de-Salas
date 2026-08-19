package br.unifil.campusflow.repository;

import br.unifil.campusflow.domain.Role;
import br.unifil.campusflow.domain.StatusRegistro;
import br.unifil.campusflow.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.cursos WHERE u.email = :email")
    Optional<Usuario> findByEmail(@Param("email") String email);

    @Query("SELECT DISTINCT u FROM Usuario u LEFT JOIN FETCH u.cursos WHERE u.id = :id")
    Optional<Usuario> findByIdComCursos(@Param("id") Long id);

    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT COUNT(u) > 0 FROM Usuario u WHERE LOWER(u.email) = LOWER(:email) AND u.id <> :id")
    boolean existsOutroComEmail(@Param("email") String email, @Param("id") Long id);

    /** Solicitantes ativos: quem pode receber uma reserva lancada pelo painel administrativo. */
    @Query("""
        SELECT DISTINCT u FROM Usuario u LEFT JOIN FETCH u.cursos
        WHERE u.status = br.unifil.campusflow.domain.StatusRegistro.ATIVO
          AND u.role <> br.unifil.campusflow.domain.Role.ADMIN
        ORDER BY u.nome
        """)
    List<Usuario> findSolicitantesAtivos();

    @Query("""
        SELECT DISTINCT u FROM Usuario u LEFT JOIN FETCH u.cursos
        WHERE (:status IS NULL OR u.status = :status)
          AND (:role IS NULL OR u.role = :role)
          AND (CAST(:termo AS string) IS NULL OR LOWER(u.nome) LIKE LOWER(CONCAT('%', CAST(:termo AS string), '%'))
                              OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:termo AS string), '%')))
          AND (:cursoId IS NULL OR EXISTS (SELECT 1 FROM Usuario u2 JOIN u2.cursos c2
                                            WHERE u2.id = u.id AND c2.id = :cursoId))
        ORDER BY u.nome
        """)
    List<Usuario> buscar(@Param("termo") String termo,
                         @Param("role") Role role,
                         @Param("status") StatusRegistro status,
                         @Param("cursoId") Long cursoId);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.status = br.unifil.campusflow.domain.StatusRegistro.ATIVO")
    long countAtivos();
}
