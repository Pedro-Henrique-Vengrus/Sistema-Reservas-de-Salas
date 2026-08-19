package br.unifil.campusflow.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_sala")
public class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 30)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoAmbiente tipo = TipoAmbiente.SALA_AULA;

    @Column(nullable = false)
    private Integer capacidade = 0;

    @Column(length = 50)
    private String andar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusRegistro status = StatusRegistro.ATIVO;

    // Visibilidade setorizada: N:N com Curso
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "tb_sala_curso",
        joinColumns = @JoinColumn(name = "id_sala"),
        inverseJoinColumns = @JoinColumn(name = "id_curso")
    )
    private Set<Curso> cursos = new HashSet<>();

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Column(name = "data_modificacao")
    private LocalDateTime dataModificacao;

    @Column(name = "data_desativacao")
    private LocalDateTime dataDesativacao;

    public boolean estaAtiva() {
        return status == StatusRegistro.ATIVO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public TipoAmbiente getTipo() { return tipo; }
    public void setTipo(TipoAmbiente tipo) { this.tipo = tipo; }
    public Integer getCapacidade() { return capacidade; }
    public void setCapacidade(Integer c) { this.capacidade = c; }
    public String getAndar() { return andar; }
    public void setAndar(String andar) { this.andar = andar; }
    public StatusRegistro getStatus() { return status; }
    public void setStatus(StatusRegistro status) { this.status = status; }
    public Set<Curso> getCursos() { return cursos; }
    public void setCursos(Set<Curso> cursos) { this.cursos = cursos; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime d) { this.dataCriacao = d; }
    public LocalDateTime getDataModificacao() { return dataModificacao; }
    public void setDataModificacao(LocalDateTime d) { this.dataModificacao = d; }
    public LocalDateTime getDataDesativacao() { return dataDesativacao; }
    public void setDataDesativacao(LocalDateTime d) { this.dataDesativacao = d; }
}
