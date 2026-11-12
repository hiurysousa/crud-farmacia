package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FarmaciaApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                FarmaciaApp.class.getResource("/view/principal.fxml")
        );

        stage.setScene(new Scene(loader.load()));
        stage.setTitle("Sistema de Farmácia");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}