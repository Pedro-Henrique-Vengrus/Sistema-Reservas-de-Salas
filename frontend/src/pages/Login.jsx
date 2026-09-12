import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Field, Notice } from '../components/ui/primitives';
import Marca from '../components/ui/Marca';
import Icone from '../components/ui/Icone';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState('');
  const [entrando, setEntrando] = useState(false);

  async function enviar(e) {
    e.preventDefault();
    setErro('');
    setEntrando(true);
    try {
      await login(email.trim(), senha);
      navigate('/', { replace: true });
    } catch (ex) {
      setErro(ex.message);
      setEntrando(false);
    }
  }

  return (
    <div className="login">
      <section className="login-hero">
        <Marca altura={34} />

        <div>
          <h1>Reserva de salas e laboratórios do campus</h1>
          <p>
            Grade bimestral, solicitações de última hora, troca de salas entre professores
            e moderação centralizada — em um só lugar.
          </p>
        </div>

        <div className="col gap-4" style={{ maxWidth: 460 }}>
          <div className="feature">
            <Icone nome="agenda" tamanho={20} />
            <span><strong>Agenda semanal</strong>Visão ambiente × horário, com conflitos evidentes.</span>
          </div>
          <div className="feature">
            <Icone nome="troca" tamanho={20} />
            <span><strong>Troca entre professores</strong>Direta no mesmo dia e turno; fora disso, com aval do gestor.</span>
          </div>
          <div className="feature">
            <Icone nome="moderacao" tamanho={20} />
            <span><strong>Moderação e relatórios</strong>Fila de aprovação e exportação por período, curso e sala.</span>
          </div>
        </div>
      </section>

      <section className="login-form-wrap">
        <form className="login-form" onSubmit={enviar}>
          <div>
            <h2>Entrar</h2>
            <p className="text-muted text-md mt-2">Use suas credenciais institucionais.</p>
          </div>

          <Field label="E-mail">
            <input className="input" type="email" autoComplete="username" required
              placeholder="voce@campus.br" value={email} onChange={(e) => setEmail(e.target.value)} />
          </Field>

          <Field label="Senha">
            <input className="input" type="password" autoComplete="current-password" required
              placeholder="••••" value={senha} onChange={(e) => setSenha(e.target.value)} />
          </Field>

          {erro && <Notice tom="danger">{erro}</Notice>}

          <button className="btn btn-block" type="submit" disabled={entrando}>
            {entrando ? 'Entrando…' : 'Entrar'}
          </button>

          <p className="text-sm text-muted">
            Esqueceu a senha ou ainda não tem acesso? Procure a administração do campus.
          </p>
        </form>
      </section>
    </div>
  );
}
