package dao;

import model.Cliente;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistência para as operações do agregado Cliente.
 */
public interface ClienteDAO {

    /**
     * Insere um novo cliente no banco de dados.
     * Deve persistir tanto os dados da superclasse (pessoa) quanto da especialização (cliente).
     *
     * @param cliente Instância de Cliente a ser persistida.
     */
    void inserir(Cliente cliente);

    /**
     * Atualiza os dados de um cliente existente nas tabelas pessoa e cliente.
     *
     * @param cliente Instância de Cliente com os dados atualizados.
     */
    void atualizar(Cliente cliente);

    /**
     * Realiza a desativação lógica do cliente (ativo = false).
     * Não deve realizar exclusão física para preservar histórico de vendas.
     *
     * @param id Identificador do cliente (pessoa_id).
     */
    void desativar(Long id);

    /**
     * Busca um cliente pelo seu identificador único.
     *
     * @param id Identificador do cliente.
     * @return Optional contendo o cliente caso encontrado.
     */
    Optional<Cliente> buscarPorId(Long id);

    /**
     * Busca um cliente pelo CPF cadastrado.
     *
     * @param cpf CPF do cliente (11 dígitos).
     * @return Optional contendo o cliente caso encontrado.
     */
    Optional<Cliente> buscarPorCpf(String cpf);

    /**
     * Lista todos os clientes ativos do sistema.
     *
     * @return Lista com todos os clientes encontrados.
     */
    List<Cliente> listarTodos();

    /**
     * Busca clientes por correspondência parcial de nome.
     *
     * @param nome Fragmento do nome para busca.
     * @return Lista com os clientes encontrados.
     */
    List<Cliente> buscarPorNome(String nome);
}
