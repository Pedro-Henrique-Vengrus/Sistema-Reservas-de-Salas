package br.unifil.campusflow.controller;

import br.unifil.campusflow.domain.Role;
import br.unifil.campusflow.domain.StatusRegistro;
import br.unifil.campusflow.dto.UsuarioRequest;
import br.unifil.campusflow.dto.UsuarioResponse;
import br.unifil.campusflow.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    public UsuarioResponse eu() {
        return UsuarioResponse.from(usuarioService.eu());
    }

    @GetMapping
    public List<UsuarioResponse> listar(@RequestParam(required = false) String termo,
                                        @RequestParam(required = false) Role role,
                                        @RequestParam(required = false) StatusRegistro status,
                                        @RequestParam(required = false) Long cursoId) {
        return usuarioService.listar(termo, role, status, cursoId).stream().map(UsuarioResponse::from).toList();
    }

    /** Solicitantes ativos: destino de uma reserva lancada pelo painel administrativo. */
    @GetMapping("/elegiveis")
    public List<UsuarioResponse> elegiveis() {
        return usuarioService.elegiveisParaReserva().stream().map(UsuarioResponse::from).toList();
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable Long id) {
        return UsuarioResponse.from(usuarioService.buscarPorId(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody UsuarioRequest req) {
        return UsuarioResponse.from(usuarioService.criar(req));
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest req) {
        return UsuarioResponse.from(usuarioService.atualizar(id, req));
    }

    @PutMapping("/{id}/cursos")
    public UsuarioResponse definirCursos(@PathVariable Long id, @RequestBody Map<String, Set<Long>> corpo) {
        return UsuarioResponse.from(usuarioService.definirCursos(id, corpo.get("cursoIds")));
    }

    /** Exclusao logica (soft-delete): o usuario deixa de autenticar mas o historico e preservado. */
    @DeleteMapping("/{id}")
    public UsuarioResponse inativar(@PathVariable Long id) {
        return UsuarioResponse.from(usuarioService.inativar(id));
    }

    @PostMapping("/{id}/reativar")
    public UsuarioResponse reativar(@PathVariable Long id) {
        return UsuarioResponse.from(usuarioService.reativar(id));
    }
}
