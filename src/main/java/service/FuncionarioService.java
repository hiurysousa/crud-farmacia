package service;

import dao.DaoFactory;
import dao.FuncionarioDAO;
import exception.RegraNegocioException;
import exception.ValidacaoException;
import model.Farmaceutico;
import model.Funcionario;
import model.enums.CargoFuncionario;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Camada de serviço para gerenciamento de funcionários e farmacêuticos.
 */
public class FuncionarioService {

    private final FuncionarioDAO funcionarioDAO;

    // Padrão esperado para CRF com UF (ex: CE-12345 ou SP-123456)
    private static final Pattern PADRAO_CRF = Pattern.compile("^[A-Z]{2}-\\d{3,10}$");

    public FuncionarioService() {
        this(DaoFactory.createFuncionarioDAO());
    }

    public FuncionarioService(FuncionarioDAO funcionarioDAO) {
        this.funcionarioDAO = Objects.requireNonNull(funcionarioDAO, "FuncionarioDAO não pode ser nulo");
    }

    /**
     * Cadastra um novo funcionário ou farmacêutico após validar os dados obrigatórios,
     * regras de cargo/CRF e garantir a unicidade de CPF, matrícula e CRF no banco.
     */
    public void cadastrar(Funcionario funcionario) {
        validarFuncionario(funcionario);
        normalizarDados(funcionario);

        // 1. Unicidade de CPF
        Optional<Funcionario> existenteCpf = funcionarioDAO.buscarPorCpf(funcionario.getCpf());
        if (existenteCpf.isPresent()) {
            throw new RegraNegocioException("Já existe um funcionário cadastrado com o CPF informado.");
        }

        // 2. Unicidade de Matrícula
        Optional<Funcionario> existenteMatricula = funcionarioDAO.buscarPorMatricula(funcionario.getMatricula());
        if (existenteMatricula.isPresent()) {
            throw new RegraNegocioException("Já existe um funcionário cadastrado com a matrícula: " + funcionario.getMatricula());
        }

        // 3. Unicidade de CRF (quando for farmacêutico)
        if (funcionario instanceof Farmaceutico farmaceutico) {
            Optional<Farmaceutico> existenteCrf = funcionarioDAO.buscarPorCrf(farmaceutico.getCrf());
            if (existenteCrf.isPresent()) {
                throw new RegraNegocioException("Já existe um farmacêutico cadastrado com o CRF: " + farmaceutico.getCrf());
            }
        }

        funcionarioDAO.inserir(funcionario);
    }

    /**
     * Atualiza os dados cadastrais de um funcionário ou farmacêutico existente.
     */
    public void atualizar(Funcionario funcionario) {
        validarFuncionario(funcionario);

        if (funcionario.getId() == null) {
            throw new ValidacaoException("O ID do funcionário é obrigatório para atualização.");
        }
        buscarPorId(funcionario.getId()); // Garante que existe no banco
        normalizarDados(funcionario);

        // 1. Unicidade de CPF
        Optional<Funcionario> outroCpf = funcionarioDAO.buscarPorCpf(funcionario.getCpf());
        if (outroCpf.isPresent() && !outroCpf.get().getId().equals(funcionario.getId())) {
            throw new RegraNegocioException("O CPF informado já está em uso por outro funcionário.");
        }

        // 2. Unicidade de Matrícula
        Optional<Funcionario> outraMatricula = funcionarioDAO.buscarPorMatricula(funcionario.getMatricula());
        if (outraMatricula.isPresent() && !outraMatricula.get().getId().equals(funcionario.getId())) {
            throw new RegraNegocioException("A matrícula " + funcionario.getMatricula() + " já está em uso por outro funcionário.");
        }

        // 3. Unicidade de CRF
        if (funcionario instanceof Farmaceutico farmaceutico) {
            Optional<Farmaceutico> outroCrf = funcionarioDAO.buscarPorCrf(farmaceutico.getCrf());
            if (outroCrf.isPresent() && !outroCrf.get().getId().equals(funcionario.getId())) {
                throw new RegraNegocioException("O CRF " + farmaceutico.getCrf() + " já está em uso por outro farmacêutico.");
            }
        }

        funcionarioDAO.atualizar(funcionario);
    }

    /**
     * Desativa logicamente o funcionário (ativo = false), preservando o histórico de vendas.
     */
    public void desativar(Long id) {
        Funcionario funcionario = buscarPorId(id);
        if (!funcionario.isAtivo()) {
            throw new RegraNegocioException("Este funcionário já se encontra desativado.");
        }
        funcionarioDAO.desativar(id);
    }

    public Funcionario buscarPorId(Long id) {
        if (id == null) {
            throw new ValidacaoException("O ID do funcionário não pode ser nulo.");
        }
        return funcionarioDAO.buscarPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Funcionário com ID " + id + " não encontrado."));
    }

