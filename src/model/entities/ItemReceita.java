package model.entities;

public class ItemReceita {
    private Receita receita;
    private Medicamento medicamento;
    private int quantidadePrescrita;


//Sem id, porque a chave da tabela é composta (receita_id, medicamento_id).
    public ItemReceita(Receita receita, Medicamento medicamento, int quantidadePrescrita) {
        this.receita = receita;
        this.medicamento = medicamento;
        this.quantidadePrescrita = quantidadePrescrita;
    }


    public Receita getReceita() { return receita; }
    public void setReceita(Receita receita) { this.receita = receita; }

    public Medicamento getMedicamento() { return medicamento; }
    public void setMedicamento(Medicamento medicamento) { this.medicamento = medicamento; }

    public int getQuantidadePrescrita() { return quantidadePrescrita; }
    public void setQuantidadePrescrita(int quantidadePrescrita) { this.quantidadePrescrita = quantidadePrescrita; }
}