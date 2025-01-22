package com.isetda.idpengine;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;

public class IDPEngineApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(IDPEngineApplication.class.getResource("IDPEngineView.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Basic IDP Engine");
        stage.setScene(scene);
        stage.setResizable(false); // 창 최대화 X
        stage.show();
    }

    public static void main(String[] args) {
        boolean canRun = true; // 로직 수행 가능여부 설정

        try{
            DatagramSocket ds = new DatagramSocket(4444); // 포트점유
        } catch (SocketException e) {
            System.out.println("동일한 프로그램이 동작중입니다. 포트 : " + 4444);
            e.printStackTrace();
            canRun = false;
        }
        if(canRun){
        launch();
        }
    }
}