import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Field, Notice } from '../components/ui/primitives';
import Logo from '../components/ui/Logo';

const CONTAS = [
  ['admin@campus.br', 'Admin', 'Painel completo, sem reservas próprias'],
  ['reitor@campus.br', 'Reitor', 'Painel completo + reservas próprias'],
  ['pedro@campus.br', 'Professor', 'Ciência da Computação'],
  ['carla@campus.br', 'Professor', 'Engenharia'],
];

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
        <div className="row gap-2">
          <span className="login-mark"><Logo tamanho={30} /></span>
          <strong style={{ fontSize: 20, letterSpacing: '-.01em' }}>CampusFlow</strong>
        </div>

        <div>
          <h1>Reserva de salas e laboratórios do campus</h1>
          <p>
            Grade bimestral, solicitações de última hora, troca de salas entre professores
            e moderação centralizada — em um só lugar.
          </p>
        </div>

        <div className="col gap-4" style={{ maxWidth: 460 }}>
          <div className="feature">
            <span aria-hidden>▤</span>
            <span><strong>Agenda semanal</strong>Visão ambiente × horário, com conflitos evidentes.</span>
          </div>
          <div className="feature">
            <span aria-hidden>⇄</span>
            <span><strong>Troca entre professores</strong>Reservas no mesmo dia e turno, com justificativa.</span>
          </div>
          <div className="feature">
            <span aria-hidden>⚖</span>
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

          <div className="mt-4">
            <p className="text-sm text-muted mb-4">Contas de demonstração (senha <code>123</code>) — clique para preencher:</p>
            <table className="cred-table">
              <tbody>
                {CONTAS.map(([mail, perfil, obs]) => (
                  <tr key={mail} onClick={() => { setEmail(mail); setSenha('123'); }}>
                    <td className="text-mono">{mail}</td>
                    <td><strong>{perfil}</strong></td>
                    <td className="text-muted text-sm">{obs}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </form>
      </section>
    </div>
  );
}
