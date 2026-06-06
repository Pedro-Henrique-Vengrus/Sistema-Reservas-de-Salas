-- V4: Reservas de exemplo para popular a agenda e permitir testar troca

-- Pedro (id 1) tem uma reserva; Joao (id 2) tem outra na mesma sala
INSERT INTO tb_reserva (id_solicitante, id_sala, data_reserva, hora_inicio, hora_fim, tipo_reserva, status) VALUES
    (1, 1, '2026-03-20', '17:00', '19:00', 'GRADE_BIMESTRAL', 'CONFIRMADA'),
    (1, 1, '2026-03-21', '19:00', '21:00', 'GRADE_BIMESTRAL', 'CONFIRMADA'),
    (2, 1, '2026-03-20', '19:00', '21:00', 'ULTIMA_HORA', 'CONFIRMADA');
