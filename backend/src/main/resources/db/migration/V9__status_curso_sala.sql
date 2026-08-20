-- V9: Ciclo de vida unificado (ATIVO/INATIVO) para Curso, Sala e Usuario,
-- alem de sigla do curso e tipo catalogado do ambiente.
-- Antes existiam dois campos concorrentes na sala (ativo booleano + situacao textual).

-- ---------- Curso ----------
ALTER TABLE tb_curso ADD COLUMN sigla  VARCHAR(20);
ALTER TABLE tb_curso ADD COLUMN status VARCHAR(20);

UPDATE tb_curso SET status = CASE WHEN ativo THEN 'ATIVO' ELSE 'INATIVO' END;

ALTER TABLE tb_curso ALTER COLUMN status SET NOT NULL;
ALTER TABLE tb_curso ALTER COLUMN status SET DEFAULT 'ATIVO';
ALTER TABLE tb_curso ADD CONSTRAINT ck_curso_status CHECK (status IN ('ATIVO', 'INATIVO'));
ALTER TABLE tb_curso DROP COLUMN ativo;

-- ---------- Usuario ----------
ALTER TABLE tb_usuario ADD COLUMN status VARCHAR(20);
UPDATE tb_usuario SET status = CASE WHEN ativo THEN 'ATIVO' ELSE 'INATIVO' END;
ALTER TABLE tb_usuario ALTER COLUMN status SET NOT NULL;
ALTER TABLE tb_usuario ALTER COLUMN status SET DEFAULT 'ATIVO';
ALTER TABLE tb_usuario ADD CONSTRAINT ck_usuario_status CHECK (status IN ('ATIVO', 'INATIVO'));
ALTER TABLE tb_usuario DROP COLUMN ativo;

-- ---------- Sala ----------
ALTER TABLE tb_sala ADD COLUMN codigo VARCHAR(30);
ALTER TABLE tb_sala ADD COLUMN status VARCHAR(20);

-- 'ativo' e 'situacao' podiam divergir; INATIVO vence se qualquer um dos dois indicar baixa
UPDATE tb_sala
   SET status = CASE WHEN ativo AND situacao = 'ATIVA' THEN 'ATIVO' ELSE 'INATIVO' END;

ALTER TABLE tb_sala ALTER COLUMN status SET NOT NULL;
ALTER TABLE tb_sala ALTER COLUMN status SET DEFAULT 'ATIVO';
ALTER TABLE tb_sala ADD CONSTRAINT ck_sala_status CHECK (status IN ('ATIVO', 'INATIVO'));
ALTER TABLE tb_sala DROP COLUMN ativo;
ALTER TABLE tb_sala DROP COLUMN situacao;

-- tipo textual livre -> catalogo TipoAmbiente
UPDATE tb_sala SET tipo = CASE
    WHEN tipo IS NULL                     THEN 'SALA_AULA'
    WHEN lower(tipo) LIKE '%sala%'        THEN 'SALA_AULA'
    WHEN lower(tipo) LIKE '%lab%'         THEN 'LAB_INFORMATICA'
    WHEN lower(tipo) LIKE '%audit%'       THEN 'AUDITORIO'
    ELSE 'OUTRO'
END;

ALTER TABLE tb_sala ALTER COLUMN tipo SET DEFAULT 'SALA_AULA';
ALTER TABLE tb_sala ALTER COLUMN tipo SET NOT NULL;
ALTER TABLE tb_sala ADD CONSTRAINT ck_sala_tipo
    CHECK (tipo IN ('SALA_AULA', 'LAB_INFORMATICA', 'LAB_CIENCIAS', 'AUDITORIO', 'OUTRO'));

-- Codigo inicial: reaproveita o nome ja cadastrado
UPDATE tb_sala SET codigo = nome WHERE codigo IS NULL;
