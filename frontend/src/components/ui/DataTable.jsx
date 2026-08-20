import { useMemo, useState } from 'react';
import { EmptyState } from './primitives';

/**
 * Tabela de dados densa para telas desktop: busca, ordenação e paginação
 * no cliente. Cada coluna declara `{ chave, titulo, render, ordenavel, valor, largura, alinhar }`.
 * `valor(linha)` é usado para ordenar e buscar quando `render` devolve JSX.
 */
export default function DataTable({
  colunas,
  dados,
  chaveLinha = (l) => l.id,
  buscaPlaceholder = 'Buscar…',
  comBusca = true,
  porPagina = 12,
  vazio,
  acoesToolbar,
  classeLinha,
  ordemInicial,
}) {
  const [busca, setBusca] = useState('');
  const [ordem, setOrdem] = useState(ordemInicial || null); // { chave, dir }
  const [pagina, setPagina] = useState(0);

  const textoDaLinha = (linha) => colunas
    .map((c) => (c.valor ? c.valor(linha) : linha[c.chave]))
    .filter((v) => v !== null && v !== undefined)
    .join(' ')
    .toLowerCase();

  const filtrados = useMemo(() => {
    const termo = busca.trim().toLowerCase();
    if (!termo) return dados;
    return dados.filter((l) => textoDaLinha(l).includes(termo));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dados, busca, colunas]);

  const ordenados = useMemo(() => {
    if (!ordem) return filtrados;
    const col = colunas.find((c) => c.chave === ordem.chave);
    if (!col) return filtrados;
    const get = (l) => (col.valor ? col.valor(l) : l[col.chave]);
    return [...filtrados].sort((a, b) => {
      const va = get(a); const vb = get(b);
      if (va === vb) return 0;
      if (va === null || va === undefined) return 1;
      if (vb === null || vb === undefined) return -1;
      const cmp = typeof va === 'number' && typeof vb === 'number'
        ? va - vb
        : String(va).localeCompare(String(vb), 'pt-BR', { numeric: true });
      return ordem.dir === 'asc' ? cmp : -cmp;
    });
  }, [filtrados, ordem, colunas]);

  const totalPaginas = Math.max(1, Math.ceil(ordenados.length / porPagina));
  const paginaAtual = Math.min(pagina, totalPaginas - 1);
  const visiveis = ordenados.slice(paginaAtual * porPagina, (paginaAtual + 1) * porPagina);

  function alternarOrdem(chave) {
    setOrdem((o) => {
      if (!o || o.chave !== chave) return { chave, dir: 'asc' };
      if (o.dir === 'asc') return { chave, dir: 'desc' };
      return null;
    });
  }

  return (
    <div className="table-wrap">
      {(comBusca || acoesToolbar) && (
        <div className="table-toolbar">
          {comBusca ? (
            <div className="search" style={{ width: 300 }}>
              <span className="ico" aria-hidden>⌕</span>
              <input className="input" placeholder={buscaPlaceholder} value={busca}
                onChange={(e) => { setBusca(e.target.value); setPagina(0); }} />
            </div>
          ) : <div />}
          <div className="row gap-2">{acoesToolbar}</div>
        </div>
      )}

      <div className="table-scroll">
        <table className="data">
          <thead>
            <tr>
              {colunas.map((c) => (
                <th key={c.chave}
                  className={`${c.ordenavel !== false ? 'sortable' : ''} ${c.alinhar === 'right' ? 'text-right' : ''}`}
                  style={c.largura ? { width: c.largura } : undefined}
                  onClick={c.ordenavel !== false ? () => alternarOrdem(c.chave) : undefined}>
                  {c.titulo}
                  {ordem?.chave === c.chave && <span className="sort">{ordem.dir === 'asc' ? '▲' : '▼'}</span>}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {visiveis.map((linha) => (
              <tr key={chaveLinha(linha)} className={classeLinha ? classeLinha(linha) : undefined}>
                {colunas.map((c) => (
                  <td key={c.chave} className={c.alinhar === 'right' ? 'actions' : undefined}>
                    {c.render ? c.render(linha) : linha[c.chave]}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>

        {visiveis.length === 0 && (vazio || (
          <EmptyState titulo="Nenhum registro encontrado"
            descricao={busca ? 'Ajuste a busca ou limpe os filtros aplicados.' : 'Ainda não há dados para exibir.'} />
        ))}
      </div>

      <div className="table-foot">
        <span>
          {ordenados.length === 0
            ? 'Nenhum registro'
            : `${paginaAtual * porPagina + 1}–${Math.min((paginaAtual + 1) * porPagina, ordenados.length)} de ${ordenados.length}`}
        </span>
        {totalPaginas > 1 && (
          <div className="pager">
            <button className="btn btn-secondary btn-sm" disabled={paginaAtual === 0}
              onClick={() => setPagina(paginaAtual - 1)}>← Anterior</button>
            <span className="text-sm">Página {paginaAtual + 1} de {totalPaginas}</span>
            <button className="btn btn-secondary btn-sm" disabled={paginaAtual >= totalPaginas - 1}
              onClick={() => setPagina(paginaAtual + 1)}>Próxima →</button>
          </div>
        )}
      </div>
    </div>
  );
}
