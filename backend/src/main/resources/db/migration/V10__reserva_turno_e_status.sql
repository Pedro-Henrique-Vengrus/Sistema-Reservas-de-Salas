-- V10: Reserva ganha turno e adota os status oficiais
-- (CONFIRMADA -> APROVADA, PENDENTE -> PENDENTE_APROVACAO).

ALTER TABLE tb_reserva ADD COLUMN turno VARCHAR(20);
ALTER TABLE tb_reserva ADD COLUMN observacao VARCHAR(300);

-- Turno derivado da hora de inicio: < 12h matutino, < 18h vespertino, demais noturno.
-- Mesma regra de Turno.de(LocalTime) no dominio.
UPDATE tb_reserva SET turno = CASE
    WHEN hora_inicio <  TIME '12:00' THEN 'MATUTINO'
    WHEN hora_inicio <  TIME '18:00' THEN 'VESPERTINO'
    ELSE 'NOTURNO'
END;

ALTER TABLE tb_reserva ALTER COLUMN turno SET NOT NULL;
ALTER TABLE tb_reserva ADD CONSTRAINT ck_reserva_turno
    CHECK (turno IN ('MATUTINO', 'VESPERTINO', 'NOTURNO'));

UPDATE tb_reserva SET status = 'APROVADA'           WHERE status = 'CONFIRMADA';
UPDATE tb_reserva SET status = 'PENDENTE_APROVACAO' WHERE status = 'PENDENTE';

ALTER TABLE tb_reserva ALTER COLUMN status DROP DEFAULT;
ALTER TABLE tb_reserva ADD CONSTRAINT ck_reserva_status
    CHECK (status IN ('APROVADA', 'PENDENTE_APROVACAO', 'RECUSADA', 'CANCELADA'));
ALTER TABLE tb_reserva ADD CONSTRAINT ck_reserva_tipo
    CHECK (tipo_reserva IN ('GRADE_BIMESTRAL', 'ULTIMA_HORA'));

-- Busca de reservas elegiveis para troca (mesmo dia + mesmo turno)
CREATE INDEX idx_reserva_data_turno ON tb_reserva(data_reserva, turno);
CREATE INDEX idx_reserva_status ON tb_reserva(status);

-- Proposta de troca: status oficial inclui CANCELADA (cancelamento pelo proponente)
ALTER TABLE tb_proposta_troca ALTER COLUMN status DROP DEFAULT;
ALTER TABLE tb_proposta_troca ADD CONSTRAINT ck_proposta_status
    CHECK (status IN ('PENDENTE', 'ACEITA', 'RECUSADA', 'CANCELADA'));
