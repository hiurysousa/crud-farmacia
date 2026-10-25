package model;
import java.time.LocalDate;

public class LoteEstoque {
    private Long id;
    private Produto produto;
    private String numeroLote;
    private LocalDate dataValidade;
    private int quantidade;
    private boolean ativo;

    public LoteEstoque(Produto produto, String numeroLote, LocalDate dataValidade, int quantidade) {
        this.produto = produto;
        this.numeroLote = numeroLote;
        this.dataValidade = dataValidade;
        this.quantidade = quantidade;
        this.ativo = true; // padrão do banco: DEFAULT TRUE
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public String getNumeroLote() {
        return numeroLote;
    }

    public void setNumeroLote(String numeroLote) {
        this.numeroLote = numeroLote;
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }

    public void setDataValidade(LocalDate dataValidade) {
        this.dataValidade = dataValidade;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
