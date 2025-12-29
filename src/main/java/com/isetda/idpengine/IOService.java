package com.isetda.idpengine;

import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.jbig2.JBIG2ImageReaderSpi;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import org.apache.pdfbox.Loader;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IOService {
    private static final Logger log = LogManager.getLogger(IOService.class);
    public static ConfigLoader configLoader = ConfigLoader.getInstance();
    public List<File> allFilesInSourceFolder;
    //추가 11/07
    public File badImgToPDF;
    public String imagePath;
    public String fullResultPath;
    public String subPath;

    // JBIG2 이미지 처리를 위한 초기화
    static {
        try {
            IIORegistry registry = IIORegistry.getDefaultInstance();
            registry.registerServiceProvider(new JBIG2ImageReaderSpi());
        } catch (Exception e) {
            LogManager.getLogger(IOService.class).error("Error registering JBIG2 image reader", e);
        }
    }

    public File[] getFilteredFiles() {
        log.info("Start filtering files {}", configLoader.imageFolderPath);
        File folder = new File(configLoader.imageFolderPath);
        List<File> filteredFiles = new ArrayList<>();

        // 폴더가 존재하고 디렉토리인 경우만 처리
        if (folder.exists() && folder.isDirectory()) {
            allFilesInSourceFolder = new ArrayList<>();
            findFilesRecursively(folder, allFilesInSourceFolder, filteredFiles);
            log.debug("A total of {} files found in {} (including subdirectories)", allFilesInSourceFolder.size(), configLoader.imageFolderPath);
        } else {
            allFilesInSourceFolder = new ArrayList<>();
            log.error("Source folder does not exist or is not a directory: {}", configLoader.imageFolderPath);
            return new File[0];
        }

        if (filteredFiles.isEmpty()) {
            log.debug("No filtered files found in {}", configLoader.imageFolderPath);
        } else {
            log.info("Total filtered files: {}", filteredFiles.size());
        }

        // 리스트를 배열로 변환하여 반환
        return filteredFiles.toArray(new File[0]);
    }

    private void findFilesRecursively(File folder, List<File> allFiles, List<File> filteredFiles) {
        File[] filesAndDirs = folder.listFiles();
        if (filesAndDirs != null) {
            for (File file : filesAndDirs) {
                if (file.isDirectory()) {
                    log.debug("Browse folders inside folders: {}", file.getAbsolutePath());
                    // 서브 폴더를 재귀적으로 탐색
                    findFilesRecursively(file, allFiles, filteredFiles);
                } else {
                    String lowercaseName = file.getName().toLowerCase();

                    // 이미지 파일이거나 PDF 파일일 경우 allFilesInSourceFolder에 추가
                    if (lowercaseName.endsWith(".jpg") || lowercaseName.endsWith(".png") || lowercaseName.endsWith(".jpeg") || lowercaseName.endsWith(".pdf")) {
                        allFiles.add(file); // 소스 폴더 내 모든 이미지 파일 및 PDF 파일을 저장
                    }

                    // 이미지 파일이면 filteredFiles에도 추가
                    if (lowercaseName.endsWith(".jpg") || lowercaseName.endsWith(".png") || lowercaseName.endsWith(".jpeg")) {
                        filteredFiles.add(file); // 이미지 파일을 filteredFiles에 추가
                    } else if (lowercaseName.endsWith(".pdf")) {
                        log.debug("PDF file found: {}", file.getAbsolutePath());
                        filteredFiles.add(file); // PDF 원본 파일을 filteredFiles에 추가
                        try {
                            // PDF에서 추출된 이미지를 filteredFiles에 추가
                            List<File> extractedImages = extractImagesFromPDF(file.getAbsolutePath());
                            filteredFiles.addAll(extractedImages);
                        } catch (IOException e) {
                            log.error("Error extracting image from PDF file: {}", file.getAbsolutePath(), e);
                        }
                    }
                }
            }
        } else {
            log.error("Error getting file list from folder: {}", folder.getAbsolutePath());
        }
    }

    public List<File> extractImagesFromPDF(String pdfPath) throws IOException {
        List<File> extractedImages = new ArrayList<>();
        final int MAX_WIDTH = 2000;
        final int MAX_HEIGHT = 2000;

        File pdfFile = Paths.get(pdfPath).toFile();

        // PDDocument.load() 대신 Loader 클래스 사용
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            int totalPages = document.getNumberOfPages();

            for (int page = 0; page < totalPages; ++page) {
                PDPage pdPage = document.getPage(page);
                float widthPt = pdPage.getMediaBox().getWidth();
                float heightPt = pdPage.getMediaBox().getHeight();

                // DPI 계산 (최대 크기에 맞추기)
                int dpi = (int) Math.min(MAX_WIDTH / (widthPt / 72), MAX_HEIGHT / (heightPt / 72));

                BufferedImage bim = pdfRenderer.renderImageWithDPI(page, dpi, ImageType.RGB);


                // 이미지가 여전히 너무 크다면 스케일링
                if (bim.getWidth() > MAX_WIDTH || bim.getHeight() > MAX_HEIGHT) {
                    double scale = Math.min((double) MAX_WIDTH / bim.getWidth(), (double) MAX_HEIGHT / bim.getHeight());
                    int scaledWidth = (int) (bim.getWidth() * scale);
                    int scaledHeight = (int) (bim.getHeight() * scale);
                    BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
                    scaledImage.getGraphics().drawImage(bim.getScaledInstance(scaledWidth, scaledHeight, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
                    bim = scaledImage;
                }

                String fileName = pdfPath.replace(".pdf", "") + "-page" + (page + 1) + ".jpg";
                File imageFile = new File(configLoader.resultFilePath, new File(fileName).getName());
                ImageIO.write(bim, "jpg", imageFile);
                extractedImages.add(imageFile);

                log.debug("Image extracted from PDF (page {}): {}", page + 1, imageFile.getAbsolutePath());
            }
            log.info("A total of {} images were extracted from PDF file: {}", totalPages, pdfPath);
        } catch (IOException e) {
            // PDF 파일 오류 시
            log.error("Error extracting image from PDF file: {}", pdfPath, e);

            moveFileToErrorDirectory(pdfFile, subPath);

            APICaller apiCaller = new APICaller();
            String apiFileName = Paths.get(subPath, pdfFile.getName()).toString();
            try {
                FileInfo fileInfo = apiCaller.getFileByName(configLoader.apiUserId, apiFileName);
                String message = "File Error";
                apiCaller.callDeleteApi(configLoader.apiUserId, fileInfo.getFilename(), fileInfo.getOcrServiceType());
                if (fileInfo.getUrlData() != null) {
                    String errorDir = Paths.get(configLoader.resultFilePath, "오류", subPath).toString();
                    apiCaller.callbackApi(fileInfo, errorDir, 666, message);
                } else {
                    log.info("URL DATA IS NULL");
                }
            } catch (UnirestException ex) {
                log.info("DELETE API/CALLBACK API 호출 실패: {}", ex);
            }

            throw e;
        }

        return extractedImages;
    }

    // 파일 "오류" 폴더로 이동
    public static void moveFileToErrorDirectory(File file, String subPath) {
        String path = Paths.get(configLoader.resultFilePath, "오류", subPath).toString();
        File errorDir = new File(path);

        if (!errorDir.exists()) {
            boolean created = errorDir.mkdirs();
            if (!created) {
                log.error("Failed to create error directory: {}", errorDir.getPath());
                return;
            }
        }

        File destFile = Paths.get(errorDir.getPath(), file.getName()).toFile();
        try {
            Files.move(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("Moved problematic PDF to: {}", destFile.getPath());
        } catch (IOException moveEx) {
            log.error("Failed to move PDF to error directory: {}", destFile.getPath(), moveEx);
        }
    }

    // 파일 삭제 메서드
    public void deleteFilesInFolder() {
        log.info("폴더의 모든 파일 삭제 시작: {}", configLoader.imageFolderPath);
        File folder = new File(configLoader.imageFolderPath);

        // 폴더가 존재하지 않거나 디렉토리가 아닌 경우
        if (!folder.exists() || !folder.isDirectory()) {
            log.error("폴더가 존재하지 않거나 디렉토리가 아닙니다: {}", configLoader.imageFolderPath);
            return;
        }

        // 폴더 안의 파일들을 삭제
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        // 파일 이름과 확장자만 로그에 출력
                        String fileName = file.getName();
                        log.trace("파일 삭제 성공: 이름: {}", fileName);
                    } else {
                        String fileName = file.getName();
                        log.warn("파일 삭제 실패: 이름: {}", fileName);
                    }
                }
            }
        } else {
            log.error("폴더에서 파일을 가져오는 중 오류가 발생했습니다: {}", configLoader.imageFolderPath);
        }
    }


    // 파일 복사
    public void copyFiles(File file) throws IOException {

            Path sourcePath = file.toPath();
            Path destinationPath = Paths.get(configLoader.resultFilePath, file.getName());

            // "PDF-"가 파일 이름에 포함된 경우 복사하지 않음
//            if (file.getName().contains("-page")) {
//                log.info("Do not copy images extracted from PDFs: {}", file.getName());
//                return;
//            }

            // 동일한 이름의 파일이 이미 존재하는지 확인
            if (Files.exists(destinationPath)) {
                log.info("Image already exists: {}", destinationPath);
                return;
            }
            try {
                Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                // 파일 이름과 확장자만 로그에 출력
                String fileName = file.getName();
                log.debug("Copy successful: Name:{}->Storage path:{}", fileName, destinationPath);
            } catch (IOException e) {
                String fileName = file.getName();
                log.error("Error copying: Name: {} -> Storage path: {}, Error: {}", fileName, destinationPath, e.getMessage(), e);
                throw e; // 오류 발생 시 던지기
            }
    }

    public void ImgToPDF(File imageFile) throws IOException {
        if (imageFile.getName().toLowerCase().matches(".*\\.(jpg|jpeg|png)$")) {
            try (PDDocument doc = new PDDocument()) {
                BufferedImage bufferedImage = ImageIO.read(imageFile);
                PDPage page = new PDPage(new PDRectangle(bufferedImage.getWidth(), bufferedImage.getHeight()));
                doc.addPage(page);
                PDImageXObject img = PDImageXObject.createFromFile(imageFile.getAbsolutePath(), doc);

                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.drawImage(img, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
                }

                String outputFileName = imageFile.getName().replaceAll("(?i)\\.(jpg|jpeg|png)$", ".pdf");
                String outputPath = configLoader.resultFilePath + File.separator + outputFileName;
                doc.save(outputPath);

                badImgToPDF = extractImagesFromPDF(outputPath).getFirst();
                log.debug("Converted : {}",imageFile.getName());
            } catch (IOException e) {
                log.warn("Error converting: {}",imageFile.getName());
            }
        } else {
            log.error("{} is not a valid image file.",imageFile.getName());
        }
    }

    // 처리 단위가 api 리스트 전체일 때
    public List<File> getFilesWithAPIAndExtractedImages() {
        APICaller apiCaller = new APICaller();
        List<File> resultFiles = new ArrayList<>();

        //File folder = new File(configLoader.imageFolderPath);

        LocalDate today = LocalDate.now();
        String formattedDate = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        try {
            // 처리 단위가 api 리스트 전체일 때
            List<FileInfo> fileInfos = apiCaller.getAllFilesWithCase(configLoader.apiUserId, configLoader.ocrServiceType, "", 0, formattedDate);
            // 시간순 정렬
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
            fileInfos.sort(
                    Comparator.comparing(file -> {
                        try {
                            return LocalDateTime.parse(file.getCreateDateTime(), formatter);
                        } catch (Exception e) {
                            return LocalDateTime.MIN; // 오류 시 가장 오래된 값으로 처리
                        }
                    })
            );

            for (FileInfo fileInfo : fileInfos) {
                if (fileInfo.getJobType() == null || !fileInfo.getJobType().contains("IDP")) {
                    continue;
                }

                String fileName = fileInfo.getFilename();
                File file = new File(configLoader.imageFolderPath, fileName);

                if (!file.exists()) {
                    log.debug("file not exist: {}", file.getAbsolutePath());
                    continue;
                }

                String lowerName = fileName.toLowerCase();

                if (lowerName.endsWith(".pdf")) {
                    List<File> extractedImages = extractImagesFromPDF(file.getAbsolutePath());
                    resultFiles.addAll(extractedImages);

                    copyFiles(file);
                    int maxPage = getPdfPageCount(file.getAbsolutePath());
                    apiCaller.callDivisionApi(configLoader.apiUserId, maxPage, fileName, fileInfo.getOcrServiceType());

                    log.trace("PDF에서 추출된 이미지 {}개 추가됨: {}", extractedImages.size(), fileName);
                } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png")) {
                    resultFiles.add(file);
                } else {
                    log.warn("지원하지 않는 파일 형식: {}", fileName);
                }
            }
        } catch (Exception e) {
            log.error("API 조회 실패");
        }

