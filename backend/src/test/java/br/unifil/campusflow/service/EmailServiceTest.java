package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.Notificacao;
import br.unifil.campusflow.domain.Role;
import br.unifil.campusflow.domain.TipoNotificacao;
import br.unifil.campusflow.domain.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static br.unifil.campusflow.service.CampusflowFixtures.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tres condicoes precisam ser verdadeiras para um e-mail sair:
 * envio habilitado, adesao do usuario e assunto de troca de sala.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;

    private EmailService servico(boolean habilitado) {
        return new EmailService(mailSender, habilitado, "nao-responda@campusflow.unifil.br");
    }

    private Usuario comEmail(boolean adere) {
        Usuario u = usuario(10L, "Pedro Henrique", Role.PROFESSOR);
        u.setEmail("pedro@campus.br");
        u.setReceberEmails(adere);
        return u;
    }

    private Notificacao aviso(TipoNotificacao tipo) {
        Notificacao n = new Notificacao();
        n.setTipo(tipo);
        n.setTitulo("Nova proposta de troca");
        n.setMensagem("Joao quer trocar o Lab 1008 pela sua reserva.");
        return n;
    }

    @Test
    @DisplayName("Usuario que aderiu recebe e-mail de troca, com remetente e destinatario corretos")
    void enviaQuandoAderiu() {
        servico(true).enviarSeAplicavel(comEmail(true), aviso(TipoNotificacao.TROCA_RECEBIDA));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assertThat(msg.getTo()).containsExactly("pedro@campus.br");
        assertThat(msg.getFrom()).isEqualTo("nao-responda@campusflow.unifil.br");
        assertThat(msg.getSubject()).contains("Nova proposta de troca");
        assertThat(msg.getText())
                .contains("Ola, Pedro.")
                .contains("Joao quer trocar")
                .contains("desmarque a opcao em Preferencias");
    }

    @Test
    @DisplayName("A troca aceita pelo gestor tambem gera e-mail")
    void enviaNaTrocaAceita() {
        servico(true).enviarSeAplicavel(comEmail(true), aviso(TipoNotificacao.TROCA_ACEITA));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Sem adesao do usuario nao sai e-mail, mesmo com envio habilitado")
    void naoEnviaSemAdesao() {
        servico(true).enviarSeAplicavel(comEmail(false), aviso(TipoNotificacao.TROCA_RECEBIDA));
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("Com o envio desabilitado no ambiente nada e enviado")
    void naoEnviaDesabilitado() {
        servico(false).enviarSeAplicavel(comEmail(true), aviso(TipoNotificacao.TROCA_RECEBIDA));
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("Assunto fora do ciclo de troca fica so no sino")
    void naoEnviaOutrosAssuntos() {
        servico(true).enviarSeAplicavel(comEmail(true), aviso(TipoNotificacao.RESERVA_APROVADA));
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("Usuario sem e-mail cadastrado e ignorado")
    void naoEnviaSemEndereco() {
        Usuario u = comEmail(true);
        u.setEmail(null);
        servico(true).enviarSeAplicavel(u, aviso(TipoNotificacao.TROCA_RECEBIDA));
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("Falha de SMTP nao propaga: a troca ja esta concluida")
    void falhaDeSmtpNaoPropaga() {
        doThrow(new MailSendException("servidor indisponivel"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> servico(true)
                .enviarSeAplicavel(comEmail(true), aviso(TipoNotificacao.TROCA_RECEBIDA)))
                .doesNotThrowAnyException();
    }
}
