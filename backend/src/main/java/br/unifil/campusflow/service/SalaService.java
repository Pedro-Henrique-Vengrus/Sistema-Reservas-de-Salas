package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.Curso;
import br.unifil.campusflow.domain.Role;
import br.unifil.campusflow.domain.Sala;
import br.unifil.campusflow.domain.Usuario;
import br.unifil.campusflow.dto.SalaRequest;
import br.unifil.campusflow.exception.RecursoNaoEncontradoException;
import br.unifil.campusflow.repository.CursoRepository;
import br.unifil.campusflow.repository.SalaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SalaService {

    private final SalaRepository salaRepository;
    private final CursoRepository cursoRepository;

    public SalaService(SalaRepository salaRepository, CursoRepository cursoRepository) {
        this.salaRepository = salaRepository;
        this.cursoRepository = cursoRepository;
    }

    public List<Sala> listarTodas() {
        return salaRepository.findByAtivoTrue();
    }

    // Salas visiveis ao curso do solicitante (visibilidade setorizada)
    public List<Sala> listarVisiveisPorCurso(Long cursoId) {
        return salaRepository.findVisiveisPorCurso(cursoId);
    }

    // Visibilidade setorizada decidida pela ROLE do usuario logado, nunca pelo cursoId vindo do cliente:
    // GESTOR ve todas (o cursoId, se informado, so filtra dentro desse universo);
    // solicitante ve apenas as do PROPRIO curso (o cursoId do parametro e ignorado);
    // solicitante sem curso vinculado nao ve nenhuma sala.
    public List<Sala> listarVisiveis(Usuario usuarioLogado, Long cursoIdParam) {
        if (usuarioLogado.getRole() == Role.GESTOR) {
            return (cursoIdParam != null) ? listarVisiveisPorCurso(cursoIdParam) : listarTodas();
        }
        Curso curso = usuarioLogado.getCurso();
        return (curso != null) ? listarVisiveisPorCurso(curso.getId()) : List.of();
    }

    public Sala buscarPorId(Long id) {
        return salaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sala nao encontrada: " + id));
    }

    @Transactional
    public Sala criar(SalaRequest dto) {
        Sala s = new Sala();
        aplicar(s, dto);
        return salaRepository.save(s);
    }

    @Transactional
    public Sala atualizar(Long id, SalaRequest dto) {
        Sala s = buscarPorId(id);
        aplicar(s, dto);
        s.setDataModificacao(LocalDateTime.now());
        return salaRepository.save(s);
    }

    @Transactional
    public void desativar(Long id) {
        Sala s = buscarPorId(id);
        s.setAtivo(false);
        s.setSituacao("DESATIVADA");
        s.setDataDesativacao(LocalDateTime.now());
        salaRepository.save(s);
    }

    private void aplicar(Sala s, SalaRequest dto) {
        s.setNome(dto.nome());
        s.setTipo(dto.tipo());
        s.setCapacidade(dto.capacidade());
        s.setAndar(dto.andar());

        Set<Curso> cursos = new HashSet<>();
        if (dto.cursoIds() != null) {
            for (Long cid : dto.cursoIds()) {
                Curso c = cursoRepository.findById(cid)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Curso nao encontrado: " + cid));
                cursos.add(c);
            }
        }
        s.setCursos(cursos);
    }
}
