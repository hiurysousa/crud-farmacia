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
- **Funcionario:** especialização de Pessoa que inclui matrícula e cargoFuncionario.
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

- Java 22
- JavaFX SDK 21.0.13 e FXML
- Scene Builder 21
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
6. Execute `application.Main` para testar a conexão JDBC. Para abrir a interface,
   configure e execute `application.FarmaciaApp` conforme a seção abaixo.

Cada integrante deve configurar sua própria conexão. O código e os scripts SQL
são compartilhados pelo GitHub; os dados dos bancos locais são independentes.
Senhas e arquivos de configuração que contenham credenciais não devem ser
incluídos nos commits.

## Interface JavaFX e Scene Builder

O projeto usa Java 22, JavaFX SDK 21.0.13 e arquivos FXML, sem Maven ou Gradle.
Cada integrante deve configurar as bibliotecas e a execução no próprio IntelliJ.

1. Em **File > Project Structure > Project**, selecione o JDK 22. Se necessário,
   adicione o JDK instalado em **Add JDK from Disk**.
2. Em **Modules > Sources**, marque `src` como **Sources** e `resources` como
   **Resources** (ou use **Mark Directory as > Resources Root** na pasta).
   Se o módulo não existir, importe o projeto a partir dos fontes existentes.
3. Baixe o **JavaFX SDK 21.0.13** para seu sistema e arquitetura na
   [página oficial da Gluon](https://gluonhq.com/products/javafx/) e extraia-o
   em uma pasta permanente. JavaFX 27 exige JDK 25 ou superior e não serve para
   esta configuração com JDK 22.
4. Em **Project Structure > Libraries > + > Java**, selecione a pasta `lib` do
   SDK JavaFX e associe a biblioteca ao módulo `farmacia`. Confira também a
   dependência do MySQL Connector/J em **Modules > Dependencies**.
5. Em **Run > Edit Configurations > + > Application**, configure:
   - Nome: `FarmaciaApp`.
   - Classe principal: `application.FarmaciaApp`.
   - Módulo: `farmacia`.
   - JDK/JRE: JDK 22.
   - Em **Modify options > Add VM options**, informe, ajustando o caminho local:

   ```text
   --module-path "E:\Program Files\javafx-sdk-21.0.13\lib" --add-modules javafx.controls,javafx.fxml
   ```

   A biblioteca e o `--module-path` devem apontar para a mesma versão do SDK.
   Essas opções pertencem à configuração **Application**, não ao Maven Runner.
   Configure `DB_PASSWORD` também nessa execução quando a interface acessar o banco.
6. Execute **Build > Rebuild Project** e inicie `FarmaciaApp`. Ao clicar em
   **Testar integração**, a tela deve exibir **JavaFX integrado!**.
7. Para editar a tela visualmente, instale o
   [Scene Builder 21](https://github.com/gluonhq/scenebuilder/releases/tag/21.0.0).
   No IntelliJ, configure **Settings > Languages & Frameworks > JavaFX > Path to
   SceneBuilder** com o executável instalado. Abra `resources/view/principal.fxml`
   por **Open In SceneBuilder**, ou diretamente pelo aplicativo.

Os arquivos da interface ficam em:

```text
src/application/FarmaciaApp.java       # Inicializa a aplicação JavaFX
src/controller/PrincipalController.java # Trata os eventos da tela
resources/view/principal.fxml          # Layout editável no Scene Builder
```

O FXML é carregado pelo caminho `/view/principal.fxml`, relativo à raiz de recursos.
Compartilhe os arquivos Java, FXML e a documentação pelo Git. As pastas `.idea/`,
`out/` e os arquivos `*.iml` são configurações ou saídas locais e estão no
`.gitignore`. O JDK, os SDKs, o Scene Builder e seus caminhos são configurados
separadamente em cada computador. Mantenha senhas apenas na configuração local.

## Banco de dados

O modelo inicial possui 12 tabelas: `pessoa`, `cliente`, `funcionario`,
`farmaceutico`, `produto`, `medicamento`, `cosmetico`, `lote_estoque`, `receita`,
`item_receita`, `venda` e `item_venda`.

Os scripts ficam na pasta `sql`. O script inicial utiliza restrições `CHECK`,
que exigem MySQL 8.0.16 ou superior para serem aplicadas.

## Situação do projeto

Em desenvolvimento. As funcionalidades serão implementadas e verificadas
gradualmente pela dupla.
