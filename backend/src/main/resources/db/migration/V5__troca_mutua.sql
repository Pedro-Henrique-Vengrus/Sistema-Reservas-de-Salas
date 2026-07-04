-- V5: Troca mutua entre professores - a reserva oferecida pelo proponente
-- Nullable: propostas antigas (anteriores a este modelo) nao tinham reserva oferecida.
-- Toda proposta nova sempre preenche esta coluna (validado na aplicacao).

ALTER TABLE tb_proposta_troca
    ADD COLUMN id_reserva_oferecida BIGINT REFERENCES tb_reserva(id);
