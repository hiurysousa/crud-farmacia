package dao;

import model.Cosmetico;
import model.Medicamento;
import model.Produto;

import java.util.List;
import java.util.Optional;

public interface ProdutoDAO {

    /**
     * Insere um novo produto no banco.
     * Persiste na tabela produto e na tabela filha correspondente (medicamento ou cosmetico).
     */
    void inserir(Produto produto);

    /**
     * Atualiza os dados comuns (produto) e específicos (medicamento ou cosmetico).
     */
    void atualizar(Produto produto);

    /**
     * Realiza a desativação lógica do produto (ativo = false).
     */
    void desativar(Long id);

    /**
     * Busca um produto pelo seu ID único (Primary Key).
     */
    Optional<Produto> buscarPorId(Long id);

    /**
     * Busca um produto pelo código de barras cadastrado.
     */
    Optional<Produto> buscarPorCodigoBarras(String codigoBarras);

    /**
     * Lista todos os produtos ativos (medicamentos e cosméticos).
     */
    List<Produto> listarTodos();

    /**
     * Busca produtos ativos por correspondência parcial do nome.
     */
    List<Produto> buscarPorNome(String nome);

    /**
     * Busca medicamentos ativos pelo princípio ativo informado.
     */
    List<Medicamento> buscarPorPrincipioAtivo(String principioAtivo);
}