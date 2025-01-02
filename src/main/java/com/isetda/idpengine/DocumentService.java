package com.isetda.idpengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    public List<Map<String, Object>> filteredResult;
    public String docType="";
    public String fileName;
    public List<String> documentType = new ArrayList<>();
    String saveFilePath;
    String textSaveFilePath;
    String datasetSavePath;

    Map<String, Map<String, String>> resultByVersion = new HashMap<>();
    Map<String, Map<String, String>> finalResultByVersion = new HashMap<>();
    Map<String, List<String>> finalCertificateResult = new HashMap<>();

    List<String> certificateType = new ArrayList<>();

    //정다현 추가
     public String imgFileName;

//    public Map<String, List<List<String[]>>> jsonData;
    public Map<String, List<Map<String, Object>>> jsonData;

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
            textSaveFilePath = configLoader.resultFilePath + "\\" + fileName + ".txt";
            datasetSavePath = configLoader.resultFilePath + "\\" + fileName + "filtered_result2.xlsx";

            imgFileName = fileName.replace("_result","");

            JsonService jsonService = new JsonService(jsonFilePath);
            //정다현 추가
            wordLocal = jsonService.jsonLocal;
//기준 꺼 1117
            String allWords = jsonService.jsonCollection.get(0).get("description").toString();



//            for (Map<String, Object> item : jsonService.jsonCollection) {
//                allWords.append(item.get("description"));
//            }

            resultByVersion.put(fileName.replace("_result", ""), new HashMap<>());
            finalResultByVersion.put(fileName.replace("_result", ""), new HashMap<>());

            // 인증서 유형
            certificateType = getCertificateType(allWords);
            finalCertificateResult.put(fileName.replace("_result", ""), certificateType);

            if (configLoader.cdAUsageFlag) {
                if (configLoader.cd1UsageFlag) {
                    classifyDocuments1(jsonData, allWords);
                    postProcessing("A1");
                }

                if (configLoader.cd2UsageFlag) {
                    classifyDocuments2(jsonData, jsonService.jsonCollection);
                    postProcessing("A2");
                }

                if (configLoader.cd3UsageFlag) {
                    JsonService.sortAnnotations(jsonService.jsonCollection);
                    JsonService.findMatchingWords(jsonService.jsonCollection);
                    classifyDocuments3(jsonData, JsonService.jsonCollection2);
                    postProcessing("A3");
                }
            }

            if (configLoader.cdBUsageFlag) {
                if (configLoader.cd1UsageFlag) {
                    classifyDocuments_B1(jsonData, allWords);
                    postProcessing("B1");
                }

                if (configLoader.cd2UsageFlag) {
                    classifyDocuments_B2(jsonData, jsonService.jsonCollection);
                    postProcessing("B2");
                }

                if (configLoader.cd3UsageFlag) {
                    JsonService.sortAnnotations(jsonService.jsonCollection);
                    JsonService.findMatchingWords(jsonService.jsonCollection);
                    classifyDocuments_B3(jsonData, JsonService.jsonCollection2);
                    postProcessing("B3");
                }
            }

            if (configLoader.cdCUsageFlag) {
                if (configLoader.cd1UsageFlag) {
                    classifyDocuments_C1(jsonData, allWords);
                    postProcessing("C1");
                }

                if (configLoader.cd2UsageFlag) {
                    classifyDocuments_C2(jsonData, jsonService.jsonCollection);
                    postProcessing("C2");
                }

                if (configLoader.cd3UsageFlag) {
//                    double eps = jsonService.jsonCollection.size() < 100 ? 15.0 : 25.0;
//                    int minPts = jsonService.jsonCollection.size() < 100 ? 2 : 3;
//                    List<Map<String, Object>> sortedCollection = JsonService.sortAnnotations4(
//                            jsonService.jsonCollection,
//                            eps,
//                            minPts
//                    );
//                    jsonService.jsonCollection = sortedCollection;
                    JsonService.sortAnnotations(jsonService.jsonCollection);
                    JsonService.findMatchingWords(JsonService.jsonCollection3);
                    classifyDocuments_C3(jsonData, JsonService.jsonCollection2);
                    postProcessing("C3");
                }
            }

            if (configLoader.writeTextResults) {
                excelService.textFinalResult(textSaveFilePath, fileName, finalResultByVersion, configLoader.classificationCriteria, configLoader.subClassificationCriteria, finalCertificateResult);
            }

            // dataset 엑셀 작성
//            datasetSorting(jsonData, allWords);
//            excelService.dataWriteExcel2(filteredResult);


//            classifyDocuments4(jsonData,jsonService.jsonLocal,JsonService.jsonCollection2);
//            postProcessing(4);

            cnt++;
        }

        System.out.println();

//        for (Map.Entry<String, List<String>> entry : finalCertificateResult.entrySet()) {
//            System.out.println("파일명: " + entry.getKey());
//            System.out.println("인증서 유형: " + entry.getValue());
//        }

        for (Map.Entry<String, Map<String, String>> entry : resultByVersion.entrySet()) {
            String filename = entry.getKey();
            Map<String, String> versionMap = entry.getValue();
            System.out.println("Filename: " + filename);
            for (Map.Entry<String, String> versionEntry : versionMap.entrySet()) {
                String version = versionEntry.getKey();
                String value = versionEntry.getValue();
                System.out.println("  Version: " + version + ", Value: " + value);
            }
        }

        if (configLoader.createFolders) {
            excelService.moveFiles(configLoader.resultFilePath, resultByVersion, configLoader.classificationCriteria, configLoader.subClassificationCriteria);
        }
    }


    public void postProcessing(String a) throws Exception {
//        log.info("문서 타입 55 :{}",docType);
//        log.info("문서 타입 54 :{}",documentType);
//        log.info("문서 타입 56 :{}",resultList);

        excelService.configLoader = configLoader;

        String baseFileName= fileName.replace("_result", "");

        if(configLoader.markingCheck){
            imgService.processMarking(matchjsonWord, configLoader.resultFilePath, imgFileName, a, docType);
        }
        log.info("matchjsonWord : {}",matchjsonWord);

        try {
            if (configLoader.writeExcelResults) {
                excelService.createExcel2(resultList, filteredResult, fileName, saveFilePath, a);
            }

            if (configLoader.writeTextResults) {
                excelService.createText(resultList, filteredResult, fileName, textSaveFilePath, a);
            }
        } catch (IOException e) {
            log.error("결과 파일 생성 실패: {}", e.getStackTrace()[0]);
        }
        matchjsonWord = new ArrayList<>();
        resultByVersion.get(baseFileName).put(a, resultList.get(2).get(1)+"("+resultList.get(1).get(1)+")");
        finalResultByVersion.get(baseFileName).put(a, resultList.get(0).get(1) + "/" + resultList.get(1).get(1) + "/" + resultList.get(2).get(1));
    }

    // 합쳐진 추출 단어(description)로 일치 단어 비교
    public void classifyDocuments1(Map<String, List<Map<String, Object>>> jsonData, String jsonDescription) {
        filteredResult = new ArrayList<>();
        resultList = new ArrayList<>();

        String finalTopTemplate;
        int finalMaxTotalCount;

        String finalCountry = "";
        String finalLanguage = "";
        double totalWtSum = 0;

        for (Map.Entry<String, List<Map<String, Object>>> countryEntry : jsonData.entrySet()) {
            String countryName = countryEntry.getKey();
            List<Map<String, Object>> formList = countryEntry.getValue();

            for (Map<String, Object> formMap : formList) {
                String formName = (String) formMap.get("Template Name");
                List<String> languages = (List<String>) formMap.get("Language");

                // H-RULE
                List<Map<String, Object>> hRules = (List<Map<String, Object>>) formMap.get("H-RULE");
                if (hRules != null) {
                    for (Map<String, Object> hRule : hRules) {
                        String word = (String) hRule.get("WD");
                        Double weight = (Double) hRule.get("WT");
                        String kr = (String) hRule.get("KR");
                        if (word != null && weight != null) {
                            int count = countOccurrences(jsonDescription, word);

                            Map<String, Object> resultMap = new HashMap<>();
                            resultMap.put("Country", countryName);
                            resultMap.put("Template Name", formName);
                            resultMap.put("Language", languages);
                            resultMap.put("WD", word);
                            resultMap.put("WT", weight);
                            resultMap.put("KR", kr);
                            resultMap.put("Count", count);

                            // 중복 확인
                            if (!filteredResult.contains(resultMap)) {
                                filteredResult.add(resultMap);
                            }
                        }
                    }
                }
            }
        }

        // Country, Template, Language로 정렬
        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (String) r.get("Country"))
                .thenComparing(r -> (String) r.get("Template Name"))
                .thenComparing(r -> String.join(",", (List<String>) r.get("Language"))));

        // Count가 높은 순서로 정렬
        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (int) r.get("Count")).reversed());

        // WT가 높은 순서로 정렬
        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (double) r.get("WT")).reversed());

        // 결과 출력
        for (Map<String, Object> res : filteredResult) {
            log.info("Sort Result : Country({}), Language Code({}), Template Name({}), Word({}), Weight({}), KR({}), Count({})",
                    res.get("Country"), res.get("Language"), res.get("Template Name"), res.get("WD"), res.get("WT"), res.get("KR"), res.get("Count"));
        }

        List<String> countryType = new ArrayList<>();
        countryType.add("국가");

        List<String> languageCode = new ArrayList<>();
        languageCode.add("언어");

        List<String> documentType = new ArrayList<>();
        documentType.add("문서 양식");

        // 국가, 언어, 템플릿 조합별로 최상위 WT의 Count 합계를 계산
        Map<String, Double> maxWtMap = new HashMap<>();
        Map<String, Integer> templateCountSum = new HashMap<>();
        Map<String, Integer> nonMaxWtCountSum = new HashMap<>();
        Map<String, Double> matchWtSum = new HashMap<>();

//        for (Map<String, Object> res : filteredResult) {
//            String key = res.get("Country") + "|" + res.get("Language") + "|" + res.get("Template Name");
//            double weight = (double) res.get("WT");
//            int count = (int) res.get("Count");
//
//            // 현재 조합의 최대 WT를 갱신
//            if (!maxWtMap.containsKey(key) || weight > maxWtMap.get(key)) {
//                maxWtMap.put(key, weight);
//                templateCountSum.put(key, count);
//            } else if (weight == maxWtMap.get(key)) {
//                // 동일한 WT일 경우 Count를 누적
//                templateCountSum.put(key, templateCountSum.get(key) + count);
//            }
//        }

