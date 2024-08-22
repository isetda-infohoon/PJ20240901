package com.isetda.idpengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

public class ExcelService {
    private static final Logger log = LogManager.getLogger(ExcelService.class);

    //환경변수 인스턴스 생성
    private ConfigLoader configLoader = ConfigLoader.getInstance();

//    private JsonService jsonService = new JsonService();

    private void createLayoutExcelFile(String filePath, String mostHeader, Map<String, Map<String, Integer>> foundData) {
        Path path = Paths.get(filePath);
        // 파일 존재 여부 확인
        if (Files.exists(path)) {
            log.debug("파일이 이미 존재합니다: {}", filePath);
            return;  // 기존 파일이 존재하면 넘어가기
        }
        log.info("새 엑셀 파일을 생성합니다: {}", filePath);

        // XSSFWorkbook 생성
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fileOut = new FileOutputStream(filePath)) {

            Sheet sheet = workbook.createSheet("Sheet1");
            Row headerRow = sheet.createRow(0);
            String[] headers = {"파일명", "국가", "문서종류", "키워드", "키워드 개수"};

            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue(path.getFileName().toString());
//            dataRow.createCell(1).setCellValue(jsonService.jsonLocal);
            dataRow.createCell(2).setCellValue(mostHeader);

            if (configLoader.isDeletedCheck()) {
                String keywordData = foundData.entrySet().stream().map(entry -> entry.getKey() + ": (" + entry.getValue().entrySet().stream().map(val -> val.getKey() + "(" + val.getValue() + ")").collect(Collectors.joining(", ")) + "); ").collect(Collectors.joining());

                dataRow.createCell(3).setCellValue(keywordData);

                //String headerCountData = columnData.entrySet().stream().map(entry -> entry.getKey() + "(" + (foundData.get(entry.getKey()) != null ? foundData.get(entry.getKey()).size() : 0) + ")").collect(Collectors.joining(" "));

                //dataRow.createCell(4).setCellValue(headerCountData);
            } else {
                dataRow.createCell(3).setCellValue("키워드 정보 포함 안 함");
                dataRow.createCell(4).setCellValue("키워드 개수 포함 안 함");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(fileOut);
            log.info("엑셀 파일 작성 및 저장 성공: {}", filePath);

        } catch (FileNotFoundException e) {
            log.error("파일을 찾을 수 없습니다: {}", filePath, e);
        } catch (IOException e) {
            log.error("엑셀 파일 저장 중 오류 발생: {}", filePath, e);
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생: {}", e.getMessage(), e);
        }
    }
}
