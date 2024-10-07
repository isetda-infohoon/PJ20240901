package com.isetda.idpengine;




import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;


public class ExcelService {
    private static final Logger log = LogManager.getLogger(ExcelService.class);

    public ConfigLoader configLoader;
//    boolean dbDataUsageFlag = configLoader.isDbDataUsageFlag();

    public File[] jsonFiles;
    public List<List<String>> resultList; // 각 변수로
    public List<List<String>> resultWord;
    public String docType="";

    public String fileName;

    // 폴더에서 JSON 파일 가져오기
    public File[] getFilteredJsonFiles() {
        File folder = new File(configLoader.resultFilePath);

        File[] files = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                return lowercaseName.endsWith(".json");
            }
        });

        jsonFiles = files;

        if (jsonFiles.length == 0) {
            log.info("폴더 내 JSON 파일 목록 저장 실패 - JSON 파일 없음");
        } else {
            log.info("폴더 내 JSON 파일 목록 저장 완료");
        }

        return jsonFiles;
    }


    // 엑셀의 단어 리스트 가져오기
    public Map<String, List<List<String[]>>> getExcelData() {
        Map<String, List<List<String[]>>> excelData = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(configLoader.excelFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                List<List<String[]>> sheetData = new ArrayList<>();

                log.info("시트명: {}", sheet.getSheetName());

                int maxColumns = 0;
                for (Row row : sheet) {
                    if (row.getLastCellNum() > maxColumns) {
                        maxColumns = row.getLastCellNum();
                    }
                }

                // 단어 리스트가 있는 열을 반복
                for (int col = 0; col < maxColumns; col+=2) {
                    List<String[]> columnData = new ArrayList<>();
                    boolean isFirstIteration = true;

                    // 각 단어와 가중치 값을 가져와 배열로 함께 저장
                    for (Row row : sheet) {
                        String[] value = new String[2];
                        Cell cell = row.getCell(col);

                        if (cell != null && !cell.toString().isEmpty()) {
                            value[0] = cell.toString();
                            value[1] = row.getCell(col+1).toString();
                            columnData.add(value);

                            if (isFirstIteration) {
                                log.info("H: {}, {}", value[0], value[1]);
                                isFirstIteration = false;
                            } else {
                                log.info("W: {}, {}", value[0], value[1]);
                            }
                        }
                    }
                    if (!columnData.isEmpty()) {
                        sheetData.add(columnData);
                    }
                }

                excelData.put(sheet.getSheetName(), sheetData);
            }

            log.info("엑셀 단어 리스트 추출 완료");

        } catch (IOException e) {
            log.error("엑셀 단어 리스트 추출 실패: {}", e.getStackTrace()[0]);
        }

        // 엑셀 데이터 출력 (테스트용)
        for (Map.Entry<String, List<List<String[]>>> entry : excelData.entrySet()) {
            log.info("엑셀 시트: {}", entry.getKey());
            for (List<String[]> column : entry.getValue()) {
                log.info("엑셀 값: {}", column);
            }
        }
        return excelData;
    }

