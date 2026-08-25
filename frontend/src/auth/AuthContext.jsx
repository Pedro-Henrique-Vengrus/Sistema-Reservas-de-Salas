import { createContext, useContext, useMemo, useState } from 'react';
import { api } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('cf_user');
    return raw ? JSON.parse(raw) : null;
  });

  async function login(email, senha) {
    const data = await api.post('/auth/login', { email, senha });
    localStorage.setItem('cf_token', data.token);
    const u = {
      id: data.id,
      nome: data.nome,
      email: data.email,
      role: data.role,
      administrativo: data.administrativo,
      cursos: data.cursos || [],
    };
    localStorage.setItem('cf_user', JSON.stringify(u));
    setUser(u);
    return u;
  }

  function logout() {
    localStorage.removeItem('cf_token');
    localStorage.removeItem('cf_user');
    setUser(null);
  }

  const valor = useMemo(() => ({
    user,
    login,
    logout,
    // Somente o ADMIN opera o painel; REITOR e PROFESSOR sao solicitantes
    ehAdministrativo: !!user?.administrativo,
    ehSolicitante: user?.role === 'PROFESSOR' || user?.role === 'REITOR',
    semCurso: !!user && user.role !== 'ADMIN' && (user.cursos || []).length === 0,
  }), [user]);

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
