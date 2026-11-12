package application;
import dao.ClienteDAO;
import dao.FuncionarioDAO;
import dao.jdbc.ClienteDAOJdbc;
import db.DataBase;
import model.Cliente;

import java.sql.*;
import java.util.List;
import java.util.Optional;

import dao.DaoFactory;
import model.Farmaceutico;
import model.Funcionario;
import model.enums.CargoFuncionario;


public class Main {
    public static void main(String[] args) {
        Connection conn = null;

        try {
            FuncionarioDAO funcionarioDAO = DaoFactory.createFuncionarioDAO();

            System.out.println("=== 1. TESTE: INSERIR ATENDENTE (FUNCIONÁRIO COMUM) ===");
            // Dica: use números novos caso rode mais de uma vez (CPF e Matrículasão UNIQUE no banco)
            Funcionario atendente = new Funcionario("Carlos Atendente", "22233344455");
            atendente.setTelefone("85911112222");
            atendente.setMatricula("ATD-001");
            atendente.setCargo(CargoFuncionario.ATENDENTE);

            funcionarioDAO.inserir(atendente);
            System.out.println(" Atendente inserido com ID: " + atendente.getId());

            System.out.println("\n=== 2. TESTE: INSERIR FARMACÊUTICO (COM CRF) ===");
            Farmaceutico farmaceutico = new Farmaceutico("Dra. Juliana", "55566677788", "CRF-CE-9999");
            farmaceutico.setTelefone("85933334444");
            farmaceutico.setMatricula("FRM-001");

            funcionarioDAO.inserir(farmaceutico);
            System.out.println(" Farmacêutica inserida com ID: " + farmaceutico.getId());

            System.out.println("\n=== 3. TESTE: BUSCAR FARMACÊUTICO POR ID (TESTANDO POLIMORFISMO) ===");
            Optional<Funcionario> buscado = funcionarioDAO.buscarPorId(farmaceutico.getId());

            if (buscado.isPresent()) {
                Funcionario f = buscado.get();
                System.out.println(" Colaborador recuperado: " + f.getNome() + " | Cargo: " + f.getCargo());

                // Verificação polimórfica: o Java reconhece como Farmaceutico?
                if (f instanceof Farmaceutico farm) {
                    System.out.println(" Polimorfismo funcionou! É Farmacêutico com CRF: " + farm.getCrf());
                } else {
                    System.err.println("❌ Erro: Deveria ter sido instanciado como Farmaceutico!");
                }
            }

            System.out.println("\n=== 4. TESTE: LISTAR APENAS FARMACÊUTICOS ATIVOS ===");
            List<Farmaceutico> farmaceuticos = funcionarioDAO.listarFarmaceuticosAtivos();

            System.out.println(" Total de farmacêuticos ativos encontrados: " + farmaceuticos.size());

            for (Farmaceutico farm : farmaceuticos) {
                System.out.println("  -> " + farm.getNome() + " (" + farm.getCrf() + ")");
            }

        } catch (Exception e) {
            System.err.println("❌ Falha no teste: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DataBase.closeConnection();
        }

//        try {
//            ClienteDAO clienteDAO = DaoFactory.createClienteDAO();
//
//            System.out.println("=== TESTE DE INSERÇÃO DE CLIENTE ===");
//            // Dica: use um CPF diferente se for rodar mais de uma vez, pois CPF é UNIQUE no banco
//            String cpfTeste = "11122233344";
//            Cliente novoCliente = new Cliente("Cliente Teste DAO", cpfTeste);
//            novoCliente.setTelefone("85988887777");
//            novoCliente.setEndereco("Av. Principal, 1000 - Bairro Centro");
//            novoCliente.setFidelidadePontos(15);
//
//            clienteDAO.inserir(novoCliente);
//            System.out.println(" Sucesso! Cliente inserido com ID: " + novoCliente.getId());
//
//            System.out.println("\n=== TESTE DE CONSULTA (BUSCA POR ID) ===");
//            Optional<Cliente> clienteEncontrado = clienteDAO.buscarPorId(novoCliente.getId());
//
//            if (clienteEncontrado.isPresent()) {
//                Cliente c = clienteEncontrado.get();
//                System.out.println(" Cliente localizado no banco MySQL:");
//                System.out.println("   ID: " + c.getId());
//                System.out.println("   Nome: " + c.getNome());
//                System.out.println("   CPF: " + c.getCpf());
//                System.out.println("   Telefone: " + c.getTelefone());
//                System.out.println("   Endereço: " + c.getEndereco());
//                System.out.println("   Pontos de Fidelidade: " + c.getFidelidadePontos());
//                System.out.println("   Ativo: " + c.isAtivo());
//            } else {
//                System.out.println("Erro: Cliente não encontrado pelo ID gerado.");
//            }
//
//        } catch (Exception e) {
//            System.err.println("Falha no teste: " + e.getMessage());
//            e.printStackTrace();
//        } finally {
//            DataBase.closeConnection();
//        }
    }
}
