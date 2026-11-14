package service;

import dao.DaoFactory;
import dao.LoteEstoqueDAO;
import dao.ProdutoDAO;
import exception.RegraNegocioException;
import exception.ValidacaoException;
import model.LoteEstoque;
import model.Produto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Camada de serviço para gerenciamento de lotes e controle de estoque com algoritmo FEFO.
 */
public class EstoqueService {

    /**
     * Registro imutável (Java Record) que representa o resultado da alocação de um lote.
     * Informa qual lote foi utilizado e quantas unidades foram consumidas dele.
     */
    public record AlocacaoLote(LoteEstoque lote, int quantidadeConsumida) {}

    private final LoteEstoqueDAO loteDAO;
    private final ProdutoDAO produtoDAO;

    public EstoqueService() {
        this(DaoFactory.createLoteEstoqueDAO(), DaoFactory.createProdutoDAO());
    }

    public EstoqueService(LoteEstoqueDAO loteDAO, ProdutoDAO produtoDAO) {
        this.loteDAO = Objects.requireNonNull(loteDAO, "LoteEstoqueDAO não pode ser nulo");
        this.produtoDAO = Objects.requireNonNull(produtoDAO, "ProdutoDAO não pode ser nulo");
    }

    /**
     * Registra a entrada de um novo lote de estoque no sistema com validações de validade.
     */
    public void darEntradaLote(LoteEstoque lote) {
        if (lote == null) {
            throw new ValidacaoException("O lote não pode ser nulo.");
        }
        if (lote.getProduto() == null || lote.getProduto().getId() == null) {
            throw new ValidacaoException("O lote deve estar associado a um produto existente.");
        }
        if (lote.getNumeroLote() == null || lote.getNumeroLote().trim().isBlank()) {
            throw new ValidacaoException("O número do lote é obrigatório.");
        }
        if (lote.getQuantidade() <= 0) {
            throw new ValidacaoException("A quantidade inicial do lote deve ser maior que zero.");
        }
        if (lote.getDataValidade() == null) {
            throw new ValidacaoException("A data de validade do lote é obrigatória.");
        }
        if (lote.getDataValidade().isBefore(LocalDate.now())) {
            throw new RegraNegocioException("Não é permitido cadastrar um lote com data de validade já expirada.");
        }

        // Garante que o produto existe no banco
        produtoDAO.buscarPorId(lote.getProduto().getId())
                .orElseThrow(() -> new RegraNegocioException("Produto associado ao lote não foi encontrado."));

        // Garante a unicidade: (produto_id + numero_lote)
        Optional<LoteEstoque> loteExistente = loteDAO.buscarPorProdutoENumero(
                lote.getProduto().getId(),
                lote.getNumeroLote().trim()
        );
        if (loteExistente.isPresent()) {
            throw new RegraNegocioException("Já existe um lote cadastrado com o número '"
                    + lote.getNumeroLote() + "' para este produto.");
        }

        loteDAO.inserir(lote);
    }

    /**
     * Retorna o saldo total de unidades disponíveis para venda de um produto.
     */
    public int consultarSaldoDisponivel(Long produtoId) {
        if (produtoId == null) {
            throw new ValidacaoException("ID do produto inválido.");
        }
        return loteDAO.listarDisponiveisPorProduto(produtoId).stream()
                .mapToInt(LoteEstoque::getQuantidade)
                .sum();
    }

    /**
     * Lista todos os lotes ativos e válidos de um produto, ordenados pela data de validade (FEFO).
     */
    public List<LoteEstoque> listarLotesDisponiveis(Long produtoId) {
        if (produtoId == null) {
            throw new ValidacaoException("ID do produto inválido.");
        }
        return loteDAO.listarDisponiveisPorProduto(produtoId);
    }

    /**
     * Lista lotes que já venceram para controle e descarte sanitário.
     */
    public List<LoteEstoque> listarLotesVencidos() {
        return loteDAO.listarVencidos();
    }

    /**
     * ALGORITMO FEFO (First Expire, First Out):
     * Deduz a quantidade solicitada consumindo sequencialmente dos lotes com vencimento mais próximo.
     *
     * @param produtoId Identificador do produto vendido.
     * @param quantidadeDesejada Quantidade total necessária.
     * @return Lista com cada lote utilizado e a respectiva quantidade consumida dele.
     */
    public List<AlocacaoLote> baixarEstoqueFefo(Long produtoId, int quantidadeDesejada) {
        if (produtoId == null) {
            throw new ValidacaoException("ID do produto não pode ser nulo.");
        }
        if (quantidadeDesejada <= 0) {
            throw new ValidacaoException("A quantidade solicitada para baixa deve ser maior que zero.");
        }

        // 1. Busca os lotes disponíveis ordenados por data de validade crescente
        List<LoteEstoque> lotesFefo = loteDAO.listarDisponiveisPorProduto(produtoId);

        // 2. Valida se o saldo total disponível cobre o pedido
        int saldoTotal = lotesFefo.stream().mapToInt(LoteEstoque::getQuantidade).sum();
        if (saldoTotal < quantidadeDesejada) {
            throw new RegraNegocioException("Estoque insuficiente para o produto ID " + produtoId
                    + ". Disponível: " + saldoTotal + ", Solicitado: " + quantidadeDesejada);
        }

        // 3. Distribui a dedução entre os lotes mais antigos
        List<AlocacaoLote> alocacoes = new ArrayList<>();
        int restante = quantidadeDesejada;

        for (LoteEstoque lote : lotesFefo) {
            if (restante <= 0) {
                break;
            }

            int qtdLote = lote.getQuantidade();
            int deducao = Math.min(qtdLote, restante);

            // Atualiza o saldo do lote no banco
            lote.setQuantidade(qtdLote - deducao);
            loteDAO.atualizar(lote);

            alocacoes.add(new AlocacaoLote(lote, deducao));
            restante -= deducao;
        }

        return alocacoes;
    }

    /**
     * Estorna a quantidade de um lote (utilizado no cancelamento de vendas ou estornos).
     */
    public void estornarEstoque(Long loteId, int quantidadeEstornada) {
        if (loteId == null) {
            throw new ValidacaoException("ID do lote não pode ser nulo.");
        }
        if (quantidadeEstornada <= 0) {
            throw new ValidacaoException("A quantidade a ser estornada deve ser maior que zero.");
        }

        LoteEstoque lote = loteDAO.buscarPorId(loteId)
                .orElseThrow(() -> new RegraNegocioException("Lote ID " + loteId + " não encontrado para estorno."));

        lote.setQuantidade(lote.getQuantidade() + quantidadeEstornada);
        loteDAO.atualizar(lote);
    }
}