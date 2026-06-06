package br.unifil.campusflow.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_proposta_troca")
public class PropostaTroca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reserva que o solicitante quer (pertence a outro professor)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_reserva_origem")
    private Reserva reservaOrigem;

    // Quem esta propondo a troca
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_solicitante")
    private Usuario usuarioSolicitante;

    @Column(nullable = false, length = 500)
    private String justificativa;

    @Column(nullable = false, length = 20)
    private String status = "PENDENTE";

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Column(name = "data_modificacao")
    private LocalDateTime dataModificacao;

    @Column(name = "data_exclusao")
    private LocalDateTime dataExclusao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Reserva getReservaOrigem() { return reservaOrigem; }
    public void setReservaOrigem(Reserva r) { this.reservaOrigem = r; }
    public Usuario getUsuarioSolicitante() { return usuarioSolicitante; }
    public void setUsuarioSolicitante(Usuario u) { this.usuarioSolicitante = u; }
    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String j) { this.justificativa = j; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime d) { this.dataCriacao = d; }
    public LocalDateTime getDataModificacao() { return dataModificacao; }
    public void setDataModificacao(LocalDateTime d) { this.dataModificacao = d; }
    public LocalDateTime getDataExclusao() { return dataExclusao; }
    public void setDataExclusao(LocalDateTime d) { this.dataExclusao = d; }
}
