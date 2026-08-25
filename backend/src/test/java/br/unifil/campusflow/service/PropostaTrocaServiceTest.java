package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.*;
import br.unifil.campusflow.dto.PropostaRequest;
import br.unifil.campusflow.exception.AcessoNegadoException;
import br.unifil.campusflow.exception.ConflitoException;
import br.unifil.campusflow.repository.PropostaTrocaRepository;
import br.unifil.campusflow.repository.ReservaRepository;
import br.unifil.campusflow.repository.UsuarioRepository;
import br.unifil.campusflow.security.UsuarioLogado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static br.unifil.campusflow.service.CampusflowFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PropostaTrocaServiceTest {

    @Mock PropostaTrocaRepository propostaRepository;
    @Mock ReservaRepository reservaRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock UsuarioLogado usuarioLogado;
    @Mock VisibilidadeService visibilidade;
    @Mock NotificacaoService notificacoes;

    @InjectMocks PropostaTrocaService service;

    Curso computacao;
    Usuario pedro;   // proponente
    Usuario joao;    // dono da reserva desejada
    Usuario admin;
    Sala salaA;
    Sala salaB;
    LocalDate dia;

    @BeforeEach
    void setUp() {
        computacao = curso(1L, "Ciencia da Computacao");
        pedro = usuario(10L, "Pedro", Role.PROFESSOR, computacao);
        joao = usuario(11L, "Joao", Role.PROFESSOR, computacao);
        admin = usuario(1L, "Administrador", Role.ADMIN);
        salaA = sala(100L, "Sala 1001", computacao);
        salaB = sala(200L, "Lab 1008", computacao);
        dia = LocalDate.now().plusDays(7);

        when(usuarioLogado.get()).thenReturn(pedro);
        when(visibilidade.podeVerSala(any(), anyLong())).thenReturn(true);
        when(reservaRepository.existeConflitoPessoal(anyLong(), any(), any(), any(), anyLong())).thenReturn(false);
        when(propostaRepository.save(any(PropostaTroca.class))).thenAnswer(inv -> inv.getArgument(0));
        when(propostaRepository.findEmAbertoEnvolvendo(any(), anyLong())).thenReturn(List.of());
        when(usuarioRepository.findAdministradoresAtivos()).thenReturn(List.of(admin));
    }

    private void reservasDisponiveis(Reserva desejada, Reserva oferecida) {
        when(reservaRepository.findByIdComDados(desejada.getId())).thenReturn(Optional.of(desejada));
        when(reservaRepository.findByIdComDados(oferecida.getId())).thenReturn(Optional.of(oferecida));
    }

    private PropostaTroca proposta(Reserva desejada, Reserva oferecida, StatusProposta status) {
        PropostaTroca p = new PropostaTroca();
        p.setId(9L);
        p.setReservaOrigem(desejada);
        p.setReservaOferecida(oferecida);
        p.setUsuarioSolicitante(pedro);
        p.setJustificativa("Preciso do laboratorio");
        p.setStatus(status);
        when(propostaRepository.findByIdComDados(9L)).thenReturn(Optional.of(p));
        return p;
    }

    // ------------------------------------------------------------------

    @Test
    @DisplayName("Reservas em datas passadas nao podem ser trocadas")
    void bloqueiaDatasPassadas() {
        Reserva desejada = reserva(1L, joao, salaA, LocalDate.now().minusDays(1), "19:00", "21:00",
                StatusReserva.APROVADA);
        Reserva oferecida = reserva(2L, pedro, salaB, dia, "19:00", "21:00", StatusReserva.APROVADA);
        reservasDisponiveis(desejada, oferecida);

        assertThatThrownBy(() -> service.criar(new PropostaRequest(1L, 2L, "justificativa")))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("datas passadas");
    }

    @Test
    @DisplayName("Recusa a troca quando alguma reserva ainda aguarda moderacao")
    void bloqueiaReservaNaoAprovada() {
        Reserva desejada = reserva(1L, joao, salaA, dia, "19:00", "21:00", StatusReserva.PENDENTE_APROVACAO);
        Reserva oferecida = reserva(2L, pedro, salaB, dia, "20:00", "22:00", StatusReserva.APROVADA);
        reservasDisponiveis(desejada, oferecida);

        assertThatThrownBy(() -> service.criar(new PropostaRequest(1L, 2L, "justificativa")))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("reservas aprovadas");
    }

    @Test
    @DisplayName("Troca que geraria sobreposicao na agenda pessoal e barrada")
    void bloqueiaConflitoPessoal() {
        Reserva desejada = reserva(1L, joao, salaA, dia, "19:00", "21:00", StatusReserva.APROVADA);
        Reserva oferecida = reserva(2L, pedro, salaB, dia, "20:00", "22:00", StatusReserva.APROVADA);
        reservasDisponiveis(desejada, oferecida);
        when(reservaRepository.existeConflitoPessoal(anyLong(), any(), any(), any(), anyLong())).thenReturn(true);

        assertThatThrownBy(() -> service.criar(new PropostaRequest(1L, 2L, "justificativa")))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("conflito de horario");
    }

    @Test
    @DisplayName("Somente o dono da reserva desejada responde a proposta")
    void terceiroNaoRespondeProposta() {
        Reserva desejada = reserva(1L, joao, salaA, dia, "19:00", "21:00", StatusReserva.APROVADA);
        Reserva oferecida = reserva(2L, pedro, salaB, dia, "20:00", "22:00", StatusReserva.APROVADA);
        proposta(desejada, oferecida, StatusProposta.PENDENTE);
        when(usuarioLogado.get()).thenReturn(usuario(12L, "Carla", Role.PROFESSOR, computacao));

        assertThatThrownBy(() -> service.responder(9L, true)).isInstanceOf(AcessoNegadoException.class);
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Mesmo dia e mesmo turno: resolve entre professores")
    class TrocaDeRotina {

        Reserva desejada;
        Reserva oferecida;

        @BeforeEach
        void cenario() {
            desejada = reserva(1L, joao, salaA, dia, "19:00", "21:00", StatusReserva.APROVADA);
            oferecida = reserva(2L, pedro, salaB, dia, "20:00", "22:00", StatusReserva.APROVADA);
        }

        @Test
        @DisplayName("Nao exige aval do gestor")
        void naoExigeGestor() {
            assertThat(PropostaTrocaService.exigeAvalDoGestor(desejada, oferecida)).isFalse();
        }

        @Test
        @DisplayName("O aceite do professor ja efetiva a troca e avisa os dois")
        void aceiteEfetivaNaHora() {
            proposta(desejada, oferecida, StatusProposta.PENDENTE);
            when(usuarioLogado.get()).thenReturn(joao);

            PropostaTroca resposta = service.responder(9L, true);

            assertThat(resposta.getStatus()).isEqualTo(StatusProposta.ACEITA);
            assertThat(desejada.getSolicitante()).isEqualTo(pedro);
            assertThat(oferecida.getSolicitante()).isEqualTo(joao);
            verify(reservaRepository).save(desejada);
            verify(reservaRepository).save(oferecida);
            verify(notificacoes).notificar(eq(pedro), eq(TipoNotificacao.TROCA_ACEITA), any(), any());
            verify(notificacoes).notificar(eq(joao), eq(TipoNotificacao.TROCA_ACEITA), any(), any());
            verify(notificacoes, never()).notificar(eq(admin), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Fora do dia ou do turno: passa pelo gestor")
    class TrocaExcepcional {

        @Test
        @DisplayName("Turno diferente no mesmo dia exige aval do gestor")
        void turnoDiferenteExigeGestor() {
            Reserva desejada = reserva(1L, joao, salaA, dia, "19:00", "21:00", StatusReserva.APROVADA);
            Reserva oferecida = reserva(2L, pedro, salaB, dia, "14:00", "16:00", StatusReserva.APROVADA);
            assertThat(PropostaTrocaService.exigeAvalDoGestor(desejada, oferecida)).isTrue();
        }

        @Test
        @DisplayName("Dia diferente exige aval do gestor")
        void diaDiferenteExigeGestor() {
            Reserva desejada = reserva(1L, joao, salaA, dia, "19:00", "21:00", StatusReserva.APROVADA);
            Reserva oferecida = reserva(2L, pedro, salaB, dia.plusDays(1), "19:00", "21:00",
                    StatusReserva.APROVADA);
            assertThat(PropostaTrocaService.exigeAvalDoGestor(desejada, oferecida)).isTrue();
        }

        @Test
        @DisplayName("Proposta fora do dia/turno e aceita na criacao, nao mais bloqueada")
        void criacaoForaDoPadraoEhPermitida() {
            Reserva desejada = reserva(1L, joao, salaA, dia, "19:00", "21:00", StatusReserva.APROVADA);
            Reserva oferecida = reserva(2L, pedro, salaB, dia.plusDays(2), "08:00", "10:00",
                    StatusReserva.APROVADA);
            reservasDisponiveis(desejada, oferecida);

            PropostaTroca p = service.criar(new PropostaRequest(1L, 2L, "Semana de provas"));

            assertThat(p.getStatus()).isEqualTo(StatusProposta.PENDENTE);
        }

        @Test
        @DisplayName("O aceite do professor encaminha ao gestor, sem efetivar a troca")
        void aceiteEncaminhaAoGestor() {
            Reserva desejada = reserva(1L, joao, salaA, dia, "19:00", "21:00", StatusReserva.APROVADA);
            Reserva oferecida = reserva(2L, pedro, salaB, dia, "08:00", "10:00", StatusReserva.APROVADA);
            proposta(desejada, oferecida, StatusProposta.PENDENTE);
            when(usuarioLogado.get()).thenReturn(joao);

            PropostaTroca resposta = service.responder(9L, true);

            assertThat(resposta.getStatus()).isEqualTo(StatusProposta.AGUARDANDO_GESTOR);
            // As reservas continuam com os donos originais ate o gestor decidir
            assertThat(desejada.getSolicitante()).isEqualTo(joao);
            assertThat(oferecida.getSolicitante()).isEqualTo(pedro);
            verify(reservaRepository, never()).save(any(Reserva.class));
            verify(notificacoes).notificar(eq(admin), eq(TipoNotificacao.TROCA_AGUARDA_GESTOR), any(), any());
        }

        @Test
        @DisplayName("A aprovacao do gestor efetiva a troca")
        void gestorAprovaEEfetiva() {
            Reserva desejada = reserva(1L, joao, salaA, dia, "19:00", "21:00", StatusReserva.APROVADA);
            Reserva oferecida = reserva(2L, pedro, salaB, dia, "08:00", "10:00", StatusReserva.APROVADA);
            proposta(desejada, oferecida, StatusProposta.AGUARDANDO_GESTOR);
            when(usuarioLogado.exigirAdministrativo()).thenReturn(admin);

            PropostaTroca resposta = service.decidirComoGestor(9L, true, null);

            assertThat(resposta.getStatus()).isEqualTo(StatusProposta.ACEITA);
            assertThat(desejada.getSolicitante()).isEqualTo(pedro);
            assertThat(oferecida.getSolicitante()).isEqualTo(joao);
            verify(notificacoes).notificar(eq(pedro), eq(TipoNotificacao.TROCA_ACEITA), any(), any());
            verify(notificacoes).notificar(eq(joao), eq(TipoNotificacao.TROCA_ACEITA), any(), any());
        }

        @Test
        @DisplayName("A recusa do gestor encerra a proposta sem trocar as reservas")
        void gestorRecusa() {
            Reserva desejada = reserva(1L, joao, salaA, dia, "19:00", "21:00", StatusReserva.APROVADA);
            Reserva oferecida = reserva(2L, pedro, salaB, dia, "08:00", "10:00", StatusReserva.APROVADA);
            proposta(desejada, oferecida, StatusProposta.AGUARDANDO_GESTOR);
            when(usuarioLogado.exigirAdministrativo()).thenReturn(admin);

            PropostaTroca resposta = service.decidirComoGestor(9L, false, "Sem justificativa pedagogica");

            assertThat(resposta.getStatus()).isEqualTo(StatusProposta.RECUSADA);
            assertThat(desejada.getSolicitante()).isEqualTo(joao);
            verify(reservaRepository, never()).save(any(Reserva.class));
        }

        @Test
        @DisplayName("O gestor so decide proposta que esta na fila dele")
        void gestorNaoDecidePropostaPendente() {
            Reserva desejada = reserva(1L, joao, salaA, dia, "19:00", "21:00", StatusReserva.APROVADA);
            Reserva oferecida = reserva(2L, pedro, salaB, dia, "08:00", "10:00", StatusReserva.APROVADA);
            proposta(desejada, oferecida, StatusProposta.PENDENTE);
            when(usuarioLogado.exigirAdministrativo()).thenReturn(admin);

            assertThatThrownBy(() -> service.decidirComoGestor(9L, true, null))
                    .isInstanceOf(ConflitoException.class)
                    .hasMessageContaining("aguardando o aval do gestor");
        }
    }
}
