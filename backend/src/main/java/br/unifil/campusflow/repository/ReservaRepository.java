package br.unifil.campusflow.repository;

import br.unifil.campusflow.domain.Reserva;
import br.unifil.campusflow.domain.StatusReserva;
import br.unifil.campusflow.domain.TipoReserva;
import br.unifil.campusflow.domain.Turno;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    String ATIVAS = "r.status IN (br.unifil.campusflow.domain.StatusReserva.APROVADA, "
            + "br.unifil.campusflow.domain.StatusReserva.PENDENTE_APROVACAO)";

    @Query("SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.solicitante WHERE r.id = :id")
    Optional<Reserva> findByIdComDados(@Param("id") Long id);

    // Reservas do solicitante que ainda valem (nao canceladas/recusadas)
    @Query("SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.solicitante "
         + "WHERE r.solicitante.id = :solicitanteId AND " + ATIVAS
         + " ORDER BY r.dataReserva DESC, r.horaInicio DESC")
    List<Reserva> findMinhasAtivas(@Param("solicitanteId") Long solicitanteId);

    @Query("SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.solicitante "
         + "WHERE r.solicitante.id = :solicitanteId ORDER BY r.dataReserva DESC, r.horaInicio DESC")
    List<Reserva> findTodasDoSolicitante(@Param("solicitanteId") Long solicitanteId);

    // Agenda: reservas ativas de uma sala num intervalo de datas (grade semanal do frontend)
    @Query("SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.solicitante "
         + "WHERE r.sala.id IN :salaIds AND r.dataReserva BETWEEN :inicio AND :fim AND " + ATIVAS
         + " ORDER BY r.dataReserva, r.horaInicio")
    List<Reserva> findAgenda(@Param("salaIds") Collection<Long> salaIds,
                             @Param("inicio") LocalDate inicio,
                             @Param("fim") LocalDate fim);

    // Conflito de sala: ja existe reserva ativa sobrepondo o horario pedido?
    @Query("SELECT COUNT(r) > 0 FROM Reserva r "
         + "WHERE r.sala.id = :salaId AND r.dataReserva = :data AND " + ATIVAS
         + " AND r.horaInicio < :horaFim AND r.horaFim > :horaInicio")
    boolean existeConflito(@Param("salaId") Long salaId,
                           @Param("data") LocalDate data,
                           @Param("horaInicio") LocalTime horaInicio,
                           @Param("horaFim") LocalTime horaFim);

    // Idem, ignorando uma reserva especifica (usado ao revalidar uma reserva ja existente)
    @Query("SELECT COUNT(r) > 0 FROM Reserva r "
         + "WHERE r.sala.id = :salaId AND r.id <> :excluirReservaId AND r.dataReserva = :data AND " + ATIVAS
         + " AND r.horaInicio < :horaFim AND r.horaFim > :horaInicio")
    boolean existeConflitoExceto(@Param("salaId") Long salaId,
                                 @Param("data") LocalDate data,
                                 @Param("horaInicio") LocalTime horaInicio,
                                 @Param("horaFim") LocalTime horaFim,
                                 @Param("excluirReservaId") Long excluirReservaId);

    // Conflito na agenda PESSOAL do professor (qualquer sala), usado para validar a troca mutua
    @Query("SELECT COUNT(r) > 0 FROM Reserva r "
         + "WHERE r.solicitante.id = :usuarioId AND r.id <> :excluirReservaId "
         + "AND r.dataReserva = :data AND " + ATIVAS
         + " AND r.horaInicio < :horaFim AND r.horaFim > :horaInicio")
    boolean existeConflitoPessoal(@Param("usuarioId") Long usuarioId,
                                  @Param("data") LocalDate data,
                                  @Param("horaInicio") LocalTime horaInicio,
                                  @Param("horaFim") LocalTime horaFim,
                                  @Param("excluirReservaId") Long excluirReservaId);

    /**
     * Reservas de OUTROS professores passiveis de proposta de troca: aprovadas, futuras
     * e em sala visivel ao proponente. Dia e turno nao restringem mais o universo — apenas
     * definem se a troca se resolve entre professores ou precisa do aval do gestor.
     */
    @Query("""
        SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.solicitante
        WHERE r.solicitante.id <> :usuarioId
          AND r.status = br.unifil.campusflow.domain.StatusReserva.APROVADA
          AND r.dataReserva >= :hoje
          AND (:data IS NULL OR r.dataReserva = :data)
          AND (:turno IS NULL OR r.turno = :turno)
          AND EXISTS (SELECT 1 FROM Sala s JOIN s.cursos c
                       WHERE s.id = r.sala.id AND c.id IN :cursoIds
                         AND c.status = br.unifil.campusflow.domain.StatusRegistro.ATIVO)
        ORDER BY r.dataReserva, r.horaInicio
        """)
    List<Reserva> findElegiveisParaTroca(@Param("usuarioId") Long usuarioId,
                                         @Param("cursoIds") Collection<Long> cursoIds,
                                         @Param("hoje") LocalDate hoje,
                                         @Param("data") LocalDate data,
                                         @Param("turno") Turno turno);

    // Fila de moderacao do painel administrativo
    @Query("SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.solicitante "
         + "WHERE r.status = br.unifil.campusflow.domain.StatusReserva.PENDENTE_APROVACAO "
         + "ORDER BY r.dataCriacao ASC")
    List<Reserva> findFilaModeracao();

    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.status = br.unifil.campusflow.domain.StatusReserva.PENDENTE_APROVACAO")
    long countFilaModeracao();

    // ---------- Ciclo de vida de Sala e Curso ----------

    @Query("SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.solicitante "
         + "WHERE r.sala.id = :salaId AND r.dataReserva >= :hoje AND " + ATIVAS)
    List<Reserva> findFuturasAtivasDaSala(@Param("salaId") Long salaId, @Param("hoje") LocalDate hoje);

    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.sala.id = :salaId")
    long countDaSala(@Param("salaId") Long salaId);

    /**
     * Reservas futuras impactadas pela inativacao de um curso: a sala pertence ao curso
     * E o solicitante tambem esta vinculado a ele.
     */
    @Query("""
        SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.solicitante
        WHERE r.dataReserva >= :hoje
          AND r.status IN (br.unifil.campusflow.domain.StatusReserva.APROVADA,
                           br.unifil.campusflow.domain.StatusReserva.PENDENTE_APROVACAO)
          AND EXISTS (SELECT 1 FROM Sala s JOIN s.cursos c WHERE s.id = r.sala.id AND c.id = :cursoId)
          AND EXISTS (SELECT 1 FROM Usuario u JOIN u.cursos uc
                       WHERE u.id = r.solicitante.id AND uc.id = :cursoId)
        """)
    List<Reserva> findFuturasAtivasDoCurso(@Param("cursoId") Long cursoId, @Param("hoje") LocalDate hoje);

    // ---------- Relatorios / tabelas com filtros ----------

    @Query(value = """
        SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.solicitante
        WHERE (CAST(:inicio AS date) IS NULL OR r.dataReserva >= :inicio)
          AND (CAST(:fim AS date) IS NULL OR r.dataReserva <= :fim)
          AND (:salaId IS NULL OR r.sala.id = :salaId)
          AND (:usuarioId IS NULL OR r.solicitante.id = :usuarioId)
          AND (:status IS NULL OR r.status = :status)
          AND (:tipo IS NULL OR r.tipoReserva = :tipo)
          AND (:turno IS NULL OR r.turno = :turno)
          AND (:cursoId IS NULL OR EXISTS (SELECT 1 FROM Sala s JOIN s.cursos c
                                            WHERE s.id = r.sala.id AND c.id = :cursoId))
        """,
        countQuery = """
        SELECT COUNT(r) FROM Reserva r
        WHERE (CAST(:inicio AS date) IS NULL OR r.dataReserva >= :inicio)
          AND (CAST(:fim AS date) IS NULL OR r.dataReserva <= :fim)
          AND (:salaId IS NULL OR r.sala.id = :salaId)
          AND (:usuarioId IS NULL OR r.solicitante.id = :usuarioId)
          AND (:status IS NULL OR r.status = :status)
          AND (:tipo IS NULL OR r.tipoReserva = :tipo)
          AND (:turno IS NULL OR r.turno = :turno)
          AND (:cursoId IS NULL OR EXISTS (SELECT 1 FROM Sala s JOIN s.cursos c
                                            WHERE s.id = r.sala.id AND c.id = :cursoId))
        """)
    Page<Reserva> buscarComFiltros(@Param("inicio") LocalDate inicio,
                                   @Param("fim") LocalDate fim,
                                   @Param("cursoId") Long cursoId,
                                   @Param("salaId") Long salaId,
                                   @Param("usuarioId") Long usuarioId,
                                   @Param("status") StatusReserva status,
                                   @Param("tipo") TipoReserva tipo,
                                   @Param("turno") Turno turno,
                                   Pageable pageable);

    @Query("SELECT r.status, COUNT(r) FROM Reserva r WHERE r.dataReserva >= :inicio GROUP BY r.status")
    List<Object[]> contarPorStatusDesde(@Param("inicio") LocalDate inicio);

    @Query("SELECT r.sala.nome, COUNT(r) FROM Reserva r WHERE r.dataReserva >= :inicio AND " + ATIVAS
         + " GROUP BY r.sala.nome ORDER BY COUNT(r) DESC")
    List<Object[]> contarPorSalaDesde(@Param("inicio") LocalDate inicio);

    @Query("SELECT c.nome, COUNT(DISTINCT r.id) FROM Reserva r JOIN r.sala s JOIN s.cursos c "
         + "WHERE r.dataReserva >= :inicio AND " + ATIVAS + " GROUP BY c.nome ORDER BY COUNT(DISTINCT r.id) DESC")
    List<Object[]> contarPorCursoDesde(@Param("inicio") LocalDate inicio);

    @Query("SELECT r FROM Reserva r JOIN FETCH r.sala JOIN FETCH r.solicitante "
         + "WHERE r.dataReserva >= :hoje AND r.status = br.unifil.campusflow.domain.StatusReserva.APROVADA "
         + "ORDER BY r.dataReserva, r.horaInicio")
    List<Reserva> findProximasAprovadas(@Param("hoje") LocalDate hoje, Pageable pageable);
}
