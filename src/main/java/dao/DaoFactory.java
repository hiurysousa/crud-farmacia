package dao;

import dao.jdbc.ClienteDAOJdbc;
import dao.jdbc.FuncionarioDAOJdbc;
import db.DataBase;

public class DaoFactory {
    public static ClienteDAO createClienteDAO(){
        return new ClienteDAOJdbc(DataBase.getConnection());
    }

    public static FuncionarioDAO createFuncionarioDAO(){
        return new FuncionarioDAOJdbc(DataBase.getConnection());
    }
}
