package sample.clientsidechat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        stage.setTitle("BOB - CLIENT");
        stage.setScene(new Scene(root, 478, 396));
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}