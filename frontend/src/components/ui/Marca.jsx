import { useState } from 'react';
import Logo from './Logo';

/**
 * Marca da aplicacao (logotipo com o nome).
 *
 * Usa a arte oficial em `frontend/public/marca.png` quando o arquivo existe.
 * Se ele nao estiver la, cai automaticamente no desenho vetorial + nome escrito:
 * assim o projeto nunca sobe sem marca nenhuma, e trocar a arte nao exige
 * mexer em codigo — basta substituir o arquivo.
 *
 * A arte fica sobre uma placa clara porque as duas telas onde ela aparece
 * (sidebar e vitrine do login) tem fundo verde-escuro; a placa funciona tanto
 * para um PNG de fundo branco quanto para um com transparencia.
 */
export default function Marca({ altura = 30 }) {
  const [semArquivo, setSemArquivo] = useState(false);

  if (semArquivo) {
    return (
      <span className="marca">
        <span className="marca-chip" style={{ width: altura + 6, height: altura + 6 }}>
          <Logo tamanho={Math.round(altura * 0.85)} />
        </span>
        <span className="marca-nome">CampusFlow</span>
      </span>
    );
  }

  return (
    <span className="marca">
      <span className="marca-placa">
        <img src="/marca.png" alt="CampusFlow" style={{ height: altura }}
          onError={() => setSemArquivo(true)} />
      </span>
    </span>
  );
}
