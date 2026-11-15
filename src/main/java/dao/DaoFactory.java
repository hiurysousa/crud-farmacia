package dao;

import dao.jdbc.*;
import db.DataBase;

import java.sql.Connection;

public class DaoFactory {
    public static ClienteDAO createClienteDAO(){
        return new ClienteDAOJdbc(DataBase.getConnection());
    }

    public static ClienteDAO createClienteDAO(Connection conn){
        return new ClienteDAOJdbc(conn);
    }

    public static FuncionarioDAO createFuncionarioDAO(){
        return new FuncionarioDAOJdbc(DataBase.getConnection());
    }

    public static FuncionarioDAO createFuncionarioDAO(Connection conn){
        return new FuncionarioDAOJdbc(conn);
    }

    public static ProdutoDAO createProdutoDAO(){
        return new ProdutoDAOJdbc(DataBase.getConnection());
    }

    public static ProdutoDAO createProdutoDAO(Connection conn){
        return new ProdutoDAOJdbc(conn);
    }

    public static LoteEstoqueDAO createLoteEstoqueDAO(){
        return new LoteEstoqueDAOJdbc(DataBase.getConnection());
    }

    public static LoteEstoqueDAO createLoteEstoqueDAO(Connection conn){
        return new LoteEstoqueDAOJdbc(conn);
    }

    public static ReceitaDAO createReceitaDAO(){
        return new ReceitaDAOJdbc(DataBase.getConnection());
    }

    public static ReceitaDAO createReceitaDAO(Connection conn){
        return new ReceitaDAOJdbc(conn);
    }

    public static VendaDAO createVendaDAO(){
        return new VendaDAOJdbc(DataBase.getConnection());
    }

    public static VendaDAO createVendaDAO(Connection conn){
        return new VendaDAOJdbc(conn);
    }
}
