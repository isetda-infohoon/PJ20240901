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
            getWordPosition2();
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


    public static List<Map<String, Object>> sortAnnotations(List<Map<String, Object>> items) {
        //System.out.println("원본 데이터:");
        //printItems(items);

        // Step 1: Sort by minY
        items.sort(Comparator.comparingInt(a -> (Integer) a.getOrDefault("minY", Integer.MAX_VALUE)));
        //System.out.println("\nStep 1 - minY 기준 정렬 후:");
        //printItems(items);

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

        //System.out.println("\nStep 2 - minY 값이 비슷한 항목들의 그룹:");
//        for (int i = 0; i < groups.size(); i++) {
//            System.out.println("Group " + (i + 1) + ":");
//            printItems(groups.get(i));
//        }

        // Step 3: Sort each group by minX
        for (List<Map<String, Object>> group : groups) {
            group.sort(Comparator.comparingInt(a -> (Integer) a.getOrDefault("minX", Integer.MAX_VALUE)));
        }

        //System.out.println("\nStep 3 - 각 그룹 내 minX 기준 정렬 후:");
//        for (int i = 0; i < groups.size(); i++) {
//            System.out.println("Group " + (i + 1) + ":");
//            printItems(groups.get(i));
//        }

        // Step 4: Flatten the groups back into a single list
        List<Map<String, Object>> result = groups.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        //System.out.println("\nStep 4 - 최종 정렬 결과:");
        //printItems(result);
        jsonCollection3 = result;
        return result;
    }

    // 항목들을 보기 좋게 출력하는 헬퍼 메소드
    public static void printItems(List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            System.out.printf("minY: %d, minX: %d, text: %s%n",
                    item.getOrDefault("minY", Integer.MAX_VALUE),
                    item.getOrDefault("minX", Integer.MAX_VALUE),
                    item.getOrDefault("text", item.get("description")));
        }
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
            log.info("Language Code: {}", jsonLocal);

            log.debug("Start word position extraction");

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

            log.debug("Word position extraction and sorting completed");

        } catch (JSONException e) {
            log.error("Error processing JSON: ", e);
            jsonCollection.add(createEmptyData());
        } catch (Exception e) {
            log.error("Error extracting word location: {}", e.getMessage(), e);
            jsonCollection.add(createEmptyData());
        }
    }

    // Synap OCR JSON(.dat) 가공
    public void getWordPosition2() {
        jsonCollection = new ArrayList<>();

        try {
            // .dat(JSON)이 Google OCR 결과인 경우
            if (jsonObject.has("responses")) {
                JSONObject responsesObject = jsonObject.getJSONArray("responses").getJSONObject(0);

                if (responsesObject == null) {
                    log.warn("No valid responsesObject found, skipping extraction.");
                    jsonCollection.add(createEmptyData());
                    return;
                }

                JSONArray textAnnotationArray = responsesObject.getJSONArray("textAnnotations");

                if (textAnnotationArray == null || textAnnotationArray.length() == 0) {
                    log.warn("No valid textAnnotationsArray found, skipping extraction.");
                    jsonCollection.add(createEmptyData());
                    return;
                }

                jsonLocal = textAnnotationArray.getJSONObject(0).getString("locale");
                log.info("Language Code: {}", jsonLocal);

                log.debug("Start word position extraction");

                JSONObject firstAnnotation = textAnnotationArray.getJSONObject(0);
                Map<String, Object> firstData = processAnnotation(firstAnnotation);
                jsonCollection.add(firstData);

                for (int i = 1; i < textAnnotationArray.length(); i++) {
                    JSONObject textAnnotation = textAnnotationArray.getJSONObject(i);
                    if (textAnnotation != null) {
                        Map<String, Object> data = processAnnotation(textAnnotation);
                        logItemInfo(data);
                        jsonCollection.add(data);
                    }
                }
                // .dat(JSON)이 Synap OCR 결과인 경우
            } else if (jsonObject.has("result")) {
                JSONObject result = jsonObject.getJSONObject("result");

                if (result == null) {
                    log.warn("No valid result object found, skipping extraction.");
                    jsonCollection.add(createEmptyData());
                    return;
                }

                jsonLocal = "";

                Map<String, Object> fullTextData = new LinkedHashMap<>();
                fullTextData.put("midY", 0);
                fullTextData.put("minY", 0);

                JSONArray vertice = new JSONArray();
                vertice.put(new JSONObject().put("x", 0).put("y", 0));
                vertice.put(new JSONObject().put("x", 0).put("y", 0));
                vertice.put(new JSONObject().put("x", 0).put("y", 0));
                vertice.put(new JSONObject().put("x", 0).put("y", 0));
                fullTextData.put("vertices", vertice);

                fullTextData.put("minX", 0);
                fullTextData.put("maxY", 0);
                fullTextData.put("maxX", 0);

                String fullText = result.getString("full_text");
                fullTextData.put("description", fullText);
                jsonCollection.add(fullTextData);

                JSONArray boxes = result.getJSONArray("boxes");

                if (boxes == null || boxes.length() == 0) {
                    log.warn("No valid boxes found, skipping extraction.");
                    return;
                }

                for (int i = 0; i < boxes.length(); i++) {
                    JSONArray box = boxes.getJSONArray(i);
                    if (box == null) {
                        log.warn("Invalid box format at index {}, skipping.", i);
                        continue;
                    }
                    String description = box.getString(5);
                    JSONArray vertices = new JSONArray();

                    for (int j = 0; j < 4; j++) {
                        JSONArray point = box.getJSONArray(j);
                        if (point == null) {
                            log.warn("Invalid point format in box {}, skipping vertex {}", i, j);
                            continue;
                        }
                        JSONObject vertex = new JSONObject();
                        vertex.put("x", point.getInt(0));
                        vertex.put("y", point.getInt(1));
                        vertices.put(vertex);
                    }

                    JSONObject tempAnnotation = new JSONObject();
                    tempAnnotation.put("description", description);
                    JSONObject boundingPoly = new JSONObject();
                    boundingPoly.put("vertices", vertices);
                    tempAnnotation.put("boundingPoly", boundingPoly);

                    Map<String, Object> data = processAnnotation(tempAnnotation);
                    logItemInfo(data);
                    jsonCollection.add(data);
                }

                //JSONArray tableList = result.optJSONArray("table_list");
//                if (tableList != null) {
//                    for (int i = 0; i < tableList.length(); i++) {
//                        JSONObject table = tableList.getJSONObject(i);
//                        JSONArray cells = table.getJSONArray("cells");
//
//                        for (int x = 0; x < cells.length(); x++) {
//                            JSONArray row = cells.getJSONArray(x);
//
//                            for (int y = 0; y < row.length(); y++) {
//                                JSONArray cell = row.getJSONArray(y);
//                                String description = cell.getString(5);
//                                JSONArray vertices = new JSONArray();
//
//                                for (int j = 0; j < 4; j++) {
//                                    JSONArray point = cell.getJSONArray(j);
//                                    JSONObject vertex = new JSONObject();
//                                    vertex.put("x", point.getInt(0));
//                                    vertex.put("y", point.getInt(1));
//                                    vertices.put(vertex);
//                                }
//
//                                JSONObject tempAnnotation = new JSONObject();
//                                tempAnnotation.put("description", description);
//                                JSONObject boundingPoly = new JSONObject();
//                                boundingPoly.put("vertices", vertices);
//                                tempAnnotation.put("boundingPoly", boundingPoly);
//
//                                Map<String, Object> data = processAnnotation(tempAnnotation);
//                                logItemInfo(data);
//                                jsonCollection.add(data);
//                            }
//                        }
//                    }
//                }
            }

            log.debug("Word position extraction and sorting completed");
        } catch (JSONException e) {
            log.error("Error processing JSON: {}", e);
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

        log.trace(String.format("L:%-45s|MinX:%5d|MinY:%5d|MaxX:%5d|MaxY:%5d|MidY:%5d|W:%-20s",
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
            log.trace("The first word: {}", currentText);

            // 단일 단어 자체를 결과에 추가
            addToCollection2(checkText, currentText.toString(), startX, startY, startMaxX, startMaxY);

            // 연속된 단어 조합 생성
            for (int j = i + 1; j < words.size(); j++) {
                Map<String, Object> nextWord = words.get(j);
//          log.info("체크 단어 : {}",checkText);
//          log.info("다음 단어 : {}",nextWord);

                if (isOnSameLineByMidY2(checkText.get(checkText.size() - 1), nextWord)) {
                    currentText.append(nextWord.get("description"));
                    log.trace("Group words:  {}", currentText); //여기 띄우기
                    a++;
                    checkText.add(nextWord);
                    maxX = Math.max(maxX, (int) nextWord.getOrDefault("maxX", 0));
                    maxY = Math.max(maxY, (int) nextWord.getOrDefault("maxY", 0));

                    // 현재 조합이 targetWords에 포함되는지 확인
                    addToCollection2(checkText, currentText.toString(), startX, startY, maxX, maxY);

                } else {
                    log.trace("Connected words: {}", a);
                    break; // 동일한 라인이 아니면 중단
                }
            }
        }
        log.trace("Total connected words: {}", a);

        return results;
    }


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

    public static Map<String, List<Map<String, Object>>> getJsonDictionary2(String decodeText) throws Exception {
        Map<String, List<Map<String, Object>>> jsonDictionary = new HashMap<>();
        try {
            JSONObject jsonObject = new JSONObject(decodeText);
            String version = jsonObject.optString("_Version", "Unknown");
            log.debug("Json Dictionary Version : {}", version);

            JSONArray countryList = jsonObject.getJSONArray("Country List");
            for (int i = 0; i < countryList.length(); i++) {
                JSONObject country = countryList.getJSONObject(i);
                String countryName = country.getString("Country");
                log.trace("Country: {}", countryName);
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
                    //log.info("Template: {}, Languages: {}, Disable: {}", formName, languages, disable);

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
                        log.trace("WD: {}, WT: {}, PL: {}, KR: {}", word, weight, pl, kr);
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
            log.trace("JSON Word list extraction completed");
        } catch (Exception e) {
            log.error("JSON Word list extraction failed: {}", e.getStackTrace());
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

}
