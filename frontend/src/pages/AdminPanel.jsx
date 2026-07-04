import { useState } from 'react';
import GerenciarSalas from '../components/GerenciarSalas';
import GerenciarCursos from '../components/GerenciarCursos';
import AprovarReservas from '../components/AprovarReservas';
import TodasReservas from '../components/TodasReservas';

export default function AdminPanel() {
  const [aba, setAba] = useState('reservas');

  return (
    <div className="page">
      <h1>👤 Painel Administrativo</h1>
      <p className="lead">Gerencie reservas, salas e cursos do sistema</p>

      <div className="subtabs">
        <button className={aba === 'reservas' ? 'active' : ''} onClick={() => setAba('reservas')}>
          Aprovar Reservas
        </button>
        <button className={aba === 'todas' ? 'active' : ''} onClick={() => setAba('todas')}>
          Todas as Reservas
        </button>
        <button className={aba === 'salas' ? 'active' : ''} onClick={() => setAba('salas')}>
          Gerenciar Salas
        </button>
        <button className={aba === 'cursos' ? 'active' : ''} onClick={() => setAba('cursos')}>
          Gerenciar Cursos
        </button>
      </div>

      {aba === 'reservas' && <AprovarReservas />}
      {aba === 'todas' && <TodasReservas />}
      {aba === 'salas' && <GerenciarSalas />}
      {aba === 'cursos' && <GerenciarCursos />}
    </div>
  );
}
