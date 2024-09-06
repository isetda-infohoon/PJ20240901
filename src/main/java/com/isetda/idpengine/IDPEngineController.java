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
    public TextField inputImageFolderPath;
    public TextField inputResultFolderPath;
    public String imageFolderPath;

    private IMGFileIOService imgFileIOService = new IMGFileIOService();
    private GoogleService googleService = new GoogleService();
    private ExcelService service = new ExcelService();

    String folderPath = configLoader.getResultFilePath();
    String jsonFolderPath = configLoader.getResultFilePath();


    //분리된 이미지 저장 변수
    private File[] imageAndPdfFiles;

    public void onButton1Click(ActionEvent event) throws IOException {
        processing();
        imageAndPdfFiles = imgFileIOService.getFilteredFiles(imageFolderPath);

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


    public void processing() {
        if (inputResultFolderPath.getText().isEmpty()) {
            imageFolderPath = configLoader.getImageFolderPath();
            log.info("이미지 폴더 기본 경로 : {} ", imageFolderPath);
        } else {
            imageFolderPath = inputResultFolderPath.getText();
            log.info("사용자 입력 이미지 폴더 경로 : {} ", imageFolderPath);
        }

        if (inputResultFolderPath.getText().isEmpty()) {
            service.resultFolderPath = configLoader.getResultFilePath();
            log.info("결과 파일 저장 기본 경로 : {} ", service.resultFolderPath);
        } else {
            service.resultFolderPath = inputResultFolderPath.getText();
            log.info("사용자 입력 결과 파일 저장 경로 : {} ", service.resultFolderPath);
        }
    }

    // 문서 분류
    public void classificationDocument() {
        // 전달 받은 폴더 경로의 json 파일 필터링
        service.getFilteredJsonFiles();
        service.createFinalResultFile();
    }
}