//        // 최상위 WT를 제외한 항목들의 count 합계를 구하는 코드
//        for (Map<String, Object> res : filteredResult) {
//            String key = res.get("Country") + "|" + res.get("Language") + "|" + res.get("Template Name");
//            double weight = (double) res.get("WT");
//            int count = (int) res.get("Count");
//
//            // 최상위 WT를 제외한 항목들의 count 합계 계산
//            if (weight != maxWtMap.get(key)) {
//                nonMaxWtCountSum.put(key, nonMaxWtCountSum.getOrDefault(key, 0) + count);
//            }
//
//            // 각 양식 별 일치 단어 WT 합계 계산
//            if (count > 0) {
//                matchWtSum.put(key, matchWtSum.getOrDefault(key, 0.0) + weight);
//            }
//        }

        // 최대 WT를 찾기
        double globalMaxWt = Double.MIN_VALUE;
        for (Map<String, Object> res : filteredResult) {
            double weight = (double) res.get("WT");
            if (weight > globalMaxWt) {
                globalMaxWt = weight;
            }
        }

        // 최대 WT에 대해서 조합별로 count를 계산
        for (Map<String, Object> res : filteredResult) {
            String key = res.get("Country") + "|" + res.get("Language") + "|" + res.get("Template Name");
            double weight = (double) res.get("WT");
            int count = (int) res.get("Count");

            if (weight == globalMaxWt) {
                // 최대 WT일 경우 Count를 누적
                templateCountSum.put(key, templateCountSum.getOrDefault(key, 0) + count);
            } else {
                // 최대 WT를 제외한 항목들의 count 합계 계산
                nonMaxWtCountSum.put(key, nonMaxWtCountSum.getOrDefault(key, 0) + count);
            }

            // 각 양식 별 일치 단어 WT 합계 계산
            if (count > 0) {
                matchWtSum.put(key, matchWtSum.getOrDefault(key, 0.0) + weight);
            }
        }

        log.info("Calculate the Count sum of the highest WT");
        for (Map.Entry<String, Integer> entry : templateCountSum.entrySet()) {
            log.info("Key: {}, Count Sum: {}", entry.getKey(), entry.getValue());
        }

        log.info("Calculate the Count sum of the non-max WT");
        for (Map.Entry<String, Integer> entry : nonMaxWtCountSum.entrySet()) {
            log.info("Non-Max WT Key: {}, Count Sum: {}", entry.getKey(), entry.getValue());
        }

        if (!templateCountSum.isEmpty()) {
            // templateCountSum 의 value 값이 가장 높은 항목 찾기
            String maxKey = null;
            int maxValue = Integer.MIN_VALUE;

            for (Map.Entry<String, Integer> entry : templateCountSum.entrySet()) {
                int value = entry.getValue();
                if (value > maxValue) {
                    maxKey = entry.getKey();
                    maxValue = value;
                } else if (value == maxValue) {
                    // 동일한 값이 존재하면 nonMaxWtCountSum 에서 비교
                    int nonMaxValue1 = nonMaxWtCountSum.getOrDefault(maxKey, 0);
                    int nonMaxValue2 = nonMaxWtCountSum.getOrDefault(entry.getKey(), 0);
                    if (nonMaxValue2 > nonMaxValue1) {
                        maxKey = entry.getKey();
                        maxValue = value;
                    }
                }
            }

            log.info("Max Key: {}, Max Value: {}", maxKey, maxValue);

            // 최종적으로 합계가 가장 높은 Template 찾기
            if (maxKey == null || maxKey.isEmpty()) {
                finalTopTemplate = "미분류";
                finalCountry = "미분류";
                finalLanguage = "미분류";
                finalMaxTotalCount = 0;
                log.info("No valid template found. Setting default value.");
            } else {
                String[] parts = maxKey.split("\\|");
                finalCountry = parts[0];
                finalLanguage = parts[1];
                finalTopTemplate = parts[2];
                finalMaxTotalCount = maxValue;
                totalWtSum = matchWtSum.getOrDefault(maxKey, 0.0);
            }
        } else {
            finalTopTemplate = "미분류";
            finalCountry = "미분류";
            finalLanguage = "미분류";
            log.info("templateCountSum is null or empty");
        }

        // WT 합계가 0.5 이하인 경우 Country 값을 "미분류"로 저장
        if (totalWtSum <= 0.5) {
            finalTopTemplate = "미분류";
            finalCountry = "미분류";
            finalLanguage = "미분류";
        }

        countryType.add(finalCountry);
        languageCode.add(finalLanguage);
        documentType.add(finalTopTemplate);

        resultList.add(countryType);
        resultList.add(languageCode);
        resultList.add(documentType);

        //log.info("Final template with highest adjusted WT sum count: {}, Count: {}", finalTopTemplate, finalMaxTotalCount);
        log.info("Document classification results: Country({}), Language Code({}), Document Type({}))", finalCountry, finalLanguage, finalTopTemplate);
    }

    public void classifyDocuments2(Map<String, List<Map<String, Object>>> jsonData, List<Map<String, Object>> items) {

        //정다혀 추가
        Map<String, List<Map<String, Object>>> formMatchedWords = new HashMap<>();

        filteredResult = new ArrayList<>();
        resultList = new ArrayList<>();

        String finalTopTemplate;
        int finalMaxTotalCount;

        String finalCountry = "";
        String finalLanguage = "";
        double totalWtSum = 0;

        for (Map.Entry<String, List<Map<String, Object>>> countryEntry : jsonData.entrySet()) {
            String countryName = countryEntry.getKey();
            List<Map<String, Object>> formList = countryEntry.getValue();

            for (Map<String, Object> formMap : formList) {
                String formName = (String) formMap.get("Template Name");
                List<String> languages = (List<String>) formMap.get("Language");

                // H-RULE
                List<Map<String, Object>> hRules = (List<Map<String, Object>>) formMap.get("H-RULE");
                if (hRules != null) {
                    for (Map<String, Object> hRule : hRules) {
                        String word = (String) hRule.get("WD");
                        Double weight = (Double) hRule.get("WT");
                        String kr = (String) hRule.get("KR");
                        if (word != null && weight != null) {
                            int count = 0;

                            // items를 순회하며 description과 일치하는 word의 개수를 카운트
                            for (Map<String, Object> item : items) {
                                String description = (String) item.get("description");

                                // 대소문자 구별 여부에 따른 비교
                                if (configLoader.checkCase) { // 대소문자 구별
                                    if (description != null && description.equals(word)) {
                                        count++;
                                        formMatchedWords.computeIfAbsent(formName, k -> new ArrayList<>()).add(item);
                                    }
                                } else { // 대소문자 구별하지 않음
                                    if (description != null && description.equalsIgnoreCase(word)) {
                                        count++;
                                        formMatchedWords.computeIfAbsent(formName, k -> new ArrayList<>()).add(item);
                                    }
                                }
                            }

                            Map<String, Object> resultMap = new HashMap<>();
                            resultMap.put("Country", countryName);
                            resultMap.put("Template Name", formName);
                            resultMap.put("Language", languages);
                            resultMap.put("WD", word);
                            resultMap.put("WT", weight);
                            resultMap.put("KR", kr);
                            resultMap.put("Count", count);

                            // 중복 확인
                            if (!filteredResult.contains(resultMap)) {
                                filteredResult.add(resultMap);
                            }
                        }
                    }
                }
            }
        }

        // Country, Template 으로 정렬
        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (String) r.get("Country"))
                .thenComparing(r -> (String) r.get("Template Name"))
                .thenComparing(r -> String.join(",", (List<String>) r.get("Language"))));

        // Count가 높은 순서로 정렬
        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (int) r.get("Count")).reversed());

        // WT가 높은 순서로 정렬
        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (double) r.get("WT")).reversed());

        // 결과 출력
        for (Map<String, Object> res : filteredResult) {
            log.info("Sort Result : Country({}), Language Code({}), Template Name({}), Word({}), Weight({}), KR({}), Count({})",
                    res.get("Country"), res.get("Language"), res.get("Template Name"), res.get("WD"), res.get("WT"), res.get("KR"), res.get("Count"));
        }

        List<String> countryType = new ArrayList<>();
        countryType.add("국가");

        List<String> languageCode = new ArrayList<>();
        languageCode.add("언어");

        List<String> documentType = new ArrayList<>();
        documentType.add("문서 양식");

        Map<String, Double> maxWtMap = new HashMap<>();
        Map<String, Integer> templateCountSum = new HashMap<>();
        Map<String, Integer> nonMaxWtCountSum = new HashMap<>();
        Map<String, Double> matchWtSum = new HashMap<>();

        // 최대 WT를 찾기
        double globalMaxWt = Double.MIN_VALUE;
        for (Map<String, Object> res : filteredResult) {
            double weight = (double) res.get("WT");
            if (weight > globalMaxWt) {
                globalMaxWt = weight;
            }
        }
        log.info("max weight: {}", globalMaxWt);

        // 최대 WT에 대해서 조합별로 count를 계산
        for (Map<String, Object> res : filteredResult) {
            String key = res.get("Country") + "|" + res.get("Language") + "|" + res.get("Template Name");
            double weight = (double) res.get("WT");
            int count = (int) res.get("Count");

            if (weight == globalMaxWt) {
                // 최대 WT일 경우 Count를 누적
                templateCountSum.put(key, templateCountSum.getOrDefault(key, 0) + count);
            } else {
                // 최대 WT를 제외한 항목들의 count 합계 계산
                nonMaxWtCountSum.put(key, nonMaxWtCountSum.getOrDefault(key, 0) + count);
            }

            // 각 양식 별 일치 단어 WT 합계 계산
            if (count > 0) {
                matchWtSum.put(key, matchWtSum.getOrDefault(key, 0.0) + weight);
            }
        }

        log.info("Calculate the Count sum of the highest WT");
        for (Map.Entry<String, Integer> entry : templateCountSum.entrySet()) {
            log.info("Key: {}, Count Sum: {}", entry.getKey(), entry.getValue());
        }

        log.info("Calculate the Count sum of the non-max WT");
        for (Map.Entry<String, Integer> entry : nonMaxWtCountSum.entrySet()) {
            log.info("Non-Max WT Key: {}, Count Sum: {}", entry.getKey(), entry.getValue());
        }

        if (!templateCountSum.isEmpty()) {
            // templateCountSum 의 value 값이 가장 높은 항목 찾기
            String maxKey = null;
            int maxValue = Integer.MIN_VALUE;

            for (Map.Entry<String, Integer> entry : templateCountSum.entrySet()) {
                int value = entry.getValue();
                if (value > maxValue) {
                    maxKey = entry.getKey();
                    maxValue = value;
                } else if (value == maxValue) {
                    // 동일한 값이 존재하면 nonMaxWtCountSum 에서 비교
                    int nonMaxValue1 = nonMaxWtCountSum.getOrDefault(maxKey, 0);
                    int nonMaxValue2 = nonMaxWtCountSum.getOrDefault(entry.getKey(), 0);
                    if (nonMaxValue2 > nonMaxValue1) {
                        maxKey = entry.getKey();
                        maxValue = value;
                    }
                }
            }

            log.info("Max Key: {}, Max Value: {}", maxKey, maxValue);

            // 최종적으로 합계가 가장 높은 Template 찾기
            if (maxKey == null || maxKey.isEmpty()) {
                finalTopTemplate = "미분류";
                finalCountry = "미분류";
                finalLanguage = "미분류";
                finalMaxTotalCount = 0;
                log.info("No valid template found. Setting default value.");
            } else {
                String[] parts = maxKey.split("\\|");
                finalCountry = parts[0];
                finalLanguage = parts[1];
                finalTopTemplate = parts[2];
                finalMaxTotalCount = maxValue;
                totalWtSum = matchWtSum.getOrDefault(maxKey, 0.0);
            }
        } else {
            finalTopTemplate = "미분류";
            finalCountry = "미분류";
            finalLanguage = "미분류";
            log.info("templateCountSum is null or empty");
        }

        // WT 합계가 0.5 이하인 경우 Country 값을 "미분류"로 저장
        if (totalWtSum <= 0.5) {
            finalTopTemplate = "미분류";
            finalCountry = "미분류";
            finalLanguage = "미분류";
        }

        if (finalTopTemplate.equals("미분류")) {
            matchjsonWord = new ArrayList<>();
        } else {
            matchjsonWord = formMatchedWords.getOrDefault(finalTopTemplate, new ArrayList<>());
        }

        countryType.add(finalCountry);
        languageCode.add(finalLanguage);
        documentType.add(finalTopTemplate);

        resultList.add(countryType);
        resultList.add(languageCode);
        resultList.add(documentType);

        //log.info("Final template with highest adjusted WT sum count: {}, Count: {}", finalTopTemplate, finalMaxTotalCount);
        log.info("Document classification results: Country({}), Language Code({}), Document Type({}))", finalCountry, finalLanguage, finalTopTemplate);
    }

    // 단어 카운트
    public int countOccurrences(String input, String word) {
        if (word.isEmpty()) {
            return 0;
        }
        if (!configLoader.checkCase) {
            input = input.toLowerCase();
            word = word.toLowerCase();
        }
        String[] parts = input.split(Pattern.quote(word));
        return parts.length - 1;
    }

    //정다현 추가 내용
    public void classifyDocuments3(Map<String, List<Map<String, Object>>> jsonData, List<Map<String, Object>> items) {
        //정다혀 추가
        Map<String, List<Map<String, Object>>> formMatchedWords = new HashMap<>();

        filteredResult = new ArrayList<>();
        resultList = new ArrayList<>();

        String finalTopTemplate;
        int finalMaxTotalCount;

        String finalCountry = "";
        String finalLanguage = "";
        double totalWtSum = 0;

        for (Map.Entry<String, List<Map<String, Object>>> countryEntry : jsonData.entrySet()) {
            String countryName = countryEntry.getKey();
            List<Map<String, Object>> formList = countryEntry.getValue();

            for (Map<String, Object> formMap : formList) {
                String formName = (String) formMap.get("Template Name");
                List<String> languages = (List<String>) formMap.get("Language");

                // H-RULE
                List<Map<String, Object>> hRules = (List<Map<String, Object>>) formMap.get("H-RULE");
                for (Map<String, Object> hRule : hRules) {
                    String word = (String) hRule.get("WD");
                    double weight = (double) hRule.get("WT");
                    String kr = (String) hRule.get("KR");
                    int count = 0;

                    // items를 순회하며 description과 일치하는 word의 개수를 카운트
                    for (Map<String, Object> item : items) {
                        String description = (String) item.get("description");

                        // 대소문자 구별 여부에 따른 비교
                        if (configLoader.checkCase) { // 대소문자 구별
                            if (description != null && description.equals(word)) {
                                count++;
                                formMatchedWords.computeIfAbsent(formName, k -> new ArrayList<>()).add(item);
                            }
                        } else { // 대소문자 구별하지 않음
                            if (description != null && description.equalsIgnoreCase(word)) {
                                count++;
                                formMatchedWords.computeIfAbsent(formName, k -> new ArrayList<>()).add(item);
                            }
                        }
                    }

                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("Country", countryName);
                    resultMap.put("Template Name", formName);
                    resultMap.put("Language", languages);
                    resultMap.put("WD", word);
                    resultMap.put("WT", weight);
                    resultMap.put("KR", kr);
                    resultMap.put("Count", count);

                    // 중복 확인
                    if (!filteredResult.contains(resultMap)) {
                        filteredResult.add(resultMap);
                    }
                }
            }
        }

        // Country, Template 으로 정렬
        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (String) r.get("Country"))
                .thenComparing(r -> (String) r.get("Template Name"))
                .thenComparing(r -> String.join(",", (List<String>) r.get("Language"))));

        // Count가 높은 순서로 정렬
        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (int) r.get("Count")).reversed());

        // WT가 높은 순서로 정렬
        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (double) r.get("WT")).reversed());

        // 결과 출력
        for (Map<String, Object> res : filteredResult) {
            log.info("Sort Result : Country({}), Language Code({}), Template Name({}), Word({}), Weight({}), KR({}), Count({})",
                    res.get("Country"), res.get("Language"), res.get("Template Name"), res.get("WD"), res.get("WT"), res.get("KR"), res.get("Count"));
        }

        List<String> countryType = new ArrayList<>();
        countryType.add("국가");
        List<String> languageCode = new ArrayList<>();
        languageCode.add("언어");
        List<String> documentType = new ArrayList<>();
        documentType.add("문서 양식");

        Map<String, Double> maxWtMap = new HashMap<>();
        Map<String, Integer> templateCountSum = new HashMap<>();
        Map<String, Integer> nonMaxWtCountSum = new HashMap<>();
        Map<String, Double> matchWtSum = new HashMap<>();

        // 최대 WT를 찾기
        double globalMaxWt = Double.MIN_VALUE;
        for (Map<String, Object> res : filteredResult) {
            double weight = (double) res.get("WT");
            if (weight > globalMaxWt) {
                globalMaxWt = weight;
            }
        }

        // 최대 WT에 대해서 조합별로 count를 계산
        for (Map<String, Object> res : filteredResult) {
            String key = res.get("Country") + "|" + res.get("Language") + "|" + res.get("Template Name");
            double weight = (double) res.get("WT");
            int count = (int) res.get("Count");

            if (weight == globalMaxWt) {
                // 최대 WT일 경우 Count를 누적
                templateCountSum.put(key, templateCountSum.getOrDefault(key, 0) + count);
            } else {
                // 최대 WT를 제외한 항목들의 count 합계 계산
                nonMaxWtCountSum.put(key, nonMaxWtCountSum.getOrDefault(key, 0) + count);
            }

            // 각 양식 별 일치 단어 WT 합계 계산
            if (count > 0) {
                matchWtSum.put(key, matchWtSum.getOrDefault(key, 0.0) + weight);
            }
        }

        log.info("Calculate the Count sum of the highest WT");
        for (Map.Entry<String, Integer> entry : templateCountSum.entrySet()) {
            log.info("Key: {}, Count Sum: {}", entry.getKey(), entry.getValue());
        }

        log.info("Calculate the Count sum of the non-max WT");
        for (Map.Entry<String, Integer> entry : nonMaxWtCountSum.entrySet()) {
            log.info("Non-Max WT Key: {}, Count Sum: {}", entry.getKey(), entry.getValue());
        }

        if (!templateCountSum.isEmpty()) {
            // templateCountSum 의 value 값이 가장 높은 항목 찾기
            String maxKey = null;
            int maxValue = Integer.MIN_VALUE;

            for (Map.Entry<String, Integer> entry : templateCountSum.entrySet()) {
                int value = entry.getValue();
                if (value > maxValue) {
                    maxKey = entry.getKey();
                    maxValue = value;
                } else if (value == maxValue) {
                    // 동일한 값이 존재하면 nonMaxWtCountSum 에서 비교
                    int nonMaxValue1 = nonMaxWtCountSum.getOrDefault(maxKey, 0);
                    int nonMaxValue2 = nonMaxWtCountSum.getOrDefault(entry.getKey(), 0);
                    if (nonMaxValue2 > nonMaxValue1) {
                        maxKey = entry.getKey();
                        maxValue = value;
                    }
                }
            }

            log.info("Max Key: {}, Max Value: {}", maxKey, maxValue);

            // 최종적으로 합계가 가장 높은 Template 찾기
            if (maxKey == null || maxKey.isEmpty()) {
                finalTopTemplate = "미분류";
                finalCountry = "미분류";
                finalLanguage = "미분류";
                finalMaxTotalCount = 0;
                log.info("No valid template found. Setting default value.");
            } else {
                String[] parts = maxKey.split("\\|");
                finalCountry = parts[0];
                finalLanguage = parts[1];
                finalTopTemplate = parts[2];
                finalMaxTotalCount = maxValue;
                totalWtSum = matchWtSum.getOrDefault(maxKey, 0.0);
            }
        } else {
            finalTopTemplate = "미분류";
            finalCountry = "미분류";
            finalLanguage = "미분류";
            log.info("templateCountSum is null or empty");
        }

        // WT 합계가 0.5 이하인 경우 Country 값을 "미분류"로 저장
        if (totalWtSum <= 0.5) {
            finalTopTemplate = "미분류";
            finalCountry = "미분류";
            finalLanguage = "미분류";
        }

        if (finalTopTemplate.equals("미분류")) {
            matchjsonWord = new ArrayList<>();
        } else {
            matchjsonWord = formMatchedWords.getOrDefault(finalTopTemplate, new ArrayList<>());
        }

        log.debug("가장 일치 단어 개수가 많은 양식: '{}'", finalTopTemplate);
        log.debug("일치하는 단어와 좌표: {}", matchjsonWord);

        countryType.add(finalCountry);
        languageCode.add(finalLanguage);
        documentType.add(finalTopTemplate);

        resultList.add(countryType);
        resultList.add(languageCode);
        resultList.add(documentType);

        log.info("Document classification results: Country({}), Language Code({}), Document Type({}))", finalCountry, finalLanguage, finalTopTemplate);
    }

    // 이전 코드 cd1, cd2, cd3
