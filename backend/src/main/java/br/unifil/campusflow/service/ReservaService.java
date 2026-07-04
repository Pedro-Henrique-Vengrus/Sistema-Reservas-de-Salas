package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.Reserva;
import br.unifil.campusflow.domain.Role;
import br.unifil.campusflow.domain.Sala;
import br.unifil.campusflow.domain.Usuario;
import br.unifil.campusflow.dto.ReservaRequest;
import br.unifil.campusflow.exception.ConflitoException;
import br.unifil.campusflow.exception.RecursoNaoEncontradoException;
import br.unifil.campusflow.repository.ReservaRepository;
import br.unifil.campusflow.repository.SalaRepository;
import br.unifil.campusflow.repository.UsuarioRepository;
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
    private final UsuarioRepository usuarioRepository;
    private final UsuarioLogado usuarioLogado;

    public ReservaService(ReservaRepository reservaRepository,
                          SalaRepository salaRepository,
                          UsuarioRepository usuarioRepository,
                          UsuarioLogado usuarioLogado) {
        this.reservaRepository = reservaRepository;
        this.salaRepository = salaRepository;
        this.usuarioRepository = usuarioRepository;
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

        Usuario solicitante;
        String status;
        if (ehAdmin(u)) {
            // Admin so pode reservar em nome de outro usuario; a reserva ja nasce confirmada
            if (dto.usuarioId() == null) {
                throw new ConflitoException("Admin deve selecionar o usuario para quem a sala sera reservada.");
            }
            if (dto.usuarioId().equals(u.getId())) {
                throw new ConflitoException("Admin nao pode reservar sala para si mesmo.");
            }
            solicitante = usuarioRepository.findById(dto.usuarioId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
            status = "CONFIRMADA";
        } else {
            if (dto.usuarioId() != null && !dto.usuarioId().equals(u.getId())) {
                throw new ConflitoException("Apenas o Admin pode reservar sala para outro usuario.");
            }
            solicitante = u;
            status = "PENDENTE";
        }

        Reserva r = new Reserva();
        r.setSolicitante(solicitante);
        r.setSala(sala);
        r.setDataReserva(dto.data());
        r.setHoraInicio(dto.horaInicio());
        r.setHoraFim(dto.horaFim());
        r.setTipoReserva(dto.tipoReserva() != null ? dto.tipoReserva() : "GRADE_BIMESTRAL");
        r.setStatus(status);
        return reservaRepository.save(r);
    }

    @Transactional
    public void cancelar(Long id) {
        Usuario u = usuarioLogado.get();
        Reserva r = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva nao encontrada"));
        if (!ehAdmin(u) && !r.getSolicitante().getId().equals(u.getId())) {
            throw new ConflitoException("Voce so pode cancelar suas proprias reservas.");
        }
        r.setStatus("CANCELADA");
        r.setDataExclusao(LocalDateTime.now());
        reservaRepository.save(r);
    }

    public List<Reserva> pendentes() {
        exigirAdmin();
        return reservaRepository.findPendentes();
    }

    public long countPendentes() {
        exigirAdmin();
        return reservaRepository.countPendentes();
    }

    public List<Reserva> todas() {
        exigirAdmin();
        return reservaRepository.findTodas();
    }

    @Transactional
    public Reserva aprovar(Long id) {
        exigirAdmin();
        Reserva r = reservaRepository.findByIdComDados(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva nao encontrada"));
        if (!"PENDENTE".equals(r.getStatus())) {
            throw new ConflitoException("Esta reserva ja foi avaliada.");
        }
        r.setStatus("CONFIRMADA");
        r.setDataModificacao(LocalDateTime.now());
        return reservaRepository.save(r);
    }

    @Transactional
    public Reserva recusar(Long id) {
        exigirAdmin();
        Reserva r = reservaRepository.findByIdComDados(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva nao encontrada"));
        if (!"PENDENTE".equals(r.getStatus())) {
            throw new ConflitoException("Esta reserva ja foi avaliada.");
        }
        r.setStatus("RECUSADA");
        r.setDataModificacao(LocalDateTime.now());
        return reservaRepository.save(r);
    }

    private void exigirAdmin() {
        Usuario u = usuarioLogado.get();
        if (!ehAdmin(u)) {
            throw new ConflitoException("Apenas o Admin pode realizar esta acao.");
        }
    }

    private boolean ehAdmin(Usuario u) {
        return u.getRole() == Role.ADMIN || u.getRole() == Role.GESTOR;
    }
}
