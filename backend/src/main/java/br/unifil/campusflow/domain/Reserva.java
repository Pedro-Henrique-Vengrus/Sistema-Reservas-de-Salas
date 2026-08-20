package br.unifil.campusflow.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "tb_reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitante")
    private Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sala")
    private Sala sala;

    @Column(name = "data_reserva", nullable = false)
    private LocalDate dataReserva;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    // Derivado de horaInicio por Turno.de(...); persistido para permitir busca por turno
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Turno turno;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_reserva", nullable = false, length = 30)
    private TipoReserva tipoReserva = TipoReserva.GRADE_BIMESTRAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusReserva status = StatusReserva.PENDENTE_APROVACAO;

    @Column(length = 300)
    private String observacao;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Column(name = "data_modificacao")
    private LocalDateTime dataModificacao;

    @Column(name = "data_exclusao")
    private LocalDateTime dataExclusao;

    /** Mantem o turno coerente com o horario de inicio. */
    public void definirHorario(LocalTime inicio, LocalTime fim) {
        this.horaInicio = inicio;
        this.horaFim = fim;
        this.turno = Turno.de(inicio);
    }

    public boolean ehFutura(LocalDate hoje) {
        return !dataReserva.isBefore(hoje);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getSolicitante() { return solicitante; }
    public void setSolicitante(Usuario s) { this.solicitante = s; }
    public Sala getSala() { return sala; }
    public void setSala(Sala sala) { this.sala = sala; }
    public LocalDate getDataReserva() { return dataReserva; }
    public void setDataReserva(LocalDate d) { this.dataReserva = d; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFim() { return horaFim; }
    public Turno getTurno() { return turno; }
    public void setTurno(Turno turno) { this.turno = turno; }
    public TipoReserva getTipoReserva() { return tipoReserva; }
    public void setTipoReserva(TipoReserva t) { this.tipoReserva = t; }
    public StatusReserva getStatus() { return status; }
    public void setStatus(StatusReserva s) { this.status = s; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime d) { this.dataCriacao = d; }
    public LocalDateTime getDataModificacao() { return dataModificacao; }
    public void setDataModificacao(LocalDateTime d) { this.dataModificacao = d; }
    public LocalDateTime getDataExclusao() { return dataExclusao; }
    public void setDataExclusao(LocalDateTime d) { this.dataExclusao = d; }
}
