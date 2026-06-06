package br.unifil.campusflow.repository;

import br.unifil.campusflow.domain.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // Reservas de um solicitante (nao canceladas), ordenadas
    @Query("SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.solicitante WHERE r.solicitante.id = :solicitanteId AND r.status <> 'CANCELADA' ORDER BY r.dataReserva DESC, r.horaInicio DESC")
    List<Reserva> findMinhasAtivas(@Param("solicitanteId") Long solicitanteId);

    // Reservas de uma sala numa data (para montar a agenda do dia)
    @Query("SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.solicitante WHERE r.sala.id = :salaId AND r.dataReserva = :data AND r.status <> 'CANCELADA'")
    List<Reserva> findDaSalaNaData(@Param("salaId") Long salaId, @Param("data") LocalDate data);

    // Deteccao de conflito: existe reserva ativa na mesma sala/data com sobreposicao de horario?
    @Query("""
        SELECT COUNT(r) > 0 FROM Reserva r
        WHERE r.sala.id = :salaId
          AND r.dataReserva = :data
          AND r.status <> 'CANCELADA'
          AND r.horaInicio < :horaFim
          AND r.horaFim > :horaInicio
        """)
    boolean existeConflito(@Param("salaId") Long salaId,
                           @Param("data") LocalDate data,
                           @Param("horaInicio") LocalTime horaInicio,
                           @Param("horaFim") LocalTime horaFim);

    // Reservas de OUTROS usuarios (para a aba "Propor Troca")
    @Query("SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.solicitante WHERE r.solicitante.id <> :usuarioId AND r.status <> 'CANCELADA' ORDER BY r.dataReserva DESC")
    List<Reserva> findDeOutros(@Param("usuarioId") Long usuarioId);

    @Query("SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.solicitante WHERE r.id = :id")
    java.util.Optional<Reserva> findByIdComDados(@Param("id") Long id);
}
