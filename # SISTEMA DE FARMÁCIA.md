# SISTEMA DE FARMÁCIA

Guia de modelagem e implementação de um CRUD desktop Java + JavaFX + JDBC + MySQL

Finalidade: trabalho acadêmico de Linguagem de Programacao II Formato: roteiro passo a passo, sem código pronto Versão: 1.0 - outubro de 2026

Autores: Professor(a):

## Como usar este guia

Este documento transforma a ideia inicial em um plano de projeto executavel. Ele não entrega classes, telas ou comandos SQL prontos. Em vez disso, define o que deve existir, por que cada parte existe, em qual ordem deve ser construída e como verificar cada etapa.

## Leitura recomendada

Primeiro fechem o escopo e a modelagem. Depois criem e validem o banco. Em seguida implementem dominio, persistencia e servicos. A interface JavaFX vem por ultimo, conectando casos de uso que ja foram testados.

## Mapa do documento

| Parte | Conteúdo | Resultado esperado |
|---|---|---|
| 1-3 | Escopo, avaliacao da proposta e decisoes de modelagem | Modelo aceito pelo grupo e pelo professor |
| 4-6 | UML, regras de negocio e banco MySQL | Diagrama e esquema relacional coerentes |
| 7-9 | Arquitetura, classes e patterns | Responsabilidades separadas |
| 10-12 | Preparacao do ambiente, MySQL e IntelliJ | Projeto vazio executavel e banco acessivel |
| 13-16 | JavaFX, venda transacional, roteiro de implementacao e testes | Aplicacao integrada por incrementos |
| 17-19 | Escalabilidade, riscos e entrega | Trabalho reproduzivel em outra maquina |

## Aviso de contexto

O sistema descrito é didático. As regras de receita, retenção e responsabilidade farmaceutica devem ser tratadas como requisitos simplificados do trabalho. Um sistema comercial real exigiria validação jurídica e sanitaria atualizada.

## 1 Proposta original

A proposta recebida contém os seguintes elementos:

### Pessoa

- Pessoa (superclasse): id, nome, cpf e telefone.
- Cliente: endereço e fidelidade_pontos.
- Funcionario / Farmaceutico: matricula, crf quando aplicavel e cargoFuncionario.

### Produtos

- Produto (superclasse): id, codigo_barras, nome, fabricante, preço e quantidade_estoque.
- Cosmetico / Perfumaria: categoria_uso (cabelo ou pele) e hipoalergênico.
- Medicamento: dosagem, principio_ativo e exige_receita.
- Antibiotico: necessita_retencao_receita e medico_prescrtor.
- Antiinflamatorio: tipo_uso (oral, injetável ou tópico).
- Medicamento controlado: numero_lote e registro_anvisa.

### Venda

- Venda / Pedido: id, data_hora, cliente, funcionario e valor_total.
- ItemVenda: venda_id, produto_id, quantidade, valor_unitario e subtotal.

### Diagnostico

A base é suficiente para iniciar um CRUD acadêmico. Ela já oferece cadastros, herança e uma operação central de venda. Antes da implementação, porém, é preciso corrigir a localizacao de receita, lote e CRF, além de definir transações e histórico.

## 2 Escopo recomendado

### O que o sistema deve fazer

- Cadastrar, consultar, editar e desativar clientes.
- Cadastrar, consultar, editar e desativar funcionarios e farmacêuticos.
- Cadastrar medicamentos, cosmetics e seus lotes de estoque.
- Registrar receitas de forma simplificada.
- Montar uma venda, validar restrições, calcular valores e baixar estoque.
- Consultar vendas e cancelar uma venda preservando o histórico.
- Pesquisar produtos por nome, código de barras ou princípio ativo.

### Fora do escopo inicial

- Nota fiscal e integração tributaria.
- Pagamentos reais, convenios, PBM e programas externos.
- Integração com Anvisa ou servicos de prescrição eletronica.
- Compras, fornecedores e previsao automatica de reposicao.
- Múltiplas filiais, sincronizacao em nuvem e operação offline.
- Leitor físico, impressora fiscal e controle contabil.

### Regra de ouro do escopo

O objetivo é demonstrar modelagem, persistencia, interface desktop e dois patterns. Não tentem reproduzir uma farmácia comercial inteira. Se uma funcionalidade não ajuda a demonstrar esses objetivos, coloquem-na na lista de evoluções.

