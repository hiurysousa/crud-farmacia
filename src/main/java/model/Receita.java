package model;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Receita {

    private Long id;
    private Cliente cliente;
    private String numero;
    private LocalDate dataEmissao;
    private String nomeMedico;
    private String crmMedico;
    private boolean retida;
    private final List<ItemReceita> itens = new ArrayList<>();

    public Receita( Cliente cliente, String numero, LocalDate dataEmissao, String nomeMedico, String crmMedico, boolean retida) {
        this.cliente = cliente;
        this.numero = numero;
        this.dataEmissao = dataEmissao;
        this.nomeMedico = nomeMedico;
        this.crmMedico = crmMedico;
        this.retida = retida;
    }


    // Cria o item e o vincula a esta receita
    public void adicionarItem(Medicamento medicamento, int quantidadePrescrita) {
        itens.add(new ItemReceita(this, medicamento, quantidadePrescrita));
    }

    // Lista somente leitura: as alterações passam por adicionarItem
    public List<ItemReceita> getItens() { return Collections.unmodifiableList(itens); }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public LocalDate getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(LocalDate dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public String getNomeMedico() {
        return nomeMedico;
    }

    public void setNomeMedico(String nomeMedico) {
        this.nomeMedico = nomeMedico;
    }

    public String getCrmMedico() {
        return crmMedico;
    }

    public void setCrmMedico(String crmMedico) {
        this.crmMedico = crmMedico;
    }

    public boolean isRetida() {
        return retida;
    }

    public void setRetida(boolean retida) {
        this.retida = retida;
    }
}
