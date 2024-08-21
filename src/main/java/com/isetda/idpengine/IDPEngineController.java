package com.isetda.idpengine;

import javafx.event.ActionEvent;
import javafx.scene.control.TextField;

import java.io.IOException;

public class IDPEngineController {
    public TextField inputImageFolderPath;
    public TextField inputResultFolderPath;

    private IDPEngineService service = new IDPEngineService();


    public void onButton1Click(ActionEvent event) throws IOException {
        //preprocessing();
    }

    public void onButton2Click(ActionEvent event) {
        classificationDocument();
    }


//    // 전처리
//    public void preprocessing() throws IOException {
//        service.imageFolderPath = inputImageFolderPath.getText();
//        service.resultFolderPath = inputResultFolderPath.getText();
//
//        service.getFilteredFiles();
//        service.uploadImagesToBucket();
//        service.processVision();
//    }

    // 문서 분류
    public void classificationDocument() {
        if (service.resultFolderPath.isEmpty()) {
            service.resultFolderPath = inputResultFolderPath.getText();
        }

        // 전달 받은 폴더 경로의 json 파일 필터링
        service.getFilteredJsonFiles();
        service.createFinalResultFile();
    }
}