### Definição de pronto do projeto

O MVP está pronto quando os CRUDs principais funcionam, uma venda válida reduz o estoque em uma única transação, uma venda inválida é rejeitada sem alteracoes parciais, e o projeto pode ser instalado por outra pessoa seguindo a documentação.

## 3 Correcoes e decisões de modelagem

| Ponto inicial | Decisão recomendada | Motivo |
|---|---|---|
| Farmaceutico misturado a Funcionario | Criar Pessoa -> Funcionario -> Farmaceutico | CRF existe somente no subtipo correto. |
| Médico prescritor no Antibiotico | Mover prescritor para Receita | O médico muda a cada prescrição, não a cada produto. |
| Lote apenas no controlado | Criar LoteEstoque para qualquer produto | Um produto pode chegar em varios lotes e validades. |
| Quantidade no Produto | Preferir soma dos lotes | Evita duas fontes de verdade para o estoque. |
| Subclasses de cada tipo de remedio | Usar enums e Strategy | Classificações podem se sobrepor e mudar. |
| Exclusão física | Adicionar ativo e status | Histórico de venda não pode perder referencias. |
| Total informado pela tela | Calcular no servicio | A interface não é fonte confiavel. |

### Herança que vale a pena manter

- Pessoa -> Cliente.
- Pessoa -> Funcionario -> Farmaceutico.
- Produto -> Medicamento e Produto -> Cosmetico.

### Classificação que deve ficar flexível

Antibiotico, antiinflamatorio e controlado funcionam melhor como classificações ou características de Medicamento. O comportamento de venda é escolhido por uma Strategy. Se a disciplina exigir essas subclasses, elas podem ser mantidas apenas para demonstracao, mas Receita e LoteEstoque continuam separados.

### Decisão a confirmar com o professor

Verifiquem se o segundo pattern foi definido pela disciplina e se as subclasses de medicamentos sao obrigatórias. Neste guia, DAO e Strategy sao a combinacao recomendada; MVC aparece como organização arquitetural do JavaFX.

## 4 Modelo de dominio proposto

A figura abaixo apresenta o núcleo recomendado. Ela privilegia clareza e crescimento controlado, sem transformar o trabalho em um sistema comercial.

[Imagem: Diagrama UML simplificado da modelagem revisada, mostrando classes Pessoa, Cliente, Funcionario, Farmaceutico, Produto, Medicamento, Cosmetico, LoteEstoque, Receita, Venda e ItemVenda, com cardinalidades.]

Figura 1 - Diagrama UML simplificado da modelagem revisada.

### Leitura das cardinalidades

- Uma venda possui exatamente um funcionario e pode ter cliente identificado ou ser anonima.
- Uma venda concluída possui um ou mais itens; cada item referencia um produto e o lote utilizado.
- Um produto possui zero ou muitos lotes; um lote pertence a um único produto.
- Um cliente pode ter varias receitas; uma receita pode ser vinculada a uma venda quando utilizada.
- Uma receita pode listar um ou mais medicamentos. Em uma implementação normalizada, isso e representado por ItemReceita.

### Atributos calculados

- ItemVenda.subtotal: quantidade multiplicada pelo valor unitario.
- Venda.valorTotal: soma dos subtotais.
- Produto.quantidadeEstoque: soma das quantidades de lotes válidos e ativos.

### Por que ainda armazenar total e preço da venda?

Mesmo sendo calculados, valor_unitario, subtotal e valor_total podem ser persistidos para preservar o retrato histórico. Se o preço atual do produto mudar, a venda antiga continua correta. A aplicação sempre recalcula antes de gravar.

## 5 Classes e responsabilidades

### Entidades do dominio

