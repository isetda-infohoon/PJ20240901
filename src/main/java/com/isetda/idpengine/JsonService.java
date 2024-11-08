package com.isetda.idpengine;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JsonService {
    public String jsonLocal = "";
    public JSONObject jsonObject;
    //그냥 기본으로 오는 단어 구조 match1
    public List<Map<String, Object>> jsonCollection;
    //단어 리스트와 일치하는 match2의 단어 값
    public static List<Map<String, Object>> jsonCollection3 = new ArrayList<>();
    //y축 기준 경우의 수 그룹화 단어 match2
    public static List<Map<String, Object>> jsonCollection2;
    public static ConfigLoader configLoader = ConfigLoader.getInstance();

    private static final Logger log = LogManager.getLogger(JsonService.class);

    // 생성자에서 JSON 데이터 로딩 및 처리
    public JsonService(String jsonFilePath) {
        try {
            if (configLoader.encodingCheck){
                byte[] encodedBytes = FileUtils.readFileToByteArray(new File(jsonFilePath));
                String decodedJson = aesDecode(encodedBytes);
                this.jsonObject = new JSONObject(decodedJson);
            }
            else{
                this.jsonObject = new JSONObject(FileUtils.readFileToString(new File(jsonFilePath), "UTF-8"));
            }
            log.info("JSON data loading successful");
            getWordPosition();

//            cd3();
        } catch (IOException e) {
            log.error("Error reading json file", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> createEmptyData() {
        // 빈 데이터를 생성하여 반환하는 메서드
        Map<String, Object> emptyData = new HashMap<>();
        emptyData.put("description", "");
        emptyData.put("locale", "");
        emptyData.put("midY", Integer.MAX_VALUE);
        emptyData.put("minX", Integer.MAX_VALUE);
        // 필요한 경우 다른 필드도 빈 값으로 설정 가능
        return emptyData;
    }

    //정렬 로직[10/16] 구 버전 CD 3에 적용
    public static List<Map<String, Object>> sortAnnotations(List<Map<String, Object>> items) {
        // Step 1: Sort by minY
        items.sort(Comparator.comparingInt(a -> (Integer) a.getOrDefault("minY", Integer.MAX_VALUE)));

        // Step 2: Group items with similar minY values
        List<List<Map<String, Object>>> groups = new ArrayList<>();
        List<Map<String, Object>> currentGroup = new ArrayList<>();
        currentGroup.add(items.get(0));

        for (int i = 1; i < items.size(); i++) {
            Map<String, Object> current = items.get(i);
            Map<String, Object> previous = items.get(i - 1);

            int currentMinY = (Integer) current.getOrDefault("minY", Integer.MAX_VALUE);
            int previousMinY = (Integer) previous.getOrDefault("minY", Integer.MAX_VALUE);

            if (Math.abs(currentMinY - previousMinY) <= 10) {
                currentGroup.add(current);
            } else {
                groups.add(new ArrayList<>(currentGroup));
                currentGroup.clear();
                currentGroup.add(current);
            }
        }
        if (!currentGroup.isEmpty()) {
            groups.add(currentGroup);
        }

        // Step 3: Sort each group by minX
        for (List<Map<String, Object>> group : groups) {
            group.sort(Comparator.comparingInt(a -> (Integer) a.getOrDefault("minX", Integer.MAX_VALUE)));
        }

        // Step 4: Flatten the groups back into a single list
        return groups.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public static List<Map<String, Object>> sortAnnotations4(List<Map<String, Object>> items, double eps, int minPts) {
        // Step 1: Convert each item to a DoublePoint for DBSCAN
        List<DoublePoint> points = items.stream()
                .map(item -> new DoublePoint(new double[]{
                        (Integer) item.get("minX"),
                        (Integer) item.get("minY")
                }))
                .collect(Collectors.toList());

        // Step 2: Perform DBSCAN clustering
        DBSCANClusterer<DoublePoint> clusterer = new DBSCANClusterer<>(eps, minPts);
        List<Cluster<DoublePoint>> clusters = clusterer.cluster(points);

        // Step 3: Group original items into clusters
        List<List<Map<String, Object>>> groupedItems = new ArrayList<>();
        for (Cluster<DoublePoint> cluster : clusters) {
            List<Map<String, Object>> group = new ArrayList<>();
            for (DoublePoint point : cluster.getPoints()) {
                // Find the corresponding item by matching minX and minY
                Map<String, Object> item = items.stream()
                        .filter(i -> (Integer) i.get("minX") == (int) point.getPoint()[0]
                                && (Integer) i.get("minY") == (int) point.getPoint()[1])
                        .findFirst()
                        .orElse(null);
                if (item != null) {
                    group.add(item);
                }
            }
            groupedItems.add(group);
        }

        // Step 4: Sort each group by minX
        for (List<Map<String, Object>> group : groupedItems) {
            group.sort(Comparator.comparingInt(a -> (Integer) a.get("minX")));
        }

        // Step 5: Flatten the groups back into a single list
        return groupedItems.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    //json에서 단어, 위치 가져와서 정렬 (1차)
    public void getWordPosition() {
        jsonCollection = new ArrayList<>();
        try {
            JSONObject responsesObject = jsonObject.getJSONArray("responses").getJSONObject(0);

            if (responsesObject == null) {
                log.warn("No valid responsesObject found, skipping extraction.");
                jsonCollection.add(createEmptyData());
                return;
            }

            JSONArray textAnnotationsArray = responsesObject.getJSONArray("textAnnotations");

            if (textAnnotationsArray == null || textAnnotationsArray.length() == 0) {
                log.warn("No valid textAnnotationsArray found, skipping extraction.");
                jsonCollection.add(createEmptyData());
                return;
            }

            jsonLocal = textAnnotationsArray.getJSONObject(0).getString("locale");
            log.info("language code: {}", jsonLocal);

            log.info("Start word position extraction");

            // 첫 번째 description(전체 텍스트)을 별도로 처리
            JSONObject firstAnnotation = textAnnotationsArray.getJSONObject(0);
            Map<String, Object> firstData = processAnnotation(firstAnnotation);
            jsonCollection.add(firstData);

            // 나머지 annotations 처리
            for (int i = 1; i < textAnnotationsArray.length(); i++) {
                JSONObject textAnnotation = textAnnotationsArray.getJSONObject(i);
                if (textAnnotation != null) {
                    Map<String, Object> data = processAnnotation(textAnnotation);
                    logItemInfo(data);
                    jsonCollection.add(data);
                }
            }

            log.info("Word position extraction and sorting completed");

        } catch (JSONException e) {
            log.error("Error processing JSON: ", e);
            jsonCollection.add(createEmptyData());
        } catch (Exception e) {
            log.error("Error extracting word location: {}", e.getMessage(), e);
            jsonCollection.add(createEmptyData());
        }
    }

    private Map<String, Object> processAnnotation(JSONObject textAnnotation) {
        Map<String, Object> data = new HashMap<>();
        String description = textAnnotation.getString("description");
        JSONArray verticesArray = textAnnotation.getJSONObject("boundingPoly").getJSONArray("vertices");

        List<JSONObject> vertices = IntStream.range(0, verticesArray.length())
                .mapToObj(verticesArray::getJSONObject)
                .collect(Collectors.toList());

        int minX = vertices.stream().mapToInt(v -> v.optInt("x", 0)).min().orElse(0);
        int minY = vertices.stream().mapToInt(v -> v.optInt("y", 0)).min().orElse(0);
        int maxX = vertices.stream().mapToInt(v -> v.optInt("x", 0)).max().orElse(0);
        int maxY = vertices.stream().mapToInt(v -> v.optInt("y", 0)).max().orElse(0);
        int midY = (minY + maxY) / 2;

        data.put("description", description);
        data.put("vertices", vertices);
        data.put("minX", minX);
        data.put("minY", minY);
        data.put("maxX", maxX);
        data.put("maxY", maxY);
        data.put("midY", midY);

        return data;
    }
//    로고 찍기 용
    public void logItemInfo(Map<String, Object> item) {
        String description = (String) item.get("description");
        List<JSONObject> vertices = (List<JSONObject>) item.get("vertices");
        int minX = (int) item.get("minX");
        int minY = (int) item.get("minY");
        int maxX = (int) item.get("maxX");
        int maxY = (int) item.get("maxY");
        int midY = (int) item.get("midY");

        String verticesString = vertices.stream()
                .map(v -> String.format("(%d, %d)", v.optInt("x", 0), v.optInt("y", 0)))
                .collect(Collectors.joining(", "));

        log.debug(String.format("L:%-45s|MinX:%5d|MinY:%5d|MaxX:%5d|MaxY:%5d|MidY:%5d|W:%-20s",
                verticesString, minX, minY, maxX, maxY, midY, description));
    }

    //json을 통해서 2차 매칭 (y축 기준으로 그룹화)[경우의 수를 통한 그룹화]
    //CD3 전용
    public static List<Map<String, Object>> findMatchingWords(List<Map<String, Object>> words) {
//        words = sortAnnotations3(words);
        List<Map<String, Object>> results = new ArrayList<>();
        jsonCollection2 = new ArrayList<>();
        int a = 0;


        // words가 비어 있거나 null이면 스킵
        if (words == null || words.isEmpty()) {
            log.warn("The words list is empty or null, skipping processing.");
            return results; // 빈 리스트 반환
        }

        List<Map<String, Object>> checkText = null;
        for (int i = 0; i < words.size(); i++) {
            a = 1;
            StringBuilder currentText = new StringBuilder();

            // null을 처리하기 위해 기본값 사용
            int startMaxX = (int) words.get(i).getOrDefault("maxX", 0);
            int startMaxY = (int) words.get(i).getOrDefault("maxY", 0);
            int startX = (int) words.get(i).getOrDefault("minX", 0);
            int startY = (int) words.get(i).getOrDefault("minY", 0);
            int maxX = startX;
            int maxY = startY;

            // 단어 자체도 추가
            checkText = new ArrayList<>();
            Map<String, Object> singleWord = words.get(i);
            checkText.add(singleWord);
            currentText.append(singleWord.get("description"));
            log.debug("The first word: {}", currentText);

            // 단일 단어 자체를 결과에 추가
            addToCollection2(checkText, currentText.toString(), startX, startY, startMaxX, startMaxY);

            // 연속된 단어 조합 생성
            for (int j = i + 1; j < words.size(); j++) {
                Map<String, Object> nextWord = words.get(j);
//          log.info("체크 단어 : {}",checkText);
//          log.info("다음 단어 : {}",nextWord);

                if (isOnSameLineByMidY2(checkText.get(checkText.size() - 1), nextWord)) {
                    currentText.append(nextWord.get("description"));
                    log.debug("Group words:  {}", currentText); //여기 띄우기
                    a++;
                    checkText.add(nextWord);
                    maxX = Math.max(maxX, (int) nextWord.getOrDefault("maxX", 0));
                    maxY = Math.max(maxY, (int) nextWord.getOrDefault("maxY", 0));

                    // 현재 조합이 targetWords에 포함되는지 확인
                    addToCollection2(checkText, currentText.toString(), startX, startY, maxX, maxY);

                } else {
                    log.debug("Connected words: {}", a);
                    break; // 동일한 라인이 아니면 중단
                }
            }
        }
        log.debug("Total connected words: {}", a);

        return results;
    }

//    public static List<Map<String, Object>> findMatchingWords(List<Map<String, Object>> words) {
//        if (words == null || words.isEmpty()) {
//            log.warn("The words list is empty or null, skipping processing.");
//            return new ArrayList<>();
//        }
//
//        jsonCollection2 = new ArrayList<>();
//        List<Map<String, Object>> sortedWords = sortAnnotations(words);
//
//        for (int i = 0; i < sortedWords.size(); i++) {
//            StringBuilder currentText = new StringBuilder();
//            List<Map<String, Object>> checkText = new ArrayList<>();
//
//            // 현재 단어 처리
//            Map<String, Object> currentWord = sortedWords.get(i);
//            int startX = (Integer) currentWord.getOrDefault("minX", 0);
//            int startY = (Integer) currentWord.getOrDefault("minY", 0);
//            int maxX = (Integer) currentWord.getOrDefault("maxX", 0);
//            int maxY = (Integer) currentWord.getOrDefault("maxY", 0);
//
//            checkText.add(currentWord);
//            currentText.append(currentWord.get("description"));
//
//            // 단일 단어 추가
//            addToCollection2(checkText, currentText.toString(), startX, startY, maxX, maxY);
//
//            // 연속된 단어 처리
//            for (int j = i + 1; j < sortedWords.size(); j++) {
//                Map<String, Object> nextWord = sortedWords.get(j);
//
//                if (isOnSameLineByMidY2(checkText.get(checkText.size() - 1), nextWord)) {
//                    currentText.append(nextWord.get("description"));
//                    checkText.add(nextWord);
//                    maxX = Math.max(maxX, (Integer) nextWord.getOrDefault("maxX", 0));
//                    maxY = Math.max(maxY, (Integer) nextWord.getOrDefault("maxY", 0));
//
//                    addToCollection2(checkText, currentText.toString(), startX, startY, maxX, maxY);
//                } else {
//                    break;
//                }
//            }
//        }
//
//        return jsonCollection2;
//    }

    //모든 연속된 단어을 저장하는 것
    private static void addToCollection2(List<Map<String, Object>> checkText, String combinedText,
                                         int startX, int startY, int maxX, int maxY) {
        int minX = checkText.stream().mapToInt(w -> (int) w.get("minX")).min().orElse(startX);

        Map<String, Object> result = new HashMap<>();
        result.put("description", combinedText);
        result.put("minX", minX);
        result.put("minY", startY);
        result.put("maxX", maxX);
        result.put("maxY", maxY);
        jsonCollection2.add(result);
    }

    private static boolean isOnSameLineByMidY2(Map<String, Object> a, Map<String, Object> b) {
        int midY_a = ((int) a.get("minY") + (int) a.get("maxY")) / 2;
        int minY_b = (int) b.get("minY");
        int maxY_b = (int) b.get("maxY");
        //추가 정다현
        int maxX_a = (int) a.get("maxX");
        int minX_b = (int) b.get("minX");


        return midY_a>=minY_b && midY_a<= maxY_b && minX_b - maxX_a <200; //
    }


    public static Map<String, List<List<String[]>>> getJsonDictionary(String decodeText) throws Exception {
//        String filePath = configLoader.jsonFilePath;
        Map<String, List<List<String[]>>> jsonDictionary = new HashMap<>();

        try {
            //String content = FileUtils.readFileToString(new File(filePath), "UTF-8");
            JSONObject jsonObject = new JSONObject(decodeText);

            JSONArray countryList = jsonObject.getJSONArray("Country List");

            for (int i = 0; i < countryList.length(); i++) {
                JSONObject country = countryList.getJSONObject(i);
                String countryName = country.getString("Country");

                log.info("Country: {}", countryName);

                JSONArray forms = country.getJSONArray("Template");
                List<List<String[]>> formList = new ArrayList<>();

                for (int j = 0; j < forms.length(); j++) {
                    JSONObject form = forms.getJSONObject(j);
                    String formName = form.getString("Template Name");
                    String language = form.getString("Language");

                    log.info("H: {}, {}", formName, language);

                    List<String[]> ruleList = new ArrayList<>();
                    ruleList.add(new String[]{formName, language});

                    int count = 0;

                    JSONArray rules = form.getJSONArray("H-RULE");
                    for (int k = 0; k < rules.length(); k++) {
                        JSONObject rule = rules.getJSONObject(k);
                        String word = rule.getString("WD");

                        if (!word.isEmpty()) {
                            String weight = String.valueOf(rule.getDouble("WT"));
                            ruleList.add(new String[]{word, weight});

                            count += 1;
                            log.info("W: {}, {}", word, weight);
                        } else {
                            log.info("Word is empty.");
                        }
                    }

                    formList.add(ruleList);
                    log.info("Number of words imported: {}", count);
                }

                jsonDictionary.put(countryName, formList);
            }
            log.info("JSON 단어 리스트 추출 완료");

            // 결과 출력
//            for (Map.Entry<String, List<List<String[]>>> entry : jsonDictionary.entrySet()) {
//                String country = entry.getKey();
//                List<List<String[]>> forms = entry.getValue();
//                System.out.println("국가: " + country);
//                for (List<String[]> form : forms) {
//                    for (String[] details : form) {
//                        System.out.print("[");
//                        for (String detail : details) {
//                            System.out.print(detail + ", ");
//                        }
//                        System.out.print("] ");
//                    }
//                    System.out.println();
//                }
//                System.out.println();
//            }

        } catch (Exception e) {
            log.error("JSON 단어 리스트 추출 실패: {}", e.getStackTrace()[0]);
        }

        return jsonDictionary;
    }

//    public static Map<String, List<List<String[]>>> getJsonDictionary2(String decodeText) throws Exception {
//        Map<String, List<List<String[]>>> jsonDictionary = new HashMap<>();
//
//        try {
//            JSONObject jsonObject = new JSONObject(decodeText);
//
//            JSONArray countryList = jsonObject.getJSONArray("Country List");
//
//            for (int i = 0; i < countryList.length(); i++) {
//                JSONObject country = countryList.getJSONObject(i);
//                String countryName = country.getString("Country");
//
//                log.info("Country: {}", countryName);
//
//                JSONArray forms = country.getJSONArray("Template");
//                List<List<String[]>> formList = new ArrayList<>();
//
//                for (int j = 0; j < forms.length(); j++) {
//                    JSONObject form = forms.getJSONObject(j);
//                    String formName = form.getString("Template Name");
//                    String language = form.getString("Language");
//
//                    log.info("H: {}, {}", formName, language);
//
//                    List<String[]> ruleList = new ArrayList<>();
//                    ruleList.add(new String[]{formName, language});
//
//                    int count = 0;
//
//                    JSONArray hrules = form.getJSONArray("H-RULE");
//                    for (int k = 0; k < hrules.length(); k++) {
//                        JSONObject hrule = hrules.getJSONObject(k);
//                        String word = hrule.getString("WD");
//
//                        if (!word.isEmpty()) {
//                            String weight = String.valueOf(hrule.getDouble("WT"));
//                            ruleList.add(new String[]{word, weight});
//
//                            count += 1;
//                            log.info("W: {}, {}", word, weight);
//                        } else {
//                            log.info("Word is empty.");
//                        }
//                    }
//
//                    JSONArray airules = form.getJSONArray("AI-RULE");
//                    for (int k = 0; k < airules.length(); k++) {
//                        JSONObject airule = airules.getJSONObject(k);
//                        String word = airule.getString("WD");
//
//                        if (!word.isEmpty()) {
//                            String weight = String.valueOf(airule.getDouble("WT"));
//                            ruleList.add(new String[]{word, weight});
//
//                            count += 1;
//                            log.info("W: {}, {}", word, weight);
//                        } else {
//                            log.info("Word is empty.");
//                        }
//                    }
//
//                    formList.add(ruleList);
//                    log.info("Number of words imported: {}", count);
//                }
//
//                jsonDictionary.put(countryName, formList);
//            }
//            log.info("JSON 단어 리스트 추출 완료");
//
//            // 결과 출력
////            for (Map.Entry<String, List<List<String[]>>> entry : jsonDictionary.entrySet()) {
////                String country = entry.getKey();
////                List<List<String[]>> forms = entry.getValue();
////                System.out.println("국가: " + country);
////                for (List<String[]> form : forms) {
////                    for (String[] details : form) {
////                        System.out.print("[");
////                        for (String detail : details) {
////                            System.out.print(detail + ", ");
////                        }
////                        System.out.print("] ");
////                    }
////                    System.out.println();
////                }
////                System.out.println();
////            }
//
//        } catch (Exception e) {
//            log.error("JSON 단어 리스트 추출 실패: {}", e.getStackTrace()[0]);
//        }
//
//        return jsonDictionary;
//    }

    public static Map<String, List<Map<String, Object>>> getJsonDictionary2(String decodeText) throws Exception {
        Map<String, List<Map<String, Object>>> jsonDictionary = new HashMap<>();
        try {
            JSONObject jsonObject = new JSONObject(decodeText);
            String version = jsonObject.optString("_Version", "Unknown");
            log.info("Json Dictionary Version : {}", version);

            JSONArray countryList = jsonObject.getJSONArray("Country List");
            for (int i = 0; i < countryList.length(); i++) {
                JSONObject country = countryList.getJSONObject(i);
                String countryName = country.getString("Country");
                log.info("Country: {}", countryName);
                JSONArray forms = country.getJSONArray("Template");
                List<Map<String, Object>> formList = new ArrayList<>();
                for (int j = 0; j < forms.length(); j++) {
                    JSONObject form = forms.getJSONObject(j);
                    String formName = form.getString("Template Name");
                    JSONArray languageArray = form.getJSONArray("Language");
                    List<String> languages = new ArrayList<>();
                    for (int l = 0; l < languageArray.length(); l++) {
                        languages.add(languageArray.getString(l));
                    }
                    String disable = form.optString("Disable", "false");
                    log.info("Template: {}, Languages: {}, Disable: {}", formName, languages, disable);

                    boolean disableValue = Boolean.parseBoolean(disable);

                    Map<String, Object> formMap = new HashMap<>();
                    formMap.put("Template Name", formName);
                    formMap.put("Language", languages);
                    formMap.put("Disable", disableValue);

                    // H-RULE
                    JSONArray hrules = form.getJSONArray("H-RULE");
                    List<Map<String, Object>> hRuleList = new ArrayList<>();
                    for (int k = 0; k < hrules.length(); k++) {
                        JSONObject hrule = hrules.getJSONObject(k);
                        String word = hrule.optString("WD", ""); // 기본값 설정
                        double weight = hrule.optDouble("WT", 0.0); // 기본값 설정
                        int pl = hrule.optInt("PL", 0);
                        String kr = hrule.optString("KR", ""); // 기본값 설정
                        Map<String, Object> ruleMap = new HashMap<>();
                        ruleMap.put("WD", word);
                        ruleMap.put("WT", weight);
                        ruleMap.put("PL", pl);
                        ruleMap.put("KR", kr);
                        hRuleList.add(ruleMap);
                        log.debug("WD: {}, WT: {}, PL: {}, KR: {}", word, weight, pl, kr);
                    }
                    formMap.put("H-RULE", hRuleList);

//                    // AI-RULE
//                    JSONArray airules = form.getJSONArray("AI-RULE");
//                    List<Map<String, Object>> aiRuleList = new ArrayList<>();
//                    for (int k = 0; k < airules.length(); k++) {
//                        JSONObject airule = airules.getJSONObject(k);
//                        String word = airule.getString("WD");
//                        double weight = airule.getDouble("WT");
//                        Map<String, Object> ruleMap = new HashMap<>();
//                        ruleMap.put("WD", word);
//                        ruleMap.put("WT", weight);
//                        aiRuleList.add(ruleMap);
//                        log.info("AI-RULE - WD: {}, WT: {}", word, weight);
//                    }
//                    formMap.put("AI-RULE", aiRuleList);

                    formList.add(formMap);
                }
                jsonDictionary.put(countryName, formList);
            }
            log.info("JSON Word list extraction completed");
        } catch (Exception e) {
            log.error("JSON Word list extraction failed: {}", e.getStackTrace());
        }

        for (Map.Entry<String, List<Map<String, Object>>> countryEntry : jsonDictionary.entrySet()) {
            String countryName = countryEntry.getKey();
            System.out.println("Country: " + countryName);

            List<Map<String, Object>> formList = countryEntry.getValue();
            for (Map<String, Object> formMap : formList) {
                String formName = (String) formMap.get("Template Name");
                List<String> languages = (List<String>) formMap.get("Language");
                Boolean disable = (boolean) formMap.get("Disable");
                System.out.println("  Template Name: " + formName);
                System.out.println("  Languages: " + languages);
                System.out.println("  Disable: " + disable);

                List<Map<String, Object>> hRules = (List<Map<String, Object>>) formMap.get("H-RULE");
                System.out.println("    H-RULE:");
                for (Map<String, Object> hRule : hRules) {
                    String word = (String) hRule.get("WD");
                    double weight = (double) hRule.get("WT");
                    int pl = (int) hRule.get("PL");
                    String kr = (String) hRule.get("KR");
                    System.out.println("      WD: " + word + ", WT: " + weight + ", PL: " + pl + ", KR: " + kr);
                }

//                List<Map<String, Object>> aiRules = (List<Map<String, Object>>) formMap.get("AI-RULE");
//                System.out.println("    AI-RULE:");
//                for (Map<String, Object> aiRule : aiRules) {
//                    String word = (String) aiRule.get("WD");
//                    double weight = (double) aiRule.get("WT");
//                    System.out.println("      WD: " + word + ", WT: " + weight);
//                }
            }
        }
        return jsonDictionary;
    }

//    인코딩 디코딩 하는 코드 옮김 -정다현-
    private static final String ALGORITHM = "AES/CBC/ISO10126Padding";
    private static final String KEY = "iset2021!1234567890abcdefghijkln";
    private static final String IV = "0987654321abcdef";

    private static SecretKeySpec getSecretKeySpec() {
        byte[] keyBytes = KEY.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static IvParameterSpec getIvParameterSpec() {
        return new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] aesEncode(String plainText) throws Exception {

        SecretKeySpec keySpec = getSecretKeySpec();
        IvParameterSpec ivSpec = getIvParameterSpec();

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        // 평문을 바이트 배열로 변환 후 암호화
        return cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
    }

    public static String aesDecode(byte[] encryptedBytes) throws Exception {
        SecretKeySpec keySpec = getSecretKeySpec();
        IvParameterSpec ivSpec = getIvParameterSpec();

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        // 암호화된 바이트 배열을 복호화
        byte[] decrypted = cipher.doFinal(encryptedBytes);

        // 복호화된 바이트 배열을 문자열로 변환하여 반환
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    //CD4 전용 정렬 로직 글자 크기 추가
    public static List<Map<String, Object>> sortAnnotations2(List<Map<String, Object>> items) {
        // Step 1: minY 값을 기준으로 전체 정렬
        items.sort(Comparator.comparingInt(a -> (Integer) a.getOrDefault("minY", Integer.MAX_VALUE)));

        // Step 2: 비슷한 minY 값을 가진 항목들을 그룹화
        List<List<Map<String, Object>>> groups = new ArrayList<>();
        List<Map<String, Object>> currentGroup = new ArrayList<>();
        currentGroup.add(items.get(0));

        for (int i = 1; i < items.size(); i++) {
            Map<String, Object> current = items.get(i);
            Map<String, Object> previous = items.get(i - 1);

            int currentMinY = (Integer) current.getOrDefault("minY", Integer.MAX_VALUE);
            int previousMinY = (Integer) previous.getOrDefault("minY", Integer.MAX_VALUE);

            // minY 값의 차이가 10 이하면 같은 그룹으로 판단
            if (Math.abs(currentMinY - previousMinY) <= 10) {
                currentGroup.add(current);
            } else {
                groups.add(new ArrayList<>(currentGroup));
                currentGroup.clear();
                currentGroup.add(current);
            }
        }
        // 마지막 그룹 처리
        if (!currentGroup.isEmpty()) {
            groups.add(currentGroup);
        }

        // Step 3: 각 그룹 내에서 글자 크기와 X축 위치로 정렬
        for (List<Map<String, Object>> group : groups) {
            group.sort((a, b) -> {
                // 글자 높이 계산 (maxY - minY)
                int heightDiffA = (Integer) a.getOrDefault("maxY", 0) - (Integer) a.getOrDefault("minY", 0);
                int heightDiffB = (Integer) b.getOrDefault("maxY", 0) - (Integer) b.getOrDefault("minY", 0);

                // 글자 높이 차이가 5 이하면 X축 기준으로 정렬
                if (Math.abs(heightDiffA - heightDiffB) <= 5) {
                    return Integer.compare(
                            (Integer) a.getOrDefault("minX", Integer.MAX_VALUE),
                            (Integer) b.getOrDefault("minX", Integer.MAX_VALUE)
                    );
                }
                // 글자 높이 차이가 5보다 크면 높이 기준으로 정렬
                return Integer.compare(heightDiffA, heightDiffB);
            });
        }

        // Step 4: 모든 그룹을 하나의 리스트로 다시 합침
        return groups.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

}
