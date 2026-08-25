-- V14: Aviso por e-mail nas trocas de sala, com adesao explicita do usuario.
-- Opt-in: ninguem passa a receber e-mail sem ativar a opcao.

ALTER TABLE tb_usuario ADD COLUMN receber_emails BOOLEAN NOT NULL DEFAULT FALSE;
