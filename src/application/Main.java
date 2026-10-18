package application;
import db.DataBase;

import java.sql.*;

public class Main {
    public static void main(String[] args){

        Connection conn = null;

        conn = DataBase.getConnection();
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
