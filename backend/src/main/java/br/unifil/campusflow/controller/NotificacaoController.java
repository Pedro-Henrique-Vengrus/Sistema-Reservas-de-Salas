package br.unifil.campusflow.controller;

import br.unifil.campusflow.dto.NotificacaoResponse;
import br.unifil.campusflow.service.NotificacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping
    public List<NotificacaoResponse> minhas() {
        return notificacaoService.minhas().stream().map(NotificacaoResponse::from).toList();
    }

    @GetMapping("/nao-lidas/count")
    public Map<String, Long> naoLidas() {
        return Map.of("count", notificacaoService.naoLidas());
    }

    @PostMapping("/{id}/lida")
    public ResponseEntity<Void> marcarComoLida(@PathVariable Long id) {
        notificacaoService.marcarComoLida(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/marcar-todas-lidas")
    public Map<String, Integer> marcarTodas() {
        return Map.of("atualizadas", notificacaoService.marcarTodasComoLidas());
    }
}
