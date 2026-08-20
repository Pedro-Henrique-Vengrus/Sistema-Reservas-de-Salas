package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/** Construtores enxutos das entidades usadas nos testes de regra de negocio. */
final class CampusflowFixtures {

    private CampusflowFixtures() {}

    static Curso curso(Long id, String nome) {
        Curso c = new Curso();
        c.setId(id);
        c.setNome(nome);
        c.setStatus(StatusRegistro.ATIVO);
        return c;
    }

    static Usuario usuario(Long id, String nome, Role role, Curso... cursos) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome(nome);
        u.setEmail(nome.toLowerCase() + "@campus.br");
        u.setSenha("hash");
        u.setRole(role);
        u.setStatus(StatusRegistro.ATIVO);
        u.setCursos(Set.of(cursos));
        return u;
    }

    static Sala sala(Long id, String nome, Curso... cursos) {
        Sala s = new Sala();
        s.setId(id);
        s.setNome(nome);
        s.setCodigo(nome);
        s.setTipo(TipoAmbiente.SALA_AULA);
        s.setCapacidade(40);
        s.setStatus(StatusRegistro.ATIVO);
        s.setCursos(Set.of(cursos));
        return s;
    }

    static Reserva reserva(Long id, Usuario solicitante, Sala sala, LocalDate data,
                           String inicio, String fim, StatusReserva status) {
        Reserva r = new Reserva();
        r.setId(id);
        r.setSolicitante(solicitante);
        r.setSala(sala);
        r.setDataReserva(data);
        r.definirHorario(LocalTime.parse(inicio), LocalTime.parse(fim));
        r.setTipoReserva(TipoReserva.GRADE_BIMESTRAL);
        r.setStatus(status);
        return r;
    }
}
