-- V7: Alinha os perfis as regras oficiais -> PROFESSOR, REITOR, ADMIN.
-- GESTOR era o nome interno do administrador; COORDENADOR nao existe no modelo oficial
-- e passa a ser PROFESSOR (continua solicitante, apenas sem privilegio extra).

UPDATE tb_usuario SET role = 'ADMIN'     WHERE role = 'GESTOR';
UPDATE tb_usuario SET role = 'PROFESSOR' WHERE role = 'COORDENADOR';

ALTER TABLE tb_usuario
    ADD CONSTRAINT ck_usuario_role CHECK (role IN ('PROFESSOR', 'REITOR', 'ADMIN'));
