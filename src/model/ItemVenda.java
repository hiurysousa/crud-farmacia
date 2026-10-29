package model;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Fotografia de um item vendido: guarda o preço praticado e o lote utilizado,
// para que o histórico não mude se o preço do produto for alterado depois.

public class ItemVenda {

    private Long id;
    private Venda venda;
    private Produto produto;
    private LoteEstoque lote;
    private int quantidade;
    private BigDecimal valorUnitario;
    private BigDecimal subtotal;

    public ItemVenda(Venda venda, Produto produto, LoteEstoque lote, int quantidade, BigDecimal valorUnitario) {
        this.venda = venda;
        this.produto = produto;
        this.lote = lote;
        this.quantidade = quantidade;
        this.valorUnitario = valorUnitario;
        recalcularSubtotal();
    }

    // Mantém subtotal coerente com quantidade e valorUnitario
    // (regra do banco: subtotal = quantidade * valor_unitario)
    private void recalcularSubtotal() {
        if (valorUnitario == null) {
            this.subtotal = null;
            return;
        }
        this.subtotal = valorUnitario
                .multiply(BigDecimal.valueOf(quantidade))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Venda getVenda() { return venda; }
    public void setVenda(Venda venda) { this.venda = venda; }

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }

    public LoteEstoque getLote() { return lote; }
    public void setLote(LoteEstoque lote) { this.lote = lote; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
        recalcularSubtotal();
    }

    public BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(BigDecimal valorUnitario) {
        this.valorUnitario = valorUnitario;
        recalcularSubtotal();
    }

    public BigDecimal getSubtotal() { return subtotal; }

    // Uso restrito ao DAO: popula o subtotal ao reconstruir uma venda já
    // gravada no banco, preservando o valor histórico sem recalcular.
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}