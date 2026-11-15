package service;

import dao.ClienteDAO;
import dao.DaoFactory;
import dao.FuncionarioDAO;
import dao.LoteEstoqueDAO;
import dao.ProdutoDAO;
import dao.ReceitaDAO;
import dao.VendaDAO;
import db.DataBase;
import exception.PersistenciaException;
import exception.RegraNegocioException;
import exception.ValidacaoException;
import model.Cliente;
import model.Farmaceutico;
import model.Funcionario;
import model.ItemReceita;
import model.ItemVenda;
import model.LoteEstoque;
import model.Medicamento;
import model.Produto;
import model.Receita;
import model.Venda;
import model.enums.CategoriaRegulatoria;
import model.enums.StatusVenda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Camada de serviço responsável pela orquestração transacional das vendas,
 * validação regulatória, congelamento de preços, baixa FEFO e cancelamento.
 */
public class VendaService {

    // Regra de fidelidade: 1 ponto acumulado a cada R$ 10,00 em compras
    private static final BigDecimal REAIS_POR_PONTO = new BigDecimal("10.00");

    private final VendaDAO vendaDAO;
    private final ProdutoDAO produtoDAO;
    private final LoteEstoqueDAO loteDAO;
    private final ClienteDAO clienteDAO;
    private final FuncionarioDAO funcionarioDAO;
    private final ReceitaDAO receitaDAO;

    public VendaService() {
        this(
                DaoFactory.createVendaDAO(),
                DaoFactory.createProdutoDAO(),
                DaoFactory.createLoteEstoqueDAO(),
                DaoFactory.createClienteDAO(),
                DaoFactory.createFuncionarioDAO(),
                DaoFactory.createReceitaDAO()
        );
    }

    public VendaService(VendaDAO vendaDAO, ProdutoDAO produtoDAO, LoteEstoqueDAO loteDAO,
                        ClienteDAO clienteDAO, FuncionarioDAO funcionarioDAO, ReceitaDAO receitaDAO) {
        this.vendaDAO = Objects.requireNonNull(vendaDAO, "VendaDAO não pode ser nulo");
        this.produtoDAO = Objects.requireNonNull(produtoDAO, "ProdutoDAO não pode ser nulo");
        this.loteDAO = Objects.requireNonNull(loteDAO, "LoteEstoqueDAO não pode ser nulo");
        this.clienteDAO = Objects.requireNonNull(clienteDAO, "ClienteDAO não pode ser nulo");
        this.funcionarioDAO = Objects.requireNonNull(funcionarioDAO, "FuncionarioDAO não pode ser nulo");
        this.receitaDAO = Objects.requireNonNull(receitaDAO, "ReceitaDAO não pode ser nulo");
    }

