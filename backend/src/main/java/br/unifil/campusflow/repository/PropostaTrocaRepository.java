package br.unifil.campusflow.repository;

import br.unifil.campusflow.domain.PropostaTroca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PropostaTrocaRepository extends JpaRepository<PropostaTroca, Long> {

    String FETCH = """
        SELECT p FROM PropostaTroca p
        JOIN FETCH p.reservaOrigem r JOIN FETCH r.sala JOIN FETCH r.solicitante
        JOIN FETCH p.usuarioSolicitante
        LEFT JOIN FETCH p.reservaOferecida o LEFT JOIN FETCH o.sala
        """;

    // Propostas enviadas por mim
    // LEFT JOIN FETCH em reservaOferecida: propostas antigas (pre troca-mutua) podem nao ter essa reserva
    @Query(FETCH + " WHERE p.usuarioSolicitante.id = :usuarioId ORDER BY p.dataCriacao DESC")
    List<PropostaTroca> findEnviadas(@Param("usuarioId") Long usuarioId);

    // Propostas recebidas: onde a reserva alvo e minha
    @Query(FETCH + " WHERE r.solicitante.id = :donoId ORDER BY p.dataCriacao DESC")
    List<PropostaTroca> findRecebidas(@Param("donoId") Long donoId);

    @Query(FETCH + " WHERE p.id = :id")
    Optional<PropostaTroca> findByIdComDados(@Param("id") Long id);

    // Contagem de pendentes recebidas (badge)
    @Query("SELECT COUNT(p) FROM PropostaTroca p WHERE p.reservaOrigem.solicitante.id = :donoId "
         + "AND p.status = br.unifil.campusflow.domain.StatusProposta.PENDENTE")
    long countPendentesRecebidas(@Param("donoId") Long donoId);

    // Outras propostas pendentes que envolvem qualquer uma das reservas informadas (origem ou oferecida),
    // usada para invalidar propostas concorrentes apos uma troca ser aceita
    @Query("""
        SELECT p FROM PropostaTroca p
        JOIN FETCH p.reservaOrigem r JOIN FETCH r.solicitante
        JOIN FETCH p.usuarioSolicitante
        WHERE p.status = br.unifil.campusflow.domain.StatusProposta.PENDENTE
          AND p.id <> :excluirId
          AND (p.reservaOrigem.id IN :reservaIds OR p.reservaOferecida.id IN :reservaIds)
        """)
    List<PropostaTroca> findPendentesEnvolvendo(@Param("reservaIds") List<Long> reservaIds,
                                                @Param("excluirId") Long excluirId);

    /** Propostas pendentes atingidas pelo cancelamento de uma reserva. */
    @Query("""
        SELECT p FROM PropostaTroca p
        JOIN FETCH p.reservaOrigem r JOIN FETCH r.solicitante
        JOIN FETCH p.usuarioSolicitante
        WHERE p.status = br.unifil.campusflow.domain.StatusProposta.PENDENTE
          AND (p.reservaOrigem.id = :reservaId OR p.reservaOferecida.id = :reservaId)
        """)
    List<PropostaTroca> findPendentesDaReserva(@Param("reservaId") Long reservaId);

    @Query("SELECT COUNT(p) FROM PropostaTroca p WHERE p.reservaOrigem.id IN :reservaIds "
         + "OR p.reservaOferecida.id IN :reservaIds")
    long countEnvolvendo(@Param("reservaIds") List<Long> reservaIds);
}
