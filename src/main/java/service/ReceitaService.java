package service;

import dao.ClienteDAO;
import dao.DaoFactory;
import dao.ProdutoDAO;
import dao.ReceitaDAO;
import exception.RegraNegocioException;
import exception.ValidacaoException;
import model.Cliente;
import model.ItemReceita;
import model.Medicamento;
import model.Produto;
import model.Receita;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Camada de serviço para registro, validação e controle de retenção de receitas médicas.
 */
public class ReceitaService {

    private final ReceitaDAO receitaDAO;
    private final ClienteDAO clienteDAO;
    private final ProdutoDAO produtoDAO;

    // Padrão esperado para CRM com UF (ex: CRM-CE 12345 ou CE-12345)
    private static final Pattern PADRAO_CRM = Pattern.compile("^(?:CRM[-\\s]?)?[A-Z]{2}[-\\s]?\\d{3,10}$");

    public ReceitaService() {
        this(
                DaoFactory.createReceitaDAO(),
                DaoFactory.createClienteDAO(),
                DaoFactory.createProdutoDAO()
        );
    }

    public ReceitaService(ReceitaDAO receitaDAO, ClienteDAO clienteDAO, ProdutoDAO produtoDAO) {
        this.receitaDAO = Objects.requireNonNull(receitaDAO, "ReceitaDAO não pode ser nulo");
        this.clienteDAO = Objects.requireNonNull(clienteDAO, "ClienteDAO não pode ser nulo");
        this.produtoDAO = Objects.requireNonNull(produtoDAO, "ProdutoDAO não pode ser nulo");
    }

    /**
     * Registra uma nova receita médica após validar:
     * - Existência e situação ativa do cliente;
     * - Dados do médico e formato do CRM com UF;
     * - Presença de pelo menos 1 medicamento válido com quantidade > 0 (sem duplicidade na mesma receita).
     */
    public void registrar(Receita receita) {
        validarReceita(receita);
        normalizarDados(receita);

        // 1. Garante que o cliente associado existe e está ativo
        Cliente cliente = clienteDAO.buscarPorId(receita.getCliente().getId())
                .orElseThrow(() -> new RegraNegocioException("Cliente associado à receita não foi encontrado."));

        if (!cliente.isAtivo()) {
            throw new RegraNegocioException("Não é possível registrar receita para um cliente desativado.");
        }

        // 2. Garante que todos os medicamentos prescritos existem no catálogo e são medicamentos válidos
        validarItensNoBanco(receita.getItens());

        receitaDAO.inserir(receita);
    }

    /**
     * Marca a receita como retida (retida = true) após sua utilização em venda,
     * impedindo a reutilização indevida de uma receita já retida.
     */
    public void reterReceita(Long receitaId) {
        Receita receita = buscarPorId(receitaId);

        if (receita.isRetida()) {
            throw new RegraNegocioException("Esta receita já foi retida anteriormente e não pode ser reutilizada.");
        }

        receita.setRetida(true);
        receitaDAO.atualizar(receita);
    }

    /**
     * Verifica se uma receita está apta para uso em uma venda (existe, pertence ao cliente e não está retida).
     */
    public void validarParaVenda(Long receitaId, Long clienteId) {
        if (clienteId == null) {
            throw new RegraNegocioException("Vendas com receita exigem um cliente identificado.");
        }

        Receita receita = buscarPorId(receitaId);

        if (receita.getCliente() == null || !clienteId.equals(receita.getCliente().getId())) {
            throw new RegraNegocioException("A receita informada pertence a outro cliente.");
        }

        if (receita.isRetida()) {
            throw new RegraNegocioException("A receita informada já se encontra retida e não pode ser utilizada novamente.");
        }
    }

    public Receita buscarPorId(Long id) {
        if (id == null) {
            throw new ValidacaoException("O ID da receita não pode ser nulo.");
        }
        return receitaDAO.buscarPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Receita com ID " + id + " não encontrada."));
    }

