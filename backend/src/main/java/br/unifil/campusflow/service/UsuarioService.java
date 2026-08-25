package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.Curso;
import br.unifil.campusflow.domain.Role;
import br.unifil.campusflow.domain.StatusRegistro;
import br.unifil.campusflow.domain.Usuario;
import br.unifil.campusflow.dto.UsuarioRequest;
import br.unifil.campusflow.exception.ConflitoException;
import br.unifil.campusflow.exception.RecursoNaoEncontradoException;
import br.unifil.campusflow.repository.CursoRepository;
import br.unifil.campusflow.repository.UsuarioRepository;
import br.unifil.campusflow.security.UsuarioLogado;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioLogado usuarioLogado;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          CursoRepository cursoRepository,
                          PasswordEncoder passwordEncoder,
                          UsuarioLogado usuarioLogado) {
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioLogado = usuarioLogado;
    }

    @Transactional(readOnly = true)
    public Usuario eu() {
        return usuarioLogado.get();
    }

    /** Adesao (ou saida) do proprio usuario aos avisos por e-mail. Nao exige perfil administrativo. */
    @Transactional
    public Usuario definirReceberEmails(boolean receber) {
        Usuario u = usuarioLogado.get();
        u.setReceberEmails(receber);
        u.setDataModificacao(LocalDateTime.now());
        return usuarioRepository.save(u);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar(String termo, Role role, StatusRegistro status, Long cursoId) {
        usuarioLogado.exigirAdministrativo();
        return usuarioRepository.buscar(termo, role, status, cursoId);
    }

    /** Usuarios que podem receber uma reserva lancada pelo painel administrativo. */
    @Transactional(readOnly = true)
    public List<Usuario> elegiveisParaReserva() {
        usuarioLogado.exigirAdministrativo();
        return usuarioRepository.findSolicitantesAtivos();
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {
        usuarioLogado.exigirAdministrativo();
        return carregar(id);
    }

    @Transactional
    public Usuario criar(UsuarioRequest dto) {
        usuarioLogado.exigirAdministrativo();
        if (dto.senha() == null || dto.senha().isBlank()) {
            throw new ConflitoException("Informe uma senha inicial para o usuario.");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(dto.email())) {
            throw new ConflitoException("Ja existe um usuario com este email.");
        }

        Usuario u = new Usuario();
        u.setNome(dto.nome());
        u.setEmail(dto.email().toLowerCase());
        u.setSenha(passwordEncoder.encode(dto.senha()));
        u.setRole(dto.role());
        u.setCursos(resolverCursos(dto.cursoIds(), dto.role()));
        return usuarioRepository.save(u);
    }

    @Transactional
    public Usuario atualizar(Long id, UsuarioRequest dto) {
        Usuario logado = usuarioLogado.exigirAdministrativo();
        Usuario u = carregar(id);

        if (usuarioRepository.existsOutroComEmail(dto.email(), id)) {
            throw new ConflitoException("Ja existe outro usuario com este email.");
        }
        if (u.getId().equals(logado.getId()) && dto.role() != u.getRole()) {
            throw new ConflitoException("Voce nao pode alterar o proprio perfil de acesso.");
        }

        u.setNome(dto.nome());
        u.setEmail(dto.email().toLowerCase());
        u.setRole(dto.role());
        u.setCursos(resolverCursos(dto.cursoIds(), dto.role()));
        // Senha em branco na edicao mantem a atual
        if (dto.senha() != null && !dto.senha().isBlank()) {
            u.setSenha(passwordEncoder.encode(dto.senha()));
        }
        u.setDataModificacao(LocalDateTime.now());
        return usuarioRepository.save(u);
    }

    /** Atribuicao de cursos isolada, usada pela acao rapida da tabela de usuarios. */
    @Transactional
    public Usuario definirCursos(Long id, Set<Long> cursoIds) {
        usuarioLogado.exigirAdministrativo();
        Usuario u = carregar(id);
        u.setCursos(resolverCursos(cursoIds, u.getRole()));
        u.setDataModificacao(LocalDateTime.now());
        return usuarioRepository.save(u);
    }

    @Transactional
    public Usuario inativar(Long id) {
        Usuario logado = usuarioLogado.exigirAdministrativo();
        Usuario u = carregar(id);
        if (u.getId().equals(logado.getId())) {
            throw new ConflitoException("Voce nao pode desativar o proprio usuario.");
        }
        if (!u.estaAtivo()) {
            throw new ConflitoException("Este usuario ja esta inativo.");
        }
        u.setStatus(StatusRegistro.INATIVO);
        u.setDataDesativacao(LocalDateTime.now());
        u.setDataModificacao(LocalDateTime.now());
        return usuarioRepository.save(u);
    }

    @Transactional
    public Usuario reativar(Long id) {
        usuarioLogado.exigirAdministrativo();
        Usuario u = carregar(id);
        u.setStatus(StatusRegistro.ATIVO);
        u.setDataDesativacao(null);
        u.setDataModificacao(LocalDateTime.now());
        return usuarioRepository.save(u);
    }

    private Usuario carregar(Long id) {
        return usuarioRepository.findByIdComCursos(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado: " + id));
    }

    /**
     * O ADMIN administra o sistema e nao solicita reservas, entao nao precisa de curso.
     * PROFESSOR e REITOR sao solicitantes: sem curso vinculado nao enxergam ambiente nenhum.
     */
    private Set<Curso> resolverCursos(Set<Long> cursoIds, Role role) {
        Set<Curso> cursos = new HashSet<>();
        if (cursoIds != null) {
            for (Long cid : cursoIds) {
                cursos.add(cursoRepository.findById(cid)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Curso nao encontrado: " + cid)));
            }
        }
        if (cursos.isEmpty() && role != Role.ADMIN) {
            throw new ConflitoException(
                    "Vincule ao menos um curso: sem vinculo o usuario nao enxerga nenhum ambiente.");
        }
        return cursos;
    }
}
