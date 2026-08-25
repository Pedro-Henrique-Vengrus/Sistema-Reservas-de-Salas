/**
 * Marca do CampusFlow: dois edificios do campus com uma seta em ascensao.
 *
 * Desenhada em SVG (nao imagem) para ficar nitida em qualquer tamanho, herdar
 * as cores institucionais e nao depender de carregamento de arquivo.
 * A seta e desenhada antes dos edificios: some atras do bloco escuro e reaparece
 * no ceu, a direita — o entrelacamento da marca, sem poluir as fachadas.
 */
export default function Logo({ tamanho = 32, className = '' }) {
  return (
    <svg
      className={className}
      width={tamanho}
      height={tamanho}
      viewBox="0 0 64 64"
      role="img"
      aria-label="CampusFlow"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      {/* Seta ascendente, ao fundo */}
      <path
        d="M4 49 C 15 48 19 35 29 28 C 37 22 43 16 48 10"
        stroke="#6cb794"
        strokeWidth="4.5"
        strokeLinecap="round"
      />
      <path d="M0 0 L-12 -5.5 L-12 5.5 Z" transform="translate(52 6) rotate(-50)" fill="#6cb794" />

      {/* Edificio da esquerda: bloco escuro com topo chanfrado */}
      <path d="M10 22 L22 15 L28 19 V53 H10 Z" fill="#114633" />
      <path d="M22 15 L28 19 V28.5 L22 24.5 Z" fill="#6cb794" />
      <rect x="14" y="27" width="6" height="7.5" rx="1" fill="#ffffff" />
      <rect x="14" y="39" width="6" height="7.5" rx="1" fill="#ffffff" />

      {/* Edificio da direita: claro, com esquadrias verdes */}
      <path d="M32 31 L44 25 L56 31 V53 H32 Z" fill="#ffffff" />
      <path d="M32 31 L44 25 L56 31 V53 H32 Z" stroke="#165a41" strokeWidth="2.8" strokeLinejoin="round" />
      <rect x="36.5" y="35.5" width="6" height="7" rx="1" fill="#1c7150" />
      <rect x="45.5" y="35.5" width="6" height="7" rx="1" fill="#1c7150" />
      <rect x="36.5" y="45" width="6" height="7" rx="1" fill="#1c7150" />
      <rect x="45.5" y="45" width="6" height="7" rx="1" fill="#1c7150" />

      {/* Base */}
      <rect x="6" y="53" width="52" height="4.5" rx="1.4" fill="#0d3527" />
    </svg>
  );
}
