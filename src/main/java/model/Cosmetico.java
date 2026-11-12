package model;
import model.enums.CategoriaUsoCosmetico;

import java.math.BigDecimal;

// Herda de Produto (dados comerciais) para adicionar classificações de
// beleza e dermatologia que não existem em medicamentos.

public class Cosmetico extends Produto{

    private CategoriaUsoCosmetico categoriaUso;
    private boolean hipoalergenico;

    public Cosmetico(String nome, String fabricante, BigDecimal preco, String codigoBarras) {
        super(nome, fabricante, preco, codigoBarras);
    }

    public void setHipoalergenico(boolean hipoalergenico) {
        this.hipoalergenico = hipoalergenico;
    }
    public void setCategoriaUso(CategoriaUsoCosmetico categoriaUso) {
        this.categoriaUso = categoriaUso;
    }

    public boolean isHipoalergenico() {
        return hipoalergenico;
    }
    public CategoriaUsoCosmetico getCategoriaUso() {
        return categoriaUso;
    }
}