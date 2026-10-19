package model.entities;

public class ItemVenda {
    private Long id;
    private Venda venda;
    private Produto produto;
    private LoteEstoque lote;
    private int quantidade;
    private double valorUnitario;
    private double subtotal;

    public ItemVenda(Venda venda, Produto produto, LoteEstoque lote, int quantidade, double valorUnitario) {
        this.venda = venda;
        this.produto = produto;
        this.lote = lote;
        this.quantidade = quantidade;
        this.valorUnitario = valorUnitario;
        this.subtotal = quantidade * valorUnitario; // regra do banco: subtotal = quantidade * valor_unitario
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
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public double getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(double valorUnitario) { this.valorUnitario = valorUnitario; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
}