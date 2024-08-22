package com.isetda.idpengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class IMGFileIOService {
    private static final Logger log = LogManager.getLogger(IMGFileIOService.class);

    private ConfigLoader configLoader = ConfigLoader.getInstance();

    String imageFolderPath = configLoader.getImageFolderPath();

    public File[] getFilteredFiles(String folderPath) {
        log.info("폴더에서 파일을 필터링 시작: {}", folderPath);
        File folder = new File(folderPath);
        List<File> filteredFiles = new ArrayList<>();

        // 파일 및 폴더를 재귀적으로 탐색
        findFilesRecursively(folder, filteredFiles);

        if (filteredFiles.isEmpty()) {
            log.error("폴더에서 파일을 가져오는 중 오류가 발생했습니다: {}", folderPath);
        } else {
            log.info("폴더에서 {}개의 파일을 가져왔습니다: {}", filteredFiles.size(), folderPath);
        }

        // 리스트를 배열로 변환하여 반환
        return filteredFiles.toArray(new File[0]);
    }

    private void findFilesRecursively(File folder, List<File> filteredFiles) {
        File[] filesAndDirs = folder.listFiles();
        if (filesAndDirs != null) {
            for (File file : filesAndDirs) {
                if (file.isDirectory()) {
                    log.debug("디렉토리 발견, 재귀적으로 탐색: {}", file.getAbsolutePath());
                    // 서브 폴더를 재귀적으로 탐색
                    findFilesRecursively(file, filteredFiles);
                } else {
                    // 파일이 이미지 또는 PDF인 경우 리스트에 추가
                    String lowercaseName = file.getName().toLowerCase();
                    if (lowercaseName.endsWith(".jpg") || lowercaseName.endsWith(".png") || lowercaseName.endsWith(".pdf")) {
                        log.debug("필터링된 파일 추가: {}", file.getAbsolutePath());
                        filteredFiles.add(file);
                    }
                }
            }
        } else {
            log.error("폴더에서 파일 목록을 가져오는 중 오류 발생: {}", folder.getAbsolutePath());
        }
    }

    // 파일 삭제 메서드
    public void deleteFilesInFolder(String folderPath) {
        log.info("폴더의 모든 파일 삭제 시작: {}", folderPath);
        File folder = new File(folderPath);

        // 폴더가 존재하지 않거나 디렉토리가 아닌 경우
        if (!folder.exists() || !folder.isDirectory()) {
            log.error("폴더가 존재하지 않거나 디렉토리가 아닙니다: {}", folderPath);
            return;
        }

        // 폴더 안의 파일들을 삭제
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        log.info("파일 삭제 성공: {}", file.getName());
                    } else {
                        log.warn("파일 삭제 실패: {}", file.getName());
                    }
                }
            }
        } else {
            log.error("폴더에서 파일을 가져오는 중 오류가 발생했습니다: {}", folderPath);
        }
    }

    // 파일 복사
    public void copyFiles(File[] files) throws IOException {
        log.info("파일 복사 시작...");
        for (File file : files) {
            Path sourcePath = file.toPath();
            Path destinationPath = Paths.get(imageFolderPath, file.getName());
            try {
                Files.copy(sourcePath, destinationPath);
                log.info("파일 복사 성공: {} -> {}", sourcePath, destinationPath);
            } catch (IOException e) {
                log.error("파일 복사 중 오류 발생: {} -> {}", sourcePath, destinationPath, e);
                throw e; // 오류 발생 시 던지기
            }
        }
        log.info("파일 복사 완료.");
    }
}