| Classe | Dados principais | Responsabilidade |
|---|---|---|
| Pessoa (abstrata) | id, nome, cpf, telefone, ativo | Dados comuns de cliente e funcionario. |
| Cliente | endereço, fidelidadePontos | Identificação do comprador e fidelidade. |
| Funcionario | matricula, cargoFuncionario | Responsavel pelo atendimento. |
| Farmaceutico | crf | Funcionario habilitado para regras restritas. |
| Produto (abstrata) | codigoBarras, nome, fabricante, preço, ativo | Dados comuns do catálogo. |
| Medicamento | dosagem, principioAtivo, tipo, uso, categoriaRegulatoria, registroAnvisa | Características farmacologicas e regulatórias. |
| Cosmetico | categoriaUso, hipoalergênico | Produto de higiene, beleza ou perfumaria. |
| LoteEstoque | numeroLote, dataValidade, quantidade | Unidade real de estoque e validade. |
| Receita | número, emissão, prescritor, CRM, retida | Documento associado ao cliente e a venda. |
| ItemReceita | receita, medicamento, quantidadePrescrita | Relaciona a receita aos medicamentos. |
| Venda | dataHora, cliente, funcionario, status, valorTotal | Agregado transacional da venda. |
| ItemVenda | produto, lote, quantidade, valorUnitario, subtotal | Fotografia de cada item vendido. |

### Enumeracoes

| Enum | Exemplos | Observacao |
|---|---|---|
| CargoFuncionario | ATENDENTE, GERENTE, FARMACÊUTICO | Persistir pelo nome, não pela posicao numérica. |
| TipoMedicamento | ANTIBIOTICO, ANTINFILAMATORIO, OUTRO | Classificação clinica simplificada. |
| CategoriaRegulatoria | LIVRE, RECEITA_SIMPLES, RETENÇÃO, CONTROLADO | Dirige a Strategy de venda. |
| TipoUso | ORAL, INJETÁVEL, TÓPICO | Pode ser opcional conforme o produto. |
| CategoriaUsoCosmetico | CABELO, PELE, HIGIENE, PERFUMARIA | Evita texto livre inconsistente. |
| StatusVenda | EM_ABERTO, CONCLUIDA, CANCELADA | Venda finalizada não é apagada. |

## 6 Regras de negócio

| ID | Regra | Camada principal |
|---|---|---|
| RN01 | CPF, matrícula, CRF e código de barras não podem se repetir. | Servico + banco |
| RN02 | Preço, quantidade, pontos e totais não podem ser negativos. | Servico + banco |
| RN03 | Uma venda deve conter pelo menos um item com quantidade maior que zero. | VendaService |
| RN04 | Não vender quantidade superior ao estoque válido disponível. | EstoqueService/VendaService |
| RN05 | Lotes vencidos ou inativos não podem ser usados. | EstoqueService |
| RN06 | Medicamento com receita exige receita válida do cliente. | Strategy |
| RN07 | Quando houver retenção, a receita deve ser marcada dentro da transação. | Strategy/VendaService |
| RN08 | Controlado exige farmacêutico responsável, como regra didática. | Strategy |
| RN09 | O item copia o preço vigente no momento da venda. | VendaService |
| RN10 | Subtotal e total sao recalculados; nunca sao aceitos cegamente da tela. | VendaService |
| RN11 | Venda, itens, baixa de estoque, receita e pontos formam uma transação única. | VendaService |
| RN12 | Falha em qualquer passo exige rollback de tudo. | VendaService |
| RN13 | Venda concluída não tem itens editados; pode apenas ser cancelada. | VendaService |
| RN14 | Cancelamento mantem histórico e recompõe o estoque conforme regra documentada. | VendaService |
| RN15 | Entidade usada no histórico e desativada, não excluída. | Servicos |

### FEFO como evolução segura

Ao escolher automaticamente o lote, priorizem o que vence primeiro (First Expire, First Out). Para o MVP, o operador pode selecionar o lote, desde que a aplicação valide validade e saldo.

## 7 Modelo relacional no MySQL

### Tabelas sugeridas

| Tabela | Colunas essenciais | Chave/restrição |
|---|---|---|
| pessoa | id, nome, cpf, telefone, ativo | cpf UNIQUE |
| cliente | pessoa_id, endereço, fidelidade_pontos | PK também FK para pessoa |
| funcionario | pessoa_id, matrícula, cargoFuncionario | matrícula UNIQUE |
| farmacêutico | funcionario_id, crf | crf UNIQUE |
| produto | id, código_barras, nome, fabricante, preço, ativo | codigo_barras UNIQUE |
| medicamento | produto_id, dosagem, principio_ativo, tipo, categoria_regulatoria, tipo_uso, registro_anvisa | PK também FK para produto |
| cosmetico | produto_id, categoria_uso, hipoalergênico | PK também FK para produto |
| lote_estoque | id, produto_id, numero_lote, data_validade, quantidade | UNIQUE produto + lote |
| receita | id, cliente_id, número, data_emissão, nome_medico, crm_medico, retida | indices por cliente e número |
| item_receita | receita_id, medicamento_id, quantidade_prescrita | chave composta ou id proprio |
| venda | id, data_hora, cliente_id opcional, funcionario_id, status, valor_total | histórico preservado |
| item_venda | id, venda_id, produto_id, lote_id, quantidade, valor_unitario, subtotal | FKs e valores positivos |

