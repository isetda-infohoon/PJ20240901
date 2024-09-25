package com.isetda.idpengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentService {
    private static final Logger log = LogManager.getLogger(DocumentService.class);
    public ConfigLoader configLoader;

    public ExcelService excelService = new ExcelService();
    public IMGService imgService = new IMGService();

    public File[] jsonFiles;
    public List<List<String>> resultList; // 각 변수로
    public List<List<String>> resultWord;
    public String docType="";
    public String fileName;
    public List<String> documentType = new ArrayList<>();

    public Map<String, List<List<String[]>>> jsonData;

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
    public void createFinalResultFile() throws Exception {
        //excelData = getExcelData();

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
                allWords.append(item.get("description"));
            }

            if (configLoader.fullTextClassify) {
                classifyDocuments(jsonData, jsonService.jsonLocal, allWords.toString());
            } else {
                classifyDocuments(jsonData, jsonService.jsonLocal, jsonService.jsonCollection);
            }

            log.info("문서 타입 55 :{}",docType);
            log.info("문서 타입 54 :{}",documentType);
            log.info("문서 타입 56 :{}",resultList);

            imgService.processMarking(jsonData, configLoader.resultFilePath, docType);

            try {
                excelService.createExcel(resultList, resultWord, fileName, saveFilePath);
            } catch (IOException e) {
                log.error("엑셀 파일 생성 실패: {}", e.getStackTrace()[0]);
            }
            cnt++;
        }
    }

    // 엑셀 데이터와 비교하여 국가 및 양식 분류
    // 쪼개어진 추출 단어 리스트로 일치 단어 비교
    public void classifyDocuments(Map<String, List<List<String[]>>> jsonData, String jsonLocale, List<Map<String, Object>> items) {
        String countryName = getCountryFromSheetName(jsonLocale);
        List<List<String[]>> targetSheetData = jsonData.get(countryName);

        if (countryName == null || countryName.equals("존재하지 않는 국가 코드")) {
            // 일치하는 시트가 없을 경우
            log.info("No matching country");
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
                log.info("'{}' - MC: {}, WT: {}", word, count, weight);
            }

            int totalWords = sheetData.size() - 1;
            int matchedWords = formMatchCount.get(formName);
            double totalWeight = formWeightSum.get(formName);
            log.info("'{}' Document Type - Number of matching word: {}/{}", formName, matchedWords, totalWords);
            log.info("'{}' Document Type - Weight sum: {}", formName, totalWeight);
            log.info("'{}' Document Type - Match result: {}", formName, matchingValues);

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
            log.info("Unclassified File: {}", items);
            documentType.add("미분류");
        } else {
            log.info("Document classification results: Country Code({}), Document Type({}), Weight({})", jsonLocale, formWithMostMatches, maxWeight);
            documentType.add(formWithMostMatches);
        }

        resultList.add(documentType);
        log.info("Excel Data Results: {}", resultList);
        docType = resultList.get(1).get(1);

    }

    // 합쳐진 추출 단어(description)로 일치 단어 비교
    public void classifyDocuments(Map<String, List<List<String[]>>> jsonData, String jsonLocale, String jsonDescription) {
        String countryName = getCountryFromSheetName(jsonLocale);
        List<List<String[]>> targetSheetData = jsonData.get(countryName);

        if (targetSheetData == null) {
            // 일치하는 시트가 없을 경우
            log.info("No matching country");
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
                        log.error("Weight calculation failed: {}", e.getStackTrace()[0]);
                    }

                    matches++;
                    cnt = countOccurrences(jsonDescription, value);
                    matchingValues.add(value + "(" + cnt + ")");
                } else {
                    cnt = 0;
                }
                log.info("'{}' - MC: {}, WT: {}", value, cnt, Double.parseDouble(columnData.get(i)[1]));
            }

            //log.info("{} / {} = {}", addWeight, matches, addWeight / matches);
            //double weight = (double) Math.round(addWeight / matches * 10) / 10;

            log.info("'{}' Document Type - Number of matching word: {}/{}", columnData.get(0)[0], matches, columnData.size() - 1);
            log.info("'{}' Document Type - Weight sum: {}", columnData.get(0)[0], addWeight);
            log.info("'{}' Document Type - Match result: {}", columnData.get(0)[0], matchingValues.subList(1, matchingValues.size()));

            matchingValues.add(matches + ""); // 매치 단어 수 결과 리스트에 추가
            resultWord.add(matchingValues);

            if (matches > maxMatches) {
                maxMatches = matches;
                matchIndex = col;
            }

            if (addWeight > maxWeight) {
                maxWeight = addWeight;
                weightIndex = col;
            }
        }

        if (matchIndex == weightIndex) {
            log.info("Word match results and weight comparison results match");
        } else {
            log.info("Discrepancies between word match results and weight comparison results");
        }


        List<String> countryType = new ArrayList<>();
        countryType.add("국가");
        countryType.add(jsonLocale);
        resultList.add(countryType);

        List<String> documentType = new ArrayList<>();
        documentType.add("문서 양식");

        if (matchIndex == -1 || weightIndex == -1) {
            log.info("Unclassified File: {}", jsonDescription);
            documentType.add("미분류");
        } else {
            log.info("Document classification results: Country Code({}), Document Type({}), Weight({})", jsonLocale, targetSheetData.get(matchIndex).get(0)[0], maxWeight);
            documentType.add(targetSheetData.get(matchIndex).get(0)[0]);
        }
        resultList.add(documentType);
        log.info("Excel Data Results: {}", resultList);
        docType = resultList.get(1).get(1);
    }

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
}
