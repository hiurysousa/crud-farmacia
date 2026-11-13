package dao.jdbc;

import dao.ProdutoDAO;
import exception.PersistenciaException;
import model.Cosmetico;
import model.Medicamento;
import model.Produto;
import model.enums.CategoriaRegulatoria;
import model.enums.CategoriaUsoCosmetico;
import model.enums.TipoMedicamento;
import model.enums.TipoUso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementação JDBC de ProdutoDAO com suporte a herança polimórfica.
 */
public class ProdutoDAOJdbc implements ProdutoDAO {

    private final Connection conn;

    public ProdutoDAOJdbc(Connection conn) {
        this.conn = Objects.requireNonNull(conn, "A conexão não pode ser nula");
    }

    @Override
    public void inserir(Produto produto) {
        Objects.requireNonNull(produto, "O produto não pode ser nulo");

        String sqlProduto = "INSERT INTO produto (codigo_barras, nome, fabricante, preco, ativo) VALUES (?, ?, ?, ?, ?)";
        String sqlMedicamento = "INSERT INTO medicamento (produto_id, dosagem, principio_ativo, tipo, categoria_regulatoria, tipo_uso, registro_anvisa) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlCosmetico = "INSERT INTO cosmetico (produto_id, categoria_uso, hipoalergenico) VALUES (?, ?, ?)";

        boolean autoCommitOriginal = true;
        try {
            autoCommitOriginal = conn.getAutoCommit();
            if (autoCommitOriginal) {
                conn.setAutoCommit(false);
            }

            // 1. Inserir na tabela produto
            try (PreparedStatement psProduto = conn.prepareStatement(sqlProduto, Statement.RETURN_GENERATED_KEYS)) {
                psProduto.setString(1, produto.getCodigoBarras());
                psProduto.setString(2, produto.getNome());
                psProduto.setString(3, produto.getFabricante());
                psProduto.setBigDecimal(4, produto.getPreco());
                psProduto.setBoolean(5, produto.isAtivo());
                psProduto.executeUpdate();

                try (ResultSet rs = psProduto.getGeneratedKeys()) {
                    if (rs.next()) {
                        produto.setId(rs.getLong(1));
                    } else {
                        throw new PersistenciaException("Falha ao obter o ID gerado para produto.");
                    }
                }
            }

            // 2. Inserir na tabela filha correspondente (medicamento ou cosmetico)
            if (produto instanceof Medicamento med) {
                Objects.requireNonNull(med.getPrincipioAtivo(), "O princípio ativo do medicamento não pode ser nulo");
                Objects.requireNonNull(med.getTipo(), "O tipo do medicamento não pode ser nulo");
                Objects.requireNonNull(med.getCategoriaRegulatoria(), "A categoria regulatória do medicamento não pode ser nula");

                try (PreparedStatement psMed = conn.prepareStatement(sqlMedicamento)) {
                    psMed.setLong(1, med.getId());
                    psMed.setString(2, med.getDosagem());
                    psMed.setString(3, med.getPrincipioAtivo());
                    psMed.setString(4, med.getTipo().name());
                    psMed.setString(5, med.getCategoriaRegulatoria().name());

                    if (med.getTipoUso() != null) {
                        psMed.setString(6, med.getTipoUso().name());
                    } else {
                        psMed.setNull(6, Types.VARCHAR);
                    }

                    psMed.setString(7, med.getRegistroAnvisa());
                    psMed.executeUpdate();
                }
            } else if (produto instanceof Cosmetico cosm) {
                Objects.requireNonNull(cosm.getCategoriaUso(), "A categoria de uso do cosmético não pode ser nula");

                try (PreparedStatement psCosm = conn.prepareStatement(sqlCosmetico)) {
                    psCosm.setLong(1, cosm.getId());
                    psCosm.setString(2, cosm.getCategoriaUso().name());
                    psCosm.setBoolean(3, cosm.isHipoalergenico());
                    psCosm.executeUpdate();
                }
            } else {
                throw new IllegalArgumentException("Subtipo de Produto não suportado: " + produto.getClass().getName());
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
            throw new PersistenciaException("Erro ao inserir produto: " + e.getMessage(), e);
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
    public void atualizar(Produto produto) {
        Objects.requireNonNull(produto, "O produto não pode ser nulo");
        if (produto.getId() == null) {
            throw new IllegalArgumentException("O ID do produto não pode ser nulo para atualização.");
        }

        String sqlProduto = "UPDATE produto SET codigo_barras = ?, nome = ?, fabricante = ?, preco = ?, ativo = ? WHERE id = ?";
        String sqlMedicamento = "UPDATE medicamento SET dosagem = ?, principio_ativo = ?, tipo = ?, categoria_regulatoria = ?, tipo_uso = ?, registro_anvisa = ? WHERE produto_id = ?";
        String sqlCosmetico = "UPDATE cosmetico SET categoria_uso = ?, hipoalergenico = ? WHERE produto_id = ?";

        boolean autoCommitOriginal = true;
        try {
            autoCommitOriginal = conn.getAutoCommit();
            if (autoCommitOriginal) {
                conn.setAutoCommit(false);
            }

            // 1. Atualizar tabela produto
            try (PreparedStatement psProduto = conn.prepareStatement(sqlProduto)) {
                psProduto.setString(1, produto.getCodigoBarras());
                psProduto.setString(2, produto.getNome());
                psProduto.setString(3, produto.getFabricante());
                psProduto.setBigDecimal(4, produto.getPreco());
                psProduto.setBoolean(5, produto.isAtivo());
                psProduto.setLong(6, produto.getId());
                psProduto.executeUpdate();
            }

            // 2. Atualizar tabela específica (medicamento ou cosmetico)
            if (produto instanceof Medicamento med) {
                Objects.requireNonNull(med.getPrincipioAtivo(), "O princípio ativo do medicamento não pode ser nulo");
                Objects.requireNonNull(med.getTipo(), "O tipo do medicamento não pode ser nulo");
                Objects.requireNonNull(med.getCategoriaRegulatoria(), "A categoria regulatória do medicamento não pode ser nula");

                try (PreparedStatement psMed = conn.prepareStatement(sqlMedicamento)) {
                    psMed.setString(1, med.getDosagem());
                    psMed.setString(2, med.getPrincipioAtivo());
                    psMed.setString(3, med.getTipo().name());
                    psMed.setString(4, med.getCategoriaRegulatoria().name());

                    if (med.getTipoUso() != null) {
                        psMed.setString(5, med.getTipoUso().name());
                    } else {
                        psMed.setNull(5, Types.VARCHAR);
                    }

                    psMed.setString(6, med.getRegistroAnvisa());
                    psMed.setLong(7, med.getId());
                    psMed.executeUpdate();
                }
            } else if (produto instanceof Cosmetico cosm) {
                Objects.requireNonNull(cosm.getCategoriaUso(), "A categoria de uso do cosmético não pode ser nula");

                try (PreparedStatement psCosm = conn.prepareStatement(sqlCosmetico)) {
                    psCosm.setString(1, cosm.getCategoriaUso().name());
                    psCosm.setBoolean(2, cosm.isHipoalergenico());
                    psCosm.setLong(3, cosm.getId());
                    psCosm.executeUpdate();
                }
            } else {
                throw new IllegalArgumentException("Subtipo de Produto não suportado: " + produto.getClass().getName());
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
            throw new PersistenciaException("Erro ao atualizar produto: " + e.getMessage(), e);
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
        String sql = "UPDATE produto SET ativo = FALSE WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao desativar produto: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Produto> buscarPorId(Long id) {
        Objects.requireNonNull(id, "O ID não pode ser nulo");
        String sql = baseSelectSql() + " WHERE p.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(instanciarProduto(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar produto por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Produto> buscarPorCodigoBarras(String codigoBarras) {
        Objects.requireNonNull(codigoBarras, "O código de barras não pode ser nulo");
        String sql = baseSelectSql() + " WHERE p.codigo_barras = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigoBarras);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(instanciarProduto(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar produto por código de barras: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Produto> listarTodos() {
        String sql = baseSelectSql() + " WHERE p.ativo = TRUE ORDER BY p.nome";
        List<Produto> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(instanciarProduto(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar produtos: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Produto> buscarPorNome(String nome) {
        Objects.requireNonNull(nome, "O nome não pode ser nulo");
        String sql = baseSelectSql() + " WHERE p.ativo = TRUE AND p.nome LIKE ? ORDER BY p.nome";
        List<Produto> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + nome + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(instanciarProduto(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar produtos por nome: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Medicamento> buscarPorPrincipioAtivo(String principioAtivo) {
        Objects.requireNonNull(principioAtivo, "O princípio ativo não pode ser nulo");
        String sql = baseSelectSql() + " WHERE p.ativo = TRUE AND m.principio_ativo LIKE ? ORDER BY p.nome";
        List<Medicamento> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + principioAtivo + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Produto produto = instanciarProduto(rs);
                    if (produto instanceof Medicamento medicamento) {
                        lista.add(medicamento);
                    }
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar medicamentos por princípio ativo: " + e.getMessage(), e);
        }
    }

    // Consulta SQL base reaproveitada com LEFT JOIN nas duas tabelas filhas
    private String baseSelectSql() {
        return "SELECT p.id, p.codigo_barras, p.nome, p.fabricante, p.preco, p.ativo, " +
                "m.produto_id AS med_id, m.dosagem, m.principio_ativo, m.tipo, " +
                "m.categoria_regulatoria, m.tipo_uso, m.registro_anvisa, " +
                "c.produto_id AS cosm_id, c.categoria_uso, c.hipoalergenico " +
                "FROM produto p " +
                "LEFT JOIN medicamento m ON p.id = m.produto_id " +
                "LEFT JOIN cosmetico c ON p.id = c.produto_id";
    }

    // Mapeamento polimórfico: decide se instancia Medicamento ou Cosmetico
    private Produto instanciarProduto(ResultSet rs) throws SQLException {
        Long medId = rs.getObject("med_id", Long.class);
        Long cosmId = rs.getObject("cosm_id", Long.class);

        Produto produto;

        if (medId != null) {
            Medicamento med = new Medicamento(
                    rs.getString("nome"),
                    rs.getString("fabricante"),
                    rs.getBigDecimal("preco"),
                    rs.getString("codigo_barras")
            );
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

            produto = med;
        } else if (cosmId != null) {
            Cosmetico cosm = new Cosmetico(
                    rs.getString("nome"),
                    rs.getString("fabricante"),
                    rs.getBigDecimal("preco"),
                    rs.getString("codigo_barras")
            );

            String catUsoStr = rs.getString("categoria_uso");
            if (catUsoStr != null) {
                cosm.setCategoriaUso(CategoriaUsoCosmetico.valueOf(catUsoStr));
            }
            cosm.setHipoalergenico(rs.getBoolean("hipoalergenico"));

            produto = cosm;
        } else {
            throw new PersistenciaException("Registro inconsistente na tabela produto (ID " + rs.getLong("id") + "): sem especialização em medicamento ou cosmetico.");
        }

        produto.setId(rs.getLong("id"));
        produto.setAtivo(rs.getBoolean("ativo"));
        return produto;
    }
}