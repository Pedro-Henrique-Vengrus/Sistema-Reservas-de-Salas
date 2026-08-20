-- V12: Completa o seed com os dados que as novas regras exigem
-- (siglas, codigos, reitor, vinculos N:N e reservas futuras para demonstrar troca e moderacao).

UPDATE tb_curso SET sigla = 'CC'    WHERE nome = 'Ciencia da Computacao';
UPDATE tb_curso SET sigla = 'ENG'   WHERE nome = 'Engenharia';
UPDATE tb_curso SET sigla = 'ADM'   WHERE nome = 'Administracao';

UPDATE tb_sala SET codigo = '1001' WHERE nome = 'Sala 1001';
UPDATE tb_sala SET codigo = '1002' WHERE nome = 'Sala 1002';
UPDATE tb_sala SET codigo = '1008' WHERE nome = 'Lab. Info. 1008';

-- Reitor: administra o sistema e tambem solicita reservas proprias.
-- Carla: professora de Engenharia apenas, usada para demonstrar a visibilidade setorizada.
-- senha "123" em BCrypt (mesma dos demais usuarios de teste)
INSERT INTO tb_usuario (nome, email, senha, role) VALUES
    ('Marta Reitora', 'reitor@campus.br', '$2b$10$yGgZNTmMLyHsQRHzHussgOI9kFGbmWYClV2KuYbgMv/cwfKNtLa1y', 'REITOR'),
    ('Carla Souza',   'carla@campus.br',  '$2b$10$yGgZNTmMLyHsQRHzHussgOI9kFGbmWYClV2KuYbgMv/cwfKNtLa1y', 'PROFESSOR');

INSERT INTO tb_usuario_curso (id_usuario, id_curso)
SELECT u.id, c.id FROM tb_usuario u, tb_curso c
 WHERE (u.email = 'reitor@campus.br' AND c.nome IN ('Ciencia da Computacao', 'Engenharia'))
    OR (u.email = 'carla@campus.br'  AND c.nome = 'Engenharia')
    OR (u.email = 'joao@campus.br'   AND c.nome = 'Engenharia')
ON CONFLICT DO NOTHING;

-- Reservas futuras no MESMO dia e MESMO turno (noturno): pre-requisito da proposta de troca.
INSERT INTO tb_reserva (id_solicitante, id_sala, data_reserva, hora_inicio, hora_fim, turno, tipo_reserva, status)
SELECT u.id, s.id, CURRENT_DATE + 7, TIME '19:00', TIME '21:00', 'NOTURNO', 'GRADE_BIMESTRAL', 'APROVADA'
  FROM tb_usuario u, tb_sala s WHERE u.email = 'pedro@campus.br' AND s.nome = 'Sala 1002';

INSERT INTO tb_reserva (id_solicitante, id_sala, data_reserva, hora_inicio, hora_fim, turno, tipo_reserva, status)
SELECT u.id, s.id, CURRENT_DATE + 7, TIME '20:00', TIME '22:00', 'NOTURNO', 'GRADE_BIMESTRAL', 'APROVADA'
  FROM tb_usuario u, tb_sala s WHERE u.email = 'joao@campus.br' AND s.nome = 'Lab. Info. 1008';

-- Solicitacao de ultima hora aguardando moderacao no painel administrativo
INSERT INTO tb_reserva (id_solicitante, id_sala, data_reserva, hora_inicio, hora_fim, turno, tipo_reserva, status, observacao)
SELECT u.id, s.id, CURRENT_DATE + 2, TIME '14:00', TIME '16:00', 'VESPERTINO', 'ULTIMA_HORA', 'PENDENTE_APROVACAO',
       'Palestra extra da semana academica'
  FROM tb_usuario u, tb_sala s WHERE u.email = 'pedro@campus.br' AND s.nome = 'Sala 1001';
