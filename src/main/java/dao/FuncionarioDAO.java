package dao;

import model.Farmaceutico;
import model.Funcionario;

import java.util.List;
import java.util.Optional;

public interface FuncionarioDAO {

    /**
     * Insere um novo funcionário ou farmacêutico no banco de dados.
     * Caso o objeto seja uma instância de Farmaceutico, deve persistir nas 3
     tabelas (pessoa, funcionario e farmaceutico).
     *
     * @param funcionario Instância a ser persistida.
     */
    void inserir(Funcionario funcionario);

    /**
     * Atualiza os dados cadastrais, cargo, matrícula e CRF (caso aplicável).
     *
     * @param funcionario Instância com os dados atualizados.
     */
    void atualizar(Funcionario funcionario);

    /**
     * Realiza a desativação lógica do funcionário (ativo = false na tabela
     pessoa).
     * Preserva o histórico de vendas realizadas pelo colaborador.
     *
     * @param id Identificador do funcionário (pessoa_id).
     */
    void desativar(Long id);

    /**
     * Busca um funcionário pelo ID.
     * Se o registro corresponder a um farmacêutico, deve retornar a instância
     concreta de Farmaceutico.
     *
     * @param id Identificador do funcionário.
     * @return Optional contendo o funcionário/farmacêutico, se encontrado.
     */
    Optional<Funcionario> buscarPorId(Long id);

    /**
     * Busca um funcionário pelo CPF.
     *
     * @param cpf CPF do funcionário.
     * @return Optional contendo o funcionário/farmacêutico, se encontrado.
     */
    Optional<Funcionario> buscarPorCpf(String cpf);

    /**
     * Busca um funcionário pela matrícula.
     *
     * @param matricula Matrícula funcional única.
     * @return Optional contendo o funcionário/farmacêutico, se encontrado.
     */
    Optional<Funcionario> buscarPorMatricula(String matricula);

    /**
     * Busca um farmacêutico específico pelo número do CRF.
     *
     * @param crf Registro no Conselho Regional de Farmácia.
     * @return Optional contendo o farmacêutico, se encontrado.
     */
    Optional<Farmaceutico> buscarPorCrf(String crf);

    /**
     * Lista todos os funcionários ativos (atendentes, gerentes e farmacêuticos).
     *
     * @return Lista de funcionários ativos.
     */
    List<Funcionario> listarTodos();

    /**
     * Lista apenas os farmacêuticos ativos.
     * Utilizado para vincular o responsável técnico em vendas de medicamentos
     controlados.
     *
     * @return Lista de farmacêuticos ativos.
     */
    List<Farmaceutico> listarFarmaceuticosAtivos();
}
