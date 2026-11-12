package model;

import model.enums.StatusVenda;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private BigDecimal valorTotal;
    private final List<ItemVenda> itens = new ArrayList<>();

    public Venda(Funcionario funcionario) {
        this.funcionario = funcionario;
        this.dataHora = LocalDateTime.now();      // padrão do banco: CURRENT_TIMESTAMP
        this.status = StatusVenda.EM_ABERTO;      // padrão do banco
        this.valorTotal = BigDecimal.ZERO;                    // padrão do banco
    }

    // O valorUnitario deve vir de produto.getPreco() no momento da venda (RN09).
    public void adicionarItem(Produto produto, LoteEstoque lote, int quantidade, BigDecimal valorUnitario) {
        itens.add(new ItemVenda(this, produto, lote, quantidade, valorUnitario));
    }

    // Soma os subtotais e atualiza valorTotal (RN10: o total é calculado, não recebido da tela)
    public BigDecimal calcularTotal() {
        this.valorTotal = itens.stream()
                .map(ItemVenda::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return this.valorTotal;
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

    public BigDecimal getValorTotal() { return valorTotal; }

    // Uso restrito ao DAO. O fluxo de venda usa calcularTotal().
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
}