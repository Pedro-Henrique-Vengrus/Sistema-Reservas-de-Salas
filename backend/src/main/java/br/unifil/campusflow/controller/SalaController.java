package br.unifil.campusflow.controller;

import br.unifil.campusflow.domain.StatusRegistro;
import br.unifil.campusflow.domain.TipoAmbiente;
import br.unifil.campusflow.dto.ImpactoResponse;
import br.unifil.campusflow.dto.SalaRequest;
import br.unifil.campusflow.dto.SalaResponse;
import br.unifil.campusflow.service.SalaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/salas")
public class SalaController {

    private final SalaService salaService;

    public SalaController(SalaService salaService) {
        this.salaService = salaService;
    }

    /**
     * Visibilidade setorizada decidida pela role do usuario logado, nunca por parametro do cliente.
     * Os filtros abaixo recortam esse universo, jamais o ampliam.
     */
    @GetMapping
    public List<SalaResponse> listar(@RequestParam(required = false) String termo,
                                     @RequestParam(required = false) StatusRegistro status,
                                     @RequestParam(required = false) TipoAmbiente tipo,
                                     @RequestParam(required = false) Integer capacidadeMinima,
                                     @RequestParam(required = false) Long cursoId) {
        return salaService.listar(termo, status, tipo, capacidadeMinima, cursoId)
                .stream().map(SalaResponse::from).toList();
    }

    /** Catalogo de tipos de ambiente para os formularios. */
    @GetMapping("/tipos")
    public List<Map<String, String>> tipos() {
        return java.util.Arrays.stream(TipoAmbiente.values())
                .map(t -> Map.of("valor", t.name(), "rotulo", t.getRotulo()))
                .toList();
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

    @GetMapping("/{id}/impacto")
    public ImpactoResponse impacto(@PathVariable Long id) {
        return salaService.impacto(id);
    }

    /** Exclusao logica; forcar=true cancela as reservas futuras e notifica os solicitantes. */
    @DeleteMapping("/{id}")
    public SalaResponse inativar(@PathVariable Long id,
                                 @RequestParam(defaultValue = "false") boolean forcar) {
        return SalaResponse.from(salaService.inativar(id, forcar));
    }

    @PostMapping("/{id}/reativar")
    public SalaResponse reativar(@PathVariable Long id) {
        return SalaResponse.from(salaService.reativar(id));
    }

    @DeleteMapping("/{id}/permanente")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        salaService.excluirFisicamente(id);
        return ResponseEntity.noContent().build();
    }
}
