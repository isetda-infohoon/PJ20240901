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

    public List<String> documentType = new ArrayList<>();
    public Map<String, List<List<String[]>>> excelData;
    public Map<String, List<List<String[]>>> jsonData;

    // 폴더에서 JSON 파일 가져오기
    public void getFilteredJsonFiles() {
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


    public String getCountryFromSheetName(String sheetName) {
        switch (sheetName) {
            case "en":
                return "미국";
            case "zh":
                return "중국";
            case "ja":
                return "일본";
            case "fr":
                return "프랑스";
            case "vn":
                return "베트남";
            case "it":
                return "이탈리아";
            default:
                return "존재하지 않는 국가 코드";
        }
    }

    // 폴더의 모든 파일(json)을 반복 (JSON Object로 저장 및 split, classifyDocuments 메소드로 분류 진행) (iterateFiles)
    public void createFinalResultFile() throws IOException {
        excelData = getExcelData();
        IMGService imgService = new IMGService();

        //JsonService jsonService = new JsonService("C:\\Users\\suaah\\OneDrive\\바탕 화면\\test-folder\\result\\식품안전관리 인증서_OCR_result.json");
        jsonData = JsonService.getJsonDictionary();

        int cnt = 1;
        for (File curFile : jsonFiles) {
            log.info("{}번째 JSON 파일 작업 시작", cnt);
            // 각 파일 JSON Object로 저장
            String jsonFilePath = curFile.getPath();

            fileName = curFile.getName().substring(0, curFile.getName().lastIndexOf("."));
            String saveFilePath = configLoader.resultFilePath + "\\" + fileName + ".xlsx";

            JsonService jsonService = new JsonService(jsonFilePath);

            StringBuilder allWords = new StringBuilder();

            for (Map<String, Object> item : jsonService.jsonCollection) {
                //TODO : Matching1 - 단어 리스트 합치지 않고 jsonCollection 리스트 값으로 매치
                allWords.append(item.get("description"));
            }

            classifyDocuments(jsonData, jsonService.jsonLocal, jsonService.jsonCollection, allWords.toString());
            log.info("문서 타입 55 :{}",docType);
            log.info("문서 타입 54 :{}",documentType);
            log.info("문서 타입 56 :{}",resultList);

//            JsonService.processMarking(excelData,configLoader.resultFilePath,docType);

            try {
                createExcel(saveFilePath);
            } catch (IOException e) {
                log.error("엑셀 파일 생성 실패: {}", e.getStackTrace()[0]);
            }
            cnt++;
        }
    }

    // 엑셀 데이터와 비교하여 국가 및 양식 분류
    public void classifyDocuments(Map<String, List<List<String[]>>> jsonData, String jsonLocale, List<Map<String, Object>> items, String jsonDescription) {
        //List<List<String[]>> targetSheetData = excelData.get("국가");
        String countryName = getCountryFromSheetName(jsonLocale);
        List<List<String[]>> targetSheetData = jsonData.get(countryName);

        if (countryName == null || countryName.equals("존재하지 않는 국가 코드")) {
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

        Map<String, Integer> matchCount = new HashMap<>();
        Map<String, Double> weightMap = new HashMap<>();
        Map<String, Double> formWeightSum = new HashMap<>();
        Map<String, Integer> formMatchCount = new HashMap<>();
        String formWithMostMatches = null;
        String formWithMostWeights = null;

        for (List<String[]> sheetData : targetSheetData) {
            String formName = sheetData.get(0)[0];
            formWeightSum.put(formName, 0.0);
            formMatchCount.put(formName, 0);

            for (int i = 1; i < sheetData.size(); i++) {
                String word = sheetData.get(i)[0];
                double weight = Double.parseDouble(sheetData.get(i)[1]);
                weightMap.put(word, weight);
                matchCount.put(word, 0);
            }

            List<String> matchingValues = new ArrayList<>();
            matchingValues.add(sheetData.get(0)[0]);

            for (Map<String, Object> item : items) {
                String description = (String) item.get("description");

                for (int i = 1; i < sheetData.size(); i++) {
                    if (description.equals(sheetData.get(i)[0])) {
                        matchCount.put(description, matchCount.get(description) + 1);
                        formWeightSum.put(formName, formWeightSum.get(formName) + weightMap.get(description));
                        formMatchCount.put(formName, formMatchCount.get(formName) + 1);
                        matchingValues.add(description + "(" + matchCount.get(description) + ")");
                    }
                }
            }

            for (int i = 1; i < sheetData.size(); i++) {
                String word = sheetData.get(i)[0];
                int count = matchCount.get(word);
                double weight = weightMap.get(word);
                log.info("'{}' - 일치 횟수: {}, 가중치: {}", word, count, weight);
            }

            int totalWords = sheetData.size() - 1;
            int matchedWords = formMatchCount.get(formName);
            double totalWeight = formWeightSum.get(formName);
            log.info("'{}' 양식 - 매치된 단어 수: {}/{}", formName, matchedWords, totalWords);
            log.info("'{}' 양식 - 가중치 합계: {}", formName, totalWeight);
            log.info("'{}' 양식 - 매치 결과: {}", formName, matchingValues);

            if (totalWeight > maxWeight) {
                maxWeight = totalWeight;
                formWithMostWeights = formName;
            }

            if (matchedWords > maxMatches) {
                maxMatches = matchedWords;
                formWithMostMatches = formName;
            }
        }

        log.info("가장 일치 단어 개수가 많은 양식: '{}', 일치 단어 수: {}", formWithMostMatches, maxMatches);


//        if (matchIndex == weightIndex) {
//            log.info("단어 매치 결과와 가중치 비교 결과 일치");
//        } else {
//            log.info("단어 매치 결과와 가중치 비교 결과 불일치");
//        }


        List<String> countryType = new ArrayList<>();
        countryType.add("국가");
        countryType.add(jsonLocale);
        resultList.add(countryType);

        List<String> documentType = new ArrayList<>();
        documentType.add("문서 양식");

        if (formWithMostMatches != null || formWithMostWeights != null) {
            log.info("미분류 파일: {}", items);
            documentType.add("미분류");
        } else {
            log.info("문서 분류 결과: 국가코드({}), 문서양식({}), 가중치({})", jsonLocale, formWithMostMatches, maxWeight);
            documentType.add(formWithMostMatches);
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

