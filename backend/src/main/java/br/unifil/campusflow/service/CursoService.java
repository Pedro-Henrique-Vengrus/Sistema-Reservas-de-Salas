package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.Curso;
import br.unifil.campusflow.dto.CursoRequest;
import br.unifil.campusflow.exception.ConflitoException;
import br.unifil.campusflow.exception.RecursoNaoEncontradoException;
import br.unifil.campusflow.repository.CursoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CursoService {

    private final CursoRepository cursoRepository;

    public CursoService(CursoRepository cursoRepository) {
        this.cursoRepository = cursoRepository;
    }

    public List<Curso> listarAtivos() {
        return cursoRepository.findByAtivoTrue();
    }

    public Curso buscarPorId(Long id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso nao encontrado: " + id));
    }

    @Transactional
    public Curso criar(CursoRequest dto) {
        if (cursoRepository.existsByNomeIgnoreCase(dto.nome())) {
            throw new ConflitoException("Ja existe um curso com este nome.");
        }
        Curso c = new Curso();
        c.setNome(dto.nome());
        return cursoRepository.save(c);
    }

    @Transactional
    public Curso atualizar(Long id, CursoRequest dto) {
        Curso c = buscarPorId(id);
        c.setNome(dto.nome());
        c.setDataModificacao(LocalDateTime.now());
        return cursoRepository.save(c);
    }

    @Transactional
    public void desativar(Long id) {
        Curso c = buscarPorId(id);
        c.setAtivo(false);
        c.setDataDesativacao(LocalDateTime.now());
        cursoRepository.save(c);
    }
}