    /**
     * Realiza o fechamento completo de uma venda de forma atômica:
     * 1. Valida envolvidos (funcionário, cliente opcional, itens).
     * 2. Aplica regras regulatórias (receita simples, retenção, controlado + farmacêutico).
     * 3. Congela os preços vigentes do catálogo e recalcula subtotais e total com BigDecimal.
     * 4. Dentro de uma única transação:
     *    - Baixa estoque via FEFO (gerando um ItemVenda por lote alocado);
     *    - Grava a Venda e os ItemVenda;
     *    - Marca a Receita como retida (quando aplicável);
     *    - Credita pontos de fidelidade ao Cliente (quando identificado).
     */
    public Venda finalizarVenda(Venda venda) {
        validarEstruturaBasica(venda);

        // 1. Validar Funcionário (vendedor obrigatório e ativo)
        Funcionario vendedor = funcionarioDAO.buscarPorId(venda.getFuncionario().getId())
                .orElseThrow(() -> new RegraNegocioException("Funcionário vendedor não encontrado."));
        if (!vendedor.isAtivo()) {
            throw new RegraNegocioException("Funcionário vendedor está desativado e não pode registrar vendas.");
        }
        venda.setFuncionario(vendedor);

        // 2. Validar Cliente (opcional na venda anônima, obrigatório se houver receita)
        Cliente cliente = null;
        if (venda.getCliente() != null && venda.getCliente().getId() != null) {
            cliente = clienteDAO.buscarPorId(venda.getCliente().getId())
                    .orElseThrow(() -> new RegraNegocioException("Cliente informado não encontrado."));
            if (!cliente.isAtivo()) {
                throw new RegraNegocioException("O cliente informado está desativado.");
            }
            venda.setCliente(cliente);
        } else {
            venda.setCliente(null);
        }

        // 3. Agrupar quantidades solicitadas por produto e carregar catálogo atualizado do banco
        Map<Long, Integer> qtdPorProduto = new HashMap<>();
        Map<Long, Produto> produtosCatalogo = new HashMap<>();

        for (ItemVenda item : venda.getItens()) {
            if (item == null || item.getProduto() == null || item.getProduto().getId() == null) {
                throw new ValidacaoException("Todos os itens da venda devem referenciar um produto válido.");
            }
            if (item.getQuantidade() <= 0) {
                throw new ValidacaoException("A quantidade de cada item deve ser maior que zero.");
            }

            Long prodId = item.getProduto().getId();
            Produto produto = produtosCatalogo.computeIfAbsent(prodId, id ->
                    produtoDAO.buscarPorId(id)
                            .orElseThrow(() -> new RegraNegocioException("Produto ID " + id + " não encontrado no catálogo."))
            );

            if (!produto.isAtivo()) {
                throw new RegraNegocioException("O produto '" + produto.getNome() + "' está desativado e não pode ser vendido.");
            }

            qtdPorProduto.merge(prodId, item.getQuantidade(), Integer::sum);
        }

        // 4. Validar Regras Regulatórias (Receita Simples, Retenção e Controlado)
        boolean exigeReceita = false;
        boolean exigeRetencao = false;
        boolean exigeFarmaceutico = false;

        for (Produto produto : produtosCatalogo.values()) {
            if (produto instanceof Medicamento med) {
                CategoriaRegulatoria cat = med.getCategoriaRegulatoria();
                if (cat == CategoriaRegulatoria.RECEITA_SIMPLES) {
                    exigeReceita = true;
                } else if (cat == CategoriaRegulatoria.RETENCAO) {
                    exigeReceita = true;
                    exigeRetencao = true;
                } else if (cat == CategoriaRegulatoria.CONTROLADO) {
                    exigeReceita = true;
                    exigeRetencao = true;
                    exigeFarmaceutico = true;
                }
            }
        }

        Receita receitaValidada = null;
        if (exigeReceita) {
            receitaValidada = validarReceitaParaItens(venda, cliente, qtdPorProduto, produtosCatalogo);
            venda.setReceita(receitaValidada);
        } else if (venda.getReceita() != null && venda.getReceita().getId() != null) {
            // Caso o operador tenha vinculado uma receita opcionalmente, valida compatibilidade
            receitaValidada = validarReceitaParaItens(venda, cliente, qtdPorProduto, produtosCatalogo);
            venda.setReceita(receitaValidada);
        } else {
            venda.setReceita(null);
        }

        if (exigeFarmaceutico) {
            validarFarmaceuticoResponsavel(venda, vendedor);
        } else if (venda.getFarmaceutico() != null && venda.getFarmaceutico().getId() != null) {
            validarFarmaceuticoResponsavel(venda, vendedor);
        } else {
            venda.setFarmaceutico(null);
        }

        // 5. Orquestração Transacional Atômica
        final Cliente clienteFinal = cliente;
        final Receita receitaFinal = receitaValidada;
        final boolean deveReterReceita = exigeRetencao && receitaValidada != null;

        Connection conn = null;
        try {
            conn = DataBase.getConnection();
            conn.setAutoCommit(false);

            // Instancia DAOs vinculados à mesma conexão transacional (se o DaoFactory suportar)
            // Caso seus DAOs usem ConnectionProvider/ThreadLocal, basta usar os atributos da classe.
            LoteEstoqueDAO txLoteDAO = DaoFactory.createLoteEstoqueDAO(conn);
            VendaDAO txVendaDAO = DaoFactory.createVendaDAO(conn);
            ReceitaDAO txReceitaDAO = DaoFactory.createReceitaDAO(conn);
            ClienteDAO txClienteDAO = DaoFactory.createClienteDAO(conn);

            List<ItemVenda> itensFinaisCongelados = new ArrayList<>();
            BigDecimal valorTotalVenda = BigDecimal.ZERO;

            // Baixa estoque FEFO e congela preço unitário de cada lote utilizado
            for (Map.Entry<Long, Integer> entry : qtdPorProduto.entrySet()) {
                Long produtoId = entry.getKey();
                int quantidadeNecessaria = entry.getValue();
                Produto produto = produtosCatalogo.get(produtoId);

                List<LoteEstoque> lotesDisponiveis = txLoteDAO.listarDisponiveisPorProduto(produtoId);
                int saldoTotal = lotesDisponiveis.stream().mapToInt(LoteEstoque::getQuantidade).sum();

                if (saldoTotal < quantidadeNecessaria) {
                    throw new RegraNegocioException("Estoque insuficiente para '" + produto.getNome()
                            + "'. Disponível: " + saldoTotal + ", Solicitado: " + quantidadeNecessaria);
                }

                // Preço unitário congelado no momento da venda
                BigDecimal precoUnitario = produto.getPreco().setScale(2, RoundingMode.HALF_UP);
                int restante = quantidadeNecessaria;

                for (LoteEstoque lote : lotesDisponiveis) {
                    if (restante <= 0) {
                        break;
                    }

                    int deducao = Math.min(lote.getQuantidade(), restante);
                    lote.setQuantidade(lote.getQuantidade() - deducao);
                    txLoteDAO.atualizar(lote);

                    BigDecimal subtotalItem = precoUnitario
                            .multiply(BigDecimal.valueOf(deducao))
                            .setScale(2, RoundingMode.HALF_UP);

                    ItemVenda itemCongelado = new ItemVenda();
                    itemCongelado.setProduto(produto);
                    itemCongelado.setLote(lote);
                    itemCongelado.setQuantidade(deducao);
                    itemCongelado.setValorUnitario(precoUnitario);
                    itemCongelado.setSubtotal(subtotalItem);

                    itensFinaisCongelados.add(itemCongelado);
                    valorTotalVenda = valorTotalVenda.add(subtotalItem);
                    restante -= deducao;
                }
            }

            // Prepara dados finais da Venda
            venda.setDataHora(LocalDateTime.now());
            venda.setStatus(StatusVenda.CONCLUIDA);
            venda.setItens(itensFinaisCongelados);
            venda.setValorTotal(valorTotalVenda.setScale(2, RoundingMode.HALF_UP));

            // Persiste Venda e seus Itens
            txVendaDAO.inserir(venda);

            // Marca retenção da receita se exigido pela categoria regulatória
            if (deveReterReceita) {
                receitaFinal.setRetida(true);
                txReceitaDAO.atualizar(receitaFinal);
            }

            // Credita pontos de fidelidade se houver cliente identificado
            if (clienteFinal != null) {
                int pontosGanhos = calcularPontosFidelidade(valorTotalVenda);
                if (pontosGanhos > 0) {
                    clienteFinal.setFidelidadePontos(clienteFinal.getFidelidadePontos() + pontosGanhos);
                    txClienteDAO.atualizar(clienteFinal);
                }
            }

            conn.commit();
            return venda;

        } catch (RuntimeException e) {
            realizarRollback(conn);
            throw e;
        } catch (SQLException e) {
            realizarRollback(conn);
            throw new PersistenciaException("Erro de banco de dados ao concluir a venda: " + e.getMessage(), e);
        } finally {
            restaurarAutoCommit(conn);
        }
    }

