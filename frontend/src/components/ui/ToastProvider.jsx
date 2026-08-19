import { createContext, useCallback, useContext, useState } from 'react';

const ToastContext = createContext(null);
let seq = 0;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const push = useCallback((texto, tom = 'ok') => {
    const id = ++seq;
    setToasts((t) => [...t, { id, texto, tom }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 4000);
  }, []);

  const valor = {
    sucesso: (m) => push(m, 'ok'),
    erro: (m) => push(m, 'err'),
    info: (m) => push(m, ''),
  };

  return (
    <ToastContext.Provider value={valor}>
      {children}
      <div className="toasts">
        {toasts.map((t) => (
          <div key={t.id} className={`toast ${t.tom}`}>
            <span aria-hidden>{t.tom === 'err' ? '⛔' : t.tom === 'ok' ? '✓' : 'ℹ'}</span>
            <span className="grow">{t.texto}</span>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  return useContext(ToastContext);
}