//        if (allFiles != null) {
//            for (File file : allFiles) {
//                if (file.isFile()) {
//                    try {
//                        FileInfo fileInfo = apiCaller.getFileWithStatus2(configLoader.apiUserId, file.getName());
//
//                        if (fileInfo != null && fileInfo.getFilename() != null) {
//                            String lowerName = file.getName().toLowerCase();
//
//                            if (lowerName.endsWith(".pdf")) {
//                                // PDF에서 이미지 추출
//                                List<File> extractedImages = extractImagesFromPDF(file.getAbsolutePath());
//                                resultFiles.addAll(extractedImages);
//
//                                // PDF 처리
//                                copyFiles(file);
//                                int maxPage = idpEngineController.getPdfPageCount(configLoader.imageFolderPath + File.separator + fileInfo.getFilename());
//                                apiCaller.callDivisionApi(configLoader.apiUserId, maxPage, fileInfo.getFilename(), fileInfo.getOcrServiceType());
//
//                                log.info("PDF에서 추출된 이미지 {}개 추가됨: {}", extractedImages.size(), file.getName());
//                            } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png")) {
//                                resultFiles.add(file); // 이미지 파일은 그대로 추가
//                            } else {
//                                log.warn("지원하지 않는 파일 형식: {}", file.getName());
//                            }
//                        }
//                    } catch (Exception e) {
//                        log.error("API 조회 실패: {}", file.getName(), e);
//                    }
//                }
//            }
//        } else {
//            log.error("폴더를 읽을 수 없습니다: {}", configLoader.imageFolderPath);
//        }

        resultFiles.sort((f1, f2) -> {
            int pageNum1 = extractPageNumber(f1.getName());
            int pageNum2 = extractPageNumber(f2.getName());
            return Integer.compare(pageNum1, pageNum2);
        });

        return resultFiles;
    }

    // 처리 단위가 1개 파일일 때
    public List<File> getFileWithAPIAndExtractedImages(FileInfo unitFileInfo) {
        APICaller apiCaller = new APICaller();
        IOService ioService = new IOService();
        List<File> resultFiles = new ArrayList<>();

        //File folder = new File(configLoader.imageFolderPath);

        LocalDate today = LocalDate.now();
        String formattedDate = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        try {
            String fileName = unitFileInfo.getFilename();
            String normalizedFileName = fileName.replace("/", File.separator).replace("\\", File.separator);
            File file = null;

            for (FolderMapping mapping : configLoader.folderMappings) {
                //File targetFile = new File(mapping.getImageFolderPath(), fileName);
                File targetFile = Paths.get(mapping.getImageFolderPath(), normalizedFileName).toFile();
                log.trace("파일 탐색 시도 경로: {}", targetFile.getAbsolutePath());

                if (targetFile.exists()) {
                    file = targetFile;

                    // 세부 경로 추출
                    String subDirPath = "";
                    int lastSeparatorIndex = normalizedFileName.lastIndexOf(File.separator);
                    if (lastSeparatorIndex != -1) {
                        String rawSubPath = fileName.substring(0, lastSeparatorIndex);
                        subDirPath = Paths.get(rawSubPath).toString() + File.separator;
                    }
//                    int lastSeparatorIndex = normalizedFileName.lastIndexOf(File.separator);
//                    String subDirPath = (lastSeparatorIndex != -1) ? fileName.substring(0, lastSeparatorIndex) + File.separator : "";

                    configLoader.imageFolderPath = mapping.getImageFolderPath() + File.separator + subDirPath;
                    configLoader.resultFilePath = mapping.getResultFilePath();
                    subPath = subDirPath;
                    fullResultPath = mapping.getResultFilePath();
                    log.debug("결과 저장 경로 (String): {}", fullResultPath);
                    log.debug("Sub Path: {}", subPath);

                    File resultDir = Paths.get(fullResultPath).toFile();
                    if (!resultDir.exists()) {
                        resultDir.mkdirs();
                    }

                    log.debug("원본 파일명: {}, 정규화 파일명: {}", fileName, normalizedFileName);
                    log.debug("탐색 대상 매핑 경로: {}", mapping.getImageFolderPath());


                    break;
                }
            }
            //File file = new File(configLoader.imageFolderPath, fileName);

            if (!file.exists()) {
                //log.info("file not exist: {}", file.getAbsolutePath());
                return Collections.emptyList();
            }

            String lowerName = fileName.toLowerCase();
            String ext = FileExtensionUtil.getExtension(fileName);

            if (lowerName.endsWith(".pdf")) {
                List<File> extractedImages = extractImagesFromPDF(file.getAbsolutePath());
                resultFiles.addAll(extractedImages);

                copyFiles(file);
                int maxPage = getPdfPageCount(file.getAbsolutePath());
                apiCaller.callDivisionApi(configLoader.apiUserId, maxPage, fileName, unitFileInfo.getOcrServiceType());

                log.debug("PDF에서 추출된 이미지 {}개 추가됨: {}", extractedImages.size(), fileName);
            } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png")) {
                resultFiles.add(file);
                ioService.copyFiles(file);
            } else if (configLoader.ocrServiceType.equalsIgnoreCase("da") && FileExtensionUtil.DA_SUPPORTED_EXT.contains(ext)) {
                //TODO: da 서비스에서 처리해야 할 Office/HWP 파일 처리
                resultFiles.add(file);
                ioService.copyFiles(file);
            } else {
                log.warn("지원하지 않는 파일 형식: {}", fileName);
            }
        } catch (Exception e) {
            log.warn("API 조회 실패: {}", e.getMessage(), e);

        }

