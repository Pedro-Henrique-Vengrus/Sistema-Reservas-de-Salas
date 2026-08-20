package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.*;
import br.unifil.campusflow.dto.PropostaRequest;
import br.unifil.campusflow.exception.AcessoNegadoException;
import br.unifil.campusflow.exception.ConflitoException;
import br.unifil.campusflow.repository.PropostaTrocaRepository;
import br.unifil.campusflow.repository.ReservaRepository;
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
    @Mock UsuarioLogado usuarioLogado;
    @Mock VisibilidadeService visibilidade;
    @Mock NotificacaoService notificacoes;

    @InjectMocks PropostaTrocaService service;

    Curso computacao;
    Usuario pedro;   // proponente
    Usuario joao;    // dono da reserva desejada
    Sala salaA;
    Sala salaB;
    LocalDate dia;

    @BeforeEach
    void setUp() {
        computacao = curso(1L, "Ciencia da Computacao");
        pedro = usuario(10L, "Pedro", Role.PROFESSOR, computacao);
        joao = usuario(11L, "Joao", Role.PROFESSOR, computacao);
        salaA = sala(100L, "Sala 1001", computacao);
        salaB = sala(200L, "Lab 1008", computacao);
        dia = LocalDate.now().plusDays(7);

        when(usuarioLogado.get()).thenReturn(pedro);
        when(visibilidade.podeVerSala(any(), anyLong())).thenReturn(true);
        when(reservaRepository.existeConflitoPessoal(anyLong(), any(), any(), any(), anyLong())).thenReturn(false);
        when(propostaRepository.save(any(PropostaTroca.class))).thenAnswer(inv -> inv.getArgument(0));
        when(propostaRepository.findPendentesEnvolvendo(any(), anyLong())).thenReturn(List.of());
    }

    private void reservasDisponiveis(Reserva desejada, Reserva oferecida) {
        when(reservaRepository.findByIdComDados(desejada.getId())).thenReturn(Optional.of(desejada));
        when(reservaRepository.findByIdComDados(oferecida.getId())).thenReturn(Optional.of(oferecida));
    }

    @Test
    @DisplayName("Aceita a proposta quando as duas reservas estao no mesmo dia e no mesmo turno")
    void criaPropostaNoMesmoDiaETurno() {
        Reserva desejada = reserva(1L, joao, salaA, dia, "19:00", "21:00", StatusReserva.APROVADA);
        Reserva oferecida = reserva(2L, pedro, salaB, dia, "20:00", "22:00", StatusReserva.APROVADA);
        reservasDisponiveis(desejada, oferecida);

        PropostaTroca p = service.criar(new PropostaRequest(1L, 2L, "Preciso do laboratorio"));

        assertThat(p.getStatus()).isEqualTo(StatusProposta.PENDENTE);
        assertThat(desejada.getTurno()).isEqualTo(oferecida.getTurno());
        // Ambos os envolvidos sao avisados
        verify(notificacoes).notificar(eq(joao), eq(TipoNotificacao.TROCA_RECEBIDA), any(), any());
        verify(notificacoes).notificar(eq(pedro), eq(TipoNotificacao.TROCA_RECEBIDA), any(), any());
    }

    @Test
    @DisplayName("Recusa a troca entre reservas de dias diferentes")
    void bloqueiaDiasDiferentes() {
        Reserva desejada = reserva(1L, joao, salaA, dia, "19:00", "21:00", StatusReserva.APROVADA);
        Reserva oferecida = reserva(2L, pedro, salaB, dia.plusDays(1), "19:00", "21:00", StatusReserva.APROVADA);
        reservasDisponiveis(desejada, oferecida);

        assertThatThrownBy(() -> service.criar(new PropostaRequest(1L, 2L, "justificativa")))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("mesmo dia");
    }

    @Test
    @DisplayName("Recusa a troca entre turnos diferentes no mesmo dia")
    void bloqueiaTurnosDiferentes() {
        Reserva desejada = reserva(1L, joao, salaA, dia, "19:00", "21:00", StatusReserva.APROVADA);   // noturno
        Reserva oferecida = reserva(2L, pedro, salaB, dia, "14:00", "16:00", StatusReserva.APROVADA); // vespertino
        reservasDisponiveis(desejada, oferecida);

        assertThatThrownBy(() -> service.criar(new PropostaRequest(1L, 2L, "justificativa")))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("mesmo turno");
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
    @DisplayName("Aceite inverte os solicitantes das duas reservas e avisa os dois professores")
    void aceiteEfetivaTrocaMutua() {
        Reserva desejada = reserva(1L, joao, salaA, dia, "19:00", "21:00", StatusReserva.APROVADA);
        Reserva oferecida = reserva(2L, pedro, salaB, dia, "20:00", "22:00", StatusReserva.APROVADA);

        PropostaTroca p = new PropostaTroca();
        p.setId(9L);
        p.setReservaOrigem(desejada);
        p.setReservaOferecida(oferecida);
        p.setUsuarioSolicitante(pedro);
        p.setJustificativa("Preciso do laboratorio");
        p.setStatus(StatusProposta.PENDENTE);

        when(usuarioLogado.get()).thenReturn(joao);   // quem responde e o dono da reserva desejada
        when(propostaRepository.findByIdComDados(9L)).thenReturn(Optional.of(p));

        PropostaTroca resposta = service.responder(9L, true);

        assertThat(resposta.getStatus()).isEqualTo(StatusProposta.ACEITA);
        assertThat(desejada.getSolicitante()).isEqualTo(pedro);
        assertThat(oferecida.getSolicitante()).isEqualTo(joao);
        verify(reservaRepository).save(desejada);
        verify(reservaRepository).save(oferecida);
        verify(notificacoes).notificar(eq(pedro), eq(TipoNotificacao.TROCA_ACEITA), any(), any());
        verify(notificacoes).notificar(eq(joao), eq(TipoNotificacao.TROCA_ACEITA), any(), any());
    }

    @Test
    @DisplayName("Somente o dono da reserva desejada responde a proposta")
    void terceiroNaoRespondeProposta() {
        Reserva desejada = reserva(1L, joao, salaA, dia, "19:00", "21:00", StatusReserva.APROVADA);
        Reserva oferecida = reserva(2L, pedro, salaB, dia, "20:00", "22:00", StatusReserva.APROVADA);
        PropostaTroca p = new PropostaTroca();
        p.setId(9L);
        p.setReservaOrigem(desejada);
        p.setReservaOferecida(oferecida);
        p.setUsuarioSolicitante(pedro);
        p.setStatus(StatusProposta.PENDENTE);

        when(usuarioLogado.get()).thenReturn(usuario(12L, "Carla", Role.PROFESSOR, computacao));
        when(propostaRepository.findByIdComDados(9L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.responder(9L, true)).isInstanceOf(AcessoNegadoException.class);
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
}
