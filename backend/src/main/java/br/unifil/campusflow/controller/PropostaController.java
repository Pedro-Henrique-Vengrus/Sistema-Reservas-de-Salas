package br.unifil.campusflow.controller;

import br.unifil.campusflow.dto.PropostaRequest;
import br.unifil.campusflow.dto.PropostaResponse;
import br.unifil.campusflow.service.PropostaTrocaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/propostas")
public class PropostaController {

    private final PropostaTrocaService propostaService;

    public PropostaController(PropostaTrocaService propostaService) {
        this.propostaService = propostaService;
    }

    @GetMapping("/enviadas")
    public List<PropostaResponse> enviadas() {
        return propostaService.enviadas().stream().map(PropostaResponse::from).toList();
    }

    @GetMapping("/recebidas")
    public List<PropostaResponse> recebidas() {
        return propostaService.recebidas().stream().map(PropostaResponse::from).toList();
    }

    @GetMapping("/pendentes/count")
    public Map<String, Long> pendentes() {
        return Map.of("count", propostaService.pendentesRecebidas());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropostaResponse criar(@Valid @RequestBody PropostaRequest req) {
        return PropostaResponse.from(propostaService.criar(req));
    }

    @PostMapping("/{id}/aceitar")
    public PropostaResponse aceitar(@PathVariable Long id) {
        return PropostaResponse.from(propostaService.responder(id, true));
    }

    @PostMapping("/{id}/recusar")
    public PropostaResponse recusar(@PathVariable Long id) {
        return PropostaResponse.from(propostaService.responder(id, false));
    }

    @PostMapping("/{id}/cancelar")
    public PropostaResponse cancelar(@PathVariable Long id) {
        return PropostaResponse.from(propostaService.cancelar(id));
    }
}
