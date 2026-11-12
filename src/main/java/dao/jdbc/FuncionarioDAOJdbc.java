package dao.jdbc;

import dao.FuncionarioDAO;
import exception.PersistenciaException;
import model.Farmaceutico;
import model.Funcionario;
import model.enums.CargoFuncionario;

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
 * Implementação JDBC de FuncionarioDAO com suporte a herança polimórfica.
 */
public class FuncionarioDAOJdbc implements FuncionarioDAO {

    private final Connection conn;

    public FuncionarioDAOJdbc(Connection conn) {
        this.conn = Objects.requireNonNull(conn, "A conexão não pode ser nula");
    }

    @Override
    public void inserir(Funcionario funcionario) {
        Objects.requireNonNull(funcionario, "O funcionário não pode ser nulo");
        Objects.requireNonNull(funcionario.getCargo(), "O cargo do funcionário não pode ser nulo");

        String sqlPessoa = "INSERT INTO pessoa (nome, cpf, telefone, ativo) VALUES (?, ?, ?, ?)";
        String sqlFuncionario = "INSERT INTO funcionario (pessoa_id, matricula, cargoFuncionario) VALUES (?, ?, ?)";
        String sqlFarmaceutico = "INSERT INTO farmaceutico (funcionario_id, crf) VALUES (?, ?)";

        boolean autoCommitOriginal = true;
        try {
            autoCommitOriginal = conn.getAutoCommit();
            if (autoCommitOriginal) {
                conn.setAutoCommit(false);
            }

            // 1. Inserir na tabela pessoa
            try (PreparedStatement psPessoa = conn.prepareStatement(sqlPessoa, Statement.RETURN_GENERATED_KEYS)) {
                psPessoa.setString(1, funcionario.getNome());
                psPessoa.setString(2, funcionario.getCpf());
                psPessoa.setString(3, funcionario.getTelefone());
                psPessoa.setBoolean(4, funcionario.isAtivo());
                psPessoa.executeUpdate();

                try (ResultSet rs = psPessoa.getGeneratedKeys()) {
                    if (rs.next()) {
                        funcionario.setId(rs.getLong(1));
                    } else {
                        throw new PersistenciaException("Falha ao obter o ID gerado para pessoa.");
                    }
                }
            }

            // 2. Inserir na tabela funcionario
            try (PreparedStatement psFunc = conn.prepareStatement(sqlFuncionario))
            {
                psFunc.setLong(1, funcionario.getId());
                psFunc.setString(2, funcionario.getMatricula());
                psFunc.setString(3, funcionario.getCargo().name());
                psFunc.executeUpdate();
            }

            // 3. Se for farmacêutico, persistir na tabela farmaceutico
            if (funcionario instanceof Farmaceutico farmaceutico) {
                try (PreparedStatement psFarm = conn.prepareStatement(sqlFarmaceutico)) {
                    psFarm.setLong(1, farmaceutico.getId());
                    psFarm.setString(2, farmaceutico.getCrf());
                    psFarm.executeUpdate();
                }
            }

            if (autoCommitOriginal) {
                conn.commit();
            }
        } catch (SQLException e) {
            if (autoCommitOriginal) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new PersistenciaException("Erro ao reverter transação",
                            ex);
                }
            }
            throw new PersistenciaException("Erro ao inserir funcionário: " + e.getMessage(), e);
        } finally {
            if (autoCommitOriginal) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    throw new PersistenciaException("Erro ao restaurar auto-commit", e);
                }
            }
        }
    }

    @Override
    public void atualizar(Funcionario funcionario) {
        Objects.requireNonNull(funcionario, "O funcionário não pode ser nulo");
        if (funcionario.getId() == null) {
            throw new IllegalArgumentException("O ID do funcionário não pode ser nulo para atualização.");
        }

        String sqlPessoa = "UPDATE pessoa SET nome = ?, cpf = ?, telefone = ?, ativo = ? WHERE id = ?";
        String sqlFuncionario = "UPDATE funcionario SET matricula = ?, cargoFuncionario = ? WHERE pessoa_id = ?";
        String sqlFarmaceuticoUpdate = "UPDATE farmaceutico SET crf = ? WHERE funcionario_id = ?";
        String sqlFarmaceuticoInsert = "INSERT INTO farmaceutico (funcionario_id, crf) VALUES (?, ?)";

        boolean autoCommitOriginal = true;
        try {
            autoCommitOriginal = conn.getAutoCommit();
            if (autoCommitOriginal) {
                conn.setAutoCommit(false);
            }

            // 1. Atualizar pessoa
            try (PreparedStatement psPessoa = conn.prepareStatement(sqlPessoa)) {
                psPessoa.setString(1, funcionario.getNome());
                psPessoa.setString(2, funcionario.getCpf());
                psPessoa.setString(3, funcionario.getTelefone());
                psPessoa.setBoolean(4, funcionario.isAtivo());
                psPessoa.setLong(5, funcionario.getId());
                psPessoa.executeUpdate();
            }

            // 2. Atualizar funcionario
            try (PreparedStatement psFunc = conn.prepareStatement(sqlFuncionario))
            {
                psFunc.setString(1, funcionario.getMatricula());
                psFunc.setString(2, funcionario.getCargo().name());
                psFunc.setLong(3, funcionario.getId());
                psFunc.executeUpdate();
            }

            // 3. Atualizar ou inserir farmaceutico (caso tenha sido promovido oualterado CRF)
            if (funcionario instanceof Farmaceutico farmaceutico) {
                try (PreparedStatement psFarm =
                             conn.prepareStatement(sqlFarmaceuticoUpdate)) {
                    psFarm.setString(1, farmaceutico.getCrf());
                    psFarm.setLong(2, farmaceutico.getId());
                    int linhas = psFarm.executeUpdate();
                    if (linhas == 0) {
                        try (PreparedStatement psInsert =
                                     conn.prepareStatement(sqlFarmaceuticoInsert)) {
                            psInsert.setLong(1, farmaceutico.getId());
                            psInsert.setString(2, farmaceutico.getCrf());
                            psInsert.executeUpdate();
                        }
                    }
                }
            }

            if (autoCommitOriginal) {
                conn.commit();
            }
        } catch (SQLException e) {
            if (autoCommitOriginal) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new PersistenciaException("Erro ao reverter transação",
                            ex);
                }
            }
            throw new PersistenciaException("Erro ao atualizar funcionário: " +
                    e.getMessage(), e);
        } finally {
            if (autoCommitOriginal) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    throw new PersistenciaException("Erro ao restaurar auto-commit", e);
                }
            }
        }
    }

    @Override
    public void desativar(Long id) {
        Objects.requireNonNull(id, "O ID não pode ser nulo");
        String sql = "UPDATE pessoa SET ativo = FALSE WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao desativar funcionário: " +
                    e.getMessage(), e);
        }
    }

    @Override
    public Optional<Funcionario> buscarPorId(Long id) {
        Objects.requireNonNull(id, "O ID não pode ser nulo");
        String sql = baseSelectSql() + " WHERE p.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(instanciarFuncionario(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar funcionário por ID: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Funcionario> buscarPorCpf(String cpf) {
        Objects.requireNonNull(cpf, "O CPF não pode ser nulo");
        String sql = baseSelectSql() + " WHERE p.cpf = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cpf);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(instanciarFuncionario(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar funcionário por CPF: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Funcionario> buscarPorMatricula(String matricula) {
        Objects.requireNonNull(matricula, "A matrícula não pode ser nula");
        String sql = baseSelectSql() + " WHERE f.matricula = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matricula);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(instanciarFuncionario(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar funcionário por matrícula: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Farmaceutico> buscarPorCrf(String crf) {
        Objects.requireNonNull(crf, "O CRF não pode ser nulo");
        String sql = baseSelectSql() + " WHERE fm.crf = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, crf);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Funcionario f = instanciarFuncionario(rs);
                    if (f instanceof Farmaceutico farmaceutico) {
                        return Optional.of(farmaceutico);
                    }
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar farmacêutico por CRF: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Funcionario> listarTodos() {
        String sql = baseSelectSql() + " WHERE p.ativo = TRUE ORDER BY p.nome";
        List<Funcionario> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(instanciarFuncionario(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar funcionários: " +
                    e.getMessage(), e);
        }
    }

    @Override
    public List<Farmaceutico> listarFarmaceuticosAtivos() {
        String sql = baseSelectSql() + " WHERE p.ativo = TRUE AND f.cargoFuncionario = 'FARMACEUTICO' ORDER BY p.nome";
        List<Farmaceutico> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Funcionario f = instanciarFuncionario(rs);
                if (f instanceof Farmaceutico farmaceutico) {
                    lista.add(farmaceutico);
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar farmacêuticos ativos: " + e.getMessage(), e);
        }
    }

    // Consulta SQL base reaproveitada com LEFT JOIN
    private String baseSelectSql() {
        return "SELECT p.id, p.nome, p.cpf, p.telefone, p.ativo, " +
                "f.matricula, f.cargoFuncionario, " +
                "fm.crf " +
                "FROM pessoa p " +
                "INNER JOIN funcionario f ON p.id = f.pessoa_id " +
                "LEFT JOIN farmaceutico fm ON f.pessoa_id = fm.funcionario_id";
    }

    // Mapeamento polimórfico: decide se instancia Farmaceutico ou Funcionario
    private Funcionario instanciarFuncionario(ResultSet rs) throws SQLException {
        String cargoStr = rs.getString("cargoFuncionario");
        CargoFuncionario cargo = CargoFuncionario.valueOf(cargoStr);
        String crf = rs.getString("crf");

        Funcionario f;
        if (cargo == CargoFuncionario.FARMACEUTICO || crf != null) {
            f = new Farmaceutico(rs.getString("nome"), rs.getString("cpf"), crf);
        } else {
            f = new Funcionario(rs.getString("nome"), rs.getString("cpf"));
            f.setCargo(cargo);
        }

        f.setId(rs.getLong("id"));
        f.setTelefone(rs.getString("telefone"));
        f.setAtivo(rs.getBoolean("ativo"));
        f.setMatricula(rs.getString("matricula"));
        return f;
    }
}