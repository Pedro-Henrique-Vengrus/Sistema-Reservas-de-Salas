package br.unifil.campusflow.repository;

import br.unifil.campusflow.domain.Sala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SalaRepository extends JpaRepository<Sala, Long> {
    @Query("SELECT DISTINCT s FROM Sala s LEFT JOIN FETCH s.cursos WHERE s.ativo = true")
    List<Sala> findByAtivoTrue();

    // Salas visiveis para um curso (visibilidade setorizada)
    @Query("SELECT DISTINCT s FROM Sala s LEFT JOIN FETCH s.cursos c WHERE s.id IN (SELECT s2.id FROM Sala s2 JOIN s2.cursos c2 WHERE c2.id = :cursoId) AND s.ativo = true")
    List<Sala> findVisiveisPorCurso(@Param("cursoId") Long cursoId);
}
