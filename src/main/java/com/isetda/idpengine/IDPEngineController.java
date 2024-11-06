package com.isetda.idpengine;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class IDPEngineController {
    private static final Logger log = LogManager.getLogger(IDPEngineController.class);
    public ConfigLoader configLoader = ConfigLoader.getInstance();
    public PasswordField inputImageFolderPath;
    public PasswordField inputResultFolderPath;
    public PasswordField countryCode;

    public int jsonfiles;

    public Text errorLabel;

    public Text btn1files;
    public Text btn2files;

    public Button btn1;
    public Button btn2;



    private ExcelService excelService = new ExcelService();
    private DocumentService documentService = new DocumentService();

    String resultFilePath = configLoader.resultFilePath;
    @FXML
    public void initialize() {
        lood();
    }

    public void lood(){
        inputImageFolderPath.setText(configLoader.imageFolderPath);
        inputResultFolderPath.setText(configLoader.resultFilePath);
        // 더블클릭 감지
        inputImageFolderPath.setOnMouseClicked(this::sourcehandleDoubleClick);
        inputResultFolderPath.setOnMouseClicked(this::resulthandleDoubleClick);
        countryCode.setOnMouseClicked(this::countryCodehandleDoubleClick);

    }

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
    private void countryCodehandleDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2 && event.isShiftDown() && event.isControlDown()) {
            if(!countryCode.getText().isEmpty()){
                countryCode.setPromptText(countryCode.getText());
                countryCode.setText("");
            }
            else if(!countryCode.getPromptText().isEmpty()) {
                countryCode.setText(countryCode.getPromptText());
                countryCode.setPromptText("");
            }
        }
    }


    //분리된 이미지 저장 변수
    private File[] imageAndPdfFiles;
    public ProgressBar progressBar;

    public void onButton1Click() throws IOException {
        // 현재 Stage 가져오기
        Stage stage = (Stage) btn1.getScene().getWindow();

        // 버튼을 누르면 프로그램 최소화
        stage.setIconified(true);

        btn1.setDisable(true);
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

        File sourceFolder = new File(configLoader.imageFolderPath);
        File[] sourceFiles = sourceFolder.listFiles(); // 폴더 안의 모든 파일 목록 가져오기


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
                btn1.setDisable(false);

                // 작업이 완료되면 프로그램을 다시 최대화
                stage.setIconified(false);
            });
        });

        // 백그라운드 스레드 시작
        taskThread.setDaemon(true);
        taskThread.start();
        btn1files.setText("files in source folder : " +IOService.allFilesInSourceFolder.size()); // 파일 개수를 UI에 표시

    }
    public double c;

    public void onButton2Click(ActionEvent event) throws Exception {
        // 현재 Stage 가져오기
        Stage stage = (Stage) btn2.getScene().getWindow();

        String countryName = countryCode.getText();

        // 버튼을 누르면 프로그램 최소화
        stage.setIconified(true);

        Thread taskThread = new Thread(() -> {
            int a = 1;
            btn2.setDisable(true);
            byte[] jsonToByte = null;

            String jsonFilePath = configLoader.jsonFilePath;
            String RullFilePath="";

            if(Objects.equals(countryName, "")){
                RullFilePath = configLoader.jsonFilePath;
            }
            else {
                RullFilePath = new StringBuilder(jsonFilePath).insert(jsonFilePath.indexOf('.'), "_"+countryName).toString();
            }

            // 결과 출력
            System.out.println("결과: " + RullFilePath);
            try {
                jsonToByte = Files.readAllBytes(Paths.get(RullFilePath));
            } catch (IOException e) {
                errorLabel.setText("all success");
                throw new RuntimeException(e);
            }
//        documentService.jsonData = JsonService.getJsonDictionary(JsonService.aesDecode(jsonToByte));
//        documentService.setController(this);  // controller 설정
            //documentService.jsonData = JsonService.getJsonDictionary(JsonService.aesDecode(jsonToByte));
            try {
                documentService.jsonData = JsonService.getJsonDictionary2(JsonService.aesDecode(jsonToByte));
            } catch (Exception e) {
                errorLabel.setText("all success");
                throw new RuntimeException(e);
            }
            try {
                classificationDocument();
            } catch (Exception e) {
                errorLabel.setText("all success");
                throw new RuntimeException(e);
            }
            errorLabel.setText("all success");
            btn2files.setText("json files in result folder : "+documentService.jsonFiles.length);
            // 모든 파일이 처리된 후 최종 50%로 고정
            Platform.runLater(() -> {
                btn2.setDisable(false);

                // 작업이 완료되면 프로그램을 다시 최대화
                stage.setIconified(false);
            });
        });

        // 백그라운드 스레드 시작
        taskThread.setDaemon(true);
        taskThread.start();
//        btn1files.setText("filse in source folder : " +IOService.allFilesInSourceFolder.size()); // 파일 개수를 UI에 표시
        //ExcelService.dataWriteExcel(documentService.jsonData);
    }

//        JsonService.processMarking(folderPath, jsonFolderPath);
//public void onButton2Click(ActionEvent event) throws Exception {
//    byte[] jsonToByte = Files.readAllBytes(Paths.get(configLoader.jsonFilePath));
//    documentService.jsonData = JsonService.getJsonDictionary(JsonService.aesDecode(jsonToByte));
//
//    // 진행률 바 초기화
//    progressBar.setProgress(0);
//    errorLabel.setText("Starting document classification...");
//    log.info("dd");
//
//    // 백그라운드 스레드에서 작업 실행
//    Thread thread = new Thread(() -> {
//        log.info("11");
//            try {
//                documentService.createFinalResultFile();
//                    public void updateProgress(final double progress, final String message) {
//                        Platform.runLater(new Runnable() {
//                            @Override
//                            public void run() {
//                                progressBar.setProgress(progress);
//                                errorLabel.setText(message);
//                            }
//                        });
//                    }
//                Platform.runLater(new Runnable() {
//                    @Override
//                    public void run() {
//                        progressBar.setProgress(1);
//                        errorLabel.setText("Document classification completed");
//                    }
//                });
//            } catch (final Exception e) {
//                Platform.runLater(new Runnable() {
//                    @Override
//                    public void run() {
//                        errorLabel.setText("Error: " + e.getMessage());
//                    }
//                });
//            }
//    });
//    thread.setDaemon(true);
//    thread.start();
//}

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
//        imgService.processMarking(documentService.matchjsonWord, configLoader.resultFilePath,);
    }

//    public void getDataset() {
//        Thread taskThread = new Thread(() -> {
//            byte[] jsonToByte = null;
//            try {
//                jsonToByte = Files.readAllBytes(Paths.get(configLoader.jsonFilePath));
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//            try {
//                documentService.jsonData = JsonService.getJsonDictionary2(JsonService.aesDecode(jsonToByte));
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//            //errorLabel.setText("all success");
//            //btn2files.setText("json files in result folder : "+documentService.jsonFiles.length);
////            // 모든 파일이 처리된 후 최종 50%로 고정
////            Platform.runLater(() -> {
////                btn2.setDisable(false);
////
////            });
//        });
//
//        // 백그라운드 스레드 시작
//        taskThread.setDaemon(true);
//        taskThread.start();
//
//        excelService.configLoader = configLoader;
//        documentService.configLoader = configLoader;
//
//        documentService.dataWriteExcel();
//    }
}