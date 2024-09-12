package com.isetda.idpengine;

import javafx.event.ActionEvent;
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



    private ExcelService service = new ExcelService();

    String resultFilePath = configLoader.getResultFilePath();

    File resultFolder = new File(resultFilePath);



    //분리된 이미지 저장 변수
    private File[] imageAndPdfFiles;

    public void onButton1Click(ActionEvent event) throws IOException {
        IMGFileIOService imgFileIOService = new IMGFileIOService();
        GoogleService googleService = new GoogleService();
        configLoader.resultFilePath = inputResultFolderPath.getText();
        imgFileIOService.configLoader = configLoader;
//        if (!resultFolder.exists()) {
//            boolean created = resultFolder.mkdirs(); // 여러 폴더도 생성 가능
//            if (created) {
//                log.info("폴더가 생성되었습니다: {}", resultFilePath);
//            } else {
//                log.error("폴더를 생성하는 데 실패했습니다: {}", resultFilePath);
//            }
//        }
//        processing();
        imageAndPdfFiles = imgFileIOService.getFilteredFiles(inputImageFolderPath.getText());
        googleService.RESULT_FILEPATH = inputResultFolderPath.getText();

        imgFileIOService.copyFiles(imageAndPdfFiles);
        log.info("이미지 파일 복사 개수 : {} 개", imageAndPdfFiles.length);

        googleService.uploadAndOCR(imageAndPdfFiles);
//        imgFileIOService.deleteFilesInFolder();
//        JsonService.processMarking(folderPath, jsonFolderPath);
    }

    public void onButton2Click(ActionEvent event) throws IOException {
        classificationDocument();
//        JsonService.processMarking(folderPath, jsonFolderPath);

    }

    public void processing() {
        if (inputImageFolderPath.getText().isEmpty()) {
            log.info("이미지 폴더 기본 경로 : {} ", configLoader.imageFolderPath);
        } else {
            configLoader.imageFolderPath = inputImageFolderPath.getText();
            log.info("사용자 입력 이미지 폴더 경로 : {} ", configLoader.imageFolderPath);
        }

        if (inputResultFolderPath.getText().isEmpty()) {
            service.resultFolderPath = configLoader.getResultFilePath();
            log.info("결과 파일 저장 경로 : {} ", configLoader.resultFilePath);
        } else {
            service.resultFolderPath = inputResultFolderPath.getText();
            configLoader.resultFilePath = inputResultFolderPath.getText();
            log.info("사용자 입력 결과 파일 저장 경로 : {} ", configLoader.resultFilePath );
        }
    }

    // 문서 분류
    public void classificationDocument() throws IOException {
        // 전달 받은 폴더 경로의 json 파일 필터링
        service.getFilteredJsonFiles();
        service.createFinalResultFile();
    }
}