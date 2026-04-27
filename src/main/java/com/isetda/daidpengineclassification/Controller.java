package com.isetda.daidpengineclassification;

import com.isetda.daidpengineclassification.service.DocuAnalyzerService;
import com.isetda.daidpengineclassification.service.GoogleService;
import com.isetda.daidpengineclassification.service.ClassificationService;
import com.isetda.daidpengineclassification.service.SynapService;
import com.mashape.unirest.http.exceptions.UnirestException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Controller {
    private static final Logger log = LogManager.getLogger(Controller.class);
    private final ProcessingState state;
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
    private ClassificationService ClassificationService = new ClassificationService();

    private Stage stage;

    List<File> badFileImg = new ArrayList<>();

    String resultFilePath = configLoader.resultFilePath;

    FileInfo unitFileInfo;
    List<FileInfo> uploadFileList = new ArrayList<>();

    double progressStep;
    AtomicReference<Double> currentProgress;

    String subPath;

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

    public Controller() {
        this.state = new ProcessingState();
    }

    public Controller(ProcessingState state) {
        this.state = state;
    }

    public boolean isProcessing() {
        return state.isProcessing();
    }

    public void setProcessing(boolean value) {
        state.setProcessing(value);
    }

    public void processPendingFiles() throws Exception {
        NetworkDriveConnector connector = new NetworkDriveConnector();
        if (connector.connectNetworkDrive()) {
            log.trace("connect network drive: true");
        } else {
            log.trace("connect network drive: false");
        }

//        LocalDate today = LocalDate.now();
//        String formattedDate = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        LocalDate startDate = LocalDate.now().minusDays(configLoader.processPeriodDays);
        String formattedDate = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        //List<FileInfo> pendingFiles = apiCaller.getFileWithStatus(configLoader.apiUserId);
        List<FileInfo> pendingFiles = ClassificationService.getAllFilesWithCase(configLoader.apiUserId, configLoader.serviceType, "", 0, formattedDate);

        // 시간순 정렬
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
//        pendingFiles.sort(Comparator.comparing(file -> {
//                    try {
//                        return LocalDateTime.parse(file.getCreateDateTime(), formatter);
//                    } catch (Exception e) {
//                        return LocalDateTime.MIN; // 오류 시 가장 오래된 값으로 처리
//                    }
//                })
//        );
        pendingFiles.sort(Comparator.comparing(file -> {
            try {
                return LocalDateTime.parse(file.getCreateDateTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                log.warn("날짜 파싱 실패: {}", file.getCreateDateTime());
                return LocalDateTime.MIN;
            }
        }));

        log.debug("------ 정렬된 파일 목록 ------");
        for (FileInfo file : pendingFiles) {
            log.debug("파일명: " + file.getFilename() + ", 생성일시: " + file.getCreateDateTime());
        }

        for (FileInfo info : pendingFiles) {
            log.debug(info.getUserId());
            log.debug(info.getFilename());
            log.debug(info.getLanguage());
        }

        //uploadFileList = pendingFiles;

        if (pendingFiles != null && !pendingFiles.isEmpty()) {
            //log.info("처리할 파일이 {}개 존재합니다.", pendingFiles.size());

            // TODO: button1, 2 실행 메소드 작성
            //onButton1Click();
            //excelService.deleteFileList();

            for (FileInfo fileInfo : pendingFiles) {
                if (fileInfo.getClassificationType() == null || fileInfo.getClassificationType().trim().isEmpty()) {
                    log.warn("파일 제외됨 (ClassificationType이 null): {}", fileInfo.getFilename());
                    continue;
                }
                unitFileInfo = fileInfo;
                //onButton1Click();
                onButton1API();
            }
        } else {
            log.info("처리할 파일이 없습니다.");
            state.setProcessing(false);
            //log.info("setProcessing : {}", state.isProcessing());
        }
    }


    //분리된 이미지 저장 변수
    private File[] imageAndPdfFiles;
    private List<FileWithLookupName> imageAndPdfFilesWithAPI;
    public ProgressBar progressBar;

    public void onButton1Click() throws IOException {
        if (!configLoader.apiUsageFlag) {
            // 현재 Stage 가져오기
            stage = (Stage) btn1.getScene().getWindow();
            // 버튼을 누르면 프로그램 최소화
            stage.setIconified(true);
        }

        Set<String> allowedCountries = Set.of("FR", "US", "IT", "VN", "JP", "CN", "KR");

        if (!configLoader.apiUsageFlag) {
            btn1.setDisable(true);
        }

        GoogleService googleService = new GoogleService();
        SynapService synapService = new SynapService();
        IOService ioService = new IOService();

        if (!configLoader.apiUsageFlag) {
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
        } else {
            // UI가 없을 경우, 설정값 그대로 사용
            log.info("Source folder path(config): {}", configLoader.imageFolderPath);
            log.info("Result folder path(config): {}", configLoader.resultFilePath);
        }

        configLoader.saveConfig(); // 변경된 경로를 XML 파일에 저장

        documentService.configLoader = configLoader;
        ioService.configLoader = configLoader;
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


        if (!configLoader.apiUsageFlag) {
            imageAndPdfFiles = ioService.getFilteredFiles();
            // 파일 하나 처리될 때마다 progress 비율 계산
            int totalFiles = imageAndPdfFiles.length;
            progressStep = 1.0 / totalFiles; // 전체 진행의 100%
            currentProgress = new AtomicReference<>(0.0); // AtomicReference 사용
        } else {
            //imageAndPdfFilesWithAPI = ioService.getFilesWithAPIAndExtractedImages();
            imageAndPdfFilesWithAPI = ioService.getFileWithAPIAndExtractedImages(unitFileInfo);
        }

        // 백그라운드에서 작업을 수행
        Thread taskThread = new Thread(() -> {
            int a = 1;

            if (configLoader.apiUsageFlag) {
                try {
                    if (imageAndPdfFilesWithAPI != null || !imageAndPdfFilesWithAPI.isEmpty()) {
                        for (FileWithLookupName item : imageAndPdfFilesWithAPI) {
                            File file = item.file;
                            log.info("{} Start processing files: {}", a, file.getName());

                            FileInfo fileInfo;
                            try {
                                fileInfo = ClassificationService.getFileByName(configLoader.apiUserId, file.getName());
                                System.out.println("get fileInfo success : " + fileInfo.getFilename());
                            } catch (UnirestException e) {
                                throw new RuntimeException(e);
                            }

                            if (fileInfo != null && fileInfo.getFilename() != null) {
                                String countryName = fileInfo.getLanguage();
                                log.info("{} COUNTRY NAME : {}", fileInfo.getFilename(), countryName);

                                if (fileInfo.getServiceType() == null) {
                                    //fileInfo.setOcrServiceType(configLoader.ocrServiceType);
                                }

                                try {
                                    if (file.getName().toLowerCase().endsWith(".pdf")) {
                                        // PDF 처리
//                                IOService.copyFiles(file);
//                                int maxPage = getPdfPageCount(configLoader.imageFolderPath + File.separator + fileInfo.getFilename());
//                                apiCaller.callDivisionApi(configLoader.apiUserId, maxPage, fileInfo.getFilename(), fileInfo.getOcrServiceType());
                                        continue;
                                    }
                                    if (countryName.equals("KR_DEV")) {
                                        googleService.uploadAndOCR(file);

                                        try {
                                            if (googleService.checkBadImg == true) {
                                                ioService.copyFiles(file);

                                                //File dir = new File(inputResultFolderPath.getText());
                                                File dir = new File(configLoader.resultFilePath);
                                                String[] fileNames = dir.list();

                                                String fileName = file.getName();
                                                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                                                baseName = baseName.replaceAll("-page\\d+", "");

                                                // "미분류" 폴더 경로 생성
                                                //File uncategorizedDir = new File(inputResultFolderPath.getText() + "/KR_DEV");
                                                File uncategorizedDir = new File(configLoader.resultFilePath + "/KR_DEV");
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

                                                // File dir = new File(inputResultFolderPath.getText());
                                                File dir = new File(configLoader.resultFilePath);
                                                String[] fileNames = dir.list();

                                                String fileName = file.getName();
                                                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                                                baseName = baseName.replaceAll("-page\\d+", "");

                                                // "미분류" 폴더 경로 생성
                                                // File uncategorizedDir = new File(inputResultFolderPath.getText() + "/KR_DEV");
                                                File uncategorizedDir = new File(configLoader.resultFilePath + "/KR_DEV");
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
                                        if (fileInfo.getServiceType().contains("google")) {
                                            googleService.FullTextOCR(file);
                                        } else if (fileInfo.getServiceType().contains("synap")) {
                                            String subPath = "";
                                            synapService.synapOCR(file, subPath);
                                        } else {
                                            log.info("The file operation is skipped because there is no matching OCR Service Type.");
                                            continue;
                                        }

                                        try {
                                            if (googleService.checkBadImg == true) {
                                                ioService.copyFiles(file);
                                            } else {
                                                badFileImg.add(file);
                                            }
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    } else {
                                        // 국가 리스트 포함
                                        if (fileInfo.getServiceType().contains("google")) {
                                            googleService.uploadAndOCR(file);
                                        } else if (fileInfo.getServiceType().contains("synap")) {
                                            String subPath = "";
                                            synapService.synapOCR(file, subPath);
                                        } else {
                                            // OCR SERVICE TYPE이 GOOGLE, SYNAP에 해당되지 않는 경우
                                            log.info("The file operation is skipped because there is no matching OCR Service Type.");
                                            continue;
                                        }

                                        try {
                                            if (googleService.checkBadImg == true || synapService.checkBadImg == true) {
                                                ioService.copyFiles(file);
                                            } else {
                                                badFileImg.add(file);
                                            }
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }

                                    }
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                a++;
                            }
                        }

                        try {
                            onButton2Click();
                        } catch (Exception e) {
                            log.error("Error executing onButton2Click", e);
                        }
                    } else {
                        log.info("Skipping because there are no files to process in the folder");
                        state.setProcessing(false);
                        log.info("setProcessing : {}", state.isProcessing());
                    }
                } finally {
                    if (state.isProcessing()) {
                        state.setProcessing(false); // 작업이 끝난 후에만 false로 설정
                        log.info("setProcessing : {}", state.isProcessing());
                    }
                }
            } else {
                String countryName = countryCode.getText();

                for (File file : imageAndPdfFiles) {
                    log.info("{} Start processing files: {}", a, file.getName());
                    try {
                        if (file.getName().toLowerCase().endsWith(".pdf")) {
                            ioService.copyFiles(file);
                            continue;
                        }
                        if (countryName.equals("KR_DEV")) {
                            googleService.uploadAndOCR(file);

                            try {
                                if (googleService.checkBadImg == true) {
                                    ioService.copyFiles(file);

                                    //File dir = new File(inputResultFolderPath.getText());
                                    File dir = new File(configLoader.resultFilePath);
                                    String[] fileNames = dir.list();

                                    String fileName = file.getName();
                                    String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                                    baseName = baseName.replaceAll("-page\\d+", "");

                                    // "미분류" 폴더 경로 생성
                                    //File uncategorizedDir = new File(inputResultFolderPath.getText() + "/KR_DEV");
                                    File uncategorizedDir = new File(configLoader.resultFilePath + "/KR_DEV");
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

                                    // File dir = new File(inputResultFolderPath.getText());
                                    File dir = new File(configLoader.resultFilePath);
                                    String[] fileNames = dir.list();

                                    String fileName = file.getName();
                                    String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                                    baseName = baseName.replaceAll("-page\\d+", "");

                                    // "미분류" 폴더 경로 생성
                                    // File uncategorizedDir = new File(inputResultFolderPath.getText() + "/KR_DEV");
                                    File uncategorizedDir = new File(configLoader.resultFilePath + "/KR_DEV");
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
                                    ioService.copyFiles(file);
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
                                    ioService.copyFiles(file);
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

                    if (!configLoader.apiUsageFlag) {
                        Platform.runLater(() -> {
                            progressBar.setProgress(updatedProgress);
                            errorLabel.setText("Processing file: " + file.getName());
                        });
                    }
                }

                if (!allowedCountries.contains(countryName)) {
                    movefilefulltext(configLoader.resultFilePath);
                }

            }

            // TODO
            for (File file : badFileImg) {
                try {
                    ioService.ImgToPDF(file);
                    ioService.copyFiles(file);
                    googleService.uploadAndOCR(ioService.badImgToPDF);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

//            // 수정 - 위쪽으로 코드 위치 옮김
//            if (!allowedCountries.contains(countryName)) {
//                // movefilefulltext(inputResultFolderPath.getText());
//                movefilefulltext(configLoader.resultFilePath);
//            }

            if (!configLoader.apiUsageFlag) {
                Platform.runLater(() -> {
                    progressBar.setProgress(1);
                    if (googleService.EnablingGoogle == false) {
                        errorLabel.setText("error");
                    } else {
                        errorLabel.setText("Files Copy and Upload success");
                    }

                    log.info("Number of image file copies: {}", imageAndPdfFiles.length);
                    btn1.setDisable(false);

                    if (!configLoader.apiUsageFlag) {
                        // 작업이 완료되면 프로그램을 다시 최대화
                        stage.setIconified(false);
                    }
                });
            }
        });

        // 백그라운드 스레드 시작
        taskThread.setDaemon(true);
        taskThread.start();
        if (!configLoader.apiUsageFlag) {
            btn1files.setText("files in source folder : " + ioService.allFilesInSourceFolder.size()); // 파일 개수를 UI에 표시
        }
    }

    public double c;

    public void onButton2Click() throws Exception {
        if (!configLoader.apiUsageFlag) {
            // 현재 Stage 가져오기
            Stage stage = (Stage) btn2.getScene().getWindow();
            // 버튼을 누르면 프로그램 최소화
            stage.setIconified(true);

            String countryName = countryCode.getText();

            Thread taskThread = new Thread(() -> {
                int a = 1;
                btn2.setDisable(true);
                byte[] jsonToByte = null;

//                String jsonFilePath = configLoader.ruleFilePath;
//                String RullFilePath = "";
//
//                if (Objects.equals(countryName, "")) {
//                    RullFilePath = configLoader.ruleFilePath;
//                } else {
//                    RullFilePath = new StringBuilder(jsonFilePath).insert(jsonFilePath.indexOf('.'), "_" + countryName).toString();
//                }

                String jsonFilePath = configLoader.ruleFilePath;
                String folderPath = configLoader.ruleFolderPath;
                String ruleFileName;
                String ruleFilePath = "";

                if (countryName == null || countryName.trim().isEmpty()) {
                    ruleFileName = jsonFilePath;
                } else {
                    int dot = jsonFilePath.lastIndexOf('.');
                    if (dot == -1) {
                        ruleFileName = jsonFilePath + "_" + countryName;
                    } else {
                        ruleFileName = new StringBuilder(jsonFilePath).insert(dot, "_" + countryName).toString();
                    }
                }

                if (folderPath != null && !folderPath.trim().isEmpty()) {
                    ruleFilePath = folderPath + java.io.File.separator + ruleFileName;
                } else {
                    ruleFilePath = ruleFileName;
                }

                // 결과 출력
                System.out.println("결과: " + ruleFilePath);
                try {
                    jsonToByte = Files.readAllBytes(Paths.get(ruleFilePath));
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
        } else {
            // API 사용 분류 진행 (국가 코드 별로 해당하는 파일을 찾아 분류 진행)
            Thread taskThread = new Thread(() -> {
                try {
                    List<String> countryNames = List.of("FR", "US", "IT", "VN", "JP", "CN", "KR");

                    for (String countryName : countryNames) {
                        System.out.println("Current country name : " + countryName);
                        String jsonFilePath = configLoader.ruleFilePath;

                        // 확장자 앞에 countryName 추가
                        int dot = jsonFilePath.lastIndexOf('.');
                        String ruleFileName = (dot == -1)
                                ? jsonFilePath + "_" + countryName
                                : jsonFilePath.substring(0, dot) + "_" + countryName + jsonFilePath.substring(dot);

                        String folderPath = configLoader.ruleFolderPath;  // 새로 추가한 config 값

                        String ruleFilePath;
                        if (folderPath != null && !folderPath.trim().isEmpty()) {
                            ruleFilePath = folderPath + File.separator + ruleFileName;
                        } else {
                            ruleFilePath = ruleFileName;   // 기존 방식 → 작업디렉토리 기준
                        }

                        // 현재 국가 코드에 해당하는 파일 정보들을 조회
//                        List<FileInfo> filesForCountry = uploadFileList.stream()
//                                .filter(info -> countryName.equals(info.getLanguage()))
//                                .collect(Collectors.toList());
//
//                        if (filesForCountry.isEmpty()) {
//                            log.info("국가 코드 {}에 해당하는 파일이 없어 분류를 생략합니다.", countryName);
//                            continue;
//                        }

                        if (!countryName.equals(unitFileInfo.getLanguage())) {
                            log.info("국가 코드 {}에 해당하지 않아 분류를 생략합니다.", countryName);
                        }

                        byte[] jsonToByte;

                        try {
                            // RULE 파일 디코딩
                            jsonToByte = Files.readAllBytes(Paths.get(ruleFilePath));
                            documentService.jsonData = JsonService.getJsonDictionary2(JsonService.aesDecode(jsonToByte));
                        } catch (Exception e) {
                            log.error("Rule 파일 처리 실패: {}", ruleFilePath);
                            continue;
                        }

                        File resultDir = new File(configLoader.resultFilePath);
                        File[] datFiles = resultDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".dat"));

                        if (datFiles == null || datFiles.length == 0) {
                            log.warn("분류할 .dat 파일이 없습니다.");
                            continue;
                        }

                        // API 전체 리스트로 돌 때 (uploadFileList 사용 시)
//                        List<File> matchedFiles = Arrays.stream(datFiles)
//                                .filter(datFile -> {
//                                    String datName = datFile.getName();
//                                    String datBaseName = datName.replaceAll("(_result)?\\.dat$", ""); // "_result.dat" 또는 ".dat" 제거
//
//                                    return filesForCountry.stream()
//                                            .anyMatch(info -> {
//                                                String imageBaseName = info.getFilename().replaceAll("\\.[^.]+$", ""); // 확장자 제거
//                                                return datBaseName.contains(imageBaseName);
//                                            });
//                                })
//                                .collect(Collectors.toList());

                        // 파일 단위로 돌 때 (unitFileInfo 사용 시)
                        List<File> matchedFiles = Arrays.stream(datFiles)
                                .filter(datFile -> {
                                    String datBaseName = datFile.getName().replaceAll("(_result)?\\.dat$", "");

                                    String imageBaseName = unitFileInfo.getFilename().replaceAll("\\.[^.]+$", "");
                                    return datBaseName.contains(imageBaseName);
                                })
                                .collect(Collectors.toList());

                        // 정렬 로직 추가
                        Comparator<File> naturalOrderComparator = (f1, f2) -> compareNaturally(f1.getName(), f2.getName());
                        matchedFiles.sort(naturalOrderComparator);

                        // 정렬된 파일을 배열로 변환
                        documentService.jsonFiles = matchedFiles.toArray(new File[0]);

                        // 디버깅 출력
                        for (File file : matchedFiles) {
                            log.info("JSON FILE LIST : " + file.getName());
                        }
                        log.info("MATCHED FILES SIZE : " + matchedFiles.size());

//                        documentService.jsonFiles = matchedFiles.toArray(new File[0]);
//
//                        for (File file : matchedFiles) {
//                            System.out.println("JSON FILE LIST : " + file.getName());
//                            System.out.println("MATCHED FILES SIZE : " + matchedFiles.size());
//                        }

                        try {
                            classificationDocument();
                        } catch (Exception e) {
                            log.error("분류 실패: {}", e.getMessage());
                        }
                    }

                    log.info("API-based classification completed");

                    configLoader.resultFilePath = resultFilePath;
                    excelService.configLoader = configLoader;
                    //excelService.deleteFileList();
                } finally {
                    //excelService.deleteFileList();
//                    state.setProcessing(false);
//                    log.info("setProcessing : {}", state.isProcessing());
                }
            });

            taskThread.start();
        }
    }

    public void onButton1API() throws IOException {
        Set<String> allowedCountries = Set.of("FR", "US", "IT", "VN", "JP", "CN", "KR");

        GoogleService googleService = new GoogleService();
        SynapService synapService = new SynapService();
        DocuAnalyzerService docuAnalyzerService = new DocuAnalyzerService();
        IOService ioService = new IOService();

        // UI가 없을 경우, 설정값 그대로 사용
        //log.info("Source folder path(config): {}", configLoader.imageFolderPath);
        //log.info("Result folder path(config): {}", configLoader.resultFilePath);

        configLoader.saveConfig(); // 변경된 경로를 XML 파일에 저장

        // 파일이 없으면 다음 작업으로 넘어감 : TEST 필요
//        File imageFolder = new File(configLoader.imageFolderPath);
//        File[] sourceFiles = imageFolder.listFiles((dir, name) -> name.toLowerCase().contains(unitFileInfo.getFilename().toLowerCase()));
//
//        if (sourceFiles == null || sourceFiles.length == 0) {
//            log.info("작업 대상 파일이 폴더에 존재하지 않습니다: {}", unitFileInfo.getFilename());
//            return;
//        }

        documentService.configLoader = configLoader;
        ioService.configLoader = configLoader;
        googleService.configLoader = configLoader;

        for (FolderMapping mapping : configLoader.folderMappings) {
            File resultFolder = new File(mapping.getResultFilePath());
            if (!resultFolder.exists()) {
                boolean created = resultFolder.mkdirs(); // 여러 폴더도 생성 가능
                if (created) {
                    log.debug("Folder created: {}", mapping.getResultFilePath());
                } else {
                    log.debug("Result folder exists: {}", mapping.getResultFilePath());
                }
            }
        }

        //imageAndPdfFilesWithAPI = ioService.getFilesWithAPIAndExtractedImages();
        imageAndPdfFilesWithAPI = ioService.getFileWithAPIAndExtractedImages(unitFileInfo);

        int a = 1;

        try {
            if (imageAndPdfFilesWithAPI != null || !imageAndPdfFilesWithAPI.isEmpty()) {
                //configLoader.imageFolderPath = ioService.imagePath;
                String configResultFilePath = configLoader.resultFilePath;
                resultFilePath = ioService.fullResultPath;
                configLoader.resultFilePath = resultFilePath;

                subPath = ioService.subPath;
                documentService.subPath = subPath;

                for (FileWithLookupName item : imageAndPdfFilesWithAPI) {
                    File file = item.file;
                    log.info("{} Start processing files: {}", a, subPath + file.getName());

                    FileInfo fileInfo;
                    try {
                        // TODO: GroupUID까지 넣어서 검색하는 것 (같은 fileName의 데이터가 여러개 존재할 경우 groupUID 확인 필요)
                        fileInfo = ClassificationService.getFileByName(configLoader.apiUserId, subPath + item.apiLookupName, unitFileInfo.getGroupUID());
                    } catch (UnirestException e) {
                        throw new RuntimeException(e);
                    }

                    if (fileInfo.getTaskName().equals("DEFAULT")) {
                        documentService.taskName = "";
                    } else {
                        documentService.taskName = fileInfo.getTaskName();
                    }

                    if (fileInfo != null && fileInfo.getFilename() != null) {
                        //TODO: fileinfo.getServiceType = ai.vision 일 경우 분기

                        String countryName = fileInfo.getLanguage();
                        log.debug("{} COUNTRY NAME : {}", fileInfo.getFilename(), countryName);

                        if (fileInfo.getServiceType() == null) {
                            //fileInfo.setOcrServiceType(configLoader.ocrServiceType);
                        }

                        try {
                            String ext = FileExtensionUtil.getExtension(file.getName());

                            if (configLoader.usePdfExtractImage && file.getName().toLowerCase().endsWith(".pdf")) {
                                // PDF 처리
//                                IOService.copyFiles(file);
//                                int maxPage = getPdfPageCount(configLoader.imageFolderPath + File.separator + fileInfo.getFilename());
//                                apiCaller.callDivisionApi(configLoader.apiUserId, maxPage, fileInfo.getFilename(), fileInfo.getOcrServiceType());
                                continue;
                            }
                            if (countryName.equals("KR_DEV")) {
                                googleService.uploadAndOCR(file);

                                try {
                                    if (googleService.checkBadImg == true) {
                                        ioService.copyFiles(file);

                                        //File dir = new File(inputResultFolderPath.getText());
                                        File dir = new File(configLoader.resultFilePath);
                                        String[] fileNames = dir.list();

                                        String fileName = file.getName();
                                        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                                        baseName = baseName.replaceAll("-page\\d+", "");

                                        // "미분류" 폴더 경로 생성
                                        //File uncategorizedDir = new File(inputResultFolderPath.getText() + "/KR_DEV");
                                        File uncategorizedDir = new File(configLoader.resultFilePath + "/KR_DEV");
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

                                        // File dir = new File(inputResultFolderPath.getText());
                                        File dir = new File(configLoader.resultFilePath);
                                        String[] fileNames = dir.list();

                                        String fileName = file.getName();
                                        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                                        baseName = baseName.replaceAll("-page\\d+", "");

                                        // "미분류" 폴더 경로 생성
                                        // File uncategorizedDir = new File(inputResultFolderPath.getText() + "/KR_DEV");
                                        File uncategorizedDir = new File(configLoader.resultFilePath + "/KR_DEV");
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
                                if (fileInfo.getServiceType().contains("google")) {
                                    googleService.FullTextOCR(file);
                                } else if (fileInfo.getServiceType().contains("synap")) {
                                    synapService.synapOCR(file, subPath);
                                } else if (fileInfo.getServiceType().contains("da")) {
                                    if (FileExtensionUtil.DA_SUPPORTED_EXT.contains(ext)) {
                                        docuAnalyzerService.docuAnalyzerForExtendedFormats(file, subPath);
                                    } else {
                                        docuAnalyzerService.docuAnalyzer(file, subPath);
                                    }
                                } else {
                                    log.info("The file operation is skipped because there is no matching OCR Service Type.");
                                    continue;
                                }

                                try {
                                    if (googleService.checkBadImg == true) {
                                        ioService.copyFiles(file);
                                    } else {
                                        badFileImg.add(file);
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                // 국가 리스트 포함
                                if (fileInfo.getServiceType().contains("google")) {
                                    googleService.uploadAndOCR(file);
                                } else if (fileInfo.getServiceType().contains("synap")) {
                                    synapService.synapOCR(file, subPath);

                                    try {
                                        onButton2API(fileInfo);
                                    } catch (Exception e) {
                                        log.error("Error executing onButton2API", e);
                                    }
                                } else if (fileInfo.getServiceType().contains("da")) {
                                    if (FileExtensionUtil.DA_SUPPORTED_EXT.contains(ext)) {
                                        docuAnalyzerService.docuAnalyzerForExtendedFormats(file, subPath);
                                    } else {
                                        docuAnalyzerService.docuAnalyzer(file, subPath);
                                    }

                                    try {
                                        onButton2API(fileInfo);
                                    } catch (Exception e) {
                                        log.error("Error executing onButton2API", e);
                                    }
                                } else if (fileInfo.getServiceType().contains("ai.vision")) {
                                    if (FileExtensionUtil.AIVISION_SUPPORTED_EXT.contains(ext)) {
                                        if (FileExtensionUtil.AIVISION_OFFICE_SUPPORTED_EXT.contains(ext)
                                                && (fileInfo.getVisionStatus() == null || fileInfo.getVisionStatus().isEmpty())) {
                                            //TODO
                                            Path source = Paths.get(file.getPath());
                                            String fName = source.getFileName().toString(); // 파일명 추출
                                            int lastDot = fName.lastIndexOf(".");

                                            String bName = (lastDot > 0) ? fName.substring(0, lastDot) : fName;
                                            String extension = (lastDot > 0) ? fName.substring(lastDot) : "";

//                                            String guidName = bName + "_" + fileInfo.getGroupUID(); // 폴더명
                                            String guidName = bName;                                 // 폴더명
                                            String guidFileName = guidName + extension;              // 파일명

                                            Path targetDir = Paths.get(configLoader.imageFolderPath, guidName);
                                            Path target = targetDir.resolve(guidFileName);

                                            //Path resultPath = Paths.get(configLoader.getResultFilePath(), bName);

                                            try {
                                                Files.createDirectories(targetDir);
                                                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                                                log.trace(ext + " 원본 파일 VISION용 폴더 생성 및 파일명 변경 성공 (파일명+GroupUID)");

                                                HwpConverter hwpConverter = new HwpConverter();
                                                hwpConverter.convertToMarkdown(target.toString(), targetDir.toString(), fileInfo);
                                            } catch (Exception e) {
                                                log.warn(ext + " 원본 파일 VISION용 폴더 생성 처리 중 오류 발생: {}", e.getMessage());
                                            }
                                            fileInfo = ClassificationService.getFileByName(configLoader.apiUserId, subPath + item.apiLookupName, unitFileInfo.getGroupUID());
                                        }
                                        if (fileInfo.getVisionStatus().equalsIgnoreCase("VS")) {
                                            log.debug("visionStatus is VS");
                                            // TODO: 파일명_groupUID 폴더 전체 복사, 원본 파일명에서 groupUID 제거
                                            // TODO: page별 md 결과 기존 파일명 -> ~.dat로 변경
                                            copyAndRenameVisionResult(fileInfo);

                                            try {
                                                onButton2API(fileInfo);
                                            } catch (Exception e) {
                                                log.error("Error executing onButton2API", e);
                                            }
                                        } else {
                                            log.info("AI.VISION processing is not complete.");
                                            break;
                                        }
                                    }
                                } else {
                                    // OCR SERVICE TYPE이 GOOGLE, SYNAP에 해당되지 않는 경우
                                    log.info("The file operation is skipped because there is no matching OCR Service Type.");
                                    continue;
                                }
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        a++;

                        // 1page의 분류 결과가 없는 경우 해당 파일의 분류 종료
                        if (!fileInfo.getServiceType().equalsIgnoreCase("ai.vision") && configLoader.usePdfExtractImage) {
                            if (file.getName().matches(".*-page1\\.jpg$")) {
                                try {
                                    FileInfo firstPagefileInfo = null;
                                    firstPagefileInfo = ClassificationService.getFileByNameAndStatus(configLoader.apiUserId, subPath + file.getName(), "CS");

                                    if (firstPagefileInfo.getFilename() == null || firstPagefileInfo.getFilename().isEmpty()) {
                                        firstPagefileInfo = ClassificationService.getFileByNameAndStatus(configLoader.apiUserId, subPath + file.getName(), "CF");
                                    }
                                    if (firstPagefileInfo.getClassificationStatus() == null || firstPagefileInfo.getClassificationStatus().isEmpty()) {
                                        log.info("Skipping remaining pages for {} due to null classificationStatus", subPath + file.getName());

                                        File targetDir = new File(configResultFilePath);
                                        String originalFileName = file.getName();
                                        String baseName = originalFileName.replace("-page1.jpg", "");

                                        File[] filesToDelete = targetDir.listFiles(f -> f.isFile() && f.getName().contains(baseName));

                                        if (filesToDelete != null) {
                                            for (File f : filesToDelete) {
                                                boolean deleted = f.delete();
                                                if (deleted) {
                                                    log.info("Deleted file: {}", f.getAbsolutePath());
                                                } else {
                                                    log.warn("Failed to delete file: {}", f.getAbsolutePath());
                                                }
                                            }
                                        }
                                        break; // 현재 pendingFiles 루프의 다음 fileInfo로 넘어감
                                    }
                                } catch (UnirestException e) {
                                    log.warn("api 조회 error");
                                }
                            }
                        }
                    }
                }

//                try {
//                    onButton2API();
//                } catch (Exception e) {
//                    log.error("Error executing onButton2API", e);
//                }
            } else {
                log.info("Skipping because there are no files to process in the folder");
                state.setProcessing(false);
                log.info("setProcessing : {}", state.isProcessing());
                return;
            }

//            for (File file : badFileImg) {
//                try {
//                    ioService.ImgToPDF(file);
//                    ioService.copyFiles(file);
//                    googleService.uploadAndOCR(ioService.badImgToPDF);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
        } finally {
            if (state.isProcessing()) {
                state.setProcessing(false); // 작업이 끝난 후에만 false로 설정
                //log.info("setProcessing : {}", state.isProcessing());
            }
        }
    }

    public void onButton2API(FileInfo fileInfo) throws Exception {
        // API 사용 분류 진행 (국가 코드 별로 해당하는 파일을 찾아 분류 진행)
        try {
            List<String> countryNames = List.of("FR", "US", "IT", "VN", "JP", "CN", "KR");

            int n = 0;

            //for (String countryName : countryNames) {

            File resultDir;
            if (fileInfo.getServiceType().equalsIgnoreCase("ai.vision")) {
                String onlyFileName = Paths.get(fileInfo.getFilename()).getFileName().toString();
                int idx = onlyFileName.lastIndexOf('.');
                Path resultDirPath = Paths.get(configLoader.resultFilePath, onlyFileName.substring(0, idx) + "_" + fileInfo.getGroupUID());
                resultDir = resultDirPath.toFile();
            } else {
                resultDir = new File(configLoader.resultFilePath);
            }

            log.info("resultDir: " + resultDir.toString());
            File[] datFiles = resultDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".dat"));

            if (datFiles == null || datFiles.length == 0) {
                log.warn("분류할 .dat 파일이 없습니다.");
                return;
            }

            String countryName = fileInfo.getLanguage();

//                n++;
//                log.info("({}) unitFileInfo NAME : {}, LANGUAGE : {}", n, unitFileInfo.getFilename(), unitFileInfo.getLanguage());
//                System.out.println("Current country name : " + countryName);
            String jsonFilePath = configLoader.ruleFilePath;

            // 확장자 앞에 countryName 추가
            int dot = jsonFilePath.lastIndexOf('.');
            String ruleFileName = (dot == -1)
                    ? jsonFilePath + "_" + countryName
                    : jsonFilePath.substring(0, dot) + "_" + countryName + jsonFilePath.substring(dot);

            String folderPath = configLoader.ruleFolderPath;  // 새로 추가한 config 값

            String ruleFilePath;
            if (folderPath != null && !folderPath.trim().isEmpty()) {
                ruleFilePath = folderPath + File.separator + ruleFileName;
            } else {
                ruleFilePath = ruleFileName;   // 기존 방식 → 작업디렉토리 기준
            }

            log.info("({}) unitFileInfo NAME : {}, LANGUAGE : {}", n, unitFileInfo.getFilename(), unitFileInfo.getLanguage());
            log.info("RULE FILE PATH : " + ruleFilePath);

            // 현재 국가 코드에 해당하는 파일 정보들을 조회
//                        List<FileInfo> filesForCountry = uploadFileList.stream()
//                                .filter(info -> countryName.equals(info.getLanguage()))
//                                .collect(Collectors.toList());
//
//                        if (filesForCountry.isEmpty()) {
//                            log.info("국가 코드 {}에 해당하는 파일이 없어 분류를 생략합니다.", countryName);
//                            continue;
//                        }

//                if (!countryName.equals(fileInfo.getLanguage())) {
//                    log.info("국가 코드 {}에 해당하지 않아 분류를 생략합니다.", countryName);
//                    continue;
//                }

            // 파일 단위 처리로 변경하면서 순서 위로 이동
//            File resultDir = new File(configLoader.resultFilePath);
//            File[] datFiles = resultDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".dat"));
//
//            if (datFiles == null || datFiles.length == 0) {
//                //log.warn("분류할 .dat 파일이 없습니다.");
//                return;
//            }

            byte[] jsonToByte;

            try {
                // RULE 파일 디코딩
                jsonToByte = Files.readAllBytes(Paths.get(ruleFilePath));
                documentService.jsonData = JsonService.getJsonDictionary2(JsonService.aesDecode(jsonToByte));
            } catch (Exception e) {
                log.error("Rule 파일 처리 실패: {}", ruleFilePath);
                return;
            }

            // API 전체 리스트로 돌 때 (uploadFileList 사용 시)
//                        List<File> matchedFiles = Arrays.stream(datFiles)
//                                .filter(datFile -> {
//                                    String datName = datFile.getName();
//                                    String datBaseName = datName.replaceAll("(_result)?\\.dat$", ""); // "_result.dat" 또는 ".dat" 제거
//
//                                    return filesForCountry.stream()
//                                            .anyMatch(info -> {
//                                                String imageBaseName = info.getFilename().replaceAll("\\.[^.]+$", ""); // 확장자 제거
//                                                return datBaseName.contains(imageBaseName);
//                                            });
//                                })
//                                .collect(Collectors.toList());

            String normalizedFileName = fileInfo.getFilename().replace("/", File.separator).replace("\\", File.separator);
            String justFileName = new File(normalizedFileName).getName(); // "샘플.pdf"
            String imageBaseName = justFileName.replaceAll("\\.[^.]+$", ""); // "샘플"
            //String imageBaseName = fileInfo.getFilename().replaceAll("\\.[^.]+$", "");

            // 파일 단위로 돌 때 (unitFileInfo 사용 시)
            List<File> matchedFiles;
            boolean officeExtensionFlag = false;

            String ext = FileExtensionUtil.getExtension(justFileName);
            if (fileInfo.getServiceType().equalsIgnoreCase("da") && FileExtensionUtil.DA_SUPPORTED_EXT.contains(ext)) {
                matchedFiles = Arrays.stream(datFiles)
                        .filter(datFile -> {
                            String datBaseName = datFile.getName().replaceAll("_result\\.dat$", "");
                            return datBaseName.equals(imageBaseName + "-page1");
                        })
                        .collect(Collectors.toList());
                officeExtensionFlag = true;
            } else if (fileInfo.getServiceType().equalsIgnoreCase("ai.vision") && FileExtensionUtil.AIVISION_SUPPORTED_EXT.contains(ext)) {
                // TODO 250226 기준: 임시로 AI.VISION 사용하는 경우 page1의 분류만 진행하도록 작업해 둠 **추후 수정 필요 (각 페이지 별 분류가 필요한지 확인 필요)
                matchedFiles = Arrays.stream(datFiles)
                        .filter(datFile -> {
                            String datBaseName = datFile.getName().replaceAll("_result\\.dat$", "");
                            return datBaseName.equals(imageBaseName + "-page1");
                        })
                        .collect(Collectors.toList());
                officeExtensionFlag = true;
            } else {
                matchedFiles = Arrays.stream(datFiles)
                        .filter(datFile -> {
                            String datBaseName = datFile.getName().replaceAll("_result\\.dat$", "");
                            return datBaseName.equals(imageBaseName);
                        })
                        .collect(Collectors.toList());
            }
            log.trace("office extension flag: {}", officeExtensionFlag);

            if (matchedFiles.isEmpty()) {
                log.warn("해당 페이지에 대한 .dat 파일이 없어 분류를 건너뜁니다: {}", imageBaseName);
                return;
            }

            // 정렬 로직 추가
//                Comparator<File> naturalOrderComparator = (f1, f2) -> compareNaturally(f1.getName(), f2.getName());
//                matchedFiles.sort(naturalOrderComparator);

            // 정렬된 파일을 배열로 변환
            documentService.jsonFiles = matchedFiles.toArray(new File[0]);

            // 디버깅 출력
            for (File file : matchedFiles) {
                log.debug("JSON FILE LIST : " + file.getName());
            }
            log.debug("MATCHED FILES SIZE : " + matchedFiles.size());

//                        documentService.jsonFiles = matchedFiles.toArray(new File[0]);
//
//                        for (File file : matchedFiles) {
//                            System.out.println("JSON FILE LIST : " + file.getName());
//                            System.out.println("MATCHED FILES SIZE : " + matchedFiles.size());
//                        }

            try {
                if (fileInfo.getServiceType().contains("da")) {
                    classificationDocumentWithDa(officeExtensionFlag);
                } else if (fileInfo.getServiceType().contains("ai.vision")) {
                    classificationDocumentWithVision(officeExtensionFlag, fileInfo);
                } else {
                    classificationDocument();
                }
            } catch (Exception e) {
                log.error("분류 실패: {}", e.getMessage());
            }
        //}

        //log.info("API-based classification completed");

        configLoader.resultFilePath = resultFilePath;
        excelService.configLoader = configLoader;
        //excelService.deleteFileList();
    } finally {
        //excelService.deleteFileList();
//                    state.setProcessing(false);
//                    log.info("setProcessing : {}", state.isProcessing());
        }

    }

    // 파일명 (페이지 넘버 포함) 정렬
    public static int compareNaturally(String s1, String s2) {
        Pattern pattern = Pattern.compile("(\\D*)(\\d*)");
        Matcher m1 = pattern.matcher(s1);
        Matcher m2 = pattern.matcher(s2);

        while (m1.find() && m2.find()) {
            int nonDigitCompare = m1.group(1).compareTo(m2.group(1));
            if (nonDigitCompare != 0) {
                return nonDigitCompare;
            }

            String num1 = m1.group(2);
            String num2 = m2.group(2);

            if (num1.isEmpty() && num2.isEmpty()) {
                continue;
            }

            int n1 = num1.isEmpty() ? 0 : Integer.parseInt(num1);
            int n2 = num2.isEmpty() ? 0 : Integer.parseInt(num2);

            int numberCompare = Integer.compare(n1, n2);
            if (numberCompare != 0) {
                return numberCompare;
            }
        }

        return s1.compareTo(s2);
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
        if (!configLoader.apiUsageFlag) {
            processing();
        } else {
            configLoader.resultFilePath = resultFilePath;
        }
        excelService.configLoader = configLoader;
        documentService.configLoader = configLoader;

        // 전달 받은 폴더 경로의 json 파일 필터링
        if (!configLoader.apiUsageFlag) {
            documentService.jsonFiles = excelService.getFilteredJsonFiles();
        }

//        for (File file : documentService.jsonFiles) {
//            System.out.println("---------- documentService jsonFiles : " + file.getPath());
//        }
        documentService.createFinalResultFile();
    }

    public void classificationDocumentWithDa(boolean officeExtensionFlag) throws Exception {
        configLoader.resultFilePath = resultFilePath;
        excelService.configLoader = configLoader;
        documentService.configLoader = configLoader;

        documentService.createFinalResultFileWithDa(officeExtensionFlag);
    }

    public void classificationDocumentWithVision(boolean officeExtensionFlag, FileInfo fileInfo) throws Exception {
        configLoader.resultFilePath = resultFilePath;
        excelService.configLoader = configLoader;
        documentService.configLoader = configLoader;

        documentService.createFinalResultFileWithVision(officeExtensionFlag, fileInfo);
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

    public void copyAndRenameVisionResult(FileInfo fileInfo) throws Exception {
        String originalFileName = buildNameForAiVision(Paths.get(fileInfo.getFilename()).getFileName().toString(), fileInfo.getGroupUID()); // 파일명_groupUID.pdf
        int idx = originalFileName.lastIndexOf('.');
        String folderName = (idx > 0) ? originalFileName.substring(0, idx) : originalFileName; // 파일명_groupUID
        String extension = (idx > 0) ? originalFileName.substring(idx) : "";

        Path source = Paths.get(configLoader.imageFolderPath, folderName);
        Path target = Paths.get(configLoader.resultFilePath, folderName);
        log.info("source path: {}, target path: {}", source, target);

        // 소스 폴더 확인
        if (!Files.exists(source)) {
            log.warn("Source folder not found: " + source);
            throw new NoSuchFileException("Source folder not found: " + source);
        }

        Files.createDirectories(target.getParent());

        // 타깃 폴더가 이미 있으면 완전히 삭제 (병합 방지)
        if (Files.exists(target)) {
            FileUtils.deleteDirectory(target.toFile());
        }

        // 원본 폴더 전체 복사 (원본은 그대로 유지)
        FileUtils.copyDirectory(source.toFile(), target.toFile());

        // 복사 한 폴더 내 파일 이름 변경 (원본 파일 및 MD 결과 파일)
        renameVisionFiles(fileInfo, target);
    }

    // 원본 파일, MD 파일 이름 변경 (파일명 내 groupUID 제거, .md -> _result.dat 변경)
    private void renameVisionFiles(FileInfo fileInfo, Path targetDir) throws IOException {
        String original = Paths.get(fileInfo.getFilename()).getFileName().toString();              // 예: 파일명.pdf
        String base = original.substring(0, original.lastIndexOf("."));  // 파일명
        String groupUid = fileInfo.getGroupUID();
        log.debug("[renameVisionFiles] 시작 - targetDir='{}', original='{}', base='{}', groupUid='{}'", targetDir, original, base, groupUid);

        // 원본 파일명의 "_gorupUID" 제거
        String originalWithUid = buildNameForAiVision(original, groupUid); // 예: "파일명_ABC123.pdf"
        Path sourcePath = targetDir.resolve(originalWithUid);
        Path targetPath = targetDir.resolve(original);
        if (Files.exists(sourcePath)) {
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("[renameVisionFiles] 원본 파일명 리네임: '{}' -> '{}'", sourcePath.getFileName(), targetPath.getFileName());
        } else {
            log.debug("[renameVisionFiles] 원본 대상 파일이 없음(스킵): '{}'", sourcePath.getFileName());
        }

        String subPdfWithUid = base + "_" + groupUid + ".pdf";
        Path subPdfPath = targetDir.resolve(subPdfWithUid);
        Path renamePdfPath = targetDir.resolve(base + ".pdf");

        if (Files.exists(subPdfPath) && !subPdfPath.equals(renamePdfPath)) {
            Files.move(subPdfPath, renamePdfPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("[renameVisionFiles] 서브 PDF 리네임 완료: '{}' -> '{}'", subPdfWithUid, renamePdfPath.getFileName());
        }

        // 페이지 MD: "파일명_groupUID-page<숫자>.md" -> "파일명-page<숫자>_result.dat"
        //String mdGlob = base + "_" + groupUid + "*.md";    // 대상 패턴: "base_groupUID.md", "base_groupUID-page{N}.md"
        Pattern pagePattern = Pattern.compile(
                Pattern.quote(base) + "_" + Pattern.quote(groupUid) + "-page(\\d+)\\.md"
        );
        // 추가: 이미 _result.md까지 붙은 케이스
        Pattern pageResultPattern = Pattern.compile(
                Pattern.quote(base) + "_" + Pattern.quote(groupUid) + "-page(\\d+)_result\\.md"
        );

        String singleMdName = base + "_" + groupUid + ".md";

        //log.debug("MD 처리: glob='{}', single='{}', pattern='{}', resultPattern='{}'", mdGlob, singleMdName, pagePattern.pattern(), pageResultPattern.pattern());

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDir,
                path -> {
                    String name = path.getFileName().toString();
                    // md: "base_groupUID.md" 또는 "base_groupUID-pageN.md" 등을 포함
                    return name.startsWith(base + "_" + groupUid) && name.endsWith(".md");
                })) {
            for (Path mdPath : stream) {
                String name = mdPath.getFileName().toString();

                // 리네임될 새 .md 파일명 결정
                String newMdName;
                Matcher m = pagePattern.matcher(name);
                Matcher mr = pageResultPattern.matcher(name);
                if (m.matches()) {
                    // "base_groupUID-pageN.md" -> "base-pageN_result.md"
                    String page = m.group(1);
                    newMdName = base + "-page" + page + "_result.md";
                } else if (mr.matches()) {
                    // "base_groupUID-pageN_result.md" -> "base-pageN_result.md"
                    String page = mr.group(1);
                    newMdName = base + "-page" + page + "_result.md";
                } else if (name.equals(singleMdName)) {
                    // "base_groupUID.md" -> "base_result.md"
                    newMdName = base + "_result.md";
                } else {
                    // 혹시 모를 유사 파일은 스킵
                    log.debug("[renameVisionFiles][MD] 스킵: '{}'", name);
                    continue;
                }

                // md 파일명 변경
                Path renamedMdPath = mdPath.resolveSibling(newMdName);
                try {
                    Files.move(mdPath, renamedMdPath, StandardCopyOption.REPLACE_EXISTING);
                    log.debug("[renameVisionFiles][MD] 리네임: '{}' -> '{}'", name, newMdName);
                } catch (Exception e) {
                    log.warn("[renameVisionFiles][MD] 리네임 실패: '{}' -> '{}', 원인: {}", name, newMdName, e.toString());
                    continue;
                }

                // 결과 .md 를 동일 파일명으로 확장자만 .dat로 '복사'
                String datName = replaceExtension(newMdName, "dat"); // ".md" -> ".dat"
                Path datPath = renamedMdPath.resolveSibling(datName);
                try {
                    Files.copy(renamedMdPath, datPath, StandardCopyOption.REPLACE_EXISTING);
                    log.debug("[renameVisionFiles][MD] DAT 생성: '{}' (from '{}')", datName, newMdName);
                } catch (Exception e) {
                    log.warn("[renameVisionFiles][MD] DAT 생성 실패: '{}' (from '{}'), 원인: {}", datName, newMdName, e.toString());
                }
            }
        }

        //String jsonGlob = base + "_" + groupUid + "*.json"; // "base_groupUID.json", "base_groupUID-page{N}.json" 등을 포함
        Pattern jsonPagePattern = Pattern.compile(
                Pattern.quote(base) + "_" + Pattern.quote(groupUid) + "-page(\\d+)\\.json"
        );
        String singleJsonName = base + "_" + groupUid + ".json";

        //log.debug("JSON 처리: glob='{}', single='{}', pagePattern='{}'", jsonGlob, singleJsonName, jsonPagePattern.pattern());

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDir,
            path -> {
                    String name = path.getFileName().toString();
                    return name.startsWith(base + "_" + groupUid) && name.endsWith(".json");
                })) {
            for (Path jsonPath : stream) {
                String name = jsonPath.getFileName().toString();

                String newJsonName;
                Matcher jm = jsonPagePattern.matcher(name);
                if (jm.matches()) {
                    // "base_groupUID-pageN.json" -> "base-pageN.json"
                    String page = jm.group(1);
                    newJsonName = base + "-page" + page + "_result.json";
                } else if (name.equals(singleJsonName)) {
                    // (옵션) "base_groupUID.json" -> "base.json"
                    newJsonName = base + ".json";
                } else {
                    // 대상 외 파일 스킵
                    log.debug("[renameVisionFiles][JSON] 스킵: '{}'", name);
                    continue;
                }

                Path renamedJsonPath = jsonPath.resolveSibling(newJsonName);
                try {
                    Files.move(jsonPath, renamedJsonPath, StandardCopyOption.REPLACE_EXISTING);
                    log.debug("[renameVisionFiles][JSON] 리네임: '{}' -> '{}'", name, newJsonName);
                } catch (Exception e) {
                    log.warn("[renameVisionFiles][JSON] 리네임 실패: '{}' -> '{}', 원인: {}", name, newJsonName, e.toString());
                }
            }
        }
    }

    // AI.VISION의 결과 파일 이름 생성 (파일명_groupUID.확장자)
    public String buildNameForAiVision(String filename, String groupUid) {
        int idx = filename.lastIndexOf('.');
        return (idx > 0)
                ? filename.substring(0, idx) + "_" + groupUid + filename.substring(idx)
                : filename + "_" + groupUid;
    }

    // 파일명의 확장자 변경
    private static String replaceExtension(String filename, String newExt) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0) return filename + "." + newExt;
        return filename.substring(0, idx + 1) + newExt;
    }

    private int extractPageNum(String filename) {
        Pattern pattern = Pattern.compile("-page(\\d+)");
        Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }
}