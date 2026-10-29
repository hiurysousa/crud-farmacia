-- Requer MySQL 8.0.16 ou superior.
CREATE DATABASE IF NOT EXISTS farmacia
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE farmacia;


-- 1. PESSOA
CREATE TABLE pessoa (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        nome VARCHAR(150) NOT NULL,
                        cpf CHAR(11) NOT NULL,
                        telefone VARCHAR(20),
                        ativo BOOLEAN NOT NULL DEFAULT TRUE,

                        CONSTRAINT uk_pessoa_cpf UNIQUE (cpf),
                        CONSTRAINT ck_pessoa_ativo CHECK (ativo IN (0, 1))
) ENGINE = InnoDB;


-- 2. CLIENTE
-- pessoa_id utiliza o mesmo ID cadastrado em pessoa.
CREATE TABLE cliente (
                         pessoa_id BIGINT PRIMARY KEY,
                         endereco VARCHAR(255),
                         fidelidade_pontos INT NOT NULL DEFAULT 0,

                         CONSTRAINT fk_cliente_pessoa
                             FOREIGN KEY (pessoa_id)
                                 REFERENCES pessoa (id)
                                 ON DELETE RESTRICT ON UPDATE RESTRICT,

                         CONSTRAINT ck_cliente_pontos
                             CHECK (fidelidade_pontos >= 0)
) ENGINE = InnoDB;


-- 3. FUNCIONARIO
CREATE TABLE funcionario (
                             pessoa_id BIGINT PRIMARY KEY,
                             matricula VARCHAR(30) NOT NULL,
                             cargoFuncionario VARCHAR(20) NOT NULL,

                             CONSTRAINT uk_funcionario_matricula UNIQUE (matricula),

                             CONSTRAINT fk_funcionario_pessoa
                                 FOREIGN KEY (pessoa_id)
                                     REFERENCES pessoa (id)
                                     ON DELETE RESTRICT ON UPDATE RESTRICT,

                             CONSTRAINT ck_funcionario_cargo
                                 CHECK (cargoFuncionario IN (
                                                  'ATENDENTE',
                                                  'GERENTE',
                                                  'FARMACEUTICO'
                                     ))
) ENGINE = InnoDB;


