package dao.jdbc;

import dao.LoteEstoqueDAO;
import exception.PersistenciaException;
import model.Cosmetico;
import model.LoteEstoque;
import model.Medicamento;
import model.Produto;
import model.enums.CategoriaRegulatoria;
import model.enums.CategoriaUsoCosmetico;
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
 * Implementação JDBC de LoteEstoqueDAO com suporte à estratégia FEFO e controle de validade.
 */
public class LoteEstoqueDAOJdbc implements LoteEstoqueDAO {

    private final Connection conn;

    public LoteEstoqueDAOJdbc(Connection conn) {
        this.conn = Objects.requireNonNull(conn, "A conexão não pode ser nula");
    }

    @Override
    public void inserir(LoteEstoque lote) {
        Objects.requireNonNull(lote, "O lote não pode ser nulo");
        Objects.requireNonNull(lote.getProduto(), "O produto do lote não pode ser nulo");
        Objects.requireNonNull(lote.getProduto().getId(), "O ID do produto do lote não pode ser nulo");
        Objects.requireNonNull(lote.getNumeroLote(), "O número do lote não pode ser nulo");
        Objects.requireNonNull(lote.getDataValidade(), "A data de validade do lote não pode ser nula");

        String sql = "INSERT INTO lote_estoque (produto_id, numero_lote, data_validade, quantidade, ativo) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, lote.getProduto().getId());
            ps.setString(2, lote.getNumeroLote());
            ps.setDate(3, Date.valueOf(lote.getDataValidade()));
            ps.setInt(4, lote.getQuantidade());
            ps.setBoolean(5, lote.isAtivo());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    lote.setId(rs.getLong(1));
                } else {
                    throw new PersistenciaException("Falha ao obter o ID gerado para lote de estoque.");
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao inserir lote de estoque: " + e.getMessage(), e);
        }
    }

    @Override
    public void atualizar(LoteEstoque lote) {
        Objects.requireNonNull(lote, "O lote não pode ser nulo");
        if (lote.getId() == null) {
            throw new IllegalArgumentException("O ID do lote não pode ser nulo para atualização.");
        }
        Objects.requireNonNull(lote.getProduto(), "O produto do lote não pode ser nulo");
        Objects.requireNonNull(lote.getProduto().getId(), "O ID do produto do lote não pode ser nulo");
        Objects.requireNonNull(lote.getNumeroLote(), "O número do lote não pode ser nulo");
        Objects.requireNonNull(lote.getDataValidade(), "A data de validade do lote não pode ser nula");

        String sql = "UPDATE lote_estoque SET produto_id = ?, numero_lote = ?, data_validade = ?, " +
                "quantidade = ?, ativo = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, lote.getProduto().getId());
            ps.setString(2, lote.getNumeroLote());
            ps.setDate(3, Date.valueOf(lote.getDataValidade()));
            ps.setInt(4, lote.getQuantidade());
            ps.setBoolean(5, lote.isAtivo());
            ps.setLong(6, lote.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao atualizar lote de estoque: " + e.getMessage(), e);
        }
    }

    @Override
    public void desativar(Long id) {
        Objects.requireNonNull(id, "O ID não pode ser nulo");
        String sql = "UPDATE lote_estoque SET ativo = FALSE WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao desativar lote de estoque: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<LoteEstoque> buscarPorId(Long id) {
        Objects.requireNonNull(id, "O ID não pode ser nulo");
        String sql = baseSelectSql() + " WHERE l.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(instanciarLoteEstoque(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar lote de estoque por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<LoteEstoque> buscarPorProdutoENumero(Long produtoId, String numeroLote) {
        Objects.requireNonNull(produtoId, "O ID do produto não pode ser nulo");
        Objects.requireNonNull(numeroLote, "O número do lote não pode ser nulo");

        String sql = baseSelectSql() + " WHERE l.produto_id = ? AND l.numero_lote = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, produtoId);
            ps.setString(2, numeroLote);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(instanciarLoteEstoque(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar lote por produto e número: " + e.getMessage(), e);
        }
    }

    @Override
    public List<LoteEstoque> listarPorProduto(Long produtoId) {
        Objects.requireNonNull(produtoId, "O ID do produto não pode ser nulo");

        String sql = baseSelectSql() + " WHERE l.produto_id = ? ORDER BY l.data_validade ASC";
        List<LoteEstoque> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, produtoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(instanciarLoteEstoque(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar lotes por produto: " + e.getMessage(), e);
        }
    }

    @Override
    public List<LoteEstoque> listarDisponiveisPorProduto(Long produtoId) {
        Objects.requireNonNull(produtoId, "O ID do produto não pode ser nulo");

        // FEFO (First Expired, First Out): saldo > 0, dentro da validade (>= CURDATE()), ativos, ordenados pela validade crescente
        String sql = baseSelectSql() +
                " WHERE l.produto_id = ? AND l.ativo = TRUE AND l.quantidade > 0 AND l.data_validade >= CURDATE()" +
                " ORDER BY l.data_validade ASC, l.id ASC";
        List<LoteEstoque> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, produtoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(instanciarLoteEstoque(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar lotes disponíveis por produto: " + e.getMessage(), e);
        }
    }

    @Override
    public List<LoteEstoque> listarVencidos() {
        String sql = baseSelectSql() +
                " WHERE l.data_validade < CURDATE() AND l.quantidade > 0" +
                " ORDER BY l.data_validade ASC, p.nome ASC";
        List<LoteEstoque> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(instanciarLoteEstoque(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar lotes vencidos: " + e.getMessage(), e);
        }
    }

    // Consulta SQL base unindo lote_estoque com produto e suas especializações (medicamento/cosmetico)
    private String baseSelectSql() {
        return "SELECT l.id, l.produto_id, l.numero_lote, l.data_validade, l.quantidade, l.ativo, " +
                "p.codigo_barras, p.nome, p.fabricante, p.preco, p.ativo AS prod_ativo, " +
                "m.produto_id AS med_id, m.dosagem, m.principio_ativo, m.tipo, " +
                "m.categoria_regulatoria, m.tipo_uso, m.registro_anvisa, " +
                "c.produto_id AS cosm_id, c.categoria_uso, c.hipoalergenico " +
                "FROM lote_estoque l " +
                "INNER JOIN produto p ON l.produto_id = p.id " +
                "LEFT JOIN medicamento m ON p.id = m.produto_id " +
                "LEFT JOIN cosmetico c ON p.id = c.produto_id";
    }

    // Mapeia o LoteEstoque e instancia polimorficamente o Produto vinculado
    private LoteEstoque instanciarLoteEstoque(ResultSet rs) throws SQLException {
        Produto produto = instanciarProduto(rs);

        LoteEstoque lote = new LoteEstoque(
                produto,
                rs.getString("numero_lote"),
                rs.getDate("data_validade").toLocalDate(),
                rs.getInt("quantidade")
        );
        lote.setId(rs.getLong("id"));
        lote.setAtivo(rs.getBoolean("ativo"));
        return lote;
    }

    // Mapeamento polimórfico do Produto (Medicamento ou Cosmetico)
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
            throw new PersistenciaException("Produto inconsistente no lote de estoque (ID " + rs.getLong("produto_id") + ").");
        }

        produto.setId(rs.getLong("produto_id"));
        produto.setAtivo(rs.getBoolean("prod_ativo"));
        return produto;
    }
}