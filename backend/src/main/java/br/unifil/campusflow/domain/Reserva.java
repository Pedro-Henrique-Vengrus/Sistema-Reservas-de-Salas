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

    @Column(name = "tipo_reserva", nullable = false, length = 50)
    private String tipoReserva = "GRADE_BIMESTRAL";

    @Column(nullable = false, length = 50)
    private String status = "PENDENTE";

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Column(name = "data_modificacao")
    private LocalDateTime dataModificacao;

    @Column(name = "data_exclusao")
    private LocalDateTime dataExclusao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getSolicitante() { return solicitante; }
    public void setSolicitante(Usuario s) { this.solicitante = s; }
    public Sala getSala() { return sala; }
    public void setSala(Sala sala) { this.sala = sala; }
    public LocalDate getDataReserva() { return dataReserva; }
    public void setDataReserva(LocalDate d) { this.dataReserva = d; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime h) { this.horaInicio = h; }
    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime h) { this.horaFim = h; }
    public String getTipoReserva() { return tipoReserva; }
    public void setTipoReserva(String t) { this.tipoReserva = t; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime d) { this.dataCriacao = d; }
    public LocalDateTime getDataModificacao() { return dataModificacao; }
    public void setDataModificacao(LocalDateTime d) { this.dataModificacao = d; }
    public LocalDateTime getDataExclusao() { return dataExclusao; }
    public void setDataExclusao(LocalDateTime d) { this.dataExclusao = d; }
}
