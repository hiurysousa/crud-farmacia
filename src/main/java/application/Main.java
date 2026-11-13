package application;

import dao.ClienteDAO;
import dao.DaoFactory;
import dao.FuncionarioDAO;
import dao.LoteEstoqueDAO;
import dao.ProdutoDAO;
import dao.ReceitaDAO;
import dao.VendaDAO;
import db.DataBase;
import model.Cliente;
import model.Cosmetico;
import model.Farmaceutico;
import model.Funcionario;
import model.ItemVenda;
import model.LoteEstoque;
import model.Medicamento;
import model.Produto;
import model.Receita;
import model.Venda;
import model.enums.CargoFuncionario;
import model.enums.CategoriaRegulatoria;
import model.enums.CategoriaUsoCosmetico;
import model.enums.StatusVenda;
import model.enums.TipoMedicamento;
import model.enums.TipoUso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        // Sufixo curto baseado no tempo atual para evitar violação de UNIQUE ao rodar várias vezes
        String sufixo = String.valueOf(System.currentTimeMillis() % 100000);

        try {
            FuncionarioDAO funcionarioDAO = DaoFactory.createFuncionarioDAO();
            ClienteDAO clienteDAO = DaoFactory.createClienteDAO();
            ProdutoDAO produtoDAO = DaoFactory.createProdutoDAO();
            LoteEstoqueDAO loteDAO = DaoFactory.createLoteEstoqueDAO();
            ReceitaDAO receitaDAO = DaoFactory.createReceitaDAO();
            VendaDAO vendaDAO = DaoFactory.createVendaDAO();

            // =========================================================================
            // 0. PREPARAÇÃO DE ATORES (FUNCIONÁRIO, FARMACÊUTICO E CLIENTE)
            // =========================================================================
            System.out.println("=== 0. PREPARAÇÃO: CADASTRANDO ATORES PARA OS TESTES ===");
            Funcionario atendente = new Funcionario("Carlos Atendente", "100000" + sufixo);
            atendente.setTelefone("85911112222");
            atendente.setMatricula("ATD-" + sufixo);
            atendente.setCargo(CargoFuncionario.ATENDENTE);
            funcionarioDAO.inserir(atendente);

            Farmaceutico farmaceutico = new Farmaceutico("Dra. Juliana", "200000" + sufixo, "CE-" + sufixo);
            farmaceutico.setTelefone("85933334444");
            farmaceutico.setMatricula("FRM-" + sufixo);
            funcionarioDAO.inserir(farmaceutico);

            Cliente cliente = new Cliente("Maria Cliente", "300000" + sufixo);
            cliente.setTelefone("85988887777");
            cliente.setEndereco("Av. Principal, 1000 - Centro");
            cliente.setFidelidadePontos(10);
            clienteDAO.inserir(cliente);

            System.out.println(" Atores criados -> Atendente ID: " + atendente.getId()
                    + " | Farmacêutico ID: " + farmaceutico.getId()
                    + " | Cliente ID: " + cliente.getId());

            // =========================================================================
            // 1. TESTE: PRODUTO DAO (POLIMORFISMO MEDICAMENTO E COSMÉTICO)
            // =========================================================================
            System.out.println("\n=== 1. TESTE: PRODUTO DAO ===");
            Medicamento amoxicilina = new Medicamento(
                    "Amoxicilina 500mg",
                    "Medley",
                    new BigDecimal("42.50"),
                    "7891000" + sufixo
            );
            amoxicilina.setDosagem("500mg - 21 cápsulas");
            amoxicilina.setPrincipioAtivo("Amoxicilina Tri-hidratada");
            amoxicilina.setTipo(TipoMedicamento.ANTIBIOTICO);
            amoxicilina.setCategoriaRegulatoria(CategoriaRegulatoria.RETENCAO);
            amoxicilina.setTipoUso(TipoUso.ORAL);
            amoxicilina.setRegistroAnvisa("1.0181.0456");

            produtoDAO.inserir(amoxicilina);
            System.out.println(" Medicamento inserido com ID: " + amoxicilina.getId());

            Cosmetico protetorSolar = new Cosmetico(
                    "Protetor Solar FPS 60",
                    "La Roche-Posay",
                    new BigDecimal("89.90"),
                    "7892000" + sufixo
            );
            protetorSolar.setCategoriaUso(CategoriaUsoCosmetico.PELE);
            protetorSolar.setHipoalergenico(true);

            produtoDAO.inserir(protetorSolar);
            System.out.println(" Cosmético inserido com ID: " + protetorSolar.getId());

            // Testando busca polimórfica por código de barras
            Optional<Produto> prodBuscado = produtoDAO.buscarPorCodigoBarras(amoxicilina.getCodigoBarras());
            if (prodBuscado.isPresent() && prodBuscado.get() instanceof Medicamento medRecuperado) {
                System.out.println(" Polimorfismo ProdutoDAO OK -> Medicamento: " + medRecuperado.getNome()
                        + " | Princípio Ativo: " + medRecuperado.getPrincipioAtivo());
            } else {
                System.err.println(" Erro: Falha ao recuperar Medicamento polimorficamente!");
            }

            // Testando busca por princípio ativo
            List<Medicamento> medsPorPrincipio = produtoDAO.buscarPorPrincipioAtivo("Amoxicilina");
            System.out.println(" Medicamentos encontrados para 'Amoxicilina': " + medsPorPrincipio.size());

            // =========================================================================
            // 2. TESTE: LOTE ESTOQUE DAO (FEFO E CONTROLE DE VALIDADE)
            // =========================================================================
            System.out.println("\n=== 2. TESTE: LOTE ESTOQUE DAO (ORDENAÇÃO FEFO) ===");
            // Lote 1 vence DEPOIS (daqui a 12 meses)
            LoteEstoque loteMaisNovo = new LoteEstoque(
                    amoxicilina,
                    "LOTE-NOVO-" + sufixo,
                    LocalDate.now().plusMonths(12),
                    50
            );
            loteDAO.inserir(loteMaisNovo);

            // Lote 2 vence ANTES (daqui a 2 meses) -> Deve vir primeiro no FEFO!
            LoteEstoque loteVencePrimeiro = new LoteEstoque(
                    amoxicilina,
                    "LOTE-FEFO-" + sufixo,
                    LocalDate.now().plusMonths(2),
                    20
            );
            loteDAO.inserir(loteVencePrimeiro);

            // Lote 3 para o cosmético
            LoteEstoque loteCosmetico = new LoteEstoque(
                    protetorSolar,
                    "LOTE-COSM-" + sufixo,
                    LocalDate.now().plusYears(1),
                    30
            );
            loteDAO.inserir(loteCosmetico);

            List<LoteEstoque> lotesDisponiveis = loteDAO.listarDisponiveisPorProduto(amoxicilina.getId());
            System.out.println(" Lotes disponíveis para " + amoxicilina.getNome() + " (ordem FEFO):");
            for (LoteEstoque l : lotesDisponiveis) {
                System.out.println("  -> Lote: " + l.getNumeroLote()
                        + " | Validade: " + l.getDataValidade()
                        + " | Qtd: " + l.getQuantidade());
            }

            // =========================================================================
            // 3. TESTE: RECEITA DAO (TRANSAÇÃO RECEITA + ITENS PRESCRITOS)
            // =========================================================================
            System.out.println("\n=== 3. TESTE: RECEITA DAO ===");
            Receita receita = new Receita(
                    cliente,
                    "REC-" + sufixo,
                    LocalDate.now(),
                    "Dr. Roberto Mendes",
                    "CRM-CE-12345",
                    false
            );
            receita.adicionarItem(amoxicilina, 2);

            receitaDAO.inserir(receita);
            System.out.println(" Receita inserida com ID: " + receita.getId());

            Optional<Receita> receitaBuscada = receitaDAO.buscarPorNumero("REC-" + sufixo);
            if (receitaBuscada.isPresent()) {
                Receita r = receitaBuscada.get();
                System.out.println(" Receita recuperada: " + r.getNumero()
                        + " | Médico: " + r.getNomeMedico()
                        + " | Qtd itens prescritos: " + r.getItens().size());

                // Simulando retenção da receita após dispensação
                r.setRetida(true);
                receitaDAO.atualizar(r);
                System.out.println(" Receita marcada como retida = true.");
            }

            // =========================================================================
            // 4. TESTE: VENDA DAO (TRANSAÇÃO COMPLETA + HIDRATAÇÃO DE ASSOCIAÇÕES)
            // =========================================================================
            System.out.println("\n=== 4. TESTE: VENDA DAO ===");
            Venda venda = new Venda(atendente);
            venda.setCliente(cliente);
            venda.setReceita(receita);
            venda.setFarmaceutico(farmaceutico);

            // Adicionando 2 unidades do lote FEFO de Amoxicilina e 1 Protetor Solar
            venda.adicionarItem(amoxicilina, loteVencePrimeiro, 2, amoxicilina.getPreco());
            venda.adicionarItem(protetorSolar, loteCosmetico, 1, protetorSolar.getPreco());
            venda.calcularTotal();

            vendaDAO.inserir(venda);
            System.out.println(" Venda registrada com ID: " + venda.getId()
                    + " | Total: R$ " + venda.getValorTotal());

            // Atualizando status da venda para CONCLUIDA
            vendaDAO.atualizarStatus(venda.getId(), StatusVenda.CONCLUIDA);

            // Buscando a venda completa pelo ID para validar todas as associações e itens
            Optional<Venda> vendaCompleta = vendaDAO.buscarPorId(venda.getId());
            if (vendaCompleta.isPresent()) {
                Venda v = vendaCompleta.get();
                System.out.println(" Venda recuperada do banco:");
                System.out.println("  -> Status: " + v.getStatus());
                System.out.println("  -> Atendente: " + v.getFuncionario().getNome());
                System.out.println("  -> Cliente: " + v.getCliente().getNome());
                System.out.println("  -> Farmacêutico Resp.: " + v.getFarmaceutico().getNome()
                        + " (" + v.getFarmaceutico().getCrf() + ")");
                System.out.println("  -> Receita vinculada: " + v.getReceita().getNumero());
                System.out.println("  -> Valor Total: R$ " + v.getValorTotal());
                System.out.println("  -> Itens da Venda (" + v.getItens().size() + "):");

                for (ItemVenda item : v.getItens()) {
                    System.out.println("     * " + item.getProduto().getNome()
                            + " | Lote: " + item.getLote().getNumeroLote()
                            + " | Qtd: " + item.getQuantidade()
                            + " | Unit: R$ " + item.getValorUnitario()
                            + " | Subtotal: R$ " + item.getSubtotal());
                }
            }

            // Testando listagem por período
            List<Venda> vendasHoje = vendaDAO.listarPorPeriodo(
                    LocalDateTime.now().minusHours(1),
                    LocalDateTime.now().plusHours(1)
            );
            System.out.println(" Total de vendas encontradas no período de hoje: " + vendasHoje.size());

            System.out.println("\n TODOS OS TESTES CONCLUÍDOS COM SUCESSO!");

        } catch (Exception e) {
            System.err.println(" Falha durante a execução dos testes: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DataBase.closeConnection();
        }
    }
}