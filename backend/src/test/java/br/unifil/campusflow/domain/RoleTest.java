package br.unifil.campusflow.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trava a separacao de papeis: somente o ADMIN administra; REITOR e PROFESSOR
 * sao solicitantes com a mesma visao (recortada pelos cursos do perfil).
 */
class RoleTest {

    @Test
    @DisplayName("Somente o ADMIN opera o painel administrativo")
    void apenasAdminEhAdministrativo() {
        assertThat(Role.ADMIN.ehAdministrativo()).isTrue();
        assertThat(Role.REITOR.ehAdministrativo()).isFalse();
        assertThat(Role.PROFESSOR.ehAdministrativo()).isFalse();
    }

    @Test
    @DisplayName("REITOR e PROFESSOR solicitam reservas; o ADMIN nao reserva para si")
    void solicitantes() {
        assertThat(Role.REITOR.ehSolicitante()).isTrue();
        assertThat(Role.PROFESSOR.ehSolicitante()).isTrue();
        assertThat(Role.ADMIN.ehSolicitante()).isFalse();
    }
}
