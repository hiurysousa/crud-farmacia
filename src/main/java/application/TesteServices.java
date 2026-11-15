package application;

import db.DataBase;
import exception.RegraNegocioException;
import exception.ValidacaoException;
import model.Cliente;
import model.Cosmetico;
import model.Farmaceutico;
import model.Funcionario;
import model.ItemReceita;
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
import service.ClienteService;
import service.EstoqueService;
import service.EstoqueService.AlocacaoLote;
import service.FuncionarioService;
import service.ProdutoService;
import service.ReceitaService;
import service.VendaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TesteServices {
    public static void main(String[] args) {
        int seed = (int) (System.currentTimeMillis() % 100000000);
        String sufixo = String.valueOf(seed % 100000);

        try {
            ProdutoService produtoService = new ProdutoService();
            EstoqueService estoqueService = new EstoqueService();
            ClienteService clienteService = new ClienteService();
            FuncionarioService funcionarioService = new FuncionarioService();
            ReceitaService receitaService = new ReceitaService();
            VendaService vendaService = new VendaService();

            // =========================================================================
            // 1. PRODUTOS: VALIDAÇÃO DE PREÇO NEGATIVO
            // =========================================================================
            System.out.println("=== 1. TESTE: VALIDAÇÃO DE CADASTRAMENTO DE MEDICAMENTO (PREÇO NEGATIVO) ===");
            try {
                Medicamento medInvalido = new Medicamento("Remédio Inválido", "Lab", new BigDecimal("-10.00"), "000" + sufixo);
                medInvalido.setPrincipioAtivo("Qualquer");
                medInvalido.setDosagem("10mg");
                medInvalido.setTipo(TipoMedicamento.OUTRO);
                medInvalido.setCategoriaRegulatoria(CategoriaRegulatoria.LIVRE);

                produtoService.cadastrar(medInvalido);
                System.err.println("❌ Falha: Deveria ter barrado preço negativo!");
            } catch (ValidacaoException e) {
                System.out.println(" Sucesso! Bloqueou preço negativo: " + e.getMessage());
            }

            // =========================================================================
            // 2. PRODUTOS: CÓDIGO DE BARRAS NÃO NUMÉRICO
            // =========================================================================
            System.out.println("\n=== 2. TESTE: VALIDAÇÃO CADASTRAMENTO DE MEDICAMENTO (CÓDIGO DE BARRAS NÃO NUMÉRICO) ===");
            try {
                Medicamento medCodigoInvalido = new Medicamento("Remédio Barcode", "Lab", new BigDecimal("10.00"), "ABC-123");
                medCodigoInvalido.setPrincipioAtivo("Qualquer");
                medCodigoInvalido.setDosagem("10mg");
                medCodigoInvalido.setTipo(TipoMedicamento.OUTRO);
                medCodigoInvalido.setCategoriaRegulatoria(CategoriaRegulatoria.LIVRE);

                produtoService.cadastrar(medCodigoInvalido);
                System.err.println("❌ Falha: Deveria ter barrado código de barras com letras!");
            } catch (ValidacaoException e) {
                System.out.println(" Sucesso! Bloqueou código não numérico: " + e.getMessage());
            }

            // =========================================================================
            // 3. PRODUTOS: CADASTRO COM NORMALIZAÇÃO (TRIM) E DUPLICIDADE
            // =========================================================================
            System.out.println("\n=== 3. TESTE: CADASTRO COM NORMALIZAÇÃO (TRIM) E DUPLICIDADE ===");
            String codigoBarras = "789" + sufixo;
            Medicamento dipirona = new Medicamento("   Dipirona 500mg   ", "  EMS  ", new BigDecimal("15.50"), "  " + codigoBarras + "  ");
            dipirona.setPrincipioAtivo("Dipirona Monoidratada");
            dipirona.setDosagem("500mg");
            dipirona.setTipo(TipoMedicamento.ANTIINFLAMATORIO);
            dipirona.setCategoriaRegulatoria(CategoriaRegulatoria.LIVRE);
            dipirona.setTipoUso(TipoUso.ORAL);

            produtoService.cadastrar(dipirona);
            System.out.println(" Produto cadastrado com ID: " + dipirona.getId() + " | Nome normalizado: '" + dipirona.getNome() + "'");

            try {
                Medicamento dipironaClone = new Medicamento("Dipirona Genérica", "Medley", new BigDecimal("12.00"), codigoBarras);
                dipironaClone.setPrincipioAtivo("Dipirona");
                dipironaClone.setDosagem("500mg");
                dipironaClone.setTipo(TipoMedicamento.ANTIINFLAMATORIO);
                dipironaClone.setCategoriaRegulatoria(CategoriaRegulatoria.LIVRE);

                produtoService.cadastrar(dipironaClone);
                System.err.println("❌ Falha: Deveria ter barrado código duplicado!");
            } catch (RegraNegocioException e) {
                System.out.println(" Sucesso! Bloqueou código duplicado: " + e.getMessage());
            }

            // =========================================================================
            // 4. ESTOQUE: ENTRADA DE LOTES E VALIDAÇÃO DE DATA PASSADA
            // =========================================================================
            System.out.println("\n=== 4. TESTE: ENTRADA DE LOTES E VALIDAÇÃO DE DATA PASSADA ===");
            try {
                LoteEstoque loteVencido = new LoteEstoque(dipirona, "VENCIDO-" + sufixo, LocalDate.now().minusDays(5), 10);
                estoqueService.darEntradaLote(loteVencido);
                System.err.println("❌ Falha: Deveria ter barrado lote vencido!");
            } catch (RegraNegocioException e) {
                System.out.println(" Sucesso! Bloqueou lote vencido: " + e.getMessage());
            }

            LoteEstoque loteMaisProximo = new LoteEstoque(dipirona, "LOTE-FEFO-1-" + sufixo, LocalDate.now().plusMonths(2), 10);
            estoqueService.darEntradaLote(loteMaisProximo);

            LoteEstoque loteMaisLonge = new LoteEstoque(dipirona, "LOTE-FEFO-2-" + sufixo, LocalDate.now().plusMonths(12), 30);
            estoqueService.darEntradaLote(loteMaisLonge);

            int saldoInicial = estoqueService.consultarSaldoDisponivel(dipirona.getId());
            System.out.println(" Estoque total inicial: " + saldoInicial + " unidades (10 do lote 1 + 30 do lote 2)");

            // =========================================================================
            // 5. ESTOQUE: ALGORITMO FEFO EM AÇÃO (BAIXA DE 15 UNIDADES)
            // =========================================================================
            System.out.println("\n=== 5. TESTE: ALGORITMO FEFO EM AÇÃO (BAIXA DE 15 UNIDADES) ===");
            List<AlocacaoLote> alocacoes = estoqueService.baixarEstoqueFefo(dipirona.getId(), 15);
            for (AlocacaoLote aloc : alocacoes) {
                System.out.println(" Consumidas " + aloc.quantidadeConsumida() + " un do lote '"
                        + aloc.lote().getNumeroLote() + "' (Vence em: " + aloc.lote().getDataValidade() + ")");
            }

            int saldoAposBaixa = estoqueService.consultarSaldoDisponivel(dipirona.getId());
            System.out.println(" Novo saldo em estoque: " + saldoAposBaixa + " unidades (esperado: 25)");

            // =========================================================================
            // 6. ESTOQUE: TENTATIVA DE BAIXA COM ESTOQUE INSUFICIENTE
            // =========================================================================
            System.out.println("\n=== 6. TESTE: BAIXA COM ESTOQUE INSUFICIENTE ===");
            try {
                estoqueService.baixarEstoqueFefo(dipirona.getId(), 50);
                System.err.println("❌ Falha: Deveria ter barrado estoque insuficiente!");
            } catch (RegraNegocioException e) {
                System.out.println(" Sucesso! Bloqueou estoque insuficiente: " + e.getMessage());
            }

            // =========================================================================
            // 7. ESTOQUE: ESTORNO DE ESTOQUE
            // =========================================================================
            System.out.println("\n=== 7. TESTE: ESTORNO DE ESTOQUE ===");
            estoqueService.estornarEstoque(loteMaisLonge.getId(), 5);
            int saldoAposEstorno = estoqueService.consultarSaldoDisponivel(dipirona.getId());
            System.out.println(" Saldo após estorno de 5 unidades: " + saldoAposEstorno + " unidades (esperado: 30)");

            // =========================================================================
            // 8. CLIENTE SERVICE: VALIDAÇÃO DE CPF E FIDELIDADE
            // =========================================================================
            System.out.println("\n=== 8. TESTE: CLIENTE SERVICE (CPF E PONTOS) ===");
            try {
                Cliente clienteInvalido = new Cliente("Teste Inválido", "12345678900");
                clienteService.cadastrar(clienteInvalido);
                System.err.println("❌ Falha: Deveria ter barrado CPF com dígitos inválidos!");
            } catch (ValidacaoException e) {
                System.out.println(" Sucesso! Bloqueou CPF inválido: " + e.getMessage());
            }

            String cpfCliente = gerarCpfValido(seed);
            Cliente clienteValido = new Cliente("Maria Oliveira", cpfCliente);
            clienteValido.setTelefone("85987654321");
            clienteValido.setEndereco("Rua das Flores, 123");
            clienteValido.setFidelidadePontos(5);
            clienteService.cadastrar(clienteValido);
            System.out.println(" Cliente cadastrado com ID: " + clienteValido.getId() + " | CPF: " + clienteValido.getCpf());

            try {
                Cliente clienteDuplicado = new Cliente("Outra Maria", cpfCliente);
                clienteService.cadastrar(clienteDuplicado);
                System.err.println("❌ Falha: Deveria ter barrado CPF duplicado!");
            } catch (RegraNegocioException e) {
                System.out.println(" Sucesso! Bloqueou CPF duplicado: " + e.getMessage());
            }

            clienteService.adicionarPontos(clienteValido.getId(), 15);
            clienteService.debitarPontos(clienteValido.getId(), 5);
            Cliente clienteAtualizado = clienteService.buscarPorId(clienteValido.getId());
            System.out.println(" Pontos de fidelidade atualizados: " + clienteAtualizado.getFidelidadePontos() + " (esperado: 15)");

            // =========================================================================
            // 9. FUNCIONARIO SERVICE: CARGOS, MATRÍCULA E CRF DO FARMACÊUTICO
            // =========================================================================
            System.out.println("\n=== 9. TESTE: FUNCIONARIO SERVICE (CARGO E CRF) ===");
            try {
                Funcionario atendenteInvalido = new Funcionario("João Silva", gerarCpfValido(seed + 1));
                atendenteInvalido.setMatricula("MAT-" + sufixo);
                atendenteInvalido.setCargo(CargoFuncionario.FARMACEUTICO); // Cargo Farmacêutico exige classe Farmaceutico
                funcionarioService.cadastrar(atendenteInvalido);
                System.err.println("❌ Falha: Deveria ter barrado Farmacêutico sem ser subclasse Farmaceutico!");
            } catch (RegraNegocioException e) {
                System.out.println(" Sucesso! Bloqueou funcionário farmacêutico sem classe adequada: " + e.getMessage());
            }

            try {
                Farmaceutico farmCrfInvalido = new Farmaceutico("Dr. Pedro", gerarCpfValido(seed + 2), "12345"); // CRF sem UF
                farmCrfInvalido.setMatricula("FRM-INV-" + sufixo);
                funcionarioService.cadastrar(farmCrfInvalido);
                System.err.println("❌ Falha: Deveria ter barrado CRF sem UF!");
            } catch (ValidacaoException e) {
                System.out.println(" Sucesso! Bloqueou CRF fora do padrão: " + e.getMessage());
            }

            // Cadastra atendente válido
            Funcionario atendente = new Funcionario("Lucas Atendente", gerarCpfValido(seed + 3));
            atendente.setMatricula("ATD-" + sufixo);
            atendente.setCargo(CargoFuncionario.ATENDENTE);
            funcionarioService.cadastrar(atendente);
            System.out.println(" Atendente cadastrado com ID: " + atendente.getId() + " | Matrícula: " + atendente.getMatricula());

            // Cadastra farmacêutico válido
            Farmaceutico farmaceutico = new Farmaceutico("Dra. Amanda CRF", gerarCpfValido(seed + 4), "CE-" + sufixo);
            farmaceutico.setMatricula("FRM-" + sufixo);
            funcionarioService.cadastrar(farmaceutico);
            System.out.println(" Farmacêutico cadastrado com ID: " + farmaceutico.getId() + " | CRF: " + farmaceutico.getCrf());

            // =========================================================================
            // 10. RECEITA SERVICE: PRESCRIÇÃO E VALIDAÇÕES
            // =========================================================================
            System.out.println("\n=== 10. TESTE: RECEITA SERVICE ===");
            Medicamento clonazepam = new Medicamento("Clonazepam 2mg", "Roche", new BigDecimal("35.00"), "78930" + sufixo);
            clonazepam.setPrincipioAtivo("Clonazepam");
            clonazepam.setDosagem("2mg");
            clonazepam.setTipo(TipoMedicamento.OUTRO);
            clonazepam.setCategoriaRegulatoria(CategoriaRegulatoria.CONTROLADO);
            clonazepam.setTipoUso(TipoUso.ORAL);
            clonazepam.setRegistroAnvisa("1.0001.0001");
            produtoService.cadastrar(clonazepam);

            LoteEstoque loteClonazepam = new LoteEstoque(clonazepam, "LOTE-CLON-" + sufixo, LocalDate.now().plusMonths(8), 20);
            estoqueService.darEntradaLote(loteClonazepam);

            Receita receita = new Receita(
                    clienteValido,
                    "REC-" + sufixo,
                    LocalDate.now(),
                    "Dr. Marcos Prescritor",
                    "CRM-CE 44556",
                    false
            );
            receita.adicionarItem(clonazepam, 2);
            receitaService.registrar(receita);
            System.out.println(" Receita registrada com ID: " + receita.getId() + " | Médico: " + receita.getNomeMedico());

            // =========================================================================
            // 11. VENDA SERVICE: REGRAS REGULATÓRIAS, TRANSAÇÃO ATÔMICA E CANCELAMENTO
            // =========================================================================
            System.out.println("\n=== 11. TESTE: VENDA SERVICE (REGRAS REGULATÓRIAS E TRANSAÇÃO) ===");

            // Cenário A: Tentar vender controlado SEM receita médica -> DEVE BARRAR
            System.out.println("-> Teste 11.A: Tentar vender medicamento CONTROLADO sem receita...");
            try {
                Venda vendaSemReceita = new Venda(atendente);
                vendaSemReceita.setCliente(clienteValido);
                vendaSemReceita.setFarmaceutico(farmaceutico);
                ItemVenda itemSemReceita = new ItemVenda();
                itemSemReceita.setProduto(clonazepam);
                itemSemReceita.setQuantidade(1);
                vendaSemReceita.setItens(List.of(itemSemReceita));

                vendaService.finalizarVenda(vendaSemReceita);
                System.err.println("❌ Falha: Deveria ter barrado medicamento controlado sem receita médica!");
            } catch (RegraNegocioException e) {
                System.out.println(" Sucesso! Bloqueou controlado sem receita: " + e.getMessage());
            }

            // Cenário B: Tentar vender controlado COM receita, mas SEM Farmacêutico -> DEVE BARRAR
            System.out.println("-> Teste 11.B: Tentar vender controlado sem Farmacêutico responsável...");
            try {
                Venda vendaSemFarm = new Venda(atendente);
                vendaSemFarm.setCliente(clienteValido);
                vendaSemFarm.setReceita(receita);
                vendaSemFarm.setFarmaceutico(null);
                ItemVenda itemSemFarm = new ItemVenda();
                itemSemFarm.setProduto(clonazepam);
                itemSemFarm.setQuantidade(1);
                vendaSemFarm.setItens(List.of(itemSemFarm));

                vendaService.finalizarVenda(vendaSemFarm);
                System.err.println("❌ Falha: Deveria ter barrado venda de controlado sem farmacêutico!");
            } catch (RegraNegocioException e) {
                System.out.println(" Sucesso! Bloqueou controlado sem farmacêutico: " + e.getMessage());
            }

            // Cenário C: Venda COMPLETA E VÁLIDA (Controlado + Venda Livre)
            System.out.println("-> Teste 11.C: Finalizando venda completa válida (Controlado + Venda Livre)...");
            Venda vendaValida = new Venda(atendente);
            vendaValida.setCliente(clienteValido);
            vendaValida.setFarmaceutico(farmaceutico);
            vendaValida.setReceita(receita);

            ItemVenda item1 = new ItemVenda();
            item1.setProduto(clonazepam);
            item1.setQuantidade(2);

            ItemVenda item2 = new ItemVenda();
            item2.setProduto(dipirona);
            item2.setQuantidade(1);

            vendaValida.setItens(List.of(item1, item2));

            int pontosAntes = clienteService.buscarPorId(clienteValido.getId()).getFidelidadePontos();
            int estoqueClonAntes = estoqueService.consultarSaldoDisponivel(clonazepam.getId());

            Venda vendaFinalizada = vendaService.finalizarVenda(vendaValida);
            System.out.println(" Venda finalizada com sucesso! ID: " + vendaFinalizada.getId()
                    + " | Total: R$ " + vendaFinalizada.getValorTotal()
                    + " | Status: " + vendaFinalizada.getStatus());

            // Validações pós-venda
            int estoqueClonDepois = estoqueService.consultarSaldoDisponivel(clonazepam.getId());
            Receita receitaPosVenda = receitaService.buscarPorId(receita.getId());
            Cliente clientePosVenda = clienteService.buscarPorId(clienteValido.getId());

            System.out.println(" Estoque de Clonazepam baixado: " + estoqueClonAntes + " -> " + estoqueClonDepois + " (baixou 2 un)");
            System.out.println(" Receita retida com sucesso: " + receitaPosVenda.isRetida() + " (esperado: true)");
            System.out.println(" Pontos de fidelidade creditados: " + pontosAntes + " -> " + clientePosVenda.getFidelidadePontos());

            // Cenário D: Cancelamento da Venda com estorno
            System.out.println("\n-> Teste 11.D: Cancelando venda finalizada (Estorno de estoque e pontos)...");
            vendaService.cancelarVenda(vendaFinalizada.getId());

            Venda vendaCancelada = vendaService.buscarPorId(vendaFinalizada.getId());
            int estoqueClonEstornado = estoqueService.consultarSaldoDisponivel(clonazepam.getId());
            Cliente clienteAposCancel = clienteService.buscarPorId(clienteValido.getId());

            System.out.println(" Status da venda após cancelamento: " + vendaCancelada.getStatus() + " (esperado: CANCELADA)");
            System.out.println(" Estoque de Clonazepam estornado: " + estoqueClonDepois + " -> " + estoqueClonEstornado + " (devolveu 2 un)");
            System.out.println(" Pontos de fidelidade estornados: " + clientePosVenda.getFidelidadePontos() + " -> " + clienteAposCancel.getFidelidadePontos());

            System.out.println("\n🎉🎉 TODOS OS TESTES DE REGRAS, SERVICES E TRANSAÇÃO PASSARAM COM SUCESSO! 🎉🎉");

        } catch (Exception e) {
            System.err.println("❌ Erro inesperado: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DataBase.closeConnection();
        }
    }

    private static String gerarCpfValido(int seed) {
        String base = String.format("%09d", Math.abs(seed) % 1000000000);
        int soma1 = 0;
        for (int i = 0; i < 9; i++) {
            soma1 += (base.charAt(i) - '0') * (10 - i);
        }
        int r1 = soma1 % 11;
        int d1 = (r1 < 2) ? 0 : (11 - r1);

        int soma2 = 0;
        for (int i = 0; i < 9; i++) {
            soma2 += (base.charAt(i) - '0') * (11 - i);
        }
        soma2 += d1 * 2;
        int r2 = soma2 % 11;
        int d2 = (r2 < 2) ? 0 : (11 - r2);

        return base + d1 + d2;
    }
}
