-- V2: Reserva e proposta de troca (negociacao)

CREATE TABLE tb_reserva (
    id              BIGSERIAL PRIMARY KEY,
    id_solicitante  BIGINT NOT NULL REFERENCES tb_usuario(id),
    id_sala         BIGINT NOT NULL REFERENCES tb_sala(id),
    data_reserva    DATE NOT NULL,
    hora_inicio     TIME NOT NULL,
    hora_fim        TIME NOT NULL,
    tipo_reserva    VARCHAR(50) NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDENTE',
    data_criacao    TIMESTAMP NOT NULL DEFAULT now(),
    data_modificacao TIMESTAMP,
    data_exclusao   TIMESTAMP
);

CREATE TABLE tb_proposta_troca (
    id                  BIGSERIAL PRIMARY KEY,
    id_reserva_origem   BIGINT NOT NULL REFERENCES tb_reserva(id),
    id_usuario_solicitante BIGINT NOT NULL REFERENCES tb_usuario(id),
    justificativa       VARCHAR(500) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    data_criacao        TIMESTAMP NOT NULL DEFAULT now(),
    data_modificacao    TIMESTAMP,
    data_exclusao       TIMESTAMP
);

-- Indices para buscas de conflito de horario (regra de bloqueio de sobreposicao)
CREATE INDEX idx_reserva_sala_data ON tb_reserva(id_sala, data_reserva);
CREATE INDEX idx_reserva_solicitante ON tb_reserva(id_solicitante);
CREATE INDEX idx_proposta_reserva ON tb_proposta_troca(id_reserva_origem);
