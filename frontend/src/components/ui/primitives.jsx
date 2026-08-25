import { useEffect } from 'react';

/* ------------------------------------------------------------- PageHeader */
export function PageHeader({ titulo, descricao, acoes }) {
  return (
    <header className="page-header">
      <div>
        <h1>{titulo}</h1>
        {descricao && <p className="lead">{descricao}</p>}
      </div>
      {acoes && <div className="page-header-actions">{acoes}</div>}
    </header>
  );
}

/* ------------------------------------------------------------ StatusBadge */
const TONS = {
  APROVADA: ['ok', 'Aprovada'],
  PENDENTE_APROVACAO: ['warn', 'Pendente'],
  RECUSADA: ['danger', 'Recusada'],
  CANCELADA: ['muted', 'Cancelada'],
  PENDENTE: ['warn', 'Aguardando o professor'],
  AGUARDANDO_GESTOR: ['info', 'Aguardando o gestor'],
  ACEITA: ['ok', 'Aceita'],
  ATIVO: ['ok', 'Ativo'],
  INATIVO: ['muted', 'Inativo'],
  GRADE_BIMESTRAL: ['info', 'Grade bimestral'],
  ULTIMA_HORA: ['warn', 'Última hora'],
  PROFESSOR: ['muted', 'Professor'],
  REITOR: ['info', 'Reitor'],
  ADMIN: ['ok', 'Admin'],
  MATUTINO: ['muted', 'Matutino'],
  VESPERTINO: ['muted', 'Vespertino'],
  NOTURNO: ['muted', 'Noturno'],
};

export function StatusBadge({ valor, rotulo }) {
  if (!valor) return null;
  const [tom, texto] = TONS[valor] || ['muted', valor];
  return <span className={`badge ${tom}`}>{rotulo || texto}</span>;
}

/* --------------------------------------------------------------- Estados */
export function EmptyState({ icone = '∅', titulo, descricao, acao }) {
  return (
    <div className="empty">
      <div className="ico">{icone}</div>
      <h4>{titulo}</h4>
      {descricao && <p>{descricao}</p>}
      {acao && <div className="mt-4">{acao}</div>}
    </div>
  );
}

export function Notice({ tom = 'info', children }) {
  const ico = { info: 'ℹ', warn: '⚠', danger: '⛔', ok: '✓' }[tom];
  return (
    <div className={`notice ${tom}`}>
      <span aria-hidden>{ico}</span>
      <div className="grow">{children}</div>
    </div>
  );
}

/* ---------------------------------------------------------------- Campos */
export function Field({ label, hint, erro, children, className = '' }) {
  return (
    <div className={`field ${className}`}>
      {label && <label>{label}</label>}
      {children}
      {hint && !erro && <span className="hint">{hint}</span>}
      {erro && <span className="err">{erro}</span>}
    </div>
  );
}

export function Switch({ checked, onChange, label, disabled }) {
  return (
    <label className={`switch ${disabled ? 'is-disabled' : ''}`}>
      <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} disabled={disabled} />
      <span className="track" />
      {label && <span className="text-md">{label}</span>}
    </label>
  );
}

export function Segmented({ valor, onChange, opcoes }) {
  return (
    <div className="segmented">
      {opcoes.map((o) => (
        <button key={o.valor} type="button"
          className={valor === o.valor ? 'active' : ''}
          onClick={() => onChange(o.valor)}>
          {o.rotulo}
        </button>
      ))}
    </div>
  );
}

/* ------------------------------------------------------- Modal e Drawer */
function useEscape(onClose) {
  useEffect(() => {
    const h = (e) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', h);
    return () => window.removeEventListener('keydown', h);
  }, [onClose]);
}

export function Modal({ titulo, subtitulo, tamanho = '', onClose, children, rodape }) {
  useEscape(onClose);
  return (
    <div className="overlay center" onClick={onClose}>
      <div className={`modal ${tamanho}`} role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
        <div className="dialog-head">
          <div>
            <h3>{titulo}</h3>
            {subtitulo && <p className="sub">{subtitulo}</p>}
          </div>
          <button className="icon-btn" onClick={onClose} aria-label="Fechar">✕</button>
        </div>
        <div className="dialog-body">{children}</div>
        {rodape && <div className="dialog-foot">{rodape}</div>}
      </div>
    </div>
  );
}

export function Drawer({ titulo, subtitulo, tamanho = '', onClose, children, rodape }) {
  useEscape(onClose);
  return (
    <div className="overlay right" onClick={onClose}>
      <aside className={`drawer ${tamanho}`} role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
        <div className="dialog-head">
          <div>
            <h3>{titulo}</h3>
            {subtitulo && <p className="sub">{subtitulo}</p>}
          </div>
          <button className="icon-btn" onClick={onClose} aria-label="Fechar">✕</button>
        </div>
        <div className="dialog-body">{children}</div>
        {rodape && <div className="dialog-foot">{rodape}</div>}
      </aside>
    </div>
  );
}

/**
 * Dialogo de confirmacao das acoes com efeito colateral (inativar ambiente/curso
 * com reservas ativas). Mostra o impacto devolvido pela API antes de forcar.
 */
export function ConfirmDialog({ titulo, mensagem, detalhes, confirmarTexto = 'Confirmar',
                               tom = 'danger', onConfirm, onClose, ocupado }) {
  return (
    <Modal titulo={titulo} tamanho="sm" onClose={onClose}
      rodape={(
        <>
          <button className="btn btn-secondary" onClick={onClose}>Cancelar</button>
          <button className={tom === 'danger' ? 'btn btn-danger-solid' : 'btn'}
            onClick={onConfirm} disabled={ocupado}>
            {ocupado ? 'Processando…' : confirmarTexto}
          </button>
        </>
      )}>
      <p className="text-md">{mensagem}</p>
      {detalhes && (
        <Notice tom="warn">
          <strong>Impacto desta ação</strong>
          <dl className="dl mt-2">
            {detalhes.reservasFuturasAtivas !== undefined && (
              <>
                <dt>Reservas futuras canceladas</dt>
                <dd>{detalhes.reservasFuturasAtivas}</dd>
              </>
            )}
            {detalhes.usuariosVinculados > 0 && (
              <>
                <dt>Usuários vinculados</dt>
                <dd>{detalhes.usuariosVinculados}</dd>
              </>
            )}
          </dl>
          <p className="text-sm mt-2">Os solicitantes envolvidos serão notificados automaticamente.</p>
        </Notice>
      )}
    </Modal>
  );
}
