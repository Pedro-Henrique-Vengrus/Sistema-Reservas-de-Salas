package br.unifil.campusflow.repository;

import br.unifil.campusflow.domain.Sala;
import br.unifil.campusflow.domain.StatusRegistro;
import br.unifil.campusflow.domain.TipoAmbiente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SalaRepository extends JpaRepository<Sala, Long> {

    @Query("SELECT DISTINCT s FROM Sala s LEFT JOIN FETCH s.cursos WHERE s.id = :id")
    Optional<Sala> findByIdComCursos(@Param("id") Long id);

    /**
     * Catalogo completo (visao administrativa), com filtros opcionais.
     * O universo do solicitante e recortado por {@link #buscarVisiveis}.
     */
    @Query("""
        SELECT DISTINCT s FROM Sala s
        LEFT JOIN FETCH s.cursos
        WHERE (:status IS NULL OR s.status = :status)
          AND (:tipo IS NULL OR s.tipo = :tipo)
          AND (:capacidadeMinima IS NULL OR s.capacidade >= :capacidadeMinima)
          AND (CAST(:termo AS string) IS NULL OR LOWER(s.nome) LIKE LOWER(CONCAT('%', CAST(:termo AS string), '%'))
                              OR LOWER(COALESCE(s.codigo, '')) LIKE LOWER(CONCAT('%', CAST(:termo AS string), '%')))
          AND (:cursoId IS NULL OR EXISTS (SELECT 1 FROM Sala s2 JOIN s2.cursos c2
                                            WHERE s2.id = s.id AND c2.id = :cursoId))
        ORDER BY s.nome
        """)
    List<Sala> buscar(@Param("termo") String termo,
                      @Param("status") StatusRegistro status,
                      @Param("tipo") TipoAmbiente tipo,
                      @Param("capacidadeMinima") Integer capacidadeMinima,
                      @Param("cursoId") Long cursoId);

    /**
     * Visibilidade setorizada: apenas salas ATIVAS ligadas a pelo menos um dos cursos do solicitante.
     * Cursos inativos deixam de conceder visibilidade.
     */
    @Query("""
        SELECT DISTINCT s FROM Sala s
        LEFT JOIN FETCH s.cursos
        WHERE s.status = br.unifil.campusflow.domain.StatusRegistro.ATIVO
          AND EXISTS (SELECT 1 FROM Sala s2 JOIN s2.cursos c2
                       WHERE s2.id = s.id
                         AND c2.id IN :cursoIds
                         AND c2.status = br.unifil.campusflow.domain.StatusRegistro.ATIVO)
          AND (:tipo IS NULL OR s.tipo = :tipo)
          AND (:capacidadeMinima IS NULL OR s.capacidade >= :capacidadeMinima)
          AND (CAST(:termo AS string) IS NULL OR LOWER(s.nome) LIKE LOWER(CONCAT('%', CAST(:termo AS string), '%'))
                              OR LOWER(COALESCE(s.codigo, '')) LIKE LOWER(CONCAT('%', CAST(:termo AS string), '%')))
          AND (:cursoId IS NULL OR EXISTS (SELECT 1 FROM Sala s3 JOIN s3.cursos c3
                                            WHERE s3.id = s.id AND c3.id = :cursoId))
        ORDER BY s.nome
        """)
    List<Sala> buscarVisiveis(@Param("cursoIds") Collection<Long> cursoIds,
                              @Param("termo") String termo,
                              @Param("tipo") TipoAmbiente tipo,
                              @Param("capacidadeMinima") Integer capacidadeMinima,
                              @Param("cursoId") Long cursoId);

    /** Regra central de acesso: a sala pertence ao escopo de algum curso ativo do usuario? */
    @Query("""
        SELECT COUNT(s) > 0 FROM Sala s JOIN s.cursos c
        WHERE s.id = :salaId
          AND s.status = br.unifil.campusflow.domain.StatusRegistro.ATIVO
          AND c.id IN :cursoIds
          AND c.status = br.unifil.campusflow.domain.StatusRegistro.ATIVO
        """)
    boolean ehVisivelPara(@Param("salaId") Long salaId, @Param("cursoIds") Collection<Long> cursoIds);

    @Query("SELECT COUNT(s) > 0 FROM Sala s JOIN s.cursos c WHERE s.id = :salaId AND c.id = :cursoId")
    boolean vinculadaAoCurso(@Param("salaId") Long salaId, @Param("cursoId") Long cursoId);
}
