import { Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './auth/ProtectedRoute';
import Layout from './components/Layout';
import Login from './pages/Login';
import Agenda from './pages/Agenda';
import Salas from './pages/Salas';
import MinhasReservas from './pages/MinhasReservas';
import Propostas from './pages/Propostas';
import AdminPanel from './pages/AdminPanel';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route path="/agenda" element={<Agenda />} />
        <Route path="/salas" element={<Salas />} />
        <Route path="/minhas-reservas" element={<MinhasReservas />} />
        <Route path="/propostas" element={<Propostas />} />
        <Route path="/admin" element={
          <ProtectedRoute requireAdmin><AdminPanel /></ProtectedRoute>
        } />
      </Route>
      <Route path="*" element={<Navigate to="/agenda" replace />} />
    </Routes>
  );
}