    /**
     * Cancela uma venda concluída de forma atômica, preservando o histórico:
     * - Altera status para CANCELADA;
     * - Estorna as quantidades aos respectivos lotes originais;
     * - Abate os pontos de fidelidade creditados naquela venda (sem deixar saldo negativo).
     */
    public void cancelarVenda(Long vendaId) {
        if (vendaId == null) {
            throw new ValidacaoException("O ID da venda não pode ser nulo.");
        }

        Venda venda = vendaDAO.buscarPorId(vendaId)
                .orElseThrow(() -> new RegraNegocioException("Venda com ID " + vendaId + " não encontrada."));

        if (venda.getStatus() == StatusVenda.CANCELADA) {
            throw new RegraNegocioException("Esta venda já se encontra cancelada.");
        }
        if (venda.getStatus() != StatusVenda.CONCLUIDA) {
            throw new RegraNegocioException("Apenas vendas concluídas podem ser canceladas por esta operação.");
        }

        Connection conn = null;
        try {
            conn = DataBase.getConnection();
            conn.setAutoCommit(false);

            VendaDAO txVendaDAO = DaoFactory.createVendaDAO(conn);
            LoteEstoqueDAO txLoteDAO = DaoFactory.createLoteEstoqueDAO(conn);
            ClienteDAO txClienteDAO = DaoFactory.createClienteDAO(conn);

            // 1. Devolve as quantidades para cada lote utilizado na venda
            if (venda.getItens() != null) {
                for (ItemVenda item : venda.getItens()) {
                    LoteEstoque lote = txLoteDAO.buscarPorId(item.getLote().getId())
                            .orElseThrow(() -> new RegraNegocioException(
                                    "Lote ID " + item.getLote().getId() + " não encontrado para estorno."));

                    lote.setQuantidade(lote.getQuantidade() + item.getQuantidade());
                    txLoteDAO.atualizar(lote);
                }
            }

            // 2. Abate os pontos de fidelidade ganhos nesta venda (se houve cliente identificado)
            if (venda.getCliente() != null && venda.getCliente().getId() != null) {
                Cliente cliente = txClienteDAO.buscarPorId(venda.getCliente().getId())
                        .orElseThrow(() -> new RegraNegocioException("Cliente da venda não encontrado para estorno de pontos."));

                int pontosEstornar = calcularPontosFidelidade(venda.getValorTotal());
                int novoSaldo = Math.max(0, cliente.getFidelidadePontos() - pontosEstornar);
                cliente.setFidelidadePontos(novoSaldo);
                txClienteDAO.atualizar(cliente);
            }

            // 3. Atualiza status para CANCELADA mantendo histórico intacto
            venda.setStatus(StatusVenda.CANCELADA);
            txVendaDAO.atualizarStatus(venda.getId(), StatusVenda.CANCELADA);

            conn.commit();

        } catch (RuntimeException e) {
            realizarRollback(conn);
            throw e;
        } catch (SQLException e) {
            realizarRollback(conn);
            throw new PersistenciaException("Erro de banco de dados ao cancelar a venda: " + e.getMessage(), e);
        } finally {
            restaurarAutoCommit(conn);
        }
    }

