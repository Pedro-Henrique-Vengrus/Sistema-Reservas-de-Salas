-- V13: Troca fora do mesmo dia/turno deixa de ser bloqueada e passa a exigir aval do gestor.
-- A proposta ganha um estado intermediario entre o aceite do professor e a efetivacao.

ALTER TABLE tb_proposta_troca DROP CONSTRAINT IF EXISTS ck_proposta_status;

ALTER TABLE tb_proposta_troca ADD CONSTRAINT ck_proposta_status
    CHECK (status IN ('PENDENTE', 'AGUARDANDO_GESTOR', 'ACEITA', 'RECUSADA', 'CANCELADA'));

-- Fila do gestor no painel de moderacao
CREATE INDEX idx_proposta_status ON tb_proposta_troca(status);
