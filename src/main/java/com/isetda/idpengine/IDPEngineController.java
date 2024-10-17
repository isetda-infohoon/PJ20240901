package com.isetda.idpengine;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

public class IDPEngineController {
    private static final Logger log = LogManager.getLogger(IDPEngineController.class);
    public ConfigLoader configLoader = ConfigLoader.getInstance();
//    public TextInputControl inputImageFolderPath;
//    public TextField inputResultFolderPath;
    public PasswordField inputImageFolderPath;
    public PasswordField inputResultFolderPath;

    // PasswordField 추가 (기본적으로 경로를 숨기는 필드)[10/16 정다현]
    private PasswordField passwordField;

    public int jsonfiles;

    public Text errorLabel;

    private ExcelService excelService = new ExcelService();
    private DocumentService documentService = new DocumentService();


    private boolean shiftPressed = false; // Shift 키 상태를 추적하기 위한 플래그
    @FXML
    private GridPane gridPane;

    String resultFilePath = configLoader.resultFilePath;
    @FXML
    public void initialize() {
        lood();
    }

    public void lood(){
        inputImageFolderPath.setText(configLoader.imageFolderPath);
        inputResultFolderPath.setText(configLoader.resultFilePath);
//        inputImageFolderPath.setOnKeyPressed(this::handleKeyPressed);
//        inputImageFolderPath.setOnKeyReleased(this::handleKeyReleased);

        // 더블클릭 감지
        inputImageFolderPath.setOnMouseClicked(this::sourcehandleDoubleClick);
        inputResultFolderPath.setOnMouseClicked(this::resulthandleDoubleClick);
    }
//    // Shift 키 눌림 상태 체크
//    private void handleKeyPressed(KeyEvent event) {
//        if (event.getCode() == KeyCode.SHIFT) {
//            shiftPressed = true; // Shift 키가 눌리면 플래그 설정
//        }
//    }
//
//    // Shift 키가 떼어질 때 플래그 해제
//    private void handleKeyReleased(KeyEvent event) {
//        if (event.getCode() == KeyCode.SHIFT) {
//            shiftPressed = false; // Shift 키가 떼어지면 플래그 해제
//        }
//    }

    // 더블클릭 이벤트 핸들러
    private void sourcehandleDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2 && event.isShiftDown() && event.isControlDown()) {
            if(!inputImageFolderPath.getText().isEmpty()){
                inputImageFolderPath.setPromptText(inputImageFolderPath.getText());
                inputImageFolderPath.setText("");
            }
            else if(!inputImageFolderPath.getPromptText().isEmpty()) {
                inputImageFolderPath.setText(inputImageFolderPath.getPromptText());
                inputImageFolderPath.setPromptText("");
            }
        }
    }
    private void resulthandleDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2 && event.isShiftDown() && event.isControlDown()) {
            if(!inputResultFolderPath.getText().isEmpty()){
                inputResultFolderPath.setPromptText(inputResultFolderPath.getText());
                inputResultFolderPath.setText("");
            }
            else if(!inputResultFolderPath.getPromptText().isEmpty()) {
                inputResultFolderPath.setText(inputResultFolderPath.getPromptText());
                inputResultFolderPath.setPromptText("");
            }
        }
    }


    //분리된 이미지 저장 변수
    private File[] imageAndPdfFiles;
    public ProgressBar progressBar;
//    public ProgressBar progressBar2;

    public void onButton1Click() throws IOException {
        IOService IOService = new IOService();
        GoogleService googleService = new GoogleService();

        if (inputImageFolderPath.getText().isEmpty()) {
            log.info("Source folder path(default): {}", configLoader.imageFolderPath);
            inputImageFolderPath.setText(configLoader.imageFolderPath);
        } else {
            configLoader.imageFolderPath = inputImageFolderPath.getText();
            log.info("Source folder path(input): {}", configLoader.imageFolderPath);
        }

        if (inputResultFolderPath.getText().isEmpty()) {
            log.info("Result folder path(default): {}", configLoader.resultFilePath);
        } else {
            configLoader.resultFilePath = inputResultFolderPath.getText();
            log.info("Result folder path(input): {}", configLoader.resultFilePath);
        }

        configLoader.saveConfig(); // 변경된 경로를 XML 파일에 저장

        documentService.configLoader = configLoader;
        IOService.configLoader = configLoader;
        googleService.configLoader = configLoader;

        File resultFolder = new File(configLoader.resultFilePath);
        if (!resultFolder.exists()) {
            boolean created = resultFolder.mkdirs(); // 여러 폴더도 생성 가능
            if (created) {
                log.info("Folder created: {}", resultFilePath);
            } else {
                log.info("Result folder exists: {}", resultFilePath);
            }
        }

        imageAndPdfFiles = IOService.getFilteredFiles();
        int totalFiles = imageAndPdfFiles.length;

        // 파일 하나 처리될 때마다 progress 비율 계산
        double progressStep = 1.0 / totalFiles; // 전체 진행의 100%
        AtomicReference<Double> currentProgress = new AtomicReference<>(0.0); // AtomicReference 사용

        // 백그라운드에서 작업을 수행
        Thread taskThread = new Thread(() -> {
            int a = 1;
            for (File file : imageAndPdfFiles) {
                log.info("{} Start processing files: {}", a, file.getName());
                try {
                    IOService.copyFiles(file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    googleService.uploadAndOCR(file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                a++;

                // UI 업데이트는 JavaFX Application Thread에서 실행
                double updatedProgress = currentProgress.get() + progressStep;
                currentProgress.set(updatedProgress); // AtomicReference 업데이트

                Platform.runLater(() -> {
                    progressBar.setProgress(updatedProgress);
                    errorLabel.setText("Processing file: " + file.getName());

                });
            }

            // 모든 파일이 처리된 후 최종 50%로 고정
            Platform.runLater(() -> {
                progressBar.setProgress(1);
                errorLabel.setText("Files Copy and Upload success");

                log.info("Number of image file copies: {}", imageAndPdfFiles.length);
            });
        });

        // 백그라운드 스레드 시작
        taskThread.setDaemon(true);
        taskThread.start();

//        진행 바 추가 되기 전 버전
//        int a =1;
//        for(File file : imageAndPdfFiles){
//            log.info("{} Start processing files: {}",a,file.getName());
//            IOService.copyFiles(file);
//            googleService.uploadAndOCR(file);
//            a++;
//            errorLabel.setText("("+file.getName()+")" +"File Copy and Upload success");
//        }
//        log.info("Number of image file copies: {}", imageAndPdfFiles.length);
////        errorLabel.setText("Files Copy and Upload success");
//
////        imgFileIOService.deleteFilesInFolder();
////        JsonService.processMarking(folderPath, jsonFolderPath);
//    }
    }
    public double c;

    public void onButton2Click(ActionEvent event) throws Exception {
        byte[] jsonToByte = Files.readAllBytes(Paths.get(configLoader.jsonFilePath));
        documentService.jsonData = JsonService.getJsonDictionary(JsonService.aesDecode(jsonToByte));
//        documentService.setController(this);  // controller 설정
        classificationDocument();
        errorLabel.setText("all success");

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
        documentService.configLoader = configLoader;

        // 전달 받은 폴더 경로의 json 파일 필터링
        documentService.jsonFiles = excelService.getFilteredJsonFiles();
        documentService.createFinalResultFile();
    }
}