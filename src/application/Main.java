package application;
import dao.ClienteDAO;
import dao.jdbc.ClienteDAOJdbc;
import db.DataBase;
import model.Cliente;

import java.sql.*;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        Connection conn = null;

        try {
            // 1. Obtém a conexão segura
            conn = DataBase.getConnection();
            ClienteDAO clienteDAO = new ClienteDAOJdbc(conn);

            System.out.println("=== TESTE DE INSERÇÃO DE CLIENTE ===");
            // Dica: use um CPF diferente se for rodar mais de uma vez, pois CPF é UNIQUE no banco
            String cpfTeste = "11122233344";
            Cliente novoCliente = new Cliente("Cliente Teste DAO", cpfTeste);
            novoCliente.setTelefone("85988887777");
            novoCliente.setEndereco("Av. Principal, 1000 - Bairro Centro");
            novoCliente.setFidelidadePontos(15);

            clienteDAO.inserir(novoCliente);
            System.out.println(" Sucesso! Cliente inserido com ID: " + novoCliente.getId());

            System.out.println("\n=== TESTE DE CONSULTA (BUSCA POR ID) ===");
            Optional<Cliente> clienteEncontrado = clienteDAO.buscarPorId(novoCliente.getId());

            if (clienteEncontrado.isPresent()) {
                Cliente c = clienteEncontrado.get();
                System.out.println(" Cliente localizado no banco MySQL:");
                System.out.println("   ID: " + c.getId());
                System.out.println("   Nome: " + c.getNome());
                System.out.println("   CPF: " + c.getCpf());
                System.out.println("   Telefone: " + c.getTelefone());
                System.out.println("   Endereço: " + c.getEndereco());
                System.out.println("   Pontos de Fidelidade: " + c.getFidelidadePontos());
                System.out.println("   Ativo: " + c.isAtivo());
            } else {
                System.out.println("❌ Erro: Cliente não encontrado pelo ID gerado.");
            }

        } catch (Exception e) {
            System.err.println("❌ Falha no teste: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DataBase.closeConnection();
        }
    }
}
