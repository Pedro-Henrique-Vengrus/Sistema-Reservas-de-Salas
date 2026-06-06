-- V1: Schema base - usuario, curso, sala e vinculo N:N (visibilidade setorizada)

CREATE TABLE tb_curso (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(100) NOT NULL,
    data_criacao    TIMESTAMP NOT NULL DEFAULT now(),
    data_modificacao TIMESTAMP,
    data_desativacao TIMESTAMP,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE tb_usuario (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    senha           VARCHAR(255) NOT NULL,
    id_curso        BIGINT REFERENCES tb_curso(id),
    role            VARCHAR(20) NOT NULL,
    data_criacao    TIMESTAMP NOT NULL DEFAULT now(),
    data_modificacao TIMESTAMP,
    data_desativacao TIMESTAMP,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE tb_sala (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(100) NOT NULL,
    tipo            VARCHAR(50),
    capacidade      INT NOT NULL DEFAULT 0,
    andar           VARCHAR(50),
    situacao        VARCHAR(20) NOT NULL DEFAULT 'ATIVA',
    data_criacao    TIMESTAMP NOT NULL DEFAULT now(),
    data_modificacao TIMESTAMP,
    data_desativacao TIMESTAMP,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE
);

-- Vinculo N:N: uma sala pode pertencer ao escopo de varios cursos
CREATE TABLE tb_sala_curso (
    id_sala     BIGINT NOT NULL REFERENCES tb_sala(id) ON DELETE CASCADE,
    id_curso    BIGINT NOT NULL REFERENCES tb_curso(id) ON DELETE CASCADE,
    PRIMARY KEY (id_sala, id_curso)
);

CREATE INDEX idx_usuario_email ON tb_usuario(email);
CREATE INDEX idx_usuario_curso ON tb_usuario(id_curso);
CREATE INDEX idx_sala_curso_curso ON tb_sala_curso(id_curso);