### Mapeamento da herança

Para o trabalho, use uma tabela para a superclasse e outra para cada especialização. O identificador da subclasse e também chave estrangeira para a superclasse. Isso preserva a normalizacao e torna a relacao com o modelo Java evidente, embora exija JOINs nas consultas.

### Tipos de dados importantes

Use BIGINT para identificadores, DECIMAL para dinheiro, DATE para validade e emissão, DATETIME ou TIMESTAMP com política documentada para venda, INT para quantidades e BOOLEAN para flags. Nunca use FLOAT ou DOUBLE para valores monetarios.

## 8 Passo a passo no MySQL

1. Instalar e validar: instalar MySQL Server e, opcionalmente, Workbench. Confirmar que o service inicia e que e possível conectar localmente.
2. Criar ambientes: criar um schema de desenvolvimento e outro de testes, ambos com codificacao utf8mb4.
3. Criar usuário da aplicação: não usar root. Conceder somente as permissoes necessarias nos schemas do projeto.
4. Definir padrao: tabelas e colunas em snake_case, nomes sem acentos e chaves primarias consistentes.
5. Criar tabelas-base: pessoa e produto antes de suas especializações.
6. Criar especializações: cliente, funcionario, farmacêutico, medicamento e cosmetico com PK/FK apropriadas.
7. Criar operacionais: lote_estoque, receita, item_receita, venda e item_venda na ordem das dependências.
8. Adicionar integridade: PRIMARY KEY, FOREIGN KEY, UNIQUE, NOT NULL e restrições de valores não negativos.
9. Definir exclusão: preferir RESTRICT para histórico. A aplicação usa ativo e status em vez de apagar registros utilizados.
10. Criar indices: CPF, código de barras, nome do produto, princípio ativo, matrícula, CRF, validade, data da venda e todas as FKs relevantes.
11. Inserir dados fictícios: cliente, atendente, farmacêutico, cosmetico, medicamento livre, medicamento restrito, lote válido e lote vencido.
12. Testar as restrições: tentar duplicidades, referencias inexistentes, valores negativos e exclusoes proibidas.
13. Versionar os scripts: numerar criação, dados iniciais e alteracoes. O banco deve ser recriado do zero na ordem documentada.
14. Registrar a configuração: documentar host, porta, nome do schema e como criar o usuário, sem incluir senha real no repositório.

### Resultado esperado

- O esquema e recriado do zero apenas executando os scripts na ordem indicada.
- A conta da aplicação conecta sem privilegios administrativos.
- Duplicidades e referencias invalidas sao rejeitadas pelo banco.
- Os dados fictícios cobrem venda livre, receita, controlado, lote válido e lote vencido.

## 9 Arquitetura Java

Fluxo obrigatório JavaFX Controller -> Service -> DAO -> MySQL. O controller nunca executa SQL. O DAO nunca decide se uma receita e válida. A entidade não conhece JavaFX.

### Pacotes sugeridos

| Pacote | Conteúdo | Não deve conter |
|---|---|---|
| app | Classe de inicialização e composição das dependências | SQL e regra de negócio |
| config | Configuração, DataSource e fabrica de conexões | Credenciais fixas no código |
| model | Entidades e objetos de valor | JavaFX e JDBC |
| model.enums | Enumeracoes do dominio | Lógica de tela |
| dao | Interfaces de persistencia | Detalhes de JavaFX |
| dao.jdbc | Implementacoes com JDBC | Regra regulatórias |
| service | Casos de uso, validações e transações | Componentes JavaFX |
| strategy | Regras variaveis da venda | Acesso direto a controles visuais |
| controller | Adaptacao entre tela e servicos | SQL |
| validation | Validadores reutilizaveis | Persistencia |
| exception | Erros de dominio e persistencia | Mensagens tecnicas exibidas diretamente |

### Recursos

