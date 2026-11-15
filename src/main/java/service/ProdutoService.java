package service;

import dao.DaoFactory;
import dao.ProdutoDAO;
import exception.RegraNegocioException;
import exception.ValidacaoException;
import model.Cosmetico;
import model.Medicamento;
import model.Produto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class ProdutoService {

    private static final Pattern SOMENTE_DIGITOS = Pattern.compile("\\d+");

    private final ProdutoDAO produtoDAO;

    public ProdutoService() {
        this(DaoFactory.createProdutoDAO());
    }

    public ProdutoService(ProdutoDAO produtoDAO) {
        this.produtoDAO = Objects.requireNonNull(produtoDAO, "ProdutoDAO não pode ser nulo");
    }

    public void cadastrar(Produto produto) {
        validarProduto(produto);
        normalizarDados(produto);

        if (produtoDAO.buscarPorCodigoBarras(produto.getCodigoBarras()).isPresent()) {
            throw new RegraNegocioException("Já existe um produto cadastrado com o código de barras: " + produto.getCodigoBarras());
        }

        produtoDAO.inserir(produto);
    }

    public void atualizar(Produto produto) {
        validarProduto(produto);
        exigirId(produto.getId(), "O ID do produto é obrigatório para atualização.");
        buscarPorId(produto.getId()); // garante que o produto existe
        normalizarDados(produto);

        Optional<Produto> outroComMesmoCodigo = produtoDAO.buscarPorCodigoBarras(produto.getCodigoBarras());
        if (outroComMesmoCodigo.isPresent() && !outroComMesmoCodigo.get().getId().equals(produto.getId())) {
            throw new RegraNegocioException("O código de barras " + produto.getCodigoBarras() + " já está em uso por outro produto.");
        }

        produtoDAO.atualizar(produto);
    }

    public void desativar(Long id) {
        buscarPorId(id); // valida id nulo e existência
        produtoDAO.desativar(id);
    }

    public Produto buscarPorId(Long id) {
        exigirId(id, "O ID para busca não pode ser nulo.");
        return produtoDAO.buscarPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Produto com ID " + id + " não encontrado."));
    }

    public Produto buscarPorCodigoBarras(String codigoBarras) {
        exigirPreenchido(codigoBarras, "O código de barras informado é inválido.");
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
        exigirPreenchido(principioAtivo, "O princípio ativo para busca não pode estar vazio.");
        return produtoDAO.buscarPorPrincipioAtivo(principioAtivo.trim());
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
        exigirPreenchido(produto.getNome(), "O nome do produto é obrigatório.");
        exigirPreenchido(produto.getFabricante(), "O fabricante do produto é obrigatório.");
        exigirPreenchido(produto.getCodigoBarras(), "O código de barras é obrigatório.");

        // Validado sobre o valor trimado, pois a normalização ocorre depois
        if (!SOMENTE_DIGITOS.matcher(produto.getCodigoBarras().trim()).matches()) {
            throw new ValidacaoException("O código de barras deve conter apenas dígitos.");
        }
        if (produto.getPreco() == null || produto.getPreco().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacaoException("O preço do produto deve ser maior que zero e não pode ser vazio.");
        }
    }

    private void validarMedicamento(Medicamento med) {
        exigirPreenchido(med.getPrincipioAtivo(), "O princípio ativo é obrigatório para medicamentos.");
        exigirPreenchido(med.getDosagem(), "A dosagem é obrigatória para medicamentos.");
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

    private static void exigirPreenchido(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new ValidacaoException(mensagem);
        }
    }

    private static void exigirId(Long id, String mensagem) {
        if (id == null) {
            throw new ValidacaoException(mensagem);
        }
    }
}