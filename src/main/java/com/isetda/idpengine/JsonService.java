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

    private void getWordPosition() {
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
                int midY_a = (int) a.get("midY");
                int midY_b = (int) b.get("midY");

                // 오차 범위를 고려한 midY 정렬
                if (Math.abs(midY_a - midY_b) <= 3) {
                    // midY가 비슷하면 minX로 정렬
                    return Integer.compare((int) a.get("minX"), (int) b.get("minX"));
                } else {
                    // midY로 정렬
                    return Integer.compare(midY_a, midY_b);
                }
            });

            for (Map<String, Object> item : jsonCollection) {
                String description = (String) item.get("description");
                List<JSONObject> vertices = (List<JSONObject>) item.get("vertices");
                int minX = (int) item.get("minX");
                int minY = (int) item.get("minY");
                int maxX = (int) item.get("maxX");
                int maxY = (int) item.get("maxY");

                // 로그 쌓기 위한 것
                String verticesString = vertices.stream()
                        .map(v -> String.format("(%d, %d)", v.getInt("x"), v.getInt("y")))
                        .collect(Collectors.joining(", "));

                log.info(String.format("좌표: %-45s | Min X: %5d | Min Y: %5d | Max X: %5d | Max Y: %5d | 단어: %-20s ", verticesString, minX, minY, maxX, maxY, description));
            }
            log.info("단어 위치 추출 완료");
        } catch (Exception e) {
            log.error("단어 위치 추출 중 오류 발생: {}", e.getMessage(), e);
        }
    }



    private List<Map<String, Object>> findMatchingWords(List<Map<String, Object>> words, List<String> targetWords) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (int i = 0; i < words.size(); i++) {
            StringBuilder currentText = new StringBuilder();
            int startX = (int) words.get(i).get("minX");
            int startY = (int) words.get(i).get("minY");
            int maxX = startX;
            int maxY = startY;

            // 단어 자체도 추가
            List<Map<String, Object>> checkText = new ArrayList<>();
            Map<String, Object> singleWord = words.get(i);
            checkText.add(singleWord);
            currentText.append(singleWord.get("description"));

            // 단일 단어 자체를 결과에 추가
            addToResults(results, checkText, currentText.toString().replace(" ", ""), startX, startY, maxX, maxY, targetWords);

            // 연속된 단어 조합 생성
            for (int j = i + 1; j < words.size(); j++) {
                Map<String, Object> nextWord = words.get(j);
                if (isOnSameLineByMidY(checkText.get(checkText.size() - 1), nextWord)) {
                    log.info("연결 단어 : {}",currentText);
                    currentText.append(nextWord.get("description"));
                    checkText.add(nextWord);
                    maxX = Math.max(maxX, (int) nextWord.get("maxX"));
                    maxY = Math.max(maxY, (int) nextWord.get("maxY"));

                    // 현재 조합이 targetWords에 포함되는지 확인
                    addToResults(results, checkText, currentText.toString().replace(" ", ""), startX, startY, maxX, maxY, targetWords);
                } else {
                    break; // 동일한 라인이 아니면 중단
                }
            }
        }

        return results;
    }

    private void addToResults(List<Map<String, Object>> results, List<Map<String, Object>> checkText,
                              String combinedText, int startX, int startY, int maxX, int maxY, List<String> targetWords) {
        // 조합된 텍스트가 targetWords에 포함되는지 확인
        if (targetWords.contains(combinedText)) {
            int minX = checkText.stream().mapToInt(w -> (int) w.get("minX")).min().orElse(startX);
            Map<String, Object> result = new HashMap<>();
            result.put("description", combinedText);
            result.put("minX", minX);
            result.put("minY", startY);
            result.put("maxX", maxX);
            result.put("maxY", maxY);
            results.add(result);
        }
    }

    private boolean isOnSameLineByMidY(Map<String, Object> a, Map<String, Object> b) {
        int midY_a = ((int) a.get("minY") + (int) a.get("maxY")) / 2;
        int midY_b = ((int) b.get("minY") + (int) b.get("maxY")) / 2;

        return Math.abs(midY_a - midY_b) <= 3; // 중간 값의 차이가 3 이내면 같은 라인으로 간주
    }






    public List<String> excelData3 = new ArrayList<>();

    public void drawMarking(File imageFile, List<Map<String, Object>> jsonCollection, Map<String, List<List<String[]>>> excelData) throws IOException {
        log.info("이미지에 마킹을 추가하는 중: {}", imageFile.getAbsolutePath());

        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            log.error("이미지를 읽어오는 데 실패했습니다: {}", imageFile.getAbsolutePath());
            return;
        }

        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.RED);
        g2d.setStroke(new java.awt.BasicStroke(1)); // 박스의 두께

        // 엑셀 데이터를 가져와 Set으로 변환
        List<List<String[]>> execelData2 = excelData.get(jsonLocal);
