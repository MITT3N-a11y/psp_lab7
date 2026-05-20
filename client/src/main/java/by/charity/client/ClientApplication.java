package by.charity.client;

import by.charity.client.controller.LoginController;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ClientApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Учёт благотворительных пожертвований");
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(560);

        LoginController loginController = new LoginController(primaryStage);
        primaryStage.setScene(loginController.createScene());
        primaryStage.show();
    }
}