package dao.jdbc;

import dao.ClienteDAO;
import exception.PersistenciaException;
import model.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementação JDBC de ClienteDAO para o banco de dados MySQL.
 */
public class ClienteDAOJdbc implements ClienteDAO {

    private final Connection conn;

    public ClienteDAOJdbc(Connection conn) {
        this.conn = Objects.requireNonNull(conn, "A conexão não pode ser nula");
    }

    @Override
    public void inserir(Cliente cliente) {
        Objects.requireNonNull(cliente, "O cliente não pode ser nulo");

        String sqlPessoa = "INSERT INTO pessoa (nome, cpf, telefone, ativo) VALUES (?, ?, ?, ?)";
        String sqlCliente = "INSERT INTO cliente (pessoa_id, endereco, fidelidade_pontos) VALUES (?, ?, ?)";

        boolean autoCommitOriginal = true;
        try {
            autoCommitOriginal = conn.getAutoCommit();
            if (autoCommitOriginal) {
                conn.setAutoCommit(false);
            }

            try (PreparedStatement psPessoa = conn.prepareStatement(sqlPessoa, Statement.RETURN_GENERATED_KEYS)) {
                psPessoa.setString(1, cliente.getNome());
                psPessoa.setString(2, cliente.getCpf());
                psPessoa.setString(3, cliente.getTelefone());
                psPessoa.setBoolean(4, cliente.isAtivo());

                psPessoa.executeUpdate();

                try (ResultSet rs = psPessoa.getGeneratedKeys()) {
                    if (rs.next()) {
                        cliente.setId(rs.getLong(1));
                    } else {
                        throw new PersistenciaException("Falha ao obter o ID gerado para a pessoa.");
                    }
                }
            }

            try (PreparedStatement psCliente = conn.prepareStatement(sqlCliente)) {
                psCliente.setLong(1, cliente.getId());
                psCliente.setString(2, cliente.getEndereco());
                psCliente.setInt(3, cliente.getFidelidadePontos());

                psCliente.executeUpdate();
            }

            if (autoCommitOriginal) {
                conn.commit();
            }
        } catch (SQLException e) {
            if (autoCommitOriginal) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new PersistenciaException("Erro ao reverter transação após falha na inserção do cliente", ex);
                }
            }
            throw new PersistenciaException("Erro ao inserir cliente: " + e.getMessage(), e);
        } finally {
            if (autoCommitOriginal) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    throw new PersistenciaException("Erro ao restaurar auto-commit da conexão", e);
                }
            }
        }
    }

    @Override
    public void atualizar(Cliente cliente) {
        Objects.requireNonNull(cliente, "O cliente não pode ser nulo");
        if (cliente.getId() == null) {
            throw new IllegalArgumentException("O ID do cliente não pode ser nulo para atualização.");
        }

        String sqlPessoa = "UPDATE pessoa SET nome = ?, cpf = ?, telefone = ?, ativo = ? WHERE id = ?";
        String sqlCliente = "UPDATE cliente SET endereco = ?, fidelidade_pontos = ? WHERE pessoa_id = ?";

        boolean autoCommitOriginal = true;
        try {
            autoCommitOriginal = conn.getAutoCommit();
            if (autoCommitOriginal) {
                conn.setAutoCommit(false);
            }

            try (PreparedStatement psPessoa = conn.prepareStatement(sqlPessoa)) {
                psPessoa.setString(1, cliente.getNome());
                psPessoa.setString(2, cliente.getCpf());
                psPessoa.setString(3, cliente.getTelefone());
                psPessoa.setBoolean(4, cliente.isAtivo());
                psPessoa.setLong(5, cliente.getId());

                psPessoa.executeUpdate();
            }

            try (PreparedStatement psCliente = conn.prepareStatement(sqlCliente)) {
                psCliente.setString(1, cliente.getEndereco());
                psCliente.setInt(2, cliente.getFidelidadePontos());
                psCliente.setLong(3, cliente.getId());

                psCliente.executeUpdate();
            }

            if (autoCommitOriginal) {
                conn.commit();
            }
        } catch (SQLException e) {
            if (autoCommitOriginal) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new PersistenciaException("Erro ao reverter transação após falha na atualização do cliente", ex);
                }
            }
            throw new PersistenciaException("Erro ao atualizar cliente: " + e.getMessage(), e);
        } finally {
            if (autoCommitOriginal) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    throw new PersistenciaException("Erro ao restaurar auto-commit da conexão", e);
                }
            }
        }
    }

    @Override
    public void desativar(Long id) {
        Objects.requireNonNull(id, "O ID não pode ser nulo para desativação.");
        String sql = "UPDATE pessoa SET ativo = FALSE WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao desativar cliente com ID " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Cliente> buscarPorId(Long id) {
        Objects.requireNonNull(id, "O ID não pode ser nulo para busca.");
        String sql = "SELECT p.id, p.nome, p.cpf, p.telefone, p.ativo, c.endereco, c.fidelidade_pontos " +
                     "FROM pessoa p " +
                     "INNER JOIN cliente c ON p.id = c.pessoa_id " +
                     "WHERE p.id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(instanciarCliente(rs));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar cliente por ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Cliente> buscarPorCpf(String cpf) {
        Objects.requireNonNull(cpf, "O CPF não pode ser nulo para busca.");
        String sql = "SELECT p.id, p.nome, p.cpf, p.telefone, p.ativo, c.endereco, c.fidelidade_pontos " +
                     "FROM pessoa p " +
                     "INNER JOIN cliente c ON p.id = c.pessoa_id " +
                     "WHERE p.cpf = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cpf);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(instanciarCliente(rs));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar cliente por CPF: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Cliente> listarTodos() {
        String sql = "SELECT p.id, p.nome, p.cpf, p.telefone, p.ativo, c.endereco, c.fidelidade_pontos " +
                     "FROM pessoa p " +
                     "INNER JOIN cliente c ON p.id = c.pessoa_id " +
                     "ORDER BY p.nome";

        List<Cliente> clientes = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clientes.add(instanciarCliente(rs));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar todos os clientes: " + e.getMessage(), e);
        }
        return clientes;
    }

    @Override
    public List<Cliente> buscarPorNome(String nome) {
        Objects.requireNonNull(nome, "O nome não pode ser nulo para busca.");
        String sql = "SELECT p.id, p.nome, p.cpf, p.telefone, p.ativo, c.endereco, c.fidelidade_pontos " +
                     "FROM pessoa p " +
                     "INNER JOIN cliente c ON p.id = c.pessoa_id " +
                     "WHERE LOWER(p.nome) LIKE LOWER(?) " +
                     "ORDER BY p.nome";

        List<Cliente> clientes = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + nome + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    clientes.add(instanciarCliente(rs));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar clientes por nome: " + e.getMessage(), e);
        }
        return clientes;
    }

    private Cliente instanciarCliente(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente(rs.getString("nome"), rs.getString("cpf"));
        cliente.setId(rs.getLong("id"));
        cliente.setTelefone(rs.getString("telefone"));
        cliente.setAtivo(rs.getBoolean("ativo"));
        cliente.setEndereco(rs.getString("endereco"));
        cliente.setFidelidadePontos(rs.getInt("fidelidade_pontos"));
        return cliente;
    }
}
