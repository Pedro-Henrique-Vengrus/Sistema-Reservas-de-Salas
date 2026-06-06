package br.unifil.campusflow.repository;

import br.unifil.campusflow.domain.PropostaTroca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PropostaTrocaRepository extends JpaRepository<PropostaTroca, Long> {

    // Propostas enviadas por mim
    @Query("SELECT p FROM PropostaTroca p JOIN FETCH p.reservaOrigem r JOIN FETCH r.sala JOIN FETCH r.solicitante JOIN FETCH p.usuarioSolicitante WHERE p.usuarioSolicitante.id = :usuarioId ORDER BY p.dataCriacao DESC")
    List<PropostaTroca> findEnviadas(@Param("usuarioId") Long usuarioId);

    // Propostas recebidas: onde a reserva alvo e minha
    @Query("SELECT p FROM PropostaTroca p JOIN FETCH p.reservaOrigem r JOIN FETCH r.sala JOIN FETCH r.solicitante JOIN FETCH p.usuarioSolicitante WHERE r.solicitante.id = :donoId ORDER BY p.dataCriacao DESC")
    List<PropostaTroca> findRecebidas(@Param("donoId") Long donoId);

    // Contagem de pendentes recebidas (badge)
    @Query("SELECT COUNT(p) FROM PropostaTroca p WHERE p.reservaOrigem.solicitante.id = :donoId AND p.status = 'PENDENTE'")
    long countPendentesRecebidas(@Param("donoId") Long donoId);

    @Query("SELECT p FROM PropostaTroca p JOIN FETCH p.reservaOrigem r JOIN FETCH r.sala JOIN FETCH r.solicitante JOIN FETCH p.usuarioSolicitante WHERE p.id = :id")
    java.util.Optional<PropostaTroca> findByIdComDados(@Param("id") Long id);
}
