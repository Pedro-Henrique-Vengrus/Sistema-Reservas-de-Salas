-- V3: Seed de dados de teste (credenciais da tela de login: senha = 123)

INSERT INTO tb_curso (nome) VALUES
    ('Ciencia da Computacao'),
    ('Engenharia'),
    ('Administracao');

-- senha "123" em BCrypt
INSERT INTO tb_usuario (nome, email, senha, id_curso, role) VALUES
    ('Pedro', 'pedro@campus.br', '$2b$10$yGgZNTmMLyHsQRHzHussgOI9kFGbmWYClV2KuYbgMv/cwfKNtLa1y', 1, 'PROFESSOR'),
    ('Joao',  'joao@campus.br',  '$2b$10$yGgZNTmMLyHsQRHzHussgOI9kFGbmWYClV2KuYbgMv/cwfKNtLa1y', 1, 'COORDENADOR'),
    ('Administrador', 'admin@campus.br', '$2b$10$yGgZNTmMLyHsQRHzHussgOI9kFGbmWYClV2KuYbgMv/cwfKNtLa1y', NULL, 'ADMIN');

INSERT INTO tb_sala (nome, tipo, capacidade, andar) VALUES
    ('Sala 1001', 'Sala de Aula', 41, '1 Andar'),
    ('Sala 1002', 'Sala de Aula', 57, '1 Andar'),
    ('Lab. Info. 1008', 'Laboratorio', 27, '1 Andar');

-- Visibilidade setorizada: Lab vinculado a Computacao e Engenharia
INSERT INTO tb_sala_curso (id_sala, id_curso) VALUES
    (1, 1), (1, 3),
    (2, 1), (2, 2), (2, 3),
    (3, 1), (3, 2);
