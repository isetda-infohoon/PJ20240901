package com.isetda.idpengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;
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
    public List<Map<String, Object>> filteredResult;
    public String docType="";
    public String fileName;
    public List<String> documentType = new ArrayList<>();
    String saveFilePath;

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
            excelService.createExcel2(resultList, filteredResult, fileName, saveFilePath, a);
        } catch (IOException e) {
            log.error("엑셀 파일 생성 실패: {}", e.getStackTrace()[0]);
        }
        matchjsonWord = new ArrayList<>();
    }

    // 합쳐진 추출 단어(description)로 일치 단어 비교
    public void classifyDocuments1(Map<String, List<Map<String, Object>>> jsonData, String jsonLocale, String jsonDescription) {
        filteredResult = new ArrayList<>();
        resultList = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, Object>>> countryEntry : jsonData.entrySet()) {
            String countryName = countryEntry.getKey();
            List<Map<String, Object>> formList = countryEntry.getValue();

            for (Map<String, Object> formMap : formList) {
                String formName = (String) formMap.get("Template Name");
                String language = (String) formMap.get("Language");

                // H-RULE
                List<Map<String, Object>> hRules = (List<Map<String, Object>>) formMap.get("H-RULE");
                for (Map<String, Object> hRule : hRules) {
                    String word = (String) hRule.get("WD");
                    double weight = (double) hRule.get("WT");
                    int count = countOccurrences(jsonDescription, word);

                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("Country", countryName);
                    resultMap.put("Template Name", formName);
                    resultMap.put("Language", language);
                    resultMap.put("WD", word);
                    resultMap.put("WT", weight);
                    resultMap.put("Count", count);

                    filteredResult.add(resultMap);
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
            log.info("Sort Result : Country({}), Language Code({}), Template Name({}), Word({}), Weight({}), Count({})", res.get("Country"), res.get("Language"), res.get("Template Name"), res.get("WD"), res.get("WT"), res.get("Count"));
        }

        // Country, Template 별로 최상위 WT의 count 합계를 찾기
        Map<String, Integer> templateCountSum = new HashMap<>();
        Map<String, Map<String, String>> templateInfo = new HashMap<>();
        for (Map<String, Object> res : filteredResult) {
            String templateName = (String) res.get("Template Name");
            int count = (int) res.get("Count");

            templateCountSum.put(templateName, templateCountSum.getOrDefault(templateName, 0) + count);

            templateInfo.put(templateName, Map.of(
                    "Country", (String) res.get("Country"),
                    "Language", (String) res.get("Language")
            ));
        }

        // 합계가 가장 높은 Template들 찾기
        int maxTotalCount = Collections.max(templateCountSum.values());
        List<String> topTemplates = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : templateCountSum.entrySet()) {
            if (entry.getValue() == maxTotalCount) {
                topTemplates.add(entry.getKey());
            }
        }

        // 합계가 같은 Template들이 여러 개 있는 경우, 앞에서 찾은 WT 값이 높은 항목들을 제외하고 count가 1 이상인 항목들의 합계 비교
        Map<String, Integer> filteredTemplateCountSum = new HashMap<>();
        for (String templateName : topTemplates) {
            for (Map<String, Object> res : filteredResult) {
                if (templateName.equals(res.get("Template Name"))) {
                    int count = (int) res.get("Count");
                    double weight = (double) res.get("WT");
                    if (count > 0 && weight != (double) filteredResult.get(0).get("WT")) {
                        filteredTemplateCountSum.put(templateName, filteredTemplateCountSum.getOrDefault(templateName, 0) + count);
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

        // 최종적으로 합계가 가장 높은 Template 찾기
        String finalTopTemplate = Collections.max(filteredTemplateCountSum.entrySet(), Map.Entry.comparingByValue()).getKey();
        int finalMaxTotalCount = filteredTemplateCountSum.get(finalTopTemplate);

        // 최종 결과로 가져온 Template의 Country와 Language를 가져오기
        String finalCountry = templateInfo.get(finalTopTemplate).get("Country");
        String finalLanguage = templateInfo.get(finalTopTemplate).get("Language");

        // 최종 템플릿에서 count가 1 이상인 항목의 WT 합계를 구하기
        double totalWtSum = 0;
        for (Map<String, Object> res : filteredResult) {
            if (finalTopTemplate.equals(res.get("Template Name"))) {
                int count = (int) res.get("Count");
                if (count > 0) {
                    totalWtSum += (double) res.get("WT");
                }
            }
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

    public void classifyDocuments2(Map<String, List<Map<String, Object>>> jsonData, String jsonLocale, List<Map<String, Object>> items) {
        filteredResult = new ArrayList<>();
        resultList = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, Object>>> countryEntry : jsonData.entrySet()) {
            String countryName = countryEntry.getKey();
            List<Map<String, Object>> formList = countryEntry.getValue();

            for (Map<String, Object> formMap : formList) {
                String formName = (String) formMap.get("Template Name");
                String language = (String) formMap.get("Language");

                // H-RULE
                List<Map<String, Object>> hRules = (List<Map<String, Object>>) formMap.get("H-RULE");
                for (Map<String, Object> hRule : hRules) {
                    String word = (String) hRule.get("WD");
                    double weight = (double) hRule.get("WT");
                    int count = 0;

                    // items를 순회하며 description과 일치하는 word의 개수를 카운트
                    for (Map<String, Object> item : items) {
                        String description = (String) item.get("description");
                        if (description.equals(word)) {
                            count++;
                        }
                    }

                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("Country", countryName);
                    resultMap.put("Template Name", formName);
                    resultMap.put("Language", language);
                    resultMap.put("WD", word);
                    resultMap.put("WT", weight);
                    resultMap.put("Count", count);

                    filteredResult.add(resultMap);
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
            log.info("Sort Result : Country({}), Language Code({}), Template Name({}), Word({}), Weight({}), Count({})",
                    res.get("Country"), res.get("Language"), res.get("Template Name"), res.get("WD"), res.get("WT"), res.get("Count"));
        }

        // Country, Template 별로 최상위 WT의 count 합계를 찾기
        Map<String, Integer> templateCountSum = new HashMap<>();
        Map<String, Map<String, String>> templateInfo = new HashMap<>();
        for (Map<String, Object> res : filteredResult) {
            String templateName = (String) res.get("Template Name");
            int count = (int) res.get("Count");
            templateCountSum.put(templateName, templateCountSum.getOrDefault(templateName, 0) + count);
            templateInfo.put(templateName, Map.of(
                    "Country", (String) res.get("Country"),
                    "Language", (String) res.get("Language")
            ));
        }

        // 합계가 가장 높은 Template들 찾기
        int maxTotalCount = Collections.max(templateCountSum.values());
        List<String> topTemplates = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : templateCountSum.entrySet()) {
            if (entry.getValue() == maxTotalCount) {
                topTemplates.add(entry.getKey());
            }
        }

        // 합계가 같은 Template들이 여러 개 있는 경우, 앞에서 찾은 WT 값이 높은 항목들을 제외하고 count가 1 이상인 항목들의 합계 비교
        Map<String, Integer> filteredTemplateCountSum = new HashMap<>();
        for (String templateName : topTemplates) {
            for (Map<String, Object> res : filteredResult) {
                if (templateName.equals(res.get("Template Name"))) {
                    int count = (int) res.get("Count");
                    double weight = (double) res.get("WT");
                    if (count > 0 && weight != (double) filteredResult.get(0).get("WT")) {
                        filteredTemplateCountSum.put(templateName, filteredTemplateCountSum.getOrDefault(templateName, 0) + count);
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

        String finalTopTemplate;
        int finalMaxTotalCount;

        String finalCountry = "";
        String finalLanguage = "";
        double totalWtSum = 0;

        // 최종적으로 합계가 가장 높은 Template 찾기
        if (filteredTemplateCountSum.isEmpty()) {
            // filteredTemplateCountSum이 비어 있는 경우 처리
            finalTopTemplate = "미분류";
            finalMaxTotalCount = 0;
            System.out.println("No valid template found. Setting default value.");
        } else {
            // 최종적으로 합계가 가장 높은 Template 찾기
            finalTopTemplate = Collections.max(filteredTemplateCountSum.entrySet(), Map.Entry.comparingByValue()).getKey();
            finalMaxTotalCount = filteredTemplateCountSum.get(finalTopTemplate);

            // 최종 결과로 가져온 Template의 Country와 Language를 가져오기
            finalCountry = templateInfo.get(finalTopTemplate).get("Country");
            finalLanguage = templateInfo.get(finalTopTemplate).get("Language");

            // 최종 템플릿에서 count가 1 이상인 항목의 WT 합계를 구하기

            for (Map<String, Object> res : filteredResult) {
                if (finalTopTemplate.equals(res.get("Template Name"))) {
                    int count = (int) res.get("Count");
                    if (count > 0) {
                        totalWtSum += (double) res.get("WT");
                    }
                }
            }
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

    // 단어 카운트
    public static int countOccurrences(String input, String word) {
        if (word.isEmpty()) {
            return 0;
        }
        String[] parts = input.split(Pattern.quote(word));
        return parts.length - 1;
    }

    //정다현 추가 내용
    public void classifyDocuments3(Map<String, List<Map<String, Object>>> jsonData, String jsonLocale, List<Map<String, Object>> items) {
        //정다혀 추가
        Map<String, List<Map<String, Object>>> formMatchedWords = new HashMap<>();

        filteredResult = new ArrayList<>();
        resultList = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, Object>>> countryEntry : jsonData.entrySet()) {
            String countryName = countryEntry.getKey();
            List<Map<String, Object>> formList = countryEntry.getValue();

            for (Map<String, Object> formMap : formList) {
                String formName = (String) formMap.get("Template Name");
                String language = (String) formMap.get("Language");

                // H-RULE
                List<Map<String, Object>> hRules = (List<Map<String, Object>>) formMap.get("H-RULE");
                for (Map<String, Object> hRule : hRules) {
                    String word = (String) hRule.get("WD");
                    double weight = (double) hRule.get("WT");
                    int count = 0;

                    // items를 순회하며 description과 일치하는 word의 개수를 카운트
                    for (Map<String, Object> item : items) {
                        String description = (String) item.get("description");
                        if (description.equals(word)) {
                            count++;
                            formMatchedWords.computeIfAbsent(formName, k -> new ArrayList<>()).add(item);
                        }
                    }

                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("Country", countryName);
                    resultMap.put("Template Name", formName);
                    resultMap.put("Language", language);
                    resultMap.put("WD", word);
                    resultMap.put("WT", weight);
                    resultMap.put("Count", count);

                    filteredResult.add(resultMap);
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
            log.info("Sort Result : Country({}), Language Code({}), Template Name({}), Word({}), Weight({}), Count({})",
                    res.get("Country"), res.get("Language"), res.get("Template Name"), res.get("WD"), res.get("WT"), res.get("Count"));
        }

        // Country, Template 별로 최상위 WT의 count 합계를 찾기
        Map<String, Integer> templateCountSum = new HashMap<>();
        Map<String, Map<String, String>> templateInfo = new HashMap<>();
        for (Map<String, Object> res : filteredResult) {
            String templateName = (String) res.get("Template Name");
            int count = (int) res.get("Count");
            templateCountSum.put(templateName, templateCountSum.getOrDefault(templateName, 0) + count);
            templateInfo.put(templateName, Map.of(
                    "Country", (String) res.get("Country"),
                    "Language", (String) res.get("Language")
            ));
        }

        // 합계가 가장 높은 Template들 찾기
        int maxTotalCount = Collections.max(templateCountSum.values());
        List<String> topTemplates = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : templateCountSum.entrySet()) {
            if (entry.getValue() == maxTotalCount) {
                topTemplates.add(entry.getKey());
            }
        }

        // 합계가 같은 Template들이 여러 개 있는 경우, 앞에서 찾은 WT 값이 높은 항목들을 제외하고 count가 1 이상인 항목들의 합계 비교
        Map<String, Integer> filteredTemplateCountSum = new HashMap<>();
        for (String templateName : topTemplates) {
            for (Map<String, Object> res : filteredResult) {
                if (templateName.equals(res.get("Template Name"))) {
                    int count = (int) res.get("Count");
                    double weight = (double) res.get("WT");
                    if (count > 0 && weight != (double) filteredResult.get(0).get("WT")) {
                        filteredTemplateCountSum.put(templateName, filteredTemplateCountSum.getOrDefault(templateName, 0) + count);
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

        String finalTopTemplate;
        int finalMaxTotalCount;

        String finalCountry = "";
        String finalLanguage = "";
        double totalWtSum = 0;

        // 최종적으로 합계가 가장 높은 Template 찾기
        if (filteredTemplateCountSum.isEmpty()) {
            // filteredTemplateCountSum이 비어 있는 경우 처리
            finalTopTemplate = "미분류";
            finalCountry = "미분류";
            finalLanguage = "미분류";
            finalMaxTotalCount = 0;
            System.out.println("No valid template found. Setting default value.");
        } else {
            // 최종적으로 합계가 가장 높은 Template 찾기
            finalTopTemplate = Collections.max(filteredTemplateCountSum.entrySet(), Map.Entry.comparingByValue()).getKey();
            finalMaxTotalCount = filteredTemplateCountSum.get(finalTopTemplate);

            // 최종 결과로 가져온 Template의 Country와 Language를 가져오기
            finalCountry = templateInfo.get(finalTopTemplate).get("Country");
            finalLanguage = templateInfo.get(finalTopTemplate).get("Language");

            // 최종 템플릿에서 count가 1 이상인 항목의 WT 합계를 구하기

            for (Map<String, Object> res : filteredResult) {
                if (finalTopTemplate.equals(res.get("Template Name"))) {
                    int count = (int) res.get("Count");
                    if (count > 0) {
                        totalWtSum += (double) res.get("WT");
                    }
                }
            }
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

        //log.info("Final template with highest adjusted WT sum count: {}, Count: {}", finalTopTemplate, finalMaxTotalCount);
        log.info("Document classification results: Country({}), Language Code({}), Document Type({}))", finalCountry, finalLanguage, finalTopTemplate);

    }
}
