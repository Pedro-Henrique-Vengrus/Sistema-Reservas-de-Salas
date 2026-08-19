package br.unifil.campusflow.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class TurnoTest {

    @DisplayName("Turno derivado da hora de inicio, com as fronteiras exatas do dia academico")
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "07:00, MATUTINO",
        "11:59, MATUTINO",
        "12:00, VESPERTINO",
        "17:59, VESPERTINO",
        "18:00, NOTURNO",
        "22:00, NOTURNO"
    })
    void derivaTurnoPelaHoraDeInicio(String hora, Turno esperado) {
        assertThat(Turno.de(LocalTime.parse(hora))).isEqualTo(esperado);
    }
}
