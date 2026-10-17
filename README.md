# Sistema de Farmácia

Projeto acadêmico em desenvolvimento para praticar programação orientada a
objetos em Java e integração com banco de dados MySQL por meio do JDBC.

## Integrantes

- Hiury Sousa
- Davi Witalo

## Objetivo

Desenvolver um sistema para auxiliar no gerenciamento de uma farmácia.
A proposta contempla:

- Cadastro de clientes, funcionários e farmacêuticos.
- Cadastro de medicamentos e cosméticos.
- Controle de estoque por lote e data de validade.
- Registro de receitas médicas.
- Registro de vendas e seus itens.

## Entidades do sistema

O modelo do projeto prevê as seguintes entidades:

- **Pessoa:** classe abstrata com os dados comuns de clientes e funcionários,
  como nome, CPF e telefone.
- **Cliente:** especialização de Pessoa que inclui endereço e pontos de fidelidade.
- **Funcionario:** especialização de Pessoa que inclui matrícula e cargo.
- **Farmaceutico:** especialização de Funcionario que inclui o registro no CRF.
- **Produto:** classe abstrata com os dados comuns dos produtos, como código de
  barras, nome, fabricante e preço.
- **Medicamento:** especialização de Produto com dosagem, princípio ativo,
  classificação e categoria regulatória.
- **Cosmetico:** especialização de Produto com categoria de uso e indicação
  de ser hipoalergênico.
- **LoteEstoque:** identifica um lote de determinado produto e controla sua
  quantidade, validade e situação ativa.
- **Receita:** registra a prescrição de um cliente, incluindo número, data de
  emissão, médico responsável e situação de retenção.
- **ItemReceita:** relaciona uma receita a um medicamento e informa a
  quantidade prescrita.
- **Venda:** registra o atendimento, com data, funcionário, cliente quando
  identificado, situação e valor total.
- **ItemVenda:** registra o produto e o lote vendidos, a quantidade, o preço
  unitário praticado e o subtotal.

## Tecnologias e ferramentas

- Java
- MySQL
- JDBC e MySQL Connector/J
- IntelliJ IDEA
- MySQL Workbench
- Git e GitHub

## Configuração inicial

1. Clone o repositório e abra o projeto no IntelliJ IDEA.
2. Configure o JDK do projeto e adicione o MySQL Connector/J às dependências.
3. Com o MySQL Server iniciado, abra o arquivo `sql/001_estrutura.sql` no
   Workbench e execute os comandos para criar o banco `farmacia` e suas tabelas.
   Esse script é destinado à instalação inicial em um banco sem essas tabelas.
4. Na classe `db.DataBase`, confira a URL de conexão e o usuário do MySQL.
   Para o banco local, a URL é `jdbc:mysql://localhost:3306/farmacia`.
5. Na configuração de execução do IntelliJ, adicione a variável de ambiente
   `DB_PASSWORD` com a senha do seu MySQL. A classe `DataBase` deve ler esse
   valor por meio de `System.getenv("DB_PASSWORD")`.
6. Execute a classe `application.Main` para iniciar o programa.

Cada integrante deve configurar sua própria conexão. O código e os scripts SQL
são compartilhados pelo GitHub; os dados dos bancos locais são independentes.
Senhas e arquivos de configuração que contenham credenciais não devem ser
incluídos nos commits.

## Banco de dados

O modelo inicial possui 12 tabelas: `pessoa`, `cliente`, `funcionario`,
`farmaceutico`, `produto`, `medicamento`, `cosmetico`, `lote_estoque`, `receita`,
`item_receita`, `venda` e `item_venda`.

Os scripts ficam na pasta `sql`. O script inicial utiliza restrições `CHECK`,
que exigem MySQL 8.0.16 ou superior para serem aplicadas.

## Situação do projeto

Em desenvolvimento. As funcionalidades serão implementadas e verificadas
gradualmente pela dupla.
