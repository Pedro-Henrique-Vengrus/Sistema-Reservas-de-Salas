package br.unifil.campusflow.repository;

import br.unifil.campusflow.domain.Notificacao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    @Query("SELECT n FROM Notificacao n WHERE n.destinatario.id = :usuarioId ORDER BY n.dataCriacao DESC")
    List<Notificacao> findDoUsuario(@Param("usuarioId") Long usuarioId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notificacao n WHERE n.destinatario.id = :usuarioId AND n.lida = false")
    long countNaoLidas(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("UPDATE Notificacao n SET n.lida = true, n.dataLeitura = :agora "
         + "WHERE n.destinatario.id = :usuarioId AND n.lida = false")
    int marcarTodasComoLidas(@Param("usuarioId") Long usuarioId, @Param("agora") LocalDateTime agora);
}
