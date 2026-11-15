package service;

import dao.DaoFactory;
import dao.LoteEstoqueDAO;
import dao.ProdutoDAO;
import exception.RegraNegocioException;
import exception.ValidacaoException;
import model.LoteEstoque;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Camada de serviço para gerenciamento de lotes e controle de estoque com algoritmo FEFO.
 */
public class EstoqueService {

    /** Lote utilizado e quantidade consumida dele em uma baixa. */
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
     * Registra a entrada de um novo lote de estoque com validações de validade e unicidade.
     */
    public void darEntradaLote(LoteEstoque lote) {
        validarLote(lote);

        String numeroLote = lote.getNumeroLote().trim();
        lote.setNumeroLote(numeroLote); // persiste o mesmo valor usado na checagem de unicidade
        Long produtoId = lote.getProduto().getId();

        produtoDAO.buscarPorId(produtoId)
                .orElseThrow(() -> new RegraNegocioException("Produto associado ao lote não foi encontrado."));

        if (loteDAO.buscarPorProdutoENumero(produtoId, numeroLote).isPresent()) {
            throw new RegraNegocioException("Já existe um lote cadastrado com o número '"
                    + numeroLote + "' para este produto.");
        }

        loteDAO.inserir(lote);
    }

    /**
     * Retorna o saldo total de unidades disponíveis (lotes ativos, válidos e com saldo > 0).
     */
    public int consultarSaldoDisponivel(Long produtoId) {
        return somarSaldo(listarLotesDisponiveis(produtoId));
    }

    /**
     * Lista os lotes ativos e válidos de um produto, ordenados pela data de validade (FEFO).
     */
    public List<LoteEstoque> listarLotesDisponiveis(Long produtoId) {
        exigirId(produtoId, "ID do produto inválido.");
        return loteDAO.listarDisponiveisPorProduto(produtoId);
    }

    /**
     * Lista lotes que já venceram para controle e descarte sanitário.
     */
    public List<LoteEstoque> listarLotesVencidos() {
        return loteDAO.listarVencidos();
    }

    /**
     * FEFO (First Expire, First Out): deduz a quantidade consumindo sequencialmente
     * os lotes com vencimento mais próximo.
     * <p>
     * Atenção: este método não controla transação. O chamador (ex.: VendaService) deve
     * executá-lo dentro de uma transação para que falhas parciais sofram rollback.
     *
     * @return cada lote utilizado e a quantidade consumida dele.
     */
    public List<AlocacaoLote> baixarEstoqueFefo(Long produtoId, int quantidadeDesejada) {
        exigirId(produtoId, "ID do produto não pode ser nulo.");
        if (quantidadeDesejada <= 0) {
            throw new ValidacaoException("A quantidade solicitada para baixa deve ser maior que zero.");
        }

        List<LoteEstoque> lotesFefo = loteDAO.listarDisponiveisPorProduto(produtoId);

        int saldoTotal = somarSaldo(lotesFefo);
        if (saldoTotal < quantidadeDesejada) {
            throw new RegraNegocioException("Estoque insuficiente para o produto ID " + produtoId
                    + ". Disponível: " + saldoTotal + ", Solicitado: " + quantidadeDesejada);
        }

        List<AlocacaoLote> alocacoes = new ArrayList<>();
        int restante = quantidadeDesejada;

        for (LoteEstoque lote : lotesFefo) {
            if (restante == 0) {
                break;
            }
            int deducao = Math.min(lote.getQuantidade(), restante);

            lote.setQuantidade(lote.getQuantidade() - deducao);
            loteDAO.atualizar(lote);

            alocacoes.add(new AlocacaoLote(lote, deducao));
            restante -= deducao;
        }

        return alocacoes;
    }

    /**
     * Estorna a quantidade de um lote (cancelamento de vendas ou estornos).
     */
    public void estornarEstoque(Long loteId, int quantidadeEstornada) {
        exigirId(loteId, "ID do lote não pode ser nulo.");
        if (quantidadeEstornada <= 0) {
            throw new ValidacaoException("A quantidade a ser estornada deve ser maior que zero.");
        }

        LoteEstoque lote = loteDAO.buscarPorId(loteId)
                .orElseThrow(() -> new RegraNegocioException("Lote ID " + loteId + " não encontrado para estorno."));

        lote.setQuantidade(lote.getQuantidade() + quantidadeEstornada);
        loteDAO.atualizar(lote);
    }

    private static void validarLote(LoteEstoque lote) {
        if (lote == null) {
            throw new ValidacaoException("O lote não pode ser nulo.");
        }
        if (lote.getProduto() == null || lote.getProduto().getId() == null) {
            throw new ValidacaoException("O lote deve estar associado a um produto existente.");
        }
        if (lote.getNumeroLote() == null || lote.getNumeroLote().isBlank()) {
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
    }

    private static int somarSaldo(List<LoteEstoque> lotes) {
        return lotes.stream().mapToInt(LoteEstoque::getQuantidade).sum();
    }

    private static void exigirId(Long id, String mensagem) {
        if (id == null) {
            throw new ValidacaoException(mensagem);
        }
    }
}