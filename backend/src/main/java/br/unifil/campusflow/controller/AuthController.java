package br.unifil.campusflow.controller;

import br.unifil.campusflow.domain.Usuario;
import br.unifil.campusflow.dto.LoginRequest;
import br.unifil.campusflow.dto.LoginResponse;
import br.unifil.campusflow.repository.UsuarioRepository;
import br.unifil.campusflow.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.senha()));

        Usuario u = usuarioRepository.findByEmail(req.email()).orElseThrow();

        String token = jwtService.generateToken(u.getEmail(), Map.of(
                "role", u.getRole().name(),
                "nome", u.getNome()
        ));

        return ResponseEntity.ok(LoginResponse.from(token, u));
    }
}
