package dao;

import dao.jdbc.*;
import db.DataBase;

public class DaoFactory {
    public static ClienteDAO createClienteDAO(){
        return new ClienteDAOJdbc(DataBase.getConnection());
    }

    public static FuncionarioDAO createFuncionarioDAO(){
        return new FuncionarioDAOJdbc(DataBase.getConnection());
    }

    public static ProdutoDAO createProdutoDAO(){
        return new ProdutoDAOJdbc(DataBase.getConnection());
    }

    public static LoteEstoqueDAO createLoteEstoqueDAO(){
        return new LoteEstoqueDAOJdbc(DataBase.getConnection());
    }

    public static ReceitaDAO createReceitaDAO(){
        return new ReceitaDAOJdbc(DataBase.getConnection());
    }

    public static VendaDAO createVendaDAO(){
        return new VendaDAOJdbc(DataBase.getConnection());
    }

}
