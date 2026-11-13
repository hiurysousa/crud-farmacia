package dao;

import model.LoteEstoque;
import java.util.List;
import java.util.Optional;

public interface LoteEstoqueDAO {

    void inserir(LoteEstoque lote);

    void atualizar(LoteEstoque lote);

    void desativar(Long id);

    Optional<LoteEstoque> buscarPorId(Long id);

    /**
     * Busca um lote específico através do produto e do número do lote (UNIQUE no banco).
     */
    Optional<LoteEstoque> buscarPorProdutoENumero(Long produtoId, String numeroLote);

    /**
     * Lista todos os lotes de um produto específico.
     */
    List<LoteEstoque> listarPorProduto(Long produtoId);

    /**
     * Lista apenas lotes com saldo > 0, dentro da validade e ativos,
     * ordenados pela data de validade crescente (essencial para a venda FEFO).
     */
    List<LoteEstoque> listarDisponiveisPorProduto(Long produtoId);

    /**
     * Lista lotes vencidos para controle e descarte na farmácia.
     */
    List<LoteEstoque> listarVencidos();
}