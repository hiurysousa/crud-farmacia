package dao;

import model.Receita;
import java.util.List;
import java.util.Optional;

public interface ReceitaDAO {

    /**
     * Salva a receita e todos os seus ItemReceita associados em uma única transação.
     */
    void inserir(Receita receita);

    /**
     * Atualiza dados da receita ou marcação de retenção.
     */
    void atualizar(Receita receita);

    /**
     * Busca a receita pelo ID, trazendo também seus itens prescritos montados.
     */
    Optional<Receita> buscarPorId(Long id);

    /**
     * Busca uma receita pelo seu número identificador.
     */
    Optional<Receita> buscarPorNumero(String numero);

    /**
     * Lista o histórico de receitas emitidas para um determinado cliente.
     */
    List<Receita> listarPorCliente(Long clienteId);
}