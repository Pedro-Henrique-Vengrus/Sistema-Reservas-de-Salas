import { createContext, useContext, useState } from 'react';
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
    const u = { nome: data.nome, role: data.role, cursoId: data.cursoId };
    localStorage.setItem('cf_user', JSON.stringify(u));
    setUser(u);
    return u;
  }

  function logout() {
    localStorage.removeItem('cf_token');
    localStorage.removeItem('cf_user');
    setUser(null);
  }

  const isAdmin = user && user.role === 'GESTOR';

  return (
    <AuthContext.Provider value={{ user, isAdmin, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
