package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.Sala;
import br.unifil.campusflow.domain.StatusRegistro;
import br.unifil.campusflow.domain.Usuario;
import br.unifil.campusflow.exception.AcessoNegadoException;
import br.unifil.campusflow.repository.SalaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Regra central de visibilidade setorizada.
 *
 * Um solicitante so enxerga (e so reserva) ambientes ligados a pelo menos um dos cursos
 * ATIVOS do seu perfil. Perfis administrativos enxergam todo o catalogo.
 * Este e o unico ponto onde a regra e avaliada.
 */
@Service
public class VisibilidadeService {

    private final SalaRepository salaRepository;

    public VisibilidadeService(SalaRepository salaRepository) {
        this.salaRepository = salaRepository;
    }

    @Transactional(readOnly = true)
    public Set<Long> cursoIdsDe(Usuario usuario) {
        return usuario.getCursos().stream()
                .filter(c -> c.getStatus() == StatusRegistro.ATIVO)
                .map(c -> c.getId())
                .collect(Collectors.toSet());
    }

    /** Perfis administrativos veem tudo; solicitante sem curso ativo nao ve nada. */
    public boolean veTodoOCatalogo(Usuario usuario) {
        return usuario.ehAdministrativo();
    }

    @Transactional(readOnly = true)
    public boolean podeVerSala(Usuario usuario, Long salaId) {
        if (veTodoOCatalogo(usuario)) return true;
        Set<Long> cursoIds = cursoIdsDe(usuario);
        if (cursoIds.isEmpty()) return false;
        return salaRepository.ehVisivelPara(salaId, cursoIds);
    }

    @Transactional(readOnly = true)
    public void exigirAcessoASala(Usuario usuario, Sala sala) {
        if (!podeVerSala(usuario, sala.getId())) {
            throw new AcessoNegadoException(
                    "O ambiente \"" + sala.getNome() + "\" nao pertence aos cursos vinculados ao seu perfil.");
        }
    }
}
