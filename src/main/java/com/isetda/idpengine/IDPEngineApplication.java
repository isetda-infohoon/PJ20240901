package com.isetda.idpengine;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class IDPEngineApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(IDPEngineApplication.class.getResource("IDPEngineView.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Intelligent Document Processing Solution");
        stage.setScene(scene);
        stage.setResizable(false); // 창 최대화 X
        stage.show();
    }

    public static void main(String[] args) {
        //JsonService jsonService = new JsonService("C:\\Users\\isetda\\testImg\\독일 식품안전 인증서16_OCR_result.json");
        JsonService jsonService = new JsonService("C:\\Users\\suaah\\OneDrive\\바탕 화면\\test-folder\\result\\식품안전관리 인증서.json");
        jsonService.getWordPosition();

        launch();
    }
}