Dentro de src/main/resources, separe fxml, css, images e config. Evitem um pacote util que vire deposito de funcoes sem relacao. Cada classe deve ter uma responsabilidade identificavel.

## 10 Classes de aplicação

### Persistencia

| Tipo | Classes sugeridas | Responsabilidade |
|---|---|---|
| Conexão | ConnectionProvider ou DataSourceProvider | Fornecer conexões configuradas; preparar futuro pool. |
| Transação | TransactionManager | Compartilhar uma conexão, confirmar ou desfazer. |
| Interfaces DAO | ClienteDAO, FuncionarioDAO, ProdutoDAO, LoteEstoqueDAO, ReceitaDAO, VendaDAO | Definir operações sem expor JDBC. |
| Implementações | ClienteDAOJdbc e equivalentes | Executar SQL preparado e mapear resultados. |
| Erro | PersistenciaException | Traduzir falhas técnicas para a camada superior. |

### Servicos

| Servico | Responsabilidades |
|---|---|
| ClienteService | Validar CPF, duplicidade, edição e desativacao. |
| FuncionarioService | Validar matricula, cargoFuncionario e CRF do farmacêutico. |
| ProdutoService | Validar catálogo, preço, código de barras e classificações. |
| EstoqueService | Gerenciar lotes, validade, saldo e critério FEFO. |
| ReceitaService | Registrar, validar e controlar uso/retenção. |
| VendaService | Orquestrar estrategias, cálculos, transação, estoque, receita, pontos e cancelamento. |

### Controladores JavaFX

Crie um controller por tela ou fluxo: ClienteController, FuncionarioController, ProdutoController, LoteController, ReceitaController, VendaController e HistoricoVendaController. Eles recebem servicos prontos, convertem entrada de tela em comandos do caso de uso e exibem o resultado.

### Injeção simples e suficiente

Não é necessário adotar um framework de injeção. A classe de inicialização pode criar DAOs e serviços e fornece-los aos controllers por construtor ou por uma fabrica de controllers do FXMLLoader.

## 11 Patterns: DAO e Strategy

### DAO - Data Access Object

O DAO separa a persistencia do restante do sistema. A interface descreve operações como salvar, localizar, pesquisar, atualizar e desativar. A implementação JDBC conhece PreparedStatement, ResultSet e o esquema MySQL.

- Permite testar servicos com implementacoes em memoria ou dublês.
- Evita SQL duplicado em controllers e servicos.
- Concentra mapeamento entre linhas do banco e objetos.
- Facilita trocar JDBC por outra tecnologia no futuro.

### Strategy - regras de venda

Strategy representa uma regra variavel. Uma interface de validação recebe o contexto da venda; implementacoes diferentes validam venda livre, receita simples, retenção ou controlado. VendaService escolhe a Strategy conforme a categoria regulatória do medicamento.

| Strategy | Validações principais |
|---|---|
| VendaLivreStrategy | Estoque, validade e dados basicos da venda. |
| ReceitaObrigatoriaStrategy | Receita presente, cliente compatível e medicamento prescrito. |
| RetencaoReceitaStrategy | Tudo da receita obrigatória e marcacao de retenção. |
| ControladoStrategy | Receita, retenção e farmacêutico responsavel, conforme regra didática. |

### Por que não contar MVC como o segundo pattern

A separacao entre modelo, views FXML e controllers lembra MVC, mas funciona aqui como organizacao arquitetural da interface. Se a avaliacao pedir dois design patterns, apresentem DAO e Strategy e mostrem onde cada um aparece no projeto.

## 12 Preparação no IntelliJ IDEA

