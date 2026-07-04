package br.unifil.campusflow.controller;

import br.unifil.campusflow.domain.Usuario;
import br.unifil.campusflow.dto.SalaRequest;
import br.unifil.campusflow.dto.SalaResponse;
import br.unifil.campusflow.security.UsuarioLogado;
import br.unifil.campusflow.service.SalaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salas")
public class SalaController {

    private final SalaService salaService;
    private final UsuarioLogado usuarioLogado;

    public SalaController(SalaService salaService, UsuarioLogado usuarioLogado) {
        this.salaService = salaService;
        this.usuarioLogado = usuarioLogado;
    }

    // Visibilidade setorizada decidida pela role do usuario logado (nao pelo cursoId vindo do cliente):
    // GESTOR ve todas; solicitante ve so as do seu curso; solicitante sem curso nao ve nenhuma.
    @GetMapping
    public List<SalaResponse> listar(@RequestParam(required = false) Long cursoId) {
        Usuario u = usuarioLogado.get();
        var salas = salaService.listarVisiveis(u, cursoId);
        return salas.stream().map(SalaResponse::from).toList();
    }

    @GetMapping("/{id}")
    public SalaResponse buscar(@PathVariable Long id) {
        return SalaResponse.from(salaService.buscarPorId(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalaResponse criar(@Valid @RequestBody SalaRequest req) {
        return SalaResponse.from(salaService.criar(req));
    }

    @PutMapping("/{id}")
    public SalaResponse atualizar(@PathVariable Long id, @Valid @RequestBody SalaRequest req) {
        return SalaResponse.from(salaService.atualizar(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        salaService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
