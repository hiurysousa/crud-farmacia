package model.entities;

public abstract class Produto {
    private Long id;
    private String codigoBarras;
    private String nome;
    private String fabricante;
    private double preco;
    private boolean ativo;

    public Produto(String nome, String fabricante, double preco, String codigoBarras) {
        this.nome = nome;
        this.fabricante = fabricante;
        this.preco = preco;
        this.codigoBarras = codigoBarras;
        this.ativo = true; // padrão do banco: DEFAULT TRUE
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getFabricante() {
        return fabricante;
    }

    public void setFabricante(String fabricante) {
        this.fabricante = fabricante;
    }

    public double getPreco() {
        return preco;
    }

    public void setPreco(double preco) {
        this.preco = preco;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
