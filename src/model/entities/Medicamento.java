package model.entities;

import model.entities.enums.TipoMedicamento;
import model.entities.enums.CategoriaRegulatoria;
import model.entities.enums.TipoUso;

// Herda de Produto para absorver os dados comerciais (preço, código de barras) e
// adiciona as propriedades químicas, clínicas e regulatórias do medicamento.

public class Medicamento extends Produto {

    private String dosagem;
    private String principioAtivo;;
    private String registroAnvisa;;
    private TipoUso tipoUso;
    private TipoMedicamento tipo;
    private CategoriaRegulatoria categoriaRegulatoria;

    public Medicamento(String nome, String fabricante, double preco, String codigoBarras) {
        super(nome, fabricante, preco, codigoBarras);
    }

    public String getDosagem() {
        return dosagem;
    }

    public void setDosagem(String dosagem) {
        this.dosagem = dosagem;
    }

    public String getPrincipioAtivo() {
        return principioAtivo;
    }

    public void setPrincipioAtivo(String principioAtivo) {
        this.principioAtivo = principioAtivo;
    }

    public String getRegistroAnvisa() {
        return registroAnvisa;
    }

    public void setRegistroAnvisa(String registroAnvisa) {
        this.registroAnvisa = registroAnvisa;
    }

    public TipoUso getTipoUso() {
        return tipoUso;
    }

    public void setTipoUso(TipoUso tipoUso) {
        this.tipoUso = tipoUso;
    }

    public TipoMedicamento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMedicamento tipo) {
        this.tipo = tipo;
    }

    public CategoriaRegulatoria getCategoriaRegulatoria() {
        return categoriaRegulatoria;
    }

    public void setCategoriaRegulatoria(CategoriaRegulatoria categoriaRegulatoria) {
        this.categoriaRegulatoria = categoriaRegulatoria;
    }
}
