package com.isetda.idpengine;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class IDPEngineController {
    private static final Logger log = LogManager.getLogger(IDPEngineController.class);
    public ConfigLoader configLoader = ConfigLoader.getInstance();
    public TextField inputImageFolderPath;
    public TextField inputResultFolderPath;

    private ExcelService excelService = new ExcelService();

    String resultFilePath = configLoader.resultFilePath;
    @FXML
    public void initialize() {
        lood();
    }

    public void lood(){
        inputImageFolderPath.setText(configLoader.imageFolderPath);
        inputResultFolderPath.setText(configLoader.resultFilePath);
    }

    //분리된 이미지 저장 변수
    private File[] imageAndPdfFiles;

    public void onButton1Click(ActionEvent event) throws IOException {
        IMGFileIOService imgFileIOService = new IMGFileIOService();
        GoogleService googleService = new GoogleService();

        if (inputImageFolderPath.getText().isEmpty()){
            log.info("소스 폴더 경로(기본) : {}",configLoader.imageFolderPath);
            inputImageFolderPath.setText(configLoader.imageFolderPath);
        }else {
            configLoader.imageFolderPath = inputImageFolderPath.getText();
            log.info("소스 폴더 경로(입력값) : {}",configLoader.imageFolderPath);
        }
        if (inputResultFolderPath.getText().isEmpty()){
            log.info("결과 폴더 경로(기본) : {}",configLoader.resultFilePath);
        }else {
            configLoader.resultFilePath = inputResultFolderPath.getText();
            log.info("결과 폴더 경로(입력값) : {}",configLoader.resultFilePath);
        }

        configLoader.saveConfig(); // 변경된 경로를 XML 파일에 저장

        imgFileIOService.configLoader = configLoader;
        googleService.configLoader =configLoader;

        File resultFolder = new File(configLoader.resultFilePath);
        if (!resultFolder.exists()) {
            boolean created = resultFolder.mkdirs(); // 여러 폴더도 생성 가능
            if (created) {
                log.info("폴더가 생성되었습니다: {}", resultFilePath);
            } else {
                log.info("결과 폴더가 존재합니다 : {}",resultFilePath);
            }
        }
        imageAndPdfFiles = imgFileIOService.getFilteredFiles();
        int a =1;
        for(File file : imageAndPdfFiles){
            log.info("{}번째 파일 처리 시작 : {}",a,file.getName());
            imgFileIOService.copyFiles(file);
            googleService.uploadAndOCR(file);
            a++;
        }
        log.info("이미지 파일 복사 개수 : {} 개", imageAndPdfFiles.length);

//        imgFileIOService.deleteFilesInFolder();
//        JsonService.processMarking(folderPath, jsonFolderPath);
    }

    public void onButton2Click(ActionEvent event) throws IOException {
        classificationDocument();
//        JsonService.processMarking(folderPath, jsonFolderPath);

    }

    public void processing() {
        configLoader.resultFilePath = inputResultFolderPath.getText();

//        if (inputImageFolderPath.getText().isEmpty()) {
//            log.info("이미지 폴더 기본 경로 : {} ", configLoader.imageFolderPath);
//        } else {
//            configLoader.imageFolderPath = inputImageFolderPath.getText();
//            log.info("사용자 입력 이미지 폴더 경로 : {} ", configLoader.imageFolderPath);
//        }
//
//        if (inputResultFolderPath.getText().isEmpty()) {
//            service.resultFolderPath = configLoader.getResultFilePath();
//            log.info("결과 파일 저장 경로 : {} ", configLoader.resultFilePath);
//        } else {
//            service.resultFolderPath = inputResultFolderPath.getText();
//            configLoader.resultFilePath = inputResultFolderPath.getText();
//            log.info("사용자 입력 결과 파일 저장 경로 : {} ", configLoader.resultFilePath );
//        }
    }

    // 문서 분류
    public void classificationDocument() throws IOException {
        processing();
        excelService.configLoader = configLoader;

        // 전달 받은 폴더 경로의 json 파일 필터링
        excelService.getFilteredJsonFiles();
        excelService.createFinalResultFile();
        JsonService.processMarking(excelService.getExcelData(), configLoader.resultFilePath, excelService.docType);

    }
}