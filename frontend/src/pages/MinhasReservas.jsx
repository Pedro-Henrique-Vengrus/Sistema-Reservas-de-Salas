import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import DataTable from '../components/ui/DataTable';
import { useToast } from '../components/ui/ToastProvider';
import {
  ConfirmDialog, EmptyState, Notice, PageHeader, Segmented, StatusBadge,
} from '../components/ui/primitives';
import { dataBr, diaDaSemana, hhmm, reservaPassada } from '../lib/format';

export default function MinhasReservas() {
  const toast = useToast();
  const [reservas, setReservas] = useState([]);
  const [aba, setAba] = useState('futuras');
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(true);
  const [confirmar, setConfirmar] = useState(null);
  const [ocupado, setOcupado] = useState(false);

  const carregar = useCallback(() => {
    setCarregando(true);
    api.get('/reservas/minhas?incluirHistorico=true')
      .then(setReservas)
      .catch((e) => setErro(e.message))
      .finally(() => setCarregando(false));
  }, []);

  useEffect(() => { carregar(); }, [carregar]);

  async function cancelar(reserva) {
    setOcupado(true);
    try {
      await api.del(`/reservas/${reserva.id}`);
      toast.sucesso('Reserva cancelada.');
      setConfirmar(null);
      carregar();
    } catch (e) {
      toast.erro(e.message);
    } finally {
      setOcupado(false);
    }
  }

  const futuras = reservas.filter((r) => !reservaPassada(r) && r.status !== 'CANCELADA' && r.status !== 'RECUSADA');
  const historico = reservas.filter((r) => reservaPassada(r) || r.status === 'CANCELADA' || r.status === 'RECUSADA');
  const lista = aba === 'futuras' ? futuras : historico;

  const colunas = [
    { chave: 'data', titulo: 'Data', largura: 150,
      render: (r) => (
        <span className="nowrap">
          {dataBr(r.data)} <span className="text-muted text-sm">{diaDaSemana(r.data, true)}</span>
        </span>
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
    { chave: 'tipoReserva', titulo: 'Modo', render: (r) => <StatusBadge valor={r.tipoReserva} /> },
    { chave: 'status', titulo: 'Status', render: (r) => <StatusBadge valor={r.status} /> },
    { chave: 'acoes', titulo: '', ordenavel: false, alinhar: 'right',
      render: (r) => (!reservaPassada(r) && (r.status === 'APROVADA' || r.status === 'PENDENTE_APROVACAO') ? (
        <button className="btn btn-danger btn-sm" onClick={() => setConfirmar(r)}>Cancelar</button>
      ) : null) },
  ];

  return (
    <>
      <PageHeader titulo="Minhas reservas"
        descricao="Suas solicitações confirmadas, pendentes de moderação e o histórico do período."
        acoes={<Link className="btn" to="/agenda">+ Nova reserva</Link>} />

      {erro && <div className="mb-4"><Notice tom="danger">{erro}</Notice></div>}

      <div className="mb-4">
        <Segmented valor={aba} onChange={setAba} opcoes={[
          { valor: 'futuras', rotulo: `Ativas (${futuras.length})` },
          { valor: 'historico', rotulo: `Histórico (${historico.length})` },
        ]} />
      </div>

      {carregando
        ? <div className="panel"><div className="skeleton" style={{ height: 180 }} /></div>
        : (
          <DataTable colunas={colunas} dados={lista}
            buscaPlaceholder="Buscar por ambiente, status…"
            ordemInicial={{ chave: 'data', dir: aba === 'futuras' ? 'asc' : 'desc' }}
            classeLinha={(r) => (reservaPassada(r) ? 'dim' : '')}
            vazio={<EmptyState icone="🔖"
              titulo={aba === 'futuras' ? 'Nenhuma reserva ativa' : 'Histórico vazio'}
              descricao={aba === 'futuras'
                ? 'Abra a agenda para reservar um ambiente do seu curso.'
                : 'Reservas passadas, canceladas e recusadas aparecem aqui.'}
              acao={aba === 'futuras' && <Link className="btn" to="/agenda">Ir para a agenda</Link>} />} />
        )}

      {confirmar && (
        <ConfirmDialog
          titulo="Cancelar reserva"
          mensagem={`Cancelar a reserva de ${confirmar.salaNome} em ${dataBr(confirmar.data)}, das ${hhmm(confirmar.horaInicio)} às ${hhmm(confirmar.horaFim)}? Propostas de troca pendentes envolvendo esta reserva também serão canceladas.`}
          confirmarTexto="Cancelar reserva"
          ocupado={ocupado}
          onConfirm={() => cancelar(confirmar)}
          onClose={() => setConfirmar(null)}
        />
      )}
    </>
  );
}
