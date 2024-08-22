package com.isetda.idpengine;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;


public class ExcelService {
    private ConfigLoader configLoader = ConfigLoader.getInstance();
    String excelFilePath = configLoader.getExcelFilePath();

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
            e.printStackTrace();
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

            StringBuilder allWords = new StringBuilder();

            for (Map<String, Object> item : jsonService.wordList) {
                allWords.append(item.get("description"));
            }

            classifyDocuments(excelData, jsonService.jsonLocal, allWords.toString());

            try {
                createExcel(saveFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 엑셀 데이터와 비교하여 국가 및 양식 분류
    public void classifyDocuments(Map<String, List<List<String>>> excelData, String jsonLocale, String jsonDescription) {
        List<List<String>> targetSheetData = excelData.get(jsonLocale);
        System.out.println(targetSheetData);

        if (targetSheetData == null) {
            // 일치하는 시트가 없을 경우
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

            System.out.println("resultWord : " + resultWord);
        }


        System.out.println(maxMatches + ", " + index);

        System.out.println("문서 분류 결과: " + targetSheetData.get(index).get(0));

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
            System.out.println("엑셀 파일이 성공적으로 생성되었습니다: " + saveFilePath);
        }
        workbook.close();
    }
}

