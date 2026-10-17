package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

public class DataBase {
    private static Connection conn=null;

    public static Connection getConnection(){
        try {
            if(conn==null){
                String senha = System.getenv("DB_PASSWORD");
                if (senha == null || senha.isBlank()){
                    throw new IllegalStateException("Configure a variável de ambiente DB_PASSWORD");
                }

                conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/farmacia", "root", senha);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return conn;
    }

    public static void closeConnection() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public static void closeResultSet(ResultSet rs){
        try {
            rs.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void closeStatement(Statement st){
        try {
            st.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
