package com.isetda.idpengine;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.formula.functions.T;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JsonService {
    public String jsonLocal = "";
    public JSONObject jsonObject;
    public List<Map<String, Object>> jsonCollection;
    public static List<Map<String, Object>> jsonCollection3;
    public static List<Map<String, Object>> jsonCollection2;
    public static ConfigLoader configLoader = ConfigLoader.getInstance();

    private static final Logger log = LogManager.getLogger(JsonService.class);

    // 생성자에서 JSON 데이터 로딩 및 처리
    public JsonService(String jsonFilePath) {
        try {
            this.jsonObject = new JSONObject(FileUtils.readFileToString(new File(jsonFilePath), "UTF-8"));
            log.info("JSON 데이터 로딩 성공");
            getWordPosition();
        } catch (IOException e) {
            log.error("JSON 파일 읽던 중 에러", e);
        }
    }
    //json에서 단어, 위치 가져와서 정렬 (1차)
    public void getWordPosition() {
        jsonCollection = new ArrayList<>();
        try {
            JSONObject responsesObject = jsonObject.getJSONArray("responses").getJSONObject(0);
            JSONArray textAnnotationsArray = responsesObject.getJSONArray("textAnnotations");
            jsonLocal = textAnnotationsArray.getJSONObject(0).getString("locale");
            log.info("언어 코드: {}", jsonLocal);

            log.info("단어 위치 추출 시작");

            for (int i = 1; i < textAnnotationsArray.length(); i++) {
                Map<String, Object> data = new HashMap<>();
                JSONObject textAnnotation = textAnnotationsArray.getJSONObject(i);

                String description = textAnnotation.getString("description");
                JSONArray verticesArray = textAnnotation.getJSONObject("boundingPoly").getJSONArray("vertices");

                List<JSONObject> vertices = IntStream.range(0, verticesArray.length()).mapToObj(verticesArray::getJSONObject).collect(Collectors.toList());

                int minX = vertices.stream().mapToInt(v -> v.getInt("x")).min().orElse(0);
                int minY = vertices.stream().mapToInt(v -> v.getInt("y")).min().orElse(0);
                int maxX = vertices.stream().mapToInt(v -> v.getInt("x")).max().orElse(0);
                int maxY = vertices.stream().mapToInt(v -> v.getInt("y")).max().orElse(0);

                int midY = (minY + maxY) / 2; // MinY와 MaxY의 중간 값 계산

                data.put("description", description);
                data.put("vertices", vertices);
                data.put("minX", minX);
                data.put("minY", minY);
                data.put("maxX", maxX);
                data.put("maxY", maxY);
                data.put("midY", midY); // 중간 값 저장

                jsonCollection.add(data);
            }

            // 중간 값을 기준으로 정렬
            jsonCollection.sort((a, b) -> {
                int midY_a = ((int) a.get("minY") + (int) a.get("maxY")) / 2;
                int midY_b = ((int) b.get("minY") + (int) b.get("maxY")) / 2;

                int minY_b = (int) b.get("minY");
                int maxY_b = (int) b.get("maxY");

                int minY_a = (int) a.get("minY");
                int maxY_a = (int) a.get("maxY");

                // 오차 범위를 고려한 midY 정렬
                if ((midY_a>=minY_b && midY_a<=maxY_b) || (midY_b>=minY_a && midY_b<=maxY_a)) {
                    // midY가 비슷하면 minX로 정렬
                    return Integer.compare((int) a.get("minX"), (int) b.get("minX"));
                } else {
                    // midY로 정렬
                    return Integer.compare(midY_a, midY_b);
                }
            });

//            for (Map<String, Object> item : jsonCollection) {
//                String description = (String) item.get("description");
//                List<JSONObject> vertices = (List<JSONObject>) item.get("vertices");
//                int minX = (int) item.get("minX");
//                int minY = (int) item.get("minY");
//                int maxX = (int) item.get("maxX");
//                int maxY = (int) item.get("maxY");
//
//                // 로그 쌓기 위한 것
//                String verticesString = vertices.stream()
//                        .map(v -> String.format("(%d, %d)", v.getInt("x"), v.getInt("y")))
//                        .collect(Collectors.joining(", "));
//
//                log.info(String.format("L:%-45s|MinX:%5d|MinY:%5d|MaxX:%5d|MaxY:%5d|W:%-20s", verticesString, minX, minY, maxX, maxY, description));
//            }
            log.info("단어 위치 추출 완료");
        } catch (Exception e) {
            log.error("단어 위치 추출 중 오류 발생: {}", e.getMessage(), e);
        }
    }


    //json을 통해서 2차 매칭 (y축 기준으로 그룹화)[경우의 수를 통한 그룹화]
    public static List<Map<String, Object>> findMatchingWords(List<Map<String, Object>> words) {
        List<Map<String, Object>> results = new ArrayList<>();
        jsonCollection2 = new ArrayList<>();
        int a = 0;

        List<Map<String, Object>> checkText = null;
        for (int i = 0; i < words.size(); i++) {
            a = 0;
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

            // 단일 단어 자체를 결과에 추가
//            addToResults(results, checkText, currentText.toString().replace(" ", ""), startX, startY, startMaxX, startMaxY, targetWords);
            addToCollection2(checkText, currentText.toString(), startX, startY, startMaxX, startMaxY);
            // 연속된 단어 조합 생성
            for (int j = i + 1; j < words.size(); j++) {
                Map<String, Object> nextWord = words.get(j);
                if (isOnSameLineByMidY2(checkText.get(checkText.size() - 1), nextWord)) {
                    log.info("연결 단어:{}", currentText);
                    currentText.append(nextWord.get("description"));
                    a++;
                    checkText.add(nextWord);
                    maxX = Math.max(maxX, (int) nextWord.get("maxX"));
                    maxY = Math.max(maxY, (int) nextWord.get("maxY"));

                    // 현재 조합이 targetWords에 포함되는지 확인
//                    addToResults(results, checkText, currentText.toString().replace(" ", ""), startX, startY, maxX, maxY, targetWords);
//                    addToCollection2(checkText, currentText.toString(), startX, startY, startMaxX, startMaxY);
                    addToCollection2(checkText, currentText.toString(), startX, startY, maxX, maxY);

                } else {
                    log.info("연결된 단어 전체 경우의 수: {}", a);
                    break; // 동일한 라인이 아니면 중단
                }
            }
        }
        log.info("연결된 단어 전체 경우의 수: {}", a);
//        log.info("안녕 @@@: {}", jsonCollection2);

//        return jsonCollection2;
        return results;
    }

    public static List<Map<String, Object>>findtheword(Set<String> targetWords){
        for (Map<String, Object> item : jsonCollection2) {
            String description = (String) item.get("description");
            if (targetWords.contains(description)) {
                jsonCollection3 = jsonCollection2;
            }
        }
        return jsonCollection3;
    }

    //엑셀 단어와 매칭된 결과만 저장
    private static void addToResults(List<Map<String, Object>> results, List<Map<String, Object>> checkText,
                                     String combinedText, int startX, int startY, int maxX, int maxY, List<String> targetWords) {
        // 조합된 텍스트가 targetWords에 포함되는지 확인
        Map<String, Object> result = new HashMap<>();
        for (Map<String, Object> item : jsonCollection2) {
                String description = (String) item.get("description");
            if (targetWords.contains(description)) {
                int minX = jsonCollection2.stream().mapToInt(w -> (int) w.get("minX")).min().orElse(startX);
                result.put("description", description);
                result.put("minX", minX);
                result.put("minY", startY);
                result.put("maxX", maxX);
                result.put("maxY", maxY);
                results.add(result);
            }
        }
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

//    private boolean isOnSameLineByMidY(Map<String, Object> a, Map<String, Object> b) {
//        int midY_a = ((int) a.get("minY") + (int) a.get("maxY")) / 2;
//        int midY_b = ((int) b.get("minY") + (int) b.get("maxY")) / 2;
//
//        return Math.abs(midY_a - midY_b) <= 3; // 중간 값의 차이가 3 이내면 같은 라인으로 간주
//    }
    private static boolean isOnSameLineByMidY2(Map<String, Object> a, Map<String, Object> b) {
        int midY_a = ((int) a.get("minY") + (int) a.get("maxY")) / 2;
        int minY_b = (int) b.get("minY");
        int maxY_b = (int) b.get("maxY");

        return midY_a>=minY_b && midY_a<= maxY_b; // 중간 값의 차이가 3 이내면 같은 라인으로 간주
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

    public static Map<String, List<List<String[]>>> getJsonDictionary() {
        String filePath = configLoader.jsonFilePath;
        Map<String, List<List<String[]>>> jsonDictionary = new HashMap<>();

        try {
            String content = FileUtils.readFileToString(new File(filePath), "UTF-8");
            JSONObject jsonObject = new JSONObject(content);

            JSONArray countryList = jsonObject.getJSONArray("국가 리스트");

            for (int i = 0; i < countryList.length(); i++) {
                JSONObject country = countryList.getJSONObject(i);
                String countryName = country.getString("국가");

                JSONArray forms = country.getJSONArray("양식");
                List<List<String[]>> formList = new ArrayList<>();

                for (int j = 0; j < forms.length(); j++) {
                    JSONObject form = forms.getJSONObject(j);
                    String formName = form.getString("양식명");
                    String language = form.getString("언어");

                    List<String[]> ruleList = new ArrayList<>();
                    ruleList.add(new String[]{formName, language});

                    JSONArray rules = form.getJSONArray("RULE");
                    for (int k = 0; k < rules.length(); k++) {
                        JSONObject rule = rules.getJSONObject(k);
                        String word = rule.getString("단어");
                        String weight = String.valueOf(rule.getDouble("가중치"));
                        ruleList.add(new String[]{word, weight});
                    }

                    formList.add(ruleList);
                }

                jsonDictionary.put(countryName, formList);
            }

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
            e.printStackTrace();
        }

        return jsonDictionary;
    }

}
