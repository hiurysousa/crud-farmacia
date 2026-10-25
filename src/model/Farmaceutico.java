package model;
import model.enums.CargoFuncionario;

// Herda de Funcionario porque o farmacêutico possui matrícula e cargo como qualquer
// colaborador, mas exige um identificador regulatório exclusivo (CRF).

public class Farmaceutico extends Funcionario {

    private String crf;

    public Farmaceutico(String nome, String cpf, String crf) {
        super(nome, cpf);
        this.crf = crf;
        setCargo(CargoFuncionario.FARMACEUTICO);
    }

    public String getCrf() {
        return crf;
    }

    public void setCrf(String crf) {
        this.crf = crf;
    }
}