//    public void classifyDocuments1(Map<String, List<List<String[]>>> jsonData, String jsonLocale, String jsonDescription) {
//        int maxMatches = 0;
//        int matchIndex = -1;
//        String matchCountry = "";
//
//        double maxWeight = 0;
//        int weightIndex = -1;
//
//        resultList = new ArrayList<>();
//        resultWord = new ArrayList<>();
//
//        resultWeightOneWord = new ArrayList<>();
//        //List<String> matchedCountries = new ArrayList<>();
//
//        for (String countryName : jsonData.keySet()) {
//            List<List<String[]>> forms = jsonData.get(countryName);
//
//            if (forms == null) {
//                log.info("No matching country for {}", countryName);
//                continue;
//            }
//
//            for (int col = 0; col < forms.size(); col++) {
//                List<String[]> form = forms.get(col);
//
//                double addWeight = 0;
//                int addWeightOneValue = 0;
//                int matches = 0;
//                int weightOneCount = 0;
//                List<String> matchingValues = new ArrayList<>();
//                List<String> weightOneValues = new ArrayList<>();
//
//                matchingValues.add(form.get(0)[0]);
//                weightOneValues.add(form.get(0)[0]);
//
//                int cnt = 0;
//                for (int i = 1; i < form.size(); i++) {
//                    String value = form.get(i)[0];
//                    double weight = Double.parseDouble(form.get(i)[1]);
//
//                    if (jsonDescription.contains(value)) {
//                        addWeight += weight;
//
//                        matches++;
//                        cnt = countOccurrences(jsonDescription, value);
//                        matchingValues.add(value + "(" + cnt + ")");
//
//                        if (configLoader.weightCountFlag) {
//                            if (weight == 1.0) {
//                                weightOneValues.add(value + "(" + cnt + ")");
//                                addWeightOneValue += cnt;
//                            }
//                        }
//                    } else {
//                        cnt = 0;
//                    }
//                    log.info("'{}' - MC: {}, WT: {}", value, cnt, Double.parseDouble(form.get(i)[1]));
//                }
//
//                log.info("'{}' Document Type - Number of matched words with weight 1: {}", form.get(0)[0], addWeightOneValue);
//                log.info("'{}' Document Type - Matched words with weight 1 result: {}", form.get(0)[0], weightOneValues.subList(1, weightOneValues.size()));
//                log.info("'{}' Document Type - Number of matching word: {}/{}", form.get(0)[0], matches, form.size() - 1);
//                log.info("'{}' Document Type - Weight sum: {}", form.get(0)[0], addWeight);
//                log.info("'{}' Document Type - Match result: {}", form.get(0)[0], matches != 0 ? matchingValues.subList(1, matchingValues.size()) : "");
//
//                matchingValues.add(matches + ""); // 매치 단어 수 결과 리스트에 추가
//                resultWord.add(matchingValues);
//
//                weightOneValues.add(addWeightOneValue + "");
//                resultWeightOneWord.add(weightOneValues);
//
//                if (matches == maxMatches) {
//                    if (addWeight > maxWeight) {
//                        maxMatches = matches;
//                        maxWeight = addWeight;
//                        matchIndex = col;
//                        weightIndex = col;
//                        matchCountry = countryName;
//                    }
//                } else if (matches > maxMatches) {
//                    maxMatches = matches;
//                    matchIndex = col;
//                    matchCountry = countryName;
//                }
//
//                if (addWeight > maxWeight) {
//                    maxWeight = addWeight;
//                    weightIndex = col;
//                }
//            }
//        }
//
//        if (matchIndex == weightIndex) {
//            log.info("Word match results and weight comparison results match");
//        } else {
//            log.info("Discrepancies between word match results and weight comparison results");
//        }
//
//        List<String> countryType = new ArrayList<>();
//        countryType.add("국가");
//
//        List<String> languageCode = new ArrayList<>();
//        languageCode.add("언어");
//
//        List<String> documentType = new ArrayList<>();
//        documentType.add("문서 양식");
//
//        if (matchIndex == -1 || weightIndex == -1) {
//            log.info("Unclassified File, Reason: No document matching classification result");
//            countryType.add("미분류");
//            languageCode.add("미분류");
//            documentType.add("미분류");
//        } else if (maxWeight <= 0.5) {
//            log.info("Unclassified File, Reason: Underweight");
//            countryType.add("미분류");
//            languageCode.add("미분류");
//            documentType.add("미분류");
//        } else {
//            log.info("Document classification results: Country({}), Language Code({}), Document Type({}), Weight({})", matchCountry, jsonData.get(matchCountry).get(matchIndex).get(0)[1], jsonData.get(matchCountry).get(matchIndex).get(0)[0], maxWeight);
//            countryType.add(matchCountry);
//            languageCode.add(jsonData.get(matchCountry).get(matchIndex).get(0)[1] + " (" + jsonData.get(matchCountry) + ")");
//            documentType.add(jsonData.get(matchCountry).get(matchIndex).get(0)[0]);
//        }
//        resultList.add(countryType);
//        resultList.add(languageCode);
//        resultList.add(documentType);
//
//        log.info("Excel Data Results: {}", resultList);
//        docType = resultList.get(1).get(1);
//    }
//
//    public void classifyDocuments2(Map<String, List<List<String[]>>> jsonData, String jsonLocale, List<Map<String, Object>> items) {
//        //정다혀 추가
//        Map<String, List<Map<String, Object>>> formMatchedWords = new HashMap<>();
//
//        int maxMatches = 0;
//        String matchCountry = "";
//        String matchLanguage = "";
//
//        double maxWeight = 0;
//
//        resultList = new ArrayList<>();
//        resultWord = new ArrayList<>();
//        resultWeightOneWord = new ArrayList<>();
//
//        // null 처리
//
//        Map<String, Integer> matchCount = new HashMap<>();
//        Map<String, Double> weightMap = new HashMap<>();
//        Map<String, Double> formWeightSum = new HashMap<>();
//        Map<String, Integer> formMatchCount = new HashMap<>();
//
//        String formWithMostMatches = null;
//        String formWithMostWeights = null;
//
//        for (String countryName : jsonData.keySet()) {
//            List<List<String[]>> forms = jsonData.get(countryName);
//
//            if (forms == null) {
//                log.info("No matching country for {}", countryName);
//                continue;
//            }
//
//            for (List<String[]> form : forms) {
//                int totalMatches = 0; // 전체 매치된 단어의 수
//                int addWeightOneValue = 0;
//                String formName = form.get(0)[0];
//                String formLanguage = form.get(0)[1];
//                formWeightSum.put(formName, 0.0);
//                formMatchCount.put(formName, 0);
//
//                for (int i = 1; i < form.size(); i++) {
//                    String word = form.get(i)[0];
//                    double weight = Double.parseDouble(form.get(i)[1]);
//                    weightMap.put(word, weight);
//                    matchCount.put(word, 0);
//                }
//
//                List<String> matchingValues = new ArrayList<>();
//                List<String> weightOneValues = new ArrayList<>();
//
//                matchingValues.add(form.get(0)[0]);
//                weightOneValues.add(form.get(0)[0]);
//
//                for (Map<String, Object> item : items) {
//                    String description = (String) item.get("description");
//
//                    for (int j = 1; j < form.size(); j++) {
//                        if (description.equals(form.get(j)[0])) {
//                            matchCount.put(description, matchCount.get(description) + 1);
//                            formWeightSum.put(formName, formWeightSum.get(formName) + weightMap.get(description));
//                            formMatchCount.put(formName, formMatchCount.get(formName) + 1);
//                            totalMatches++; // 전체 매치 수 증가
//                            formMatchedWords.computeIfAbsent(formName, k -> new ArrayList<>()).add(item);
//                        }
//                    }
//                }
//
//                for (int i = 1; i < form.size(); i++) {
//                    String word = form.get(i)[0];
//                    int count = matchCount.get(word);
//                    double weight = weightMap.get(word);
//
//                    if (count >= 1) {
//                        matchingValues.add(word + "(" + count + ")");
//
//                        if (configLoader.weightCountFlag) {
//                            if (weight == 1.0) {
//                                weightOneValues.add(word + "(" + count + ")");
//                                addWeightOneValue += count;
//                            }
//                        }
//                    }
//                    log.info("'{}' - MC: {}, WT: {}", word, count, weight);
//                }
//
//                int totalWords = form.size() - 1;
//                int matchedWords = formMatchCount.get(formName);
//                double totalWeight = formWeightSum.get(formName);
//                log.info("'{}' Document Type - Number of matched words with weight 1: {}", formName, addWeightOneValue);
//                log.info("'{}' Document Type - Matched words with weight 1 result: {}", formName, matchingValues);
//                log.info("'{}' Document Type - Number of matching word: {}/{}", formName, matchedWords, totalWords);
//                log.info("'{}' Document Type - Weight sum: {}", formName, totalWeight);
//                log.info("'{}' Document Type - Match result: {}", formName, matchedWords != 0 ? matchingValues : "");
//
//                matchingValues.add(totalMatches + ""); // 매치 단어 수 결과 리스트에 추가
//                resultWord.add(matchingValues);
//
//                weightOneValues.add(addWeightOneValue + "");
//                resultWeightOneWord.add(weightOneValues);
//
//                if (matchedWords == maxMatches) {
//                    if (totalWeight > maxWeight) {
//                        maxMatches = matchedWords;
//                        maxWeight = totalWeight;
//                        formWithMostMatches = formName;
//                        formWithMostWeights = formName;
//                        matchLanguage = formLanguage;
//                        matchCountry = countryName;
//                    }
//                } else if (matchedWords > maxMatches) {
//                    maxMatches = matchedWords;
//                    formWithMostMatches = formName;
//                    matchLanguage = formLanguage;
//                    matchCountry = countryName;
//                }
//
//                if (totalWeight > maxWeight) {
//                    maxWeight = totalWeight;
//                    formWithMostWeights = formName;
//                }
//            }
//        }
//        if (formWithMostMatches != null) {
//            matchjsonWord = formMatchedWords.getOrDefault(formWithMostMatches, new ArrayList<>());
//        } else {
//            matchjsonWord = new ArrayList<>(); // 일치하는 양식이 없을 경우 빈 리스트
//        }
//
//        log.debug("가장 일치 단어 개수가 많은 양식: '{}', 일치 단어 수: {}", formWithMostMatches, maxMatches);
//        log.debug("일치하는 단어와 좌표: {}", matchjsonWord);
//
//
////        if (matchIndex == weightIndex) {
////            log.info("단어 매치 결과와 가중치 비교 결과 일치");
////        } else {
////            log.info("단어 매치 결과와 가중치 비교 결과 불일치");
////        }
//
//        List<String> countryType = new ArrayList<>();
//        countryType.add("국가");
//
//        List<String> languageCode = new ArrayList<>();
//        languageCode.add("언어");
//
//        List<String> documentType = new ArrayList<>();
//        documentType.add("문서 양식");
//
//        if (formWithMostMatches == null || formWithMostWeights == null) {
//            log.info("Unclassified File, Reason: No document matching classification result");
//            countryType.add("미분류");
//            languageCode.add("미분류");
//            documentType.add("미분류");
//        } else if (maxWeight <= 0.5) {
//            log.info("Unclassified File, Reason: Underweight");
//            countryType.add("미분류");
//            languageCode.add("미분류");
//            documentType.add("미분류");
//        } else {
//            log.info("Document classification results: Country({}), Language Code({}), Document Type({}), Weight({})", matchCountry, matchLanguage, formWithMostMatches, maxWeight);
//            countryType.add(matchCountry);
//            languageCode.add(matchLanguage + " (" + matchCountry + ")");
//            documentType.add(formWithMostMatches);
//        }
//
//        resultList.add(countryType);
//        resultList.add(languageCode);
//        resultList.add(documentType);
//
//        log.info("Excel Data Results: {}", resultList);
//        docType = resultList.get(1).get(1);
//    }
//
//    public void classifyDocuments3(Map<String, List<List<String[]>>> jsonData, String jsonLocale, List<Map<String, Object>> items) {
//        //정다현 추가
////        List<Map<String, Object>> matchjsonWord2 = new ArrayList<>();
//        //정다혀 추가
//        Map<String, List<Map<String, Object>>> formMatchedWords = new HashMap<>();
//
//        int maxMatches = 0;
//        String matchCountry = "";
//        String matchLanguage = "";
//
//        double maxWeight = 0;
//
//        resultList = new ArrayList<>();
//        resultWord = new ArrayList<>();
//        resultWeightOneWord = new ArrayList<>();
//
//        // null 처리
//
//        Map<String, Integer> matchCount = new HashMap<>();
//        Map<String, Double> weightMap = new HashMap<>();
//        Map<String, Double> formWeightSum = new HashMap<>();
//        Map<String, Integer> formMatchCount = new HashMap<>();
//
//        String formWithMostMatches = null;
//        String formWithMostWeights = null;
//
//        for (String countryName : jsonData.keySet()) {
//            List<List<String[]>> forms = jsonData.get(countryName);
//
//            if (forms == null) {
//                log.info("No matching country for {}", countryName);
//                continue;
//            }
//
//            for (List<String[]> form : forms) {
//                int totalMatches = 0; // 전체 매치된 단어의 수
//                int addWeightOneValue = 0;
//                String formName = form.get(0)[0];
//                String formLanguage = form.get(0)[1];
//                formWeightSum.put(formName, 0.0);
//                formMatchCount.put(formName, 0);
//                formMatchedWords.put(formName, new ArrayList<>()); // 여기서 빈 리스트를 초기화
//
//                for (int i = 1; i < form.size(); i++) {
//                    String word = form.get(i)[0];
//                    double weight = Double.parseDouble(form.get(i)[1]);
//                    weightMap.put(word, weight);
//                    matchCount.put(word, 0);
//                }
//
//                List<String> matchingValues = new ArrayList<>();
//                List<String> weightOneValues = new ArrayList<>();
//
//                matchingValues.add(form.get(0)[0]);
//                weightOneValues.add(form.get(0)[0]);
//
//                for (Map<String, Object> item : items) {
//                    String description = (String) item.get("description");
//
//                    for (int j = 1; j < form.size(); j++) {
//                        if (description.equals(form.get(j)[0])) {
//                            matchCount.put(description, matchCount.get(description) + 1);
//                            formWeightSum.put(formName, formWeightSum.get(formName) + weightMap.get(description));
//                            formMatchCount.put(formName, formMatchCount.get(formName) + 1);
//                            totalMatches++; // 전체 매치 수 증가
//                            formMatchedWords.computeIfAbsent(formName, k -> new ArrayList<>()).add(item);
//                        }
//                    }
//                }
//
//                for (int i = 1; i < form.size(); i++) {
//                    String word = form.get(i)[0];
//                    int count = matchCount.get(word);
//                    double weight = weightMap.get(word);
//
//                    if (count >= 1) {
//                        matchingValues.add(word + "(" + count + ")");
//
//                        if (configLoader.weightCountFlag) {
//                            if (weight == 1.0) {
//                                weightOneValues.add(word + "(" + count + ")");
//                                addWeightOneValue += count;
//                            }
//                        }
//                    }
//                    log.info("'{}' - MC: {}, WT: {}", word, count, weight);
//                }
//
//                int totalWords = form.size() - 1;
//                int matchedWords = formMatchCount.get(formName);
//                double totalWeight = formWeightSum.get(formName);
//                log.info("'{}' Document Type - Number of matched words with weight 1: {}", formName, addWeightOneValue);
//                log.info("'{}' Document Type - Matched words with weight 1 result: {}", formName, matchingValues);
//                log.info("'{}' Document Type - Number of matching word: {}/{}", formName, matchedWords, totalWords);
//                log.info("'{}' Document Type - Weight sum: {}", formName, totalWeight);
//                log.info("'{}' Document Type - Match result: {}", formName, matchedWords != 0 ? matchingValues : "");
//
//                matchingValues.add(totalMatches + ""); // 매치 단어 수 결과 리스트에 추가
//                resultWord.add(matchingValues);
//
//                weightOneValues.add(addWeightOneValue + "");
//                resultWeightOneWord.add(weightOneValues);
//
//                if (matchedWords == maxMatches) {
//                    if (totalWeight > maxWeight) {
//                        maxMatches = matchedWords;
//                        maxWeight = totalWeight;
//                        formWithMostMatches = formName;
//                        formWithMostWeights = formName;
//                        matchLanguage = formLanguage;
//                        matchCountry = countryName;
//                    }
//                } else if (matchedWords > maxMatches) {
//                    maxMatches = matchedWords;
//                    formWithMostMatches = formName;
//                    matchLanguage = formLanguage;
//                    matchCountry = countryName;
//                }
//
//                if (totalWeight > maxWeight) {
//                    maxWeight = totalWeight;
//                    formWithMostWeights = formName;
//                }
//            }
//        }
//        if (formWithMostMatches != null) {
//            matchjsonWord = formMatchedWords.getOrDefault(formWithMostMatches, new ArrayList<>());
//        } else {
//            matchjsonWord = new ArrayList<>(); // 일치하는 양식이 없을 경우 빈 리스트
//        }
//
//        log.debug("가장 일치 단어 개수가 많은 양식: '{}', 일치 단어 수: {}", formWithMostMatches, maxMatches);
//        log.debug("일치하는 단어와 좌표: {}", matchjsonWord);
//
//        List<String> countryType = new ArrayList<>();
//        countryType.add("국가");
//
//        List<String> languageCode = new ArrayList<>();
//        languageCode.add("언어");
//
//        List<String> documentType = new ArrayList<>();
//        documentType.add("문서 양식");
//
//        if (formWithMostMatches == null || formWithMostWeights == null) {
//            log.info("Unclassified File: {}", items);
//            countryType.add("미분류");
//            languageCode.add("미분류");
//            documentType.add("미분류");
//        } else if (maxWeight <= 0.5) {
//            log.info("Unclassified File, Reason: Underweight");
//            countryType.add("미분류");
//            languageCode.add("미분류");
//            documentType.add("미분류");
//        } else {
//            log.info("Document classification results: Country({}), Language Code({}), Document Type({}), Weight({})", matchCountry, matchLanguage, formWithMostMatches, maxWeight);
//            countryType.add(matchCountry);
//            //countryType.add(wordLocal);
//            languageCode.add(matchLanguage + " (" + matchCountry + ")");
//            documentType.add(formWithMostMatches);
//        }
//
//        resultList.add(countryType);
//        resultList.add(languageCode);
//        resultList.add(documentType);
//
//        log.debug("Excel Data Results: {}", resultList);
//        docType = resultList.get(1).get(1);
//
////            matchjsonWord = matchjsonWord2;
////            log.debug("filteredMatchJsonWord2 : {}", filteredMatchJsonWord2);
//
//    }

    public void classifyDocuments_B1(Map<String, List<Map<String, Object>>> jsonData, String jsonDescription) {
        filteredResult = new ArrayList<>();
        resultList = new ArrayList<>();

        String finalTopTemplate = "미분류";
        String finalCountry = "미분류";
        String finalLanguage = "미분류";

        for (Map.Entry<String, List<Map<String, Object>>> countryEntry : jsonData.entrySet()) {
            String countryName = countryEntry.getKey();
            List<Map<String, Object>> formList = countryEntry.getValue();

            for (Map<String, Object> formMap : formList) {
                String formName = (String) formMap.get("Template Name");
                List<String> languages = (List<String>) formMap.get("Language");

                // H-RULE
                List<Map<String, Object>> hRules = (List<Map<String, Object>>) formMap.get("H-RULE");
                if (hRules != null) {
                    for (Map<String, Object> hRule : hRules) {
                        String word = (String) hRule.get("WD");
                        Double weight = (Double) hRule.get("WT");
                        String kr = (String) hRule.get("KR");
                        if (word != null && weight != null) {
                            int count = countOccurrences(jsonDescription, word);

                            Map<String, Object> resultMap = new HashMap<>();
                            resultMap.put("Country", countryName);
                            resultMap.put("Template Name", formName);
                            resultMap.put("Language", languages);
                            resultMap.put("WD", word);
                            resultMap.put("WT", weight);
                            resultMap.put("KR", kr);
                            resultMap.put("Count", count);

                            // 중복 확인
                            if (!filteredResult.contains(resultMap)) {
                                filteredResult.add(resultMap);
                            }
                        }
                    }
                }
            }
        }

        // Country, Template 으로 정렬
        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (String) r.get("Country"))
                .thenComparing(r -> (String) r.get("Template Name")));

        // Count가 높은 순서로 정렬
        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (int) r.get("Count")).reversed());

        // WT가 높은 순서로 정렬
        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (double) r.get("WT")).reversed());

        // 결과 출력
        for (Map<String, Object> res : filteredResult) {
            log.info("Sort Result : Country({}), Language Code({}), Template Name({}), Word({}), Weight({}), KR({}), Count({})",
                    res.get("Country"), res.get("Language"), res.get("Template Name"), res.get("WD"), res.get("WT"), res.get("KR"), res.get("Count"));
        }

        // 일치 단어 중 가중치 ~(config로 설정) 이상인 단어 count에 상관 없이 1회만 합계 구하기
        List<String> countryType = new ArrayList<>();
        countryType.add("국가");

        List<String> languageCode = new ArrayList<>();
        languageCode.add("언어");

        List<String> documentType = new ArrayList<>();
        documentType.add("문서 양식");

        Map<String, Double> weightSum;
        try {
            weightSum = sumFilteredWeights();
        } catch (Exception e) {
            log.error("Error in sumFilteredWeights: ", e);
            weightSum = new HashMap<>();
        }

        double maxTotalWeight = 0;

        try {
            for (Map.Entry<String, Double> entry : weightSum.entrySet()) {
                if (entry.getValue() > maxTotalWeight) {
                    maxTotalWeight = entry.getValue();
                    String[] parts = entry.getKey().split("\\|");
                    finalCountry = parts[0];
                    finalTopTemplate = parts[1];
                    finalLanguage = parts[2];
                }
            }
        } catch (Exception e) {
            log.error("Error processing weightSum entries: ", e);
            finalCountry = "미분류";
            finalTopTemplate = "미분류";
            finalLanguage = "미분류";
        }

        countryType.add(finalCountry);
        languageCode.add(finalLanguage);
        documentType.add(finalTopTemplate);

        resultList.add(countryType);
        resultList.add(languageCode);
        resultList.add(documentType);

        //log.info("Final template with highest adjusted WT sum count: {}, Count: {}", finalTopTemplate, finalMaxTotalCount);
        log.info("Document classification results (B1 version): Country({}), Language Code({}), Document Type({}), maxTotalWeight({}))", finalCountry, finalLanguage, finalTopTemplate, maxTotalWeight);
    }

    public void classifyDocuments_B2(Map<String, List<Map<String, Object>>> jsonData, List<Map<String, Object>> items) {

        //정다혀 추가
        Map<String, List<Map<String, Object>>> formMatchedWords = new HashMap<>();

        filteredResult = new ArrayList<>();
        resultList = new ArrayList<>();

        String finalTopTemplate = "미분류";
        String finalCountry = "미분류";
        String finalLanguage = "미분류";

        for (Map.Entry<String, List<Map<String, Object>>> countryEntry : jsonData.entrySet()) {
            String countryName = countryEntry.getKey();
            List<Map<String, Object>> formList = countryEntry.getValue();

            for (Map<String, Object> formMap : formList) {
                String formName = (String) formMap.get("Template Name");
                List<String> languages = (List<String>) formMap.get("Language");

                // H-RULE
                List<Map<String, Object>> hRules = (List<Map<String, Object>>) formMap.get("H-RULE");
                if (hRules != null) {
                    for (Map<String, Object> hRule : hRules) {
                        String word = (String) hRule.get("WD");
                        Double weight = (Double) hRule.get("WT");
                        String kr = (String) hRule.get("KR");
                        if (word != null && weight != null) {
                            int count = 0;

                            // items를 순회하며 description과 일치하는 word의 개수를 카운트
                            for (Map<String, Object> item : items) {
                                String description = (String) item.get("description");

                                // 대소문자 구별 여부에 따른 비교
                                if (configLoader.checkCase) { // 대소문자 구별
                                    if (description != null && description.equals(word)) {
                                        count++;
                                        formMatchedWords.computeIfAbsent(formName, k -> new ArrayList<>()).add(item);
                                    }
                                } else { // 대소문자 구별하지 않음
                                    if (description != null && description.equalsIgnoreCase(word)) {
                                        count++;
                                        formMatchedWords.computeIfAbsent(formName, k -> new ArrayList<>()).add(item);
                                    }
                                }
                            }

                            Map<String, Object> resultMap = new HashMap<>();
                            resultMap.put("Country", countryName);
                            resultMap.put("Template Name", formName);
                            resultMap.put("Language", languages);
                            resultMap.put("WD", word);
                            resultMap.put("WT", weight);
                            resultMap.put("KR", kr);
                            resultMap.put("Count", count);

                            // 중복 확인
                            if (!filteredResult.contains(resultMap)) {
                                filteredResult.add(resultMap);
                            }
                        }
                    }
                }
            }
        }

        // Country, Template 으로 정렬
        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (String) r.get("Country"))
                .thenComparing(r -> (String) r.get("Template Name")));

        // Count가 높은 순서로 정렬
        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (int) r.get("Count")).reversed());

        // WT가 높은 순서로 정렬
        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (double) r.get("WT")).reversed());

        // 결과 출력
