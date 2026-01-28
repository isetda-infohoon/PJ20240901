package com.isetda.idpengine;




import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class ExcelService {
    private static final Logger log = LogManager.getLogger(ExcelService.class);

    public ConfigLoader configLoader;
//    boolean dbDataUsageFlag = configLoader.isDbDataUsageFlag();

    public File[] jsonFiles;
    public List<List<String>> resultList; // 각 변수로
    public List<List<String>> resultWord;
    public String docType="";

    public String fileName;
    public String classificationStartDateTime;

    // 폴더에서 JSON 파일 가져오기
    public File[] getFilteredJsonFiles() {
        APICaller apiCaller = new APICaller();
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

                    if (configLoader.writeDetailResult) {
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

                    if (configLoader.writeDetailResult) {
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
            int pl = result.containsKey("PL") ? (int) result.get("PL") : 0;

            // count가 1 이상인 항목만 저장
            if (count >= 1) {
                String key = country + "(" + String.join(",", languages) + ")" + templateName;

                // 해당 키가 없다면 초기화하고, WD와 Count 값을 추가
                templateEntries.putIfAbsent(key, new StringBuilder(templateName).append("(").append(country).append(" - ").append(String.join(",", languages)).append(") 일치 단어 리스트 ("));
                StringBuilder sb = templateEntries.get(key);
                sb.append(wd).append("[").append(wt).append("]").append("[").append(pl).append("]").append("(").append(count).append("), ");

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
            log.info("{} Excel File exists. Continuing from the existing file.", fileName);

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

                    if (configLoader.writeDetailResult) {
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

                    if (configLoader.writeDetailResult) {
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

    public void createText(List<List<String>> resultList, List<Map<String, Object>> filteredResult, String fileName, String saveFilePath, String a) throws IOException {
        Map<String, StringBuilder> templateEntries = new HashMap<>();
        Map<String, Map<String, Object>> keyCountMap = new HashMap<>();

        for (Map<String, Object> result : filteredResult) {
            String country = (String) result.get("Country");
            List<String> languages = (List<String>) result.get("Language");
            String templateName = (String) result.get("Template Name");
            String wd = (String) result.get("WD");
            Double wt = (Double) result.get("WT");
            int count = (int) result.get("Count");
            int pl = result.containsKey("PL") ? (int) result.get("PL") : 0;

            // count가 1 이상인 항목만 저장
            if (count >= 1) {
                String key = country + "(" + String.join(",", languages) + ")" + templateName;

                // 해당 키가 없다면 초기화하고, WD와 Count 값을 추가
                templateEntries.putIfAbsent(key, new StringBuilder(templateName).append("(").append(country).append(" - ").append(String.join(",", languages)).append(") 일치 단어 리스트 ("));
                StringBuilder sb = templateEntries.get(key);
                sb.append(wd).append("[").append(wt).append("]").append("[").append(pl).append("]").append("(").append(count).append("), ");

                // 양식 별 일치 단어 개수 카운트
                keyCountMap.putIfAbsent(key, new HashMap<>());
                Map<String, Object> keyInfo = keyCountMap.get(key);
                keyInfo.put("Count", (int) keyInfo.getOrDefault("Count", 0) + 1);
                keyInfo.put("Country", country);
                keyInfo.put("Languages", languages);
                keyInfo.put("TemplateName", templateName);
            }
        }

        try {
            // 파일이 존재하는지 확인
            boolean fileExists = Files.exists(Paths.get(saveFilePath));

            // FileWriter의 두 번째 인자로 true를 전달하여 파일에 이어서 쓰기 모드로 설정
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(saveFilePath, true))) {
                if (fileExists) {
                    log.debug("{} Text File exists. Continuing from the existing file.", fileName);

                    writer.write("[cd " + a + "]\n");

                    //
                    for (int i = 0; i < resultList.size(); i++) {
                        writer.write(resultList.get(i).get(0) + ": " + resultList.get(i).get(1) + "\n");
                    }
                    writer.write("\n");

                    if (configLoader.writeDetailResult) {
                        List<Map.Entry<String, StringBuilder>> entryList = new ArrayList<>(templateEntries.entrySet());

                        for (int j = 0; j < entryList.size(); j++) {
                            Map.Entry<String, StringBuilder> entry = entryList.get(j);
                            String keyName = entry.getKey();
                            StringBuilder wdEntries = entry.getValue();

                            // 마지막 쉼표와 공백을 제거하고 닫는 괄호 추가
                            if (wdEntries.length() > 0) {
                                wdEntries.setLength(wdEntries.length() - 2); // 마지막 쉼표와 공백 제거
                                wdEntries.append(")");
                            }

                            writer.write(wdEntries + "\n");
                            Map<String, Object> keyInfo = keyCountMap.get(keyName);
                            writer.write(keyInfo.get("TemplateName") + "(" + keyInfo.get("Country") + " - " + String.join(",", (List<String>) keyInfo.get("Languages")) + ")" + " 일치 단어 전체 개수 (" + keyInfo.get("Count") + ")" + "\n");
                            writer.write("\n");
                        }
                    }
                } else {
                    // 첫 번째 행에 파일 이름 삽입
                    writer.write("파일이름: " + fileName.replace("_result", "") + "\n");
                    writer.write("\n");
                    writer.write("-----------------------------------------------------\n");
                    writer.write("\n");
                    writer.write("[cd " + a + "]\n");

                    //
                    for (int i = 0; i < resultList.size(); i++) {
                        writer.write(resultList.get(i).get(0) + ": " + resultList.get(i).get(1) + "\n");
                    }
                    writer.write("\n");

                    if (configLoader.writeDetailResult) {
                        List<Map.Entry<String, StringBuilder>> entryList = new ArrayList<>(templateEntries.entrySet());

                        for (int j = 0; j < entryList.size(); j++) {
                            Map.Entry<String, StringBuilder> entry = entryList.get(j);
                            String keyName = entry.getKey();
                            StringBuilder wdEntries = entry.getValue();

                            // 마지막 쉼표와 공백을 제거하고 닫는 괄호 추가
                            if (wdEntries.length() > 0) {
                                wdEntries.setLength(wdEntries.length() - 2); // 마지막 쉼표와 공백 제거
                                wdEntries.append(")");
                            }

                            writer.write(wdEntries + "\n");
                            Map<String, Object> keyInfo = keyCountMap.get(keyName);
                            writer.write(keyInfo.get("TemplateName") + "(" + keyInfo.get("Country") + " - " + String.join(",", (List<String>) keyInfo.get("Languages")) + ")" + " 일치 단어 전체 개수 (" + keyInfo.get("Count") + ")" + "\n");
                            writer.write("\n");

//                            // api 사용 시 update 진행
//                            if (configLoader.apiUsageFlag) {
//                                jsonDataWithUpdate(fileName, keyInfo);
//                            }
                        }
                    }
                }
                writer.write("-----------------------------------------------------\n");
                writer.write("\n");

                log.info("Text file creation completed: {} ", saveFilePath);
            }
        } catch (Exception e) {
            log.warn("Text file creation failed: {} " + e.getMessage());
        }
    }

    public void jsonDataWithUpdate(String fileName, String[] values) throws UnirestException {
        log.debug("update start");

        APICaller apiCaller = new APICaller();
        IOService ioService = new IOService();
        String userId = configLoader.apiUserId;
        String name = fileName.replace("_result", "");
        String[] extensions = {".jpg", ".png", ".jpeg", ".JPG", ".PNG", ".JPEG"};
        FileInfo fileInfo = null;
        String imageExt = null;

        for (String ext : extensions) {
            fileInfo = apiCaller.getFileByName(userId, name + ext);
            if (fileInfo != null && fileInfo.getFilename() != null) {
                imageExt = ext;
                break; // 성공했으면 반복 종료
            }
        }

        if (fileInfo == null || fileInfo.getFilename() == null) {
            log.warn("파일 정보가 없습니다. API 호출 생략: {}", name);
            return;
        }

        // 원본 파일이 아닌 경우 (jpg가 원본일 경우를 제외, pdf에서 추출된 page.jpg인 경우)
        if (fileInfo.getPageNum() != 0) {
            String basename = name.replaceAll("-page\\d+$", "");
            //int maxPage = ioService.getPdfPageCount(configLoader.resultFilePath + File.separator + values[2] + File.separator + basename + ".pdf");
            String endDateTime = getCurrentTime();

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("fileName", fileInfo.getFilename());
            jsonBody.put("pageNum", fileInfo.getPageNum());
            jsonBody.put("userId", userId);
            jsonBody.put("ocrServiceType", fileInfo.getOcrServiceType());
            jsonBody.put("language", fileInfo.getLanguage());
            jsonBody.put("lClassification", getCountryName(fileInfo.getLanguage()));
            jsonBody.put("mClassification", values[2]);
            if (values[2].contains("미분류")) {
                jsonBody.put("classificationStatus", "CF"); // 미분류: CF
            } else {
                jsonBody.put("classificationStatus", "CS"); // 정상분류: CS
            }
            jsonBody.put("ocrResultFileName", name + "_result.dat");
            jsonBody.put("classificationResultFileName", name + "_result.txt");
            jsonBody.put("classificationStartDateTime", classificationStartDateTime);
            jsonBody.put("classificationEndDateTime", endDateTime);

            apiCaller.callUpdateApi(jsonBody);

            // 마지막 페이지인 경우 원본 PDF 파일 업데이트 진행
//            if (fileInfo.getPageNum() == maxPage) {
//                FileInfo resultFileInfo = apiCaller.getFileWithStatus3(userId, basename + "-page1.jpg", "CS");
//                if (resultFileInfo.getFilename() == null) {
//                    resultFileInfo = apiCaller.getFileWithStatus3(userId, basename + "-page1.jpg", "CF");
//                }
//
//                System.out.println("resultFileInfo의 basename : " + basename + "-page1.jpg");
//
//                System.out.println("userid : " + resultFileInfo.getUserId());
//                System.out.println("fileName : " + resultFileInfo.getFilename());
//                System.out.println("ocrServiceType : " + resultFileInfo.getOcrServiceType());
//                System.out.println("language : " + resultFileInfo.getLanguage());
//                System.out.println("lClassification : " + resultFileInfo.getLClassification());
//                System.out.println("mClassification : " + resultFileInfo.getMClassification());
//                System.out.println("classificationStatus : " + resultFileInfo.getClassificationStatus());
//                System.out.println("classificationStartDateTime : " + resultFileInfo.getClassificationStartDateTime());
//                System.out.println("classificationEndDateTime : " + resultFileInfo.getClassificationEndDateTime());
//
//
//                JSONObject pdfJsonBody = new JSONObject();
//                pdfJsonBody.put("fileName", basename + ".pdf");
//                pdfJsonBody.put("pageNum", 0);
//                pdfJsonBody.put("userId", userId);
//                pdfJsonBody.put("ocrServiceType", resultFileInfo.getOcrServiceType());
//                pdfJsonBody.put("language", resultFileInfo.getLanguage());
//                pdfJsonBody.put("lClassification", resultFileInfo.getLClassification());
//                pdfJsonBody.put("mClassification", resultFileInfo.getMClassification());
//                pdfJsonBody.put("classificationStatus", resultFileInfo.getClassificationStatus());
//                pdfJsonBody.put("classificationStartDateTime", resultFileInfo.getClassificationStartDateTime());
//                pdfJsonBody.put("classificationEndDateTime", endDateTime);
//
//                apiCaller.callUpdateApi(pdfJsonBody);
//            }

            // TODO: 마지막 페이지일 때 원본 pdf 업데이트 수정 했고 테스트 필요 / 테스트 전  if (fileInfo.getPageNum() == maxPage) 조건 수정 필요
            int processedCount = getProcessedPageCount(userId, basename); // 처리된 페이지 수
            int totalPageCount = ioService.getPdfPageCount(configLoader.resultFilePath + File.separator + values[2] + File.separator + basename + ".pdf"); // PDF의 총 페이지 수

            log.debug("{} 처리된 페이지 수: {}, PDF 총 페이지 수: {}", basename+".pdf", processedCount, totalPageCount);

            // 처리된 페이지 수와 PDF의 총 페이지 수 비교
            if (processedCount == totalPageCount) {
                FileInfo resultFileInfo = apiCaller.getFileByNameAndStatus(userId, basename + "-page1.jpg", "CS");
                if (resultFileInfo == null || resultFileInfo.getFilename() == null) {
                    resultFileInfo = apiCaller.getFileByNameAndStatus(userId, basename + "-page1.jpg", "CF");
                }

                if (resultFileInfo != null && resultFileInfo.getFilename() != null) {
                    JSONObject pdfJsonBody = new JSONObject();
                    pdfJsonBody.put("fileName", basename + ".pdf");
                    pdfJsonBody.put("pageNum", 0);
                    pdfJsonBody.put("userId", userId);
                    pdfJsonBody.put("ocrServiceType", resultFileInfo.getOcrServiceType());
                    pdfJsonBody.put("language", resultFileInfo.getLanguage());
                    pdfJsonBody.put("lClassification", resultFileInfo.getLClassification());
                    pdfJsonBody.put("mClassification", resultFileInfo.getMClassification());
                    pdfJsonBody.put("classificationStatus", resultFileInfo.getClassificationStatus());
                    pdfJsonBody.put("classificationStartDateTime", resultFileInfo.getClassificationStartDateTime());
                    pdfJsonBody.put("classificationEndDateTime", endDateTime);

                    apiCaller.callUpdateApi(pdfJsonBody);
                } else {
                    log.warn("page1 결과가 없어 PDF 업데이트를 진행하지 못했습니다: {}", basename);
                }
            } else {
                log.debug("아직 모든 페이지가 처리되지 않았습니다. PDF 업데이트 보류: {}", basename);
            }
        }

        // jpg, png, jpeg가 원본일 경우
        if (fileInfo.getPageNum() == 0 && imageExt != null && !imageExt.equals(".pdf")) {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("fileName", fileInfo.getFilename());
            jsonBody.put("pageNum", 0);
            jsonBody.put("userId", userId);
            jsonBody.put("ocrServiceType", fileInfo.getOcrServiceType());
            jsonBody.put("language", fileInfo.getLanguage());
            jsonBody.put("lClassification", getCountryName(fileInfo.getLanguage()));
            jsonBody.put("mClassification", values[2]);

            if (values[2].contains("미분류")) {
                jsonBody.put("classificationStatus", "CF"); // 미분류: CF
            } else {
                jsonBody.put("classificationStatus", "CS"); // 정상분류: CS
            }

            jsonBody.put("ocrResultFileName", name + "_result.dat");
            jsonBody.put("classificationResultFileName", name + "_result.txt");
            jsonBody.put("classificationStartDateTime", classificationStartDateTime);
            jsonBody.put("classificationEndDateTime", getCurrentTime());

            apiCaller.callUpdateApi(jsonBody);
        }


//        // 첫번째 페이지의 결과로 pdf의 결과도 업데이트
//        if (fileInfo.getPageNum() == 1) {
//            String basename = name.replaceAll("-page.*$", "");
//
//            JSONObject pdfJsonBody = new JSONObject();
//            pdfJsonBody.put("fileName", basename + ".pdf");
//            pdfJsonBody.put("pageNum", 0);
//            pdfJsonBody.put("userId", userId);
//            pdfJsonBody.put("ocrServiceType", fileInfo.getOcrServiceType());
//            pdfJsonBody.put("language", fileInfo.getLanguage());
//            pdfJsonBody.put("lClassification", getCountryName(fileInfo.getLanguage()));
//            pdfJsonBody.put("mClassification", values[2]);
//            if (values[2].contains("미분류")) {
//                pdfJsonBody.put("classificationStatus", "CF"); // 미분류: CF
//            } else {
//                pdfJsonBody.put("classificationStatus", "CS"); // 정상분류: CS
//            }
//            pdfJsonBody.put("classificationStartDateTime", classificationStartDateTime);
//            pdfJsonBody.put("classificationEndDateTime", getCurrentTime());
//
//            apiCaller.callUpdateApi(pdfJsonBody);
//        }

//        JSONObject jsonBody = new JSONObject();
//        jsonBody.put("fileName", fileInfo.getFilename());
//        jsonBody.put("pageNum", fileInfo.getPageNum());
//        jsonBody.put("userId", userId);
//        jsonBody.put("ocrServiceType", fileInfo.getOcrServiceType());
//        jsonBody.put("language", fileInfo.getLanguage());
//        jsonBody.put("lClassification", getCountryName(fileInfo.getLanguage()));
//        jsonBody.put("mClassification", values[2]);
//        if (values[2].contains("미분류")) {
//            jsonBody.put("classificationStatus", "CF"); // 미분류: CF
//        } else {
//            jsonBody.put("classificationStatus", "CS"); // 정상분류: CS
//        }
//        jsonBody.put("classificationStartDateTime", classificationStartDateTime);
//        jsonBody.put("classificationEndDateTime", getCurrentTime());
//
//        System.out.println("Template name : " + values[2]);
//
//        apiCaller.callUpdateApi(jsonBody);
    }

    public void jsonDataUpdateWithUnitFile(String taskName, String fileName, String[] values) throws UnirestException {
        log.info("update start: " + fileName);

        APICaller apiCaller = new APICaller();
        IOService ioService = new IOService();
        String userId = configLoader.apiUserId;
        String name = fileName.replace("_result", "");
        String name2 = name.replaceAll("-page\\d+$", "");
        String defaultName = new File(name).getName();
        String[] extensions = {".jpg", ".png", ".jpeg", ".JPG", ".PNG", ".JPEG"};
        FileInfo fileInfo = null;
        String imageExt = null;
        String officeExt = null;
        String apiTaskName = "";

        if (taskName.isEmpty() || taskName.equals("")) {
            apiTaskName = "DEFAULT";
        } else {
            apiTaskName = taskName;
        }
        log.info("taskName: " + apiTaskName);

        for (String ext : FileExtensionUtil.DA_SUPPORTED_EXT) {
            fileInfo = apiCaller.getFileByName(userId, name2 + "." + ext);
            if (fileInfo != null && fileInfo.getFilename() != null) {
                officeExt = ext;
                if (configLoader.usePdfExtractImage && officeExt.equals("pdf")) {
                    officeExt = null;
                }
                log.info("officeExt: " + officeExt);
                break; // 성공했으면 반복 종료
            }
        }

        if (officeExt == null) {
            for (String ext : extensions) {
                fileInfo = apiCaller.getFileByName(userId, name + ext);
                if (fileInfo != null && fileInfo.getFilename() != null) {
                    imageExt = ext;
                    log.info("imageExt: " + imageExt);
                    break; // 성공했으면 반복 종료
                }
            }
        }

        if (fileInfo == null || fileInfo.getFilename() == null) {
            log.warn("파일 정보가 없습니다. API 호출 생략: {}", name);
            return;
        }

        // 원본 파일이 아닌 경우 (jpg가 원본일 경우를 제외, pdf에서 추출된 page.jpg인 경우)
        if (fileInfo.getPageNum() != 0) {
            String basename = name.replaceAll("-page\\d+$", "");
            //int maxPage = ioService.getPdfPageCount(configLoader.resultFilePath + File.separator + values[2] + File.separator + basename + ".pdf");
            int maxPage = 0;

            String endDateTime = getCurrentTime();

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("fileName", fileInfo.getFilename());
            jsonBody.put("pageNum", fileInfo.getPageNum());
            jsonBody.put("userId", userId);
            jsonBody.put("ocrServiceType", fileInfo.getOcrServiceType());
            jsonBody.put("language", fileInfo.getLanguage());
            jsonBody.put("lClassification", getCountryName(fileInfo.getLanguage()));
            jsonBody.put("mClassification", values[2]);
            if (values[2].contains("미분류")) {
                if (configLoader.useUnclassifiedAsCS) {
                    jsonBody.put("classificationStatus", "CS");
                } else {
                    jsonBody.put("classificationStatus", "CF"); // 미분류: CF
                }
            } else {
                jsonBody.put("classificationStatus", "CS"); // 정상분류: CS
            }
            jsonBody.put("ocrResultFileName", defaultName + "_result.dat");
            jsonBody.put("classificationResultFileName", defaultName + "_result.txt");
            jsonBody.put("classificationStartDateTime", classificationStartDateTime);
            jsonBody.put("classificationEndDateTime", endDateTime);
            jsonBody.put("taskName", apiTaskName);

            apiCaller.callUpdateApi(jsonBody);

            File pdfResultPath;

            try {
                String fileNameOnly = new File(basename).getName();
                File pdfFile;
                if (configLoader.createClassifiedFolder) {
                    pdfFile = new File(configLoader.resultFilePath + File.separator + values[2] + File.separator + taskName + File.separator + basename + ".pdf");
                    //pdfFile = new File(configLoader.resultFilePath + File.separator + values[2] + File.separator + basename + ".pdf");
                } else {
                    pdfFile = new File(configLoader.resultFilePath + File.separator + taskName + File.separator + basename + ".pdf");
                    //pdfFile = new File(configLoader.resultFilePath + File.separator + basename + ".pdf");
                }
                if (pdfFile.exists()) {
                    pdfResultPath = pdfFile;
                    maxPage = ioService.getPdfPageCount(pdfFile.getAbsolutePath());
                } else {
                    log.debug("PDF 파일이 아직 존재하지 않음: {}", pdfFile.getAbsolutePath());
                    return;
                }
            } catch (Exception e) {
                pdfResultPath = null;
                log.error("PDF 페이지 수 조회 중 오류 발생", e);
                return;
            }

            // 마지막 페이지인 경우 원본 PDF 파일 업데이트 진행
            if (fileInfo.getPageNum() == maxPage) {
                FileInfo resultFileInfo = apiCaller.getFileByNameAndStatus(userId, basename + "-page1.jpg", "CS");
                if (resultFileInfo.getFilename() == null) {
                    resultFileInfo = apiCaller.getFileByNameAndStatus(userId, basename + "-page1.jpg", "CF");
                }

//                log.info("resultFileInfo의 basename : " + basename + "-page1.jpg");
//
//                log.info("userid : " + resultFileInfo.getUserId());
//                log.info("fileName : " + resultFileInfo.getFilename());
//                log.info("ocrServiceType : " + resultFileInfo.getOcrServiceType());
//                log.info("language : " + resultFileInfo.getLanguage());
//                log.info("lClassification : " + resultFileInfo.getLClassification());
//                log.info("mClassification : " + resultFileInfo.getMClassification());
//                log.info("classificationStatus : " + resultFileInfo.getClassificationStatus());
//                log.info("classificationStartDateTime : " + resultFileInfo.getClassificationStartDateTime());
//                log.info("classificationEndDateTime : " + resultFileInfo.getClassificationEndDateTime());

                String fileNameOnly = new File(basename).getName();

                FileInfo checkFileName = apiCaller.getFileByName(configLoader.apiUserId, basename + ".pdf");
                log.debug("basename: {}, checkFileName: {}", basename + ".pdf", checkFileName.getFilename());
                if (checkFileName.getFilename() == null || checkFileName.getFilename().isEmpty()) {
                    basename = basename.replace(File.separator, "/");
                    log.debug("checkFileName is null. 수정 basename: {}", basename);
                }

                JSONObject pdfJsonBody = new JSONObject();
                pdfJsonBody.put("fileName", basename + ".pdf");
                pdfJsonBody.put("pageNum", 0);
                pdfJsonBody.put("userId", userId);
                pdfJsonBody.put("ocrServiceType", resultFileInfo.getOcrServiceType());
                pdfJsonBody.put("language", resultFileInfo.getLanguage());
                pdfJsonBody.put("lClassification", resultFileInfo.getLClassification());
                pdfJsonBody.put("mClassification", resultFileInfo.getMClassification());
                pdfJsonBody.put("classificationStatus", resultFileInfo.getClassificationStatus());
                pdfJsonBody.put("classificationResultFileName", fileNameOnly + "_result.txt");
                pdfJsonBody.put("classificationStartDateTime", resultFileInfo.getClassificationStartDateTime());
                pdfJsonBody.put("classificationEndDateTime", endDateTime);
                pdfJsonBody.put("taskName", apiTaskName);

                apiCaller.callUpdateApi(pdfJsonBody);

                FileInfo originalFileInfo = apiCaller.getFileByNameAndPageNumAndStatusNotNull(userId, basename + ".pdf", "CS", 0);
                if (originalFileInfo.getFilename() == null) {
                    originalFileInfo = apiCaller.getFileByNameAndPageNumAndStatusNotNull(userId, basename + ".pdf", "CF", 0);
                }

                if (configLoader.useCallbackUpdate && configLoader.ocrServiceType.contains("da")) {
                    apiCaller.callbackApi(originalFileInfo, pdfResultPath.getPath(), 200, "완료");
                }
            }

            // 마지막 페이지일 때 원본 pdf 업데이트
//            int processedCount = getProcessedPageCount(userId, basename); // 처리된 페이지 수
//            int totalPageCount = ioService.getPdfPageCount(configLoader.resultFilePath + File.separator + values[2] + File.separator + basename + ".pdf"); // PDF의 총 페이지 수
//
//            log.info("{} 처리된 페이지 수: {}, PDF 총 페이지 수: {}", basename+".pdf", processedCount, totalPageCount);
//
//            // 처리된 페이지 수와 PDF의 총 페이지 수 비교
//            if (processedCount == totalPageCount) {
//                FileInfo resultFileInfo = apiCaller.getFileByNameAndStatus(userId, basename + "-page1.jpg", "CS");
//                if (resultFileInfo == null || resultFileInfo.getFilename() == null) {
//                    resultFileInfo = apiCaller.getFileByNameAndStatus(userId, basename + "-page1.jpg", "CF");
//                }
//
//                if (resultFileInfo != null && resultFileInfo.getFilename() != null) {
//                    JSONObject pdfJsonBody = new JSONObject();
//                    pdfJsonBody.put("fileName", basename + ".pdf");
//                    pdfJsonBody.put("pageNum", 0);
//                    pdfJsonBody.put("userId", userId);
//                    pdfJsonBody.put("ocrServiceType", resultFileInfo.getOcrServiceType());
//                    pdfJsonBody.put("language", resultFileInfo.getLanguage());
//                    pdfJsonBody.put("lClassification", resultFileInfo.getLClassification());
//                    pdfJsonBody.put("mClassification", resultFileInfo.getMClassification());
//                    pdfJsonBody.put("classificationStatus", resultFileInfo.getClassificationStatus());
//                    pdfJsonBody.put("classificationStartDateTime", resultFileInfo.getClassificationStartDateTime());
//                    pdfJsonBody.put("classificationEndDateTime", endDateTime);
//
//                    apiCaller.callUpdateApi(pdfJsonBody);
//                } else {
//                    log.warn("page1 결과가 없어 PDF 업데이트를 진행하지 못했습니다: {}", basename);
//                }
//            } else {
//                log.info("아직 모든 페이지가 처리되지 않았습니다. PDF 업데이트 보류: {}", basename);
//            }
        }

        // jpg, png, jpeg가 원본일 경우
        if (fileInfo.getPageNum() == 0 && imageExt != null && !imageExt.equals(".pdf")) {
            File imgResultPath;
            String fileNameOnly = new File(name).getName();
            if (configLoader.createClassifiedFolder) {
                imgResultPath = new File(configLoader.resultFilePath + File.separator + values[2] + File.separator + taskName + File.separator + name + imageExt);
            } else {
                imgResultPath = new File(configLoader.resultFilePath + File.separator + taskName + File.separator + name + imageExt);
            }

            if (!imgResultPath.exists()) {
                imgResultPath = null;
                log.debug("IMAGE 파일이 존재하지 않음: {}", imgResultPath.getAbsolutePath());
                return;
            }

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("fileName", fileInfo.getFilename());
            jsonBody.put("pageNum", 0);
            jsonBody.put("userId", userId);
            jsonBody.put("ocrServiceType", fileInfo.getOcrServiceType());
            jsonBody.put("language", fileInfo.getLanguage());
            jsonBody.put("lClassification", getCountryName(fileInfo.getLanguage()));
            jsonBody.put("mClassification", values[2]);

            if (values[2].contains("미분류")) {
                if (configLoader.useUnclassifiedAsCS) {
                    jsonBody.put("classificationStatus", "CS");
                } else {
                    jsonBody.put("classificationStatus", "CF"); // 미분류: CF
                }
            } else {
                jsonBody.put("classificationStatus", "CS"); // 정상분류: CS
            }

            jsonBody.put("ocrResultFileName", defaultName + "_result.dat");
            jsonBody.put("classificationResultFileName", defaultName + "_result.txt");
            jsonBody.put("classificationStartDateTime", classificationStartDateTime);
            jsonBody.put("classificationEndDateTime", getCurrentTime());
            jsonBody.put("taskName", apiTaskName);

            apiCaller.callUpdateApi(jsonBody);
            if (configLoader.useCallbackUpdate && configLoader.ocrServiceType.contains("da")) {
                apiCaller.callbackApi(fileInfo, imgResultPath.getPath(), 200, "완료");
            }
        }

        if (fileInfo.getPageNum() == 0 && officeExt != null && !officeExt.equals(".pdf")) {
            File officeResultPath;
            String fileNameOnly = new File(name).getName();
            if (configLoader.createClassifiedFolder) {
                officeResultPath = new File(configLoader.resultFilePath + File.separator + values[2] + File.separator + taskName + File.separator + name2 + "." + officeExt);
            } else {
                officeResultPath = new File(configLoader.resultFilePath + File.separator + taskName + File.separator + name2 + "." + officeExt);
            }
            log.info("offceResultPath: " + officeResultPath);

            if (!officeResultPath.exists()) {
                officeResultPath = null;
                log.debug("Office 파일이 존재하지 않음: {}", officeResultPath.getAbsolutePath());
                return;
            }

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("fileName", fileInfo.getFilename());
            jsonBody.put("pageNum", 0);
            jsonBody.put("userId", userId);
            jsonBody.put("ocrServiceType", fileInfo.getOcrServiceType());
            jsonBody.put("language", fileInfo.getLanguage());
            jsonBody.put("lClassification", getCountryName(fileInfo.getLanguage()));
            jsonBody.put("mClassification", values[2]);

            if (values[2].contains("미분류")) {
                if (configLoader.useUnclassifiedAsCS) {
                    jsonBody.put("classificationStatus", "CS");
                } else {
                    jsonBody.put("classificationStatus", "CF"); // 미분류: CF
                }
            } else {
                jsonBody.put("classificationStatus", "CS"); // 정상분류: CS
            }

            String defaultName2 = new File(name2).getName();

            jsonBody.put("classificationResultFileName", defaultName2 + "_result.txt");
            jsonBody.put("classificationStartDateTime", classificationStartDateTime);
            jsonBody.put("classificationEndDateTime", getCurrentTime());
            jsonBody.put("taskName", apiTaskName);

            apiCaller.callUpdateApi(jsonBody);
            if (configLoader.useCallbackUpdate && configLoader.ocrServiceType.contains("da")) {
                apiCaller.callbackApi(fileInfo, officeResultPath.getPath(), 200, "완료");
            }
        }
    }

    public int getProcessedPageCount(String userId, String basename) throws UnirestException {
        APICaller apiCaller = new APICaller();
        int count = 0;

        List<FileInfo> filesWithCS = apiCaller.getAllFilesWithStatus(userId, "CS");
        for (FileInfo file : filesWithCS) {
            if (file.getFilename().startsWith(basename + "-page")) {
                count++;
            }
        }

        List<FileInfo> filesWithCF = apiCaller.getAllFilesWithStatus(userId, "CF");
        for (FileInfo file : filesWithCF) {
            if (file.getFilename().startsWith(basename + "-page")) {
                count++;
            }
        }

        return count;
    }

    public void textFinalResult(String textSaveFilePath, String fileName, Map<String, Map<String, String>> finalResultByVersion, String version, String subVersion, Map<String, List<String>> finalCertificateResult) {
        String baseName = fileName.replace("_result", "");

        if (finalResultByVersion != null) {
            Map<String, String> valueList = finalResultByVersion.get(baseName);

            if (valueList != null) {
                String value = valueList.get(version); // 버전 별 결과 양식

                if (value != null) {
                    if (value.contains("미분류")) {
                        value = valueList.get(subVersion);
                    }

                    String[] values = value.split(Pattern.quote(File.separator));

                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(textSaveFilePath, true))) {
                        if (configLoader.ocrServiceType.equals("google")) {
                            writer.write("[결과]\n");
                            writer.write("국가: " + values[0] + "\n");
                            writer.write("언어: " + values[1] + "\n");
                            writer.write("문서 양식: " + values[2] + "\n");

                            if (values[2].contains("인증서")) {
                                List<String> certificateTypeList = finalCertificateResult.get(baseName);
                                String certificateType = certificateTypeList.isEmpty() ? "-" : String.join(", ", certificateTypeList);
                                writer.write("인증서 유형: " + certificateType);
                            }

                            log.info("Text final results completed. (Google OCR)");
                        } else if (configLoader.ocrServiceType.equals("synap")) {
                            writer.write("[결과]\n");
                            writer.write("대분류: " + "\n");
                            writer.write("언어: " + values[1] + "\n");
                            writer.write("중분류: " + values[2] + "\n");

                            log.info("Text final results completed. (Synap OCR)");
                        }
                    } catch (IOException e) {
                        log.warn("Text final results failed. {}", e.getMessage());
                    }

//                    // api 사용 시 update 진행
//                    // DocumentService의 createFinalResultFile()에서 moveFiles() 실행 후 UPDATE 하도록 순서 변경
//                    if (configLoader.apiUsageFlag) {
//                        try {
//                            jsonDataWithUpdate(fileName, values);
//                        } catch (Exception e) {
//                            log.info("update api failed. {}", e.getMessage());
//                        }
//                    }
                }
            }
        }
    }

    public void appendPageResultToMaster(String pageFileName) {
        // pageFileName 예: "파일명-page1_result"
        String pageFilePath = configLoader.resultFilePath + File.separator + pageFileName + ".txt";

        // masterFileName 예: "파일명_result"
        String masterFileName = pageFileName.contains("-page")
                ? pageFileName.substring(0, pageFileName.indexOf("-page")) + "_result"
                : pageFileName;

        String masterFilePath = configLoader.resultFilePath + File.separator + masterFileName + ".txt";

        File pageFile = new File(pageFilePath);
        File masterFile = new File(masterFilePath);

        if (!pageFile.exists()) {
            log.warn("Page result file not found: {}", pageFilePath);
            return;
        }

        boolean masterExists = masterFile.exists();

        try (
                BufferedReader reader = new BufferedReader(new FileReader(pageFile));
                BufferedWriter writer = new BufferedWriter(new FileWriter(masterFile, true)) // 이어쓰기
        ) {
            String firstLine = reader.readLine();

            if (firstLine != null) {
                if (!masterExists) {
                    writer.write("[결과] ");
                } else {
                    writer.newLine();
                    writer.write("-----------------------------------------------------");
                    writer.newLine();
                    writer.newLine();
                    writer.write("[다음 페이지] ");
                }
                writer.write(firstLine);
                writer.newLine();
            }

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            log.debug("Appended page result to master result file: {}", masterFileName);
        } catch (IOException e) {
            log.warn("Failed to append page result. {}", e.getMessage());
        }
    }

    public void appendMdResultToMaster(String pageFileName, boolean officeExtensionFlag) {
        // pageFileName 예: "파일명-page1_result"
        String pageFilePath = configLoader.resultFilePath + File.separator + pageFileName + ".dat";

        // masterFileName 예: "파일명_result"
        String masterFileName = pageFileName.contains("-page")
                ? pageFileName.substring(0, pageFileName.indexOf("-page")) + "_result"
                : pageFileName;

        String masterFilePath = configLoader.resultFilePath + File.separator + masterFileName + ".md";

        File pageFile = new File(pageFilePath);
        File masterFile = new File(masterFilePath);

        if (!pageFile.exists()) {
            log.warn("Page result file not found: {}", pageFilePath);
            return;
        }

        if (configLoader.useMdFileCreation) {
            // .jpg인 경우 (단일 페이지) md 파일 저장
            if (!pageFileName.contains("-page")) {
                try {
                    String content;
                    if (configLoader.encodingCheck) {
                        // 암호화된 .dat인 경우: 복호화
                        byte[] bytes = Files.readAllBytes(pageFile.toPath());
                        content = JsonService.aesDecode(bytes);
                    } else {
                        // 평문 .dat인 경우: UTF-8 그대로 읽기
                        content = Files.readString(pageFile.toPath(), StandardCharsets.UTF_8);
                    }

                    // master 파일 생성/덮어쓰기 (필요시 APPEND로 바꿀 수 있음)
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(masterFile, false))) {
                        writer.write(content);
                    }

                    log.info("Converted single-page .dat to .md: {}", masterFileName);
                } catch (Exception e) {
                    log.warn("Failed to convert single-page .dat to .md. {}", e.getMessage());
                }
                return; // 단일 페이지 처리 완료
            }

            if (officeExtensionFlag) {
                Path resultDir = Paths.get(configLoader.resultFilePath);
                String baseName = pageFileName.substring(0, pageFileName.indexOf("-page"));
                // 파일명 정규식: 파일명-pageNN_result.dat
                Pattern pat = Pattern.compile(Pattern.quote(baseName) + "-page(\\d+)_result\\.dat", Pattern.CASE_INSENSITIVE);

                // 결과 폴더에서 대상 페이지 파일 검색
                File dirFile = resultDir.toFile();
                File[] candidates = dirFile.listFiles((dir, name) -> pat.matcher(name).matches());

                if (candidates == null || candidates.length == 0) {
                    log.warn("No page result files found for base: {}", baseName);
                    return;
                }

                // 숫자 페이지 기준 정렬 (page1, page2, ... page10, page11)
                Arrays.sort(candidates, Comparator.comparingInt(f -> {
                    Matcher m = pat.matcher(f.getName());
                    if (m.matches()) {
                        return Integer.parseInt(m.group(1)); // "1", "2", ..."10"
                    }
                    return Integer.MAX_VALUE; // 안전장치
                }));

                boolean masterExists = Files.exists(Paths.get(masterFilePath));

                try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(masterFilePath), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

                    for (File file : candidates) {
                        // 각 페이지 파일 읽어서 기존 형식대로 작성
                        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                            String firstLine = reader.readLine();

                            if (firstLine != null) {
                                if (!masterExists) {
                                    writer.write(""); // 첫 생성 시(기존 로직 유지)
                                    masterExists = true; // 이후부터는 구분 라인 적용
                                } else {
                                    writer.newLine();
                                    writer.newLine();
                                    //writer.write("[다음 페이지] ");
                                }
                                writer.write(firstLine);
                                writer.newLine();
                            }

                            String line;
                            while ((line = reader.readLine()) != null) {
                                writer.write(line);
                                writer.newLine();
                            }
                        }
                    }
                    log.debug("Appended ordered page results to master md: {}", masterFileName);
                } catch (IOException e) {
                    log.warn("Failed to append ordered page results. {}", e.getMessage());
                }
                return; // office 처리 완료
            }

            // pdf인 경우 (멀티 페이지) md 파일 저장
            if (configLoader.usePdfExtractImage) {
                boolean masterExists = masterFile.exists();

                try (
                        BufferedReader reader = new BufferedReader(new FileReader(pageFile));
                        BufferedWriter writer = new BufferedWriter(new FileWriter(masterFile, true)) // 이어쓰기
                ) {
                    String firstLine = reader.readLine();

                    if (firstLine != null) {
                        if (!masterExists) {
                            writer.write("");
                        } else {
                            writer.newLine();
                            //writer.write("-----------------------------------------------------");
                            //writer.newLine();
                            writer.newLine();
                            //writer.write("[다음 페이지] ");
                        }
                        writer.write(firstLine);
                        writer.newLine();
                    }

                    String line;
                    while ((line = reader.readLine()) != null) {
                        writer.write(line);
                        writer.newLine();
                    }
                    log.info("Appended page result to master result file: {}", masterFileName);
                } catch (IOException e) {
                    log.warn("Failed to append page result. {}", e.getMessage());
                }
            }
        }
    }

    public void excelFinalResult(String saveFilePath, Map<String, Map<String, String>> finalResultByVersion, String version) {
        String baseName = fileName.replace("_result.xlsx", "");

        try (FileInputStream fis = new FileInputStream(saveFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // 첫 번째 시트를 가져옵니다
            int lastColumnIndex = getLastColumnIndex(sheet);
            //System.out.println("마지막 열의 인덱스: " + lastColumnIndex);

            if (finalResultByVersion != null) {
                Map<String, String> valueList = finalResultByVersion.get(baseName);

                if (valueList != null) {
                    String value = valueList.get(version); // 버전 별 결과 양식
                    if (value != null) {
                        String[] values = value.split("/"); // 쉼표로 구분

                        // 데이터를 추가할 행 번호
                        int rowNum = sheet.getLastRowNum() + 1;

                        // 각 행에 데이터를 추가하는 함수 호출
                        addRow(sheet, rowNum++, "최종 결과", null);
                        addRow(sheet, rowNum++, "국가", values.length > 0 ? values[0] : "");
                        addRow(sheet, rowNum++, "언어", values.length > 1 ? values[1] : "");
                        addRow(sheet, rowNum++, "양식", values.length > 2 ? values[2] : "");
                    }
                }
            }

            // 파일 저장
            try (FileOutputStream fos = new FileOutputStream(saveFilePath)) {
                workbook.write(fos);
                log.info("Excel final results completed.");
            }

        } catch (IOException e) {
            log.warn("Excel final results failed. {}", e.getMessage());
        }
    }

    private static int getLastColumnIndex(Sheet sheet) {
        int lastColumnIndex = -1;
        for (Row row : sheet) {
            if (row.getLastCellNum() > lastColumnIndex) {
                lastColumnIndex = row.getLastCellNum();
            }
        }
        return lastColumnIndex - 1; // 인덱스는 0부터 시작하므로 1을 뺍니다
    }
    private static void addRow(Sheet sheet, int rowNum, String firstCellValue, String secondCellValue) {
        Row row = sheet.createRow(rowNum);
        Cell cell1 = row.createCell(0);
        cell1.setCellValue(firstCellValue);
        if (secondCellValue != null) {
            Cell cell2 = row.createCell(1);
            cell2.setCellValue(secondCellValue);
        }
    }

    public void moveFiles(String resultFilePath, Map<String, Map<String, String>> resultByVersion, String version, String subVersion, String subPath, String taskName) throws InterruptedException, UnirestException {
        IOService ioService = new IOService();
        APICaller apiCaller = new APICaller();

//        Set<String> allowedOriginals =
//                (resultByVersion != null)
//                        ? resultByVersion.keySet().stream()
//                        .map(k -> k.replaceFirst("-page\\d+$", "")) // "…-pageN" 제거
//                        .collect(Collectors.toCollection(LinkedHashSet::new))
//                        : Collections.emptySet();
//        log.debug("[std] allowedOriginals: {}", allowedOriginals);

        File folder = Paths.get(resultFilePath).toFile();
        File[] listOfFiles = folder.listFiles((dir, name) -> name.endsWith(".dat"));

        if (listOfFiles != null || listOfFiles.length != 0) {
            for (File file : listOfFiles) {
                String fileName = file.getName();
                String baseName = fileName.replace("_result.dat", ""); // 파일 이름에서 확장자 제거
//                String origName = baseName.replaceFirst("-page\\d+$", "");
//
//                // 다른 배치 스킵
//                if (!allowedOriginals.contains(origName)) {
//                    log.debug("[std] skip other batch originalName: {}", origName);
//                    continue;
//                }

                if (resultByVersion != null) {
                    Map<String, String> valueList = resultByVersion.get(baseName);

                    if (valueList != null) {
                        String value = valueList.get(version); // 버전 별 결과 양식
                        if (value != null) {
                            if (value.contains("미분류")) {
                                value = valueList.get(subVersion);
                            }

                            Path targetDir;

                            if (configLoader.createClassifiedFolder) {
                                targetDir = Paths.get(resultFilePath, value, taskName, subPath);
                            } else {
                                targetDir = Paths.get(resultFilePath, taskName, subPath);
                            }

                            if (!Files.exists(targetDir)) {
                                try {
                                    Files.createDirectories(targetDir);
                                } catch (IOException e) {
                                    log.warn("Folder create failed");
                                    e.printStackTrace();
                                }
                            }

                            // 이동 api 사용에 따라 분기
                            if (!configLoader.apiUsageFlag) {
                                //if (!baseName.contains("-page")) { // pdfBaseName 으로 시작하는 모든 파일 이동 시
                                File[] relatedFiles = folder.listFiles((dir, name) -> name.startsWith(baseName));
                                if (relatedFiles != null) {
                                    for (File relatedFile : relatedFiles) {
                                        Path targetPath = targetDir.resolve(relatedFile.getName());
                                        try {
                                            Files.move(relatedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                                            log.debug("Moved file : '{}' to '{}'", relatedFile.getName(), targetPath);
                                        } catch (IOException e) {
                                            log.warn("'{}' File move failed : {}", relatedFile.getName(), e);
                                        }
                                    }
                                }
                                //}

                                // PDF 파일 이동 처리
                                Pattern pattern = Pattern.compile("(.+)-page\\d+$");
                                Matcher matcher = pattern.matcher(baseName);
                                //if (matcher.matches() && baseName.endsWith("-page1")) { // pdfBaseName 으로 시작하는 모든 파일 이동 시
                                if (matcher.matches()) {
                                    String pdfBaseName = matcher.group(1); // -page?를 제외한 이름

                                    //구버전
                                    // pdf 원본 파일만 이동
                                    File[] pdfFiles = folder.listFiles((dir, name) -> name.startsWith(pdfBaseName) && (name.toLowerCase().matches(".*\\.(pdf)$")));

                                    // pdfBaseName 으로 시작하는 모든 파일 (pdf 원본, 추출 이미지, 결과) 전체 이동
                                    //File[] pdfFiles = folder.listFiles((dir, name) -> name.startsWith(pdfBaseName));
                                    if (pdfFiles != null) {
                                        for (File pdfFile : pdfFiles) {
                                            Path targetPath = targetDir.resolve(pdfFile.getName());
                                            try {
                                                Files.copy(pdfFile.toPath(), targetPath);
                                                log.debug("Moved PDF file : '{}' to '{}'", pdfFile.getName(), targetPath);

                                                // 복사 성공 후 원본 파일 삭제
                                                Files.delete(pdfFile.toPath());
                                                if (Files.exists(Paths.get(configLoader.imageFolderPath, pdfFile.getName()))) {
                                                    Files.delete(Paths.get(configLoader.imageFolderPath, pdfFile.getName()));
                                                    log.info("Deleted image folder path PDF file");
                                                } else {
                                                    log.info("image folder path PDF file Not exist");
                                                }
                                                log.info("Deleted original PDF file : '{}'", "path: " + pdfFile.toPath() + ", name: " +pdfFile.getName());
                                            } catch (IOException e) {
                                                log.warn("'{}' PDF file move failed : {}", pdfFile.getName(), e);
                                            }
                                        }
                                    }
                                }
                            } else {
                                String[] expectedSuffixes = {
                                        ".jpg",
                                        ".png",
                                        ".jpeg",
                                        "_result.dat",
                                        "_result.txt",
                                        "_result.xlsx",
                                        "_result.zip"
                                };

                                for (String suffix : expectedSuffixes) {
                                    String expectedFileName = baseName + suffix;
                                    File sourceFile = Paths.get(folder.getAbsolutePath(), expectedFileName).toFile();

                                    if (sourceFile.exists()) {
                                        Path targetPath = targetDir.resolve(expectedFileName);
                                        try {
                                            Files.move(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                                            log.debug("Moved file : '{}' to '{}'", expectedFileName, targetPath);
                                        } catch (IOException e) {
                                            log.info("'{}' File move failed : {}", expectedFileName, e);
                                        }
                                    } else {
                                        log.debug("File not found, skipping: {}", expectedFileName);
                                    }
                                }

                                // CSV 파일들 (_result.csv, _result_2.csv, _result_3.csv 등) 이동 처리
                                File[] resultFiles = folder.listFiles((dir, name) -> {
                                    boolean matchCsv = name.matches(Pattern.quote(baseName) + "_result(_\\d+)?\\.csv");
                                    boolean matchHtml = name.matches(Pattern.quote(baseName) + "_result(_\\d+)?\\.html");
                                    boolean match = matchCsv || matchHtml;
                                    if (match) {
                                        log.info("Matched result(CSV/HTML) file: {}", name);
                                    }
                                    return match;
                                });

                                if (resultFiles == null) {
                                    log.warn("Result(CSV/HTML) file list is null. Check folder path or permissions.");
                                } else {
                                    log.debug("Found {} matching result(CSV/HTML) files", resultFiles.length);
                                }

                                if (resultFiles != null) {
                                    for (File resultFile : resultFiles) {
                                        Path targetPath = targetDir.resolve(resultFile.getName());
                                        try {
                                            Files.move(resultFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                                            log.debug("Moved result(CSV/HTML) file : '{}' to '{}'", resultFile.getName(), targetPath);
                                        } catch (IOException e) {
                                            log.warn("Result(CSV/HTML) file move failed : '{}', {}", resultFile.getName(), e.getMessage());
                                        }
                                    }
                                }

                                // ZIP 파일에서 png 파일 추출 후 ZIP 삭제
                                String zipFileName = baseName + "_result.zip";
                                //File zipFilePath = Paths.get(folder.getAbsolutePath(), zipFileName).toFile();
                                File zipFilePath;
                                if (configLoader.createClassifiedFolder) {
                                    zipFilePath = Paths.get(folder.getAbsolutePath(), value, taskName, subPath, zipFileName).toFile();
                                } else {
                                    zipFilePath = Paths.get(folder.getAbsolutePath(), taskName, subPath, zipFileName).toFile();
                                }
                                log.debug("zipFileName: {}, zipFilePath: {}, targetDir: {}", zipFileName, zipFilePath, targetDir.toFile());

                                if (zipFilePath.exists() && zipFilePath.isFile()) {
                                    log.debug("ZIP file found. Extracting PNG… -> {}", zipFilePath.getAbsolutePath());

                                    String originalName = baseName.replaceAll("-page\\d+$", "");
                                    FileInfo fileInfo = apiCaller.getFileByName(configLoader.apiUserId, subPath + baseName + ".jpg");
                                    String uid = fileInfo.getGroupUID();
                                    if (uid == null || uid.length() < 8) {
                                        throw new IllegalArgumentException("groupUID must be at least 8 characters: " + uid);
                                    }
                                    String groupUID = uid.substring(0, 8);

                                    extractPNGFromZIP(zipFilePath, targetDir.toFile(), groupUID);
                                } else {
                                    log.warn("ZIP file not found: {}", zipFilePath.getAbsolutePath());
                                }

                                // PDF 및 전체 결과 TXT 파일 이동 처리
                                try {
                                    String originalName = baseName.replaceAll("-page\\d+$", "");
                                    int maxPage = ioService.getPdfPageCount(configLoader.resultFilePath + File.separator + originalName + ".pdf");
                                    FileInfo fileInfo = apiCaller.getFileByName(configLoader.apiUserId, subPath + baseName + ".jpg");

                                    if (fileInfo == null) {
                                        log.info("fileInfo 조회 불가로 PDF 이동을 위한 마지막 페이지 이동 여부 체크 불가");
                                        return;
                                    }

                                    if (maxPage == fileInfo.getPageNum()) {
                                        File pdfFile = Paths.get(configLoader.resultFilePath, originalName + ".pdf").toFile();
                                        File txtFile = Paths.get(configLoader.resultFilePath, originalName + "_result.txt").toFile();

                                        if (pdfFile.exists()) {
                                            Path targetPath = targetDir.resolve(pdfFile.getName());

                                            try {
                                                Files.move(pdfFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                                                log.debug("Moved PDF file : '{}' to '{}'", pdfFile.getName(), targetPath);

                                                // 복사 성공 후 원본 파일 삭제
                                                //Files.delete(pdfFile.toPath());
                                                //log.info("Deleted original PDF file : '{}'", pdfFile.getName());

                                                // 소스 경로 원본 파일 삭제
                                                if (configLoader.useSourceDeletion) {
                                                    if (Files.exists(Paths.get(configLoader.imageFolderPath, pdfFile.getName()))) {
                                                        Files.delete(Paths.get(configLoader.imageFolderPath, pdfFile.getName()));
                                                        log.info("Deleted image folder path PDF file");
                                                    } else {
                                                        log.info("image folder path PDF file Not exist");
                                                    }
                                                }
                                            } catch (IOException e) {
                                                log.warn("'{}' PDF file move failed : {}", pdfFile.getName(), e);
                                            }
                                        } else {
                                            log.warn("PDF file not found: '{}'", pdfFile.getAbsolutePath());
                                        }

                                        // 전체 결과 TXT 이동
                                        if (txtFile.exists()) {
                                            Path targetPath = targetDir.resolve(txtFile.getName());

                                            try {
                                                Files.move(txtFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                                                log.debug("Moved TXT file : '{}' to '{}'", txtFile.getName(), targetPath);
                                            } catch (IOException e) {
                                                log.warn("'{}' TXT file move failed : {}", txtFile.getName(), e);
                                            }
                                        } else {
                                            log.warn("TXT file not found: '{}'", txtFile.getAbsolutePath());
                                        }

                                        if (configLoader.useMdFileCreation) {
                                            File mdFile = Paths.get(configLoader.resultFilePath, originalName + "_result.md").toFile();

                                            // 전체 결과 MD 이동
                                            if (mdFile.exists()) {
                                                Path targetPath = targetDir.resolve(mdFile.getName());

                                                try {
                                                    // 파일 이동
                                                    Files.move(mdFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                                                    log.debug("Moved MD file : '{}' to '{}'", mdFile.getName(), targetPath);

                                                    // md 파일 내용 수정 (이미지 파일 태그의 이미지 파일명에 groupUID 추가)
                                                    String uid = fileInfo.getGroupUID();
                                                    if (uid == null || uid.length() < 8) {
                                                        throw new IllegalArgumentException("groupUID must be at least 8 characters: " + uid);
                                                    }

                                                    String groupUID = uid.substring(0, 8);

                                                    String original = Files.readString(targetPath, StandardCharsets.UTF_8);
                                                    String replaced = original.replace("](", "](" + groupUID + configLoader.resultFileNamingRule);
                                                    Files.writeString(targetPath, replaced, StandardCharsets.UTF_8);

                                                    // md 파일명 수정 (기존 파일명 앞에 groupUID 추가)
                                                    String oldName = targetPath.getFileName().toString();
                                                    String newName = groupUID + configLoader.resultFileNamingRule + oldName;

                                                    Path parent = targetPath.getParent();
                                                    Path newPath = (parent == null) ? Path.of(newName) : parent.resolve(newName);
                                                    Files.move(targetPath, newPath, StandardCopyOption.REPLACE_EXISTING); // 파일명 변경
                                                } catch (IOException e) {
                                                    log.warn("'{}' MD file move failed : {}", mdFile.getName(), e);
                                                }
                                            } else {
                                                log.warn("MD file not found: '{}'", mdFile.getAbsolutePath());
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("Move Files - PDF 파일 정보 조회 실패: {}", e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        }

        Thread.sleep(200);

        // 모든 파일을 확인한 후, 폴더를 제외하고 남아있는 PDF 파일 삭제
        if (!configLoader.apiUsageFlag) {
            File[] remainingFiles = folder.listFiles();
            if (remainingFiles != null) {
                for (File remainingFile : remainingFiles) {
                    if (remainingFile.isFile() && remainingFile.getName().toLowerCase().matches(".*\\.(pdf|jpg|png|jpeg)$")) {
                        try {
                            Files.delete(remainingFile.toPath());
                            log.info("Deleted PDF or IMG file : '{}'", remainingFile.getName());
                        } catch (IOException e) {
                            log.info("'{}' PDF file or IMG delete failed : {}", remainingFile.getName(), e);
                        }
                    }
                }
            }
        } else {
            if (configLoader.useSourceDeletion) {
                File imageFolder = new File(configLoader.imageFolderPath);
                if (imageFolder.exists() && imageFolder.isDirectory()) {
                    for (File file : listOfFiles) {
                        String baseName = file.getName().replace("_result.dat", "");

                        File[] imageFilesToDelete = imageFolder.listFiles((dir, name) ->
                                name.startsWith(baseName) &&
                                        name.toLowerCase().matches(".*\\.(pdf|jpg|png|jpeg)$")
                        );

                        if (imageFilesToDelete != null) {
                            for (File imageFile : imageFilesToDelete) {
                                try {
                                    if (Files.exists(imageFile.toPath())) {
                                        Files.delete(imageFile.toPath());
                                        log.info("Deleted image file from imageFolderPath: '{}'", imageFile.getName());
                                    }
                                } catch (IOException e) {
                                    log.info("Failed to delete image file '{}': {}", imageFile.getName(), e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    // 오피스 확장자(hwp 등) 전용: .dat/.zip이 늦게 생성되어도 이번 배치 키 기반으로 안전하게 처리
    public void moveFilesForOffice(String resultFilePath,
                                   Map<String, Map<String, String>> resultByVersion,
                                   String version,
                                   String subVersion,
                                   String subPath,
                                   String taskName) throws InterruptedException {

        APICaller apiCaller = new APICaller();
        File folder = Paths.get(resultFilePath).toFile();

        // 이번 배치에서 생성된 baseName 집합
        Set<String> baseNames = (resultByVersion != null) ? resultByVersion.keySet() : Collections.emptySet();
        if (baseNames.isEmpty()) {
            log.warn("[officeExt] resultByVersion is empty. Nothing to move.");
            return;
        }

        // 같은 originalName을 여러 page에서 중복 처리하지 않도록 방지
        Set<String> processedOriginals = new HashSet<>();

        for (String baseName : baseNames) {
            // baseName 예: "25123108_TEST_02-page1" → "25123108_TEST_02"
            String originalName = baseName.replaceFirst("-page\\d+$", "");
            if (!processedOriginals.add(originalName)) {
                log.debug("[officeExt] already processed originalName: {}", originalName);
                continue;
            }

            // 분류 값(value) 결정: originalName으로 우선 찾고, 없으면 baseName으로 찾기
            Map<String, String> valueList =
                    (resultByVersion.get(originalName) != null)
                            ? resultByVersion.get(originalName)
                            : resultByVersion.get(baseName);

            String value = null;
            if (valueList != null) {
                value = valueList.get(version);
                if (value != null && value.contains("미분류")) {
                    value = valueList.get(subVersion);
                }
            }

            // 대상 폴더 결정 및 생성
            Path targetDir = computeTargetDir(resultFilePath, value, taskName, subPath);
            ensureDir(targetDir);

            // 이번 배치 파일만 선별: originalName + "_result..." 또는 originalName + "-page..."
            File[] toMove = folder.listFiles((dir, name) -> {
                File f = new File(dir, name);
                if (!f.isFile()) return false;

                boolean belongsToCurrentBatch =
                        name.startsWith(originalName + "_result") ||
                                name.startsWith(originalName + "-page");

                return belongsToCurrentBatch;
            });

            String officeExt = null;
            FileInfo fileInfo = null;

            for (String ext : FileExtensionUtil.DA_SUPPORTED_EXT) {
                try {
                    fileInfo = apiCaller.getFileByName(configLoader.apiUserId, subPath + originalName + "." + ext);
                    if (fileInfo != null && fileInfo.getFilename() != null) {
                        officeExt = ext;
                        log.debug("officeExt: " + officeExt);
                        break; // 성공했으면 반복 종료
                    }
                } catch (Exception e) {
                    log.info("ofiice 확장자 찾기 실패");
                }
            }

            // md 파일 내용 수정 (이미지 파일 태그의 이미지 파일명에 groupUID 추가)
            String uid = fileInfo.getGroupUID();
            if (uid == null || uid.length() < 8) {
                throw new IllegalArgumentException("groupUID must be at least 8 characters: " + uid);
            }

            String groupUID = uid.substring(0, 8);

            moveFilesIntoTarget(toMove, targetDir);
            extractZipIfExists(targetDir, originalName, groupUID);
            moveOfficeOriginalIfExists(originalName, targetDir);
            if (configLoader.useSourceDeletion) {
                deleteSourceOriginalIfMatch(originalName); // 원본(hwp 등) 삭제
            }

            if (configLoader.useMdFileCreation) {
                for (File file : toMove) {
                    if (file.getName().endsWith(".md")) {
                        Path movedMdPath = targetDir.resolve(file.getName());
                        File mdFile = movedMdPath.toFile();

                        if (mdFile.exists()) {
                            Path targetPath = targetDir.resolve(mdFile.getName());

                            try {
                                // md 파일 내용 수정 (이미지 파일 태그의 이미지 파일명에 groupUID 추가)
                                String original = Files.readString(targetPath, StandardCharsets.UTF_8);
                                String replaced = original.replace("](", "](" + groupUID + configLoader.resultFileNamingRule);
                                Files.writeString(targetPath, replaced, StandardCharsets.UTF_8);

                                // md 파일명 수정 (기존 파일명 앞에 groupUID 추가)
                                String oldName = targetPath.getFileName().toString();
                                String newName = groupUID + configLoader.resultFileNamingRule + oldName;

                                Path parent = targetPath.getParent();
                                Path newPath = (parent == null) ? Path.of(newName) : parent.resolve(newName);
                                Files.move(targetPath, newPath, StandardCopyOption.REPLACE_EXISTING); // 파일명 변경
                            } catch (IOException e) {
                                log.warn("'{}' MD file move failed : {}", mdFile.getName(), e);
                            }
                        } else {
                            log.warn("MD file not found: '{}'", mdFile.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }

    private Path computeTargetDir(String resultFilePath, String value, String taskName, String subPath) {
        if (configLoader.createClassifiedFolder) {
            String group = (value != null && !value.isBlank()) ? value : "미분류";
            return Paths.get(resultFilePath, group, taskName, subPath);
        } else {
            return Paths.get(resultFilePath, taskName, subPath);
        }
    }

    private void ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.warn("Failed to create dir '{}': {}", dir, e.getMessage());
        }
    }

    private void moveFilesIntoTarget(File[] toMove, Path targetDir) {
        if (toMove == null || toMove.length == 0) {
            log.debug("No files to move into '{}'", targetDir);
            return;
        }
        log.info("Found {} files to move into '{}'", toMove.length, targetDir);
        for (File src : toMove) {
            Path targetPath = targetDir.resolve(src.getName());
            try {
                Files.move(src.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                log.debug("Moved '{}' → '{}'", src.getName(), targetPath);
            } catch (IOException e) {
                log.warn("Move failed: '{}' → '{}': {}", src.getAbsolutePath(), targetPath, e.getMessage());
            }
        }
    }

    private void extractZipIfExists(Path targetDir, String originalName, String groupUID) {
        File[] zipFilesInTarget = targetDir.toFile().listFiles((dir, name) ->
                name.toLowerCase().endsWith(".zip") && name.startsWith(originalName + "_result"));

        if (zipFilesInTarget != null && zipFilesInTarget.length > 0) {
            for (File zip : zipFilesInTarget) {
                log.info("[officeExt] Extract PNG from ZIP: {}", zip.getAbsolutePath());
                try {
                    extractPNGFromZIP(zip, targetDir.toFile(), groupUID);
                    // 필요 시 ZIP 삭제:
                    // Files.deleteIfExists(zip.toPath());
                } catch (Exception ex) {
                    log.warn("[officeExt] ZIP extract failed: '{}': {}", zip.getAbsolutePath(), ex.getMessage());
                }
            }
        } else {
            log.debug("[officeExt] No ZIP found in target dir for '{}'", originalName);
        }
    }

    // 이미지 폴더(configLoader.imageFolderPath)에서 원본 파일(예: 25123108_TEST_02.hwp)을 찾아 삭제
    private void deleteSourceOriginalIfMatch(String originalName) {
        File imageDir = new File(configLoader.imageFolderPath);

        File[] files = imageDir.listFiles(f ->
                f.isFile() &&
                        // originalName + "." + ext 형태만 일치
                        f.getName().startsWith(originalName + ".") &&
                        FileExtensionUtil.DA_SUPPORTED_EXT.contains(FileExtensionUtil.getExtension(f.getName()))
        );

        if (files != null) {
            for (File f : files) {
                try {
                    Files.delete(f.toPath());
                    log.info("Deleted source file: {}", f.getName());
                } catch (Exception e) {
                    log.warn("Failed to delete: {}", f.getName());
                }
            }
        }
    }


    private void moveOfficeOriginalIfExists(String originalName, Path targetDir) {
        // resultFilePath에서 먼저 찾기 → 없으면 imageFolderPath에서 찾기
        File src = null;

        for (String ext : FileExtensionUtil.DA_SUPPORTED_EXT) {
            File cand = Paths.get(configLoader.resultFilePath, originalName + "." + ext).toFile();
            if (cand.exists()) { src = cand; break; }
        }
        if (src == null) {
            for (String ext : FileExtensionUtil.DA_SUPPORTED_EXT) {
                File cand = Paths.get(configLoader.imageFolderPath, originalName + "." + ext).toFile();
                if (cand.exists()) { src = cand; break; }
            }
        }
        if (src == null) {
            log.debug("[officeExt] original not found in result/image folder: {}", originalName);
            return;
        }

        Path targetPath = targetDir.resolve(src.getName());
        try {
            Files.move(src.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("[officeExt] Moved original file : '{}' → '{}'", src.getAbsolutePath(), targetPath);
        } catch (IOException e) {
            log.warn("[officeExt] Original move failed: '{}' → '{}': {}", src.getAbsolutePath(), targetPath, e.getMessage());
        }
    }

    public void extractPNGFromZIP(File zipFile, File targetDir, String groupUID) {
        targetDir.mkdirs();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buf = new byte[4096];

            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".png")) {

                    // ZIP 내부 경로 제거하고 파일명만 추출
                    String name = entry.getName().substring(entry.getName().lastIndexOf('/') + 1);
                    String newName = groupUID + configLoader.resultFileNamingRule + name;

                    File outFile = new File(targetDir, newName);
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        int n;
                        while ((n = zis.read(buf)) > 0) {
                            fos.write(buf, 0, n);
                        }
                    }
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ZIP 삭제
        zipFile.delete();
    }

    public void deleteFileList() {
        File folder = new File(configLoader.resultFilePath);
        File[] remainingFiles = folder.listFiles();
        if (remainingFiles != null) {
            for (File remainingFile : remainingFiles) {
                if (remainingFile.isFile() && remainingFile.getName().toLowerCase().matches(".*\\.(pdf|jpg|png|jpeg)$")) {
                    try {
                        Files.delete(remainingFile.toPath());
                        log.info("Deleted PDF or IMG file : '{}'", remainingFile.getName());
                    } catch (IOException e) {
                        log.info("'{}' PDF file or IMG delete failed : {}", remainingFile.getName(), e);
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

    // 현재 시간 가져오기
    public String getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        String formattedDateTime = now.format(formatter);

        //System.out.println(formattedDateTime);
        return formattedDateTime;
    }

    public static String getCountryName(String languageCode) {
        switch (languageCode.toUpperCase()) {
            case "FR":
                return "프랑스";
            case "US":
                return "미국";
            case "IT":
                return "이탈리아";
            case "VN":
                return "베트남";
            case "JP":
                return "일본";
            case "CN":
                return "중국";
            case "KR":
                return "한국";
            default:
                return "알 수 없는 국가";
        }
    }

}