1. Definir versões: confirmar com o professor o JDK e o JavaFX aceitos. Escolher uma versão LTS compatível e usar a mesma em todas as máquinas.
2. Instalar ferramentas: JDK, IntelliJ IDEA, MySQL Server e, opcionalmente, Scene Builder.
3. Criar projeto Maven: informar groupld, artifactld e pacote-base. Maven evita adicionar JARs manualmente.
4. Selecionar o SDK: conferir Project SDK, linguagem do projeto e Java usado pelo Maven.
5. Planejar dependências: JavaFX Controls, JavaFX FXML, MySQL Connector/J, JUnit Jupiter e plugin de execução JavaFX. Pool e migracao sao opcionais.
6. Decidir modularização: para iniciantes, projeto não modular reduz configuração. Se houver module-info.java, documentar exports, requires e opens para FXML.
7. Criar pacotes e recursos: montar a estrutura definida antes de criar telas e DAOs.
8. Testar JavaFX isolado: abrir uma janela vazia por meio do Maven. Resolver configuração antes de integrar MySQL.
9. Externalizar o banco: URL, usuário e senha em propriedades externas ou variaveis de ambiente. Criar arquivo de exemplo sem segredos.
10. Testar conexão isolada: abrir e fechar uma conexão e mostrar um diagnostico controlado. Não avancar enquanto isso falhar.
11. Configurar testes: apontar testes de integração para o schema de testes, nunca para os dados de desenvolvimento.
12. Configurar versionamento: ignorar senhas, arquivos locais da IDE, build e logs. Versionar pom, fontes, recursos e scripts SQL.

### Decisão de versão

Não escolham versões diferentes por máquina. Registrem JDK, JavaFX, Maven, Connector/J e MySQL no README. A compatibilidade vale mais do que perseguir a versão mais nova durante o semestre.

## 13 Ordem de implementação no IntelliJ

| Fase | O que fazer | Pronto quando |
|---|---|---|
| 1. Base | Criar enums, excecoes e classes de dominio, sem JavaFX ou JDBC. | O modelo compila e seus invariantes sao testaveis. |
| 2. Conexão | Configurar DataSource/ConnectionProvider e banco de testes. | Conexão abre e fecha sem segredo no código. |
| 3. DAO | Criar interfaces e implementar um agregado por vez. | CRUD funciona sem interface gráfica. |
| 4. Servicos | Adicionar validações e casos de uso de cadastro. | Entradas invalidas geram erros de dominio claros. |
| 5. Strategy | Implementar e testar cada regra regulatória. | Cada categoria aceita e rejeita os cenarios corretos. |
| 6. Venda | Implementar cálculos e transação completa. | Falha provoca rollback e estoque nunca fica parcial. |
| 7. JavaFX | Criar telas por módulo, chamando os servicos existentes. | Controller não contém SQL nem regra central. |
| 8. Integração | Testar fluxo completo, cancelamento e reinicio. | Persistencia e histórico permanecem corretos. |
| 9. Entrega | Testar em outra máquina e revisar documentação. | Uma pessoa externa instala sem improvisacao. |

### Ordem dos módulos de tela

- Clientes e funcionarios.
- Produtos e lotes.
- Receitas.
- Carrinho e conclusao de venda.
- Histórico e cancelamento.
- Painel, filtros e refinamento visual.

### Evitem construir a tela primeiro

Uma tela bonita pode esconder uma arquitetura fragil. Se DAO e Service já estiverem testados, a interface vira uma camada fina e os erros ficam mais faceis de localizar.

## 14 Planejamento das telas JavaFX

| Tela | Objetivo | Cuidado |
|---|---|---|
| Identificação | Selecionar ou autenticar funcionario | Manter o funcionario atual no contexto da sessão. |
| Painel | Acessos aos módulos e alertas | Pode mostrar lotes proximos do vencimento. |
| Clientes | Lista, busca, cadastro, edição e desativação | Mascaras ajudam, mas Service válida de novo. |
| Funcionarios | Lista e formulario com campos condicionais | CRF aparece quando o cargoFuncionario exigir. |
| Produtos | Catálogo unificado com campos por subtipo | Evitar controller gigante; separar formularios se necessário. |
| Lotes | Entradas, saldos, validade e filtros | Destacar vencidos e proximos do vencimento. |
| Receitas | Cliente, prescritor e itens prescritos | Registrar uso/retenção sem apagar histórico. |
| Venda | Pesquisa, carrinho, cliente, receita e total | Botao concluir fica desabilitado quando inválida. |
| Histórico | Busca, detalhes e cancelamento | Não permitir editar itens de venda concluída. |

### Especificidades importantes do JavaFX

- FXML define estrutura e CSS define aparencia; não dupliquem estilos em cada controller.
- Consultas demoradas não devem bloquear a JavaFX Application Thread.
- Use Task ou Service do JavaFX para operações potencialmente lentas e atualize os controles apenas na thread visual.
- Controllers recebem servicos; não criam DAOs por conta propria.
- TableView deve ter pesquisa, filtros e possibilidade futura de paginação.
- Mensagens de validação devem ser amigaveis; detalhes tecnicos ficam em log controlado.
- Navegação pode ser centralizada em SceneManager para evitar código repetido.

