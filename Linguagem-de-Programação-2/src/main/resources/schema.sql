CREATE TABLE IF NOT EXISTS tb_cad_cliente (
 codigo INTEGER PRIMARY KEY AUTOINCREMENT,
 nome TEXT NOT NULL CHECK(length(trim(nome)) > 0),
 documento TEXT NOT NULL UNIQUE,
 email TEXT NOT NULL,
 telefone TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS tb_cad_tecnico (
 codigo INTEGER PRIMARY KEY AUTOINCREMENT,
 nome TEXT NOT NULL,
 email TEXT NOT NULL UNIQUE,
 especialidade TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS tb_cad_categoria_servico (
 codigo INTEGER PRIMARY KEY AUTOINCREMENT,
 nome TEXT NOT NULL UNIQUE,
 descricao TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS tb_cad_tipo_servico (
 codigo INTEGER PRIMARY KEY AUTOINCREMENT,
 codigo_categoria INTEGER NOT NULL REFERENCES tb_cad_categoria_servico(codigo) ON DELETE RESTRICT,
 nome TEXT NOT NULL,
 descricao TEXT NOT NULL,
 UNIQUE(codigo_categoria, nome)
);
CREATE TABLE IF NOT EXISTS tb_ord_servico (
 codigo INTEGER PRIMARY KEY AUTOINCREMENT,
 codigo_cliente INTEGER NOT NULL REFERENCES tb_cad_cliente(codigo) ON DELETE RESTRICT,
 codigo_tipo_servico INTEGER NOT NULL REFERENCES tb_cad_tipo_servico(codigo) ON DELETE RESTRICT,
 titulo TEXT NOT NULL,
 descricao TEXT NOT NULL,
 prioridade TEXT NOT NULL CHECK(prioridade IN ('BAIXA','MEDIA','ALTA')),
 status TEXT NOT NULL DEFAULT 'ABERTA' CHECK(status IN ('ABERTA','EM_ANDAMENTO','CONCLUIDA','CANCELADA')),
 data_abertura TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS tb_ord_atividade (
 codigo INTEGER PRIMARY KEY AUTOINCREMENT,
 codigo_ordem INTEGER NOT NULL REFERENCES tb_ord_servico(codigo) ON DELETE CASCADE,
 descricao TEXT NOT NULL,
 status TEXT NOT NULL DEFAULT 'PENDENTE' CHECK(status IN ('PENDENTE','EM_EXECUCAO','CONCLUIDA')),
 data_conclusao TEXT,
 CHECK((status = 'CONCLUIDA' AND data_conclusao IS NOT NULL) OR
       (status <> 'CONCLUIDA' AND data_conclusao IS NULL))
);
CREATE TABLE IF NOT EXISTS tb_ord_tecnico (
 codigo_ordem INTEGER NOT NULL REFERENCES tb_ord_servico(codigo) ON DELETE CASCADE,
 codigo_tecnico INTEGER NOT NULL REFERENCES tb_cad_tecnico(codigo) ON DELETE RESTRICT,
 PRIMARY KEY(codigo_ordem, codigo_tecnico)
);
CREATE INDEX IF NOT EXISTS idx_tb_cad_tipo_servico_categoria ON tb_cad_tipo_servico(codigo_categoria);
CREATE INDEX IF NOT EXISTS idx_tb_ord_servico_cliente ON tb_ord_servico(codigo_cliente);
CREATE INDEX IF NOT EXISTS idx_tb_ord_servico_tipo ON tb_ord_servico(codigo_tipo_servico);
CREATE INDEX IF NOT EXISTS idx_tb_ord_atividade_ordem ON tb_ord_atividade(codigo_ordem);
CREATE INDEX IF NOT EXISTS idx_tb_ord_tecnico_tecnico ON tb_ord_tecnico(codigo_tecnico);

CREATE TABLE IF NOT EXISTS tb_cad_tecnico_tipo_servico (
 codigo_tecnico INTEGER NOT NULL REFERENCES tb_cad_tecnico(codigo) ON DELETE CASCADE,
 codigo_tipo_servico INTEGER NOT NULL REFERENCES tb_cad_tipo_servico(codigo) ON DELETE CASCADE,
 PRIMARY KEY(codigo_tecnico, codigo_tipo_servico)
);
CREATE INDEX IF NOT EXISTS idx_tb_cad_tecnico_tipo_servico_tipo ON tb_cad_tecnico_tipo_servico(codigo_tipo_servico);
