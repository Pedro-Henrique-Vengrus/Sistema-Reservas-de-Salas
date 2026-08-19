package br.unifil.campusflow.repository;

import br.unifil.campusflow.domain.Curso;
import br.unifil.campusflow.domain.StatusRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CursoRepository extends JpaRepository<Curso, Long> {

    List<Curso> findByStatusOrderByNome(StatusRegistro status);

    List<Curso> findAllByOrderByNome();

    boolean existsByNomeIgnoreCase(String nome);

    @Query("SELECT COUNT(c) > 0 FROM Curso c WHERE LOWER(c.nome) = LOWER(:nome) AND c.id <> :id")
    boolean existsOutroComNome(@Param("nome") String nome, @Param("id") Long id);

    @Query("""
        SELECT c FROM Curso c
        WHERE (:status IS NULL OR c.status = :status)
          AND (CAST(:termo AS string) IS NULL OR LOWER(c.nome) LIKE LOWER(CONCAT('%', CAST(:termo AS string), '%'))
                              OR LOWER(COALESCE(c.sigla, '')) LIKE LOWER(CONCAT('%', CAST(:termo AS string), '%')))
        ORDER BY c.nome
        """)
    List<Curso> buscar(@Param("termo") String termo, @Param("status") StatusRegistro status);
}
