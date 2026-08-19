package br.unifil.campusflow.dto;

import java.util.List;

/** Metricas do painel inicial. */
public record DashboardResponse(
    long reservasAprovadas,
    long reservasPendentes,
    long reservasRecusadas,
    long reservasCanceladas,
    long ambientesAtivos,
    long cursosAtivos,
    long usuariosAtivos,
    boolean gradeAberta,
    List<Contagem> reservasPorSala,
    List<Contagem> reservasPorCurso,
    List<ReservaResponse> proximasReservas
) {
    public record Contagem(String rotulo, long total) {
        public static Contagem de(Object[] linha) {
            return new Contagem((String) linha[0], ((Number) linha[1]).longValue());
        }
    }
}
