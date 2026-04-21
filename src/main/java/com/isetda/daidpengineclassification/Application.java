package com.isetda.daidpengineclassification;

import com.isetda.daidpengineclassification.service.ClassificationService;
import com.mashape.unirest.http.exceptions.UnirestException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Application extends javafx.application.Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("/com/isetda/daidpengineclassification/IDPEngineView.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("DA.IDP Engine Classification");
        stage.setScene(scene);
        stage.setResizable(false); // 창 최대화 X
        stage.show();
    }

    public static void main(String[] args) throws UnirestException {
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

                ClassificationService classificationService = new ClassificationService();
                classificationService.callUpdateUserApi("START");

                ProcessingState sharedState = new ProcessingState();
                Controller controller = new Controller(sharedState);
                PollingScheduler scheduler = new PollingScheduler(controller);
                DocumentService documentService = new DocumentService();
                documentService.sharedState = sharedState;
                scheduler.startPolling();
            }
        }
    }
}