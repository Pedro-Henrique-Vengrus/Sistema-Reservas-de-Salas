import { useCallback, useEffect, useState } from 'react';
import { Link, useOutletContext } from 'react-router-dom';
import { api } from '../api/client';
import DataTable from '../components/ui/DataTable';
import { useToast } from '../components/ui/ToastProvider';
import {
  EmptyState, Modal, Notice, PageHeader, Segmented, StatusBadge,
} from '../components/ui/primitives';
import { dataBr, diaDaSemana, hhmm } from '../lib/format';

/** Propostas de troca recebidas e enviadas, com o detalhe lado a lado das duas reservas. */
export default function Trocas() {
  const toast = useToast();
  const { recarregarContadores } = useOutletContext() || {};
  const [aba, setAba] = useState('recebidas');
  const [recebidas, setRecebidas] = useState([]);
  const [enviadas, setEnviadas] = useState([]);
  const [detalhe, setDetalhe] = useState(null);
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(true);

  const carregar = useCallback(async () => {
    setCarregando(true);
    try {
      const [r, e] = await Promise.all([
        api.get('/propostas/recebidas'),
        api.get('/propostas/enviadas'),
      ]);
      setRecebidas(r);
      setEnviadas(e);
      recarregarContadores?.();
    } catch (ex) { setErro(ex.message); } finally { setCarregando(false); }
  }, [recarregarContadores]);

  useEffect(() => { carregar(); }, [carregar]);

  async function agir(proposta, acao) {
    try {
      await api.post(`/propostas/${proposta.id}/${acao}`);
      toast.sucesso({
        aceitar: 'Troca confirmada — as reservas foram invertidas.',
        recusar: 'Proposta recusada.',
        cancelar: 'Proposta cancelada.',
      }[acao]);
      setDetalhe(null);
      carregar();
    } catch (e) { toast.erro(e.message); }
  }

  const lista = aba === 'recebidas' ? recebidas : enviadas;
  const pendentesRecebidas = recebidas.filter((p) => p.status === 'PENDENTE').length;

  const colunas = [
    { chave: 'data', titulo: 'Dia / turno', largura: 170,
      render: (p) => (
        <div>
          <span className="nowrap">{dataBr(p.data)} <span className="text-muted text-sm">{diaDaSemana(p.data, true)}</span></span>
          <div className="mt-2"><StatusBadge valor={p.turno} /></div>
        </div>
      ) },
    { chave: 'contraparte', titulo: aba === 'recebidas' ? 'Proposta por' : 'Enviada para',
      valor: (p) => (aba === 'recebidas' ? p.solicitanteNome : p.donoNome),
      render: (p) => <strong>{aba === 'recebidas' ? p.solicitanteNome : p.donoNome}</strong> },
    { chave: 'salaDesejada', titulo: aba === 'recebidas' ? 'Quer a sua sala' : 'Você quer',
      render: (p) => (
        <div>
          <strong>{p.salaDesejada}</strong>
          <div className="text-sm text-muted">{hhmm(p.origemInicio)}–{hhmm(p.origemFim)}</div>
        </div>
      ) },
    { chave: 'salaOferecida', titulo: aba === 'recebidas' ? 'Oferece' : 'Você oferece',
      render: (p) => (
        <div>
          <strong>{p.salaOferecida || '—'}</strong>
          <div className="text-sm text-muted">
            {p.oferecidaInicio ? `${hhmm(p.oferecidaInicio)}–${hhmm(p.oferecidaFim)}` : ''}
          </div>
        </div>
      ) },
    { chave: 'status', titulo: 'Status', render: (p) => <StatusBadge valor={p.status} /> },
    { chave: 'acoes', titulo: '', ordenavel: false, alinhar: 'right',
      render: (p) => (
        <>
          <button className="btn btn-secondary btn-sm" onClick={() => setDetalhe(p)}>Detalhes</button>
          {aba === 'recebidas' && p.status === 'PENDENTE' && (
            <button className="btn btn-sm" onClick={() => agir(p, 'aceitar')}>Aceitar</button>
          )}
          {aba === 'enviadas' && p.status === 'PENDENTE' && (
            <button className="btn btn-danger btn-sm" onClick={() => agir(p, 'cancelar')}>Cancelar</button>
          )}
        </>
      ) },
  ];

  return (
    <>
      <PageHeader titulo="Trocas de sala"
        descricao="A troca é mútua: cada professor assume a reserva do outro. Só é possível entre reservas aprovadas no mesmo dia e no mesmo turno."
        acoes={<Link className="btn btn-secondary" to="/agenda">Procurar na agenda</Link>} />

      {erro && <div className="mb-4"><Notice tom="danger">{erro}</Notice></div>}

      <div className="mb-4">
        <Segmented valor={aba} onChange={setAba} opcoes={[
          { valor: 'recebidas', rotulo: `Recebidas${pendentesRecebidas ? ` (${pendentesRecebidas})` : ''}` },
          { valor: 'enviadas', rotulo: `Enviadas (${enviadas.length})` },
        ]} />
      </div>

      {carregando
        ? <div className="panel"><div className="skeleton" style={{ height: 180 }} /></div>
        : (
          <DataTable colunas={colunas} dados={lista} buscaPlaceholder="Buscar por professor, ambiente…"
            vazio={<EmptyState icone="⇄"
              titulo={aba === 'recebidas' ? 'Nenhuma proposta recebida' : 'Nenhuma proposta enviada'}
              descricao={aba === 'recebidas'
                ? 'Quando outro professor quiser trocar de sala com você, a proposta aparece aqui.'
                : 'Na agenda, clique em um horário ocupado para propor uma troca.'} />} />
        )}

      {detalhe && (
        <Modal titulo="Proposta de troca" tamanho="lg" onClose={() => setDetalhe(null)}
          subtitulo={`${dataBr(detalhe.data)} · turno ${detalhe.turno.toLowerCase()}`}
          rodape={(
            <>
              <button className="btn btn-secondary" onClick={() => setDetalhe(null)}>Fechar</button>
              {aba === 'recebidas' && detalhe.status === 'PENDENTE' && (
                <>
                  <button className="btn btn-danger" onClick={() => agir(detalhe, 'recusar')}>Recusar</button>
                  <button className="btn" onClick={() => agir(detalhe, 'aceitar')}>Aceitar troca</button>
                </>
              )}
              {aba === 'enviadas' && detalhe.status === 'PENDENTE' && (
                <button className="btn btn-danger" onClick={() => agir(detalhe, 'cancelar')}>Cancelar proposta</button>
              )}
            </>
          )}>
          <div className="split">
            <div className="panel">
              <h4 className="mb-4">Reserva desejada</h4>
              <dl className="dl">
                <dt>Ambiente</dt><dd>{detalhe.salaDesejada}</dd>
                <dt>Horário</dt><dd>{hhmm(detalhe.origemInicio)}–{hhmm(detalhe.origemFim)}</dd>
                <dt>Hoje é de</dt><dd>{detalhe.donoNome}</dd>
              </dl>
            </div>
            <div className="panel">
              <h4 className="mb-4">Reserva oferecida</h4>
              <dl className="dl">
                <dt>Ambiente</dt><dd>{detalhe.salaOferecida || '—'}</dd>
                <dt>Horário</dt>
                <dd>{detalhe.oferecidaInicio ? `${hhmm(detalhe.oferecidaInicio)}–${hhmm(detalhe.oferecidaFim)}` : '—'}</dd>
                <dt>Hoje é de</dt><dd>{detalhe.solicitanteNome}</dd>
              </dl>
            </div>
          </div>

          <div>
            <h4 className="mb-4">Justificativa</h4>
            <Notice tom="info">{detalhe.justificativa}</Notice>
          </div>

          <div className="row gap-2">
            <span className="text-muted text-md">Status atual:</span>
            <StatusBadge valor={detalhe.status} />
          </div>
        </Modal>
      )}
    </>
  );
}
