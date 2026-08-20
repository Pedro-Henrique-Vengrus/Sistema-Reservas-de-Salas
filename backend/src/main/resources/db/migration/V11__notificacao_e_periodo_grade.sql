-- V11: Avisos in-app e controle do periodo de preenchimento da grade bimestral

CREATE TABLE tb_notificacao (
    id              BIGSERIAL PRIMARY KEY,
    id_destinatario BIGINT NOT NULL REFERENCES tb_usuario(id) ON DELETE CASCADE,
    tipo            VARCHAR(30) NOT NULL,
    titulo          VARCHAR(120) NOT NULL,
    mensagem        VARCHAR(500) NOT NULL,
    lida            BOOLEAN NOT NULL DEFAULT FALSE,
    data_criacao    TIMESTAMP NOT NULL DEFAULT now(),
    data_leitura    TIMESTAMP
);

CREATE INDEX idx_notificacao_destinatario ON tb_notificacao(id_destinatario, lida);

-- Linha unica (id = 1): flag global liberada pelo Admin/Reitor conforme o edital
CREATE TABLE tb_periodo_grade (
    id                BIGINT PRIMARY KEY,
    aberto            BOOLEAN NOT NULL DEFAULT FALSE,
    descricao         VARCHAR(120),
    inicio_vigencia   DATE,
    fim_vigencia      DATE,
    id_atualizado_por BIGINT REFERENCES tb_usuario(id),
    data_modificacao  TIMESTAMP
);

INSERT INTO tb_periodo_grade (id, aberto, descricao)
VALUES (1, TRUE, 'Preenchimento da grade bimestral liberado');
