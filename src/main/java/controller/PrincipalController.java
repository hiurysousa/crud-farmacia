package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class PrincipalController {

    @FXML
    private Label mensagem;

    @FXML
    private void testar() {
        mensagem.setText("JavaFX integrado!");
    }
}