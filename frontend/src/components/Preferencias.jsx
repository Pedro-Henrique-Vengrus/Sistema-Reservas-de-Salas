import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { useToast } from './ui/ToastProvider';
import { Modal, Notice, Switch } from './ui/primitives';

/**
 * Preferencias do proprio usuario. Hoje contem a adesao aos avisos por e-mail,
 * que e opt-in: ninguem recebe sem ligar aqui.
 */
export default function Preferencias({ onFechar }) {
  const toast = useToast();
  const [perfil, setPerfil] = useState(null);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');

  useEffect(() => {
    api.get('/usuarios/me').then(setPerfil).catch((e) => setErro(e.message));
  }, []);

  async function alternar(valor) {
    setSalvando(true);
    setErro('');
    try {
      const atualizado = await api.put('/usuarios/me/preferencias', { receberEmails: valor });
      setPerfil(atualizado);
      toast.sucesso(valor
        ? 'Avisos por e-mail ativados.'
        : 'Avisos por e-mail desativados.');
    } catch (e) { setErro(e.message); } finally { setSalvando(false); }
  }

  return (
    <Modal titulo="Preferências" subtitulo="Como você quer ser avisado" onClose={onFechar}
      rodape={<button className="btn btn-secondary" onClick={onFechar}>Fechar</button>}>

      {erro && <Notice tom="danger">{erro}</Notice>}

      {!perfil ? <div className="skeleton" style={{ height: 90 }} /> : (
        <>
          <div className="panel">
            <Switch
              checked={perfil.receberEmails}
              disabled={salvando}
              onChange={alternar}
              label="Receber avisos de troca de sala por e-mail" />
            <p className="text-sm text-muted mt-2">
              Enviados para <strong>{perfil.email}</strong>
            </p>
          </div>

          <Notice tom="info">
            <strong>O que chega por e-mail</strong>
            <ul className="mt-2 text-md" style={{ listStyle: 'disc', paddingLeft: 18 }}>
              <li>Uma nova proposta de troca chegou para você</li>
              <li>Sua troca foi aceita — pelo professor ou pelo gestor</li>
              <li>A troca foi recusada ou cancelada</li>
            </ul>
            <p className="text-sm mt-2">
              Os demais avisos — aprovação de reserva, cancelamentos — continuam
              apenas no sino, independente desta opção.
            </p>
          </Notice>
        </>
      )}
    </Modal>
  );
}