## 15 Fluxo transacional da venda

1. Identificar o funcionario responsavel e, se houver, o cliente.
2. Adicionar produtos e quantidades ao carrinho.
3. Selecionar ou reservar lotes válidos com saldo suficiente.
4. Associar receita quando uma Strategy a exigir.
5. Executar as Strategies de todos os medicamentos.
6. Recalcular subtotais e total com BigDecimal.
7. Iniciar uma transação e compartilhar a mesma conexão entre os DAOs envolvidos.
8. Gravar a venda e seus itens.
9. Baixar os lotes sem permitir saldo negativo.
10. Registrar uso ou retenção da receita e atualizar fidelidade.
11. Confirmar tudo com commit.
12. Se qualquer passo falhar, executar rollback e apresentar mensagem segura.

### Fronteira da transação

A transação pertence ao VendaService, porque ele coordena varios DAOs. Cada DAO não pode confirmar a propria parte isoladamente. Caso contrario, uma falha posterior deixaria venda, estoque e receita inconsistentes.

### Concorrencia e estoque

Mesmo em um aplicativo inicialmente usado em um computador, construam a baixa de estoque de modo atômico. Uma evolução para dois terminais não pode permitir que ambos vendam a ultima unidade. O banco pode bloquear a linha do lote durante a transação ou executar uma atualizacao condicional que somente funciona quando o saldo e suficiente.

## 16 Testes e critérios de aceite

### Testes unitarios

- Cálculo de subtotal, total e pontos de fidelidade.
- Quantidade positiva e preço válido.
- Lote vencido e estoque insuficiente.
- Cada Strategy com cenarios aceitos é rejeitados.
- Receita ausente, medicamento não prescrito e funcionario inadequado.

### Testes de integração

- Salvar, consultar, atualizar, pesquisar e desativar por DAO.
- Rejeitar CPF, CRF, matrícula e código de barras duplicados.
- Concluir venda e comprovar baixa dos lotes.
- Forcar falha no meio da venda e comprovar rollback completo.
- Cancelar venda e comprovar histórico e recomposicao do estoque.

### Checklist de aceitacao manual

| Área | Critério |
|---|---|
| Cadastro | Cadastrar e editar cliente; impedir CPF duplicado. |
| Equipe | Cadastrar farmacêutico; exigir CRF quando aplicável. |
| Produto | Impedir preço inválido e código de barras duplicado. |
| Estoque | Cadastrar dois lotes; impedir lote vencido e saldo negativo. |
| Receita | Exigir receita e retenção conforme a categoria. |
| Venda | Calcular valores, baixar estoque e registrar responsaveis. |
| Rollback | Falha em um item desfaz toda a operação. |
| Histórico | Venda cancelada permanece consultavel e não é editavel. |
| Persistencia | Reiniciar o aplicativo e confirmar que os dados continuam. |
| Instalação | Executar o projeto e recriar o banco em outra máquina. |

## 17 Escalabilidade planejada

| Área | Decisão que preserva crescimento |
|---|---|
| Persistencia | DAO por interface; configuração externa; DataSource; opção futura de pool. |
| Banco | Scripts versionados; indices; migrações futuras com Flyway; banco de testes separado. |
| Domínio | Enums persistidos por nome; regras em Service e Strategy; dinheiro com BigDecimal. |
| Histórico | Status e desativação em vez de exclusão; preço copiado no item. |
| Estoque | Lotes como fonte de verdade; FEFO; baixa atômica; previsão de movimentos futura. |
| JavaFX | FXML e CSS separados; controllers pequenos; operações lentas em background. |
| Listagens | Pesquisa no banco, indices e possibilidade de paginação. |
| Configuração | Versões fixadas no build; segredos fora do repositório; arquivo de exemplo. |
| Tempo | Política definida para data/hora; conversao apenas nas bordas da aplicação. |
| Testabilidade | Servicos independentes da tela; banco de teste; DAOs substituíveis. |

### Evoluções que o modelo permite

- Fornecedor, compra e entrada de estoque.
- Movimentacao de estoque com auditoria completa.
- Usuários, senha, perfis e permissoes.
- Múltiplas filiais e transferencia entre estoques.
- Alertas de vencimento e reposicao.
- Integração fiscal e sanitaria, após analise especializada.

