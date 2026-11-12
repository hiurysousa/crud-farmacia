package exception;

/**
 * Exceção lançada quando falham validações de formato ou consistência de dados de entrada.
 */
public class ValidacaoException extends RegraNegocioException {

    public ValidacaoException(String message) {
        super(message);
    }
}
