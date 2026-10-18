package model.entities;

import model.entities.enums.StatusVenda;
import java.time.LocalDateTime;

// Registra um atendimento. Cliente, receita e farmacêutico são opcionais,
// conforme as regras do banco; funcionário, data e status são obrigatórios.

public class Venda {
    private Long id;
    private LocalDateTime dataHora;
    private Cliente cliente;
    private Funcionario funcionario;
    private Receita receita;
    private Farmaceutico farmaceutico;
    private StatusVenda status;
    private double valorTotal;

    public Venda(Funcionario funcionario) {
        this.funcionario = funcionario;
        this.dataHora = LocalDateTime.now();      // padrão do banco: CURRENT_TIMESTAMP
        this.status = StatusVenda.EM_ABERTO;      // padrão do banco
        this.valorTotal = 0.0;                    // padrão do banco
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Funcionario getFuncionario() { return funcionario; }
    public void setFuncionario(Funcionario funcionario) { this.funcionario = funcionario; }

    public Receita getReceita() { return receita; }
    public void setReceita(Receita receita) { this.receita = receita; }

    public Farmaceutico getFarmaceutico() { return farmaceutico; }
    public void setFarmaceutico(Farmaceutico farmaceutico) { this.farmaceutico = farmaceutico; }

    public StatusVenda getStatus() { return status; }
    public void setStatus(StatusVenda status) { this.status = status; }

    public double getValorTotal() { return valorTotal; }
    public void setValorTotal(double valorTotal) { this.valorTotal = valorTotal; }
}