import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, qs } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { useToast } from '../components/ui/ToastProvider';
import { Drawer, Field, Notice, PageHeader, StatusBadge, EmptyState } from '../components/ui/primitives';
import CampoData from '../components/ui/CampoData';
import {
  dataBr, diaDaSemana, hhmm, hojeIso, inicioDaSemana, reservaPassada, somaDias, TIPOS_RESERVA,
} from '../lib/format';

const HORAS = ['07:00', '08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00',
  '15:00', '16:00', '17:00', '18:00', '19:00', '20:00', '21:00', '22:00'];

const minutos = (hhmmss) => {
  const [h, m] = String(hhmmss).slice(0, 5).split(':').map(Number);
  return h * 60 + m;
};

/**
 * Agenda desktop: filtros a esquerda e matriz ambientes x horarios a direita.
 * Celula livre abre o formulario de reserva; celula ocupada abre o detalhe,
 * com proposta de troca — direta no mesmo dia/turno, ou via gestor fora disso.
 */
export default function Agenda() {
  const { user, semCurso } = useAuth();
  const toast = useToast();

  const [dia, setDia] = useState(hojeIso());
  const [filtros, setFiltros] = useState({ termo: '', tipo: '', cursoId: '', capacidadeMinima: '' });
  const [tipos, setTipos] = useState([]);
  const [salas, setSalas] = useState([]);
  const [reservas, setReservas] = useState([]);
  const [grade, setGrade] = useState(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [novaReserva, setNovaReserva] = useState(null);  // { sala, hora }
  const [detalhe, setDetalhe] = useState(null);          // reserva ocupada

  const semana = useMemo(() => {
    const seg = inicioDaSemana(dia);
    return Array.from({ length: 7 }, (_, i) => somaDias(seg, i));
  }, [dia]);

  useEffect(() => {
    api.get('/salas/tipos').then(setTipos).catch(() => {});
    api.get('/periodo-grade').then(setGrade).catch(() => {});
  }, []);

  const carregar = useCallback(async () => {
    setCarregando(true);
    setErro('');
    try {
      const lista = await api.get(`/salas${qs({
        termo: filtros.termo || undefined,
        tipo: filtros.tipo || undefined,
        cursoId: filtros.cursoId || undefined,
        capacidadeMinima: filtros.capacidadeMinima || undefined,
        status: 'ATIVO',
      })}`);
      setSalas(lista);
      if (lista.length === 0) { setReservas([]); return; }
      const agenda = await api.get(`/reservas/agenda${qs({
        salaIds: lista.map((s) => s.id), inicio: dia, fim: dia,
      })}`);
      setReservas(agenda);
    } catch (e) {
      setErro(e.message);
    } finally {
      setCarregando(false);
    }
  }, [dia, filtros]);

  useEffect(() => { carregar(); }, [carregar]);

  /** Reserva ativa que cobre a hora cheia daquele ambiente. */
  function ocupacao(salaId, hora) {
    const ini = minutos(hora);
    const fim = ini + 60;
    return reservas.find((r) => r.salaId === salaId
      && minutos(r.horaInicio) < fim && minutos(r.horaFim) > ini);
  }

  const cursosDoUsuario = user?.cursos || [];

  return (
    <>
      <PageHeader
        titulo="Agenda de ambientes"
        descricao="Selecione um dia e clique em um horário livre para reservar. Horários ocupados abrem o detalhe e a proposta de troca."
        acoes={(
          <div className="row gap-2">
            <button className="btn btn-secondary btn-sm" onClick={() => setDia(hojeIso())}>Hoje</button>
            <button className="btn btn-secondary btn-icon btn-sm" title="Semana anterior"
              onClick={() => setDia(somaDias(dia, -7))}>←</button>
            <button className="btn btn-secondary btn-icon btn-sm" title="Próxima semana"
              onClick={() => setDia(somaDias(dia, 7))}>→</button>
          </div>
        )}
      />

      {semCurso && (
        <div className="mb-4">
          <Notice tom="warn">
            Você não está vinculado a nenhum curso, então nenhum ambiente é visível para o seu perfil.
          </Notice>
        </div>
      )}

      {grade && !grade.aberto && (
        <div className="mb-4">
          <Notice tom="warn">
            O preenchimento da <strong>grade bimestral</strong> está fechado. Novas solicitações só podem
            ser feitas no modo <strong>última hora</strong>, que passa pela moderação.
          </Notice>
        </div>
      )}

      {erro && <div className="mb-4"><Notice tom="danger">{erro}</Notice></div>}

      <div className="agenda-layout">
        <aside className="agenda-side">
          <section className="panel">
            <h3 className="mb-4">Dia</h3>
            <div className="col gap-2">
              {semana.map((d) => (
                <button key={d}
                  className={`btn btn-sm ${d === dia ? '' : 'btn-secondary'}`}
                  style={{ justifyContent: 'space-between' }}
                  onClick={() => setDia(d)}>
                  <span>{diaDaSemana(d, true)}</span>
                  <span className="text-mono">{dataBr(d).slice(0, 5)}</span>
                </button>
              ))}
            </div>
            <div className="mt-4">
              <Field label="Ir para a data">
                <CampoData value={dia} onChange={(iso) => iso && setDia(iso)} />
              </Field>
            </div>
          </section>

          <section className="panel">
            <h3 className="mb-4">Filtros</h3>
            <div className="col gap-4">
              <Field label="Buscar ambiente">
                <input className="input" placeholder="Nome ou código" value={filtros.termo}
                  onChange={(e) => setFiltros({ ...filtros, termo: e.target.value })} />
              </Field>
              <Field label="Tipo de ambiente">
                <select className="select" value={filtros.tipo}
                  onChange={(e) => setFiltros({ ...filtros, tipo: e.target.value })}>
                  <option value="">Todos</option>
                  {tipos.map((t) => <option key={t.valor} value={t.valor}>{t.rotulo}</option>)}
                </select>
              </Field>
              {cursosDoUsuario.length > 1 && (
                <Field label="Curso">
                  <select className="select" value={filtros.cursoId}
                    onChange={(e) => setFiltros({ ...filtros, cursoId: e.target.value })}>
                    <option value="">Todos os meus cursos</option>
                    {cursosDoUsuario.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
                  </select>
                </Field>
              )}
              <Field label="Capacidade mínima">
                <input className="input" type="number" min="0" placeholder="Ex: 40"
                  value={filtros.capacidadeMinima}
                  onChange={(e) => setFiltros({ ...filtros, capacidadeMinima: e.target.value })} />
              </Field>
            </div>
          </section>

          <section className="panel">
            <h3 className="mb-4">Legenda</h3>
            <div className="col gap-2 legend" style={{ flexDirection: 'column', alignItems: 'flex-start' }}>
              <span><i className="free" /> Livre</span>
              <span><i className="mine" /> Sua reserva</span>
              <span><i className="taken" /> Ocupado (troca possível)</span>
              <span><i className="pending" /> Aguardando moderação</span>
            </div>
          </section>
        </aside>

        <section className="card">
          <div className="card-head">
            <div>
              <h3>{diaDaSemana(dia)}, {dataBr(dia)}</h3>
              <p className="text-sm text-muted">{salas.length} ambiente(s) visível(is) para o seu perfil</p>
            </div>
          </div>

          {carregando && <div className="card-body"><div className="skeleton" style={{ height: 220 }} /></div>}

          {!carregando && salas.length === 0 && (
            <EmptyState icone="🏛" titulo="Nenhum ambiente visível"
              descricao="Ajuste os filtros ou verifique com a administração os cursos vinculados ao seu perfil." />
          )}

          {!carregando && salas.length > 0 && (
            <div className="grid-scroll">
              <table className="agenda">
                <thead>
                  <tr>
                    <th className="room">Ambiente</th>
                    {HORAS.map((h) => <th key={h}>{h}</th>)}
                  </tr>
                </thead>
                <tbody>
                  {salas.map((s) => (
                    <tr key={s.id}>
                      <th className="room">
                        {s.nome}
                        <span className="sub">{s.tipoRotulo} · {s.capacidade} lugares</span>
                      </th>
                      {HORAS.map((h) => {
                        const r = ocupacao(s.id, h);
                        const minha = r && r.solicitanteId === user?.id;
                        const classe = !r ? 'free'
                          : minha ? 'mine'
                          : r.status === 'PENDENTE_APROVACAO' ? 'pending' : 'taken';
                        return (
                          <td className="slot" key={h}>
                            <button className={`slot-btn ${classe}`}
                              title={r ? `${r.salaNome} · ${r.solicitanteNome} · ${hhmm(r.horaInicio)}–${hhmm(r.horaFim)}` : `Reservar ${s.nome} às ${h}`}
                              onClick={() => (r ? setDetalhe(r) : setNovaReserva({ sala: s, hora: h }))}>
                              {r ? <span className="who">{minha ? 'Você' : r.solicitanteNome}</span> : '+'}
                            </button>
                          </td>
                        );
                      })}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>

      {novaReserva && (
        <DrawerNovaReserva
          sala={novaReserva.sala}
          data={dia}
          horaInicial={novaReserva.hora}
          gradeAberta={!!grade?.aberto}
          onFechar={() => setNovaReserva(null)}
          onCriada={(r) => {
            setNovaReserva(null);
            toast.sucesso(r.status === 'APROVADA'
              ? 'Reserva confirmada.'
              : 'Solicitação enviada para moderação.');
            carregar();
          }}
        />
      )}

      {detalhe && (
        <DrawerDetalhe
          reserva={detalhe}
          onFechar={() => setDetalhe(null)}
          onTrocaEnviada={() => { setDetalhe(null); toast.sucesso('Proposta de troca enviada.'); }}
          onCancelada={() => { setDetalhe(null); toast.sucesso('Reserva cancelada.'); carregar(); }}
        />
      )}
    </>
  );
}

/* ------------------------------------------------------------- Nova reserva */
function DrawerNovaReserva({ sala, data, horaInicial, gradeAberta, onFechar, onCriada }) {
  const [form, setForm] = useState({
    horaInicio: horaInicial,
    horaFim: `${String(Number(horaInicial.slice(0, 2)) + 1).padStart(2, '0')}:00`,
    tipoReserva: gradeAberta ? 'GRADE_BIMESTRAL' : 'ULTIMA_HORA',
    observacao: '',
  });
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);

  async function salvar() {
    setErro('');
    if (form.horaFim <= form.horaInicio) { setErro('O término deve ser depois do início.'); return; }
    setEnviando(true);
    try {
      const criada = await api.post('/reservas', {
        salaId: sala.id,
        data,
        horaInicio: `${form.horaInicio}:00`,
        horaFim: `${form.horaFim}:00`,
        tipoReserva: form.tipoReserva,
        observacao: form.observacao || null,
      });
      onCriada(criada);
    } catch (e) {
      setErro(e.message);
      setEnviando(false);
    }
  }

  return (
    <Drawer titulo="Nova reserva" subtitulo={`${sala.nome} · ${dataBr(data)}`} onClose={onFechar}
      rodape={(
        <>
          <button className="btn btn-secondary" onClick={onFechar}>Cancelar</button>
          <button className="btn" onClick={salvar} disabled={enviando}>
            {enviando ? 'Enviando…' : 'Reservar'}
          </button>
        </>
      )}>
      <dl className="dl">
        <dt>Ambiente</dt><dd>{sala.nome} {sala.codigo && <span className="text-muted">({sala.codigo})</span>}</dd>
        <dt>Tipo</dt><dd>{sala.tipoRotulo}</dd>
        <dt>Capacidade</dt><dd>{sala.capacidade} lugares</dd>
        <dt>Cursos</dt><dd>{sala.cursos.map((c) => <span className="tag" key={c.id}>{c.sigla || c.nome}</span>)}</dd>
      </dl>

      <div className="split">
        <Field label="Início">
          <input className="input" type="time" value={form.horaInicio}
            onChange={(e) => setForm({ ...form, horaInicio: e.target.value })} />
        </Field>
        <Field label="Término">
          <input className="input" type="time" value={form.horaFim}
            onChange={(e) => setForm({ ...form, horaFim: e.target.value })} />
        </Field>
      </div>

      <Field label="Modo da reserva"
        hint={gradeAberta
          ? 'A grade bimestral confirma direto quando não há conflito; última hora passa pela moderação.'
          : 'O período da grade está fechado — apenas solicitações de última hora são aceitas.'}>
        <select className="select" value={form.tipoReserva}
          onChange={(e) => setForm({ ...form, tipoReserva: e.target.value })}>
          {TIPOS_RESERVA.map((t) => (
            <option key={t.valor} value={t.valor} disabled={t.valor === 'GRADE_BIMESTRAL' && !gradeAberta}>
              {t.rotulo}{t.valor === 'GRADE_BIMESTRAL' && !gradeAberta ? ' (período fechado)' : ''}
            </option>
          ))}
        </select>
      </Field>

      <Field label="Observação" hint="Opcional — ajuda a administração a avaliar solicitações de última hora.">
        <textarea className="textarea" maxLength={300} value={form.observacao}
          placeholder="Ex: palestra extra da semana acadêmica"
          onChange={(e) => setForm({ ...form, observacao: e.target.value })} />
      </Field>

      {erro && <Notice tom="danger">{erro}</Notice>}
    </Drawer>
  );
}

/* ---------------------------------------------------- Detalhe / proposta de troca */
function DrawerDetalhe({ reserva, onFechar, onTrocaEnviada, onCancelada }) {
  const { user } = useAuth();
  const minha = reserva.solicitanteId === user?.id;

  const [elegiveis, setElegiveis] = useState(null);
  const [oferecidaId, setOferecidaId] = useState('');
  const [justificativa, setJustificativa] = useState('');
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    if (minha || reserva.status !== 'APROVADA') { setElegiveis([]); return; }
    // Qualquer reserva minha aprovada e futura serve. Dia e turno nao restringem mais
    // a troca — apenas definem se ela se resolve entre professores ou passa pelo gestor.
    api.get('/reservas/minhas')
      .then((lista) => setElegiveis(lista.filter((r) => r.status === 'APROVADA' && !reservaPassada(r))))
      .catch((e) => { setErro(e.message); setElegiveis([]); });
  }, [reserva, minha]);

  const oferecida = elegiveis?.find((r) => String(r.id) === String(oferecidaId));
  const passaPeloGestor = !!oferecida
    && (oferecida.data !== reserva.data || oferecida.turno !== reserva.turno);

  async function propor() {
    setErro('');
    if (!oferecidaId) { setErro('Selecione qual reserva sua será oferecida na troca.'); return; }
    if (!justificativa.trim()) { setErro('A justificativa é obrigatória.'); return; }
    setEnviando(true);
    try {
      await api.post('/propostas', {
        reservaOrigemId: reserva.id,
        reservaOferecidaId: Number(oferecidaId),
        justificativa,
      });
      onTrocaEnviada();
    } catch (e) { setErro(e.message); setEnviando(false); }
  }

  async function cancelar() {
    setEnviando(true);
    try {
      await api.del(`/reservas/${reserva.id}`);
      onCancelada();
    } catch (e) { setErro(e.message); setEnviando(false); }
  }

  const podeTrocar = !minha && reserva.status === 'APROVADA' && (elegiveis?.length ?? 0) > 0;

  return (
    <Drawer titulo={minha ? 'Sua reserva' : 'Horário ocupado'}
      subtitulo={`${reserva.salaNome} · ${dataBr(reserva.data)}`} onClose={onFechar}
      rodape={(
        <>
          <button className="btn btn-secondary" onClick={onFechar}>Fechar</button>
          {minha && reserva.status !== 'CANCELADA' && (
            <button className="btn btn-danger-solid" onClick={cancelar} disabled={enviando}>
              Cancelar reserva
            </button>
          )}
          {podeTrocar && (
            <button className="btn" onClick={propor} disabled={enviando}>
              {enviando ? 'Enviando…' : '⇄ Propor troca'}
            </button>
          )}
        </>
      )}>
      <dl className="dl">
        <dt>Solicitante</dt><dd>{minha ? 'Você' : reserva.solicitanteNome}</dd>
        <dt>Horário</dt><dd>{hhmm(reserva.horaInicio)} – {hhmm(reserva.horaFim)}</dd>
        <dt>Turno</dt><dd><StatusBadge valor={reserva.turno} /></dd>
        <dt>Modo</dt><dd><StatusBadge valor={reserva.tipoReserva} /></dd>
        <dt>Status</dt><dd><StatusBadge valor={reserva.status} /></dd>
        {reserva.observacao && <><dt>Observação</dt><dd>{reserva.observacao}</dd></>}
      </dl>

      {!minha && reserva.status === 'PENDENTE_APROVACAO' && (
        <Notice tom="warn">
          Esta solicitação ainda aguarda moderação. A troca só é possível entre reservas já aprovadas.
        </Notice>
      )}

      {!minha && reserva.status === 'APROVADA' && elegiveis !== null && elegiveis.length === 0 && (
        <Notice tom="info">
          Para propor uma troca você precisa ter ao menos uma reserva aprovada e futura para oferecer.
        </Notice>
      )}

      {podeTrocar && (
        <>
          <Field label="Sua reserva oferecida em troca"
            hint="Mesmo dia e turno resolve direto com o professor; fora disso passa pelo gestor.">
            <select className="select" value={oferecidaId} onChange={(e) => setOferecidaId(e.target.value)}>
              <option value="">Selecione…</option>
              {elegiveis.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.salaNome} · {dataBr(r.data)} · {hhmm(r.horaInicio)}–{hhmm(r.horaFim)}
                  {r.data === reserva.data && r.turno === reserva.turno ? '' : '  (passa pelo gestor)'}
                </option>
              ))}
            </select>
          </Field>

          {passaPeloGestor && (
            <Notice tom="warn">
              Esta troca é <strong>fora do mesmo dia/turno</strong>. Depois que {reserva.solicitanteNome}
              {' '}aceitar, ela ainda depende do aval do gestor para se efetivar.
            </Notice>
          )}
          <Field label="Justificativa" hint="Obrigatória — será enviada ao professor responsável.">
            <textarea className="textarea" maxLength={500} value={justificativa}
              placeholder="Explique por que precisa deste ambiente…"
              onChange={(e) => setJustificativa(e.target.value)} />
          </Field>
        </>
      )}

      {erro && <Notice tom="danger">{erro}</Notice>}
    </Drawer>
  );
}
