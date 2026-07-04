package br.unifil.campusflow.repository;

import br.unifil.campusflow.domain.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // Reservas de um solicitante (nao canceladas/recusadas), ordenadas
    @Query("SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.solicitante WHERE r.solicitante.id = :solicitanteId AND r.status NOT IN ('CANCELADA','RECUSADA') ORDER BY r.dataReserva DESC, r.horaInicio DESC")
    List<Reserva> findMinhasAtivas(@Param("solicitanteId") Long solicitanteId);

    // Reservas de uma sala numa data (para montar a agenda do dia)
    @Query("SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.solicitante WHERE r.sala.id = :salaId AND r.dataReserva = :data AND r.status NOT IN ('CANCELADA','RECUSADA')")
    List<Reserva> findDaSalaNaData(@Param("salaId") Long salaId, @Param("data") LocalDate data);

    // Deteccao de conflito: existe reserva ativa na mesma sala/data com sobreposicao de horario?
    @Query("""
        SELECT COUNT(r) > 0 FROM Reserva r
        WHERE r.sala.id = :salaId
          AND r.dataReserva = :data
          AND r.status NOT IN ('CANCELADA','RECUSADA')
          AND r.horaInicio < :horaFim
          AND r.horaFim > :horaInicio
        """)
    boolean existeConflito(@Param("salaId") Long salaId,
                           @Param("data") LocalDate data,
                           @Param("horaInicio") LocalTime horaInicio,
                           @Param("horaFim") LocalTime horaFim);

    // Deteccao de conflito de agenda PESSOAL do professor (qualquer sala), excluindo uma reserva especifica
    // Usado para validar a troca mutua: cada professor so pode assumir o novo horario se nao tiver outro compromisso sobreposto
    @Query("""
        SELECT COUNT(r) > 0 FROM Reserva r
        WHERE r.solicitante.id = :usuarioId
          AND r.id <> :excluirReservaId
          AND r.dataReserva = :data
          AND r.status NOT IN ('CANCELADA','RECUSADA')
          AND r.horaInicio < :horaFim
          AND r.horaFim > :horaInicio
        """)
    boolean existeConflitoPessoal(@Param("usuarioId") Long usuarioId,
                                  @Param("data") LocalDate data,
                                  @Param("horaInicio") LocalTime horaInicio,
                                  @Param("horaFim") LocalTime horaFim,
                                  @Param("excluirReservaId") Long excluirReservaId);

    // Reservas de OUTROS usuarios ja aprovadas (para a aba "Propor Troca")
    @Query("SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.solicitante WHERE r.solicitante.id <> :usuarioId AND r.status = 'CONFIRMADA' ORDER BY r.dataReserva DESC")
    List<Reserva> findDeOutros(@Param("usuarioId") Long usuarioId);

    @Query("SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.solicitante WHERE r.id = :id")
    java.util.Optional<Reserva> findByIdComDados(@Param("id") Long id);

    // Fila de aprovacao do Admin
    @Query("SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.solicitante WHERE r.status = 'PENDENTE' ORDER BY r.dataCriacao ASC")
    List<Reserva> findPendentes();

    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.status = 'PENDENTE'")
    long countPendentes();

    // Todas as reservas do sistema, qualquer usuario/status (aba "Todas as Reservas" do Admin)
    @Query("SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.solicitante ORDER BY r.dataReserva DESC, r.horaInicio DESC")
    List<Reserva> findTodas();
}
