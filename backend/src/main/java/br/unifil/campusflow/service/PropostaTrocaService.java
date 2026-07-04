package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.PropostaTroca;
import br.unifil.campusflow.domain.Reserva;
import br.unifil.campusflow.domain.Role;
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
        if (u.getRole() == Role.GESTOR) {
            throw new ConflitoException("Administradores nao propoem troca de sala.");
        }
        Reserva reservaOrigem = reservaRepository.findByIdComDados(dto.reservaOrigemId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva nao encontrada"));

        if (reservaOrigem.getSolicitante().getId().equals(u.getId())) {
            throw new ConflitoException("Voce nao pode propor troca da sua propria reserva.");
        }
        if (!"CONFIRMADA".equals(reservaOrigem.getStatus())) {
            throw new ConflitoException("Somente reservas confirmadas podem ser objeto de troca.");
        }

        Reserva reservaOferecida = reservaRepository.findByIdComDados(dto.reservaOferecidaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva oferecida nao encontrada"));
        if (!reservaOferecida.getSolicitante().getId().equals(u.getId())) {
            throw new ConflitoException("Voce so pode oferecer uma reserva sua.");
        }
        if (!"CONFIRMADA".equals(reservaOferecida.getStatus())) {
            throw new ConflitoException("A reserva oferecida precisa estar confirmada.");
        }

        validarConflitoDaTroca(reservaOrigem, reservaOferecida);

        PropostaTroca p = new PropostaTroca();
        p.setReservaOrigem(reservaOrigem);
        p.setUsuarioSolicitante(u);
        p.setReservaOferecida(reservaOferecida);
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
            Reserva origem = p.getReservaOrigem();
            Reserva oferecida = p.getReservaOferecida();

            // Revalida estado atual: as reservas podem ter mudado de dono/status desde a criacao da proposta
            if (!"CONFIRMADA".equals(origem.getStatus()) || !"CONFIRMADA".equals(oferecida.getStatus())
                    || !oferecida.getSolicitante().getId().equals(p.getUsuarioSolicitante().getId())) {
                throw new ConflitoException("Uma das reservas envolvidas nao esta mais disponivel para troca.");
            }
            validarConflitoDaTroca(origem, oferecida);

            // Troca mutua: cada professor assume a reserva do outro
            Usuario dono = origem.getSolicitante();
            Usuario proponente = oferecida.getSolicitante();
            origem.setSolicitante(proponente);
            oferecida.setSolicitante(dono);
            origem.setDataModificacao(LocalDateTime.now());
            oferecida.setDataModificacao(LocalDateTime.now());
            reservaRepository.save(origem);
            reservaRepository.save(oferecida);

            invalidarPropostasConcorrentes(origem.getId(), oferecida.getId(), p.getId());

            p.setStatus("ACEITA");
        } else {
            p.setStatus("RECUSADA");
        }
        p.setDataModificacao(LocalDateTime.now());
        return propostaRepository.save(p);
    }

    // Garante que, apos a troca, nenhum dos dois professores fique com dois compromissos sobrepostos
    private void validarConflitoDaTroca(Reserva origem, Reserva oferecida) {
        boolean conflitoDono = reservaRepository.existeConflitoPessoal(
                origem.getSolicitante().getId(), oferecida.getDataReserva(),
                oferecida.getHoraInicio(), oferecida.getHoraFim(), origem.getId());
        boolean conflitoProponente = reservaRepository.existeConflitoPessoal(
                oferecida.getSolicitante().getId(), origem.getDataReserva(),
                origem.getHoraInicio(), origem.getHoraFim(), oferecida.getId());
        if (conflitoDono || conflitoProponente) {
            throw new ConflitoException("A troca geraria conflito de horario para um dos professores.");
        }
    }

    // Uma vez efetivada a troca, qualquer outra proposta pendente que envolva as mesmas reservas
    // (como origem ou oferecida) deixou de fazer sentido e e recusada automaticamente
    private void invalidarPropostasConcorrentes(Long idOrigem, Long idOferecida, Long idPropostaAceita) {
        List<PropostaTroca> concorrentes = propostaRepository.findPendentesEnvolvendo(
                List.of(idOrigem, idOferecida), idPropostaAceita);
        for (PropostaTroca c : concorrentes) {
            c.setStatus("RECUSADA");
            c.setDataModificacao(LocalDateTime.now());
        }
        propostaRepository.saveAll(concorrentes);
    }
}
