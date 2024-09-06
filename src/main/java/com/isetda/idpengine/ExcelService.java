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
    private static final Logger log = LogManager.getLogger(IDPEngineController.class);

    private ConfigLoader configLoader = ConfigLoader.getInstance();
    String excelFilePath = configLoader.getExcelFilePath();
    boolean dbDataUsageFlag = configLoader.isDbDataUsageFlag();

    public String resultFolderPath;

    public File[] jsonFiles;
    public List<List<String>> resultList; // 각 변수로
    public List<List<String>> resultWord;

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

    public Map<String, List<List<String[]>>> getDBData() {
        Map<String, List<List<String[]>>> dbData = new HashMap<>();

        DBService dbService = new DBService();
        List<Word> wordList = dbService.getWordData();

        String currentCountry = "";

        for (Word wordData : wordList) {
            String countryCode = wordData.getCountryCode();
            String documentType = wordData.getDocumentType();
            String word = wordData.getWord();
            String wordWeight = String.valueOf(wordData.getWordWeight());

            // countryCode에 해당하는 리스트 가져오기
            List<List<String[]>> documentList = dbData.getOrDefault(countryCode, new ArrayList<>());

            if (!currentCountry.equals(countryCode)) {
                log.info("국가코드: {}", countryCode);
                currentCountry = countryCode;
            }

            // documentType에 해당하는 리스트 찾기
            List<String[]> currentDocument = null;
            for (List<String[]> doc : documentList) {
                if (doc.get(0)[0].equals(documentType)) {
                    currentDocument = doc;
                    break;
                }
            }

            // documentType에 해당하는 리스트가 없으면 새로 생성
            if (currentDocument == null) {
                currentDocument = new ArrayList<>();
                currentDocument.add(new String[]{documentType, ""});
                documentList.add(currentDocument);

                log.info("H: {} ", documentType);
            }

            // 단어와 가중치를 리스트에 추가
            currentDocument.add(new String[]{word, wordWeight});
            log.info("W: {}, {}", word, wordWeight);

            // 결과 Map에 업데이트
            dbData.put(countryCode, documentList);
        }

        for (Map.Entry<String, List<List<String[]>>> entry : dbData.entrySet()) {
            log.info("국가 코드: {}", entry.getKey());
            for (List<String[]> column : entry.getValue()) {
                log.info("DB 값: {}", column);
            }
        }

        return dbData;
    }

    // TODO json 경로 추가 후 실행 확인
    public Map<String, List<List<String[]>>> getJsonData() {
        Map<String, List<List<String[]>>> jsonData = new HashMap<>();

        String jsonFilePath = "C:\\Users\\suaah\\OneDrive\\바탕 화면\\식품안전관리 서류\\국가, 문서 양식별 추출 단어 리스트.json";

        try (FileReader reader = new FileReader(new File(jsonFilePath))) {
            // JSON 파일 읽기
            StringBuilder sb = new StringBuilder();
            int i;
            while ((i = reader.read()) != -1) {
                sb.append((char) i);
            }

            // JSON 파싱
            JSONObject rootObject = new JSONObject(sb.toString());

            System.out.println(rootObject);

            for (String countryCode : rootObject.keySet()) {
                JSONArray documents = rootObject.getJSONArray(countryCode);

                Map<String, List<String[]>> tempMap = new HashMap<>();

                for (int j = 0; j < documents.length(); j++) {
                    JSONObject document = documents.getJSONObject(j);

                    for (String documentType : document.keySet()) {
                        if (!documentType.endsWith(" 가중치")) {
                            String word = document.getString(documentType);
                            double weight = document.getDouble(documentType + " 가중치");

                            tempMap.putIfAbsent(documentType, new ArrayList<>());
                            tempMap.get(documentType).add(new String[]{word, String.valueOf(weight)});
                        }
                    }
                }

                List<List<String[]>> documentList = new ArrayList<>();
                for (Map.Entry<String, List<String[]>> entry : tempMap.entrySet()) {
                    List<String[]> wordList = new ArrayList<>();
                    wordList.add(new String[]{entry.getKey(), "empty"});
                    wordList.addAll(entry.getValue());
                    documentList.add(wordList);
                }

                jsonData.put(countryCode, documentList);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Map.Entry<String, List<List<String[]>>> entry : jsonData.entrySet()) {
            log.info("국가 코드: {}", entry.getKey());
            for (List<String[]> column : entry.getValue()) {
                log.info("단어 값: {}", column);
            }
        }

        return jsonData;
    }

    // 폴더의 모든 파일(json)을 반복 (JSON Object로 저장 및 split, classifyDocuments 메소드로 분류 진행) (iterateFiles)
    public void createFinalResultFile() {
        Map<String, List<List<String[]>>> wordData;
//        if (dbDataUsageFlag) {
//            wordData = getDBData();
//        } else {
//            wordData = getExcelData();
//        }
        wordData = getJsonData();

        int cnt = 1;
        for (File curFile : jsonFiles) {
            log.info("{}번째 JSON 파일 작업 시작", cnt);
            // 각 파일 JSON Object로 저장
            String jsonFilePath = curFile.getPath();

            String fileName = curFile.getName().substring(0, curFile.getName().lastIndexOf("."));
            String saveFilePath = resultFolderPath + "\\" + fileName + ".xlsx";

            JsonService jsonService = new JsonService(jsonFilePath);

            StringBuilder allWords = new StringBuilder();

            for (Map<String, Object> item : jsonService.jsonCollection) {
                allWords.append(item.get("description"));
            }

            classifyDocuments(wordData, jsonService.jsonLocal, allWords.toString());

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

        log.info("문서 분류 결과: 국가코드({}), 문서양식({}), 가중치({})", jsonLocale, targetSheetData.get(matchIndex).get(0)[0], maxWeight);

        List<String> countryType = new ArrayList<>();
        countryType.add("국가");
        countryType.add(jsonLocale);
        resultList.add(countryType);

        List<String> documentType = new ArrayList<>();
        documentType.add("문서 양식");
        documentType.add(targetSheetData.get(matchIndex).get(0)[0]);
        resultList.add(documentType);
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

        try (FileOutputStream fileOut = new FileOutputStream(saveFilePath)) {
            workbook.write(fileOut);
            log.info("엑셀 파일 생성 완료: {} ", saveFilePath);
        }
        workbook.close();
    }




}

