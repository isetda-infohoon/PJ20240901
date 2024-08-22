package com.isetda.idpengine;

import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class IDPEngineController {
    private static final Logger log = LogManager.getLogger(IDPEngineController.class);
    private ConfigLoader configLoader = ConfigLoader.getInstance();
    String folderPath = configLoader.getResultFilePath();
    String jsonFolderPath = configLoader.getResultFilePath();

    public TextField inputImageFolderPath;
    public TextField inputResultFolderPath;

    private IDPEngineService service = new IDPEngineService();
    private IMGFileIOService imgFileIOService = new IMGFileIOService();
    private GoogleService googleService = new GoogleService();
//    private JsonService jsonService = new JsonService();

    //분리된 이미지 저장 변수
    private File[] imageAndPdfFiles;

    public void onButton1Click(ActionEvent event) throws IOException {
        imageAndPdfFiles = imgFileIOService.getFilteredFiles(inputImageFolderPath.getText());
        log.info("사용자로부터 받은 이미지 폴더 경로 : {} ",inputImageFolderPath.getText());

            imgFileIOService.copyFiles(imageAndPdfFiles);
            log.info("파일 복사 성공 : {} 개",imageAndPdfFiles.length );
            imgFileIOService.deleteFilesInFolder(inputImageFolderPath.getText());
            log.info("파일 삭제 성공 ");


        googleService.uploadAndOCR();
        JsonService.processMarking(folderPath,jsonFolderPath);
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
//        if (service.resultFolderPath.isEmpty()) {
//            service.resultFolderPath = inputResultFolderPath.getText();
//        }

        // 전달 받은 폴더 경로의 json 파일 필터링
        service.getFilteredJsonFiles();
        service.createFinalResultFile();
    }
}