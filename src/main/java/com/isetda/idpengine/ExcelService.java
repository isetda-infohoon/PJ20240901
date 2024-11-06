package com.isetda.idpengine;




import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
                //json -> dat로 변경
                return lowercaseName.endsWith(".dat");
            }
        });
        if (files != null && files.length > 0) {
            // 파일들을 생성 시간 순으로 정렬
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    return Long.compare(f1.lastModified(), f2.lastModified());
                }
            });
            jsonFiles = files;
            log.info("폴더 내 JSON 파일 목록 저장 완료");
        } else {
            log.info("폴더 내 JSON 파일 목록 저장 실패 - JSON 파일 없음");
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
    public void createExcel(List<List<String>> resultList, List<List<String>> resultWord, List<List<String>> resultWeightOneWord, String fileName, String saveFilePath, int a) throws IOException {
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
            //for (int i = 1; i < resultList.size(); i++) {
            for (int i = 0; i < resultList.size(); i++) {
                Row row = sheet.createRow(startRow + i + 1); // 수정
                for (int j = 0; j < resultList.get(i).size(); j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(resultList.get(i).get(j));
//                    log.info("안녕 11 :{}  : {}", cell);
                }

                if (i == resultList.size() - 1) {
                    // Writing resultWord
                    int colNum = 2; // 3열부터 시작 (C열에 해당)

                    if (configLoader.writeExcelDetails) {
                        for (int j = 0; j < resultWord.size(); j++) {
                            List<String> rowData = resultWord.get(j);
                            Cell cell = row.createCell(colNum); // 2열에 해당

                            StringBuilder cellValue = new StringBuilder(rowData.get(0) + " 일치 단어 리스트");
                            cellValue.append(" (");

                            for (int k = 1; k < rowData.size() - 1; k++) {
                                cellValue.append(rowData.get(k));
                                if (k < rowData.size() - 2) {
                                    cellValue.append(", ");
                                }
                            }

                            cellValue.append(")");
                            cell.setCellValue(cellValue.toString());
                            colNum++;

                            Cell cell2 = row.createCell(colNum);
                            cell2.setCellValue(rowData.get(0) + " 일치 단어 전체 개수 (" + rowData.get(rowData.size() - 1) + ")");
                            colNum++;

                            if (configLoader.weightCountFlag) {
                                List<String> rowData2 = resultWeightOneWord.get(j);

                                Cell cell3 = row.createCell(colNum);

                                StringBuilder cellValue2 = new StringBuilder(rowData2.get(0) + " 가중치 1인 일치 단어 리스트");
                                cellValue2.append(" (");

                                for (int x = 1; x < rowData2.size() - 1; x++) {
                                    cellValue2.append(rowData2.get(x));
                                    if (x < rowData2.size() - 2) {
                                        cellValue2.append(", ");
                                    }
                                }

                                cellValue2.append(")");
                                cell3.setCellValue(cellValue2.toString());
                                colNum++;

                                Cell cell4 = row.createCell(colNum);
                                cell4.setCellValue(rowData2.get(0) + " 가중치 1인 일치 단어 전체 개수 (" + rowData2.get(rowData2.size() - 1) + ")");
                                colNum++;
                            }
                        }
                    }

                    // cd1, cd2, cd3 .. 분류 유형에 따라 파일 이름 작성
                    Cell cell5 = row.createCell(colNum);
                    cell5.setCellValue(fileName + " (cd"+a+")");
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
                        for (int j = 0; j < resultWord.size(); j++) {
                            List<String> rowData = resultWord.get(j);
                            Cell cell = row.createCell(colNum); // 2열에 해당

                            StringBuilder cellValue = new StringBuilder(rowData.get(0) + " 일치 단어 리스트");
                            cellValue.append(" (");

                            for (int k = 1; k < rowData.size() - 1; k++) {
                                cellValue.append(rowData.get(k));
                                if (k < rowData.size() - 2) {
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

                            if (configLoader.weightCountFlag) {
                                List<String> rowData2 = resultWeightOneWord.get(j);

                                Cell cell3 = row.createCell(colNum);

                                StringBuilder cellValue2 = new StringBuilder(rowData2.get(0) + " 가중치 1인 일치 단어 리스트");
                                cellValue2.append(" (");

                                for (int x = 1; x < rowData2.size() - 1; x++) {
                                    cellValue2.append(rowData2.get(x));
                                    if (x < rowData2.size() - 2) {
                                        cellValue2.append(", ");
                                    }
                                }

                                cellValue2.append(")");
                                cell3.setCellValue(cellValue2.toString());
                                colNum++;

                                Cell cell4 = row.createCell(colNum);
                                cell4.setCellValue(rowData2.get(0) + " 가중치 1인 일치 단어 전체 개수 (" + rowData2.get(rowData2.size() - 1) + ")");
                                colNum++;
                            }
                        }
                    }

                    // cd1, cd2, cd3 .. 분류 유형에 따라 파일 이름 작성
                    Cell cell5 = row.createCell(colNum);
                    cell5.setCellValue(fileName + " (cd"+a+")");
                }
            }
        }

        try (FileOutputStream fileOut = new FileOutputStream(saveFilePath)) {
            workbook.write(fileOut);
            log.info("엑셀 파일 생성 완료: {} ", saveFilePath);
        }
        workbook.close();
    }

    public void createExcel2(List<List<String>> resultList, List<Map<String, Object>> filteredResult, String fileName, String saveFilePath, String a) throws IOException {
        File file = new File(saveFilePath);
        Workbook workbook;
        Sheet sheet;

        Map<String, StringBuilder> templateEntries = new HashMap<>();
        Map<String, Map<String, Object>> keyCountMap = new HashMap<>();

        for (Map<String, Object> result : filteredResult) {
            String country = (String) result.get("Country");
            List<String> languages = (List<String>) result.get("Language");
            String templateName = (String) result.get("Template Name");
            String wd = (String) result.get("WD");
            Double wt = (Double) result.get("WT");
            int count = (int) result.get("Count");

            // count가 1 이상인 항목만 저장
            if (count >= 1) {
                String key = country + "(" + String.join(",", languages) + ")" + templateName;

                // 해당 키가 없다면 초기화하고, WD와 Count 값을 추가
                templateEntries.putIfAbsent(key, new StringBuilder(templateName).append("(").append(country).append(" - ").append(String.join(",", languages)).append(") 일치 단어 리스트 ("));
                StringBuilder sb = templateEntries.get(key);
                sb.append(wd).append("[").append(wt).append("]").append("(").append(count).append("), ");

                // 양식 별 일치 단어 개수 카운트
                keyCountMap.putIfAbsent(key, new HashMap<>());
                Map<String, Object> keyInfo = keyCountMap.get(key);
                keyInfo.put("Count", (int) keyInfo.getOrDefault("Count", 0) + 1);
                keyInfo.put("Country", country);
                keyInfo.put("Languages", languages);
                keyInfo.put("TemplateName", templateName);
            }
        }

        // 이미 생성된 파일이 있는지 확인
        if (file.exists()) {
            // 생성된 파일이 있는 경우 기존 파일에 이어서 결과 작성
            log.info("{} File exists. Continuing from the existing file.", fileName);

            try (FileInputStream fileIn = new FileInputStream(file)) {
                workbook = new XSSFWorkbook(fileIn);
            }
            sheet = workbook.getSheetAt(0);

            int startRow = sheet.getLastRowNum();
            //for (int i = 1; i < resultList.size(); i++) {
            for (int i = 0; i < resultList.size(); i++) {
                Row row = sheet.createRow(startRow + i + 1); // 수정
                for (int j = 0; j < resultList.get(i).size(); j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(resultList.get(i).get(j));
//                    log.info("안녕 11 :{}  : {}", cell);
                }

                if (i == resultList.size() - 1) {
                    // Writing resultWord
                    int colNum = 2; // 3열부터 시작 (C열에 해당)

                    if (configLoader.writeExcelDetails) {
                        for (Map.Entry<String, StringBuilder> entry : templateEntries.entrySet()) {
                            String keyName = entry.getKey();
                            StringBuilder wdEntries = entry.getValue();

                            // 마지막 쉼표와 공백을 제거하고 닫는 괄호 추가
                            if (wdEntries.length() > 0) {
                                wdEntries.setLength(wdEntries.length() - 2); // 마지막 쉼표와 공백 제거
                                wdEntries.append(")");
                            }

                            Cell cell = row.createCell(colNum);
                            cell.setCellValue(wdEntries.toString());
                            colNum++;

                            Cell cell2 = row.createCell(colNum);
                            Map<String, Object> keyInfo = keyCountMap.get(keyName);
                            cell2.setCellValue(keyInfo.get("TemplateName") + "(" + keyInfo.get("Country") + " - " + String.join(",", (List<String>) keyInfo.get("Languages")) + ")" + " 일치 단어 전체 개수 (" + keyInfo.get("Count") + ")");
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
            fileNameCell2.setCellValue(fileName.replace("_result", "")); // B열에 해당

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
                        for (Map.Entry<String, StringBuilder> entry : templateEntries.entrySet()) {
                            String keyName = entry.getKey();
                            StringBuilder wdEntries = entry.getValue();

                            // 마지막 쉼표와 공백을 제거하고 닫는 괄호 추가
                            if (wdEntries.length() > 0) {
                                wdEntries.setLength(wdEntries.length() - 2); // 마지막 쉼표와 공백 제거
                                wdEntries.append(")");
                            }

                            Cell cell = row.createCell(colNum);
                            cell.setCellValue(wdEntries.toString());
                            colNum++;

                            Cell cell2 = row.createCell(colNum);
                            Map<String, Object> keyInfo = keyCountMap.get(keyName);
                            cell2.setCellValue(keyInfo.get("TemplateName") + "(" + keyInfo.get("Country") + " - " + String.join(",", (List<String>) keyInfo.get("Languages")) + ")" + " 일치 단어 전체 개수 (" + keyInfo.get("Count") + ")");
                            colNum++;
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
            log.info("Excel file creation completed: {} ", saveFilePath);
        }
        workbook.close();
    }

    public static void moveFiles(String resultFilePath, Map<String, Map<String, String>> resultByVersion, String version) {
        File folder = new File(resultFilePath);
        File[] listOfFiles = folder.listFiles((dir, name) -> name.endsWith(".xlsx"));

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                String fileName = file.getName();
                String baseName = fileName.replace("_result.xlsx", ""); // 파일 이름에서 확장자 제거
                System.out.println("base name : " + baseName);

                if (resultByVersion != null) {
                    Map<String, String> valueList = resultByVersion.get(baseName);

                    if (valueList != null) {
                        String value = valueList.get(version); // 버전 별 결과 양식
                        if (value != null) {
                            Path targetDir = Paths.get(resultFilePath, value);
                            if (!Files.exists(targetDir)) {
                                try {
                                    Files.createDirectories(targetDir);
                                } catch (IOException e) {
                                    log.info("Folder create failed");
                                    e.printStackTrace();
                                }
                            }

                            File[] relatedFiles = folder.listFiles((dir, name) -> name.startsWith(baseName));
                            if (relatedFiles != null) {
                                for (File relatedFile : relatedFiles) {
                                    Path targetPath = targetDir.resolve(relatedFile.getName());
                                    try {
                                        Files.move(relatedFile.toPath(), targetPath);
                                        log.info("Moved file : '{}' to '{}'",  relatedFile.getName(), targetPath);
                                    } catch (IOException e) {
                                        log.info("'{}' File move failed : {}", relatedFile.getName(), e);
                                    }
                                }
                            }

                            // PDF 파일 이동 처리
                            Pattern pattern = Pattern.compile("(.+)-page\\d+$");
                            Matcher matcher = pattern.matcher(baseName);
                            if (matcher.matches()) {
                                String pdfBaseName = matcher.group(1); // -page?를 제외한 이름
                                File[] pdfFiles = folder.listFiles((dir, name) -> name.startsWith(pdfBaseName) && name.endsWith(".pdf"));
                                if (pdfFiles != null) {
                                    for (File pdfFile : pdfFiles) {
                                        Path targetPath = targetDir.resolve(pdfFile.getName());
                                        try {
                                            Files.copy(pdfFile.toPath(), targetPath);
                                            log.info("Moved PDF file : '{}' to '{}'", pdfFile.getName(), targetPath);
                                        } catch (IOException e) {
                                            log.info("'{}' PDF file move failed : {}", pdfFile.getName(), e);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 모든 파일을 확인한 후, 폴더를 제외하고 남아있는 PDF 파일 삭제
        File[] remainingFiles = folder.listFiles();
        if (remainingFiles != null) {
            for (File remainingFile : remainingFiles) {
                if (remainingFile.isFile() && remainingFile.getName().endsWith(".pdf")) {
                    try {
                        Files.delete(remainingFile.toPath());
                        log.info("Deleted PDF file : '{}'", remainingFile.getName());
                    } catch (IOException e) {
                        log.info("'{}' PDF file delete failed : {}", remainingFile.getName(), e);
                    }
                }
            }
        }
    }

    public static void datasetWriteExcel(Map<String, List<Map<String, Object>>> jsonData) {
        try (Workbook workbook = new XSSFWorkbook()) {
            for (String sheetName : jsonData.keySet()) {
                Sheet sheet = workbook.createSheet(sheetName);
                List<Map<String, Object>> rows = jsonData.get(sheetName);

                if (!rows.isEmpty()) {
                    // 헤더 생성
                    Row headerRow = sheet.createRow(0);
                    Map<String, Object> firstRow = rows.get(0);
                    int headerCellIndex = 0;
                    for (String key : firstRow.keySet()) {
                        if (!key.equals("H-RULE") && !key.equals("Language")) {
                            Cell cell = headerRow.createCell(headerCellIndex++);
                            cell.setCellValue(key);
                        }
                    }
                    // Language 헤더 추가
                    headerRow.createCell(headerCellIndex++).setCellValue("Language");
                    // H-RULE 헤더 추가
                    headerRow.createCell(headerCellIndex++).setCellValue("WD");
                    headerRow.createCell(headerCellIndex++).setCellValue("WT");
                    headerRow.createCell(headerCellIndex++).setCellValue("KR");

                    // 데이터 행 생성
                    int rowIndex = 1;
                    for (Map<String, Object> rowData : rows) {
                        Row row = sheet.createRow(rowIndex++);
                        int cellIndex = 0;
                        for (String key : rowData.keySet()) {
                            if (!key.equals("H-RULE") && !key.equals("Language")) {
                                Cell cell = row.createCell(cellIndex++);
                                Object value = rowData.get(key);
                                if (value instanceof String) {
                                    cell.setCellValue((String) value);
                                } else if (value instanceof Integer) {
                                    cell.setCellValue((Integer) value);
                                } else if (value instanceof Double) {
                                    cell.setCellValue((Double) value);
                                }
                            }
                        }
                        // Language 데이터 추가
                        List<String> languageList = (List<String>) rowData.get("Language");
                        String languages = String.join(", ", languageList);
                        row.createCell(cellIndex++).setCellValue(languages);

                        // H-RULE 데이터 추가
                        List<Map<String, Object>> hRuleList = (List<Map<String, Object>>) rowData.get("H-RULE");
                        for (Map<String, Object> hRule : hRuleList) {
                            row.createCell(cellIndex++).setCellValue(hRule.get("WD").toString());
                            row.createCell(cellIndex++).setCellValue(Double.parseDouble(hRule.get("WT").toString()));
                            row.createCell(cellIndex++).setCellValue(hRule.get("KR").toString());
                        }
                    }
                }
            }

            // 엑셀 파일 저장
            try (FileOutputStream fileOut = new FileOutputStream("C:\\Users\\suaah\\IdeaProjects\\test\\result\\dataset.xlsx")) {
                workbook.write(fileOut);
            }

            System.out.println("엑셀 파일이 성공적으로 생성되었습니다!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void datasetWriteExcel2(List<Map<String, Object>> filteredResult) {
        // filteredResult를 엑셀 파일로 작성
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Filtered Result");

            // 헤더 생성
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Country", "Template Name", "Language", "WD", "WT", "KR", "Count"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 데이터 행 생성
            int rowIndex = 1;
            for (Map<String, Object> resultMap : filteredResult) {
                Row row = sheet.createRow(rowIndex++);
                int cellIndex = 0;

                row.createCell(cellIndex++).setCellValue((String) resultMap.get("Country"));
                row.createCell(cellIndex++).setCellValue((String) resultMap.get("Template Name"));
                row.createCell(cellIndex++).setCellValue(String.join(", ", (List<String>) resultMap.get("Language")));
                row.createCell(cellIndex++).setCellValue((String) resultMap.get("WD"));
                row.createCell(cellIndex++).setCellValue((Double) resultMap.get("WT"));
                row.createCell(cellIndex++).setCellValue((String) resultMap.get("KR"));
                row.createCell(cellIndex++).setCellValue((Integer) resultMap.get("Count"));
            }

            // 엑셀 파일 저장
            try (FileOutputStream fileOut = new FileOutputStream("C:\\Users\\suaah\\IdeaProjects\\test\\result\\dataset_filtered_result2.xlsx")) {
                workbook.write(fileOut);
            }

            System.out.println("엑셀 파일이 성공적으로 생성되었습니다!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void datasetWriteExcel3(List<Map<String, Object>> jsonData, String filePath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Grouped Result");

            // 헤더 생성
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Country", "Template Name", "Language", "WD", "WT", "KR", "Count"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 데이터 행 생성
            int rowIndex = 1;
            for (Map<String, Object> resultMap : jsonData) {
                Row row = sheet.createRow(rowIndex++);
                int cellIndex = 0;

                row.createCell(cellIndex++).setCellValue((String) resultMap.get("Country"));
                row.createCell(cellIndex++).setCellValue((String) resultMap.get("Template Name"));
                row.createCell(cellIndex++).setCellValue(String.join(", ", (List<String>) resultMap.get("Language")));
                row.createCell(cellIndex++).setCellValue((String) resultMap.get("WD"));
                row.createCell(cellIndex++).setCellValue((Double) resultMap.get("WT"));
                row.createCell(cellIndex++).setCellValue((String) resultMap.get("KR"));

                // Count 필드 처리
                Object countValue = resultMap.get("Count");
                if (countValue instanceof Long) {
                    row.createCell(cellIndex++).setCellValue(((Long) countValue).doubleValue());
                } else if (countValue instanceof Integer) {
                    row.createCell(cellIndex++).setCellValue(((Integer) countValue).doubleValue());
                }
            }

            // 엑셀 파일 저장
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }

            System.out.println("엑셀 파일이 성공적으로 생성되었습니다!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

