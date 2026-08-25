import { useEffect, useRef, useState } from 'react';

/**
 * Campo de data que exibe sempre dd/mm/aaaa.
 *
 * O <input type="date"> nativo desenha a mascara no idioma do NAVEGADOR, nao no da
 * pagina: num Windows/Chrome em ingles ele mostra mm/dd/yyyy mesmo com <html lang="pt-BR">.
 * Por isso o texto visivel e nosso; o calendario nativo fica atras do botao, so para
 * escolher o dia no mouse. O valor trafega sempre em ISO (aaaa-mm-dd), como a API espera.
 */
export default function CampoData({ value = '', onChange, min, max, disabled, id }) {
  const [texto, setTexto] = useState(() => paraBr(value));
  const nativo = useRef(null);

  // Reflete mudancas vindas de fora (limpar filtros, carregar do servidor).
  useEffect(() => { setTexto(paraBr(value)); }, [value]);

  function digitar(bruto) {
    const digitos = bruto.replace(/\D/g, '').slice(0, 8);
    const partes = [digitos.slice(0, 2), digitos.slice(2, 4), digitos.slice(4, 8)];
    setTexto(partes.filter(Boolean).join('/'));

    if (!digitos) onChange('');
    else if (digitos.length === 8) {
      const iso = paraIso(partes);
      if (iso) onChange(iso);
    }
  }

  /** Texto incompleto ou data inexistente (31/02) volta para o ultimo valor valido. */
  function sair() { setTexto(paraBr(value)); }

  function abrirCalendario() {
    if (nativo.current?.showPicker) nativo.current.showPicker();
    else nativo.current?.focus();
  }

  return (
    <div className="campo-data">
      <input className="input" id={id} inputMode="numeric" placeholder="dd/mm/aaaa"
        value={texto} disabled={disabled}
        onChange={(e) => digitar(e.target.value)} onBlur={sair} />

      <button type="button" className="campo-data-btn" onClick={abrirCalendario}
        disabled={disabled} tabIndex={-1} aria-label="Escolher no calendário">🗓</button>

      {/* Fora do fluxo de foco: existe apenas para abrir o seletor nativo. */}
      <input ref={nativo} type="date" className="campo-data-nativo" tabIndex={-1} aria-hidden="true"
        value={value || ''} min={min} max={max} disabled={disabled}
        onChange={(e) => onChange(e.target.value)} />
    </div>
  );
}

function paraBr(iso) {
  if (!iso) return '';
  const [a, m, d] = iso.split('-');
  return a && m && d ? `${d}/${m}/${a}` : '';
}

/** So devolve ISO se a data existir no calendario. */
function paraIso([d, m, a]) {
  const data = new Date(Number(a), Number(m) - 1, Number(d));
  const existe = data.getFullYear() === Number(a)
    && data.getMonth() === Number(m) - 1
    && data.getDate() === Number(d);
  return existe ? `${a}-${m}-${d}` : null;
}
