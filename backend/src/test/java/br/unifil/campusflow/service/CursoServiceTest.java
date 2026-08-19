package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.*;
import br.unifil.campusflow.exception.ConfirmacaoNecessariaException;
import br.unifil.campusflow.exception.ConflitoException;
import br.unifil.campusflow.repository.CursoRepository;
import br.unifil.campusflow.repository.ReservaRepository;
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
import java.util.List;
import java.util.Optional;

import static br.unifil.campusflow.service.CampusflowFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CursoServiceTest {

    @Mock CursoRepository cursoRepository;
    @Mock ReservaRepository reservaRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock ReservaService reservaService;
    @Mock UsuarioLogado usuarioLogado;

    @InjectMocks CursoService service;

    Curso computacao;
    Usuario professor;
    Reserva futura;

    @BeforeEach
    void setUp() {
        computacao = curso(1L, "Ciencia da Computacao");
        professor = usuario(10L, "Pedro", Role.PROFESSOR, computacao);
        futura = reserva(500L, professor, sala(100L, "Sala 1001", computacao),
                LocalDate.now().plusDays(5), "19:00", "21:00", StatusReserva.APROVADA);

        when(usuarioLogado.exigirAdministrativo()).thenReturn(usuario(1L, "Administrador", Role.ADMIN));
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(computacao));
        when(cursoRepository.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.buscar(any(), any(), any(), any())).thenReturn(List.of(professor));
    }

    @Test
    @DisplayName("Inativar curso com reservas futuras dos seus professores exige confirmacao")
    void inativacaoComReservasPedeConfirmacao() {
        when(reservaRepository.findFuturasAtivasDoCurso(eq(1L), any())).thenReturn(List.of(futura));

        assertThatThrownBy(() -> service.inativar(1L, false))
                .isInstanceOf(ConfirmacaoNecessariaException.class)
                .hasMessageContaining("reserva(s) futura(s)");

        assertThat(computacao.getStatus()).isEqualTo(StatusRegistro.ATIVO);
        verify(reservaService, never()).cancelarInterno(any(), any());
    }

    @Test
    @DisplayName("Inativacao forcada cancela as reservas futuras vinculadas ao curso")
    void inativacaoForcadaCancelaReservas() {
        when(reservaRepository.findFuturasAtivasDoCurso(eq(1L), any())).thenReturn(List.of(futura));

        Curso inativado = service.inativar(1L, true);

        assertThat(inativado.getStatus()).isEqualTo(StatusRegistro.INATIVO);
        verify(reservaService).cancelarInterno(eq(futura), contains("inativado"));
    }

    @Test
    @DisplayName("Exclusao fisica e barrada enquanto houver usuarios vinculados")
    void exclusaoFisicaBloqueadaComUsuarios() {
        computacao.setStatus(StatusRegistro.INATIVO);
        when(reservaRepository.findFuturasAtivasDoCurso(eq(1L), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.excluirFisicamente(1L))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("usuario(s)");
        verify(cursoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Curso inativo, sem usuarios e sem reservas pode ser removido")
    void exclusaoFisicaPermitida() {
        computacao.setStatus(StatusRegistro.INATIVO);
        when(reservaRepository.findFuturasAtivasDoCurso(eq(1L), any())).thenReturn(List.of());
        when(usuarioRepository.buscar(any(), any(), any(), any())).thenReturn(List.of());

        service.excluirFisicamente(1L);

        verify(cursoRepository).delete(computacao);
    }

    @Test
    @DisplayName("Nome de curso duplicado e recusado")
    void nomeDuplicadoBloqueado() {
        when(cursoRepository.existsByNomeIgnoreCase("Ciencia da Computacao")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(
                new br.unifil.campusflow.dto.CursoRequest("Ciencia da Computacao", "CC")))
                .isInstanceOf(ConflitoException.class);
    }
}