    public Venda buscarPorId(Long id) {
        if (id == null) {
            throw new ValidacaoException("O ID da venda não pode ser nulo.");
        }
        return vendaDAO.buscarPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Venda com ID " + id + " não encontrada."));
    }

    public List<Venda> listarTodas() {
        return vendaDAO.listarTodas();
    }

    // =========================================================================
    // MÉTODOS AUXILIARES DE VALIDAÇÃO E CÁLCULO
    // =========================================================================

    private void validarEstruturaBasica(Venda venda) {
        if (venda == null) {
            throw new ValidacaoException("A venda não pode ser nula.");
        }
        if (venda.getFuncionario() == null || venda.getFuncionario().getId() == null) {
            throw new ValidacaoException("O funcionário responsável pelo atendimento é obrigatório.");
        }
        if (venda.getItens() == null || venda.getItens().isEmpty()) {
            throw new RegraNegocioException("A venda deve conter pelo menos um item.");
        }
        if (venda.getStatus() == StatusVenda.CONCLUIDA || venda.getStatus() == StatusVenda.CANCELADA) {
            throw new RegraNegocioException("Não é possível alterar ou reprocessar uma venda já finalizada ou cancelada.");
        }
    }

    private Receita validarReceitaParaItens(Venda venda, Cliente cliente,
                                            Map<Long, Integer> qtdPorProduto,
                                            Map<Long, Produto> produtosCatalogo) {
        if (cliente == null) {
            throw new RegraNegocioException("A venda contém medicamentos controlados/tarjados e exige um cliente identificado.");
        }
        if (venda.getReceita() == null || venda.getReceita().getId() == null) {
            throw new RegraNegocioException("A venda contém medicamentos que exigem receita médica válida.");
        }

        Receita receita = receitaDAO.buscarPorId(venda.getReceita().getId())
                .orElseThrow(() -> new RegraNegocioException("Receita médica informada não foi encontrada."));

        if (receita.getCliente() == null || !cliente.getId().equals(receita.getCliente().getId())) {
            throw new RegraNegocioException("A receita médica pertence a outro cliente.");
        }
        if (receita.isRetida()) {
            throw new RegraNegocioException("A receita médica informada já foi retida anteriormente e não pode ser reutilizada.");
        }

        // Mapeia medicamentos prescritos na receita
        Map<Long, Integer> prescritos = new HashMap<>();
        if (receita.getItens() != null) {
            for (ItemReceita itemRec : receita.getItens()) {
                prescritos.put(itemRec.getMedicamento().getId(), itemRec.getQuantidadePrescrita());
            }
        }

        // Verifica se todo medicamento que exige receita consta na prescrição em quantidade suficiente
        for (Map.Entry<Long, Produto> entry : produtosCatalogo.entrySet()) {
            if (entry.getValue() instanceof Medicamento med
                    && med.getCategoriaRegulatoria() != CategoriaRegulatoria.LIVRE) {

                Integer qtdPrescrita = prescritos.get(med.getId());
                int qtdComprada = qtdPorProduto.get(med.getId());

                if (qtdPrescrita == null) {
                    throw new RegraNegocioException("O medicamento '" + med.getNome()
                            + "' não consta na receita médica apresentada.");
                }
                if (qtdComprada > qtdPrescrita) {
                    throw new RegraNegocioException("A quantidade solicitada de '" + med.getNome()
                            + "' (" + qtdComprada + ") excede a quantidade prescrita na receita (" + qtdPrescrita + ").");
                }
            }
        }

        return receita;
    }

