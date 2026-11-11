package exception;

/**
 * Exceção lançada para encapsular erros da camada de persistência (JDBC/SQL).
 */
public class PersistenciaException extends RuntimeException {

    public PersistenciaException(String message) {
        super(message);
    }

    public PersistenciaException(String message, Throwable cause) {
        super(message, cause);
    }
}