//        for (Map<String, Object> res : filteredResult) {
//            log.info("Sort Result : Country({}), Language Code({}), Template Name({}), Word({}), Weight({}), KR({}), Count({})",
//                    res.get("Country"), res.get("Language"), res.get("Template Name"), res.get("WD"), res.get("WT"), res.get("KR"), res.get("Count"));
//        }

        List<String> countryType = new ArrayList<>();
        countryType.add("국가");

        List<String> languageCode = new ArrayList<>();
        languageCode.add("언어");

        List<String> documentType = new ArrayList<>();
        documentType.add("문서 양식");

        Map<String, Double> weightSum;
        try {
            weightSum = sumFilteredWeights();
        } catch (Exception e) {
            log.error("Error in sumFilteredWeights: ", e);
            weightSum = new HashMap<>();
        }

        double maxTotalWeight = 0;

        try {
            for (Map.Entry<String, Double> entry : weightSum.entrySet()) {
                if (entry.getValue() > maxTotalWeight) {
                    maxTotalWeight = entry.getValue();
                    String[] parts = entry.getKey().split("\\|");
                    finalCountry = parts[0];
                    finalTopTemplate = parts[1];
                    finalLanguage = parts[2];
                }
            }
        } catch (Exception e) {
            log.error("Error processing weightSum entries: ", e);
            finalCountry = "미분류";
            finalTopTemplate = "미분류";
            finalLanguage = "미분류";
        }

        if (finalTopTemplate.equals("미분류")) {
            matchjsonWord = new ArrayList<>();
        } else {
            matchjsonWord = formMatchedWords.getOrDefault(finalTopTemplate, new ArrayList<>());
        }

        countryType.add(finalCountry);
        languageCode.add(finalLanguage);
        documentType.add(finalTopTemplate);

        resultList.add(countryType);
        resultList.add(languageCode);
        resultList.add(documentType);

        //log.info("Final template with highest adjusted WT sum count: {}, Count: {}", finalTopTemplate, finalMaxTotalCount);
        log.info("Document classification results (B2 version): Country({}), Language Code({}), Document Type({}), maxTotalWeight({}))", finalCountry, finalLanguage, finalTopTemplate, maxTotalWeight);
    }

    //정다현 추가 내용
    public void classifyDocuments_B3(Map<String, List<Map<String, Object>>> jsonData, List<Map<String, Object>> items) {
        //정다혀 추가
        Map<String, List<Map<String, Object>>> formMatchedWords = new HashMap<>();

        filteredResult = new ArrayList<>();
        resultList = new ArrayList<>();

        String finalTopTemplate = "미분류";
        String finalCountry = "미분류";
        String finalLanguage = "미분류";

        for (Map.Entry<String, List<Map<String, Object>>> countryEntry : jsonData.entrySet()) {
            String countryName = countryEntry.getKey();
            List<Map<String, Object>> formList = countryEntry.getValue();

            for (Map<String, Object> formMap : formList) {
                String formName = (String) formMap.get("Template Name");
                List<String> languages = (List<String>) formMap.get("Language");

                // H-RULE
                List<Map<String, Object>> hRules = (List<Map<String, Object>>) formMap.get("H-RULE");
                for (Map<String, Object> hRule : hRules) {
                    String word = (String) hRule.get("WD");
                    double weight = (double) hRule.get("WT");
                    String kr = (String) hRule.get("KR");
                    int count = 0;

                    // items를 순회하며 description과 일치하는 word의 개수를 카운트
                    for (Map<String, Object> item : items) {
                        String description = (String) item.get("description");

                        // 대소문자 구별 여부에 따른 비교
                        if (configLoader.checkCase) { // 대소문자 구별
                            if (description != null && description.equals(word)) {
                                count++;
                                formMatchedWords.computeIfAbsent(formName, k -> new ArrayList<>()).add(item);
                            }
                        } else { // 대소문자 구별하지 않음
                            if (description != null && description.equalsIgnoreCase(word)) {
                                count++;
                                formMatchedWords.computeIfAbsent(formName, k -> new ArrayList<>()).add(item);
                            }
                        }
                    }

                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("Country", countryName);
                    resultMap.put("Template Name", formName);
                    resultMap.put("Language", languages);
                    resultMap.put("WD", word);
                    resultMap.put("WT", weight);
                    resultMap.put("KR", kr);
                    resultMap.put("Count", count);

                    // 중복 확인
                    if (!filteredResult.contains(resultMap)) {
                        filteredResult.add(resultMap);
                    }
                }
            }
        }

        // Country, Template 으로 정렬
        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (String) r.get("Country"))
                .thenComparing(r -> (String) r.get("Template Name")));

        // Count가 높은 순서로 정렬
        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (int) r.get("Count")).reversed());

        // WT가 높은 순서로 정렬
        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (double) r.get("WT")).reversed());

        // 결과 출력
