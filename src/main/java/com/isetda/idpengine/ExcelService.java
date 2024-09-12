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

    private ConfigLoader configLoader = ConfigLoader.getInstance();
    String excelFilePath = configLoader.getExcelFilePath();
//    boolean dbDataUsageFlag = configLoader.isDbDataUsageFlag();

    public String resultFolderPath;

    public File[] jsonFiles;
    public List<List<String>> resultList; // 각 변수로
    public List<List<String>> resultWord;
    public String docType="";

    public String fileName;

    public List<String> documentType = new ArrayList<>();

    // 폴더에서 JSON 파일 가져오기
    public void getFilteredJsonFiles() {
        File folder = new File(resultFolderPath);

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

    }


    // 엑셀의 단어 리스트 가져오기
    public Map<String, List<List<String[]>>> getExcelData() {
        Map<String, List<List<String[]>>> excelData = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(excelFilePath);
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
    String folderPath = configLoader.getResultFilePath();
    String jsonFolderPath = configLoader.getResultFilePath();
    // 폴더의 모든 파일(json)을 반복 (JSON Object로 저장 및 split, classifyDocuments 메소드로 분류 진행) (iterateFiles)
    public void createFinalResultFile() throws IOException {
        Map<String, List<List<String[]>>> excelData = getExcelData();

        int cnt = 1;
        for (File curFile : jsonFiles) {
            log.info("{}번째 JSON 파일 작업 시작", cnt);
            // 각 파일 JSON Object로 저장
            String jsonFilePath = curFile.getPath();

            fileName = curFile.getName().substring(0, curFile.getName().lastIndexOf("."));
            String saveFilePath = resultFolderPath + "\\" + fileName + ".xlsx";

            JsonService jsonService = new JsonService(jsonFilePath);

            StringBuilder allWords = new StringBuilder();

            for (Map<String, Object> item : jsonService.jsonCollection) {
                allWords.append(item.get("description"));
            }

            classifyDocuments(excelData, jsonService.jsonLocal, allWords.toString());
            log.info("문서 타입 55 :{}",docType);
            log.info("문서 타입 54 :{}",documentType);
            log.info("문서 타입 56 :{}",resultList);

            JsonService.processMarking(folderPath,jsonFolderPath,docType);

            try {
                createExcel(saveFilePath);
            } catch (IOException e) {
                log.error("엑셀 파일 생성 실패: {}", e.getStackTrace()[0]);
            }
            cnt++;
        }
    }

    // 엑셀 데이터와 비교하여 국가 및 양식 분류
    public void classifyDocuments(Map<String, List<List<String[]>>> excelData, String jsonLocale, String jsonDescription) {
        List<List<String[]>> targetSheetData = excelData.get(jsonLocale);

        if (targetSheetData == null) {
            // 일치하는 시트가 없을 경우
            log.info("일치 시트(국가) 없음");
        }

        int maxMatches = 0;
        int matchIndex = -1;

        double maxWeight = 0;
        int weightIndex = -1;

        resultList = new ArrayList<>();
        resultWord = new ArrayList<>();

        // null 처리
        for (int col = 0; col < targetSheetData.size(); col++) {
            List<String[]> columnData = targetSheetData.get(col);

            double addWeight = 0;

            int matches = 0;
            List<String> matchingValues = new ArrayList<>();

            matchingValues.add(columnData.getFirst()[0]);

            int cnt = 0;
            for (int i = 1; i < columnData.size(); i++) {
                String value = columnData.get(i)[0];
                if (jsonDescription.contains(value)) {
                    try {
                        addWeight += Double.parseDouble(columnData.get(i)[1]);
                    } catch (Exception e) {
                        log.error("가중치 계산 실패: {}", e.getStackTrace()[0]);
                    }

                    matches++;
                    cnt = countOccurrences(jsonDescription, value);
                    matchingValues.add(value + "(" + cnt + ")");
                } else {
                    cnt = 0;
                }
                log.info("'{}' - 일치 횟수: {}, 가중치: {}", value, cnt, Double.parseDouble(columnData.get(i)[1]));
            }

            //log.info("{} / {} = {}", addWeight, matches, addWeight / matches);
            double weight = (double) Math.round(addWeight / matches * 10) / 10;

            log.info("'{}' 양식 - 매치된 단어 수: {}/{}", columnData.get(0)[0], matches, columnData.size() - 1);
            log.info("'{}' 양식 - 가중치 평균 값: {}", columnData.get(0)[0], weight);
            log.info("'{}' 양식 - 매치 결과: {}", columnData.get(0)[0], matchingValues.subList(1, matchingValues.size()));


            matchingValues.add(matches + ""); // 매치 단어 수 결과 리스트에 추가
            resultWord.add(matchingValues);

            if (matches > maxMatches) {
                maxMatches = matches;
                matchIndex = col;
            }

            if (weight > maxWeight) {
                maxWeight = weight;
                weightIndex = col;
            }
        }

        if (matchIndex == weightIndex) {
            log.info("단어 매치 결과와 가중치 비교 결과 일치");
        } else {
            log.info("단어 매치 결과와 가중치 비교 결과 불일치");
        }


        List<String> countryType = new ArrayList<>();
        countryType.add("국가");
        countryType.add(jsonLocale);
        resultList.add(countryType);

        List<String> documentType = new ArrayList<>();
        documentType.add("문서 양식");

        if (matchIndex == -1 || weightIndex == -1) {
            log.info("미분류 파일: {}", jsonDescription);
            documentType.add("미분류");
        } else {
            log.info("문서 분류 결과: 국가코드({}), 문서양식({}), 가중치({})", jsonLocale, targetSheetData.get(matchIndex).get(0)[0], maxWeight);
            documentType.add(targetSheetData.get(matchIndex).get(0)[0]);
        }
        resultList.add(documentType);
        log.info("엑셀 데이터 결과 : {}",resultList);
        docType = resultList.get(1).get(1);



    }
    //</editor-fold>

    // 단어 카운트
    public static int countOccurrences(String input, String word) {
        int count = 0;
        int index = input.indexOf(word);
        while (index != -1) {
            count++;
            index = input.indexOf(word, index + 1);
        }
        return count;
    }

    // 엑셀에 결과 값 쓰기
    public void createExcel(String saveFilePath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");

        // Writing resultList
        for (int i = 0; i < resultList.size(); i++) {
            Row row = sheet.createRow(i);
            for (int j = 0; j < resultList.get(i).size(); j++) {
                Cell cell = row.createCell(j);
                cell.setCellValue(resultList.get(i).get(j));
                log.info("안녕 11 :{}  : {}",cell);
            }

            if (i == resultList.size() - 1) {
                // Writing resultWord
                int colNum = 2; // 3열부터 시작 (C열에 해당)

                for (List<String> rowData : resultWord) {
                    Cell cell = row.createCell(colNum); // 2열에 해당

                    StringBuilder cellValue = new StringBuilder(rowData.getFirst());
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
                    cell2.setCellValue(rowData.getFirst() + " (" + rowData.getLast() + ")");
                    colNum++;
                }
            }
        }
        Row thirdRow = sheet.createRow(2); // Create the 3rd row (index 2)
        Cell fileNameCell = thirdRow.createCell(0); // A열에 해당
        fileNameCell.setCellValue("파일이름: ");
        Cell fileNameCell2 = thirdRow.createCell(1);
        fileNameCell2.setCellValue(fileName.replace("_OCR_result",""));// A열에 해당


        try (FileOutputStream fileOut = new FileOutputStream(saveFilePath)) {
            workbook.write(fileOut);
            log.info("엑셀 파일 생성 완료: {} ", saveFilePath);
        }
        workbook.close();
    }

}

