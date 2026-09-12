/**
 * Conjunto de icones da aplicacao, em SVG.
 *
 * No lugar dos emojis: emoji e desenhado pela fonte do sistema operacional, vem
 * colorido e muda de forma entre Windows, Mac e Android — ao lado de uma marca
 * vetorial isso destoa. Estes herdam a cor do texto (currentColor), tem a mesma
 * espessura de traco e ficam nitidos em qualquer tamanho.
 *
 * Grade de 24x24, traco de 1.6 e pontas arredondadas: o mesmo vocabulario do Logo.
 */

const FORMAS = {
  painel: (
    <>
      <rect x="3" y="3" width="7.5" height="7.5" rx="1.5" />
      <rect x="13.5" y="3" width="7.5" height="7.5" rx="1.5" />
      <rect x="3" y="13.5" width="7.5" height="7.5" rx="1.5" />
      <rect x="13.5" y="13.5" width="7.5" height="7.5" rx="1.5" />
    </>
  ),

  agenda: (
    <>
      <rect x="3" y="5" width="18" height="16" rx="2" />
      <path d="M8 3v4M16 3v4M3 10h18M9.5 10v11M15 10v11" />
    </>
  ),

  calendario: (
    <>
      <rect x="3" y="5" width="18" height="16" rx="2" />
      <path d="M8 3v4M16 3v4M3 10h18" />
    </>
  ),

  periodo: (
    <>
      <rect x="3" y="5" width="18" height="16" rx="2" />
      <path d="M8 3v4M16 3v4M3 10h18" />
      <path d="M8.6 14.8l2.4 2.4 4.4-4.6" />
    </>
  ),

  ambiente: (
    <>
      <path d="M3 21h18" />
      <path d="M5 21V9l7-4.5L19 9v12" />
      <path d="M9.6 12.4h1.6M12.8 12.4h1.6M9.6 16h1.6M12.8 16h1.6" />
    </>
  ),

  reservas: (
    <>
      <rect x="5" y="4" width="14" height="17" rx="2" />
      <rect x="9" y="2" width="6" height="4" rx="1.2" />
      <path d="M9 11.5h6M9 15.5h4" />
    </>
  ),

  troca: (
    <>
      <path d="M4 8.5h13l-3.6-3.6" />
      <path d="M20 15.5H7l3.6 3.6" />
    </>
  ),

  moderacao: (
    <>
      <path d="M12 3l7.5 3v5.6c0 4.3-3.2 7.6-7.5 9.4-4.3-1.8-7.5-5.1-7.5-9.4V6z" />
      <path d="M9 12.2l2.2 2.2 4.3-4.4" />
    </>
  ),

  usuarios: (
    <>
      <circle cx="12" cy="8" r="3.6" />
      <path d="M4.5 20.5v-.8A5.5 5.5 0 0 1 10 14.2h4a5.5 5.5 0 0 1 5.5 5.5v.8" />
    </>
  ),

  cursos: (
    <>
      <path d="M2.5 9L12 4.5 21.5 9 12 13.5z" />
      <path d="M6.5 11.2V16c0 1.6 2.5 3 5.5 3s5.5-1.4 5.5-3v-4.8" />
    </>
  ),

  relatorios: (
    <>
      <path d="M3.5 21h17" />
      <path d="M7 21v-6.5M12 21V6.5M17 21v-9.5" />
    </>
  ),

  sino: (
    <>
      <path d="M18 9.5a6 6 0 1 0-12 0c0 5.5-2 7-2 7h16s-2-1.5-2-7" />
      <path d="M13.8 20a2.1 2.1 0 0 1-3.6 0" />
    </>
  ),

  preferencias: (
    <>
      <path d="M4 7h8.6M17.5 7H20M4 12h3.1M12.3 12H20M4 17h8.6M17.5 17H20" />
      <circle cx="15.1" cy="7" r="2.4" />
      <circle cx="9.7" cy="12" r="2.4" />
      <circle cx="15.1" cy="17" r="2.4" />
    </>
  ),

  sair: (
    <>
      <path d="M12 3.5v8.2" />
      <path d="M7.4 6.7a7.2 7.2 0 1 0 9.2 0" />
    </>
  ),

  ok: (
    <>
      <circle cx="12" cy="12" r="8.5" />
      <path d="M8.5 12.4l2.5 2.4 4.6-4.9" />
    </>
  ),

  marcador: <path d="M6.5 3.5h11a1 1 0 0 1 1 1v16.2l-6.5-4.3-6.5 4.3V4.5a1 1 0 0 1 1-1z" />,
};

export default function Icone({ nome, tamanho = 18, className = '' }) {
  const forma = FORMAS[nome];
  if (!forma) return null;
  return (
    <svg
      className={className}
      width={tamanho}
      height={tamanho}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {forma}
    </svg>
  );
}
