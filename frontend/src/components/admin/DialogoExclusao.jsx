import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import { Modal, Notice } from '../ui/primitives';

/**
 * Decide a remocao de um curso ou ambiente em um passo so.
 *
 * O ciclo de vida tem dois estagios (ATIVO -> INATIVO -> exclusao fisica) e antes disso o
 * botao executava o primeiro deles direto: quem queria apagar clicava, o registro continuava
 * na lista como "Inativo" e nada explicava que faltava um segundo passo. Aqui o dialogo
 * consulta /impacto primeiro e ja oferece o desfecho possivel:
 *
 *   - sem nada vinculado  -> "Excluir definitivamente" some com o registro de vez;
 *   - com historico       -> explica o que prende e oferece apenas a inativacao.
 *
 * Inativar continua disponivel a parte, para quem quer so tirar de circulação sem perder o
 * historico.
 */
export default function DialogoExclusao({ recurso, rotulo, item, onFechar, onConcluido }) {
  const [impacto, setImpacto] = useState(null);
  const [erro, setErro] = useState('');
  const [ocupado, setOcupado] = useState('');

  useEffect(() => {
    let vivo = true;
    api.get(`/${recurso}/${item.id}/impacto`)
      .then((i) => { if (vivo) setImpacto(i); })
      .catch((e) => { if (vivo) setErro(e.message); });
    return () => { vivo = false; };
  }, [recurso, item.id]);

  const ativo = item.status === 'ATIVO';

  async function excluirDeVez() {
    setOcupado('excluir');
    try {
      // Um registro ativo precisa passar por INATIVO antes da exclusao fisica.
      if (ativo) await api.del(`/${recurso}/${item.id}?forcar=true`);
      await api.del(`/${recurso}/${item.id}/permanente`);
      onConcluido(`${maiuscula(rotulo)} excluído definitivamente.`);
    } catch (e) { setErro(e.message); } finally { setOcupado(''); }
  }

  async function apenasInativar() {
    setOcupado('inativar');
    try {
      await api.del(`/${recurso}/${item.id}?forcar=true`);
      onConcluido(impacto?.reservasFuturasAtivas > 0
        ? `${maiuscula(rotulo)} inativado e reservas futuras canceladas.`
        : `${maiuscula(rotulo)} inativado.`);
    } catch (e) { setErro(e.message); } finally { setOcupado(''); }
  }

  const carregando = !impacto && !erro;
  const podeExcluir = impacto?.podeExcluirFisicamente;

  return (
    <Modal titulo={`Excluir ${rotulo}`} subtitulo={item.nome} tamanho="sm" onClose={onFechar}
      rodape={(
        <>
          <button className="btn btn-secondary" onClick={onFechar}>Cancelar</button>
          {ativo && (
            <button className="btn btn-secondary" onClick={apenasInativar}
              disabled={carregando || !!ocupado}>
              {ocupado === 'inativar' ? 'Inativando…' : 'Apenas inativar'}
            </button>
          )}
          <button className="btn btn-danger-solid" onClick={excluirDeVez}
            disabled={carregando || !!ocupado || !podeExcluir}>
            {ocupado === 'excluir' ? 'Excluindo…' : 'Excluir definitivamente'}
          </button>
        </>
      )}>

      {carregando && <p className="text-md text-muted">Verificando o que está vinculado…</p>}
      {erro && <Notice tom="danger">{erro}</Notice>}

      {impacto && !podeExcluir && (
        <>
          <Notice tom="warn">
            <strong>Não dá para excluir de vez</strong>
            <p className="text-sm mt-2">{impacto.bloqueio}</p>
          </Notice>
          <p className="text-md mt-4">
            {ativo
              ? <>Você pode <strong>inativar</strong>: {item.nome} sai de circulação e para de aparecer
                  para os solicitantes, mas o histórico é preservado e dá para reativar depois.</>
              : <>{item.nome} já está inativo. Enquanto houver histórico vinculado ele precisa
                  continuar assim — inativo não atrapalha, apenas não aparece para os solicitantes.</>}
          </p>
        </>
      )}

      {impacto && podeExcluir && (
        <>
          <p className="text-md">
            Nada está vinculado a {item.nome}. A exclusão remove o registro do banco
            e <strong>não tem volta</strong>.
          </p>
          {ativo && (
            <p className="text-md mt-4 text-muted">
              Se preferir guardar para depois, <strong>Apenas inativar</strong> tira de circulação
              sem apagar — e dá para reativar quando quiser.
            </p>
          )}
        </>
      )}

      {impacto && (impacto.reservasTotais > 0 || impacto.usuariosVinculados > 0) && (
        <dl className="dl mt-4">
          {impacto.usuariosVinculados > 0 && (
            <><dt>Usuários vinculados</dt><dd>{impacto.usuariosVinculados}</dd></>
          )}
          {impacto.reservasTotais > 0 && (
            <><dt>Reservas no histórico</dt><dd>{impacto.reservasTotais}</dd></>
          )}
          {impacto.reservasFuturasAtivas > 0 && (
            <><dt>Reservas futuras que seriam canceladas</dt><dd>{impacto.reservasFuturasAtivas}</dd></>
          )}
        </dl>
      )}
    </Modal>
  );
}

function maiuscula(t) {
  return t.charAt(0).toUpperCase() + t.slice(1);
}
