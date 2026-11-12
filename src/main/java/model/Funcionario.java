package model;
import model.enums.CargoFuncionario;

// Herda de Pessoa para os dados de identificação e estende com vínculos corporativos
// (matrícula e cargo) necessários para os colaboradores da farmácia.

public class Funcionario extends Pessoa{

    private String matricula;
    private CargoFuncionario cargo;

    public Funcionario(String nome, String cpf) {
        super(nome, cpf);
    }

    public String getMatricula() {return matricula;}

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public CargoFuncionario getCargo() {
        return cargo;
    }

    public void setCargo(CargoFuncionario cargo) {
        this.cargo = cargo;
    }
}