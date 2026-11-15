package service;

import dao.ClienteDAO;
import dao.DaoFactory;
import exception.RegraNegocioException;
import exception.ValidacaoException;
import model.Cliente;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Camada de serviço para gerenciamento de clientes e programa de fidelidade.
 */
public class ClienteService {

    private final ClienteDAO clienteDAO;

    public ClienteService() {
        this(DaoFactory.createClienteDAO());
    }

    public ClienteService(ClienteDAO clienteDAO) {
        this.clienteDAO = Objects.requireNonNull(clienteDAO, "ClienteDAO não pode ser nulo");
    }

    /**
     * Cadastra um novo cliente após validar os dados obrigatórios, o formato/dígitos do CPF
     * e garantir que o CPF não esteja duplicado no banco.
     */
    public void cadastrar(Cliente cliente) {
        validarCliente(cliente);
        normalizarDados(cliente);

        if (cliente.getFidelidadePontos() < 0) {
            throw new ValidacaoException("Os pontos de fidelidade iniciais não podem ser negativos.");
        }

        Optional<Cliente> existente = clienteDAO.buscarPorCpf(cliente.getCpf());
        if (existente.isPresent()) {
            throw new RegraNegocioException("Já existe um cliente cadastrado com o CPF informado.");
        }

        clienteDAO.inserir(cliente);
    }

    /**
     * Atualiza os dados cadastrais de um cliente existente.
     */
    public void atualizar(Cliente cliente) {
        validarCliente(cliente);

        if (cliente.getId() == null) {
            throw new ValidacaoException("O ID do cliente é obrigatório para atualização.");
        }
        buscarPorId(cliente.getId()); // Garante que o cliente existe no banco
        normalizarDados(cliente);

        if (cliente.getFidelidadePontos() < 0) {
            throw new ValidacaoException("Os pontos de fidelidade não podem ser negativos.");
        }

        Optional<Cliente> outroComMesmoCpf = clienteDAO.buscarPorCpf(cliente.getCpf());
        if (outroComMesmoCpf.isPresent() && !outroComMesmoCpf.get().getId().equals(cliente.getId())) {
            throw new RegraNegocioException("O CPF informado já está em uso por outro cliente.");
        }

        clienteDAO.atualizar(cliente);
    }

    /**
     * Desativa logicamente o cliente (ativo = false), preservando o histórico de vendas e receitas.
     */
    public void desativar(Long id) {
        Cliente cliente = buscarPorId(id);
        if (!cliente.isAtivo()) {
            throw new RegraNegocioException("Este cliente já se encontra desativado.");
        }
        clienteDAO.desativar(id);
    }

    public Cliente buscarPorId(Long id) {
        if (id == null) {
            throw new ValidacaoException("O ID do cliente não pode ser nulo.");
        }
        return clienteDAO.buscarPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Cliente com ID " + id + " não encontrado."));
    }

    public Cliente buscarPorCpf(String cpf) {
        String cpfLimpo = limparEValidarCpf(cpf);
        return clienteDAO.buscarPorCpf(cpfLimpo)
                .orElseThrow(() -> new RegraNegocioException("Cliente não encontrado para o CPF informado."));
    }

    public List<Cliente> listarTodos() {
        return clienteDAO.listarTodos();
    }

    public List<Cliente> buscarPorNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return listarTodos();
        }
        return clienteDAO.buscarPorNome(nome.trim());
    }

    /**
     * Adiciona pontos ao saldo de fidelidade de um cliente ativo.
     */
    public void adicionarPontos(Long clienteId, int pontos) {
        if (pontos <= 0) {
            throw new ValidacaoException("A quantidade de pontos a adicionar deve ser maior que zero.");
        }

        Cliente cliente = buscarPorId(clienteId);
        if (!cliente.isAtivo()) {
            throw new RegraNegocioException("Não é possível adicionar pontos a um cliente desativado.");
        }

        cliente.setFidelidadePontos(cliente.getFidelidadePontos() + pontos);
        clienteDAO.atualizar(cliente);
    }

    /**
     * Debita (resgata) pontos do saldo de fidelidade de um cliente ativo,
     * impedindo que o saldo fique negativo.
     */
    public void debitarPontos(Long clienteId, int pontos) {
        if (pontos <= 0) {
            throw new ValidacaoException("A quantidade de pontos a debitar deve ser maior que zero.");
        }

        Cliente cliente = buscarPorId(clienteId);
        if (!cliente.isAtivo()) {
            throw new RegraNegocioException("Não é possível debitar pontos de um cliente desativado.");
        }

        if (cliente.getFidelidadePontos() < pontos) {
            throw new RegraNegocioException("Saldo de pontos insuficiente. Disponível: "
                    + cliente.getFidelidadePontos() + ", Solicitado: " + pontos);
        }

        cliente.setFidelidadePontos(cliente.getFidelidadePontos() - pontos);
        clienteDAO.atualizar(cliente);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES DE VALIDAÇÃO E NORMALIZAÇÃO
    // =========================================================================

    private void validarCliente(Cliente cliente) {
        if (cliente == null) {
            throw new ValidacaoException("O cliente não pode ser nulo.");
        }
        if (cliente.getNome() == null || cliente.getNome().isBlank()) {
            throw new ValidacaoException("O nome do cliente é obrigatório.");
        }
        // Valida formato (11 dígitos) e dígitos verificadores do CPF
        limparEValidarCpf(cliente.getCpf());
    }

    private void normalizarDados(Cliente cliente) {
        cliente.setNome(cliente.getNome().trim());
        // Garante que o CPF salvo no banco tenha exatamente 11 dígitos numéricos (CHAR(11))
        cliente.setCpf(somenteDigitos(cliente.getCpf()));

        if (cliente.getTelefone() != null) {
            String tel = cliente.getTelefone().trim();
            cliente.setTelefone(tel.isEmpty() ? null : tel);
        }
        if (cliente.getEndereco() != null) {
            String end = cliente.getEndereco().trim();
            cliente.setEndereco(end.isEmpty() ? null : end);
        }
    }

    /**
     * Remove pontuações (caso venham da interface com máscara) e valida os 11 dígitos e DVs.
     */
    private String limparEValidarCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            throw new ValidacaoException("O CPF do cliente é obrigatório.");
        }

        String digitos = somenteDigitos(cpf);
        if (!isCpfValido(digitos)) {
            throw new ValidacaoException("O CPF informado é inválido.");
        }
        return digitos;
    }

    private String somenteDigitos(String valor) {
        return valor.replaceAll("\\D", "");
    }

    /**
     * Valida se o CPF possui 11 dígitos, não é uma sequência repetida (ex: 111.111.111-11)
     * e confere ambos os dígitos verificadores (módulo 11).
     */
    private boolean isCpfValido(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return false;
        }

        // Rejeita sequências de dígitos iguais (00000000000, 11111111111, etc.)
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }

        // Cálculo do 1º dígito verificador
        int soma = 0;
        for (int i = 0; i < 9; i++) {
            int num = cpf.charAt(i) - '0';
            soma += num * (10 - i);
        }
        int resto = soma % 11;
        int digito1 = (resto < 2) ? 0 : (11 - resto);

        if ((cpf.charAt(9) - '0') != digito1) {
            return false;
        }

        // Cálculo do 2º dígito verificador
        soma = 0;
        for (int i = 0; i < 10; i++) {
            int num = cpf.charAt(i) - '0';
            soma += num * (11 - i);
        }
        resto = soma % 11;
        int digito2 = (resto < 2) ? 0 : (11 - resto);

        return (cpf.charAt(10) - '0') == digito2;
    }
}