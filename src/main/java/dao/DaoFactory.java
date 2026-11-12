package dao;

import dao.jdbc.ClienteDAOJdbc;
import db.DataBase;

public class DaoFactory {
    public static ClienteDAO createClienteDAO(){
        return new ClienteDAOJdbc(DataBase.getConnection());
    }
}