### Escalavel não significa complexo agora

Escalabilidade, neste projeto, significa manter fronteiras claras e evitar decisões que bloqueiem evoluções. Não significa implementar todos os recursos futuros no primeiro semestre.

## 18 Segurança, privacidade e erros

- Usar PreparedStatement; nunca concatenar entrada do usuário em SQL.
- Criar usuário MySQL com privilegios mínimos e não usar root na aplicação.
- Manter senha fora do código e fora do repositório.
- Não registrar CPF completo, receitas ou credenciais em logs.
- Se houver login, guardar apenas hash seguro de senha; nunca senha em texto puro.
- Apresentar mensagens amigaveis e registrar detalhes tecnicos separadamente.
- Coletar somente dados necessários e tratar CPF e receita como dados pessoais sensíveis ao contexto.
- Documentar backup e restauracao, mesmo que a automacao fique fora do MVP.

### Camadas de validação

| Camada | O que válida | Por que existe |
|---|---|---|
| JavaFX | Campo obrigatório, formato e feedback imediato | Boa experiencia; não é barreira de segurança. |
| Service | Regra de negócio e consistência do caso de uso | Protege qualquer interface presente ou futura. |
| MySQL | Unicidade, referencias, nulidade e limites simples | Ultima defesa contra dados invalidos. |

### Erro comum

Validar apenas na tela e insuficiente. Um teste, uma importacao futura ou outro controller poderia contornar a interface. As regras essenciais pertencem ao Service e as garantias estruturais também ficam no banco.

## 19 Plano de entrega

### Entregaveis sugeridos

- Projeto Maven com fontes e recursos.
- Scripts SQL numerados para criar o schema e inserir dados fictícios.
- Arquivo de configuração de exemplo, sem credenciais reais.
- README com versões, instalação, criação do banco e execução.
- Diagrama UML atualizado e modelo relacional descrito.
- Testes unitarios e de integração principais.
- Capturas das telas e roteiro curto de demonstracao.
- Este PDF revisado com decisões finais do grupo.

### Checklist final do grupo

- [ ] O professor confirmou o segundo pattern e a estrategia de herança.
- [ ] JDK, JavaFX, Maven, Connector/J e MySQL estao documentados.
- [ ] O banco e recriado do zero pelos scripts.
- [ ] Credenciais reais não éstao no repositório.
- [ ] DAO e Strategy estao identificados e justificaveis.
- [ ] Controllers não contém SQL nem regras centrais.
- [ ] Estoque nunca fica negativo e lote vencido é rejeitado.
- [ ] Receita e farmacêutico sao exigidos nas categorias corretas.
- [ ] Venda usa transação e rollback foi testado.
- [ ] Cancelamento preserva histórico.
- [ ] Projeto foi testado em uma segunda máquina.
- [ ] UML, banco e classes Java dizem a mesma coisa.

### Próximas decisões, antes de programar

1. Confirmar se o professor exige subclasses Antibiotico, Antiinflamatorio e Controlado.
2. Confirmar se o segundo pattern e livre; se for, adotar Strategy.
3. Definir se o projeto usara module-info.java.
4. Definir a regra de pontos de fidelidade e cancelamento.
5. Definir se o cliente é obrigatório ou se a venda anonima será aceita.
6. Escolher as versões instaladas no laboratorio e fixa-las no build.

## Referencias para consulta durante o desenvolvimento

Consultem as documentacoes oficiais correspondentes as versões escolhidas pelo grupo. O objetivo destas referencias e resolver configuração e comportamento da plataforma, não copiar uma implementação pronta.

- OpenJFX Documentation - configuração do JavaFX com Maven e FXML.
- IntelliJ IDEA Documentation - SDK, Maven, execução e depuracao.
- MySQL Reference Manual - tipos, restrições, transações, índices e bloqueios.
- MySQL Connector/J Developer Guide - configuração JDBC e conexões.
- JUnit User Guide - testes unitarios e organização de suites.
- Gamma, Helm, Johnson e Vlissides - Design Patterns.
- Martin Fowler - Patterns of Enterprise Application Architecture.

Documento preparado como guia de planejamento. Atualizem a modelagem sempre que uma decisão do professor ou do grupo alterar o escopo.