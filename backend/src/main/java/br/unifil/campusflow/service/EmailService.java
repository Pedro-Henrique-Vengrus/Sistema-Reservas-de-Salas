package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.Notificacao;
import br.unifil.campusflow.domain.TipoNotificacao;
import br.unifil.campusflow.domain.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Aviso por e-mail no endereco academico do usuario.
 *
 * Tres condicoes precisam ser verdadeiras para um e-mail sair: o envio esta
 * habilitado no ambiente, o usuario aderiu explicitamente e o assunto e uma
 * troca de sala. Fora disso o aviso fica apenas no sino da aplicacao.
 *
 * O envio e assincrono e nunca propaga erro: uma falha de SMTP nao pode
 * desfazer a troca nem derrubar a requisicao que a originou.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    /** Assuntos que geram e-mail: o ciclo da troca de sala. */
    private static final Set<TipoNotificacao> ASSUNTOS_POR_EMAIL = Set.of(
            TipoNotificacao.TROCA_RECEBIDA,
            TipoNotificacao.TROCA_AGUARDA_GESTOR,
            TipoNotificacao.TROCA_ACEITA,
            TipoNotificacao.TROCA_RECUSADA,
            TipoNotificacao.TROCA_CANCELADA);

    private final JavaMailSender mailSender;
    private final boolean habilitado;
    private final String remetente;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.email.enabled}") boolean habilitado,
                        @Value("${app.email.remetente}") String remetente) {
        this.mailSender = mailSender;
        this.habilitado = habilitado;
        this.remetente = remetente;
    }

    public boolean assuntoGeraEmail(TipoNotificacao tipo) {
        return ASSUNTOS_POR_EMAIL.contains(tipo);
    }

    /** Decide e envia. Chamado pelo NotificacaoService a cada aviso criado. */
    @Async
    public void enviarSeAplicavel(Usuario destinatario, Notificacao n) {
        if (!assuntoGeraEmail(n.getTipo())) return;
        if (!destinatario.querReceberEmails()) return;
        if (destinatario.getEmail() == null || destinatario.getEmail().isBlank()) return;

        if (!habilitado) {
            log.info("[email desabilitado] enviaria para {}: {}", destinatario.getEmail(), n.getTitulo());
            return;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(remetente);
            msg.setTo(destinatario.getEmail());
            msg.setSubject("CampusFlow · " + n.getTitulo());
            msg.setText(corpo(destinatario, n));
            mailSender.send(msg);
            log.info("E-mail enviado para {} ({})", destinatario.getEmail(), n.getTipo());
        } catch (Exception e) {
            // Nunca propaga: o aviso no sino ja foi gravado e a operacao de negocio esta concluida
            log.warn("Falha ao enviar e-mail para {}: {}", destinatario.getEmail(), e.getMessage());
        }
    }

    private String corpo(Usuario destinatario, Notificacao n) {
        return """
               Ola, %s.

               %s

               %s

               ---
               Voce recebe este aviso porque ativou os e-mails de troca de sala no CampusFlow.
               Para desativar, entre no sistema e desmarque a opcao em Preferencias.
               Esta e uma mensagem automatica; nao responda.
               """.formatted(
                primeiroNome(destinatario.getNome()),
                n.getTitulo(),
                n.getMensagem());
    }

    private String primeiroNome(String nome) {
        if (nome == null || nome.isBlank()) return "";
        return nome.trim().split("\\s+")[0];
    }
}
