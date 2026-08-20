// Formatacao pt-BR compartilhada pelas telas.

export function hhmm(hora) {
  return hora ? String(hora).slice(0, 5) : '';
}

export function dataBr(iso) {
  if (!iso) return '';
  const [a, m, d] = iso.split('-');
  return `${d}/${m}/${a}`;
}

const DIAS = ['Domingo', 'Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado'];

export function diaDaSemana(iso, curto = false) {
  const d = new Date(`${iso}T12:00:00`);
  const nome = DIAS[d.getDay()];
  return curto ? nome.slice(0, 3) : nome;
}

export function hojeIso() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export function somaDias(iso, dias) {
  const d = new Date(`${iso}T12:00:00`);
  d.setDate(d.getDate() + dias);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** Segunda-feira da semana que contem a data informada. */
export function inicioDaSemana(iso) {
  const d = new Date(`${iso}T12:00:00`);
  const offset = (d.getDay() + 6) % 7;
  return somaDias(iso, -offset);
}

export function periodoIso(iso) {
  return `${dataBr(iso)} (${diaDaSemana(iso, true)})`;
}

export function dataHoraBr(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
}

export function iniciais(nome = '') {
  return nome.trim().split(/\s+/).slice(0, 2).map((p) => p[0]).join('').toUpperCase();
}

export function reservaPassada(r) {
  return new Date(`${r.data}T${r.horaFim}`) < new Date();
}

export const TURNOS = [
  { valor: 'MATUTINO', rotulo: 'Matutino' },
  { valor: 'VESPERTINO', rotulo: 'Vespertino' },
  { valor: 'NOTURNO', rotulo: 'Noturno' },
];

export const STATUS_RESERVA = [
  { valor: 'APROVADA', rotulo: 'Aprovada' },
  { valor: 'PENDENTE_APROVACAO', rotulo: 'Pendente de aprovação' },
  { valor: 'RECUSADA', rotulo: 'Recusada' },
  { valor: 'CANCELADA', rotulo: 'Cancelada' },
];

export const TIPOS_RESERVA = [
  { valor: 'GRADE_BIMESTRAL', rotulo: 'Grade bimestral' },
  { valor: 'ULTIMA_HORA', rotulo: 'Última hora' },
];
