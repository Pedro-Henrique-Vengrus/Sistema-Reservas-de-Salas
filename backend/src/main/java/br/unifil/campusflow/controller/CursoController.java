package br.unifil.campusflow.controller;

import br.unifil.campusflow.domain.StatusRegistro;
import br.unifil.campusflow.dto.CursoRequest;
import br.unifil.campusflow.dto.CursoResponse;
import br.unifil.campusflow.dto.ImpactoResponse;
import br.unifil.campusflow.service.CursoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cursos")
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @GetMapping
    public List<CursoResponse> listar(@RequestParam(required = false) String termo,
                                      @RequestParam(required = false) StatusRegistro status) {
        return cursoService.listar(termo, status).stream().map(CursoResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CursoResponse buscar(@PathVariable Long id) {
        return CursoResponse.from(cursoService.buscarPorId(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CursoResponse criar(@Valid @RequestBody CursoRequest req) {
        return CursoResponse.from(cursoService.criar(req));
    }

    @PutMapping("/{id}")
    public CursoResponse atualizar(@PathVariable Long id, @Valid @RequestBody CursoRequest req) {
        return CursoResponse.from(cursoService.atualizar(id, req));
    }

    /** Previa do impacto antes de inativar/excluir (alimenta o dialogo de confirmacao). */
    @GetMapping("/{id}/impacto")
    public ImpactoResponse impacto(@PathVariable Long id) {
        return cursoService.impacto(id);
    }

    /** Exclusao logica; forcar=true cancela as reservas futuras e notifica os solicitantes. */
    @DeleteMapping("/{id}")
    public CursoResponse inativar(@PathVariable Long id,
                                  @RequestParam(defaultValue = "false") boolean forcar) {
        return CursoResponse.from(cursoService.inativar(id, forcar));
    }

    @PostMapping("/{id}/reativar")
    public CursoResponse reativar(@PathVariable Long id) {
        return CursoResponse.from(cursoService.reativar(id));
    }

    @DeleteMapping("/{id}/permanente")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        cursoService.excluirFisicamente(id);
        return ResponseEntity.noContent().build();
    }
}
