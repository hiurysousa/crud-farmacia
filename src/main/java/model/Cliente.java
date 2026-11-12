package model;

// Herda de Pessoa para reaproveitar os dados cadastrais básicos (nome, cpf, telefone)
// e adiciona características específicas de quem consome na farmácia (endereço e pontos).

public class Cliente extends Pessoa{

    private String endereco;
    private int fidelidadePontos;

    public Cliente(String nome, String cpf) {
        super(nome, cpf);
        this.fidelidadePontos = 0; //Padrão do banco: DEFAULT 0
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public int getFidelidadePontos() {
        return fidelidadePontos;
    }

    public void setFidelidadePontos(int fidelidadePontos) {
        this.fidelidadePontos = fidelidadePontos;
    }
}
