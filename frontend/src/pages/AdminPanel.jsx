import { useState } from 'react';
import GerenciarSalas from '../components/GerenciarSalas';
import GerenciarCursos from '../components/GerenciarCursos';

export default function AdminPanel() {
  const [aba, setAba] = useState('salas');

  return (
    <div className="page">
      <h1>👤 Painel Administrativo</h1>
      <p className="lead">Gerencie salas e cursos do sistema</p>

      <div className="subtabs">
        <button className={aba === 'salas' ? 'active' : ''} onClick={() => setAba('salas')}>
          Gerenciar Salas
        </button>
        <button className={aba === 'cursos' ? 'active' : ''} onClick={() => setAba('cursos')}>
          Gerenciar Cursos
        </button>
      </div>

      {aba === 'salas' ? <GerenciarSalas /> : <GerenciarCursos />}
    </div>
  );
}
