import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit() {
    setErro('');
    setLoading(true);
    try {
      await login(email, senha);
      navigate('/admin');
    } catch (e) {
      setErro(e.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-bg">
      <div className="login-card">
        <div className="logo">🏫</div>
        <h2>CampusFlow</h2>
        <p className="sub">Sistema Inteligente de Reservas de Salas</p>

        <label>Email</label>
        <input
          type="email"
          placeholder="seu.email@campus.br"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
        />

        <label>Senha</label>
        <input
          type="password"
          placeholder="••••••"
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
        />

        <button className="btn" onClick={handleSubmit} disabled={loading}>
          {loading ? 'Entrando...' : 'Entrar'}
        </button>

        {erro && <p className="error">{erro}</p>}

        <div className="creds">
          Credenciais de teste:<br />
          pedro@campus.br / 123<br />
          joao@campus.br / 123<br />
          👤 Admin: admin@campus.br / 123
        </div>
      </div>
    </div>
  );
}
