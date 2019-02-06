package org.sla;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainServer extends Application {
    private ClientServerPictureViewerController myController;
    // Change multicastMode to enable multicast
    static boolean multicastMode = true;

    @Override
    public void start(Stage primaryStage) throws Exception{
        // Load View from xml description
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ClientServerPictureViewer.fxml"));
        Parent root = loader.load();

        Thread.currentThread().setName("PictureViewer MainServer GUI Thread");

        // Display the scene
        primaryStage.setTitle("PictureViewer SERVER");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        myController = loader.getController();
        myController.setServerMode();
        myController.setStage(primaryStage);

    }

    public static void main(String[] args) {
        launch(args);
    }
}
