package br.unifil.campusflow.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    // Visibilidade setorizada: um usuario pode pertencer a varios cursos
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "tb_usuario_curso",
        joinColumns = @JoinColumn(name = "id_usuario"),
        inverseJoinColumns = @JoinColumn(name = "id_curso")
    )
    private Set<Curso> cursos = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusRegistro status = StatusRegistro.ATIVO;

    // Adesao explicita: so recebe e-mail quem ligou a opcao
    @Column(name = "receber_emails", nullable = false)
    private Boolean receberEmails = false;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Column(name = "data_modificacao")
    private LocalDateTime dataModificacao;

    @Column(name = "data_desativacao")
    private LocalDateTime dataDesativacao;

    public boolean estaAtivo() {
        return status == StatusRegistro.ATIVO;
    }

    public boolean ehAdministrativo() {
        return role != null && role.ehAdministrativo();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public Set<Curso> getCursos() { return cursos; }
    public void setCursos(Set<Curso> cursos) { this.cursos = cursos; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public StatusRegistro getStatus() { return status; }
    public void setStatus(StatusRegistro status) { this.status = status; }
    public Boolean getReceberEmails() { return receberEmails; }
    public void setReceberEmails(Boolean receberEmails) { this.receberEmails = receberEmails; }
    public boolean querReceberEmails() { return Boolean.TRUE.equals(receberEmails); }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime d) { this.dataCriacao = d; }
    public LocalDateTime getDataModificacao() { return dataModificacao; }
    public void setDataModificacao(LocalDateTime d) { this.dataModificacao = d; }
    public LocalDateTime getDataDesativacao() { return dataDesativacao; }
    public void setDataDesativacao(LocalDateTime d) { this.dataDesativacao = d; }
}