    public Funcionario buscarPorMatricula(String matricula) {
        if (matricula == null || matricula.isBlank()) {
            throw new ValidacaoException("A matrícula informada é inválida.");
        }
        return funcionarioDAO.buscarPorMatricula(matricula.trim().toUpperCase())
                .orElseThrow(() -> new RegraNegocioException("Funcionário com matrícula " + matricula + " não encontrado."));
    }

    public Farmaceutico buscarPorCrf(String crf) {
        if (crf == null || crf.isBlank()) {
            throw new ValidacaoException("O CRF informado é inválido.");
        }
        return funcionarioDAO.buscarPorCrf(crf.trim().toUpperCase())
                .orElseThrow(() -> new RegraNegocioException("Farmacêutico com CRF " + crf + " não encontrado."));
    }

    public List<Funcionario> listarTodos() {
        return funcionarioDAO.listarTodos();
    }

    public List<Funcionario> buscarPorNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return listarTodos();
        }
        String termo = nome.trim().toLowerCase();
        return listarTodos().stream()
                .filter(f -> f.getNome() != null && f.getNome().toLowerCase().contains(termo))
                .toList();
    }

    // =========================================================================
    // MÉTODOS AUXILIARES DE VALIDAÇÃO E NORMALIZAÇÃO
    // =========================================================================

    private void validarFuncionario(Funcionario funcionario) {
        if (funcionario == null) {
            throw new ValidacaoException("O funcionário não pode ser nulo.");
        }
        if (funcionario.getNome() == null || funcionario.getNome().isBlank()) {
            throw new ValidacaoException("O nome do funcionário é obrigatório.");
        }

        limparEValidarCpf(funcionario.getCpf());

        if (funcionario.getMatricula() == null || funcionario.getMatricula().isBlank()) {
            throw new ValidacaoException("A matrícula do funcionário é obrigatória.");
        }
        if (funcionario.getCargo() == null) {
            throw new ValidacaoException("O cargo do funcionário é obrigatório.");
        }

        // Regra de cargos: FARMACEUTICO <-> instância de Farmaceutico com CRF
        if (funcionario.getCargo() == CargoFuncionario.FARMACEUTICO) {
            if (!(funcionario instanceof Farmaceutico farmaceutico)) {
                throw new RegraNegocioException(
                        "Funcionários com cargo FARMACEUTICO devem ser cadastrados como uma instância de Farmaceutico com CRF.");
            }
            validarCrf(farmaceutico.getCrf());
        } else if (funcionario instanceof Farmaceutico) {
            throw new RegraNegocioException(
                    "Uma instância de Farmaceutico deve possuir obrigatoriamente o cargo FARMACEUTICO.");
        }
    }

    private void validarCrf(String crf) {
        if (crf == null || crf.isBlank()) {
            throw new ValidacaoException("O CRF é obrigatório para farmacêuticos.");
        }
        String crfNormalizado = crf.trim().toUpperCase();
        if (!PADRAO_CRF.matcher(crfNormalizado).matches()) {
            throw new ValidacaoException("O CRF deve seguir o padrão UF-Número (exemplo: CE-12345).");
        }
    }

    private void normalizarDados(Funcionario funcionario) {
        funcionario.setNome(funcionario.getNome().trim());
        funcionario.setCpf(somenteDigitos(funcionario.getCpf()));
        funcionario.setMatricula(funcionario.getMatricula().trim().toUpperCase());

        if (funcionario.getTelefone() != null) {
            String tel = funcionario.getTelefone().trim();
            funcionario.setTelefone(tel.isEmpty() ? null : tel);
        }

        if (funcionario instanceof Farmaceutico farmaceutico) {
            farmaceutico.setCrf(farmaceutico.getCrf().trim().toUpperCase());
        }
    }

    private String limparEValidarCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            throw new ValidacaoException("O CPF do funcionário é obrigatório.");
        }

        String digitos = somenteDigitos(cpf);
        if (!isCpfValido(digitos)) {
            throw new ValidacaoException("O CPF informado é inválido.");
        }
        return digitos;
    }

    private String somenteDigitos(String valor) {
        return valor.replaceAll("\\D", "");
    }

    private boolean isCpfValido(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return false;
        }
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }

        int soma = 0;
        for (int i = 0; i < 9; i++) {
            soma += (cpf.charAt(i) - '0') * (10 - i);
        }
        int resto = soma % 11;
        int digito1 = (resto < 2) ? 0 : (11 - resto);
        if ((cpf.charAt(9) - '0') != digito1) {
            return false;
        }

        soma = 0;
        for (int i = 0; i < 10; i++) {
            soma += (cpf.charAt(i) - '0') * (11 - i);
        }
        resto = soma % 11;
        int digito2 = (resto < 2) ? 0 : (11 - resto);

        return (cpf.charAt(10) - '0') == digito2;
    }
}