import { Navigate, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './auth/ProtectedRoute';
import AppShell from './components/AppShell';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Agenda from './pages/Agenda';
import Ambientes from './pages/Ambientes';
import MinhasReservas from './pages/MinhasReservas';
import Trocas from './pages/Trocas';
import AdminUsuarios from './pages/admin/Usuarios';
import AdminSalas from './pages/admin/Salas';
import AdminCursos from './pages/admin/Cursos';
import AdminModeracao from './pages/admin/Moderacao';
import AdminPeriodoGrade from './pages/admin/PeriodoGrade';
import AdminRelatorios from './pages/admin/Relatorios';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route element={<ProtectedRoute><AppShell /></ProtectedRoute>}>
        <Route index element={<Dashboard />} />

        {/* Fluxo do solicitante */}
        <Route path="agenda" element={<ProtectedRoute roles={['PROFESSOR', 'REITOR']}><Agenda /></ProtectedRoute>} />
        <Route path="ambientes" element={<ProtectedRoute roles={['PROFESSOR', 'REITOR']}><Ambientes /></ProtectedRoute>} />
        <Route path="minhas-reservas" element={<ProtectedRoute roles={['PROFESSOR', 'REITOR']}><MinhasReservas /></ProtectedRoute>} />
        <Route path="trocas" element={<ProtectedRoute roles={['PROFESSOR', 'REITOR']}><Trocas /></ProtectedRoute>} />

        {/* Painel administrativo (ADMIN e REITOR) */}
        <Route path="admin/moderacao" element={<ProtectedRoute administrativo><AdminModeracao /></ProtectedRoute>} />
        <Route path="admin/usuarios" element={<ProtectedRoute administrativo><AdminUsuarios /></ProtectedRoute>} />
        <Route path="admin/salas" element={<ProtectedRoute administrativo><AdminSalas /></ProtectedRoute>} />
        <Route path="admin/cursos" element={<ProtectedRoute administrativo><AdminCursos /></ProtectedRoute>} />
        <Route path="admin/periodo-grade" element={<ProtectedRoute administrativo><AdminPeriodoGrade /></ProtectedRoute>} />
        <Route path="admin/relatorios" element={<ProtectedRoute administrativo><AdminRelatorios /></ProtectedRoute>} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
