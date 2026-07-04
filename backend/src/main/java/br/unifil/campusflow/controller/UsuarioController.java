package br.unifil.campusflow.controller;

import br.unifil.campusflow.dto.UsuarioResponse;
import br.unifil.campusflow.service.UsuarioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResponse> elegiveisParaReserva() {
        return usuarioService.elegiveisParaReserva().stream().map(UsuarioResponse::from).toList();
    }
}
