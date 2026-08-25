package br.unifil.campusflow.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Formatacao pt-BR de datas e horas para textos exibidos ao usuario
 * (notificacoes e exportacoes). Ponto unico: nao formatar data solta pelo codigo.
 */
public final class FormatoBr {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private FormatoBr() {}

    public static String data(LocalDate data) {
        return data == null ? "" : data.format(DATA);
    }

    public static String hora(LocalTime hora) {
        return hora == null ? "" : hora.format(HORA);
    }

    /** Ex.: "25/08/2026 das 19:00 as 21:00" */
    public static String periodo(LocalDate data, LocalTime inicio, LocalTime fim) {
        return data(data) + " das " + hora(inicio) + " as " + hora(fim);
    }
}
