package br.unifil.campusflow.repository;

import br.unifil.campusflow.domain.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CursoRepository extends JpaRepository<Curso, Long> {
    List<Curso> findByAtivoTrue();
    boolean existsByNomeIgnoreCase(String nome);
}
