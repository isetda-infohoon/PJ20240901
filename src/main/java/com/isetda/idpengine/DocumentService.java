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

    //정다현 추가
    public List<Map<String, Object>> jsonWord = new ArrayList<>();
    public String wordLocal = "";
    public List<Map<String, Object>> matchjsonWord = new ArrayList<>();

    public File[] jsonFiles;
    public List<List<String>> resultList; // 각 변수로
    public List<List<String>> resultWord;
    public String docType="";
    public String fileName;
    public List<String> documentType = new ArrayList<>();

    public Map<String, List<List<String[]>>> jsonData;
    public boolean fullTextClassify = false;

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
            String imgFileName = fileName.replace("_result","");

            JsonService jsonService = new JsonService(jsonFilePath);
            //정다현 추가
            jsonWord = jsonService.jsonCollection;
            wordLocal = jsonService.jsonLocal;

            StringBuilder allWords = new StringBuilder();

            for (Map<String, Object> item : jsonService.jsonCollection) {
                allWords.append(item.get("description"));
            }

            if (fullTextClassify) {
                classifyDocuments(jsonData, jsonService.jsonLocal, allWords.toString());
                log.info("11");
            } else {
                JsonService.findMatchingWords(jsonService.jsonCollection);
//                classifyDocuments(jsonData, jsonService.jsonLocal, JsonService.jsonCollection2);
                classifyDocuments3(JsonService.jsonCollection2);
                log.info("22");

            }

            log.info("문서 타입 55 :{}",docType);
            log.info("문서 타입 54 :{}",documentType);
            log.info("문서 타입 56 :{}",resultList);

            imgService.processMarking(matchjsonWord, configLoader.resultFilePath,imgFileName);
            log.info("matchjsonWord : {}",matchjsonWord);

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
        log.info("countryName : {}",countryName);
        List<List<String[]>> targetSheetData = jsonData.get(countryName);
        log.info("targetSheetData : {}",targetSheetData);


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

        if (formWithMostMatches == null || formWithMostWeights == null) {
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

    // 합쳐진 추출 단어(description)로 일치 단어 비교
    public void classifyDocuments(Map<String, List<List<String[]>>> jsonData, String jsonLocale, String jsonDescription) throws IOException {
        String countryName = getCountryFromSheetName(jsonLocale);
        List<List<String[]>> targetSheetData = jsonData.get(countryName);

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
            //double weight = (double) Math.round(addWeight / matches * 10) / 10;

            log.info("'{}' 양식 - 매치된 단어 수: {}/{}", columnData.get(0)[0], matches, columnData.size() - 1);
            log.info("'{}' 양식 - 가중치 합계: {}", columnData.get(0)[0], addWeight);
            log.info("'{}' 양식 - 매치 결과: {}", columnData.get(0)[0], matchingValues.subList(1, matchingValues.size()));


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

//        imgService.processMarking(jsonData, configLoader.resultFilePath, docType);

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

    //정다현 추가 내용
    public void classifyDocuments3(List<Map<String, Object>> jsonWord2) {
        String countryName = getCountryFromSheetName(wordLocal);
        log.debug("countryName : {}", countryName);
        List<List<String[]>> targetSheetData = jsonData.get(countryName);
        log.debug("targetSheetData : {}", targetSheetData);
        //정다현 추가
//        List<Map<String, Object>> matchjsonWord2 = new ArrayList<>();
        //정다혀 추가
        Map<String, List<Map<String, Object>>> formMatchedWords = new HashMap<>();


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
            formMatchedWords.put(formName, new ArrayList<>()); // 여기서 빈 리스트를 초기화

            for (int i = 1; i < sheetData.size(); i++) {
                String word = sheetData.get(i)[0];
                double weight = Double.parseDouble(sheetData.get(i)[1]);
                weightMap.put(word, weight);
                matchCount.put(word, 0);
            }

            List<String> matchingValues = new ArrayList<>();
            matchingValues.add(sheetData.get(0)[0]);

            for (Map<String, Object> item : jsonWord2) {
                String description = (String) item.get("description");

                for (int i = 1; i < sheetData.size(); i++) {
                    if (description.equals(sheetData.get(i)[0])) {
                        matchCount.put(description, matchCount.get(description) + 1);
                        formWeightSum.put(formName, formWeightSum.get(formName) + weightMap.get(description));
                        formMatchCount.put(formName, formMatchCount.get(formName) + 1);
                        matchingValues.add(description + "(" + matchCount.get(description) + ")");
                        formMatchedWords.computeIfAbsent(formName, k -> new ArrayList<>()).add(item);
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
        if (formWithMostMatches != null) {
            matchjsonWord = formMatchedWords.getOrDefault(formWithMostMatches, new ArrayList<>());
        } else {
            matchjsonWord = new ArrayList<>(); // 일치하는 양식이 없을 경우 빈 리스트
        }

        log.info("가장 일치 단어 개수가 많은 양식: '{}', 일치 단어 수: {}", formWithMostMatches, maxMatches);
        log.info("일치하는 단어와 좌표: {}", matchjsonWord);

//        if (matchIndex == weightIndex) {
//            log.info("단어 매치 결과와 가중치 비교 결과 일치");
//        } else {
//            log.info("단어 매치 결과와 가중치 비교 결과 불일치");
//        }


            List<String> countryType = new ArrayList<>();
            countryType.add("국가");
            countryType.add(wordLocal);
            resultList.add(countryType);

            List<String> documentType = new ArrayList<>();
            documentType.add("문서 양식");

            if (formWithMostMatches == null || formWithMostWeights == null) {
                log.info("미분류 파일: {}", jsonWord2);
                documentType.add("미분류");
            } else {
                log.info("문서 분류 결과: 국가코드({}), 문서양식({}), 가중치({})", wordLocal, formWithMostMatches, maxWeight);
                documentType.add(formWithMostMatches);
            }

            resultList.add(documentType);
            log.info("엑셀 데이터 결과 : {}", resultList);
            docType = resultList.get(1).get(1);

//            matchjsonWord = matchjsonWord2;
//            log.debug("filteredMatchJsonWord2 : {}", filteredMatchJsonWord2);

        }
    }
