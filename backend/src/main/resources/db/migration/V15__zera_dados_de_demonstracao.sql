-- V15: Zera a base para operacao real.
--
-- Ate aqui o banco vinha com os dados de demonstracao semeados em V3, V4, V8 e V12
-- (tres cursos, tres salas, cinco usuarios e reservas de exemplo). Eles serviram para
-- exercitar as regras durante o desenvolvimento; para uso real o Admin monta tudo pelo painel.
--
-- Migrations ja aplicadas nao podem ser editadas -- o Flyway guarda o checksum de cada uma e
-- alterar V3 quebraria a validacao em qualquer banco existente. Por isso a limpeza vem aqui,
-- como um passo novo. O efeito final e o mesmo nos dois casos: banco recem-criado (que aplica
-- V1..V15 em sequencia) ou banco que ja estava rodando.
--
-- Sobrevivem exatamente duas coisas: as contas de Admin (senao ninguem entra) e a linha unica
-- de tb_periodo_grade, que a aplicacao espera encontrar sempre.

-- A ordem respeita as chaves estrangeiras: filhos antes dos pais.
DELETE FROM tb_proposta_troca;
DELETE FROM tb_notificacao;
DELETE FROM tb_reserva;
DELETE FROM tb_sala_curso;
DELETE FROM tb_usuario_curso;
DELETE FROM tb_sala;
DELETE FROM tb_curso;

-- Solta a referencia antes de remover os usuarios de demonstracao.
UPDATE tb_periodo_grade SET id_atualizado_por = NULL;

DELETE FROM tb_usuario WHERE role <> 'ADMIN';

-- Rede de seguranca: se a base ficou sem nenhum administrador, recria o acesso padrao
-- (admin@campus.br / senha 123). Nao roda se ja existir algum Admin.
INSERT INTO tb_usuario (nome, email, senha, role)
SELECT 'Administrador', 'admin@campus.br',
       '$2b$10$yGgZNTmMLyHsQRHzHussgOI9kFGbmWYClV2KuYbgMv/cwfKNtLa1y', 'ADMIN'
 WHERE NOT EXISTS (SELECT 1 FROM tb_usuario WHERE role = 'ADMIN');

-- Grade fechada: e o Admin quem libera o preenchimento bimestral quando o edital abrir.
UPDATE tb_periodo_grade
   SET aberto = FALSE, descricao = NULL,
       inicio_vigencia = NULL, fim_vigencia = NULL, data_modificacao = NULL
 WHERE id = 1;

-- Numeracao volta a comecar do 1 nas tabelas esvaziadas; em tb_usuario continua depois do Admin.
DO $$
DECLARE
    tabela text;
BEGIN
    FOREACH tabela IN ARRAY ARRAY['tb_curso', 'tb_sala', 'tb_reserva',
                                  'tb_proposta_troca', 'tb_notificacao', 'tb_usuario']
    LOOP
        EXECUTE format(
            'SELECT setval(pg_get_serial_sequence(%L, ''id''), COALESCE((SELECT MAX(id) FROM %I), 0) + 1, false)',
            tabela, tabela);
    END LOOP;
END $$;
