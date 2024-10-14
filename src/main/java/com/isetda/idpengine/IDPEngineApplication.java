package com.isetda.idpengine;

import com.google.api.client.json.Json;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IDPEngineApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(IDPEngineApplication.class.getResource("IDPEngineView.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Intelligent Document Processing Engine");
        stage.setScene(scene);
        stage.setResizable(false); // 창 최대화 X
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}