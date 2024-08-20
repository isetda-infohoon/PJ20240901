package com.isetda.IDPEngine;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import okhttp3.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;


public class IDPService {
    private static final String XML_FILE_PATH = "src/main/resources/com/isetda/IDPEngine/config.xml";
    private static String PROJECT_ID;
    private static String BUCKET_NAME;
    private static String KEY_FILE_PATH;
    private static String EXCEL_FILE_PATH;

    public String imageFolderPath;
    public String resultFolderPath;

    public File[] imageAndPdfFiles;
    public File[] jsonFiles;
    public JSONObject jsonObject;
    public List<List<String>> resultList; // 각 변수로
    public List<List<String>> resultWord;
    public List<Map<String, Object>> jsonCollection;

    public void getVariable() {
        // Properties 객체 생성
        Properties properties = new Properties();

        try (FileInputStream inputStream = new FileInputStream(new File(XML_FILE_PATH))) {
            // XML 파일을 Properties 객체로 로드
            properties.loadFromXML(inputStream);

            // 환경 설정 값 가져오기
            BUCKET_NAME = properties.getProperty("BUCKET_NAME");
            EXCEL_FILE_PATH = properties.getProperty("EXCEL_FILE_PATH");
            KEY_FILE_PATH = properties.getProperty("KEY_FILE_PATH");
            PROJECT_ID = properties.getProperty("PROJECT_ID");

            // 가져온 값 활용
            System.out.println("Bucket Name: " + BUCKET_NAME);
            System.out.println("Excel File Path: " + EXCEL_FILE_PATH);
            System.out.println("Key File Path: " + KEY_FILE_PATH);
            System.out.println("Project ID: " + PROJECT_ID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 폴더에서 이미지(jpg, png), PDF 파일 가져오기
    public void getFilteredFiles() {
        File folder = new File(imageFolderPath);

        File[] files = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                return lowercaseName.endsWith(".jpg") || lowercaseName.endsWith(".png") || lowercaseName.endsWith(".pdf");
            }
        });

        imageAndPdfFiles = files;
    }

