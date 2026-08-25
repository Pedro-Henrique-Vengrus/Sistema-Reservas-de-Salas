import { useCallback, useEffect, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { api } from '../../api/client';
import DataTable from '../../components/ui/DataTable';
import { useToast } from '../../components/ui/ToastProvider';
import {
  EmptyState, Field, Modal, Notice, PageHeader, Segmented, StatusBadge,
} from '../../components/ui/primitives';
import { dataBr, diaDaSemana, hhmm } from '../../lib/format';

/**
 * Fila de decisao do gestor, em duas frentes:
 * reservas de ultima hora e trocas fora do mesmo dia/turno.
 */
export default function Moderacao() {
  const toast = useToast();
  const { recarregarContadores } = useOutletContext() || {};
  const [aba, setAba] = useState('reservas');
  const [reservas, setReservas] = useState([]);
  const [trocas, setTrocas] = useState([]);
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(true);
  const [recusando, setRecusando] = useState(null); // { tipo: 'reserva'|'troca', item }
  const [motivo, setMotivo] = useState('');

  const carregar = useCallback(async () => {
    setCarregando(true);
    try {
      const [r, t] = await Promise.all([
        api.get('/reservas/moderacao'),
        api.get('/propostas/moderacao'),
      ]);
      setReservas(r);
      setTrocas(t);
      recarregarContadores?.();
    } catch (e) { setErro(e.message); } finally { setCarregando(false); }
  }, [recarregarContadores]);

  useEffect(() => { carregar(); }, [carregar]);

  async function decidir(rota, mensagem, corpo) {
    try {
      await api.post(rota, corpo);
      toast.sucesso(mensagem);
      setRecusando(null);
      setMotivo('');
      carregar();
    } catch (e) { toast.erro(e.message); }
  }

  const colunasReservas = [
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
          <button className="btn btn-danger btn-sm"
            onClick={() => setRecusando({ tipo: 'reserva', item: r })}>Recusar</button>
          <button className="btn btn-sm"
            onClick={() => decidir(`/reservas/${r.id}/aprovar`,
              `Reserva de ${r.solicitanteNome} aprovada — o solicitante foi notificado.`)}>
            Aprovar
          </button>
        </>
      ) },
  ];

  const colunasTrocas = [
    { chave: 'origemData', titulo: 'Quem pede', largura: 190,
      valor: (p) => p.solicitanteNome,
      render: (p) => (
        <div>
          <strong>{p.solicitanteNome}</strong>
          <div className="text-sm text-muted">oferece {p.salaOferecida}</div>
          <div className="text-sm text-muted nowrap">
            {dataBr(p.oferecidaData)} · {hhmm(p.oferecidaInicio)}–{hhmm(p.oferecidaFim)}
          </div>
        </div>
      ) },
    { chave: 'donoNome', titulo: 'Quem cede', largura: 190,
      render: (p) => (
        <div>
          <strong>{p.donoNome}</strong>
          <div className="text-sm text-muted">cede {p.salaDesejada}</div>
          <div className="text-sm text-muted nowrap">
            {dataBr(p.origemData)} · {hhmm(p.origemInicio)}–{hhmm(p.origemFim)}
          </div>
        </div>
      ) },
    { chave: 'turnos', titulo: 'Por que precisa de aval', ordenavel: false,
      valor: (p) => `${p.origemTurno} ${p.oferecidaTurno}`,
      render: (p) => (
        <div className="text-sm">
          {p.origemData !== p.oferecidaData && <div>Dias diferentes</div>}
          {p.origemTurno !== p.oferecidaTurno && (
            <div className="row gap-1" style={{ marginTop: 2 }}>
              <StatusBadge valor={p.oferecidaTurno} /> <span className="text-muted">→</span> <StatusBadge valor={p.origemTurno} />
            </div>
          )}
        </div>
      ) },
    { chave: 'justificativa', titulo: 'Justificativa', ordenavel: false,
      render: (p) => <span className="text-muted text-md">{p.justificativa}</span> },
    { chave: 'acoes', titulo: '', ordenavel: false, alinhar: 'right',
      render: (p) => (
        <>
          <button className="btn btn-danger btn-sm"
            onClick={() => setRecusando({ tipo: 'troca', item: p })}>Recusar</button>
          <button className="btn btn-sm"
            onClick={() => decidir(`/propostas/${p.id}/gestor/aprovar`,
              'Troca aprovada — as reservas foram invertidas e ambos notificados.')}>
            Aprovar
          </button>
        </>
      ) },
  ];

  const ehTroca = recusando?.tipo === 'troca';

  return (
    <>
      <PageHeader titulo="Moderação"
        descricao="Solicitações de última hora e trocas fora do mesmo dia/turno — as duas decisões que dependem do gestor. Reservas da grade e trocas de rotina se resolvem sozinhas." />

      {erro && <div className="mb-4"><Notice tom="danger">{erro}</Notice></div>}

      <div className="mb-4">
        <Segmented valor={aba} onChange={setAba} opcoes={[
          { valor: 'reservas', rotulo: `Reservas${reservas.length ? ` (${reservas.length})` : ''}` },
          { valor: 'trocas', rotulo: `Trocas${trocas.length ? ` (${trocas.length})` : ''}` },
        ]} />
      </div>

      {carregando && <div className="panel"><div className="skeleton" style={{ height: 180 }} /></div>}

      {!carregando && aba === 'reservas' && (
        <DataTable colunas={colunasReservas} dados={reservas} porPagina={15}
          buscaPlaceholder="Buscar por solicitante, ambiente…"
          ordemInicial={{ chave: 'data', dir: 'asc' }}
          vazio={<EmptyState icone="✓" titulo="Nenhuma reserva na fila"
            descricao="Solicitações de última hora aguardando decisão aparecem aqui." />} />
      )}

      {!carregando && aba === 'trocas' && (
        <DataTable colunas={colunasTrocas} dados={trocas} porPagina={15}
          buscaPlaceholder="Buscar por professor, ambiente…"
          vazio={<EmptyState icone="⇄" titulo="Nenhuma troca aguardando aval"
            descricao="Trocas no mesmo dia e turno se resolvem entre os professores. Só as fora desse padrão chegam aqui, depois do aceite do professor." />} />
      )}

      {recusando && (
        <Modal titulo={ehTroca ? 'Recusar troca' : 'Recusar solicitação'} tamanho="sm"
          onClose={() => { setRecusando(null); setMotivo(''); }}
          subtitulo={ehTroca
            ? `${recusando.item.solicitanteNome} ⇄ ${recusando.item.donoNome}`
            : `${recusando.item.salaNome} · ${dataBr(recusando.item.data)} · ${hhmm(recusando.item.horaInicio)}–${hhmm(recusando.item.horaFim)}`}
          rodape={(
            <>
              <button className="btn btn-secondary" onClick={() => { setRecusando(null); setMotivo(''); }}>Voltar</button>
              <button className="btn btn-danger-solid"
                onClick={() => (ehTroca
                  ? decidir(`/propostas/${recusando.item.id}/gestor/recusar`, 'Troca recusada — ambos foram notificados.', { motivo })
                  : decidir(`/reservas/${recusando.item.id}/recusar`, 'Solicitação recusada — o solicitante foi notificado.', { motivo }))}>
                {ehTroca ? 'Recusar troca' : 'Recusar solicitação'}
              </button>
            </>
          )}>
          <p className="text-md">
            {ehTroca
              ? 'Os dois professores serão notificados e as reservas permanecem como estão.'
              : `${recusando.item.solicitanteNome} será notificado da recusa.`}
          </p>
          <Field label="Motivo (opcional)" hint="Aparece no aviso enviado aos envolvidos.">
            <textarea className="textarea" value={motivo} onChange={(e) => setMotivo(e.target.value)}
              placeholder={ehTroca
                ? 'Ex: troca entre turnos prejudica a grade do curso'
                : 'Ex: ambiente reservado para manutenção nesta data'} />
          </Field>
        </Modal>
      )}
    </>
  );
}