-- 4. FARMACEUTICO
CREATE TABLE farmaceutico (
                              funcionario_id BIGINT PRIMARY KEY,
                              crf VARCHAR(30) NOT NULL COMMENT 'Registro com UF, exemplo CE-12345',

                              CONSTRAINT uk_farmaceutico_crf UNIQUE (crf),

                              CONSTRAINT fk_farmaceutico_funcionario
                                  FOREIGN KEY (funcionario_id)
                                      REFERENCES funcionario (pessoa_id)
                                      ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB;


-- 5. PRODUTO
CREATE TABLE produto (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         codigo_barras VARCHAR(50) NOT NULL,
                         nome VARCHAR(150) NOT NULL,
                         fabricante VARCHAR(150) NOT NULL,
                         preco DECIMAL(10, 2) NOT NULL,
                         ativo BOOLEAN NOT NULL DEFAULT TRUE,

                         CONSTRAINT uk_produto_codigo_barras UNIQUE (codigo_barras),
                         CONSTRAINT ck_produto_preco CHECK (preco >= 0),
                         CONSTRAINT ck_produto_ativo CHECK (ativo IN (0, 1)),

                         INDEX idx_produto_nome (nome)
) ENGINE = InnoDB;


-- 6. MEDICAMENTO
CREATE TABLE medicamento (
                             produto_id BIGINT PRIMARY KEY,
                             dosagem VARCHAR(100),
                             principio_ativo VARCHAR(200) NOT NULL,
                             tipo VARCHAR(30) NOT NULL,
                             categoria_regulatoria VARCHAR(30) NOT NULL,
                             tipo_uso VARCHAR(20),
                             registro_anvisa VARCHAR(30),

                             CONSTRAINT fk_medicamento_produto
                                 FOREIGN KEY (produto_id)
                                     REFERENCES produto (id)
                                     ON DELETE RESTRICT ON UPDATE RESTRICT,

                             CONSTRAINT ck_medicamento_tipo
                                 CHECK (tipo IN (
                                                 'ANTIBIOTICO',
                                                 'ANTIINFLAMATORIO',
                                                 'OUTRO'
                                     )),

                             CONSTRAINT ck_medicamento_categoria
                                 CHECK (categoria_regulatoria IN (
                                                                  'LIVRE',
                                                                  'RECEITA_SIMPLES',
                                                                  'RETENCAO',
                                                                  'CONTROLADO'
                                     )),

                             CONSTRAINT ck_medicamento_uso
                                 CHECK (tipo_uso IN (
                                                     'ORAL',
                                                     'INJETAVEL',
                                                     'TOPICO'
                                     )),

                             INDEX idx_medicamento_principio_ativo (principio_ativo)
) ENGINE = InnoDB;


-- 7. COSMETICO
CREATE TABLE cosmetico (
                           produto_id BIGINT PRIMARY KEY,
                           categoria_uso VARCHAR(20) NOT NULL,
                           hipoalergenico BOOLEAN NOT NULL DEFAULT FALSE,

                           CONSTRAINT fk_cosmetico_produto
                               FOREIGN KEY (produto_id)
                                   REFERENCES produto (id)
                                   ON DELETE RESTRICT ON UPDATE RESTRICT,

                           CONSTRAINT ck_cosmetico_categoria
                               CHECK (categoria_uso IN (
                                                        'CABELO',
                                                        'PELE',
                                                        'HIGIENE',
                                                        'PERFUMARIA'
                                   )),

                           CONSTRAINT ck_cosmetico_hipoalergenico
                               CHECK (hipoalergenico IN (0, 1))
) ENGINE = InnoDB;


-- 8. LOTE_ESTOQUE
-- ativo permite bloquear lotes conforme a regra do guia.
CREATE TABLE lote_estoque (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              produto_id BIGINT NOT NULL,
                              numero_lote VARCHAR(50) NOT NULL,
                              data_validade DATE NOT NULL,
                              quantidade INT NOT NULL DEFAULT 0,
                              ativo BOOLEAN NOT NULL DEFAULT TRUE,

                              CONSTRAINT uk_lote_produto_numero
                                  UNIQUE (produto_id, numero_lote),

    -- Utilizada pela chave composta de item_venda.
                              CONSTRAINT uk_lote_id_produto
                                  UNIQUE (id, produto_id),

                              CONSTRAINT fk_lote_produto
                                  FOREIGN KEY (produto_id)
                                      REFERENCES produto (id)
                                      ON DELETE RESTRICT ON UPDATE RESTRICT,

                              CONSTRAINT ck_lote_quantidade CHECK (quantidade >= 0),
                              CONSTRAINT ck_lote_ativo CHECK (ativo IN (0, 1)),

                              INDEX idx_lote_validade (data_validade)
) ENGINE = InnoDB;


-- 9. RECEITA
CREATE TABLE receita (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         cliente_id BIGINT NOT NULL,
                         numero VARCHAR(50) NOT NULL,
                         data_emissao DATE NOT NULL,
                         nome_medico VARCHAR(150) NOT NULL,
                         crm_medico VARCHAR(30) NOT NULL COMMENT 'Registro com UF',
                         retida BOOLEAN NOT NULL DEFAULT FALSE,

                         CONSTRAINT fk_receita_cliente
                             FOREIGN KEY (cliente_id)
                                 REFERENCES cliente (pessoa_id)
                                 ON DELETE RESTRICT ON UPDATE RESTRICT,

                         CONSTRAINT ck_receita_retida CHECK (retida IN (0, 1)),

                         INDEX idx_receita_cliente (cliente_id),
                         INDEX idx_receita_numero (numero)
) ENGINE = InnoDB;


-- 10. ITEM_RECEITA
CREATE TABLE item_receita (
                              receita_id BIGINT NOT NULL,
                              medicamento_id BIGINT NOT NULL,
                              quantidade_prescrita INT NOT NULL,

                              PRIMARY KEY (receita_id, medicamento_id),

                              CONSTRAINT fk_item_receita_receita
                                  FOREIGN KEY (receita_id)
                                      REFERENCES receita (id)
                                      ON DELETE RESTRICT ON UPDATE RESTRICT,

                              CONSTRAINT fk_item_receita_medicamento
                                  FOREIGN KEY (medicamento_id)
                                      REFERENCES medicamento (produto_id)
                                      ON DELETE RESTRICT ON UPDATE RESTRICT,

                              CONSTRAINT ck_item_receita_quantidade
                                  CHECK (quantidade_prescrita > 0)
) ENGINE = InnoDB;


-- 11. VENDA
-- Complementos ao quadro do guia:
-- receita_id registra a receita utilizada (no maximo uma por venda).
-- farmaceutico_id registra o responsavel quando necessario.
CREATE TABLE venda (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       data_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       cliente_id BIGINT,
                       funcionario_id BIGINT NOT NULL,
                       receita_id BIGINT,
                       farmaceutico_id BIGINT,
                       status VARCHAR(20) NOT NULL DEFAULT 'EM_ABERTO',
                       valor_total DECIMAL(14, 2) NOT NULL DEFAULT 0.00,

                       CONSTRAINT fk_venda_cliente
                           FOREIGN KEY (cliente_id)
                               REFERENCES cliente (pessoa_id)
                               ON DELETE RESTRICT ON UPDATE RESTRICT,

                       CONSTRAINT fk_venda_funcionario
                           FOREIGN KEY (funcionario_id)
                               REFERENCES funcionario (pessoa_id)
                               ON DELETE RESTRICT ON UPDATE RESTRICT,

                       CONSTRAINT fk_venda_receita
                           FOREIGN KEY (receita_id)
                               REFERENCES receita (id)
                               ON DELETE RESTRICT ON UPDATE RESTRICT,

                       CONSTRAINT fk_venda_farmaceutico
                           FOREIGN KEY (farmaceutico_id)
                               REFERENCES farmaceutico (funcionario_id)
                               ON DELETE RESTRICT ON UPDATE RESTRICT,

                       CONSTRAINT ck_venda_cliente_receita
                           CHECK (receita_id IS NULL OR cliente_id IS NOT NULL),

                       CONSTRAINT ck_venda_status
                           CHECK (status IN (
                                             'EM_ABERTO',
                                             'CONCLUIDA',
                                             'CANCELADA'
                               )),

                       CONSTRAINT ck_venda_total CHECK (valor_total >= 0),

                       INDEX idx_venda_data_hora (data_hora)
) ENGINE = InnoDB;


-- 12. ITEM_VENDA
CREATE TABLE item_venda (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            venda_id BIGINT NOT NULL,
                            produto_id BIGINT NOT NULL,
                            lote_id BIGINT NOT NULL,
                            quantidade INT NOT NULL,
                            valor_unitario DECIMAL(10, 2) NOT NULL,
                            subtotal DECIMAL(14, 2) NOT NULL,

                            CONSTRAINT fk_item_venda_venda
                                FOREIGN KEY (venda_id)
                                    REFERENCES venda (id)
                                    ON DELETE RESTRICT ON UPDATE RESTRICT,

                            CONSTRAINT fk_item_venda_produto
                                FOREIGN KEY (produto_id)
                                    REFERENCES produto (id)
                                    ON DELETE RESTRICT ON UPDATE RESTRICT,

    -- Garante que o lote pertence ao produto informado.
                            CONSTRAINT fk_item_venda_lote_produto
                                FOREIGN KEY (lote_id, produto_id)
                                    REFERENCES lote_estoque (id, produto_id)
                                    ON DELETE RESTRICT ON UPDATE RESTRICT,

                            CONSTRAINT ck_item_venda_quantidade
                                CHECK (quantidade > 0),

                            CONSTRAINT ck_item_venda_valor
                                CHECK (valor_unitario >= 0),

                            CONSTRAINT ck_item_venda_subtotal
                                CHECK (
                                    subtotal >= 0
                                        AND subtotal = quantidade * valor_unitario
                                    )
) ENGINE = InnoDB;