    // 폴더에서 JSON 파일 가져오기
    public void getFilteredJsonFiles() {
        File folder = new File(resultFolderPath);

        File[] files = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                return lowercaseName.endsWith(".json");
            }
        });
        jsonFiles = files;
    }

    // JSON 파일을 JSON Object로 저장
    public void jsonToJsonObject(String jsonFilePath) {
        try {
            String json = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
            jsonObject = new JSONObject(json);

            //System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(jsonObject);
    }

    // 단어, 위치 정보 가져오는 메소드 (jsonObjectSplit)
    public void getWordPosition() {
        jsonCollection = new ArrayList<>();

        JSONObject responsesObject = jsonObject.getJSONArray("responses").getJSONObject(0);
        JSONArray textAnnotationsArray = responsesObject.getJSONArray("textAnnotations");

        for (int i = 0; i < textAnnotationsArray.length(); i++) {
            Map<String, Object> data = new HashMap<>();

            JSONObject textAnnotation = textAnnotationsArray.getJSONObject(i);

            if (i == 0) {
//                String locale = jsonObject.optString("locale", "N/A");
                String locale = textAnnotation.getString("locale");
                data.put("locale", locale);
            }
            String description = textAnnotation.getString("description");
            JSONArray verticesArray = textAnnotation.getJSONObject("boundingPoly").getJSONArray("vertices");

            List<Map<String, Integer>> coordinates = new ArrayList<>();
            for (int j = 0; j < verticesArray.length(); j++) {
                JSONObject vertices = verticesArray.getJSONObject(j);
                int x = vertices.getInt("x");
                int y = vertices.getInt("y");

                Map<String, Integer> coordinate = new HashMap<>();
                coordinate.put("x", x);
                coordinate.put("y", y);
                coordinates.add(coordinate);
            }

            data.put("description", description);
            data.put("vertices", coordinates);
            jsonCollection.add(data);
        }

//        for (Map<String, Object> data : dataCollection) {
//            System.out.println("locale: " + data.get("locale"));
//            System.out.println("Description: " + data.get("Description"));
//            System.out.println("Vertices: " + data.get("Vertices"));
//            System.out.println();
//        }
    }

    // 버킷에 이미지 업로드
    public void uploadImagesToBucket() {
        try {
            //FileInputStream keyFile = new FileInputStream(KEY_FILE_PATH);

            Storage storage = StorageOptions
                    .newBuilder()
                    .setProjectId(PROJECT_ID)
                    .build()
                    .getService();

            for (File file : imageAndPdfFiles) {
                //String uuid = UUID.randomUUID().toString();
                String contentType = "image/jpeg";

                Path sourcePath = Paths.get(file.getParentFile().toString(), file.getName());
                Path targetPath = Paths.get(resultFolderPath, file.getName());

                BlobInfo blobInfo = BlobInfo.newBuilder(BUCKET_NAME, file.getName())
                        .setContentType(contentType)
                        .build();

                Blob blob = storage.create(blobInfo, new FileInputStream(file));
                Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Image uploaded successfully. URL: " + blob.getMediaLink());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // sourceDir = 이미지 폴더 경로 / targetPath = 결과 저장 폴더 (json, excel, image)
    // 버킷 에서드로 이동
//    public static void moveFile(String sourceDir, String targetDir, String fileName) {
//        Path sourcePath = Paths.get(sourceDir, fileName);
//        Path targetPath = Paths.get(targetDir, fileName);
//
//        try {
//            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
//            System.out.println("File moved successfully!");
//        } catch (IOException e) {
//            System.err.println("Failed to move the file: " + e.getMessage());
//        }
//    }

    public void processVision() throws IOException {
        List<String> paths = new ArrayList<>();

        String accessToken = getAccessToken(KEY_FILE_PATH);

        // Storage 인스턴스 생성
        Storage storage = StorageOptions.newBuilder()
                .setProjectId(PROJECT_ID)
                .setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(KEY_FILE_PATH)))
                .build()
                .getService();

        // 버킷에서 객체 가져오기
        Bucket bucket = storage.get(BUCKET_NAME);

        for (File file : imageAndPdfFiles) {
            String fileName = file.getName().substring(0, file.getName().lastIndexOf("."));
            String path = resultFolderPath + "\\" + fileName + ".json";
            Blob blob = bucket.get(file.getName());

            // 이미지 파일을 Base64로 인코딩
            byte[] imgBytes = blob.getContent();
            String imgBase64 = Base64.getEncoder().encodeToString(imgBytes);

            String jsonRequest = new JSONObject()
                    .put("requests", new JSONObject()
                            .put("image", new JSONObject().put("content", imgBase64))
                            .put("features", new JSONObject().put("type", "TEXT_DETECTION")))
                    .toString();

            OkHttpClient client = new OkHttpClient();
            RequestBody body = RequestBody.create(jsonRequest, MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url("https://vision.googleapis.com/v1/images:annotate")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("HTTP error code: " + response.code());
                    System.err.println("Response body: " + response.body().string());
                    throw new IOException("Unexpected code " + response);
                }

                String responseBody = response.body().string();
                try (FileWriter writer = new FileWriter(path)) {
                    writer.write(responseBody);
                    System.out.println("OCR 결과를 " + path + "에 저장했습니다.");
                    paths.add(path);
                }
            }
        }
    }

    public String getAccessToken(String keyFilePath) throws IOException {
        GoogleCredentials credentials = ServiceAccountCredentials.fromStream(new FileInputStream(keyFilePath))
                .createScoped("https://www.googleapis.com/auth/cloud-platform");

        // 액세스 토큰을 얻기 위해 credentials.refreshIfExpired() 호출
        credentials.refreshIfExpired();
        AccessToken token = credentials.getAccessToken();

        return token.getTokenValue();
    }

    public void ocrImageMarker(String imageFileName, String projectId, String bucketName, String keyFilePath) throws IOException {
        Storage storage = StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(keyFilePath)))
                .build()
                .getService();
        Blob blob = storage.get(bucketName, imageFileName);
        byte[] imgBytes = blob.getContent();
        InputStream imgStream = new ByteArrayInputStream(imgBytes);
        BufferedImage bufferedImage = ImageIO.read(imgStream);
        WritableImage image = SwingFXUtils.toFXImage(bufferedImage, null);

        // Load OCR results from JSON file
        String fileName = imageFileName.substring(0, imageFileName.lastIndexOf("."));
        String path = resultFolderPath + "\\" + fileName + ".json";
        JSONObject jsonObject = new JSONObject(new FileReader(path));
        JSONArray textAnnotations = jsonObject.getJSONArray("responses").getJSONObject(0).getJSONArray("textAnnotations");

        // Draw bounding boxes around detected text
        Canvas canvas = new Canvas(image.getWidth(), image.getHeight());
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.drawImage(image, 0, 0);
        gc.setStroke(Color.RED);
        gc.setLineWidth(2);
        for (int i = 1; i < textAnnotations.length(); i++) { // 첫 번째 요소는 전체 텍스트이므로 건너뜁니다.
            JSONObject annotation = textAnnotations.getJSONObject(i);
            JSONObject boundingPoly = annotation.getJSONObject("boundingPoly");
            JSONArray vertices = boundingPoly.getJSONArray("vertices");
            double[] xPoints = new double[vertices.length()];
            double[] yPoints = new double[vertices.length()];
            for (int j = 0; j < vertices.length(); j++) {
                JSONObject vertex = vertices.getJSONObject(j);
                xPoints[j] = vertex.getDouble("x");
                yPoints[j] = vertex.getDouble("y");
            }
            gc.strokePolygon(xPoints, yPoints, vertices.length());
        }

        // Save the marked image to local path
        String outputPath = resultFolderPath + "\\result-" + fileName;
        File outputFile = new File(outputPath);
        ImageIO.write(bufferedImage, "jpg", outputFile);
        System.out.println("마킹된 이미지를 " + outputPath + "에 저장했습니다.");
    }


    // 엑셀의 단어 리스트 가져오기
    public Map<String, List<List<String>>> getExcelData(String excelFilePath) {
        Map<String, List<List<String>>> excelData = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(excelFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                List<List<String>> sheetData = new ArrayList<>();

                int maxColumns = 0;
                for (Row row : sheet) {
                    if (row.getLastCellNum() > maxColumns) {
                        maxColumns = row.getLastCellNum();
                    }
                }

                for (int col = 0; col < maxColumns; col++) {
                    List<String> columnData = new ArrayList<>();
                    for (Row row : sheet) {
                        Cell cell = row.getCell(col);
                        if (cell != null && !cell.toString().isEmpty()) {
                            columnData.add(cell.toString());
                        }
                    }
                    if (!columnData.isEmpty()) {
                        sheetData.add(columnData);
                    }
                }

                excelData.put(sheet.getSheetName(), sheetData);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        // 엑셀 데이터 출력 (테스트용)
//        for (Map.Entry<String, List<List<String>>> entry : excelData.entrySet()) {
//            System.out.println("Sheet: " + entry.getKey());
//            for (List<String> column : entry.getValue()) {
//                System.out.println(column);
//            }
//        }
        return excelData;
    }

    // 폴더의 모든 파일(json)을 반복 (JSON Object로 저장 및 split, classifyDocuments 메소드로 분류 진행) (iterateFiles)
    public void createFinalResultFile() {
        Map<String, List<List<String>>> excelData = getExcelData(EXCEL_FILE_PATH);

        for (File curFile : jsonFiles) {
            // 각 파일 JSON Object로 저장
            String jsonFilePath = curFile.getPath();

            String fileName = curFile.getName().substring(0, curFile.getName().lastIndexOf("."));
            String saveFilePath = resultFolderPath + "\\" + fileName + ".xlsx";

            jsonToJsonObject(jsonFilePath);
            getWordPosition();

            // 엑셀 데이터와 비교해서 문서 분류
            String jsonLocale = jsonCollection.getFirst().get("locale").toString();
            String jsonDescription = jsonCollection.getFirst().get("description").toString();

            classifyDocuments(excelData, jsonLocale, jsonDescription);

            try {
                createExcel(saveFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //<editor-fold desc="">
    // 엑셀 데이터와 비교하여 국가 및 양식 분류
    public void classifyDocuments(Map<String, List<List<String>>> excelData, String jsonLocale, String jsonDescription) {
        List<List<String>> targetSheetData = excelData.get(jsonLocale);
        System.out.println(targetSheetData);

        if (targetSheetData == null) {
            // 일치하는 시트가 없을 경우
        }

        int maxMatches = 0;
        int index = -1;

        resultList = new ArrayList<>();
        resultWord = new ArrayList<>();

        // null 처리
        for (int col = 0; col < targetSheetData.size(); col++) {
            List<String> columnData = targetSheetData.get(col);

            int matches = 0;
            List<String> matchingValues = new ArrayList<>();

            matchingValues.add(columnData.get(0));

            for (String value : columnData) {
                if (jsonDescription.contains(value)) {
                    matches++;
                    matchingValues.add(value + "(" + countOccurrences(jsonDescription, value) + ")");
                }
            }

            matchingValues.add(matches + ""); // 각 열 매치 단어 수
            resultWord.add(matchingValues);

            // 각 열마다 일치하는 값들을 1줄로 콘솔에 출력
            System.out.println("열 " + col + ": " + matchingValues);

            if (matches > maxMatches) {
                maxMatches = matches;
                index = col;
            }

            System.out.println("resultWord : " + resultWord);
        }


        System.out.println(maxMatches + ", " + index);

        System.out.println("문서 분류 결과: " + targetSheetData.get(index).get(0));

        List<String> countryType = new ArrayList<>();
        countryType.add("국가");
        countryType.add(jsonLocale);
        resultList.add(countryType);

        List<String> documentType = new ArrayList<>();
        documentType.add("문서 양식");
        documentType.add(targetSheetData.get(index).get(0));
        resultList.add(documentType);
    }
    //</editor-fold>

    // 단어 카운트
    public static int countOccurrences(String input, String word) {
        int count = 0;
        int index = input.indexOf(word);
        while (index != -1) {
            count++;
            index = input.indexOf(word, index + 1);
        }
        return count;
    }

    // 엑셀에 결과 값 쓰기
    public void createExcel(String saveFilePath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");

        // Writing resultList
        for (int i = 0; i < resultList.size(); i++) {
            Row row = sheet.createRow(i);
            for (int j = 0; j < resultList.get(i).size(); j++) {
                Cell cell = row.createCell(j);
                cell.setCellValue(resultList.get(i).get(j));
            }

            if (i == resultList.size() - 1) {
                // Writing resultWord
                int colNum = 2; // 3열부터 시작 (C열에 해당)

                for (List<String> rowData : resultWord) {
                    Cell cell = row.createCell(colNum); // 2열에 해당

                    StringBuilder cellValue = new StringBuilder(rowData.getFirst());
                    cellValue.append(" (");

                    for (int j = 1; j < rowData.size() - 1; j++) {
                        cellValue.append(rowData.get(j));
                        if (j < rowData.size() - 2) {
                            cellValue.append(", ");
                        }
                    }

                    cellValue.append(")");
                    cell.setCellValue(cellValue.toString());
                    colNum++;

                    Cell cell2 = row.createCell(colNum);
                    cell2.setCellValue(rowData.getFirst() + " (" + rowData.getLast() + ")");
                    colNum++;
                }
            }
        }

        try (FileOutputStream fileOut = new FileOutputStream(saveFilePath)) {
            workbook.write(fileOut);
            System.out.println("엑셀 파일이 성공적으로 생성되었습니다: " + saveFilePath);
        }
        workbook.close();

    }

    // 중간 값을 계산
    public int getYMidValue(List<Map<String, Integer>> vertices) {
        int yMin = Integer.MAX_VALUE;
        int yMax = Integer.MIN_VALUE;
        for (Map<String, Integer> vertex : vertices) {
            int y = vertex.get("y");
            if (y < yMin) yMin = y;
            if (y > yMax) yMax = y;
        }
        return (yMin + yMax) / 2;
    }

    // Json Object의 좌표값으로 값 정렬
    public void sortByCoordinates(List<Map<String, Object>> jsonObject) {
        // 두 번째 리스트부터 처리
        List<Map<String, Object>> subList = jsonObject.subList(1, jsonObject.size());

        // 중간값과 description 값을 저장할 리스트
        List<Map<String, Object>> midValueList = new ArrayList<>();

        // 모든 리스트를 돌면서 y 중간값을 구하고 리스트에 저장
        for (Map<String, Object> map : subList) {
            List<Map<String, Integer>> vertices = (List<Map<String, Integer>>) map.get("vertices");
            int yMid = getYMidValue(vertices);
            int x = vertices.get(0).get("x");

            Map<String, Object> midValueMap = new HashMap<>();
            midValueMap.put("yMid", yMid);
            midValueMap.put("x", x);
            midValueMap.put("description", map.get("description"));
            midValueList.add(midValueMap);
        }

        // y 중간값을 기준으로 오름차순 정렬
        midValueList.sort(Comparator.comparingInt(map -> (int) map.get("yMid")));

        // 결과 출력
//        for (Map<String, Object> map : midValueList) {
//            System.out.println("yMid: " + map.get("yMid") + ", x: " + map.get("x") + ", description: " + map.get("description"));
//        }
        //System.out.println(midValueList);

        // 그룹화 및 정렬
        List<List<Map<String, Object>>> groupedLists = new ArrayList<>();
        while (!midValueList.isEmpty()) {
            Map<String, Object> firstElement = midValueList.get(0);
            int yMid = (int) firstElement.get("yMid");

            List<Map<String, Object>> group = new ArrayList<>();
            Iterator<Map<String, Object>> iterator = midValueList.iterator();
            while (iterator.hasNext()) {
                Map<String, Object> element = iterator.next();
                int elementYMid = (int) element.get("yMid");
                if (Math.abs(elementYMid - yMid) <= 10) {
                    group.add(element);
                    iterator.remove();
                }
            }

            // 그룹 내에서 x 좌표 기준으로 정렬
            group.sort(Comparator.comparingInt(map -> (int) map.get("x")));

            // 정렬된 그룹을 결과 리스트에 추가
            groupedLists.add(group);
        }

        // 그룹 별로 description 값을 모아서 리스트로 저장
        List<List<String>> groupedDescriptions = new ArrayList<>();
        for (List<Map<String, Object>> group : groupedLists) {
            List<String> descriptions = new ArrayList<>();
            for (Map<String, Object> map : group) {
                descriptions.add((String) map.get("description"));
            }
            groupedDescriptions.add(descriptions);
        }

        // 결과 출력
//        for (List<String> descriptions : groupedDescriptions) {
//            System.out.println(descriptions);
//        }

        for (List<String> descriptions : groupedDescriptions) {
            for (String s : descriptions) {
                System.out.print(s);
            }
            System.out.println();
        }
    }
}