//        log.info("안녕 1 {}",execelData2);

        for (List<String[]> row : execelData2) {
            for (String[] cell : row) {
                // cell 배열의 첫 번째 요소만 추가
                if (cell.length > 0) {
                    excelData3.add(cell[0]);
                }
            }
        }

//        log.info("안녕 2 {}", excelData3);

        // jsonLocal을 대문자로 변환하고 엑셀 데이터에서 해당 로컬에 대한 단어를 추출
        Set<String> excelWords = excelData.getOrDefault(jsonLocal, Collections.emptyList())
                .stream()
                .flatMap(List::stream)
                .flatMap(Arrays::stream)
                .collect(Collectors.toSet());

        log.info("국가 코드: {}", jsonLocal);
        log.info("엑셀 데이터에서 가져온 단어 목록: {}", excelWords);

        List<Map<String, Object>> matchedWords = findMatchingWords(jsonCollection, excelData3);
//        log.info("경우의 수 : {}",matchedWords);

        // jsonCollection을 순회하면서 엑셀 데이터와 일치하는 단어의 좌표만 마킹
        for (Map<String, Object> word : matchedWords) {
            String description = (String) word.get("description");

            if (excelWords.contains(description)) { // 엑셀 데이터와 일치하는지 확인
                int minX = (int) word.get("minX");
                int minY = (int) word.get("minY");
                int maxX = (int) word.get("maxX");
                int maxY = (int) word.get("maxY");

                log.info("일치하는 단어: [{}] , 좌표: Min X: {}, Min Y: {}, Max X: {}, Max Y: {}",
                        description, minX, minY, maxX, maxY);

                // 일치하는 단어의 좌표에 마킹
                g2d.drawRect(minX, minY, maxX - minX, maxY - minY);
            }
        }
        g2d.dispose();

        File outputImageFile = new File(imageFile.getName()+"annotated");
        ImageIO.write(image, "png", outputImageFile);

        log.info("주석이 추가된 이미지가 저장: {}", outputImageFile.getAbsolutePath());
    }

    public static void processMarking(String folderPath, String jsonFolderPath) {
        ExcelService excelService = new ExcelService();
        Map<String, List<List<String[]>>> excelData = excelService.getExcelData();

        log.info("이미지와 JSON 파일 처리 시작");
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            log.warn("유효하지 않은 폴더 경로입니다: {}", folderPath);
            return;
        }

        File[] imageFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png"));
        if (imageFiles == null || imageFiles.length == 0) {
            log.warn("폴더에 이미지 파일이 없습니다: {}", folderPath);
            return;
        }

        for (File imageFile : imageFiles) {
            try {
                String baseName = imageFile.getName().substring(0, imageFile.getName().lastIndexOf('.'));
                File jsonFile = new File(jsonFolderPath, baseName + "_OCR_result.json");

                if (jsonFile.exists()) {
                    log.info("JSON 파일 경로: {}", jsonFile.getAbsolutePath());
                    JsonService jsonService = new JsonService(jsonFile.getAbsolutePath());
                    jsonService.drawMarking(imageFile, jsonService.jsonCollection,excelData);
                } else {
                    log.warn("이미지에 대한 JSON 파일을 찾을 수 없습니다: {}", imageFile.getName());
                }
            } catch (IOException e) {
                log.error("이미지 처리 중 오류 발생: {}", imageFile.getName(), e);
            }
        }
        log.info("이미지 및 JSON 파일 처리 완료");
    }

}
