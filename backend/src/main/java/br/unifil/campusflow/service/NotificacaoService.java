package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.Notificacao;
import br.unifil.campusflow.domain.TipoNotificacao;
import br.unifil.campusflow.domain.Usuario;
import br.unifil.campusflow.exception.RecursoNaoEncontradoException;
import br.unifil.campusflow.repository.NotificacaoRepository;
import br.unifil.campusflow.security.UsuarioLogado;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Avisos in-app: trocas, moderacao de reservas e cancelamentos forcados. */
@Service
public class NotificacaoService {

    private static final int LIMITE_PADRAO = 50;

    private final NotificacaoRepository notificacaoRepository;
    private final UsuarioLogado usuarioLogado;
    private final EmailService emailService;

    public NotificacaoService(NotificacaoRepository notificacaoRepository,
                              UsuarioLogado usuarioLogado,
                              EmailService emailService) {
        this.notificacaoRepository = notificacaoRepository;
        this.usuarioLogado = usuarioLogado;
        this.emailService = emailService;
    }

    @Transactional
    public Notificacao notificar(Usuario destinatario, TipoNotificacao tipo, String titulo, String mensagem) {
        Notificacao n = new Notificacao();
        n.setDestinatario(destinatario);
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensagem(truncar(mensagem, 500));
        Notificacao salva = notificacaoRepository.save(n);

        // O sino e a via principal; o e-mail e um espelho opcional, por adesao
        emailService.enviarSeAplicavel(destinatario, salva);
        return salva;
    }

    @Transactional(readOnly = true)
    public List<Notificacao> minhas() {
        return notificacaoRepository.findDoUsuario(usuarioLogado.get().getId(), PageRequest.of(0, LIMITE_PADRAO));
    }

    @Transactional(readOnly = true)
    public long naoLidas() {
        return notificacaoRepository.countNaoLidas(usuarioLogado.get().getId());
    }

    @Transactional
    public void marcarComoLida(Long id) {
        Usuario u = usuarioLogado.get();
        Notificacao n = notificacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificacao nao encontrada"));
        if (!n.getDestinatario().getId().equals(u.getId())) {
            throw new RecursoNaoEncontradoException("Notificacao nao encontrada");
        }
        if (Boolean.FALSE.equals(n.getLida())) {
            n.setLida(true);
            n.setDataLeitura(LocalDateTime.now());
            notificacaoRepository.save(n);
        }
    }

    @Transactional
    public int marcarTodasComoLidas() {
        return notificacaoRepository.marcarTodasComoLidas(usuarioLogado.get().getId(), LocalDateTime.now());
    }

    private String truncar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() <= max ? texto : texto.substring(0, max - 3) + "...";
    }
}
