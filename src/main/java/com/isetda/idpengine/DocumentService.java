package com.isetda.idpengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class DocumentService {
    private static final Logger log = LogManager.getLogger(DocumentService.class);
    public ConfigLoader configLoader;

    public ExcelService excelService = new ExcelService();
    public IMGService imgService = new IMGService();

    //정다현 추가
    public String wordLocal = "";
    public List<Map<String, Object>> matchjsonWord = new ArrayList<>();

    public File[] jsonFiles;
    public List<List<String>> resultList; // 각 변수로
    public List<List<String>> resultWord;
    public List<List<String>> resultWeightOneWord;
    public String docType="";
    public String fileName;
    public List<String> documentType = new ArrayList<>();
    String saveFilePath;

    //정다현 추가
     public String imgFileName;

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
        new IDPEngineController().jsonfiles = jsonFiles.length;

        log.info("jsonfiles idp :{}",new IDPEngineController().jsonfiles);
        int cnt = 1;
        for (File curFile : jsonFiles) {
            log.info("{}번째 JSON 파일 작업 시작 : {}", cnt , curFile.getName());
            // 각 파일 JSON Object로 저장
            String jsonFilePath = curFile.getPath();

            fileName = curFile.getName().substring(0, curFile.getName().lastIndexOf("."));
            saveFilePath = configLoader.resultFilePath + "\\" + fileName + ".xlsx";

            imgFileName = fileName.replace("_result","");

            JsonService jsonService = new JsonService(jsonFilePath);
            //정다현 추가
            wordLocal = jsonService.jsonLocal;

            //StringBuilder allWords = new StringBuilder();
            String allWords = jsonService.jsonCollection.get(0).get("description").toString();

//            for (Map<String, Object> item : jsonService.jsonCollection) {
//                allWords.append(item.get("description"));
//            }

            classifyDocuments1(jsonData, jsonService.jsonLocal, allWords);
            postProcessing(1);
            classifyDocuments2(jsonData, jsonService.jsonLocal, jsonService.jsonCollection);
            postProcessing(2);
            JsonService.findMatchingWords(jsonService.jsonCollection);
            classifyDocuments3(jsonData,jsonService.jsonLocal,JsonService.jsonCollection2);
            postProcessing(3);

            cnt++;
        }
    }

//    public interface ProgressCallback {
//        void updateProgress(double progress, String message);
//    }

