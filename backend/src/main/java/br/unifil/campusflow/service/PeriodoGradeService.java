package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.PeriodoGrade;
import br.unifil.campusflow.domain.Usuario;
import br.unifil.campusflow.dto.PeriodoGradeRequest;
import br.unifil.campusflow.repository.PeriodoGradeRepository;
import br.unifil.campusflow.security.UsuarioLogado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Controle do periodo de preenchimento da grade bimestral.
 * Enquanto fechado, solicitantes nao lancam reservas GRADE_BIMESTRAL.
 */
@Service
public class PeriodoGradeService {

    private final PeriodoGradeRepository periodoRepository;
    private final UsuarioLogado usuarioLogado;

    public PeriodoGradeService(PeriodoGradeRepository periodoRepository, UsuarioLogado usuarioLogado) {
        this.periodoRepository = periodoRepository;
        this.usuarioLogado = usuarioLogado;
    }

    @Transactional
    public PeriodoGrade obter() {
        return periodoRepository.findById(PeriodoGrade.ID_UNICO)
                .orElseGet(() -> periodoRepository.save(new PeriodoGrade()));
    }

    /** Aberto tambem exige estar dentro da vigencia, quando ela foi informada. */
    @Transactional(readOnly = true)
    public boolean gradeAberta() {
        PeriodoGrade p = periodoRepository.findById(PeriodoGrade.ID_UNICO).orElse(null);
        if (p == null || Boolean.FALSE.equals(p.getAberto())) return false;
        LocalDate hoje = LocalDate.now();
        if (p.getInicioVigencia() != null && hoje.isBefore(p.getInicioVigencia())) return false;
        return p.getFimVigencia() == null || !hoje.isAfter(p.getFimVigencia());
    }

    @Transactional
    public PeriodoGrade alterar(PeriodoGradeRequest dto) {
        Usuario u = usuarioLogado.exigirAdministrativo();
        PeriodoGrade p = obter();
        p.setAberto(Boolean.TRUE.equals(dto.aberto()));
        p.setDescricao(dto.descricao());
        p.setInicioVigencia(dto.inicioVigencia());
        p.setFimVigencia(dto.fimVigencia());
        p.setAtualizadoPor(u);
        p.setDataModificacao(LocalDateTime.now());
        return periodoRepository.save(p);
    }
}
