package dao;

import model.Venda;
import model.enums.StatusVenda;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VendaDAO {

    /**
     * Salva a venda e todos os seus itens (ItemVenda) associados.
     */
    void inserir(Venda venda);

    /**
     * Atualiza o status da venda (ex: de EM_ABERTO para CONCLUIDA ou CANCELADA).
     */
    void atualizarStatus(Long id, StatusVenda status);

    /**
     * Busca a venda completa por ID, carregando Cliente, Funcionário, Receita,
     * Farmacêutico e a lista de itens vendidos.
     */
    Optional<Venda> buscarPorId(Long id);

    List<Venda> listarTodas();

    /**
     * Lista vendas dentro de um intervalo de datas (para relatórios ou fechamento de caixa).
     */
    List<Venda> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fim);

    /**
     * Lista o histórico de compras de um cliente específico.
     */
    List<Venda> listarPorCliente(Long clienteId);
}