//        for (Map<String, Object> res : filteredResult) {
//            log.info("Sort Result : Country({}), Language Code({}), Template Name({}), Word({}), Weight({}), KR({}), Count({})",
//                    res.get("Country"), res.get("Language"), res.get("Template Name"), res.get("WD"), res.get("WT"), res.get("KR"), res.get("Count"));
//        }

        List<String> countryType = new ArrayList<>();
        countryType.add("국가");
        List<String> languageCode = new ArrayList<>();
        languageCode.add("언어");
        List<String> documentType = new ArrayList<>();
        documentType.add("문서 양식");

        Map<String, Double> weightSum;
        try {
            weightSum = sumFilteredWeights();
        } catch (Exception e) {
            log.error("Error in sumFilteredWeights: ", e);
            weightSum = new HashMap<>();
        }

        double maxTotalWeight = 0;

        try {
            for (Map.Entry<String, Double> entry : weightSum.entrySet()) {
                if (entry.getValue() > maxTotalWeight) {
                    maxTotalWeight = entry.getValue();
                    String[] parts = entry.getKey().split("\\|");
                    finalCountry = parts[0];
                    finalTopTemplate = parts[1];
                    finalLanguage = parts[2];
                }
            }
        } catch (Exception e) {
            log.error("Error processing weightSum entries: ", e);
            finalCountry = "미분류";
            finalTopTemplate = "미분류";
            finalLanguage = "미분류";
        }

        if (finalTopTemplate.equals("미분류")) {
            matchjsonWord = new ArrayList<>();
        } else {
            matchjsonWord = formMatchedWords.getOrDefault(finalTopTemplate, new ArrayList<>());
        }

        log.debug("결과 양식: '{}'", finalTopTemplate);
        log.debug("일치하는 단어와 좌표: {}", matchjsonWord);

        countryType.add(finalCountry);
        languageCode.add(finalLanguage);
        documentType.add(finalTopTemplate);

        resultList.add(countryType);
        resultList.add(languageCode);
        resultList.add(documentType);

        log.info("Document classification results (B3 version): Country({}), Language Code({}), Document Type({}), maxTotalWeight({}))", finalCountry, finalLanguage, finalTopTemplate,maxTotalWeight);
    }

    public Map<String, Double> sumFilteredWeights() {
        Map<String, Double> templateWeightSum = new HashMap<>();
        double allowableWeight = configLoader.cdAllowableWeight;

        for (Map<String, Object> resultMap : filteredResult) {
            String country = (String) resultMap.get("Country");
            String templateName = (String) resultMap.get("Template Name");
            List<String> languages = (List<String>) resultMap.get("Language");
            int count = (int) resultMap.get("Count");
            double weight = (double) resultMap.get("WT");

            if (count >= 1 && weight >= allowableWeight) {
                String key = country + "|" + templateName + "|" + String.join(",", languages);
                templateWeightSum.put(key, templateWeightSum.getOrDefault(key, 0.0) + weight);
            }
        }

        // 결과 출력 (선택 사항)
        for (Map.Entry<String, Double> entry : templateWeightSum.entrySet()) {
            log.info("Key: " + entry.getKey() + ", Total WT: " + entry.getValue());
        }

        return templateWeightSum;
    }

