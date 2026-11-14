package service;

import dao.DaoFactory;
import dao.LoteEstoqueDAO;
import dao.ProdutoDAO;
import exception.RegraNegocioException;
import exception.ValidacaoException;
import model.Cosmetico;
import model.LoteEstoque;
import model.Medicamento;
import model.Produto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class ProdutoService {

    private final ProdutoDAO produtoDAO;
    private final LoteEstoqueDAO loteDAO;

    // MUDANÇA: expressão regular que aceita apenas dígitos (\d+ = um ou mais números).
    // Fica como constante estática para ser compilada uma única vez, e não a cada cadastro.
    private static final Pattern SOMENTE_DIGITOS = Pattern.compile("\\d+");

    public ProdutoService() {
        this(DaoFactory.createProdutoDAO(), DaoFactory.createLoteEstoqueDAO());
    }

    public ProdutoService(ProdutoDAO produtoDAO, LoteEstoqueDAO loteDAO) {
        this.produtoDAO = Objects.requireNonNull(produtoDAO, "ProdutoDAO não pode ser nulo");
        this.loteDAO = Objects.requireNonNull(loteDAO, "LoteEstoqueDAO não pode ser nulo");
    }

    public void cadastrar(Produto produto) {
        validarProduto(produto);
        normalizarDados(produto);

        Optional<Produto> existente = produtoDAO.buscarPorCodigoBarras(produto.getCodigoBarras());
        if (existente.isPresent()) {
            throw new RegraNegocioException("Já existe um produto cadastrado com o código de barras: " + produto.getCodigoBarras());
        }

        produtoDAO.inserir(produto);
    }

    public void atualizar(Produto produto) {
        validarProduto(produto);

        if (produto.getId() == null) {
            throw new ValidacaoException("O ID do produto é obrigatório para atualização.");
        }
        buscarPorId(produto.getId()); // Garante que o produto existe no banco
        normalizarDados(produto);

        Optional<Produto> outroComMesmoCodigo = produtoDAO.buscarPorCodigoBarras(produto.getCodigoBarras());
        if (outroComMesmoCodigo.isPresent() && !outroComMesmoCodigo.get().getId().equals(produto.getId())) {
            throw new RegraNegocioException("O código de barras " + produto.getCodigoBarras() + " já está em uso por outro produto.");
        }

        produtoDAO.atualizar(produto);
    }

    public void desativar(Long id) {
        buscarPorId(id); // Já valida se id == null e se o produto existe
        produtoDAO.desativar(id);
    }

    public Produto buscarPorId(Long id) {
        if (id == null) {
            throw new ValidacaoException("O ID para busca não pode ser nulo.");
        }
        return produtoDAO.buscarPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Produto com ID " + id + " não encontrado."));
    }

    public Produto buscarPorCodigoBarras(String codigoBarras) {
        if (codigoBarras == null || codigoBarras.isBlank()) {
            throw new ValidacaoException("O código de barras informado é inválido.");
        }
        return produtoDAO.buscarPorCodigoBarras(codigoBarras.trim())
                .orElseThrow(() -> new RegraNegocioException("Produto com código de barras " + codigoBarras + " não encontrado."));
    }

    public List<Produto> listarTodos() {
        return produtoDAO.listarTodos();
    }

    public List<Produto> buscarPorNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return listarTodos();
        }
        return produtoDAO.buscarPorNome(nome.trim());
    }

    public List<Medicamento> buscarPorPrincipioAtivo(String principioAtivo) {
        if (principioAtivo == null || principioAtivo.isBlank()) {
            throw new ValidacaoException("O princípio ativo para busca não pode estar vazio.");
        }
        return produtoDAO.buscarPorPrincipioAtivo(principioAtivo.trim());
    }

    /**
     * Calcula o saldo total em estoque somando apenas as quantidades de lotes válidos e ativos.
     */
    public int obterEstoqueTotal(Long produtoId) {
        if (produtoId == null) {
            throw new ValidacaoException("O ID do produto não pode ser nulo para cálculo de estoque.");
        }
        return loteDAO.listarDisponiveisPorProduto(produtoId)
                .stream()
                .mapToInt(LoteEstoque::getQuantidade)
                .sum();
    }

    private void validarProduto(Produto produto) {
        validarDadosComuns(produto);

        if (produto instanceof Medicamento med) {
            validarMedicamento(med);
        } else if (produto instanceof Cosmetico cosm) {
            validarCosmetico(cosm);
        } else {
            throw new ValidacaoException("Tipo de produto desconhecido.");
        }
    }

    private void normalizarDados(Produto produto) {
        produto.setNome(produto.getNome().trim());
        produto.setFabricante(produto.getFabricante().trim());
        produto.setCodigoBarras(produto.getCodigoBarras().trim());

        if (produto instanceof Medicamento med) {
            med.setPrincipioAtivo(med.getPrincipioAtivo().trim());
            med.setDosagem(med.getDosagem().trim());
        }
    }

    private void validarDadosComuns(Produto produto) {
        if (produto == null) {
            throw new ValidacaoException("O produto não pode ser nulo.");
        }
        if (produto.getNome() == null || produto.getNome().isBlank()) {
            throw new ValidacaoException("O nome do produto é obrigatório.");
        }
        if (produto.getFabricante() == null || produto.getFabricante().isBlank()) {
            throw new ValidacaoException("O fabricante do produto é obrigatório.");
        }
        if (produto.getCodigoBarras() == null || produto.getCodigoBarras().isBlank()) {
            throw new ValidacaoException("O código de barras é obrigatório.");
        }

        if (produto.getCodigoBarras() == null || produto.getCodigoBarras().isBlank()) {
            throw new ValidacaoException("O código de barras é obrigatório.");
        }

        // MUDANÇA: nova validação exigida pela issue (formato numérico).
        // Usamos trim() aqui porque o normalizarDados() só roda DEPOIS da validação.
        // Sem o trim, um código como "  789123  " seria rejeitado por causa dos espaços,
        // e o seu teste 2 (que envia espaços de propósito) quebraria.
        if (!SOMENTE_DIGITOS.matcher(produto.getCodigoBarras().trim()).matches()) {
            throw new ValidacaoException("O código de barras deve conter apenas dígitos.");
        }

        if (produto.getPreco() == null || produto.getPreco().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacaoException("O preço do produto deve ser maior que zero.");
        }
    }

    private void validarMedicamento(Medicamento med) {
        if (med.getPrincipioAtivo() == null || med.getPrincipioAtivo().isBlank()) {
            throw new ValidacaoException("O princípio ativo é obrigatório para medicamentos.");
        }
        if (med.getDosagem() == null || med.getDosagem().isBlank()) {
            throw new ValidacaoException("A dosagem é obrigatória para medicamentos.");
        }
        if (med.getTipo() == null) {
            throw new ValidacaoException("O tipo de medicamento (Antibiótico, Anti-inflamatório, etc.) é obrigatório.");
        }
        if (med.getCategoriaRegulatoria() == null) {
            throw new ValidacaoException("A categoria regulatória (Venda Livre, Retenção, etc.) é obrigatória.");
        }
    }

    private void validarCosmetico(Cosmetico cosm) {
        if (cosm.getCategoriaUso() == null) {
            throw new ValidacaoException("A categoria de uso do cosmético (Cabelo, Pele, etc.) é obrigatória.");
        }
    }
}