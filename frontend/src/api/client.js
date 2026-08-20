// Cliente HTTP: injeta o token, normaliza erros da API e trata download de arquivos.
const BASE = '/api';

function getToken() {
  return localStorage.getItem('cf_token');
}

/** Monta a querystring ignorando filtros vazios (undefined, null ou ''). */
export function qs(params = {}) {
  const sp = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v === undefined || v === null || v === '') return;
    if (Array.isArray(v)) v.forEach((item) => sp.append(k, item));
    else sp.append(k, v);
  });
  const s = sp.toString();
  return s ? `?${s}` : '';
}

/** Erro da API com o status e os detalhes de impacto usados nos dialogos de confirmacao. */
export class ApiError extends Error {
  constructor(message, status, detalhes, fieldErrors) {
    super(message);
    this.status = status;
    this.detalhes = detalhes;
    this.fieldErrors = fieldErrors;
  }
}

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(`${BASE}${path}`, { ...options, headers });
  } catch {
    throw new ApiError('Sem conexão com o servidor (o backend está rodando?)', 0);
  }

  if (res.status === 401) {
    localStorage.removeItem('cf_token');
    localStorage.removeItem('cf_user');
    if (!path.includes('/auth/login')) window.location.href = '/login';
    throw new ApiError('Sessão expirada. Entre novamente.', 401);
  }

  if (!res.ok) {
    let msg = `Erro ${res.status}`;
    let detalhes;
    let fieldErrors;
    try {
      const body = await res.json();
      if (body.message) msg = body.message;
      detalhes = body.detalhes;
      fieldErrors = body.fieldErrors;
      if (fieldErrors) msg = Object.values(fieldErrors).join(' · ') || msg;
    } catch { /* resposta sem corpo JSON */ }
    throw new ApiError(msg, res.status, detalhes, fieldErrors);
  }

  if (res.status === 204) return null;
  return res.json();
}

/** Baixa um arquivo gerado pela API (relatório CSV) preservando o nome sugerido. */
async function download(path, nomePadrao) {
  const res = await fetch(`${BASE}${path}`, { headers: { Authorization: `Bearer ${getToken()}` } });
  if (!res.ok) {
    throw new ApiError(res.status === 403 ? 'Você não tem acesso a este relatório.' : `Erro ${res.status}`, res.status);
  }
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = nomePadrao;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

export const api = {
  get: (p) => request(p),
  post: (p, body) => request(p, { method: 'POST', body: JSON.stringify(body ?? {}) }),
  put: (p, body) => request(p, { method: 'PUT', body: JSON.stringify(body ?? {}) }),
  del: (p) => request(p, { method: 'DELETE' }),
  download,
};