//        if (allFiles != null) {
//            for (File file : allFiles) {
//                if (file.isFile()) {
//                    try {
//                        FileInfo fileInfo = apiCaller.getFileWithStatus2(configLoader.apiUserId, file.getName());
//
//                        if (fileInfo != null && fileInfo.getFilename() != null) {
//                            String lowerName = file.getName().toLowerCase();
//
//                            if (lowerName.endsWith(".pdf")) {
//                                // PDF에서 이미지 추출
//                                List<File> extractedImages = extractImagesFromPDF(file.getAbsolutePath());
//                                resultFiles.addAll(extractedImages);
//
//                                // PDF 처리
//                                copyFiles(file);
//                                int maxPage = idpEngineController.getPdfPageCount(configLoader.imageFolderPath + File.separator + fileInfo.getFilename());
//                                apiCaller.callDivisionApi(configLoader.apiUserId, maxPage, fileInfo.getFilename(), fileInfo.getOcrServiceType());
//
//                                log.info("PDF에서 추출된 이미지 {}개 추가됨: {}", extractedImages.size(), file.getName());
//                            } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png")) {
//                                resultFiles.add(file); // 이미지 파일은 그대로 추가
//                            } else {
//                                log.warn("지원하지 않는 파일 형식: {}", file.getName());
//                            }
//                        }
//                    } catch (Exception e) {
//                        log.error("API 조회 실패: {}", file.getName(), e);
//                    }
//                }
//            }
//        } else {
//            log.error("폴더를 읽을 수 없습니다: {}", configLoader.imageFolderPath);
//        }

        resultFiles.sort((f1, f2) -> {
            int pageNum1 = extractPageNumber(f1.getName());
            int pageNum2 = extractPageNumber(f2.getName());
            return Integer.compare(pageNum1, pageNum2);
        });

        // 정렬 결과 로그 출력
//        log.trace("FileList 시간순 정렬 확인");
//        for (File file : resultFiles) {
//            log.trace("Filename: {}, StartDateTime: {}",
//                    file.getName());
//        }

        return resultFiles;
    }

    // 페이지 번호 추출 함수 (정렬)
    private int extractPageNumber(String fileName) {
        try {
            Pattern pattern = Pattern.compile("-page(\\d+)");
            Matcher matcher = pattern.matcher(fileName);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            // 예외 발생 시 가장 작은 값으로 처리
        }
        return Integer.MIN_VALUE;
    }

    // PDF 페이지 수 계산
    public int getPdfPageCount(String pdfFilePath) {
        try (PDDocument document = Loader.loadPDF(new File(pdfFilePath))) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            log.error("PDF 페이지 수 계산 중 오류 발생: {}", pdfFilePath, e);
            return 0;
        }
    }

}
