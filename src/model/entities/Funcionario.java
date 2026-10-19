package model.entities;
import model.entities.enums.Cargo;

// Herda de Pessoa para os dados de identificação e estende com vínculos corporativos
// (matrícula e cargo) necessários para os colaboradores da farmácia.

public class Funcionario extends Pessoa{

    private String matricula;
    private Cargo cargo;

    public Funcionario(String nome, String cpf) {
        super(nome, cpf);
    }

    public String getMatricula() {return matricula;}

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public Cargo getCargo() {
        return cargo;
    }

    public void setCargo(Cargo cargo) {
        this.cargo = cargo;
    }
}
