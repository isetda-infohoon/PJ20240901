package com.isetda.idpengine;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.formula.functions.T;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
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
            this.jsonObject = new JSONObject(FileUtils.readFileToString(new File(jsonFilePath), "UTF-8"));
            log.info("JSON data loading successful");
            getWordPosition();
        } catch (IOException e) {
            log.error("Error reading json file", e);
        }
    }
    //json에서 단어, 위치 가져와서 정렬 (1차)
    public void getWordPosition() {
        jsonCollection = new ArrayList<>();
        try {
            JSONObject responsesObject = jsonObject.getJSONArray("responses").getJSONObject(0);
            JSONArray textAnnotationsArray = responsesObject.getJSONArray("textAnnotations");
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
                Map<String, Object> data = processAnnotation(textAnnotation);
                jsonCollection.add(data);
            }

            // 첫 번째 항목을 제외한 나머지 항목들에 대해 정렬 수행
            List<Map<String, Object>> remainingItems = jsonCollection.subList(1, jsonCollection.size());

            // midY를 기준으로 정렬
            remainingItems.sort(Comparator.comparingInt(a -> {
                        Integer midY = (Integer) ((Map<String, Object>) a).get("midY");
                        return midY != null ? midY : Integer.MAX_VALUE; // null인 경우 최대값으로 처리
                    })
                    .thenComparingInt(a -> {
                        Integer minX = (Integer) ((Map<String, Object>) a).get("minX");
                        return minX != null ? minX : Integer.MAX_VALUE; // null인 경우 최대값으로 처리
                    })); // midY가 같으면 minX로 추가 정렬


            // 정렬된 결과를 jsonCollection에 다시 설정
            jsonCollection = new ArrayList<>();
            jsonCollection.add(firstData); // 첫 번째 항목 추가
            jsonCollection.addAll(remainingItems); // 나머지 항목 추가

            for (Map<String, Object> item : jsonCollection) {
                logItemInfo(item);
            }
            log.info("Word position extraction successful");
        } catch (Exception e) {
            log.error("Error extracting word location: {}", e.getMessage(), e);
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

    private void logItemInfo(Map<String, Object> item) {
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
    public static List<Map<String, Object>> findMatchingWords(List<Map<String, Object>> words) {
        List<Map<String, Object>> results = new ArrayList<>();
        jsonCollection2 = new ArrayList<>();
        int a = 0;

        List<Map<String, Object>> checkText = null;
        for (int i = 0; i < words.size(); i++) {
            a = 1;
            StringBuilder currentText = new StringBuilder();
            int startMaxX = (int) words.get(i).get("maxX");
            int startMaxY = (int) words.get(i).get("maxY");
            int startX = (int) words.get(i).get("minX");
            int startY = (int) words.get(i).get("minY");
            int maxX = startX;
            int maxY = startY;

            // 단어 자체도 추가
            checkText = new ArrayList<>();
            Map<String, Object> singleWord = words.get(i);
            checkText.add(singleWord);
            currentText.append(singleWord.get("description"));
            log.debug("The first word :{}",currentText);

            // 단일 단어 자체를 결과에 추가
//            addToResults(results, checkText, currentText.toString().replace(" ", ""), startX, startY, startMaxX, startMaxY, targetWords);
            addToCollection2(checkText, currentText.toString(), startX, startY, startMaxX, startMaxY);
            // 연속된 단어 조합 생성
            for (int j = i + 1; j < words.size(); j++) {
                Map<String, Object> nextWord = words.get(j);
//                log.info("체크 단어 : {}",checkText);
//                log.info("다음 단어 : {}",nextWord);
                if (isOnSameLineByMidY2(checkText.get(checkText.size() - 1), nextWord)) {
                    currentText.append(nextWord.get("description"));
                    log.debug("Group words:{}", currentText);
                    a++;
                    checkText.add(nextWord);
                    maxX = Math.max(maxX, (int) nextWord.get("maxX"));
                    maxY = Math.max(maxY, (int) nextWord.get("maxY"));

                    // 현재 조합이 targetWords에 포함되는지 확인
//                    addToResults(results, checkText, currentText.toString().replace(" ", ""), startX, startY, maxX, maxY, targetWords);
//                    addToCollection2(checkText, currentText.toString(), startX, startY, startMaxX, startMaxY);
                    addToCollection2(checkText, currentText.toString(), startX, startY, maxX, maxY);

                } else {
                    log.debug("Connected words: {}", a);
                    break; // 동일한 라인이 아니면 중단
                }
            }
        }
        log.debug("Total connected words: {}", a);
//        log.info("안녕 @@@: {}", jsonCollection2);

//        return jsonCollection2;
        return results;
    }
    //TODO 아직 더 수정해야 됨 match2에서 단어 리스트와 동일한 것의 값을 가져오면서 그에 해당하는 좌표를 가져와야 함
    public static List<Map<String, Object>>findtheword(Set<String> targetWords){
        jsonCollection3 = new ArrayList<>();
        for (Map<String, Object> item : jsonCollection2) {
            String description = (String) item.get("description");
            if (targetWords.contains(description)) {
//                log.info("확인 : {}",item);
                jsonCollection3.add(item);
            }
        }
//        log.info("jsonCollection3 :{}",jsonCollection3);
        return jsonCollection3;
    }

//    //엑셀 단어와 매칭된 결과만 저장
//    private static void addToResults(List<Map<String, Object>> results, List<Map<String, Object>> checkText,
//                                     String combinedText, int startX, int startY, int maxX, int maxY, List<String> targetWords) {
//        // 조합된 텍스트가 targetWords에 포함되는지 확인
//        Map<String, Object> result = new HashMap<>();
//        for (Map<String, Object> item : jsonCollection2) {
//                String description = (String) item.get("description");
//            if (targetWords.contains(description)) {
//                int minX = jsonCollection2.stream().mapToInt(w -> (int) w.get("minX")).min().orElse(startX);
//                result.put("description", description);
//                result.put("minX", minX);
//                result.put("minY", startY);
//                result.put("maxX", maxX);
//                result.put("maxY", maxY);
//                results.add(result);
//            }
//        }
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


        return midY_a>=minY_b && midY_a<= maxY_b && minX_b - maxX_a <100; //
    }


//    public void drawMarking(File imageFile, List<Map<String, Object>> jsonCollection, Map<String, List<List<String[]>>> excelData ,String documentType ) throws IOException {
//        log.info("이미지에 마킹을 추가하는 중: {}", imageFile.getAbsolutePath());
//        ExcelService excelService =new ExcelService();
//
//        BufferedImage image = ImageIO.read(imageFile);
//        if (image == null) {
//            log.error("이미지를 읽어오는 데 실패했습니다: {}", imageFile.getAbsolutePath());
//            return;
//        }
//
//        Graphics2D g2d = image.createGraphics();
//        g2d.setColor(Color.RED);
//        g2d.setStroke(new java.awt.BasicStroke(1)); // 박스의 두께
//
//        // 엑셀 데이터에서 해당 문서 양식에 해당하는 데이터만 추출
//        List<List<String[]>> excelRows = excelData.get(jsonLocal);
//        if (excelRows == null) {
//            log.warn("해당하는 엑셀 데이터가 없습니다: {}", jsonLocal);
//            return;
//        }
//        //안녕22
//
//        Set<String> excelWords = new HashSet<>();
//
//        // "사업자등록증(영업허가증)" 또는 다른 문서 양식 이름에 해당하는 데이터를 가져오기
//        // 또는 다른 문서 양식 이름으로 변경 가능
//        log.info("문서 타입 : {}",documentType);
//        for (List<String[]> row : excelRows) {
//            if (row.size() > 0 && documentType.equals(row.get(0)[0])) {
//                for (int i = 1; i < row.size(); i++) { // 첫 번째 열은 컬럼 이름이므로 제외
//                    excelWords.add(row.get(i)[0]); // 문서 양식에 해당하는 단어들만 수집
//                }
//                break; // 해당 문서 양식에 해당하는 행을 찾았으므로 루프 종료
//            }
//        }
//
//        if (excelWords.isEmpty()) {
//            log.warn("문서 양식 '{}'에 해당하는 단어가 없습니다.", documentType);
//            return;
//        }
//
//        log.info("엑셀에서 추출한 단어들: {}", excelWords);
//
//        List<Map<String, Object>> matchedWords = findMatchingWords(jsonCollection, new ArrayList<>(excelWords));
//
//        for (Map<String, Object> word : matchedWords) {
//            String description = (String) word.get("description");
//
//            if (excelWords.contains(description)) {
//                int minX = (int) word.get("minX");
//                int minY = (int) word.get("minY");
//                int maxX = (int) word.get("maxX");
//                int maxY = (int) word.get("maxY");
//
//                log.info("W:[{}],L:MinX:{},MinY:{},MaxX:{},MaxY:{}",
//                        description, minX, minY, maxX, maxY);
//
//                g2d.drawRect(minX, minY, maxX - minX, maxY - minY);
//            }
//        }
//        g2d.dispose();
//
//        // 이미지 파일의 이름을 PNG 확장자로 변경
//        String outputImagePath = imageFile.getParent() + File.separator + imageFile.getName().substring(0, imageFile.getName().lastIndexOf('.')) + "_annotated.png";
//        File outputImageFile = new File(outputImagePath);
//        ImageIO.write(image, "png", outputImageFile);
//
//        log.info("주석이 추가된 이미지가 저장되었습니다: {}", outputImageFile.getAbsolutePath());
//    }
//
//
//    public static void processMarking(Map<String, List<List<String[]>>> excelData, String resultfolderPath,String documentType) throws IOException {
//
//        log.info("이미지와 JSON 파일 처리 시작");
//        File folder = new File(resultfolderPath);
//        if (!folder.exists() || !folder.isDirectory()) {
//            log.warn("유효하지 않은 폴더 경로입니다: {}", resultfolderPath);
//            return;
//        }
//
//        File[] imageFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpeg") && !name.toLowerCase().contains("_annotated.png"));
//        if (imageFiles == null || imageFiles.length == 0) {
//            log.warn("폴더에 이미지 파일이 없습니다: {}", resultfolderPath);
//            return;
//        }
//
//        for (File imageFile : imageFiles) {
//            String baseName = imageFile.getName().substring(0, imageFile.getName().lastIndexOf('.'));
//            File jsonFile = new File(resultfolderPath, baseName + "_result.json");
//
//            if (jsonFile.exists()) {
//                log.info("JSON 파일 경로: {}", jsonFile.getAbsolutePath());
//                JsonService jsonService = new JsonService(jsonFile.getAbsolutePath());
//                jsonService.drawMarking(imageFile, jsonService.jsonCollection, excelData,documentType);
//            } else {
//                log.warn("이미지에 대한 JSON 파일을 찾을 수 없습니다: {}", imageFile.getName());
//            }
//        }
//        log.info("이미지 및 JSON 파일 처리 완료");
//    }

    public static Map<String, List<List<String[]>>> getJsonDictionary(String decodeText) throws Exception {
        //String filePath = configLoader.jsonFilePath;
        Map<String, List<List<String[]>>> jsonDictionary = new HashMap<>();

        try {
            //String content = FileUtils.readFileToString(new File(filePath), "UTF-8");
            JSONObject jsonObject = new JSONObject(decodeText);

            JSONArray countryList = jsonObject.getJSONArray("국가 리스트");

            for (int i = 0; i < countryList.length(); i++) {
                JSONObject country = countryList.getJSONObject(i);
                String countryName = country.getString("국가");

                log.info("Country: {}", countryName);

                JSONArray forms = country.getJSONArray("양식");
                List<List<String[]>> formList = new ArrayList<>();

                for (int j = 0; j < forms.length(); j++) {
                    JSONObject form = forms.getJSONObject(j);
                    String formName = form.getString("양식명");
                    String language = form.getString("언어");

                    log.info("H: {}, {}", formName, language);

                    List<String[]> ruleList = new ArrayList<>();
                    ruleList.add(new String[]{formName, language});

                    JSONArray rules = form.getJSONArray("RULE");
                    for (int k = 0; k < rules.length(); k++) {
                        JSONObject rule = rules.getJSONObject(k);
                        String word = rule.getString("단어");
                        String weight = String.valueOf(rule.getDouble("가중치"));
                        ruleList.add(new String[]{word, weight});

                        log.info("W: {}, {}", word, weight);
                    }

                    formList.add(ruleList);
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
