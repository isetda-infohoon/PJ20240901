package com.isetda.idpengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class IMGFileIOService {
    private static final Logger log = LogManager.getLogger(IMGFileIOService.class);
    public ConfigLoader configLoader;
//    String resultFilePath;
//왜 이래
    public File[] getFilteredFiles() {
        log.info("{} 경로의 폴더에서 파일을 필터링 시작", configLoader.imageFolderPath);
        File folder = new File(configLoader.imageFolderPath);
        List<File> filteredFiles = new ArrayList<>();

        // 파일 및 폴더를 재귀적으로 탐색
        findFilesRecursively(folder, filteredFiles);

        if (filteredFiles.isEmpty()) {
            log.info("{} 폴더에서 파일이 없습니다", configLoader.imageFolderPath);
        } else {
            log.info("{} 폴더에서 {}개의 파일을 가져왔습니다", configLoader.imageFolderPath, filteredFiles.size());
        }
        // 리스트를 배열로 변환하여 반환
        return filteredFiles.toArray(new File[0]);
    }

    private void findFilesRecursively(File folder, List<File> filteredFiles) {
        File[] filesAndDirs = folder.listFiles();
        if (filesAndDirs != null) {
            for (File file : filesAndDirs) {
                if (file.isDirectory()) {
                    log.info("폴더 안에 폴더 탐색: {}", file.getAbsolutePath());
                    // 서브 폴더를 재귀적으로 탐색
                    findFilesRecursively(file, filteredFiles);
                } else {
                    // 파일이 이미지 또는 PDF인 경우 리스트에 추가
                    String lowercaseName = file.getName().toLowerCase();
                    if (lowercaseName.endsWith(".jpg") || lowercaseName.endsWith(".png") || lowercaseName.endsWith(".jpeg")) {
                        filteredFiles.add(file);
                    } else if (lowercaseName.endsWith(".pdf")) {
                        log.info("PDF 파일 발견: {}", file.getAbsolutePath());
                        try {
                            // PDF에서 추출된 이미지를 필터링된 파일 리스트에 추가
                            filteredFiles.addAll(extractImagesFromPDF(file.getAbsolutePath()));
                        } catch (IOException e) {
                            log.error("PDF 파일에서 이미지를 추출하는 중 오류 발생: {}", file.getAbsolutePath(), e);
                        }
                    }
                }
            }
        } else {
            log.error("폴더에서 파일 목록을 가져오는 중 오류 발생: {}", folder.getAbsolutePath());
        }
    }

    // PDF에서 이미지를 추출하는 메서드 (수정됨: 추출된 이미지를 반환)
    private List<File> extractImagesFromPDF(String pdfPath) throws IOException {
        List<File> extractedImages = new ArrayList<>();
        log.info("www : {}",pdfPath);

        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            for (int page = 0; page < document.getNumberOfPages(); ++page) {
                BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 600, ImageType.RGB);
                String fileName = pdfPath.replace(".pdf", "") + "PDF-" + page + ".jpg";
                File imageFile = new File(configLoader.resultFilePath, new File(fileName).getName());
                ImageIO.write(bim, "jpg", imageFile);
                extractedImages.add(imageFile);

                log.info("PDF에서 이미지 추출 완료: {}", imageFile.getAbsolutePath());
            }
        } catch (IOException e) {
            log.error("PDF 파일에서 이미지를 추출하는 중 오류 발생: {}", pdfPath, e);
            throw e;
        }

        return extractedImages; // 추출된 이미지를 반환
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
                        log.info("파일 삭제 성공: 이름: {}", fileName);
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
            if (file.getName().contains("PDF-")) {
                log.info("PDF에서 추출된 이미지는 복사하지 않음: {}", file.getName());
                return;
            }

            // 동일한 이름의 파일이 이미 존재하는지 확인
            if (Files.exists(destinationPath)) {
                log.info("이미지가 이미 존재: {}", destinationPath);
                return;
            }
            try {
                Files.copy(sourcePath, destinationPath);
                // 파일 이름과 확장자만 로그에 출력
                String fileName = file.getName();
                log.info("복사 성공: 이름: {} -> 저장 경로: {}", fileName, destinationPath);
            } catch (IOException e) {
                String fileName = file.getName();
                log.error("복사 중 오류 발생: 이름: {} -> 저장경로: {}, 오류: {}", fileName, destinationPath, e.getMessage(), e);
                throw e; // 오류 발생 시 던지기
            }
    }
}
