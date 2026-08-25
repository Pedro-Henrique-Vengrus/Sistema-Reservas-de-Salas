package br.unifil.campusflow.security;

import br.unifil.campusflow.domain.Usuario;
import br.unifil.campusflow.exception.AcessoNegadoException;
import br.unifil.campusflow.repository.UsuarioRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class UsuarioLogado {

    private final UsuarioRepository usuarioRepository;

    public UsuarioLogado(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario get() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = (principal instanceof UserDetails ud) ? ud.getUsername() : principal.toString();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario logado nao encontrado"));
    }

    /** Exige o perfil ADMIN: e o unico que opera o painel administrativo. */
    public Usuario exigirAdministrativo() {
        Usuario u = get();
        if (!u.ehAdministrativo()) {
            throw new AcessoNegadoException("Acao restrita ao painel administrativo.");
        }
        return u;
    }
}
