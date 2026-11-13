package dao.jdbc;

import dao.ReceitaDAO;
import exception.PersistenciaException;
import model.Cliente;
import model.ItemReceita;
import model.Medicamento;
import model.Receita;
import model.enums.CategoriaRegulatoria;
import model.enums.TipoMedicamento;
import model.enums.TipoUso;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementação JDBC de ReceitaDAO com persistência transacional da receita e seus itens prescritos.
 */
public class ReceitaDAOJdbc implements ReceitaDAO {

    private final Connection conn;

    public ReceitaDAOJdbc(Connection conn) {
        this.conn = Objects.requireNonNull(conn, "A conexão não pode ser nula");
    }

    @Override
    public void inserir(Receita receita) {
        Objects.requireNonNull(receita, "A receita não pode ser nula");
        Objects.requireNonNull(receita.getCliente(), "O cliente da receita não pode ser nulo");
        Objects.requireNonNull(receita.getCliente().getId(), "O ID do cliente da receita não pode ser nulo");
        Objects.requireNonNull(receita.getNumero(), "O número da receita não pode ser nulo");
        Objects.requireNonNull(receita.getDataEmissao(), "A data de emissão da receita não pode ser nula");
        Objects.requireNonNull(receita.getNomeMedico(), "O nome do médico não pode ser nulo");
        Objects.requireNonNull(receita.getCrmMedico(), "O CRM do médico não pode ser nulo");

        String sqlReceita = "INSERT INTO receita (cliente_id, numero, data_emissao, nome_medico, crm_medico, retida) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        String sqlItem = "INSERT INTO item_receita (receita_id, medicamento_id, quantidade_prescrita) " +
                "VALUES (?, ?, ?)";

        boolean autoCommitOriginal = true;
        try {
            autoCommitOriginal = conn.getAutoCommit();
            if (autoCommitOriginal) {
                conn.setAutoCommit(false);
            }

            // 1. Inserir cabeçalho da receita
            try (PreparedStatement psReceita = conn.prepareStatement(sqlReceita, Statement.RETURN_GENERATED_KEYS)) {
                psReceita.setLong(1, receita.getCliente().getId());
                psReceita.setString(2, receita.getNumero());
                psReceita.setDate(3, Date.valueOf(receita.getDataEmissao()));
                psReceita.setString(4, receita.getNomeMedico());
                psReceita.setString(5, receita.getCrmMedico());
                psReceita.setBoolean(6, receita.isRetida());
                psReceita.executeUpdate();

                try (ResultSet rs = psReceita.getGeneratedKeys()) {
                    if (rs.next()) {
                        receita.setId(rs.getLong(1));
                    } else {
                        throw new PersistenciaException("Falha ao obter o ID gerado para receita.");
                    }
                }
            }

            // 2. Inserir itens da receita (chave primária composta: receita_id, medicamento_id)
            List<ItemReceita> itens = receita.getItens();
            if (itens != null && !itens.isEmpty()) {
                try (PreparedStatement psItem = conn.prepareStatement(sqlItem)) {
                    for (ItemReceita item : itens) {
                        Objects.requireNonNull(item.getMedicamento(), "O medicamento do item da receita não pode ser nulo");
                        Objects.requireNonNull(item.getMedicamento().getId(), "O ID do medicamento do item da receita não pode ser nulo");

                        psItem.setLong(1, receita.getId());
                        psItem.setLong(2, item.getMedicamento().getId());
                        psItem.setInt(3, item.getQuantidadePrescrita());
                        psItem.executeUpdate();
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
                    throw new PersistenciaException("Erro ao reverter transação", ex);
                }
            }
            throw new PersistenciaException("Erro ao inserir receita: " + e.getMessage(), e);
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
    public void atualizar(Receita receita) {
        Objects.requireNonNull(receita, "A receita não pode ser nula");
        if (receita.getId() == null) {
            throw new IllegalArgumentException("O ID da receita não pode ser nulo para atualização.");
        }
        Objects.requireNonNull(receita.getCliente(), "O cliente da receita não pode ser nulo");
        Objects.requireNonNull(receita.getCliente().getId(), "O ID do cliente da receita não pode ser nulo");

        String sqlReceita = "UPDATE receita SET cliente_id = ?, numero = ?, data_emissao = ?, " +
                "nome_medico = ?, crm_medico = ?, retida = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sqlReceita)) {
            ps.setLong(1, receita.getCliente().getId());
            ps.setString(2, receita.getNumero());
            ps.setDate(3, Date.valueOf(receita.getDataEmissao()));
            ps.setString(4, receita.getNomeMedico());
            ps.setString(5, receita.getCrmMedico());
            ps.setBoolean(6, receita.isRetida());
            ps.setLong(7, receita.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao atualizar receita: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Receita> buscarPorId(Long id) {
        Objects.requireNonNull(id, "O ID não pode ser nulo");
        String sql = baseSelectSql() + " WHERE r.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Receita receita = instanciarReceita(rs);
                    carregarItensReceita(receita);
                    return Optional.of(receita);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar receita por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Receita> buscarPorNumero(String numero) {
        Objects.requireNonNull(numero, "O número da receita não pode ser nulo");
        String sql = baseSelectSql() + " WHERE r.numero = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, numero);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Receita receita = instanciarReceita(rs);
                    carregarItensReceita(receita);
                    return Optional.of(receita);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar receita por número: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Receita> listarPorCliente(Long clienteId) {
        Objects.requireNonNull(clienteId, "O ID do cliente não pode ser nulo");
        String sql = baseSelectSql() + " WHERE r.cliente_id = ? ORDER BY r.data_emissao DESC, r.id DESC";
        List<Receita> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Receita receita = instanciarReceita(rs);
                    carregarItensReceita(receita);
                    lista.add(receita);
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar receitas por cliente: " + e.getMessage(), e);
        }
    }

    // Consulta SQL base da receita com INNER JOIN em cliente e pessoa
    private String baseSelectSql() {
        return "SELECT r.id, r.cliente_id, r.numero, r.data_emissao, r.nome_medico, r.crm_medico, r.retida, " +
                "p.nome AS cli_nome, p.cpf AS cli_cpf, p.telefone AS cli_telefone, p.ativo AS cli_ativo, " +
                "c.endereco AS cli_endereco, c.fidelidade_pontos AS cli_pontos " +
                "FROM receita r " +
                "INNER JOIN cliente c ON r.cliente_id = c.pessoa_id " +
                "INNER JOIN pessoa p ON c.pessoa_id = p.id";
    }

    // Mapeia o cabeçalho da Receita e o Cliente associado
    private Receita instanciarReceita(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente(rs.getString("cli_nome"), rs.getString("cli_cpf"));
        cliente.setId(rs.getLong("cliente_id"));
        cliente.setTelefone(rs.getString("cli_telefone"));
        cliente.setAtivo(rs.getBoolean("cli_ativo"));
        cliente.setEndereco(rs.getString("cli_endereco"));
        cliente.setFidelidadePontos(rs.getInt("cli_pontos"));

        Receita receita = new Receita(
                cliente,
                rs.getString("numero"),
                rs.getDate("data_emissao").toLocalDate(),
                rs.getString("nome_medico"),
                rs.getString("crm_medico"),
                rs.getBoolean("retida")
        );
        receita.setId(rs.getLong("id"));
        return receita;
    }

    // Carrega os itens prescritos da receita e reconstitui cada Medicamento associado
    private void carregarItensReceita(Receita receita) throws SQLException {
        String sqlItens = "SELECT ir.quantidade_prescrita, " +
                "p.id AS med_id, p.codigo_barras, p.nome, p.fabricante, p.preco, p.ativo, " +
                "m.dosagem, m.principio_ativo, m.tipo, m.categoria_regulatoria, m.tipo_uso, m.registro_anvisa " +
                "FROM item_receita ir " +
                "INNER JOIN medicamento m ON ir.medicamento_id = m.produto_id " +
                "INNER JOIN produto p ON m.produto_id = p.id " +
                "WHERE ir.receita_id = ? " +
                "ORDER BY p.nome";

        try (PreparedStatement ps = conn.prepareStatement(sqlItens)) {
            ps.setLong(1, receita.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Medicamento med = new Medicamento(
                            rs.getString("nome"),
                            rs.getString("fabricante"),
                            rs.getBigDecimal("preco"),
                            rs.getString("codigo_barras")
                    );
                    med.setId(rs.getLong("med_id"));
                    med.setAtivo(rs.getBoolean("ativo"));
                    med.setDosagem(rs.getString("dosagem"));
                    med.setPrincipioAtivo(rs.getString("principio_ativo"));
                    med.setRegistroAnvisa(rs.getString("registro_anvisa"));

                    String tipoStr = rs.getString("tipo");
                    if (tipoStr != null) {
                        med.setTipo(TipoMedicamento.valueOf(tipoStr));
                    }

                    String catRegStr = rs.getString("categoria_regulatoria");
                    if (catRegStr != null) {
                        med.setCategoriaRegulatoria(CategoriaRegulatoria.valueOf(catRegStr));
                    }

                    String tipoUsoStr = rs.getString("tipo_uso");
                    if (tipoUsoStr != null) {
                        med.setTipoUso(TipoUso.valueOf(tipoUsoStr));
                    }

                    receita.adicionarItem(med, rs.getInt("quantidade_prescrita"));
                }
            }
        }
    }
}