package model.entities;
import model.entities.enums.CategoriaCosmetico;

// Herda de Produto (dados comerciais) para adicionar classificações de
// beleza e dermatologia que não existem em medicamentos.

public class Cosmetico extends Produto{

    private CategoriaCosmetico categoriaUso;
    private boolean hipoalergenico;

    public Cosmetico(String nome, String fabricante, double preco, String codigoBarras) {
        super(nome, fabricante, preco, codigoBarras);
    }

    public void setHipoalergenico(boolean hipoalergenico) {
        this.hipoalergenico = hipoalergenico;
    }
    public void setCategoriaUso(CategoriaCosmetico categoriaUso) {
        this.categoriaUso = categoriaUso;
    }

    public boolean isHipoalergenico() {
        return hipoalergenico;
    }
    public CategoriaCosmetico getCategoriaUso() {
        return categoriaUso;
    }
}