//    public void createFinalResultFile(ProgressCallback progressCallback) throws Exception {
//        int totalFiles = jsonFiles.length;
//        log.info("11 :{}",totalFiles);
//        int cnt = 1;
//        for (File curFile : jsonFiles) {
//
//            log.info("{}번째 JSON 파일 작업 시작 : {}", cnt , curFile.getName());
//            // 각 파일 JSON Object로 저장
//            String jsonFilePath = curFile.getPath();
//
//            fileName = curFile.getName().substring(0, curFile.getName().lastIndexOf("."));
//            saveFilePath = configLoader.resultFilePath + "\\" + fileName + ".xlsx";
//
//            imgFileName = fileName.replace("_result","");
//
//            JsonService jsonService = new JsonService(jsonFilePath);
//            //정다현 추가
//            wordLocal = jsonService.jsonLocal;
//
//            //StringBuilder allWords = new StringBuilder();
//            String allWords = jsonService.jsonCollection.get(0).get("description").toString();
//
////            for (Map<String, Object> item : jsonService.jsonCollection) {
////                allWords.append(item.get("description"));
////            }
//
//            classifyDocuments1(jsonData, jsonService.jsonLocal, allWords);
//            postProcessing(1);
//            classifyDocuments2(jsonData, jsonService.jsonLocal, jsonService.jsonCollection);
//            postProcessing(2);
//            JsonService.findMatchingWords(jsonService.jsonCollection);
//            classifyDocuments3(jsonData,jsonService.jsonLocal,JsonService.jsonCollection2);
//            postProcessing(3);
//
//            // 진행률 업데이트
//            double progress = (double) cnt / totalFiles;
//            progressCallback.updateProgress(progress, "Processing file " + cnt + " of " + totalFiles + ": " + curFile.getName());
//
//            cnt++;
//        }
//    }

    public void postProcessing(int a) throws Exception {
//        log.info("문서 타입 55 :{}",docType);
//        log.info("문서 타입 54 :{}",documentType);
//        log.info("문서 타입 56 :{}",resultList);

        excelService.configLoader = configLoader;
        imgService.processMarking(matchjsonWord, configLoader.resultFilePath,imgFileName,a,docType);
        log.info("matchjsonWord : {}",matchjsonWord);

        try {
            excelService.createExcel(resultList, resultWord, resultWeightOneWord, fileName, saveFilePath, a);
        } catch (IOException e) {
            log.error("엑셀 파일 생성 실패: {}", e.getStackTrace()[0]);
        }
        matchjsonWord = new ArrayList<>();
    }

    // TODO : 가중치 별로 매칭되는 단어 개수를 카운트하고 가중치 1인 단어 개수를 비교 -> 파라메터로 동작 유무 설정 -> cd4로 만든 후에 작업 완료 되면 cd1으로 이름 변경

    // 합쳐진 추출 단어(description)로 일치 단어 비교
    public void classifyDocuments1(Map<String, List<List<String[]>>> jsonData, String jsonLocale, String jsonDescription) {
        int maxMatches = 0;
        int matchIndex = -1;
        String matchCountry = "";

        double maxWeight = 0;
        int weightIndex = -1;

        resultList = new ArrayList<>();
        resultWord = new ArrayList<>();
        resultWeightOneWord = new ArrayList<>();
        //List<String> matchedCountries = new ArrayList<>();

        for (String countryName : jsonData.keySet()) {
            List<List<String[]>> forms = jsonData.get(countryName);

            if (forms == null) {
                log.info("No matching country for {}", countryName);
                continue;
            }

            for (int col = 0; col < forms.size(); col++) {
                List<String[]> form = forms.get(col);

                double addWeight = 0;
                int addWeightOneValue = 0;
                int matches = 0;
                int weightOneCount = 0;
                List<String> matchingValues = new ArrayList<>();
                List<String> weightOneValues = new ArrayList<>();

                matchingValues.add(form.get(0)[0]);
                weightOneValues.add(form.get(0)[0]);

                int cnt = 0;
                for (int i = 1; i < form.size(); i++) {
                    String value = form.get(i)[0];
                    double weight = Double.parseDouble(form.get(i)[1]);

                    if (jsonDescription.contains(value)) {
                        addWeight += weight;

                        matches++;
                        cnt = countOccurrences(jsonDescription, value);
                        matchingValues.add(value + "(" + cnt + ")");

                        // TODO : cd2, cd3 내용 추가
                        if (configLoader.weightCountFlag) {
                            if (weight == 1.0) {
                                weightOneValues.add(value + "(" + cnt + ")");
                                addWeightOneValue += cnt;
                            }
                        }
                    } else {
                        cnt = 0;
                    }
                    log.info("'{}' - MC: {}, WT: {}", value, cnt, Double.parseDouble(form.get(i)[1]));
                }

                log.info("'{}' Document Type - Number of matched words with weight 1: {}", form.get(0)[0], addWeightOneValue);
                log.info("'{}' Document Type - Matched words with weight 1 result: {}", form.get(0)[0], weightOneValues.subList(1, weightOneValues.size()));
                log.info("'{}' Document Type - Number of matching word: {}/{}", form.get(0)[0], matches, form.size() - 1);
                log.info("'{}' Document Type - Weight sum: {}", form.get(0)[0], addWeight);
                log.info("'{}' Document Type - Match result: {}", form.get(0)[0], matches != 0 ? matchingValues.subList(1, matchingValues.size()) : "");

                matchingValues.add(matches + ""); // 매치 단어 수 결과 리스트에 추가
                resultWord.add(matchingValues);

                weightOneValues.add(addWeightOneValue + "");
                resultWeightOneWord.add(weightOneValues);

                if (matches == maxMatches) {
                    if (addWeight > maxWeight) {
                        maxMatches = matches;
                        maxWeight = addWeight;
                        matchIndex = col;
                        weightIndex = col;
                        matchCountry = countryName;
                    }
                } else if (matches > maxMatches) {
                    maxMatches = matches;
                    matchIndex = col;
                    matchCountry = countryName;
                }

                if (addWeight > maxWeight) {
                    maxWeight = addWeight;
                    weightIndex = col;
                }
            }
        }

        if (matchIndex == weightIndex) {
            log.info("Word match results and weight comparison results match");
        } else {
            log.info("Discrepancies between word match results and weight comparison results");
        }

        List<String> countryType = new ArrayList<>();
        countryType.add("국가");

        List<String> languageCode = new ArrayList<>();
        languageCode.add("언어");

        List<String> documentType = new ArrayList<>();
        documentType.add("문서 양식");

        if (matchIndex == -1 || weightIndex == -1) {
            log.info("Unclassified File, Reason: No document matching classification result");
            countryType.add("미분류");
            languageCode.add("미분류");
            documentType.add("미분류");
        } else if (maxWeight <= 0.5) {
            log.info("Unclassified File, Reason: Underweight");
            countryType.add("미분류");
            languageCode.add("미분류");
            documentType.add("미분류");
        } else {
            log.info("Document classification results: Country({}), Language Code({}), Document Type({}), Weight({})", matchCountry, jsonData.get(matchCountry).get(matchIndex).get(0)[1], jsonData.get(matchCountry).get(matchIndex).get(0)[0], maxWeight);
            countryType.add(matchCountry);
            languageCode.add(jsonData.get(matchCountry).get(matchIndex).get(0)[1]);
            documentType.add(jsonData.get(matchCountry).get(matchIndex).get(0)[0]);
        }
        resultList.add(countryType);
        resultList.add(languageCode);
        resultList.add(documentType);

        log.info("Excel Data Results: {}", resultList);
        docType = resultList.get(1).get(1);
        log.info("docType : {}",docType);
    }

    public void classifyDocuments2(Map<String, List<List<String[]>>> jsonData, String jsonLocale, List<Map<String, Object>> items) {
        //정다혀 추가
        Map<String, List<Map<String, Object>>> formMatchedWords = new HashMap<>();

        int maxMatches = 0;
        String matchCountry = "";
        String matchLanguage = "";

        double maxWeight = 0;

        resultList = new ArrayList<>();
        resultWord = new ArrayList<>();
        resultWeightOneWord = new ArrayList<>();

        // null 처리

        Map<String, Integer> matchCount = new HashMap<>();
        Map<String, Double> weightMap = new HashMap<>();
        Map<String, Double> formWeightSum = new HashMap<>();
        Map<String, Integer> formMatchCount = new HashMap<>();

        String formWithMostMatches = null;
        String formWithMostWeights = null;

        for (String countryName : jsonData.keySet()) {
            List<List<String[]>> forms = jsonData.get(countryName);

            if (forms == null) {
                log.info("No matching country for {}", countryName);
                continue;
            }

            for (List<String[]> form : forms) {
                int totalMatches = 0; // 전체 매치된 단어의 수
                int addWeightOneValue = 0;
                String formName = form.get(0)[0];
                String formLanguage = form.get(0)[1];
                formWeightSum.put(formName, 0.0);
                formMatchCount.put(formName, 0);

                for (int i = 1; i < form.size(); i++) {
                    String word = form.get(i)[0];
                    double weight = Double.parseDouble(form.get(i)[1]);
                    weightMap.put(word, weight);
                    matchCount.put(word, 0);
                }

                List<String> matchingValues = new ArrayList<>();
                List<String> weightOneValues = new ArrayList<>();

                matchingValues.add(form.get(0)[0]);
                weightOneValues.add(form.get(0)[0]);

                for (Map<String, Object> item : items) {
                    String description = (String) item.get("description");

                    for (int j = 1; j < form.size(); j++) {
                        if (description.equals(form.get(j)[0])) {
                            matchCount.put(description, matchCount.get(description) + 1);
                            formWeightSum.put(formName, formWeightSum.get(formName) + weightMap.get(description));
                            formMatchCount.put(formName, formMatchCount.get(formName) + 1);
                            totalMatches++; // 전체 매치 수 증가
                            formMatchedWords.computeIfAbsent(formName, k -> new ArrayList<>()).add(item);
                        }
                    }
                }

                for (int i = 1; i < form.size(); i++) {
                    String word = form.get(i)[0];
                    int count = matchCount.get(word);
                    double weight = weightMap.get(word);

                    if (count >= 1) {
                        matchingValues.add(word + "(" + count + ")");

                        if (configLoader.weightCountFlag) {
                            if (weight == 1.0) {
                                weightOneValues.add(word + "(" + count + ")");
                                addWeightOneValue += count;
                            }
                        }
                    }
                    log.info("'{}' - MC: {}, WT: {}", word, count, weight);
                }

                int totalWords = form.size() - 1;
                int matchedWords = formMatchCount.get(formName);
                double totalWeight = formWeightSum.get(formName);
                log.info("'{}' Document Type - Number of matched words with weight 1: {}", formName, addWeightOneValue);
                log.info("'{}' Document Type - Matched words with weight 1 result: {}", formName, matchingValues);
                log.info("'{}' Document Type - Number of matching word: {}/{}", formName, matchedWords, totalWords);
                log.info("'{}' Document Type - Weight sum: {}", formName, totalWeight);
                log.info("'{}' Document Type - Match result: {}", formName, matchedWords != 0 ? matchingValues : "");

                matchingValues.add(totalMatches + ""); // 매치 단어 수 결과 리스트에 추가
                resultWord.add(matchingValues);

                weightOneValues.add(addWeightOneValue + "");
                resultWeightOneWord.add(weightOneValues);

                if (matchedWords == maxMatches) {
                    if (totalWeight > maxWeight) {
                        maxMatches = matchedWords;
                        maxWeight = totalWeight;
                        formWithMostMatches = formName;
                        formWithMostWeights = formName;
                        matchLanguage = formLanguage;
                        matchCountry = countryName;
                    }
                } else if (matchedWords > maxMatches) {
                    maxMatches = matchedWords;
                    formWithMostMatches = formName;
                    matchLanguage = formLanguage;
                    matchCountry = countryName;
                }

                if (totalWeight > maxWeight) {
                    maxWeight = totalWeight;
                    formWithMostWeights = formName;
                }
            }
        }
        if (formWithMostMatches != null) {
            matchjsonWord = formMatchedWords.getOrDefault(formWithMostMatches, new ArrayList<>());
        } else {
            matchjsonWord = new ArrayList<>(); // 일치하는 양식이 없을 경우 빈 리스트
        }

        log.debug("가장 일치 단어 개수가 많은 양식: '{}', 일치 단어 수: {}", formWithMostMatches, maxMatches);
        log.debug("일치하는 단어와 좌표: {}", matchjsonWord);


//        if (matchIndex == weightIndex) {
//            log.info("단어 매치 결과와 가중치 비교 결과 일치");
//        } else {
//            log.info("단어 매치 결과와 가중치 비교 결과 불일치");
//        }

        List<String> countryType = new ArrayList<>();
        countryType.add("국가");

        List<String> languageCode = new ArrayList<>();
        languageCode.add("언어");

        List<String> documentType = new ArrayList<>();
        documentType.add("문서 양식");

        if (formWithMostMatches == null || formWithMostWeights == null) {
            log.info("Unclassified File, Reason: No document matching classification result");
            countryType.add("미분류");
            languageCode.add("미분류");
            documentType.add("미분류");
        } else if (maxWeight <= 0.5) {
            log.info("Unclassified File, Reason: Underweight");
            countryType.add("미분류");
            languageCode.add("미분류");
            documentType.add("미분류");
        } else {
            log.info("Document classification results: Country({}), Language Code({}), Document Type({}), Weight({})", matchCountry, matchLanguage, formWithMostMatches, maxWeight);
            countryType.add(matchCountry);
            languageCode.add(matchLanguage);
            documentType.add(formWithMostMatches);
        }

        resultList.add(countryType);
        resultList.add(languageCode);
        resultList.add(documentType);

        log.info("Excel Data Results: {}", resultList);
        docType = resultList.get(1).get(1);
    }

    // 단어 카운트
    public static int countOccurrences(String input, String word) {
        if (word.isEmpty()) {
            return 0;
        }
        String[] parts = input.split(Pattern.quote(word));
        return parts.length - 1;
    }

    //정다현 추가 내용
    public void classifyDocuments3(Map<String, List<List<String[]>>> jsonData, String jsonLocale, List<Map<String, Object>> items) {
        //정다현 추가
//        List<Map<String, Object>> matchjsonWord2 = new ArrayList<>();
        //정다혀 추가
        Map<String, List<Map<String, Object>>> formMatchedWords = new HashMap<>();

        int maxMatches = 0;
        String matchCountry = "";
        String matchLanguage = "";

        double maxWeight = 0;

        resultList = new ArrayList<>();
        resultWord = new ArrayList<>();
        resultWeightOneWord = new ArrayList<>();

        // null 처리

        Map<String, Integer> matchCount = new HashMap<>();
        Map<String, Double> weightMap = new HashMap<>();
        Map<String, Double> formWeightSum = new HashMap<>();
        Map<String, Integer> formMatchCount = new HashMap<>();

        String formWithMostMatches = null;
        String formWithMostWeights = null;

        for (String countryName : jsonData.keySet()) {
            List<List<String[]>> forms = jsonData.get(countryName);

            if (forms == null) {
                log.info("No matching country for {}", countryName);
                continue;
            }

            for (List<String[]> form : forms) {
                int totalMatches = 0; // 전체 매치된 단어의 수
                int addWeightOneValue = 0;
                String formName = form.get(0)[0];
                String formLanguage = form.get(0)[1];
                formWeightSum.put(formName, 0.0);
                formMatchCount.put(formName, 0);
                formMatchedWords.put(formName, new ArrayList<>()); // 여기서 빈 리스트를 초기화

                for (int i = 1; i < form.size(); i++) {
                    String word = form.get(i)[0];
                    double weight = Double.parseDouble(form.get(i)[1]);
                    weightMap.put(word, weight);
                    matchCount.put(word, 0);
                }

                List<String> matchingValues = new ArrayList<>();
                List<String> weightOneValues = new ArrayList<>();

                matchingValues.add(form.get(0)[0]);
                weightOneValues.add(form.get(0)[0]);

                for (Map<String, Object> item : items) {
                    String description = (String) item.get("description");

                    for (int j = 1; j < form.size(); j++) {
                        if (description.equals(form.get(j)[0])) {
                            matchCount.put(description, matchCount.get(description) + 1);
                            formWeightSum.put(formName, formWeightSum.get(formName) + weightMap.get(description));
                            formMatchCount.put(formName, formMatchCount.get(formName) + 1);
                            totalMatches++; // 전체 매치 수 증가
                            formMatchedWords.computeIfAbsent(formName, k -> new ArrayList<>()).add(item);
                        }
                    }
                }

                for (int i = 1; i < form.size(); i++) {
                    String word = form.get(i)[0];
                    int count = matchCount.get(word);
                    double weight = weightMap.get(word);

                    if (count >= 1) {
                        matchingValues.add(word + "(" + count + ")");

                        if (configLoader.weightCountFlag) {
                            if (weight == 1.0) {
                                weightOneValues.add(word + "(" + count + ")");
                                addWeightOneValue += count;
                            }
                        }
                    }
                    log.info("'{}' - MC: {}, WT: {}", word, count, weight);
                }

                int totalWords = form.size() - 1;
                int matchedWords = formMatchCount.get(formName);
                double totalWeight = formWeightSum.get(formName);
                log.info("'{}' Document Type - Number of matched words with weight 1: {}", formName, addWeightOneValue);
                log.info("'{}' Document Type - Matched words with weight 1 result: {}", formName, matchingValues);
                log.info("'{}' Document Type - Number of matching word: {}/{}", formName, matchedWords, totalWords);
                log.info("'{}' Document Type - Weight sum: {}", formName, totalWeight);
                log.info("'{}' Document Type - Match result: {}", formName, matchedWords != 0 ? matchingValues : "");

                matchingValues.add(totalMatches + ""); // 매치 단어 수 결과 리스트에 추가
                resultWord.add(matchingValues);

                weightOneValues.add(addWeightOneValue + "");
                resultWeightOneWord.add(weightOneValues);

                if (matchedWords == maxMatches) {
                    if (totalWeight > maxWeight) {
                        maxMatches = matchedWords;
                        maxWeight = totalWeight;
                        formWithMostMatches = formName;
                        formWithMostWeights = formName;
                        matchLanguage = formLanguage;
                        matchCountry = countryName;
                    }
                } else if (matchedWords > maxMatches) {
                    maxMatches = matchedWords;
                    formWithMostMatches = formName;
                    matchLanguage = formLanguage;
                    matchCountry = countryName;
                }

                if (totalWeight > maxWeight) {
                    maxWeight = totalWeight;
                    formWithMostWeights = formName;
                }
            }
        }
        if (formWithMostMatches != null) {
            matchjsonWord = formMatchedWords.getOrDefault(formWithMostMatches, new ArrayList<>());
        } else {
            matchjsonWord = new ArrayList<>(); // 일치하는 양식이 없을 경우 빈 리스트
        }

        log.debug("가장 일치 단어 개수가 많은 양식: '{}', 일치 단어 수: {}", formWithMostMatches, maxMatches);
        log.debug("일치하는 단어와 좌표: {}", matchjsonWord);

        List<String> countryType = new ArrayList<>();
        countryType.add("국가");

        List<String> languageCode = new ArrayList<>();
        languageCode.add("언어");

        List<String> documentType = new ArrayList<>();
        documentType.add("문서 양식");

        if (formWithMostMatches == null || formWithMostWeights == null) {
            log.info("Unclassified File: {}", items);
            countryType.add("미분류");
            languageCode.add("미분류");
            documentType.add("미분류");
        } else if (maxWeight <= 0.5) {
            log.info("Unclassified File, Reason: Underweight");
            countryType.add("미분류");
            languageCode.add("미분류");
            documentType.add("미분류");
        } else {
            log.info("Document classification results: Country({}), Language Code({}), Document Type({}), Weight({})", matchCountry, matchLanguage, formWithMostMatches, maxWeight);
            countryType.add(matchCountry);
            //countryType.add(wordLocal);
            languageCode.add(matchLanguage);
            documentType.add(formWithMostMatches);
        }

        resultList.add(countryType);
        resultList.add(languageCode);
        resultList.add(documentType);

        log.debug("Excel Data Results: {}", resultList);
        docType = resultList.get(1).get(1);

//            matchjsonWord = matchjsonWord2;
//            log.debug("filteredMatchJsonWord2 : {}", filteredMatchJsonWord2);

    }
}
