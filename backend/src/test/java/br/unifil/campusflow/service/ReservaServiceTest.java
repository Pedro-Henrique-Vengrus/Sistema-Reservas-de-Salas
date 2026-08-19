package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.*;
import br.unifil.campusflow.dto.ReservaRequest;
import br.unifil.campusflow.exception.AcessoNegadoException;
import br.unifil.campusflow.exception.ConflitoException;
import br.unifil.campusflow.repository.PropostaTrocaRepository;
import br.unifil.campusflow.repository.ReservaRepository;
import br.unifil.campusflow.repository.SalaRepository;
import br.unifil.campusflow.repository.UsuarioRepository;
import br.unifil.campusflow.security.UsuarioLogado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static br.unifil.campusflow.service.CampusflowFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservaServiceTest {

    @Mock ReservaRepository reservaRepository;
    @Mock SalaRepository salaRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock PropostaTrocaRepository propostaRepository;
    @Mock UsuarioLogado usuarioLogado;
    @Mock VisibilidadeService visibilidade;
    @Mock PeriodoGradeService periodoGrade;
    @Mock NotificacaoService notificacoes;

    @InjectMocks ReservaService service;

    Curso computacao;
    Usuario professor;
    Sala sala;
    LocalDate amanha;

    @BeforeEach
    void setUp() {
        computacao = curso(1L, "Ciencia da Computacao");
        professor = usuario(10L, "Pedro", Role.PROFESSOR, computacao);
        sala = sala(100L, "Sala 1001", computacao);
        amanha = LocalDate.now().plusDays(1);

        when(usuarioLogado.get()).thenReturn(professor);
        when(salaRepository.findById(100L)).thenReturn(Optional.of(sala));
        when(reservaRepository.existeConflito(anyLong(), any(), any(), any())).thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ReservaRequest pedido(TipoReserva tipo) {
        return new ReservaRequest(100L, amanha, LocalTime.of(19, 0), LocalTime.of(21, 0), tipo, null, null);
    }

    @Test
    @DisplayName("Grade bimestral com periodo aberto confirma direto")
    void gradeBimestralAbertaNasceAprovada() {
        when(periodoGrade.gradeAberta()).thenReturn(true);

        Reserva r = service.criar(pedido(TipoReserva.GRADE_BIMESTRAL));

        assertThat(r.getStatus()).isEqualTo(StatusReserva.APROVADA);
        assertThat(r.getTurno()).isEqualTo(Turno.NOTURNO);
        assertThat(r.getSolicitante()).isEqualTo(professor);
    }

    @Test
    @DisplayName("Grade bimestral e bloqueada enquanto o Admin nao libera o periodo")
    void gradeBimestralFechadaBloqueia() {
        when(periodoGrade.gradeAberta()).thenReturn(false);

        assertThatThrownBy(() -> service.criar(pedido(TipoReserva.GRADE_BIMESTRAL)))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("grade bimestral esta fechado");

        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ultima hora sempre entra na fila de moderacao, mesmo com a grade fechada")
    void ultimaHoraNascePendente() {
        when(periodoGrade.gradeAberta()).thenReturn(false);

        Reserva r = service.criar(pedido(TipoReserva.ULTIMA_HORA));

        assertThat(r.getStatus()).isEqualTo(StatusReserva.PENDENTE_APROVACAO);
    }

    @Test
    @DisplayName("Professor nao reserva ambiente fora dos cursos do seu perfil")
    void ambienteForaDoCursoEhNegado() {
        when(periodoGrade.gradeAberta()).thenReturn(true);
        doThrow(new AcessoNegadoException("fora do escopo"))
                .when(visibilidade).exigirAcessoASala(professor, sala);

        assertThatThrownBy(() -> service.criar(pedido(TipoReserva.GRADE_BIMESTRAL)))
                .isInstanceOf(AcessoNegadoException.class);

        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Horario ja ocupado no ambiente bloqueia a reserva")
    void conflitoDeHorarioBloqueia() {
        when(periodoGrade.gradeAberta()).thenReturn(true);
        when(reservaRepository.existeConflito(anyLong(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.criar(pedido(TipoReserva.GRADE_BIMESTRAL)))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("ja ocupado");
    }

    @Test
    @DisplayName("Aprovacao exige perfil administrativo e avisa o solicitante")
    void aprovacaoNotificaSolicitante() {
        Usuario reitor = usuario(2L, "Marta", Role.REITOR, computacao);
        Reserva pendente = reserva(500L, professor, sala, amanha, "14:00", "16:00",
                StatusReserva.PENDENTE_APROVACAO);
        when(usuarioLogado.exigirAdministrativo()).thenReturn(reitor);
        when(reservaRepository.findByIdComDados(500L)).thenReturn(Optional.of(pendente));
        when(reservaRepository.existeConflitoExceto(anyLong(), any(), any(), any(), anyLong())).thenReturn(false);

        Reserva aprovada = service.aprovar(500L);

        assertThat(aprovada.getStatus()).isEqualTo(StatusReserva.APROVADA);
        verify(notificacoes).notificar(eq(professor), eq(TipoNotificacao.RESERVA_APROVADA), any(), any());
    }

    @Test
    @DisplayName("Reserva ja avaliada nao volta para a fila de moderacao")
    void naoReavaliaReservaJaDecidida() {
        when(usuarioLogado.exigirAdministrativo()).thenReturn(usuario(2L, "Marta", Role.REITOR, computacao));
        Reserva aprovada = reserva(501L, professor, sala, amanha, "14:00", "16:00", StatusReserva.APROVADA);
        when(reservaRepository.findByIdComDados(501L)).thenReturn(Optional.of(aprovada));

        assertThatThrownBy(() -> service.aprovar(501L))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("ja foi avaliada");
    }

    @Test
    @DisplayName("Cancelamento por terceiro exige perfil administrativo")
    void cancelamentoPorOutroProfessorEhNegado() {
        Usuario outro = usuario(11L, "Joao", Role.PROFESSOR, computacao);
        Reserva alheia = reserva(600L, professor, sala, amanha, "19:00", "21:00", StatusReserva.APROVADA);
        when(usuarioLogado.get()).thenReturn(outro);
        when(reservaRepository.findByIdComDados(600L)).thenReturn(Optional.of(alheia));

        assertThatThrownBy(() -> service.cancelar(600L)).isInstanceOf(AcessoNegadoException.class);
    }
}
