package application;
import db.DataBase;

import java.sql.*;

public class Main {
    public static void main(String[] args){

        Connection conn = null;

        conn = DataBase.getConnection();

        // 1. Comando de Inserção usando PreparedStatement
        String sqlInsert = "INSERT INTO pessoa (nome, cpf) VALUES (?, ?)";

        try (PreparedStatement psInsert = conn.prepareStatement(sqlInsert)) {
            psInsert.setString(1, "Witalo");
            psInsert.setString(2, "00000000000");

            //executeUpdate é usado para INSERT, UPDATE e DELETE
            int linhasAfetadas = psInsert.executeUpdate();
            System.out.println("Cadastro realizado com sucesso! Linhas afetadas: " + linhasAfetadas);

        } catch (SQLException e) {
            System.out.println("Erro ao inserir (provavelmente o CPF já está cadastrado): " + e.getMessage());
        }


        String sql = "SELECT id, nome FROM pessoa WHERE cpf = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, "00000000000");

            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()){
                    System.out.println("ID: " + rs.getLong("id"));
                    System.out.println("Nome: " + rs.getString("nome"));
                } else {
                    System.out.println("Pessoa não encontrada.");
                }
            }
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            DataBase.closeConnection();
        }
    }
}