    private void validarFarmaceuticoResponsavel(Venda venda, Funcionario vendedor) {
        // Se o próprio vendedor já for um farmacêutico ativo e nenhum outro foi informado, assume ele
        if ((venda.getFarmaceutico() == null || venda.getFarmaceutico().getId() == null)
                && vendedor instanceof Farmaceutico farmVendedor) {
            validarDadosFarmaceutico(farmVendedor);
            venda.setFarmaceutico(farmVendedor);
            return;
        }

        if (venda.getFarmaceutico() == null || venda.getFarmaceutico().getId() == null) {
            throw new RegraNegocioException(
                    "Vendas contendo medicamentos CONTROLADOS exigem a identificação de um farmacêutico responsável.");
        }

        Funcionario func = funcionarioDAO.buscarPorId(venda.getFarmaceutico().getId())
                .orElseThrow(() -> new RegraNegocioException("Farmacêutico responsável não encontrado."));

        if (!(func instanceof Farmaceutico farmaceutico)) {
            throw new RegraNegocioException("O funcionário indicado como responsável técnico não é um farmacêutico.");
        }

        validarDadosFarmaceutico(farmaceutico);
        venda.setFarmaceutico(farmaceutico);
    }

    private void validarDadosFarmaceutico(Farmaceutico farmaceutico) {
        if (!farmaceutico.isAtivo()) {
            throw new RegraNegocioException("O farmacêutico responsável está desativado no sistema.");
        }
        if (farmaceutico.getCrf() == null || farmaceutico.getCrf().isBlank()) {
            throw new RegraNegocioException("O farmacêutico responsável não possui CRF ativo cadastrado.");
        }
    }

    public int calcularPontosFidelidade(BigDecimal valorTotal) {
        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return valorTotal.divide(REAIS_POR_PONTO, 0, RoundingMode.DOWN).intValue();
    }

    private void realizarRollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Falha ao executar rollback da transação: " + ex.getMessage());
            }
        }
    }

    private void restaurarAutoCommit(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException ex) {
                System.err.println("Falha ao restaurar auto-commit da conexão: " + ex.getMessage());
            }
        }
    }
}