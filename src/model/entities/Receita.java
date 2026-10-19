package model.entities;
import java.util.Date;

public class Receita {

    private Long id;
    private Cliente cliente;
    private String numero;
    private Date dataEmissao;
    private String nomeMedico;
    private String crmMedico;
    private boolean retida;

    public Receita(Long id, Cliente cliente, String numero, Date dataEmissao, String nomeMedico, String crmMedico, boolean retida) {
        this.id = id;
        this.cliente = cliente;
        this.numero = numero;
        this.dataEmissao = dataEmissao;
        this.nomeMedico = nomeMedico;
        this.crmMedico = crmMedico;
        this.retida = false; // padrão do banco: DEFAULT FALSE
    }

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

    public Date getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(Date dataEmissao) {
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
