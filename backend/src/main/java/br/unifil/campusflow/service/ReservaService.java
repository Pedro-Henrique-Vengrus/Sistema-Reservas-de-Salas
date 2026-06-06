package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.Reserva;
import br.unifil.campusflow.domain.Sala;
import br.unifil.campusflow.domain.Usuario;
import br.unifil.campusflow.dto.ReservaRequest;
import br.unifil.campusflow.exception.ConflitoException;
import br.unifil.campusflow.exception.RecursoNaoEncontradoException;
import br.unifil.campusflow.repository.ReservaRepository;
import br.unifil.campusflow.repository.SalaRepository;
import br.unifil.campusflow.security.UsuarioLogado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final SalaRepository salaRepository;
    private final UsuarioLogado usuarioLogado;

    public ReservaService(ReservaRepository reservaRepository,
                          SalaRepository salaRepository,
                          UsuarioLogado usuarioLogado) {
        this.reservaRepository = reservaRepository;
        this.salaRepository = salaRepository;
        this.usuarioLogado = usuarioLogado;
    }

    public List<Reserva> minhasReservas() {
        Usuario u = usuarioLogado.get();
        return reservaRepository.findMinhasAtivas(u.getId());
    }

    public List<Reserva> reservasDeOutros() {
        Usuario u = usuarioLogado.get();
        return reservaRepository.findDeOutros(u.getId());
    }

    public List<Reserva> agendaDaSala(Long salaId, LocalDate data) {
        return reservaRepository.findDaSalaNaData(salaId, data);
    }

    @Transactional
    public Reserva criar(ReservaRequest dto) {
        Usuario u = usuarioLogado.get();
        Sala sala = salaRepository.findById(dto.salaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sala nao encontrada"));

        // Regra: bloqueio de sobreposicao de horario
        if (reservaRepository.existeConflito(dto.salaId(), dto.data(), dto.horaInicio(), dto.horaFim())) {
            throw new ConflitoException("Horario ja ocupado para esta sala. Use a proposta de troca.");
        }

        Reserva r = new Reserva();
        r.setSolicitante(u);
        r.setSala(sala);
        r.setDataReserva(dto.data());
        r.setHoraInicio(dto.horaInicio());
        r.setHoraFim(dto.horaFim());
        r.setTipoReserva(dto.tipoReserva() != null ? dto.tipoReserva() : "GRADE_BIMESTRAL");
        r.setStatus("CONFIRMADA");
        return reservaRepository.save(r);
    }

    @Transactional
    public void cancelar(Long id) {
        Usuario u = usuarioLogado.get();
        Reserva r = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva nao encontrada"));
        boolean ehAdmin = u.getRole() == br.unifil.campusflow.domain.Role.ADMIN
                || u.getRole() == br.unifil.campusflow.domain.Role.GESTOR;
        if (!ehAdmin && !r.getSolicitante().getId().equals(u.getId())) {
            throw new ConflitoException("Voce so pode cancelar suas proprias reservas.");
        }
        r.setStatus("CANCELADA");
        r.setDataExclusao(LocalDateTime.now());
        reservaRepository.save(r);
    }
}
