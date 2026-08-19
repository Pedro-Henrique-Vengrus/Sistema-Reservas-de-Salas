import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { PageHeader, StatusBadge, EmptyState, Notice } from '../components/ui/primitives';
import { dataBr, diaDaSemana, hhmm, reservaPassada } from '../lib/format';

/** Painel inicial: metricas administrativas e/ou a agenda pessoal do solicitante. */
export default function Dashboard() {
  const { user, ehAdministrativo, ehSolicitante, semCurso } = useAuth();
  const [metricas, setMetricas] = useState(null);
  const [minhas, setMinhas] = useState([]);
  const [trocas, setTrocas] = useState(0);
  const [grade, setGrade] = useState(null);
  const [erro, setErro] = useState('');

  useEffect(() => {
    (async () => {
      try {
        const tarefas = [api.get('/periodo-grade').then(setGrade)];
        if (ehAdministrativo) tarefas.push(api.get('/relatorios/dashboard').then(setMetricas));
        if (ehSolicitante) {
          tarefas.push(api.get('/reservas/minhas').then(setMinhas));
          tarefas.push(api.get('/propostas/pendentes/count').then((r) => setTrocas(r.count)));
        }
        await Promise.all(tarefas);
      } catch (e) { setErro(e.message); }
    })();
  }, [ehAdministrativo, ehSolicitante]);

  const futuras = minhas.filter((r) => !reservaPassada(r));
  const aprovadas = futuras.filter((r) => r.status === 'APROVADA');
  const pendentes = futuras.filter((r) => r.status === 'PENDENTE_APROVACAO');

  return (
    <>
      <PageHeader
        titulo={`Olá, ${user?.nome?.split(' ')[0]}`}
        descricao={ehAdministrativo
          ? 'Visão geral das reservas, ambientes e pendências do campus.'
          : 'Sua agenda, suas solicitações e o estado do preenchimento da grade.'}
        acoes={ehSolicitante && <Link className="btn" to="/agenda">+ Nova reserva</Link>}
      />

      {erro && <div className="mb-4"><Notice tom="danger">{erro}</Notice></div>}

      {semCurso && (
        <div className="mb-4">
          <Notice tom="warn">
            Seu perfil não está vinculado a nenhum curso, por isso nenhum ambiente aparece para você.
            Peça à administração para vincular seus cursos.
          </Notice>
        </div>
      )}

      {grade && (
        <div className="mb-4">
          <Notice tom={grade.aberto ? 'ok' : 'warn'}>
            <strong>
              {grade.aberto
                ? 'Preenchimento da grade bimestral aberto'
                : 'Preenchimento da grade bimestral fechado'}
            </strong>
            <div className="text-sm mt-2">
              {grade.descricao || (grade.aberto
                ? 'Você pode lançar reservas da grade normalmente.'
                : 'Enquanto estiver fechado, use o modo "última hora" — que passa pela moderação.')}
              {grade.fimVigencia && ` · Vigência até ${dataBr(grade.fimVigencia)}`}
            </div>
          </Notice>
        </div>
      )}

      {ehAdministrativo && metricas && (
        <>
          <div className="kpi-grid mb-4">
            <Kpi icone="⚖" tom="warn" valor={metricas.reservasPendentes} rotulo="Aguardando moderação"
              dica={<Link to="/admin/moderacao">Abrir fila →</Link>} />
            <Kpi icone="✓" tom="ok" valor={metricas.reservasAprovadas} rotulo="Reservas aprovadas"
              dica="Últimos 3 meses" />
            <Kpi icone="🏛" tom="info" valor={metricas.ambientesAtivos} rotulo="Ambientes ativos"
              dica={`${metricas.cursosAtivos} cursos ativos`} />
            <Kpi icone="👤" valor={metricas.usuariosAtivos} rotulo="Usuários ativos"
              dica={`${metricas.reservasCanceladas} reservas canceladas`} />
          </div>

          <div className="two-col mb-4">
            <section className="card">
              <div className="card-head">
                <h3>Próximas reservas aprovadas</h3>
                <Link className="text-sm" to="/admin/relatorios">Ver relatórios →</Link>
              </div>
              <div className="table-scroll">
                {metricas.proximasReservas.length === 0
                  ? <EmptyState icone="▤" titulo="Nada agendado" descricao="Não há reservas aprovadas à frente." />
                  : (
                    <table className="data">
                      <thead>
                        <tr><th>Data</th><th>Horário</th><th>Ambiente</th><th>Solicitante</th><th>Modo</th></tr>
                      </thead>
                      <tbody>
                        {metricas.proximasReservas.map((r) => (
                          <tr key={r.id}>
                            <td className="nowrap">{dataBr(r.data)} <span className="text-muted text-sm">{diaDaSemana(r.data, true)}</span></td>
                            <td className="nowrap">{hhmm(r.horaInicio)}–{hhmm(r.horaFim)}</td>
                            <td>{r.salaNome}</td>
                            <td>{r.solicitanteNome}</td>
                            <td><StatusBadge valor={r.tipoReserva} /></td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
              </div>
            </section>

            <section className="card">
              <div className="card-head"><h3>Ocupação por ambiente</h3></div>
              <div className="card-body">
                <Ranking dados={metricas.reservasPorSala} vazio="Sem reservas no período." />
              </div>
            </section>
          </div>

          <section className="card">
            <div className="card-head"><h3>Reservas por curso</h3></div>
            <div className="card-body">
              <Ranking dados={metricas.reservasPorCurso} vazio="Sem reservas vinculadas a cursos no período." />
            </div>
          </section>
        </>
      )}

      {ehSolicitante && (
        <>
          <div className="kpi-grid mb-4">
            <Kpi icone="✓" tom="ok" valor={aprovadas.length} rotulo="Reservas confirmadas" dica="À frente na sua agenda" />
            <Kpi icone="⏳" tom="warn" valor={pendentes.length} rotulo="Aguardando aprovação"
              dica="Solicitações de última hora" />
            <Kpi icone="⇄" tom="info" valor={trocas} rotulo="Trocas a responder"
              dica={<Link to="/trocas">Ver propostas →</Link>} />
            <Kpi icone="🎓" valor={(user?.cursos || []).length} rotulo="Cursos vinculados"
              dica={(user?.cursos || []).map((c) => c.sigla || c.nome).join(' · ') || 'Nenhum'} />
          </div>

          <section className="card">
            <div className="card-head">
              <h3>Sua próxima semana</h3>
              <Link className="text-sm" to="/minhas-reservas">Ver todas →</Link>
            </div>
            <div className="table-scroll">
              {futuras.length === 0
                ? (
                  <EmptyState icone="▤" titulo="Nenhuma reserva à frente"
                    descricao="Abra a agenda para reservar um ambiente do seu curso."
                    acao={<Link className="btn" to="/agenda">Ir para a agenda</Link>} />
                )
                : (
                  <table className="data">
                    <thead>
                      <tr><th>Data</th><th>Horário</th><th>Ambiente</th><th>Modo</th><th>Status</th></tr>
                    </thead>
                    <tbody>
                      {futuras.slice(0, 8).map((r) => (
                        <tr key={r.id}>
                          <td className="nowrap">{dataBr(r.data)} <span className="text-muted text-sm">{diaDaSemana(r.data, true)}</span></td>
                          <td className="nowrap">{hhmm(r.horaInicio)}–{hhmm(r.horaFim)}</td>
                          <td>{r.salaNome} <span className="text-muted text-sm">{r.salaAndar}</span></td>
                          <td><StatusBadge valor={r.tipoReserva} /></td>
                          <td><StatusBadge valor={r.status} /></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
            </div>
          </section>
        </>
      )}
    </>
  );
}

function Kpi({ icone, valor, rotulo, dica, tom }) {
  return (
    <div className={`kpi ${tom ? `tone-${tom}` : ''}`}>
      <span className="kpi-icon" aria-hidden>{icone}</span>
      <div className="grow">
        <div className="kpi-value">{valor ?? 0}</div>
        <div className="kpi-label">{rotulo}</div>
        {dica && <div className="kpi-hint">{dica}</div>}
      </div>
    </div>
  );
}

function Ranking({ dados = [], vazio }) {
  if (dados.length === 0) return <p className="text-muted text-md">{vazio}</p>;
  const max = Math.max(...dados.map((d) => d.total));
  return (
    <div className="bars">
      {dados.map((d) => (
        <div className="bar-row" key={d.rotulo}>
          <span className="truncate" title={d.rotulo}>{d.rotulo}</span>
          <span className="bar-track"><span className="bar-fill" style={{ width: `${(d.total / max) * 100}%` }} /></span>
          <span className="val">{d.total}</span>
        </div>
      ))}
    </div>
  );
}
