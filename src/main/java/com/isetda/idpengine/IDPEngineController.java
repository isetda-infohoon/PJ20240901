package com.isetda.idpengine;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class IDPEngineController {
    private static final Logger log = LogManager.getLogger(IDPEngineController.class);
    public ConfigLoader configLoader = ConfigLoader.getInstance();
    public TextField inputImageFolderPath;
    public TextField inputResultFolderPath;

    private ExcelService excelService = new ExcelService();
    private DocumentService documentService = new DocumentService();
    private JsonEnDecode jsonEnDecode = new JsonEnDecode();

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
        IOService IOService = new IOService();
        GoogleService googleService = new GoogleService();

        if (inputImageFolderPath.getText().isEmpty()){
            log.info("Source folder path(default): {}",configLoader.imageFolderPath);
            inputImageFolderPath.setText(configLoader.imageFolderPath);
        }else {
            configLoader.imageFolderPath = inputImageFolderPath.getText();
            log.info("Source folder path(input): {}",configLoader.imageFolderPath);
        }
        if (inputResultFolderPath.getText().isEmpty()){
            log.info("Result folder path(default): {}",configLoader.resultFilePath);
        }else {
            configLoader.resultFilePath = inputResultFolderPath.getText();
            log.info("Result folder path(input): {}",configLoader.resultFilePath);
        }

        configLoader.saveConfig(); // 변경된 경로를 XML 파일에 저장

        googleService.configLoader = configLoader;
        documentService.configLoader = configLoader;
        IOService.configLoader = configLoader;
        googleService.configLoader =configLoader;

        File resultFolder = new File(configLoader.resultFilePath);
        if (!resultFolder.exists()) {
            boolean created = resultFolder.mkdirs(); // 여러 폴더도 생성 가능
            if (created) {
                log.info("Folder created: {}", resultFilePath);
            } else {
                log.info("Result folder exists: {}",resultFilePath);
            }
        }
        imageAndPdfFiles = IOService.getFilteredFiles();
        int a =1;
        for(File file : imageAndPdfFiles){
            log.info("{} Start processing files: {}",a,file.getName());
            IOService.copyFiles(file);
            googleService.uploadAndOCR(file);
            a++;
        }
        log.info("Number of image file copies: {}", imageAndPdfFiles.length);

//        imgFileIOService.deleteFilesInFolder();
//        JsonService.processMarking(folderPath, jsonFolderPath);
    }

    public void onButton2Click(ActionEvent event) throws Exception {
        byte[] jsonToByte = Files.readAllBytes(Paths.get(configLoader.jsonFilePath));
        documentService.jsonData = JsonService.getJsonDictionary(JsonService.aesDecode(jsonToByte));
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
    public void classificationDocument() throws Exception {
        IMGService imgService =new IMGService();
        processing();
        excelService.configLoader = configLoader;

        // 전달 받은 폴더 경로의 json 파일 필터링
        documentService.jsonFiles = excelService.getFilteredJsonFiles();
        documentService.createFinalResultFile();
//        imgService.processMarking(documentService.matchjsonWord, configLoader.resultFilePath,);
    }
}