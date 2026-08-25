import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import { useToast } from '../../components/ui/ToastProvider';
import { Field, Notice, PageHeader, Switch } from '../../components/ui/primitives';
import CampoData from '../../components/ui/CampoData';
import { dataBr, dataHoraBr } from '../../lib/format';

/**
 * Liberacao periodica do preenchimento da grade bimestral (conforme o edital).
 * Com o periodo fechado, solicitantes so conseguem pedir reservas de ultima hora.
 */
export default function PeriodoGrade() {
  const toast = useToast();
  const [periodo, setPeriodo] = useState(null);
  const [form, setForm] = useState(null);
  const [erro, setErro] = useState('');
  const [ocupado, setOcupado] = useState(false);

  useEffect(() => {
    api.get('/periodo-grade')
      .then((p) => {
        setPeriodo(p);
        setForm({
          aberto: !!p.aberto,
          descricao: p.descricao || '',
          inicioVigencia: p.inicioVigencia || '',
          fimVigencia: p.fimVigencia || '',
        });
      })
      .catch((e) => setErro(e.message));
  }, []);

  async function salvar() {
    setOcupado(true);
    setErro('');
    try {
      const atualizado = await api.put('/periodo-grade', {
        aberto: form.aberto,
        descricao: form.descricao || null,
        inicioVigencia: form.inicioVigencia || null,
        fimVigencia: form.fimVigencia || null,
      });
      setPeriodo(atualizado);
      toast.sucesso(form.aberto
        ? 'Preenchimento da grade liberado.'
        : 'Preenchimento da grade encerrado.');
    } catch (e) { setErro(e.message); } finally { setOcupado(false); }
  }

  if (!form) return <div className="panel"><div className="skeleton" style={{ height: 160 }} /></div>;

  return (
    <>
      <PageHeader titulo="Período da grade bimestral"
        descricao="Controla a janela em que professores podem lançar reservas da grade. Fora dela, só solicitações de última hora — que passam pela moderação." />

      {erro && <div className="mb-4"><Notice tom="danger">{erro}</Notice></div>}

      <div className="two-col">
        <section className="card">
          <div className="card-head"><h3>Configuração</h3></div>
          <div className="card-body col gap-4">
            <div className="panel" style={{ background: form.aberto ? 'var(--ok-bg)' : 'var(--warn-bg)', borderColor: form.aberto ? 'var(--ok-br)' : 'var(--warn-br)' }}>
              <Switch checked={form.aberto} onChange={(v) => setForm({ ...form, aberto: v })}
                label={form.aberto
                  ? 'Preenchimento liberado — reservas da grade confirmam direto'
                  : 'Preenchimento fechado — apenas solicitações de última hora'} />
            </div>

            <Field label="Descrição do período" hint="Aparece no painel dos professores.">
              <input className="input" maxLength={120} value={form.descricao}
                placeholder="Ex: Grade 2026/2 — 1º bimestre"
                onChange={(e) => setForm({ ...form, descricao: e.target.value })} />
            </Field>

            <div className="split">
              <Field label="Início da vigência" hint="Opcional.">
                <CampoData value={form.inicioVigencia}
                  onChange={(iso) => setForm({ ...form, inicioVigencia: iso })} />
              </Field>
              <Field label="Fim da vigência" hint="Após esta data a grade fecha sozinha.">
                <CampoData value={form.fimVigencia} min={form.inicioVigencia || undefined}
                  onChange={(iso) => setForm({ ...form, fimVigencia: iso })} />
              </Field>
            </div>

            <div className="row">
              <button className="btn" onClick={salvar} disabled={ocupado}>
                {ocupado ? 'Salvando…' : 'Salvar configuração'}
              </button>
            </div>
          </div>
        </section>

        <div className="col gap-4">
          <section className="card">
            <div className="card-head"><h3>Estado atual</h3></div>
            <div className="card-body">
              <dl className="dl">
                <dt>Situação</dt>
                <dd>
                  <span className={`badge ${periodo?.aberto ? 'ok' : 'warn'}`}>
                    {periodo?.aberto ? 'Aberto' : 'Fechado'}
                  </span>
                </dd>
                <dt>Descrição</dt><dd>{periodo?.descricao || '—'}</dd>
                <dt>Vigência</dt>
                <dd>
                  {periodo?.inicioVigencia || periodo?.fimVigencia
                    ? `${periodo.inicioVigencia ? dataBr(periodo.inicioVigencia) : '—'} → ${periodo.fimVigencia ? dataBr(periodo.fimVigencia) : '—'}`
                    : 'Sem limite de datas'}
                </dd>
                <dt>Última alteração</dt>
                <dd>
                  {periodo?.dataModificacao
                    ? `${dataHoraBr(periodo.dataModificacao)}${periodo.atualizadoPor ? ` por ${periodo.atualizadoPor}` : ''}`
                    : '—'}
                </dd>
              </dl>
            </div>
          </section>

          <Notice tom="info">
            <strong>Como a regra se aplica</strong>
            <ul className="mt-2 text-md" style={{ listStyle: 'disc', paddingLeft: 18 }}>
              <li>Grade bimestral: confirma direto quando não há conflito de sala e horário.</li>
              <li>Última hora: sempre entra na fila de moderação, independentemente do período.</li>
              <li>O painel administrativo lança reservas em nome de terceiros mesmo com a grade fechada.</li>
            </ul>
          </Notice>
        </div>
      </div>
    </>
  );
}
