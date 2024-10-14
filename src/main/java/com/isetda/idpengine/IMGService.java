package com.isetda.idpengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class IMGService {
    private static final Logger log = LogManager.getLogger(IMGService.class);
    public ConfigLoader configLoader;

    public void drawMarking(File imageFile, List<Map<String, Object>> jsonCollection ,int a,String docType) throws IOException {
        log.info("Adding marking to image: {}", imageFile.getAbsolutePath());
        DocumentService documentService = new DocumentService();

        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            log.error("Failed to read image: {}", imageFile.getAbsolutePath());
            return;
        }

        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.RED);
        g2d.setStroke(new java.awt.BasicStroke(1)); // 박스의 두께

        //나라 이름
//        String countryName = documentService.getCountryFromSheetName(jsonLocal);

//        // json 데이터에서 해당 나라 문서 데이터만 추출
//        List<List<String[]>> jsonWord = jsonData.get(countryName);
//        if (jsonWord == null) {
//            log.warn("No JSON data corresponding: {}", jsonLocal);
//            return;
//        }
//        //안녕22
//
//        Set<String> excelWords = new HashSet<>();
//
//        // "사업자등록증(영업허가증)" 또는 다른 문서 양식 이름에 해당하는 데이터를 가져오기
//        // 또는 다른 문서 양식 이름으로 변경 가능
//        log.info("DocumentType : {}",documentType);
//        for (List<String[]> row : jsonWord) {
//            if (row.size() > 0 && documentType.equals(row.get(0)[0])) {
//                for (int i = 1; i < row.size(); i++) {
//                    excelWords.add(row.get(i)[0]);
//                }
//            }
//        }
//
//        if (excelWords.isEmpty()) {
//            log.warn("No words corresponding to document form '{}.", documentType);
//            return;
//        }
//
//        log.info("words extracted from Json: {}", excelWords);
//
//        //연속되는 단어들
////        List<Map<String, Object>> matchedWords = JsonService.findMatchingWords(jsonCollection);
//        JsonService.findMatchingWords(jsonCollection);
////        log.info("확인 작업 : {}", JsonService.jsonCollection2);
//        List<Map<String, Object>> matchedWords = JsonService.findtheword(excelWords);
//        log.info("matchedWords : {}",matchedWords);
//            log.info("jsonCollection 2: {}",jsonCollection);
        if (!docType.equals("미분류")){
            for (Map<String, Object> word : jsonCollection) {
                String description = (String) word.get("description");
//            log.info("word:{}",word);

                int minX = (int) word.get("minX");
                int minY = (int) word.get("minY");
                int maxX = (int) word.get("maxX");
                int maxY = (int) word.get("maxY");

                log.info("W:[{}],L:MinX:{},MinY:{},MaxX:{},MaxY:{}",
                        description, minX, minY, maxX, maxY);

                g2d.drawRect(minX, minY, maxX - minX, maxY - minY);
            }
            g2d.dispose();
        }

        // 이미지 파일의 이름을 PNG 확장자로 변경
        String outputImagePath = imageFile.getParent() + File.separator + imageFile.getName().substring(0, imageFile.getName().lastIndexOf('.')) + "_annotated"+a+".png";
        File outputImageFile = new File(outputImagePath);
        ImageIO.write(image, "png", outputImageFile);

        log.info("Image marking completed: {}", outputImageFile.getAbsolutePath());
    }


    public void processMarking(List<Map<String, Object>> jsonCollection, String resultFilePath, String targetFileName, int a, String docType) throws IOException {
        log.info("Start processing images and JSON files");

        File folder = new File(resultFilePath);
        if (!folder.exists() || !folder.isDirectory()) {
            log.warn("Invalid folder path: {}", resultFilePath);
            return;
        }

        // 확장자가 무엇인지 모르는 상황에서 jpg, png, jpeg를 모두 확인
        String[] extensions = {".jpg", ".png", ".jpeg"};
        File imageFile = null;

        for (String ext : extensions) {
            File tempFile = new File(resultFilePath, targetFileName + ext);
            if (tempFile.exists() && tempFile.isFile()) {
                imageFile = tempFile;
                break;
            }
        }

        if (imageFile == null) {
            log.warn("Image file not found: {}.jpg, {}.png, or {}.jpeg", targetFileName, targetFileName, targetFileName);
            return;
        }

        log.info("imageFile : {}", imageFile.getName());
        String baseName = imageFile.getName().substring(0, imageFile.getName().lastIndexOf('.'));
        File jsonFile = new File(resultFilePath, baseName + "_result.json");

        if (jsonFile.exists()) {
            log.info("JSON file path: {}", jsonFile.getAbsolutePath());
            this.drawMarking(imageFile, jsonCollection,a,docType);
        } else {
            log.warn("JSON file not found for image: {}", imageFile.getName());
        }

        log.info("Image and JSON file processing completed");
    }
}
