package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.Role;
import br.unifil.campusflow.domain.Usuario;
import br.unifil.campusflow.exception.ConflitoException;
import br.unifil.campusflow.repository.UsuarioRepository;
import br.unifil.campusflow.security.UsuarioLogado;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioLogado usuarioLogado;

    public UsuarioService(UsuarioRepository usuarioRepository, UsuarioLogado usuarioLogado) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioLogado = usuarioLogado;
    }

    public List<Usuario> elegiveisParaReserva() {
        Usuario u = usuarioLogado.get();
        boolean ehAdmin = u.getRole() == Role.ADMIN || u.getRole() == Role.GESTOR;
        if (!ehAdmin) {
            throw new ConflitoException("Apenas o Admin pode listar usuarios.");
        }
        return usuarioRepository.findElegiveisParaReserva();
    }
}