//    public void getDatabase() {
//        DBService databaseConnection = new DBService();
//        Connection connection = databaseConnection.getConnection();
//
//        Map<String, List<List<String[]>>> database = new HashMap<>();
//
//
//
//    }

    // 엑셀에 결과 값 쓰기
    public void createExcel(List<List<String>> resultList, List<List<String>> resultWord, String fileName, String saveFilePath, int a) throws IOException {
        File file = new File(saveFilePath);
        Workbook workbook;
        Sheet sheet;

        // 이미 생성된 파일이 있는지 확인
        if (file.exists()) {
            // 생성된 파일이 있는 경우 기존 파일에 이어서 결과 작성
            log.info("{} File exists. Continuing from the existing file.", fileName);

            try (FileInputStream fileIn = new FileInputStream(file)) {
                workbook = new XSSFWorkbook(fileIn);
            }
            sheet = workbook.getSheetAt(0);

            int startRow = sheet.getLastRowNum();
            for (int i = 1; i < resultList.size(); i++) {
                Row row = sheet.createRow(startRow + i);
                for (int j = 0; j < resultList.get(i).size(); j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(resultList.get(i).get(j));
//                    log.info("안녕 11 :{}  : {}", cell);
                }

                if (i == resultList.size() - 1) {
                    // Writing resultWord
                    int colNum = 2; // 3열부터 시작 (C열에 해당)

                    if (configLoader.writeExcelDetails) {
                        for (List<String> rowData : resultWord) {
                            Cell cell = row.createCell(colNum); // 2열에 해당

                            StringBuilder cellValue = new StringBuilder(rowData.get(0) + " 일치 단어 리스트");
                            cellValue.append(" (");

                            for (int j = 1; j < rowData.size() - 1; j++) {
                                cellValue.append(rowData.get(j));
                                if (j < rowData.size() - 2) {
                                    cellValue.append(", ");
                                }
                            }

                            cellValue.append(")");
                            cell.setCellValue(cellValue.toString());
                            colNum++;

                            Cell cell2 = row.createCell(colNum);
                            cell2.setCellValue(rowData.get(0) + " 일치 단어 전체 개수 (" + rowData.get(rowData.size() - 1) + ")");
                            colNum++;
                        }
                    }

                    // cd1, cd2, cd3 .. 분류 유형에 따라 파일 이름 작성
                    Cell cell3 = row.createCell(colNum);
                    cell3.setCellValue(fileName + " (cd"+a+")");
                }
            }
        } else {
            // 생성된 파일이 없는 경우 새 파일 생성
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("Sheet1");

            // 첫 번째 행에 파일 이름 삽입
            Row firstRow = sheet.createRow(0);
            Cell fileNameCell = firstRow.createCell(0); // A열에 해당
            fileNameCell.setCellValue("파일이름");
            Cell fileNameCell2 = firstRow.createCell(1);
            fileNameCell2.setCellValue(fileName.replace("_OCR_result", "")); // B열에 해당

            // Writing resultList
            int startRow = sheet.getLastRowNum() + 1;
            for (int i = 0; i < resultList.size(); i++) {
                Row row = sheet.createRow(startRow + i);
                for (int j = 0; j < resultList.get(i).size(); j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(resultList.get(i).get(j));
                }

                if (i == resultList.size() - 1) {
                    // Writing resultWord
                    int colNum = 2; // 3열부터 시작 (C열에 해당)

                    if (configLoader.writeExcelDetails) {
                        for (List<String> rowData : resultWord) {
                            Cell cell = row.createCell(colNum); // 2열에 해당

                            StringBuilder cellValue = new StringBuilder(rowData.get(0) + " 일치 단어 리스트");
                            cellValue.append(" (");

                            for (int j = 1; j < rowData.size() - 1; j++) {
                                cellValue.append(rowData.get(j));
                                if (j < rowData.size() - 2) {
                                    cellValue.append(", ");
                                }
                            }

                            cellValue.append(")");
                            cell.setCellValue(cellValue.toString());
                            colNum++;

                            Cell cell2 = row.createCell(colNum);
                            cell2.setCellValue(rowData.get(0) + " 일치 단어 전체 개수 (" + rowData.get(rowData.size() - 1) + ")");
                            colNum++;
                            log.info("안녕 11 :{}  : {}", cell);
                            log.info("안녕 22 :{}  : {}", cell2);
                        }
                    }

                    // cd1, cd2, cd3 .. 분류 유형에 따라 파일 이름 작성
                    Cell cell3 = row.createCell(colNum);
                    cell3.setCellValue(fileName + " (cd"+a+")");
                }
            }
        }

        try (FileOutputStream fileOut = new FileOutputStream(saveFilePath)) {
            workbook.write(fileOut);
            log.info("엑셀 파일 생성 완료: {} ", saveFilePath);
        }
        workbook.close();
    }

}

