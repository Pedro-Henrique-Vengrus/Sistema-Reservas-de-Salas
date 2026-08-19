import { useCallback, useEffect, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { api } from '../../api/client';
import DataTable from '../../components/ui/DataTable';
import { useToast } from '../../components/ui/ToastProvider';
import {
  EmptyState, Field, Modal, Notice, PageHeader, StatusBadge,
} from '../../components/ui/primitives';
import { dataBr, diaDaSemana, hhmm } from '../../lib/format';

/** Fila de aprovacao das solicitacoes de ultima hora. */
export default function Moderacao() {
  const toast = useToast();
  const { recarregarContadores } = useOutletContext() || {};
  const [fila, setFila] = useState([]);
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(true);
  const [recusando, setRecusando] = useState(null);
  const [motivo, setMotivo] = useState('');

  const carregar = useCallback(() => {
    setCarregando(true);
    api.get('/reservas/moderacao')
      .then(setFila)
      .catch((e) => setErro(e.message))
      .finally(() => setCarregando(false));
  }, []);

  useEffect(() => { carregar(); }, [carregar]);

  async function aprovar(r) {
    try {
      await api.post(`/reservas/${r.id}/aprovar`);
      toast.sucesso(`Reserva de ${r.solicitanteNome} aprovada — o solicitante foi notificado.`);
      carregar();
      recarregarContadores?.();
    } catch (e) { toast.erro(e.message); }
  }

  async function recusar() {
    try {
      await api.post(`/reservas/${recusando.id}/recusar`, { motivo });
      toast.sucesso('Solicitação recusada — o solicitante foi notificado.');
      setRecusando(null);
      setMotivo('');
      carregar();
      recarregarContadores?.();
    } catch (e) { toast.erro(e.message); }
  }

  const colunas = [
    { chave: 'data', titulo: 'Data', largura: 150,
      render: (r) => (
        <span className="nowrap">{dataBr(r.data)} <span className="text-muted text-sm">{diaDaSemana(r.data, true)}</span></span>
      ) },
    { chave: 'horaInicio', titulo: 'Horário', largura: 130,
      render: (r) => <span className="nowrap">{hhmm(r.horaInicio)}–{hhmm(r.horaFim)}</span> },
    { chave: 'turno', titulo: 'Turno', render: (r) => <StatusBadge valor={r.turno} /> },
    { chave: 'salaNome', titulo: 'Ambiente',
      render: (r) => (
        <div>
          <strong>{r.salaNome}</strong>
          <div className="text-sm text-muted">{r.salaAndar}</div>
        </div>
      ) },
    { chave: 'solicitanteNome', titulo: 'Solicitante' },
    { chave: 'observacao', titulo: 'Justificativa', ordenavel: false,
      render: (r) => <span className="text-muted text-md">{r.observacao || '—'}</span> },
    { chave: 'acoes', titulo: '', ordenavel: false, alinhar: 'right',
      render: (r) => (
        <>
          <button className="btn btn-danger btn-sm" onClick={() => setRecusando(r)}>Recusar</button>
          <button className="btn btn-sm" onClick={() => aprovar(r)}>Aprovar</button>
        </>
      ) },
  ];

  return (
    <>
      <PageHeader titulo="Moderação de reservas"
        descricao="Solicitações de última hora aguardando decisão. Reservas da grade bimestral são confirmadas automaticamente quando não há conflito." />

      {erro && <div className="mb-4"><Notice tom="danger">{erro}</Notice></div>}

      {carregando
        ? <div className="panel"><div className="skeleton" style={{ height: 180 }} /></div>
        : (
          <DataTable colunas={colunas} dados={fila} porPagina={15}
            buscaPlaceholder="Buscar por solicitante, ambiente…"
            ordemInicial={{ chave: 'data', dir: 'asc' }}
            vazio={<EmptyState icone="✓" titulo="Fila vazia"
              descricao="Nenhuma solicitação aguardando aprovação no momento." />} />
        )}

      {recusando && (
        <Modal titulo="Recusar solicitação" tamanho="sm" onClose={() => setRecusando(null)}
          subtitulo={`${recusando.salaNome} · ${dataBr(recusando.data)} · ${hhmm(recusando.horaInicio)}–${hhmm(recusando.horaFim)}`}
          rodape={(
            <>
              <button className="btn btn-secondary" onClick={() => setRecusando(null)}>Voltar</button>
              <button className="btn btn-danger-solid" onClick={recusar}>Recusar solicitação</button>
            </>
          )}>
          <p className="text-md">
            {recusando.solicitanteNome} será notificado da recusa.
          </p>
          <Field label="Motivo (opcional)" hint="Aparece no aviso enviado ao solicitante.">
            <textarea className="textarea" value={motivo} onChange={(e) => setMotivo(e.target.value)}
              placeholder="Ex: ambiente reservado para manutenção nesta data" />
          </Field>
        </Modal>
      )}
    </>
  );
}
