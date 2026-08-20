package br.unifil.campusflow.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Controle do periodo de preenchimento da grade bimestral (linha unica, id = 1).
 * Enquanto {@code aberto = false}, solicitantes nao lancam reservas GRADE_BIMESTRAL.
 */
@Entity
@Table(name = "tb_periodo_grade")
public class PeriodoGrade {

    public static final Long ID_UNICO = 1L;

    @Id
    private Long id = ID_UNICO;

    @Column(nullable = false)
    private Boolean aberto = false;

    @Column(length = 120)
    private String descricao;

    @Column(name = "inicio_vigencia")
    private LocalDate inicioVigencia;

    @Column(name = "fim_vigencia")
    private LocalDate fimVigencia;

    // EAGER de proposito: a resposta expoe o nome de quem liberou a grade e a tabela tem uma unica linha
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_atualizado_por")
    private Usuario atualizadoPor;

    @Column(name = "data_modificacao")
    private LocalDateTime dataModificacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Boolean getAberto() { return aberto; }
    public void setAberto(Boolean aberto) { this.aberto = aberto; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public LocalDate getInicioVigencia() { return inicioVigencia; }
    public void setInicioVigencia(LocalDate d) { this.inicioVigencia = d; }
    public LocalDate getFimVigencia() { return fimVigencia; }
    public void setFimVigencia(LocalDate d) { this.fimVigencia = d; }
    public Usuario getAtualizadoPor() { return atualizadoPor; }
    public void setAtualizadoPor(Usuario u) { this.atualizadoPor = u; }
    public LocalDateTime getDataModificacao() { return dataModificacao; }
    public void setDataModificacao(LocalDateTime d) { this.dataModificacao = d; }
}
