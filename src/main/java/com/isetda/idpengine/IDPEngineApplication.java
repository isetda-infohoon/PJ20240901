package com.isetda.idpengine;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Map;

public class IDPEngineApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(IDPEngineApplication.class.getResource("/com/isetda/idpengine/IDPEngineView.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Basic IDP Engine");
        stage.setScene(scene);
        stage.setResizable(false); // 창 최대화 X
        stage.show();
    }

    public static void main(String[] args) {
        ConfigLoader configLoader = ConfigLoader.getInstance();
        boolean canRun = true; // 로직 수행 가능여부 설정

        try{
            DatagramSocket ds = new DatagramSocket(4444); // 포트점유
        } catch (SocketException e) {
            System.out.println("동일한 프로그램이 동작중입니다. 포트 : " + 4444);
            e.printStackTrace();
            canRun = false;
        }
        if(canRun){
            if (!configLoader.apiUsageFlag) {
                launch();
            } else {
//                String jsonFilePath = "C:\\Users\\suaah\\OneDrive\\바탕 화면\\IDP TEST\\result\\company_cn_12_result.dat";
//                JsonService jsonService = new JsonService(jsonFilePath);
//
//                System.out.println("======== json Locale ========");
//                System.out.println(jsonService.jsonLocal);
//                System.out.println("\n======== json Collection ========");
//                for (Map<String, Object> item : jsonService.jsonCollection) {
//                    System.out.println(item);
//                }
//                System.out.println("\n======== json Collection2 ========");
//                for (Map<String, Object> item3 : JsonService.jsonCollection2) {
//                    System.out.println(item3);
//                }

                ProcessingState sharedState = new ProcessingState();
                IDPEngineController controller = new IDPEngineController(sharedState);
                IDPPollingScheduler scheduler = new IDPPollingScheduler(controller);
                DocumentService documentService = new DocumentService();
                documentService.sharedState = sharedState;
                scheduler.startPolling();
            }
        }
    }
}