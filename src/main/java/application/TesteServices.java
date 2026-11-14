package application;

import db.DataBase;
import exception.RegraNegocioException;
import exception.ValidacaoException;
import model.LoteEstoque;
import model.Medicamento;
import model.enums.CategoriaRegulatoria;
import model.enums.TipoMedicamento;
import model.enums.TipoUso;
import service.EstoqueService;
import service.EstoqueService.AlocacaoLote;
import service.ProdutoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TesteServices {
    public static void main(String[] args) {
        String sufixo = String.valueOf(System.currentTimeMillis() % 100000);

        try {
            ProdutoService produtoService = new ProdutoService();
            EstoqueService estoqueService = new EstoqueService();

            System.out.println("=== 1. TESTE: VALIDAÇÃO DE PREÇO NEGATIVO ===");
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

            System.out.println("\n=== 2. TESTE: CADASTRO COM NORMALIZAÇÃO (TRIM) E DUPLICIDADE ===");
            String codigoBarras = "789" + sufixo;
            // Enviamos com espaços nas pontas para testar o normalizarDados()
            Medicamento dipirona = new Medicamento("   Dipirona 500mg   ", "  EMS  ", new BigDecimal("15.50"), "  " + codigoBarras + "  ");
            dipirona.setPrincipioAtivo("Dipirona Monoidratada");
            dipirona.setDosagem("500mg");
            dipirona.setTipo(TipoMedicamento.ANTIINFLAMATORIO);
            dipirona.setCategoriaRegulatoria(CategoriaRegulatoria.LIVRE);
            dipirona.setTipoUso(TipoUso.ORAL);

            produtoService.cadastrar(dipirona);
            System.out.println(" Produto cadastrado com ID: " + dipirona.getId() + " | Nome normalizado: '" + dipirona.getNome() + "'");

            try {
                // Tentando cadastrar outro produto com o MESMO código de barras
                Medicamento dipironaClone = new Medicamento("Dipirona Genérica", "Medley", new BigDecimal("12.00"), codigoBarras);
                dipironaClone.setPrincipioAtivo("Dipirona");
                dipironaClone.setDosagem("500mg");
                dipironaClone.setTipo(TipoMedicamento.ANTIINFLAMATORIO);
                dipironaClone.setCategoriaRegulatoria(CategoriaRegulatoria.LIVRE);

                produtoService.cadastrar(dipironaClone);
                System.err.println("❌ Falha: Deveria ter barrado código de barras duplicado!");
            } catch (RegraNegocioException e) {
                System.out.println(" Sucesso! Bloqueou código de barras duplicado: " + e.getMessage());
            }

            System.out.println("\n=== 3. TESTE: ENTRADA DE LOTES E VALIDAÇÃO DE DATA PASSADA ===");
            try {
                // Lote vencido no passado
                LoteEstoque loteVencido = new LoteEstoque(dipirona, "VENCIDO-" + sufixo, LocalDate.now().minusDays(5), 10);
                estoqueService.darEntradaLote(loteVencido);
                System.err.println("❌ Falha: Deveria ter barrado lote vencido!");
            } catch (RegraNegocioException e) {
                System.out.println(" Sucesso! Bloqueou lote com validade no passado: " + e.getMessage());
            }

            // Lote 1: 10 unidades, vence daqui a 2 meses (DEVE SAIR PRIMEIRO NO FEFO)
            LoteEstoque loteMaisProximo = new LoteEstoque(dipirona, "LOTE-FEFO-1-" + sufixo, LocalDate.now().plusMonths(2), 10);
            estoqueService.darEntradaLote(loteMaisProximo);

            // Lote 2: 30 unidades, vence daqui a 12 meses
            LoteEstoque loteMaisLonge = new LoteEstoque(dipirona, "LOTE-FEFO-2-" + sufixo, LocalDate.now().plusMonths(12), 30);
            estoqueService.darEntradaLote(loteMaisLonge);

            int saldoInicial = produtoService.obterEstoqueTotal(dipirona.getId());
            System.out.println(" Estoque total inicial da Dipirona: " + saldoInicial + " unidades (10 do lote 1 + 30 do lote 2)");

            System.out.println("\n=== 4. TESTE: ALGORITMO FEFO EM AÇÃO (BAIXA DE 15 UNIDADES) ===");
            System.out.println("-> Solicitando baixa de 15 unidades...");
            List<AlocacaoLote> alocacoes = estoqueService.baixarEstoqueFefo(dipirona.getId(), 15);

            for (AlocacaoLote aloc : alocacoes) {
                System.out.println(" Consumidas " + aloc.quantidadeConsumida() + " un do lote '"
                        + aloc.lote().getNumeroLote() + "' (Vence em: " + aloc.lote().getDataValidade() + ")");
            }

            int saldoAposBaixa = produtoService.obterEstoqueTotal(dipirona.getId());
            System.out.println(" Novo saldo total em estoque: " + saldoAposBaixa + " unidades (esperado: 25)");

            System.out.println("\n=== 5. TESTE: TENTATIVA DE BAIXA COM ESTOQUE INSUFICIENTE ===");
            try {
                // Tentando baixar 50 unidades quando só restam 25
                estoqueService.baixarEstoqueFefo(dipirona.getId(), 50);
                System.err.println("❌ Falha: Deveria ter barrado estoque insuficiente!");
            } catch (RegraNegocioException e) {
                System.out.println(" Sucesso! Bloqueou estoque insuficiente: " + e.getMessage());
            }

            System.out.println("\n=== 6. TESTE: ESTORNO DE ESTOQUE (CANCELAMENTO) ===");
            // Estornando 5 unidades de volta para o Lote 2
            estoqueService.estornarEstoque(loteMaisLonge.getId(), 5);
            int saldoAposEstorno = produtoService.obterEstoqueTotal(dipirona.getId());
            System.out.println(" Saldo após estorno de 5 unidades: " + saldoAposEstorno + " unidades (esperado: 30)");

            System.out.println("\n🎉 TODOS OS TESTES DE REGRAS E FEFO PASSARAM COM SUCESSO!");

        } catch (Exception e) {
            System.err.println("❌ Erro inesperado: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DataBase.closeConnection();
        }
    }
}