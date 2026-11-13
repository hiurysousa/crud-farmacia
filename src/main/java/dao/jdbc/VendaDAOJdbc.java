package dao.jdbc;

import dao.VendaDAO;
import exception.PersistenciaException;
import model.Cliente;
import model.Cosmetico;
import model.Farmaceutico;
import model.Funcionario;
import model.ItemVenda;
import model.LoteEstoque;
import model.Medicamento;
import model.Produto;
import model.Receita;
import model.Venda;
import model.enums.CargoFuncionario;
import model.enums.CategoriaRegulatoria;
import model.enums.CategoriaUsoCosmetico;
import model.enums.StatusVenda;
import model.enums.TipoMedicamento;
import model.enums.TipoUso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementação JDBC de VendaDAO com persistência transacional de cabeçalho e itens.
 */
public class VendaDAOJdbc implements VendaDAO {

    private final Connection conn;

    public VendaDAOJdbc(Connection conn) {
        this.conn = Objects.requireNonNull(conn, "A conexão não pode ser nula");
    }

    @Override
    public void inserir(Venda venda) {
        Objects.requireNonNull(venda, "A venda não pode ser nula");
        Objects.requireNonNull(venda.getFuncionario(), "O funcionário da venda não pode ser nulo");
        Objects.requireNonNull(venda.getFuncionario().getId(), "O ID do funcionário da venda não pode ser nulo");
        Objects.requireNonNull(venda.getStatus(), "O status da venda não pode ser nulo");

        String sqlVenda = "INSERT INTO venda (data_hora, cliente_id, funcionario_id, receita_id, farmaceutico_id, status, valor_total) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlItem = "INSERT INTO item_venda (venda_id, produto_id, lote_id, quantidade, valor_unitario, subtotal) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        boolean autoCommitOriginal = true;
        try {
            autoCommitOriginal = conn.getAutoCommit();
            if (autoCommitOriginal) {
                conn.setAutoCommit(false);
            }

            // 1. Inserir cabeçalho da venda
            try (PreparedStatement psVenda = conn.prepareStatement(sqlVenda, Statement.RETURN_GENERATED_KEYS)) {
                LocalDateTime dataHora = venda.getDataHora() != null ? venda.getDataHora() : LocalDateTime.now();
                psVenda.setTimestamp(1, Timestamp.valueOf(dataHora));

                if (venda.getCliente() != null && venda.getCliente().getId() != null) {
                    psVenda.setLong(2, venda.getCliente().getId());
                } else {
                    psVenda.setNull(2, Types.BIGINT);
                }

                psVenda.setLong(3, venda.getFuncionario().getId());

                if (venda.getReceita() != null && venda.getReceita().getId() != null) {
                    psVenda.setLong(4, venda.getReceita().getId());
                } else {
                    psVenda.setNull(4, Types.BIGINT);
                }

                if (venda.getFarmaceutico() != null && venda.getFarmaceutico().getId() != null) {
                    psVenda.setLong(5, venda.getFarmaceutico().getId());
                } else {
                    psVenda.setNull(5, Types.BIGINT);
                }

                psVenda.setString(6, venda.getStatus().name());
                psVenda.setBigDecimal(7, venda.getValorTotal());
                psVenda.executeUpdate();

                try (ResultSet rs = psVenda.getGeneratedKeys()) {
                    if (rs.next()) {
                        venda.setId(rs.getLong(1));
                    } else {
                        throw new PersistenciaException("Falha ao obter o ID gerado para venda.");
                    }
                }
            }

            // 2. Inserir itens da venda
            List<ItemVenda> itens = venda.getItens();
            if (itens != null && !itens.isEmpty()) {
                try (PreparedStatement psItem = conn.prepareStatement(sqlItem, Statement.RETURN_GENERATED_KEYS)) {
                    for (ItemVenda item : itens) {
                        Objects.requireNonNull(item.getProduto(), "O produto do item de venda não pode ser nulo");
                        Objects.requireNonNull(item.getProduto().getId(), "O ID do produto do item de venda não pode ser nulo");
                        Objects.requireNonNull(item.getLote(), "O lote do item de venda não pode ser nulo");
                        Objects.requireNonNull(item.getLote().getId(), "O ID do lote do item de venda não pode ser nulo");

                        psItem.setLong(1, venda.getId());
                        psItem.setLong(2, item.getProduto().getId());
                        psItem.setLong(3, item.getLote().getId());
                        psItem.setInt(4, item.getQuantidade());
                        psItem.setBigDecimal(5, item.getValorUnitario());
                        psItem.setBigDecimal(6, item.getSubtotal());
                        psItem.executeUpdate();

                        try (ResultSet rsItem = psItem.getGeneratedKeys()) {
                            if (rsItem.next()) {
                                item.setId(rsItem.getLong(1));
                            }
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
                    throw new PersistenciaException("Erro ao reverter transação", ex);
                }
            }
            throw new PersistenciaException("Erro ao inserir venda: " + e.getMessage(), e);
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
    public void atualizarStatus(Long id, StatusVenda status) {
        Objects.requireNonNull(id, "O ID da venda não pode ser nulo");
        Objects.requireNonNull(status, "O status da venda não pode ser nulo");

        String sql = "UPDATE venda SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao atualizar status da venda: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Venda> buscarPorId(Long id) {
        Objects.requireNonNull(id, "O ID não pode ser nulo");
        String sql = baseSelectSql() + " WHERE v.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Venda venda = instanciarVenda(rs);
                    carregarItensVenda(venda);
                    return Optional.of(venda);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar venda por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Venda> listarTodas() {
        String sql = baseSelectSql() + " ORDER BY v.data_hora DESC";
        List<Venda> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Venda venda = instanciarVenda(rs);
                carregarItensVenda(venda);
                lista.add(venda);
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar vendas: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Venda> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        Objects.requireNonNull(inicio, "A data/hora de início não pode ser nula");
        Objects.requireNonNull(fim, "A data/hora de fim não pode ser nula");

        String sql = baseSelectSql() + " WHERE v.data_hora BETWEEN ? AND ? ORDER BY v.data_hora DESC";
        List<Venda> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(inicio));
            ps.setTimestamp(2, Timestamp.valueOf(fim));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Venda venda = instanciarVenda(rs);
                    carregarItensVenda(venda);
                    lista.add(venda);
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar vendas por período: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Venda> listarPorCliente(Long clienteId) {
        Objects.requireNonNull(clienteId, "O ID do cliente não pode ser nulo");

        String sql = baseSelectSql() + " WHERE v.cliente_id = ? ORDER BY v.data_hora DESC";
        List<Venda> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Venda venda = instanciarVenda(rs);
                    carregarItensVenda(venda);
                    lista.add(venda);
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar vendas por cliente: " + e.getMessage(), e);
        }
    }

    // Consulta SQL base da venda com LEFT JOIN nas associações opcionais
    private String baseSelectSql() {
        return "SELECT v.id, v.data_hora, v.status, v.valor_total, " +
                "v.cliente_id, pc.nome AS cli_nome, pc.cpf AS cli_cpf, pc.telefone AS cli_telefone, pc.ativo AS cli_ativo, " +
                "c.endereco AS cli_endereco, c.fidelidade_pontos AS cli_pontos, " +
                "v.funcionario_id, pf.nome AS func_nome, pf.cpf AS func_cpf, pf.telefone AS func_telefone, pf.ativo AS func_ativo, " +
                "f.matricula AS func_matricula, f.cargoFuncionario AS func_cargo, fmf.crf AS func_crf, " +
                "v.receita_id, r.numero AS rec_numero, r.data_emissao AS rec_data_emissao, " +
                "r.nome_medico AS rec_nome_medico, r.crm_medico AS rec_crm_medico, r.retida AS rec_retida, " +
                "v.farmaceutico_id, pfarm.nome AS farm_nome, pfarm.cpf AS farm_cpf, pfarm.telefone AS farm_telefone, pfarm.ativo AS farm_ativo, " +
                "ffarm.matricula AS farm_matricula, fmfarm.crf AS farm_crf " +
                "FROM venda v " +
                "INNER JOIN pessoa pf ON v.funcionario_id = pf.id " +
                "INNER JOIN funcionario f ON pf.id = f.pessoa_id " +
                "LEFT JOIN farmaceutico fmf ON f.pessoa_id = fmf.funcionario_id " +
                "LEFT JOIN cliente c ON v.cliente_id = c.pessoa_id " +
                "LEFT JOIN pessoa pc ON c.pessoa_id = pc.id " +
                "LEFT JOIN receita r ON v.receita_id = r.id " +
                "LEFT JOIN farmaceutico fmfarm ON v.farmaceutico_id = fmfarm.funcionario_id " +
                "LEFT JOIN funcionario ffarm ON fmfarm.funcionario_id = ffarm.pessoa_id " +
                "LEFT JOIN pessoa pfarm ON ffarm.pessoa_id = pfarm.id";
    }

    // Mapeia cabeçalho da Venda e suas associações (Cliente, Funcionario, Receita, Farmaceutico)
    private Venda instanciarVenda(ResultSet rs) throws SQLException {
        // 1. Funcionário (obrigatório)
        CargoFuncionario cargoFunc = CargoFuncionario.valueOf(rs.getString("func_cargo"));
        String funcCrf = rs.getString("func_crf");
        Funcionario funcionario;
        if (cargoFunc == CargoFuncionario.FARMACEUTICO || funcCrf != null) {
            funcionario = new Farmaceutico(rs.getString("func_nome"), rs.getString("func_cpf"), funcCrf);
        } else {
            funcionario = new Funcionario(rs.getString("func_nome"), rs.getString("func_cpf"));
            funcionario.setCargo(cargoFunc);
        }
        funcionario.setId(rs.getLong("funcionario_id"));
        funcionario.setTelefone(rs.getString("func_telefone"));
        funcionario.setAtivo(rs.getBoolean("func_ativo"));
        funcionario.setMatricula(rs.getString("func_matricula"));

        Venda venda = new Venda(funcionario);
        venda.setId(rs.getLong("id"));
        Timestamp dataHoraTs = rs.getTimestamp("data_hora");
        if (dataHoraTs != null) {
            venda.setDataHora(dataHoraTs.toLocalDateTime());
        }
        venda.setStatus(StatusVenda.valueOf(rs.getString("status")));
        venda.setValorTotal(rs.getBigDecimal("valor_total"));

        // 2. Cliente (opcional)
        Long clienteId = rs.getObject("cliente_id", Long.class);
        Cliente cliente = null;
        if (clienteId != null) {
            cliente = new Cliente(rs.getString("cli_nome"), rs.getString("cli_cpf"));
            cliente.setId(clienteId);
            cliente.setTelefone(rs.getString("cli_telefone"));
            cliente.setAtivo(rs.getBoolean("cli_ativo"));
            cliente.setEndereco(rs.getString("cli_endereco"));
            cliente.setFidelidadePontos(rs.getInt("cli_pontos"));
            venda.setCliente(cliente);
        }

        // 3. Receita (opcional)
        Long receitaId = rs.getObject("receita_id", Long.class);
        if (receitaId != null) {
            Receita receita = new Receita(
                    cliente,
                    rs.getString("rec_numero"),
                    rs.getDate("rec_data_emissao").toLocalDate(),
                    rs.getString("rec_nome_medico"),
                    rs.getString("rec_crm_medico"),
                    rs.getBoolean("rec_retida")
            );
            receita.setId(receitaId);
            venda.setReceita(receita);
        }

        // 4. Farmacêutico responsável (opcional)
        Long farmaceuticoId = rs.getObject("farmaceutico_id", Long.class);
        if (farmaceuticoId != null) {
            Farmaceutico farmaceutico = new Farmaceutico(
                    rs.getString("farm_nome"),
                    rs.getString("farm_cpf"),
                    rs.getString("farm_crf")
            );
            farmaceutico.setId(farmaceuticoId);
            farmaceutico.setTelefone(rs.getString("farm_telefone"));
            farmaceutico.setAtivo(rs.getBoolean("farm_ativo"));
            farmaceutico.setMatricula(rs.getString("farm_matricula"));
            venda.setFarmaceutico(farmaceutico);
        }

        return venda;
    }

    // Carrega os itens associados à venda, reconstituindo Produto (polimórfico) e LoteEstoque
    private void carregarItensVenda(Venda venda) throws SQLException {
        String sqlItens = "SELECT iv.id AS item_id, iv.quantidade AS item_qtd, iv.valor_unitario, iv.subtotal, " +
                "p.id AS prod_id, p.codigo_barras, p.nome AS prod_nome, p.fabricante, p.preco AS prod_preco, p.ativo AS prod_ativo, " +
                "m.produto_id AS med_id, m.dosagem, m.principio_ativo, m.tipo AS med_tipo, " +
                "m.categoria_regulatoria, m.tipo_uso, m.registro_anvisa, " +
                "c.produto_id AS cosm_id, c.categoria_uso, c.hipoalergenico, " +
                "l.id AS lote_id, l.numero_lote, l.data_validade, l.quantidade AS lote_qtd, l.ativo AS lote_ativo " +
                "FROM item_venda iv " +
                "INNER JOIN produto p ON iv.produto_id = p.id " +
                "LEFT JOIN medicamento m ON p.id = m.produto_id " +
                "LEFT JOIN cosmetico c ON p.id = c.produto_id " +
                "INNER JOIN lote_estoque l ON iv.lote_id = l.id " +
                "WHERE iv.venda_id = ? " +
                "ORDER BY iv.id";

        try (PreparedStatement ps = conn.prepareStatement(sqlItens)) {
            ps.setLong(1, venda.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Produto produto = instanciarProdutoItem(rs);

                    LoteEstoque lote = new LoteEstoque(
                            produto,
                            rs.getString("numero_lote"),
                            rs.getDate("data_validade").toLocalDate(),
                            rs.getInt("lote_qtd")
                    );
                    lote.setId(rs.getLong("lote_id"));
                    lote.setAtivo(rs.getBoolean("lote_ativo"));

                    int quantidade = rs.getInt("item_qtd");
                    venda.adicionarItem(produto, lote, quantidade, rs.getBigDecimal("valor_unitario"));

                    List<ItemVenda> itens = venda.getItens();
                    ItemVenda itemCriado = itens.get(itens.size() - 1);
                    itemCriado.setId(rs.getLong("item_id"));
                    itemCriado.setSubtotal(rs.getBigDecimal("subtotal"));
                }
            }
        }
    }

    // Mapeamento polimórfico do Produto dentro do item da venda
    private Produto instanciarProdutoItem(ResultSet rs) throws SQLException {
        Long medId = rs.getObject("med_id", Long.class);
        Long cosmId = rs.getObject("cosm_id", Long.class);

        Produto produto;
        if (medId != null) {
            Medicamento med = new Medicamento(
                    rs.getString("prod_nome"),
                    rs.getString("fabricante"),
                    rs.getBigDecimal("prod_preco"),
                    rs.getString("codigo_barras")
            );
            med.setDosagem(rs.getString("dosagem"));
            med.setPrincipioAtivo(rs.getString("principio_ativo"));
            med.setRegistroAnvisa(rs.getString("registro_anvisa"));

            String tipoStr = rs.getString("med_tipo");
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
                    rs.getString("prod_nome"),
                    rs.getString("fabricante"),
                    rs.getBigDecimal("prod_preco"),
                    rs.getString("codigo_barras")
            );
            String catUsoStr = rs.getString("categoria_uso");
            if (catUsoStr != null) {
                cosm.setCategoriaUso(CategoriaUsoCosmetico.valueOf(catUsoStr));
            }
            cosm.setHipoalergenico(rs.getBoolean("hipoalergenico"));
            produto = cosm;
        } else {
            throw new PersistenciaException("Produto inconsistente no item de venda (ID " + rs.getLong("prod_id") + ").");
        }

        produto.setId(rs.getLong("prod_id"));
        produto.setAtivo(rs.getBoolean("prod_ativo"));
        return produto;
    }
}