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

    public void drawMarking(File imageFile, List<Map<String, Object>> jsonCollection, Map<String, List<List<String[]>>> excelData , String documentType,String jsonLocal ) throws IOException {
        log.info("이미지에 마킹을 추가하는 중: {}", imageFile.getAbsolutePath());
//        ExcelService excelService =new ExcelService();

        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            log.error("이미지를 읽어오는 데 실패했습니다: {}", imageFile.getAbsolutePath());
            return;
        }

        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.RED);
        g2d.setStroke(new java.awt.BasicStroke(1)); // 박스의 두께

        // 엑셀 데이터에서 해당 문서 양식에 해당하는 데이터만 추출
        List<List<String[]>> excelRows = excelData.get(jsonLocal);
        if (excelRows == null) {
            log.warn("해당하는 엑셀 데이터가 없습니다: {}", jsonLocal);
            return;
        }
        //안녕22

        Set<String> excelWords = new HashSet<>();

        // "사업자등록증(영업허가증)" 또는 다른 문서 양식 이름에 해당하는 데이터를 가져오기
        // 또는 다른 문서 양식 이름으로 변경 가능
        log.info("문서 타입 : {}",documentType);
        for (List<String[]> row : excelRows) {
            if (row.size() > 0 && documentType.equals(row.get(0)[0])) {
                for (int i = 1; i < row.size(); i++) { // 첫 번째 열은 컬럼 이름이므로 제외
                    excelWords.add(row.get(i)[0]); // 문서 양식에 해당하는 단어들만 수집
                }
//                break; // 해당 문서 양식에 해당하는 행을 찾았으므로 루프 종료
            }
        }

        if (excelWords.isEmpty()) {
            log.warn("문서 양식 '{}'에 해당하는 단어가 없습니다.", documentType);
            return;
        }

        log.info("엑셀에서 추출한 단어들: {}", excelWords);

        //연속되는 단어들
//        List<Map<String, Object>> matchedWords = JsonService.findMatchingWords(jsonCollection);
        JsonService.findMatchingWords(jsonCollection);
//        log.info("확인 작업 : {}", JsonService.jsonCollection2);
        List<Map<String, Object>> matchedWords = JsonService.findtheword(excelWords);

        for (Map<String, Object> word : matchedWords) {
            String description = (String) word.get("description");

            if (excelWords.contains(description)) {
                int minX = (int) word.get("minX");
                int minY = (int) word.get("minY");
                int maxX = (int) word.get("maxX");
                int maxY = (int) word.get("maxY");

                log.info("W:[{}],L:MinX:{},MinY:{},MaxX:{},MaxY:{}",
                        description, minX, minY, maxX, maxY);

                g2d.drawRect(minX, minY, maxX - minX, maxY - minY);
            }
        }
        g2d.dispose();

        // 이미지 파일의 이름을 PNG 확장자로 변경
        String outputImagePath = imageFile.getParent() + File.separator + imageFile.getName().substring(0, imageFile.getName().lastIndexOf('.')) + "_annotated.png";
        File outputImageFile = new File(outputImagePath);
        ImageIO.write(image, "png", outputImageFile);

        log.info("주석이 추가된 이미지가 저장되었습니다: {}", outputImageFile.getAbsolutePath());
    }


    public void processMarking(Map<String, List<List<String[]>>> excelData, String resultfolderPath, String documentType) throws IOException {

        log.info("이미지와 JSON 파일 처리 시작");
        File folder = new File(resultfolderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            log.warn("유효하지 않은 폴더 경로입니다: {}", resultfolderPath);
            return;
        }

        File[] imageFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpeg") && !name.toLowerCase().contains("_annotated.png"));
        if (imageFiles == null || imageFiles.length == 0) {
            log.warn("폴더에 이미지 파일이 없습니다: {}", resultfolderPath);
            return;
        }

        for (File imageFile : imageFiles) {
            String baseName = imageFile.getName().substring(0, imageFile.getName().lastIndexOf('.'));
            File jsonFile = new File(resultfolderPath, baseName + "_result.json");

            if (jsonFile.exists()) {
                log.info("JSON 파일 경로: {}", jsonFile.getAbsolutePath());
                JsonService jsonService = new JsonService(jsonFile.getAbsolutePath());
                log.info("1111");
                this.drawMarking(imageFile, jsonService.jsonCollection, excelData,documentType,jsonService.jsonLocal);
            } else {
                log.warn("이미지에 대한 JSON 파일을 찾을 수 없습니다: {}", imageFile.getName());
            }
        }
        log.info("이미지 및 JSON 파일 처리 완료");
    }
}
