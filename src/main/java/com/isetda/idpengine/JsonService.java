package com.isetda.idpengine;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
        log.info("JSON 파일 경로: {}", jsonFilePath);
        try {
            this.jsonObject = new JSONObject(FileUtils.readFileToString(new File(jsonFilePath), "UTF-8"));
            log.info("JSON 데이터 로딩 성공");
            getWordPosition();
        } catch (IOException e) {
            log.error("JSON 파일 읽던 중 에러", e);
        }
    }

    private void getWordPosition() {
        log.info("단어 위치 추출 시작");
        jsonCollection = new ArrayList<>();

        JSONObject responsesObject = jsonObject.getJSONArray("responses").getJSONObject(0);
        JSONArray textAnnotationsArray = responsesObject.getJSONArray("textAnnotations");
        jsonLocal = textAnnotationsArray.getJSONObject(0).getString("locale");

        log.info("언어 코드: {}", jsonLocal);

        for (int i = 1; i < textAnnotationsArray.length(); i++) {
            Map<String, Object> data = new HashMap<>();
            JSONObject textAnnotation = textAnnotationsArray.getJSONObject(i);

            String description = textAnnotation.getString("description");
            JSONArray verticesArray = textAnnotation.getJSONObject("boundingPoly").getJSONArray("vertices");

            List<JSONObject> vertices = IntStream.range(0, verticesArray.length())
                    .mapToObj(verticesArray::getJSONObject)
                    .collect(Collectors.toList());

            int minX = vertices.stream().mapToInt(v -> v.getInt("x")).min().orElse(0);
            int minY = vertices.stream().mapToInt(v -> v.getInt("y")).min().orElse(0);
            int maxX = vertices.stream().mapToInt(v -> v.getInt("x")).max().orElse(0);
            int maxY = vertices.stream().mapToInt(v -> v.getInt("y")).max().orElse(0);

            data.put("description", description);
            data.put("vertices", vertices);
            data.put("minX", minX);
            data.put("minY", minY);
            data.put("maxX", maxX);
            data.put("maxY", maxY);

            jsonCollection.add(data);
        }

        jsonCollection.sort(Comparator.comparingInt((Map<String, Object> a) -> (int) a.get("minY"))
                .thenComparingInt(a -> (int) a.get("maxX")));

        log.info("단어 위치 추출 완료");

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

            log.info("단어: {}, 좌표: [{}], Min X: {}, Min Y: {}, Max X: {}, Max Y: {}",
                    description, verticesString, minX, minY, maxX, maxY);
        }
    }

    public void drawMarking(File imageFile, List<Map<String, Object>> jsonCollection) throws IOException {
        log.info("이미지에 마킹을 추가하는 중: {}", imageFile.getAbsolutePath());
        BufferedImage image = ImageIO.read(imageFile);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.RED);
        g2d.setStroke(new java.awt.BasicStroke(2)); // 박스의 두께

        for (Map<String, Object> word : jsonCollection) {
            int minX = (int) word.get("minX");
            int minY = (int) word.get("minY");
            int maxX = (int) word.get("maxX");
            int maxY = (int) word.get("maxY");
            g2d.drawRect(minX, minY, maxX - minX, maxY - minY);
        }
        g2d.dispose();

        File outputImageFile = new File("annotated_" + imageFile.getName());
        ImageIO.write(image, "png", outputImageFile);

        log.info("주석이 추가된 이미지가 저장되었습니다: {}", outputImageFile.getAbsolutePath());
    }

    public static void processMarking(String folderPath, String jsonFolderPath) {
        log.info("이미지와 JSON 파일 처리 시작");
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            log.error("유효하지 않은 폴더 경로: {}", folderPath);
            return;
        }

        File[] imageFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png"));
        if (imageFiles == null || imageFiles.length == 0) {
            log.warn("폴더에서 이미지 파일을 찾을 수 없습니다: {}", folderPath);
            return;
        }

        for (File imageFile : imageFiles) {
            try {
                String baseName = imageFile.getName().substring(0, imageFile.getName().lastIndexOf('.'));
                File jsonFile = new File(jsonFolderPath, baseName + "_OCR_result.json");

                if (jsonFile.exists()) {
                    log.info("JSON 파일 발견: {}", jsonFile.getAbsolutePath());
                    JsonService jsonService = new JsonService(jsonFile.getAbsolutePath());
                    jsonService.drawMarking(imageFile, jsonService.jsonCollection);
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