//    public void datasetSorting(Map<String, List<Map<String, Object>>> jsonData, String jsonDescription) {
//        filteredResult = new ArrayList<>();
//        resultList = new ArrayList<>();
//
//        for (Map.Entry<String, List<Map<String, Object>>> countryEntry : jsonData.entrySet()) {
//            String countryName = countryEntry.getKey();
//            List<Map<String, Object>> formList = countryEntry.getValue();
//
//            for (Map<String, Object> formMap : formList) {
//                String formName = (String) formMap.get("Template Name");
//                List<String> languages = (List<String>) formMap.get("Language");
//
//                // H-RULE
//                List<Map<String, Object>> hRules = (List<Map<String, Object>>) formMap.get("H-RULE");
//                if (hRules != null) {
//                    for (Map<String, Object> hRule : hRules) {
//                        String word = (String) hRule.get("WD");
//                        Double weight = (Double) hRule.get("WT");
//                        String kr = (String) hRule.get("KR");
//                        if (word != null && weight != null) {
//                            int count = countOccurrences(jsonDescription, word);
//
//                            Map<String, Object> resultMap = new HashMap<>();
//                            resultMap.put("Country", countryName);
//                            resultMap.put("Template Name", formName);
//                            resultMap.put("Language", languages);
//                            resultMap.put("WD", word);
//                            resultMap.put("WT", weight);
//                            resultMap.put("KR", kr);
//                            resultMap.put("Count", count);
//
//                            // 중복 확인
//                            if (!filteredResult.contains(resultMap)) {
//                                filteredResult.add(resultMap);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        // Country, Template, Language로 정렬
//        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (String) r.get("Country"))
//                .thenComparing(r -> (String) r.get("Template Name"))
//                .thenComparing(r -> String.join(",", (List<String>) r.get("Language"))));
//
//        // WT가 높은 순서로 정렬
//        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (double) r.get("WT")).reversed());
//
//        // Count가 높은 순서로 정렬
//        filteredResult.sort(Comparator.comparing((Map<String, Object> r) -> (int) r.get("Count")).reversed());
//
//        // 결과 출력
//        for (Map<String, Object> res : filteredResult) {
//            log.info("Sort Result : Country({}), Language Code({}), Template Name({}), Word({}), Weight({}), KR({}), Count({})",
//                    res.get("Country"), res.get("Language"), res.get("Template Name"), res.get("WD"), res.get("WT"), res.get("KR"), res.get("Count"));
//        }
//    }

    public void classifyDocuments_C1(Map<String, List<Map<String, Object>>> jsonData, String jsonDescription) {
        filteredResult = new ArrayList<>();
        resultList = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, Object>>> countryEntry : jsonData.entrySet()) {
            String countryName = countryEntry.getKey();
            List<Map<String, Object>> formList = countryEntry.getValue();

            for (Map<String, Object> formMap : formList) {
                String formName = (String) formMap.get("Template Name");
                List<String> languages = (List<String>) formMap.get("Language");
                Boolean disable = (boolean) formMap.get("Disable");

                if (!disable) {
                    // H-RULE
                    List<Map<String, Object>> hRules = (List<Map<String, Object>>) formMap.get("H-RULE");
                    if (hRules != null) {
                        for (Map<String, Object> hRule : hRules) {
                            String word = (String) hRule.get("WD");
                            Double weight = (Double) hRule.get("WT");
                            Integer pl = (Integer) hRule.get("PL");
                            String kr = (String) hRule.get("KR");
                            if (word != null && weight != null) {
                                int count = countOccurrences(jsonDescription, word);

                                Map<String, Object> resultMap = new HashMap<>();
                                resultMap.put("Country", countryName);
                                resultMap.put("Template Name", formName);
                                resultMap.put("Language", languages);
                                resultMap.put("WD", word);
                                resultMap.put("WT", weight);
                                resultMap.put("PL", pl);
                                resultMap.put("KR", kr);
                                resultMap.put("Count", count);

                                // 중복 확인
                                if (!filteredResult.contains(resultMap)) {
                                    filteredResult.add(resultMap);
                                }
                            }
                        }
                    }
                }
            }
        }

        filterAndGroupResults();
    }

    public void classifyDocuments_C2(Map<String, List<Map<String, Object>>> jsonData, List<Map<String, Object>> items) {
        //정다혀 추가
        Map<String, List<Map<String, Object>>> formMatchedWords = new HashMap<>();

        filteredResult = new ArrayList<>();
        resultList = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, Object>>> countryEntry : jsonData.entrySet()) {
            String countryName = countryEntry.getKey();
            List<Map<String, Object>> formList = countryEntry.getValue();

            for (Map<String, Object> formMap : formList) {
                String formName = (String) formMap.get("Template Name");
                List<String> languages = (List<String>) formMap.get("Language");
                Boolean disable = (boolean) formMap.get("Disable");

                if (!disable) {
                    // H-RULE
                    List<Map<String, Object>> hRules = (List<Map<String, Object>>) formMap.get("H-RULE");
                    if (hRules != null) {
                        for (Map<String, Object> hRule : hRules) {
                            String word = (String) hRule.get("WD");
                            Double weight = (Double) hRule.get("WT");
                            Integer pl = (Integer) hRule.get("PL");
                            String kr = (String) hRule.get("KR");
                            if (word != null && weight != null) {
                                int count = 0;

                                // items를 순회하며 description과 일치하는 word의 개수를 카운트
                                for (Map<String, Object> item : items) {
                                    String description = (String) item.get("description");

                                    // 대소문자 구별 여부에 따른 비교
                                    if (configLoader.checkCase) { // 대소문자 구별
                                        if (description != null && description.equals(word)) {
                                            count++;
                                            formMatchedWords.computeIfAbsent(formName, k -> new ArrayList<>()).add(item);
                                        }
                                    } else { // 대소문자 구별하지 않음
                                        if (description != null && description.equalsIgnoreCase(word)) {
                                            count++;
                                            formMatchedWords.computeIfAbsent(formName, k -> new ArrayList<>()).add(item);
                                        }
                                    }
                                }

                                Map<String, Object> resultMap = new HashMap<>();
                                resultMap.put("Country", countryName);
                                resultMap.put("Template Name", formName);
                                resultMap.put("Language", languages);
                                resultMap.put("WD", word);
                                resultMap.put("WT", weight);
                                resultMap.put("PL", pl);
                                resultMap.put("KR", kr);
                                resultMap.put("Count", count);

                                // 중복 확인
                                if (!filteredResult.contains(resultMap)) {
                                    filteredResult.add(resultMap);
                                }
                            }
                        }
                    }
                }
            }
        }

        filterAndGroupResults();
    }

    public void classifyDocuments_C3(Map<String, List<Map<String, Object>>> jsonData, List<Map<String, Object>> items) {
        //정다혀 추가
        Map<String, List<Map<String, Object>>> formMatchedWords = new HashMap<>();

        filteredResult = new ArrayList<>();
        resultList = new ArrayList<>();

        String finalTemplate = "미분류";
        String finalCountry = "미분류";
        String finalLanguage = "미분류";

        String defaultCountry = "";
        String defaultLanguage = "";
        String defaultTemplate = "";

        for (Map.Entry<String, List<Map<String, Object>>> countryEntry : jsonData.entrySet()) {
            String countryName = countryEntry.getKey();
            List<Map<String, Object>> formList = countryEntry.getValue();

            for (Map<String, Object> formMap : formList) {
                String formName = (String) formMap.get("Template Name");
                List<String> languages = (List<String>) formMap.get("Language");
                Boolean disable = (boolean) formMap.get("Disable");

                if (!disable) {
                    // H-RULE
                    List<Map<String, Object>> hRules = (List<Map<String, Object>>) formMap.get("H-RULE");
                    for (Map<String, Object> hRule : hRules) {
                        String word = (String) hRule.get("WD");
                        double weight = (double) hRule.get("WT");
                        Integer pl = (Integer) hRule.get("PL");
                        String kr = (String) hRule.get("KR");
                        int count = 0;

                        // items를 순회하며 description과 일치하는 word의 개수를 카운트
                        for (Map<String, Object> item : items) {
                            String description = (String) item.get("description");

                            // 대소문자 구별 여부에 따른 비교
                            if (configLoader.checkCase) { // 대소문자 구별
                                if (description != null && description.equals(word)) {
                                    count++;
                                    formMatchedWords.computeIfAbsent(formName, k -> new ArrayList<>()).add(item);
                                }
                            } else { // 대소문자 구별하지 않음
                                if (description != null && description.equalsIgnoreCase(word)) {
                                    count++;
                                    formMatchedWords.computeIfAbsent(formName, k -> new ArrayList<>()).add(item);
                                }
                            }
                        }

                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("Country", countryName);
                        resultMap.put("Template Name", formName);
                        resultMap.put("Language", languages);
                        resultMap.put("WD", word);
                        resultMap.put("WT", weight);
                        resultMap.put("PL", pl);
                        resultMap.put("KR", kr);
                        resultMap.put("Count", count);

                        // 중복 확인
                        if (!filteredResult.contains(resultMap)) {
                            filteredResult.add(resultMap);
                        }
                    }
                }
            }
        }

        List<String> countryType = new ArrayList<>();
        countryType.add("국가");
        List<String> languageCode = new ArrayList<>();
        languageCode.add("언어");
        List<String> documentType = new ArrayList<>();
        documentType.add("문서 양식");

        // 조건에 맞는 항목 필터링
        List<Map<String, Object>> filtered = filteredResult.stream()
                .filter(r -> (int) r.get("Count") >= 1 && (double) r.get("WT") >= configLoader.cdAllowableWeight)
                .collect(Collectors.toList());

        // 그룹핑 및 카운트
        Map<String, Long> grouped = filtered.stream()
                .collect(Collectors.groupingBy(
                        r -> r.get("Country") + "|" + r.get("Template Name") + "|" + String.join(",", (List<String>) r.get("Language")),
                        Collectors.counting()
                ));

        // 그룹핑 결과를 새 리스트로 저장
        List<Map<String, Object>> groupedResult = new ArrayList<>();
        for (Map.Entry<String, Long> entry : grouped.entrySet()) {
            String[] keys = entry.getKey().split("\\|");
            String country = keys[0];
            String templateName = keys[1];
            List<String> languages = Arrays.asList(keys[2].split(","));

            // 해당 그룹의 모든 항목을 필터링하여 저장
            List<Map<String, Object>> groupItems = filtered.stream()
                    .filter(r -> r.get("Country").equals(country)
                            && r.get("Template Name").equals(templateName)
                            && String.join(",", (List<String>) r.get("Language")).equals(String.join(",", languages)))
                    .collect(Collectors.toList());

            for (Map<String, Object> item : groupItems) {
                Map<String, Object> resultMap = new HashMap<>(item);
                resultMap.put("Count", entry.getValue());
                groupedResult.add(resultMap);
            }
        }

        // 그룹핑 결과를 로그로 출력
        for (Map<String, Object> res : groupedResult) {
            log.info("Grouped Result - Country: {}, Template Name: {}, Language: {}, WD: {}, WT: {}, KR: {}, Count: {}",
                    res.get("Country"), res.get("Template Name"), res.get("Language"), res.get("WD"), res.get("WT"), res.get("KR"), res.get("Count"));

            System.out.println("Grouped Result - Country: " + res.get("Country") + ", Template Name: " + res.get("Template Name") + ", " +
                    "Language: " + res.get("Language") + ", WD: " + res.get("WD") + ", WT: " + res.get("WT") + ", KR: " + res.get("KR") + ", PL: " + res.get("PL") + ", Count: " + res.get("Count"));

//            if (0 < ((Number) res.get("PL")).intValue()) {
//                System.out.println("Grouped Result - Country: " + res.get("Country") + ", Template Name: " + res.get("Template Name") + ", " +
//                        "Language: " + res.get("Language") + ", WD: " + res.get("WD") + ", WT: " + res.get("WT") + ", KR: " + res.get("KR") + ", PL: " + res.get("PL") + ", Count: " + res.get("Count"));
//            }
        }
        //excelService.dataWriteExcel3(groupedResult, datasetSavePath);

        // 카운트가 가장 높은 그룹 찾기
        try {
            // 최대 값을 가진 항목을 찾기
            long maxCount = grouped.values().stream().max(Long::compare).orElseThrow(() -> new NoSuchElementException("No max element found"));

            // 최대 값이 설정한 값보다 큰지 확인
            if (maxCount <= configLoader.wordMinimumCount) {
                // 최대 값이 설정한 값보다 설정 값 보다 작을 경우 미분류 처리
                defaultCountry = "미분류";
                defaultLanguage = "미분류";
                defaultTemplate = "미분류";
                log.info("Number of matching words is less than the set value - Classified as unclassified");
            } else {

                // 최대 값이 여러 개 있는지 확인
                long maxCountOccurrences = grouped.values().stream().filter(count -> count == maxCount).count();

                if (maxCountOccurrences > 1) {
                    defaultCountry = "미분류";
                    defaultTemplate = "미분류";
                    defaultLanguage = "미분류";
                    log.info("Multiple max elements found - Classified as unclassified");
                } else {
                    Map.Entry<String, Long> maxEntry = grouped.entrySet().stream()
                            .filter(entry -> entry.getValue() == maxCount)
                            .findFirst()
                            .orElseThrow(() -> new NoSuchElementException("No max element found"));

                    log.info("Filtering And Grouping Result Max Entry: Key{}, Count{}", maxEntry.getKey(), maxEntry.getValue());
                    System.out.println("Filtering And Grouping Result Max Entry: Key : " + maxEntry.getKey() + ", Count : " + maxEntry.getValue());

                    String[] resultKeys = maxEntry.getKey().split("\\|");
                    defaultCountry = resultKeys[0];
                    defaultTemplate = resultKeys[1];
                    defaultLanguage = resultKeys[2];
                }
            }
        } catch (NoSuchElementException e) {
            defaultCountry = "미분류";
            defaultLanguage = "미분류";
            defaultTemplate = "미분류";
            log.info("No max element found - Classified as unclassified : {}", e);
        }

        log.info("default classify Country : " + defaultCountry + ", default classify Template : " + defaultTemplate + ",default classify Language : " + defaultLanguage);

        // COUNT가 1 이상인 항목 필터링 및 PL이 0 이상, plValue 이하
        List<Map<String, Object>> filteredGroupedResult = groupedResult.stream()
                .filter(r -> ((Number) r.get("Count")).intValue() >= 1 && ((Number) r.get("PL")).intValue() > 0 && ((Number) r.get("PL")).intValue() <= configLoader.plValue)
                .collect(Collectors.toList());

        // PL을 오름차순으로 정렬
        Map<String, List<Map<String, Object>>> groupedByCountryTemplateLanguage = filteredGroupedResult.stream()
                .collect(Collectors.groupingBy(r -> r.get("Country") + "|" + r.get("Template Name") + "|" + String.join(",", (List<String>) r.get("Language"))));

        List<Map<String, Object>> sortedResult = new ArrayList<>();
        groupedByCountryTemplateLanguage.forEach((key, group) -> {
            group.sort(Comparator.comparingInt(r -> ((Number) r.get("PL")).intValue()));
            sortedResult.addAll(group);
        });

        // 결과 출력
        filteredGroupedResult.forEach(item -> log.info("PL Sorted item: " + item));

        // 연속되는 PL 값 확인 및 가장 높은 PL 값을 가진 그룹 찾기
        String highestPLGroupKey = null;
        int highestPLValue = 0;
        List<String> highestPLGroupKeys = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, Object>>> entry : groupedByCountryTemplateLanguage.entrySet()) {
            List<Map<String, Object>> group = entry.getValue();
            List<Integer> plValues = group.stream()
                    .map(r -> (int) r.get("PL"))
                    .sorted()
                    .collect(Collectors.toList());

            boolean isContinuous = true;
            if (plValues.isEmpty() || plValues.get(0) != 1) {
                isContinuous = false;
            } else {
                for (int i = 0; i < plValues.size() - 1; i++) {
                    if (plValues.get(i) + 1 != plValues.get(i + 1)) {
                        isContinuous = false;
                        break;
                    }
                }
            }

            if (isContinuous) {
                int maxPL = plValues.get(plValues.size() - 1);
                if (maxPL > highestPLValue) {
                    highestPLValue = maxPL;
                    highestPLGroupKeys.clear();
                    highestPLGroupKeys.add(entry.getKey());
                } else if (maxPL == highestPLValue) {
                    highestPLGroupKeys.add(entry.getKey());
                }
            }
        }

        // 가장 높은 PL 값을 가진 그룹의 Country, Template Name, Language 정보 저장
        if (highestPLGroupKeys.size() == 1) {
            String[] parts = highestPLGroupKeys.get(0).split("\\|");
            finalCountry = parts[0];
            finalTemplate = parts[1];
            finalLanguage = String.join(",", parts[2]);

            log.info("PL Match found: Country({}), Template=({}), Language({})", finalCountry, finalTemplate, finalLanguage);
        } else if (highestPLGroupKeys.size() > 1) {
            finalCountry = defaultCountry;
            finalTemplate = defaultTemplate;
            finalLanguage = defaultLanguage;

            log.info("Highest PL Group (Multiple groups found):");
        } else {
            finalCountry = defaultCountry;
            finalTemplate = defaultTemplate;
            finalLanguage = defaultLanguage;

            log.info("No continuous PL group found.");
        }

        System.out.println("  Country: " + finalCountry);
        System.out.println("  Template Name: " + finalTemplate);
        System.out.println("  Languages: " + finalLanguage);

        if (finalTemplate.equals("미분류")) {
            matchjsonWord = new ArrayList<>();
        } else {
            matchjsonWord = formMatchedWords.getOrDefault(finalTemplate, new ArrayList<>());
        }

        countryType.add(finalCountry);
        languageCode.add(finalLanguage);
        documentType.add(finalTemplate);

        resultList.add(countryType);
        resultList.add(languageCode);
        resultList.add(documentType);

        log.info("Document classification results (C version): Country({}), Language Code({}), Document Type({}), maxTotalWeight({}))", finalCountry, finalLanguage, finalTemplate);
    }

    public List<List<String>> filterAndGroupResults() {
        List<String> countryType = new ArrayList<>();
        countryType.add("국가");
        List<String> languageCode = new ArrayList<>();
        languageCode.add("언어");
        List<String> documentType = new ArrayList<>();
        documentType.add("문서 양식");

        String finalCountry = "미분류";
        String finalLanguage = "미분류";
        String finalTemplate = "미분류";

        String defaultCountry = "미분류";
        String defaultLanguage = "미분류";
        String defaultTemplate = "미분류";

        // 조건에 맞는 항목 필터링
        List<Map<String, Object>> filtered = filteredResult.stream()
                .filter(r -> (int) r.get("Count") >= 1 && (double) r.get("WT") >= configLoader.cdAllowableWeight)
                .collect(Collectors.toList());

        // 그룹핑 및 카운트
        Map<String, Long> grouped = filtered.stream()
                .collect(Collectors.groupingBy(
                        r -> r.get("Country") + "|" + r.get("Template Name") + "|" + String.join(",", (List<String>) r.get("Language")),
                        Collectors.counting()
                ));

        // 그룹핑 결과를 새 리스트로 저장
        List<Map<String, Object>> groupedResult = new ArrayList<>();
        for (Map.Entry<String, Long> entry : grouped.entrySet()) {
            String[] keys = entry.getKey().split("\\|");
            String country = keys[0];
            String templateName = keys[1];
            List<String> languages = Arrays.asList(keys[2].split(","));

            // 해당 그룹의 모든 항목을 필터링하여 저장
            List<Map<String, Object>> groupItems = filtered.stream()
                    .filter(r -> r.get("Country").equals(country)
                            && r.get("Template Name").equals(templateName)
                            && String.join(",", (List<String>) r.get("Language")).equals(String.join(",", languages)))
                    .collect(Collectors.toList());

            for (Map<String, Object> item : groupItems) {
                Map<String, Object> resultMap = new HashMap<>(item);
                resultMap.put("Count", entry.getValue());
                groupedResult.add(resultMap);
            }
        }

        // 그룹핑 결과를 로그로 출력
        for (Map<String, Object> res : groupedResult) {
            log.info("Grouped Result - Country: {}, Template Name: {}, Language: {}, WD: {}, WT: {}, KR: {}, Count: {}",
                    res.get("Country"), res.get("Template Name"), res.get("Language"), res.get("WD"), res.get("WT"), res.get("KR"), res.get("Count"));
//            System.out.println("Grouped Result - Country: " + res.get("Country") + ", Template Name: " + res.get("Template Name") + ", Language: " + res.get("Language") +
//                    ", WD: " + res.get("WD") + ", WT: " + res.get("WT") + ", PL: " + res.get("PL") + ", KR: " + res.get("KR") + ", Count: " + res.get("Count"));
        }

        //excelService.dataWriteExcel3(groupedResult, datasetSavePath);

        // 카운트가 가장 높은 그룹 찾기
        try {
            // 최대 값을 가진 항목을 찾기
            long maxCount = grouped.values().stream().max(Long::compare).orElseThrow(() -> new NoSuchElementException("No max element found"));

            // 최대 값이 설정한 값보다 큰지 확인
            if (maxCount <= configLoader.wordMinimumCount) {
                // 최대 값이 설정한 값보다 설정 값 보다 작을 경우 미분류 처리
                defaultCountry = "미분류";
                defaultLanguage = "미분류";
                defaultTemplate = "미분류";
                log.info("Number of matching words is less than the set value - Classified as unclassified");
            } else {

                // 최대 값이 여러 개 있는지 확인
                long maxCountOccurrences = grouped.values().stream().filter(count -> count == maxCount).count();

                if (maxCountOccurrences > 1) {
                    defaultCountry = "미분류";
                    defaultTemplate = "미분류";
                    defaultLanguage = "미분류";
                    log.info("Multiple max elements found - Classified as unclassified");
                } else {
                    Map.Entry<String, Long> maxEntry = grouped.entrySet().stream()
                            .filter(entry -> entry.getValue() == maxCount)
                            .findFirst()
                            .orElseThrow(() -> new NoSuchElementException("No max element found"));

                    log.info("Filtering And Grouping Result Max Entry: Key{}, Count{}", maxEntry.getKey(), maxEntry.getValue());

                    String[] resultKeys = maxEntry.getKey().split("\\|");
                    defaultCountry = resultKeys[0];
                    defaultTemplate = resultKeys[1];
                    defaultLanguage = resultKeys[2];
                }
            }
        } catch (NoSuchElementException e) {
            defaultCountry = "미분류";
            defaultLanguage = "미분류";
            defaultTemplate = "미분류";
            log.info("No max element found - Classified as unclassified : {}", e);
        }

        log.info("default classify Country : " + defaultCountry + ", default classify Template : " + defaultTemplate + ",default classify Language : " + defaultLanguage);

        // COUNT가 1 이상인 항목 필터링 및 PL이 0 이상, plValue 이하
        List<Map<String, Object>> filteredGroupedResult = groupedResult.stream()
                .filter(r -> ((Number) r.get("Count")).intValue() >= 1 && ((Number) r.get("PL")).intValue() > 0 && ((Number) r.get("PL")).intValue() <= configLoader.plValue)
                .collect(Collectors.toList());

        // PL을 오름차순으로 정렬
        Map<String, List<Map<String, Object>>> groupedByCountryTemplateLanguage = filteredGroupedResult.stream()
                .collect(Collectors.groupingBy(r -> r.get("Country") + "|" + r.get("Template Name") + "|" + String.join(",", (List<String>) r.get("Language"))));

        List<Map<String, Object>> sortedResult = new ArrayList<>();
        groupedByCountryTemplateLanguage.forEach((key, group) -> {
            group.sort(Comparator.comparingInt(r -> ((Number) r.get("PL")).intValue()));
            sortedResult.addAll(group);
        });

        // 결과 출력
        filteredGroupedResult.forEach(item -> log.info("PL Sorted item: " + item));

        // 연속되는 PL 값 확인 및 가장 높은 PL 값을 가진 그룹 찾기
        String highestPLGroupKey = null;
        int highestPLValue = 0;
        List<String> highestPLGroupKeys = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, Object>>> entry : groupedByCountryTemplateLanguage.entrySet()) {
            List<Map<String, Object>> group = entry.getValue();
            List<Integer> plValues = group.stream()
                    .map(r -> (int) r.get("PL"))
                    .sorted()
                    .collect(Collectors.toList());

            boolean isContinuous = true;
            if (plValues.isEmpty() || plValues.get(0) != 1) {
                isContinuous = false;
            } else {
                for (int i = 0; i < plValues.size() - 1; i++) {
                    if (plValues.get(i) + 1 != plValues.get(i + 1)) {
                        isContinuous = false;
                        break;
                    }
                }
            }

            if (isContinuous) {
                int maxPL = plValues.get(plValues.size() - 1);
                if (maxPL > highestPLValue) {
                    highestPLValue = maxPL;
                    highestPLGroupKeys.clear();
                    highestPLGroupKeys.add(entry.getKey());
                } else if (maxPL == highestPLValue) {
                    highestPLGroupKeys.add(entry.getKey());
                }
            }
        }

        // 가장 높은 PL 값을 가진 그룹의 Country, Template Name, Language 정보 저장
        if (highestPLGroupKeys.size() == 1) {
            String[] parts = highestPLGroupKeys.get(0).split("\\|");
            finalCountry = parts[0];
            finalTemplate = parts[1];
            finalLanguage = String.join(",", parts[2]);

            log.info("PL Match found: Country({}), Template=({}), Language({})", finalCountry, finalTemplate, finalLanguage);
        } else if (highestPLGroupKeys.size() > 1) {
            finalCountry = defaultCountry;
            finalTemplate = defaultTemplate;
            finalLanguage = defaultLanguage;

            log.info("Highest PL Group (Multiple groups found):");
        } else {
            finalCountry = defaultCountry;
            finalTemplate = defaultTemplate;
            finalLanguage = defaultLanguage;

            log.info("No continuous PL group found.");
        }

        Map<String, List<Map<String, Object>>> formMatchedWords = new HashMap<>();

        if (finalTemplate.equals("미분류")) {
            matchjsonWord = new ArrayList<>();
        } else {
            matchjsonWord = formMatchedWords.getOrDefault(finalTemplate, new ArrayList<>());
        }

        countryType.add(finalCountry);
        languageCode.add(finalLanguage);
        documentType.add(finalTemplate);

        resultList.add(countryType);
        resultList.add(languageCode);
        resultList.add(documentType);

        log.info("Document classification results (C version): Country({}), Language Code({}), Document Type({})", finalCountry, finalLanguage, finalTemplate);
        //System.out.println("Document classification results (C version): Country(" + finalCountry + "), Language Code(" +  finalLanguage +"), Document Type(" + finalTemplate + "))");

        return resultList;
    }

    public List<String> getCertificateType(String jsonDescription) {
        List<String> results = new ArrayList<>();

        if (jsonDescription.contains("ISO")) {
            results.add("ISO 22000");
        }
        if (jsonDescription.contains("HACCP")) {
            results.add("HACCP");
        }
        if (jsonDescription.contains("GMP")) {
            results.add("GMP");
        }
        if (jsonDescription.contains("FSSC 22000") || jsonDescription.contains("BRC") || jsonDescription.contains("SQF")) {
            results.add("GFSI 규격");
        }

        return results;
    }
}
