-- V6: Renomeia a role ADMIN para GESTOR (alinhamento com a documentacao do projeto)

UPDATE tb_usuario SET role = 'GESTOR' WHERE role = 'ADMIN';
