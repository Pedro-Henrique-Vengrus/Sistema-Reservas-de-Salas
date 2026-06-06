package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.PropostaTroca;
import br.unifil.campusflow.domain.Reserva;
import br.unifil.campusflow.domain.Usuario;
import br.unifil.campusflow.dto.PropostaRequest;
import br.unifil.campusflow.exception.ConflitoException;
import br.unifil.campusflow.exception.RecursoNaoEncontradoException;
import br.unifil.campusflow.repository.PropostaTrocaRepository;
import br.unifil.campusflow.repository.ReservaRepository;
import br.unifil.campusflow.security.UsuarioLogado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PropostaTrocaService {

    private final PropostaTrocaRepository propostaRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioLogado usuarioLogado;

    public PropostaTrocaService(PropostaTrocaRepository propostaRepository,
                                ReservaRepository reservaRepository,
                                UsuarioLogado usuarioLogado) {
        this.propostaRepository = propostaRepository;
        this.reservaRepository = reservaRepository;
        this.usuarioLogado = usuarioLogado;
    }

    public List<PropostaTroca> enviadas() {
        Usuario u = usuarioLogado.get();
        return propostaRepository.findEnviadas(u.getId());
    }

    public List<PropostaTroca> recebidas() {
        Usuario u = usuarioLogado.get();
        return propostaRepository.findRecebidas(u.getId());
    }

    public long pendentesRecebidas() {
        Usuario u = usuarioLogado.get();
        return propostaRepository.countPendentesRecebidas(u.getId());
    }

    @Transactional
    public PropostaTroca criar(PropostaRequest dto) {
        Usuario u = usuarioLogado.get();
        Reserva reserva = reservaRepository.findByIdComDados(dto.reservaOrigemId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva nao encontrada"));

        if (reserva.getSolicitante().getId().equals(u.getId())) {
            throw new ConflitoException("Voce nao pode propor troca da sua propria reserva.");
        }

        PropostaTroca p = new PropostaTroca();
        p.setReservaOrigem(reserva);
        p.setUsuarioSolicitante(u);
        p.setJustificativa(dto.justificativa());
        p.setStatus("PENDENTE");
        return propostaRepository.save(p);
    }

    @Transactional
    public PropostaTroca responder(Long id, boolean aceitar) {
        Usuario u = usuarioLogado.get();
        PropostaTroca p = propostaRepository.findByIdComDados(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta nao encontrada"));

        // So o dono da reserva alvo pode responder
        if (!p.getReservaOrigem().getSolicitante().getId().equals(u.getId())) {
            throw new ConflitoException("Apenas o dono da reserva pode responder a proposta.");
        }
        if (!"PENDENTE".equals(p.getStatus())) {
            throw new ConflitoException("Esta proposta ja foi respondida.");
        }

        if (aceitar) {
            // Transfere a reserva para quem propos
            Reserva r = p.getReservaOrigem();
            r.setSolicitante(p.getUsuarioSolicitante());
            r.setDataModificacao(LocalDateTime.now());
            reservaRepository.save(r);
            p.setStatus("ACEITA");
        } else {
            p.setStatus("RECUSADA");
        }
        p.setDataModificacao(LocalDateTime.now());
        return propostaRepository.save(p);
    }
}
