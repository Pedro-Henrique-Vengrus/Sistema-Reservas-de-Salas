package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.*;
import br.unifil.campusflow.exception.ConfirmacaoNecessariaException;
import br.unifil.campusflow.exception.ConflitoException;
import br.unifil.campusflow.repository.CursoRepository;
import br.unifil.campusflow.repository.ReservaRepository;
import br.unifil.campusflow.repository.SalaRepository;
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
class SalaServiceTest {

    @Mock SalaRepository salaRepository;
    @Mock CursoRepository cursoRepository;
    @Mock ReservaRepository reservaRepository;
    @Mock ReservaService reservaService;
    @Mock UsuarioLogado usuarioLogado;
    @Mock VisibilidadeService visibilidade;

    @InjectMocks SalaService service;

    Curso computacao;
    Usuario admin;
    Usuario professor;
    Sala sala;
    Reserva futura;

    @BeforeEach
    void setUp() {
        computacao = curso(1L, "Ciencia da Computacao");
        admin = usuario(1L, "Administrador", Role.ADMIN);
        professor = usuario(10L, "Pedro", Role.PROFESSOR, computacao);
        sala = sala(100L, "Sala 1001", computacao);
        futura = reserva(500L, professor, sala, LocalDate.now().plusDays(3), "19:00", "21:00",
                StatusReserva.APROVADA);

        when(usuarioLogado.exigirAdministrativo()).thenReturn(admin);
        when(salaRepository.findByIdComCursos(100L)).thenReturn(Optional.of(sala));
        when(salaRepository.save(any(Sala.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Inativar ambiente com reservas futuras exige confirmacao explicita")
    void inativacaoComReservasFuturasPedeConfirmacao() {
        when(reservaRepository.findFuturasAtivasDaSala(eq(100L), any())).thenReturn(List.of(futura));
        when(reservaRepository.countDaSala(100L)).thenReturn(1L);

        assertThatThrownBy(() -> service.inativar(100L, false))
                .isInstanceOf(ConfirmacaoNecessariaException.class)
                .hasMessageContaining("reserva(s) futura(s)");

        assertThat(sala.getStatus()).isEqualTo(StatusRegistro.ATIVO);
        verify(reservaService, never()).cancelarInterno(any(), any());
    }

    @Test
    @DisplayName("Inativacao forcada cancela as reservas futuras e notifica os solicitantes")
    void inativacaoForcadaCancelaReservas() {
        when(reservaRepository.findFuturasAtivasDaSala(eq(100L), any())).thenReturn(List.of(futura));

        Sala inativada = service.inativar(100L, true);

        assertThat(inativada.getStatus()).isEqualTo(StatusRegistro.INATIVO);
        assertThat(inativada.getDataDesativacao()).isNotNull();
        verify(reservaService).cancelarInterno(eq(futura), contains("inativado"));
    }

    @Test
    @DisplayName("Ambiente sem reservas futuras e inativado direto")
    void inativacaoSemImpactoNaoPedeConfirmacao() {
        when(reservaRepository.findFuturasAtivasDaSala(eq(100L), any())).thenReturn(List.of());

        assertThat(service.inativar(100L, false).getStatus()).isEqualTo(StatusRegistro.INATIVO);
        verify(reservaService, never()).cancelarInterno(any(), any());
    }

    @Test
    @DisplayName("Exclusao fisica exige ambiente inativo")
    void exclusaoFisicaExigeInativo() {
        assertThatThrownBy(() -> service.excluirFisicamente(100L))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("Inative o ambiente");
        verify(salaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Exclusao fisica e barrada quando ha historico de reservas")
    void exclusaoFisicaPreservaHistorico() {
        sala.setStatus(StatusRegistro.INATIVO);
        when(reservaRepository.countDaSala(100L)).thenReturn(4L);

        assertThatThrownBy(() -> service.excluirFisicamente(100L))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("historico");
        verify(salaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Ambiente inativo e sem historico pode ser removido definitivamente")
    void exclusaoFisicaPermitida() {
        sala.setStatus(StatusRegistro.INATIVO);
        when(reservaRepository.countDaSala(100L)).thenReturn(0L);

        service.excluirFisicamente(100L);

        verify(salaRepository).delete(sala);
    }

    @Test
    @DisplayName("Solicitante sem curso vinculado nao enxerga nenhum ambiente")
    void solicitanteSemCursoNaoVeAmbientes() {
        Usuario semCurso = usuario(11L, "Carla", Role.PROFESSOR);
        when(usuarioLogado.get()).thenReturn(semCurso);
        when(visibilidade.veTodoOCatalogo(semCurso)).thenReturn(false);
        when(visibilidade.cursoIdsDe(semCurso)).thenReturn(java.util.Set.of());

        assertThat(service.listar(null, null, null, null, null)).isEmpty();
        verify(salaRepository, never()).buscarVisiveis(any(), any(), any(), any(), anyLong());
    }
}
