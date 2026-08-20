-- V8: Usuario <-> Curso passa de 1:N para N:N (visibilidade setorizada por multiplos cursos)

CREATE TABLE tb_usuario_curso (
    id_usuario  BIGINT NOT NULL REFERENCES tb_usuario(id) ON DELETE CASCADE,
    id_curso    BIGINT NOT NULL REFERENCES tb_curso(id) ON DELETE CASCADE,
    PRIMARY KEY (id_usuario, id_curso)
);

-- Preserva os vinculos existentes antes de remover a FK unica
INSERT INTO tb_usuario_curso (id_usuario, id_curso)
SELECT id, id_curso FROM tb_usuario WHERE id_curso IS NOT NULL;

DROP INDEX IF EXISTS idx_usuario_curso;
ALTER TABLE tb_usuario DROP COLUMN id_curso;

CREATE INDEX idx_usuario_curso_curso ON tb_usuario_curso(id_curso);
CREATE INDEX idx_usuario_curso_usuario ON tb_usuario_curso(id_usuario);