    public List<Receita> listarPorCliente(Long clienteId) {
        if (clienteId == null) {
            throw new ValidacaoException("O ID do cliente não pode ser nulo.");
        }
        return receitaDAO.listarPorCliente(clienteId);
    }

    public List<Receita> listarTodas() {
        return receitaDAO.listarTodas();
    }

    // =========================================================================
    // MÉTODOS AUXILIARES DE VALIDAÇÃO E NORMALIZAÇÃO
    // =========================================================================

    private void validarReceita(Receita receita) {
        if (receita == null) {
            throw new ValidacaoException("A receita não pode ser nula.");
        }
        if (receita.getCliente() == null || receita.getCliente().getId() == null) {
            throw new ValidacaoException("A receita deve estar associada a um cliente válido.");
        }
        if (receita.getNumero() == null || receita.getNumero().isBlank()) {
            throw new ValidacaoException("O número da receita é obrigatório.");
        }
        if (receita.getDataEmissao() == null) {
            throw new ValidacaoException("A data de emissão da receita é obrigatória.");
        }
        if (receita.getDataEmissao().isAfter(LocalDate.now())) {
            throw new RegraNegocioException("A data de emissão da receita não pode ser futura.");
        }
        if (receita.getNomeMedico() == null || receita.getNomeMedico().isBlank()) {
            throw new ValidacaoException("O nome do médico prescritor é obrigatório.");
        }

        validarCrm(receita.getCrmMedico());
        validarItensPrescritos(receita.getItens());
    }

    private void validarCrm(String crm) {
        if (crm == null || crm.isBlank()) {
            throw new ValidacaoException("O CRM do médico é obrigatório.");
        }
        String crmNormalizado = crm.trim().toUpperCase();
        if (!PADRAO_CRM.matcher(crmNormalizado).matches()) {
            throw new ValidacaoException("O CRM do médico deve conter a UF e o número (exemplo: CE-12345 ou CRM-CE 12345).");
        }
    }

    private void validarItensPrescritos(List<ItemReceita> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new RegraNegocioException("A receita deve conter pelo menos 1 medicamento prescrito.");
        }

        Set<Long> medicamentosVistos = new HashSet<>();

        for (ItemReceita item : itens) {
            if (item == null) {
                throw new ValidacaoException("Item da receita não pode ser nulo.");
            }
            if (item.getMedicamento() == null || item.getMedicamento().getId() == null) {
                throw new ValidacaoException("Todo item da receita deve referenciar um medicamento válido.");
            }
            if (item.getQuantidadePrescrita() <= 0) {
                throw new ValidacaoException("A quantidade prescrita do medicamento deve ser maior que zero.");
            }
            // Protege a PK composta (receita_id, medicamento_id) de duplicatas na lista
            if (!medicamentosVistos.add(item.getMedicamento().getId())) {
                throw new RegraNegocioException("O medicamento ID " + item.getMedicamento().getId()
                        + " foi informado mais de uma vez na mesma receita.");
            }
        }
    }

    private void validarItensNoBanco(List<ItemReceita> itens) {
        for (ItemReceita item : itens) {
            Long medId = item.getMedicamento().getId();
            Produto produto = produtoDAO.buscarPorId(medId)
                    .orElseThrow(() -> new RegraNegocioException("Medicamento ID " + medId + " não encontrado no catálogo."));

            if (!(produto instanceof Medicamento)) {
                throw new RegraNegocioException("O produto '" + produto.getNome() + "' não é um medicamento e não pode constar em receita.");
            }
            if (!produto.isAtivo()) {
                throw new RegraNegocioException("O medicamento '" + produto.getNome() + "' está desativado no catálogo.");
            }
        }
    }

    private void normalizarDados(Receita receita) {
        receita.setNumero(receita.getNumero().trim());
        receita.setNomeMedico(receita.getNomeMedico().trim());
        receita.setCrmMedico(receita.getCrmMedico().trim().toUpperCase());
    }
}