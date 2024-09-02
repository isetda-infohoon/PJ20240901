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
import java.util.stream.Collectors;


public class ExcelService {
    private static final Logger log = LogManager.getLogger(IDPEngineController.class);

    private ConfigLoader configLoader = ConfigLoader.getInstance();
    String excelFilePath = configLoader.getExcelFilePath();

    public String resultFolderPath=configLoader.getResultFilePath();

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
    }


    // 엑셀의 단어 리스트 가져오기
    public Map<String, List<List<String>>> getExcelData() {
        Map<String, List<List<String>>> excelData = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(excelFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                List<List<String>> sheetData = new ArrayList<>();

                int maxColumns = 0;
                for (Row row : sheet) {
                    if (row.getLastCellNum() > maxColumns) {
                        maxColumns = row.getLastCellNum();
                    }
                }

                for (int col = 0; col < maxColumns; col++) {
                    List<String> columnData = new ArrayList<>();
                    for (Row row : sheet) {
                        Cell cell = row.getCell(col);
                        if (cell != null && !cell.toString().isEmpty()) {
                            columnData.add(cell.toString());
                        }
                    }
                    if (!columnData.isEmpty()) {
                        sheetData.add(columnData);
                    }
                }

                excelData.put(sheet.getSheetName(), sheetData);
            }

        } catch (IOException e) {
            log.error("엑셀에서 단어 리스트를 가져오지 못했습니다: {}", e.getStackTrace()[0]);
        }

        // 엑셀 데이터 출력 (테스트용)
//        for (Map.Entry<String, List<List<String>>> entry : excelData.entrySet()) {
//            System.out.println("Sheet: " + entry.getKey());
//            for (List<String> column : entry.getValue()) {
//                System.out.println(column);
//            }
//        }
        return excelData;
    }

    // 폴더의 모든 파일(json)을 반복 (JSON Object로 저장 및 split, classifyDocuments 메소드로 분류 진행) (iterateFiles)
    public void createFinalResultFile() {
        Map<String, List<List<String>>> excelData = getExcelData();

        for (File curFile : jsonFiles) {
            // 각 파일 JSON Object로 저장
            String jsonFilePath = curFile.getPath();

            String fileName = curFile.getName().substring(0, curFile.getName().lastIndexOf("."));
            String saveFilePath = resultFolderPath + "\\" + fileName + ".xlsx";

            JsonService jsonService = new JsonService(jsonFilePath);
            //모든 단어 저장 할 객체 왜?? 이걸 사용했는 지 질문
            StringBuilder allWords = new StringBuilder();

            for (Map<String, Object> item : jsonService.jsonCollection) {
                allWords.append(item.get("description"));
            }

            classifyDocuments(excelData, jsonService.jsonLocal, allWords.toString());

            createExcel(saveFilePath);
            log.info("엑셀 저장 성공 {}",saveFilePath);
        }
    }

    // 엑셀 데이터와 비교하여 국가 및 양식 분류
    public void classifyDocuments(Map<String, List<List<String>>> excelData, String jsonLocale, String jsonDescription) {
        List<List<String>> targetSheetData = excelData.get(jsonLocale.toUpperCase());
        System.out.println(excelData);
        System.out.println(targetSheetData);

        if (targetSheetData == null) {
            // 일치하는 시트가 없을 경우
            log.warn("일치하는 시트(국가)가 없습니다.");
        }

        int maxMatches = 0;
        int index = -1;

        resultList = new ArrayList<>();
        resultWord = new ArrayList<>();

        // null 처리
        for (int col = 0; col < targetSheetData.size(); col++) {
            List<String> columnData = targetSheetData.get(col);

            int matches = 0;
            List<String> matchingValues = new ArrayList<>();

            matchingValues.add(columnData.get(0));

            for (String value : columnData) {
                if (jsonDescription.contains(value)) {
                    matches++;
                    matchingValues.add(value + "(" + countOccurrences(jsonDescription, value) + ")");
                }
            }

            matchingValues.add(matches + ""); // 각 열 매치 단어 수
            resultWord.add(matchingValues);

            // 각 열마다 일치하는 값들을 1줄로 콘솔에 출력
            System.out.println("열 " + col + ": " + matchingValues);

            if (matches > maxMatches) {
                maxMatches = matches;
                index = col;
            }

            //System.out.println("resultWord : " + resultWord);
        }


        System.out.println(maxMatches + ", " + index);

        //System.out.println("문서 분류 결과: " + targetSheetData.get(index).get(0));
        log.info("문서 분류 결과: {}", targetSheetData.get(index).get(0));

        List<String> countryType = new ArrayList<>();
        countryType.add("국가");
        countryType.add(jsonLocale);
        resultList.add(countryType);

        List<String> documentType = new ArrayList<>();
        documentType.add("문서 양식");
        documentType.add(targetSheetData.get(index).get(0));
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
    public void createExcel(String saveFilePath) {
        Path path = Paths.get(saveFilePath);

        // 파일 존재 여부 확인
        if (Files.exists(path)) {
            log.debug("파일이 이미 존재합니다: {}", saveFilePath);
            return;  // 기존 파일이 존재하면 넘어가기
        }
        log.info("새 엑셀 파일을 생성합니다: {}", saveFilePath);

        // XSSFWorkbook 생성
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fileOut = new FileOutputStream(saveFilePath)) {

            Sheet sheet = workbook.createSheet("Sheet1");

            // 헤더 작성
            Row headerRow = sheet.createRow(0);
            String[] headers = {"국가", "문서 양식", "키워드", "키워드 개수"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // 데이터 작성
            Row dataRow = sheet.createRow(1);

            // 국가 정보와 문서 양식 정보 작성
            if (!resultList.isEmpty()) {
                dataRow.createCell(0).setCellValue(resultList.get(0).get(1)); // 국가
                dataRow.createCell(1).setCellValue(resultList.get(1).get(1)); // 문서 양식
            }
            // 키워드 및 키워드 개수 작성
                String keywordData = resultWord.stream().map(rowData -> rowData.get(0) + ": (" + rowData.stream().skip(1).limit(rowData.size() - 2).collect(Collectors.joining(", ")) + ")").collect(Collectors.joining("; "));

                dataRow.createCell(2).setCellValue(keywordData);

                String keywordCountData = resultWord.stream().map(rowData -> rowData.get(0) + "(" + rowData.get(rowData.size() - 1) + ")").collect(Collectors.joining(" "));

                dataRow.createCell(3).setCellValue(keywordCountData);
                dataRow.createCell(2).setCellValue("키워드 정보 포함 안 함");
                dataRow.createCell(3).setCellValue("키워드 개수 포함 안 함");

            // 자동 열 크기 조정
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            // 파일 저장
            workbook.write(fileOut);
            log.info("엑셀 파일 작성 및 저장 성공: {}", saveFilePath);

        } catch (FileNotFoundException e) {
            log.error("파일을 찾을 수 없습니다: {}", saveFilePath, e);
        } catch (IOException e) {
            log.error("엑셀 파일 저장 중 오류 발생: {}", saveFilePath, e);
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생: {}", e.getMessage(), e);
        }
    }
}

