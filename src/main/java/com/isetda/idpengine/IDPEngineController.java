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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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

    List<File> badFileImg = new ArrayList<>();


    String resultFilePath = configLoader.resultFilePath;

    @FXML
    public void initialize() {
        lood();
    }

    public void lood() {
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
            if (!inputImageFolderPath.getText().isEmpty()) {
                inputImageFolderPath.setPromptText(inputImageFolderPath.getText());
                inputImageFolderPath.setText("");
            } else if (!inputImageFolderPath.getPromptText().isEmpty()) {
                inputImageFolderPath.setText(inputImageFolderPath.getPromptText());
                inputImageFolderPath.setPromptText("");
            }
        }
    }

    private void resulthandleDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2 && event.isShiftDown() && event.isControlDown()) {
            if (!inputResultFolderPath.getText().isEmpty()) {
                inputResultFolderPath.setPromptText(inputResultFolderPath.getText());
                inputResultFolderPath.setText("");
            } else if (!inputResultFolderPath.getPromptText().isEmpty()) {
                inputResultFolderPath.setText(inputResultFolderPath.getPromptText());
                inputResultFolderPath.setPromptText("");
            }
        }
    }

    private void countryCodehandleDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2 && event.isShiftDown() && event.isControlDown()) {
            if (!countryCode.getText().isEmpty()) {
                countryCode.setPromptText(countryCode.getText());
                countryCode.setText("");
            } else if (!countryCode.getPromptText().isEmpty()) {
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

        String countryName = countryCode.getText();

        Set<String> allowedCountries = Set.of("FR", "US", "IT", "VN", "JP", "CN");

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
                    if (file.getName().toLowerCase().endsWith(".pdf")) {
                        IOService.copyFiles(file);
                        continue;
                    }
                    if (countryName.equals("KR_DEV")) {
                        googleService.uploadAndOCR(file);

                        try {
                            if (googleService.checkBadImg == true) {
                                IOService.copyFiles(file);

                                File dir = new File(inputResultFolderPath.getText());
                                String[] fileNames = dir.list();

                                String fileName = file.getName();
                                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                                baseName = baseName.replaceAll("-page\\d+", "");

                                // "미분류" 폴더 경로 생성
                                File uncategorizedDir = new File(inputResultFolderPath.getText() + "/KR_DEV");
                                if (!uncategorizedDir.exists()) {
                                    uncategorizedDir.mkdir(); // "미분류" 폴더가 없다면 생성
                                }

                                // baseName을 포함하는 모든 파일들을 필터링 (확장자에 상관없이)
                                List<File> files = new ArrayList<>();
                                for (String name : fileNames) {
                                    if (name.contains(baseName)) {  // baseName을 포함하는 모든 파일을 필터링
                                        files.add(new File(dir, name));
                                    }
                                }

                                // "미분류" 폴더로 파일 이동
                                for (File fileToMove : files) {
                                    File destFile = new File(uncategorizedDir, fileToMove.getName());
                                    if (fileToMove.renameTo(destFile)) {
                                        log.info("Moved file: {} to 'KR_DEV' folder.", fileToMove.getName());
                                    } else {
                                        log.error("Failed to move file: {}", fileToMove.getName());
                                    }
                                }
                            } else {
                                badFileImg.add(file);

                                File dir = new File(inputResultFolderPath.getText());
                                String[] fileNames = dir.list();

                                String fileName = file.getName();
                                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                                baseName = baseName.replaceAll("-page\\d+", "");

                                // "미분류" 폴더 경로 생성
                                File uncategorizedDir = new File(inputResultFolderPath.getText() + "/KR_DEV");
                                if (!uncategorizedDir.exists()) {
                                    uncategorizedDir.mkdir(); // "미분류" 폴더가 없다면 생성
                                }

                                // baseName을 포함하는 모든 파일들을 필터링 (확장자에 상관없이)
                                List<File> files = new ArrayList<>();
                                for (String name : fileNames) {
                                    if (name.contains(baseName)) {  // baseName을 포함하는 모든 파일을 필터링
                                        files.add(new File(dir, name));
                                    }
                                }

                                // "미분류" 폴더로 파일 이동
                                for (File fileToMove : files) {
                                    File destFile = new File(uncategorizedDir, fileToMove.getName());
                                    if (fileToMove.renameTo(destFile)) {
                                        log.info("Moved file: {} to 'KR_DEV' folder.", fileToMove.getName());
                                    } else {
                                        log.error("Failed to move file: {}", fileToMove.getName());
                                    }
                                }
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        //추가 버전 20250117
                    } else if (!allowedCountries.contains(countryName)) {
                        googleService.FullTextOCR(file);

                        try {
                            if (googleService.checkBadImg == true) {
                                IOService.copyFiles(file);
                            } else {
                                badFileImg.add(file);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        googleService.uploadAndOCR(file);

                        try {
                            if (googleService.checkBadImg == true) {
                                IOService.copyFiles(file);
                            } else {
                                badFileImg.add(file);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    }
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
            for (File file : badFileImg) {
                try {
                    IOService.ImgToPDF(file);
                    IOService.copyFiles(file);
                    googleService.uploadAndOCR(IOService.badImgToPDF);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (!allowedCountries.contains(countryName)) {
                movefilefulltext(inputResultFolderPath.getText());
            }

            Platform.runLater(() -> {
                progressBar.setProgress(1);
                if (googleService.EnablingGoogle == false) {
                    errorLabel.setText("error");
                } else {
                    errorLabel.setText("Files Copy and Upload success");
                }

                log.info("Number of image file copies: {}", imageAndPdfFiles.length);
                btn1.setDisable(false);

                // 작업이 완료되면 프로그램을 다시 최대화
                stage.setIconified(false);
            });
        });

        // 백그라운드 스레드 시작
        taskThread.setDaemon(true);
        taskThread.start();
        btn1files.setText("files in source folder : " + IOService.allFilesInSourceFolder.size()); // 파일 개수를 UI에 표시

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
            String RullFilePath = "";

            if (Objects.equals(countryName, "")) {
                RullFilePath = configLoader.jsonFilePath;
            } else {
                RullFilePath = new StringBuilder(jsonFilePath).insert(jsonFilePath.indexOf('.'), "_" + countryName).toString();
            }

            // 결과 출력
            System.out.println("결과: " + RullFilePath);
            try {
                jsonToByte = Files.readAllBytes(Paths.get(RullFilePath));
            } catch (IOException e) {
                errorLabel.setText("all success");
                throw new RuntimeException(e);
            }
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
            btn2files.setText("json files in result folder : " + documentService.jsonFiles.length);
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
        IMGService imgService = new IMGService();
        processing();
        excelService.configLoader = configLoader;
        documentService.configLoader = configLoader;

        // 전달 받은 폴더 경로의 json 파일 필터링
        documentService.jsonFiles = excelService.getFilteredJsonFiles();
        documentService.createFinalResultFile();
    }

    public void movefilefulltext(String outputpath) {
        File uncategorizedDir = new File(outputpath + "/미분류");
        File sourceDir = new File(outputpath);
        if (!uncategorizedDir.exists()) {
            uncategorizedDir.mkdir();
            log.info("Created '미분류' directory: {}", uncategorizedDir.getPath());
        }
        // 특정 경로에 있는 파일들을 리스트에 저장
        File[] files = sourceDir.listFiles();
        if (files == null || files.length == 0) {
            log.warn("No files found in the source directory: {}", outputpath);
            return;
        }

        // 파일들을 "미분류" 폴더로 이동
        for (File file : files) {
            if (file.isFile()) { // 디렉토리가 아닌 파일만 이동
                File destFile = new File(uncategorizedDir, file.getName());
                if (file.renameTo(destFile)) {
                    log.info("Moved file: {} to '미분류' folder.", file.getName());
                } else {
                    log.error("Failed to move file: {}", file.getName());
                }
            }

        }
    }
}