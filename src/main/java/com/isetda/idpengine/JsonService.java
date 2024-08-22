package com.isetda.idpengine;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class JsonService {
    public String jsonLocal ="";

    private String jsonFilePath; // JSON 파일 경로를 저장할 변수

    public JSONObject jsonObject;
    public List<Map<String, Object>> wordList;

    private static final Logger log = LogManager.getLogger(JsonService.class);

    public JsonService(String jsonFilePath) {
        this.jsonFilePath = jsonFilePath;
        loadJsonData();
        getWordPosition();
    }

    public void loadJsonData() {
        try {
            jsonObject = new JSONObject(FileUtils.readFileToString(new File(jsonFilePath), "UTF-8"));
        } catch (IOException e) {
            log.error("Error reading JSON file", e);
        }
    }

    //그룹화 json 가공
    public void getWordPosition() {
        wordList = new ArrayList<>(); // 단일 리스트로 변경

        JSONObject responsesObject = jsonObject.getJSONArray("responses").getJSONObject(0);
        JSONArray textAnnotationsArray = responsesObject.getJSONArray("textAnnotations");
        jsonLocal = textAnnotationsArray.getJSONObject(0).getString("locale");

        for (int i = 1; i < textAnnotationsArray.length(); i++) {
            Map<String, Object> data = new HashMap<>();
            JSONObject textAnnotation = textAnnotationsArray.getJSONObject(i);

            String description = textAnnotation.getString("description");
            JSONArray verticesArray = textAnnotation.getJSONObject("boundingPoly").getJSONArray("vertices");

            List<Map<String, Integer>> coordinates = new ArrayList<>();
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;

            for (int j = 0; j < verticesArray.length(); j++) {
                JSONObject vertices = verticesArray.getJSONObject(j);
                int x = vertices.getInt("x");
                int y = vertices.getInt("y");

                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;

                Map<String, Integer> coordinate = new HashMap<>();
                coordinate.put("x", x);
                coordinate.put("y", y);
                coordinates.add(coordinate);
            }

            data.put("description", description);
            data.put("vertices", coordinates);
            data.put("minX", minX);
            data.put("minY", minY);
            data.put("maxX", maxX);
            data.put("maxY", maxY);

            wordList.add(data);
        }

        // Y축 우선, 그 다음 X축으로 정렬
        wordList.sort(Comparator.comparingInt((Map<String, Object> a) -> (int) a.get("minY")).thenComparingInt(a -> (int) a.get("maxX")));
        System.out.println(jsonLocal);
        // 정렬된 결과 출력
//        for (Map<String, Object> item : wordList) {
//            System.out.println("Word:");
//            System.out.println("  Description: " + item.get("description"));
//            System.out.println("  Coordinates: " + item.get("vertices"));
//            System.out.println("  Min X: " + item.get("minX") + ", Min Y: " + item.get("minY"));
//            System.out.println("  Max X: " + item.get("maxX") + ", Max Y: " + item.get("maxY"));
//            System.out.println();
//        }
    }

}
