package com.isetda.idpengine;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Service { // 로그
    private static final Logger log = LogManager.getLogger(Service.class);
    //X,Y축 범위
    private static final int Y_THRESHOLD = 20;
    private static final int X_THRESHOLD = 22;

    public String fullText = "";
    // ConfigLoader 인스턴스 생성
    private ConfigLoader configLoader = ConfigLoader.getInstance();

    // 환경 변수
    private final String PROJECT_ID;
    private final List<String> BUCKET_NAMES;
    private final String KEY_FILE_PATH;
    private final boolean DELETED_CHECK;
    private final String OCR_URL;
    private final String CLOUD_PLATFORM;

    // 환경 변수 불러오기
    public Service() {
        PROJECT_ID = configLoader.getProjectId();
        BUCKET_NAMES = configLoader.getBucketNames(); // 수정된 부분
        KEY_FILE_PATH = configLoader.getKeyFilePath();
        DELETED_CHECK = configLoader.isDeletedCheck();
        OCR_URL = configLoader.getOcrUrl();
        CLOUD_PLATFORM = configLoader.getCloudPlatform();
    }

    //json파일에 해당하는 나라
    public String jsonLocal = "";
    //받는 이미지 경로
    public String excelFilePath = "C:\\Users\\isetda\\OneDrive\\바탕 화면\\식품안전 프로젝트 관련 자료\\국가, 문서 양식별 추출 단어 리스트.xlsx";
    //분리된 이미지 저장 경로
    private String imageFolderPath = "C:\\Users\\isetda\\testImg";

    private String resultFilePath = "C:\\Users\\isetda\\testImg\\";

    //결과 파일 경로
//    private String resultFilePath = "C:\\Users\\isetda\\OneDrive\\바탕 화면\\testresult\\";
    //json파일에 대한 이름을 가져올려고
    private List<String> jsonFilePaths = new ArrayList<>();
    //엑셀에 대한 데이터
    private Map<String, List<String>> columnData;


    // 폴더에서 이미지(jpg, png), PDF 파일 가져오기
//    public File[] getFilteredFiles(String folderPath) {
//        File folder = new File(folderPath);
//
//        File[] files = folder.listFiles((dir, name) -> {
//            String lowercaseName = name.toLowerCase();
//            return lowercaseName.endsWith(".jpg") || lowercaseName.endsWith(".png") || lowercaseName.endsWith(".pdf");
//        });
//        if (files == null) {
//            log.error("폴더에서 파일을 가져오는 중 오류가 발생했습니다: {}", folderPath);
//        } else {
//            log.info("폴더에서 {}개의 파일을 가져왔습니다: {}", files.length, folderPath);
//        }
//        return files;
//    }
    public File[] getFilteredFiles(String folderPath) {
        File folder = new File(folderPath);
        List<File> filteredFiles = new ArrayList<>();

        // 파일 및 폴더를 재귀적으로 탐색
        findFilesRecursively(folder, filteredFiles);

        if (filteredFiles.isEmpty()) {
            log.error("폴더에서 파일을 가져오는 중 오류가 발생했습니다: {}", folderPath);
        } else {
            log.info("폴더에서 {}개의 파일을 가져왔습니다: {}", filteredFiles.size(), folderPath);
        }

        // 리스트를 배열로 변환하여 반환
        return filteredFiles.toArray(new File[0]);
    }

    private void findFilesRecursively(File folder, List<File> filteredFiles) {
        File[] filesAndDirs = folder.listFiles();
        if (filesAndDirs != null) {
            for (File file : filesAndDirs) {
                if (file.isDirectory()) {
                    // 서브 폴더를 재귀적으로 탐색
                    findFilesRecursively(file, filteredFiles);
                } else {
                    // 파일이 이미지 또는 PDF인 경우 리스트에 추가
                    String lowercaseName = file.getName().toLowerCase();
                    if (lowercaseName.endsWith(".jpg") || lowercaseName.endsWith(".png")) {
                        filteredFiles.add(file);
                    } else if (lowercaseName.endsWith(".pdf")) {

                    }
                }
            }
        }
    }

    //파일 삭제 메서드
    public void deleteFilesInFolder(String folderPath) {
        File folder = new File(folderPath);

        // 폴더가 존재하지 않거나 디렉토리가 아닌 경우
        if (!folder.exists() || !folder.isDirectory()) {
            log.error("폴더가 존재하지 않거나 디렉토리가 아닙니다: {}", folderPath);
            return;
        }

        // 폴더 안의 파일들을 삭제
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        log.info("파일 삭제 성공: {}", file.getName());
                    } else {
                        log.warn("파일 삭제 실패: {}", file.getName());
                    }
                }
            }
        } else {
            log.error("폴더에서 파일을 가져오는 중 오류가 발생했습니다: {}", folderPath);
        }
    }

    //파일 복사
    public void copyFiles(File[] files) throws IOException {
        for (File file : files) {
            Path sourcePath = file.toPath();
            Path destinationPath = Paths.get(imageFolderPath, file.getName());
            try {
                Files.copy(sourcePath, destinationPath);
                log.info("파일 복사 성공: {} -> {}", sourcePath, destinationPath);
            } catch (IOException e) {
                log.error("파일 복사 중 오류 발생: {} -> {}", sourcePath, destinationPath, e);
                throw e; // 오류 발생 시 던지기
            }
        }
    }

    //문장 그룹화 아직 미완성
    // TextElement 그룹화
    public List<String> groupTextByCoordinate(JSONArray textAnnotationsArray) {
        List<TextElement> textElements = new ArrayList<>();
        for (int i = 1; i < textAnnotationsArray.length(); i++) {
            JSONObject textAnnotation = textAnnotationsArray.getJSONObject(i);
            textElements.add(new TextElement(textAnnotation, i));
        }
        jsonLocal = textAnnotationsArray.getJSONObject(0).getString("locale");
        fullText = textAnnotationsArray.getJSONObject(0).getString("description").replaceAll("\\n", " ");
        ;

        log.debug("JSON 나라 :{}", jsonLocal);
        log.debug("JSON 나라 :{}", fullText);

        List<TextElement> currentGroup = new ArrayList<>();
        List<String> groupedTextList = new ArrayList<>();
        TextElement lastElement = textElements.get(0);
        currentGroup.add(lastElement);

        for (int i = 1; i < textElements.size(); i++) {
            TextElement currentElement = textElements.get(i);
            if (isWithinThreshold(lastElement, currentElement)) {
                currentGroup.add(currentElement);
            } else {
                processGroup(currentGroup, groupedTextList);
                currentGroup.clear();
                currentGroup.add(currentElement);
            }
            lastElement = currentElement;
        }
        processGroup(currentGroup, groupedTextList);
        return groupedTextList;
    }

    private boolean isWithinThreshold(TextElement last, TextElement current) {
        double lastYCenter = (last.getY1() + last.getY2()) / 2.0;
        double currentYCenter = (current.getY1() + current.getY2()) / 2.0;
        int lastXEnd = Math.max(last.getX1(), last.getX2());
        int currentXStart = Math.min(current.getX1(), current.getX2());
        return Math.abs(lastYCenter - currentYCenter) <= Y_THRESHOLD || Math.abs(lastXEnd - currentXStart) <= X_THRESHOLD;
    }

    private void processGroup(List<TextElement> group, List<String> groupedTextList) {
        if (group.isEmpty()) return;
        String groupedLine = group.stream()
                .sorted(Comparator.comparingInt(TextElement::getIndex))
                .map(TextElement::getText)
                .collect(Collectors.joining(" "));
        groupedTextList.add(groupedLine);
        log.debug("그룹화 문장: {}", groupedLine);
    }

    public static class TextElement {
        private final String text;
        private final int y1, y2, x1, x2, index;

        public TextElement(JSONObject annotation, int index) {
            this.text = annotation.getString("description");
            JSONArray vertices = annotation.getJSONObject("boundingPoly").getJSONArray("vertices");
            this.y1 = vertices.getJSONObject(0).getInt("y");
            this.y2 = vertices.getJSONObject(2).getInt("y");
            this.x1 = vertices.getJSONObject(0).getInt("x");
            this.x2 = vertices.getJSONObject(2).getInt("x");
            this.index = index;
        }

        public String getText() {
            return text;
        }

        public int getY1() {
            return y1;
        }

        public int getY2() {
            return y2;
        }

        public int getX1() {
            return x1;
        }

        public int getX2() {
            return x2;
        }

        public int getIndex() {
            return index;
        }
    }

    //구글 버킷에 이미지 올리기 및 ocr 진행
    public void uploadAndOCR() throws IOException {
        File[] files = getFilteredFiles(imageFolderPath);
        Storage storage = getStorageService();
        File localDir = new File(resultFilePath);

        if (!localDir.exists()) {
            localDir.mkdirs();
        }
        String accessToken = getAccessToken();
        OkHttpClient client = new OkHttpClient();

        for (File file : files) {
            String objectName = file.getName();
            BlobId blobId = BlobId.of(BUCKET_NAMES.get(0), objectName);

            // 버킷에 해당 파일이 있는 지 확인
            if (storage.get(blobId) != null) {
                log.warn("파일이 이미 버킷에 존재합니다: {}", objectName);
                continue;  // 스킵
            }
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

            // 버킷 업로드
            try { // 이미지 버킷에 업로드
                storage.create(blobInfo, Files.readAllBytes(file.toPath()));
                log.info("파일 업로드 성공: {}", file.getName());
            } catch (IOException e) {
                log.error("파일 업로드 중 오류 발생: {}", file.getName(), e);
                throw e;
            }

            // OCR 수행
            byte[] imgBytes = storage.readAllBytes(blobId);
            String imgBase64 = Base64.getEncoder().encodeToString(imgBytes);

            String jsonRequest = new JSONObject().put("requests", new JSONArray().put(new JSONObject().put("image", new JSONObject().put("content", imgBase64)).put("features", new JSONArray().put(new JSONObject().put("type", "TEXT_DETECTION"))))).toString();

            RequestBody body = RequestBody.create(jsonRequest, MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder().url(OCR_URL).addHeader("Authorization", "Bearer " + accessToken).post(body).build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("OCR 요청 실패: {}", response.message());
                    continue;
                }

                String responseBody = response.body().string();
                String outputFileName = file.getName().substring(0, file.getName().lastIndexOf("."));
                String outputPath = resultFilePath + outputFileName + "_OCR_result.json";
                try (FileWriter writer = new FileWriter(outputPath)) {
                    writer.write(responseBody);
                    log.info("OCR 결과 경로 :{}", outputPath);
                    jsonFilePaths.add(outputPath); // JSON 파일 경로 리스트에 추가
                }
                deleteFileInBucket(storage, blobId);
            }
        }
    }

    //구글 인증 토큰 설정
    public String getAccessToken() throws IOException {
        GoogleCredentials credentials = ServiceAccountCredentials.fromStream(new FileInputStream(KEY_FILE_PATH)).createScoped(CLOUD_PLATFORM);
        //https://www.googleapis.com/auth/cloud-platform 이거 향후에 바뀔 수 있는 지 확인하기(환경파일로 빼기)
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }

    public Storage getStorageService() throws IOException {
        System.out.println("111 :" + BUCKET_NAMES);


        GoogleCredentials credentials = ServiceAccountCredentials.fromStream(new FileInputStream(KEY_FILE_PATH)).createScoped(CLOUD_PLATFORM);
        return StorageOptions.newBuilder().setProjectId(PROJECT_ID).setCredentials(credentials).build().getService();
    }

    //구글 버킷 파일 삭제
    public void deleteFileInBucket(Storage storage, BlobId blobId) {
        boolean deleted = storage.delete(blobId);
        if (deleted) {
            log.info("{}버킷에서 삭제된 파일 :{} =", blobId.getBucket(), blobId.getName());
        } else {
            log.error("{}버킷에서 삭제되지 않은 파일 :{} =", blobId.getBucket(), blobId.getName());
        }
    }

    // Excel 데이터 읽기(jsonlocal에 해당하는 시트에 대한)
    public Map<String, List<String>> readExcelByLocale() throws IOException {
        log.debug("나라 : {}", jsonLocal);
        Map<String, List<String>> columnData = new LinkedHashMap<>();

        try (FileInputStream file = new FileInputStream(new File(excelFilePath));
             Workbook workbook = new XSSFWorkbook(file)) {
            Sheet sheet = workbook.getSheet(jsonLocal);
            if (sheet == null) {
                log.debug("나라가 없습니다 {}", jsonLocal);
            }
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                log.debug("해당하는 나라의 키워드가 없습니다");
            }

            // <editor-fold desc="헤더로 열 데이터 맵에 추가">
            for (Cell cell : headerRow) {
                columnData.put(cell.getStringCellValue().trim(), new ArrayList<>());
            }
            // </editor-fold>

            // <editor-fold desc="시트의 각 행 처리">
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row dataRow = sheet.getRow(i);

                if (dataRow != null) {
                    // <editor-fold desc="행의 각 셀 처리">
                    for (int j = 0; j < headerRow.getPhysicalNumberOfCells(); j++) {
                        Cell cell = dataRow.getCell(j);
                        String value = (cell != null) ? cell.getStringCellValue().trim() : "";
                        if (!value.isEmpty()) {
                            String header = headerRow.getCell(j).toString().trim();
                            columnData.get(header).add(value);
                        }
                    }
                    // </editor-fold>
                }
            }
            // </editor-fold>
        }
        return columnData;
    }

    //전체 메서드 사용해서 json 처리, 가공 ,엑셀 데이터 읽어서 비교, 엑셀 결과 파일 생성
    public void createExcel() {
        List<JSONObject> jsonObjects = new ArrayList<>();

        for (String jsonFilePath : jsonFilePaths) {
            try {
                String jsonContent = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
                jsonObjects.add(new JSONObject(jsonContent));
            } catch (IOException e) {
                log.error("JSON 파일 로드 실패: {}", jsonFilePath, e);
            }
        }

        log.debug("{}개의 JSON파일 로드를 성공하였습니다.", jsonObjects.size());

        for (int i = 0; i < jsonObjects.size(); i++) {
            JSONObject jsonObject = jsonObjects.get(i);
            String fileName = jsonFilePaths.get(i).substring(jsonFilePaths.get(i).lastIndexOf("\\") + 1).replace("_OCR_result.json", ".xlsx");
            String excelFilePath2 = resultFilePath + fileName;

            log.debug("파일 이름 : {}", fileName);
            log.debug("파일 경로 : {}", excelFilePath2);

            try {
                List<String> groupedTextList = groupTextByCoordinate(jsonObject.getJSONArray("responses").getJSONObject(0).getJSONArray("textAnnotations"));
                List<String> lineNoSpace = groupedTextList.stream().map(text -> text.replaceAll("\\s+", "")).collect(Collectors.toList());
                columnData = readExcelByLocale();
                analyzeFile(excelFilePath2, jsonObject, groupedTextList, lineNoSpace);
            } catch (IOException e) {
                log.error("파일 처리 중 오류 발생: {}", excelFilePath2, e);
            } catch (Exception e) {
                log.error("예상치 못한 오류 발생: {}", e.getMessage(), e);
            }
        }
    }

    // 키워드 찾기
    //파라메타를 멤버 함수로 해보자.
    private void analyzeFile(String filePath, JSONObject jsonObject, List<String> groupedTextList, List<String> lineNoSpace) {
        Map<String, Map<String, Integer>> foundData = new LinkedHashMap<>();
        String mostHeader = null;
        int maxMatchCount = 0;

        try {
            for (Map.Entry<String, List<String>> entry : columnData.entrySet()) {
                String header = entry.getKey();
                log.info("진행중인 헤더 : {}", header);
                List<String> values = entry.getValue();
                Map<String, Integer> foundValues = new HashMap<>();
                int matchCount = 0;

                for (String value : values) {
                    String valueNoSpace = value.replaceAll("\\s+", "");
                    long valueCount = lineNoSpace.stream().filter(line -> line.contains(valueNoSpace)).count();

                    if (valueCount > 0) {
                        foundValues.put(value, (int) valueCount);
                        matchCount += valueCount;
                    }
                }
                if (!foundValues.isEmpty()) {
                    foundData.put(header, foundValues);
                    if (matchCount > maxMatchCount) {
                        mostHeader = header;
                        maxMatchCount = matchCount;
                    }
                }
            }
            if (mostHeader != null) {
                log.debug("문서 종류 : {}", mostHeader);
                log.debug("키워드 개수 : {}", maxMatchCount);
                createLayoutExcelFile(filePath, mostHeader, foundData);
            }
        } catch (Exception e) {
            log.error("파일 분석 중 오류 발생: {}", filePath, e);
        }
    }

    //예외 처리 자세하게 알 수 있도록 메서드 밖에서 말고 안에서 하자.
    //변수 좀 더 직관적으로
    private void createLayoutExcelFile(String filePath, String mostHeader, Map<String, Map<String, Integer>> foundData) {
        Path path = Paths.get(filePath);
        // 파일 존재 여부 확인
        if (Files.exists(path)) {
            log.debug("파일이 이미 존재합니다: {}", filePath);
            return;  // 기존 파일이 존재하면 넘어가기
        }
        log.info("새 엑셀 파일을 생성합니다: {}", filePath);

        // XSSFWorkbook 생성
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fileOut = new FileOutputStream(filePath)) {

            Sheet sheet = workbook.createSheet("Sheet1");
            Row headerRow = sheet.createRow(0);
            String[] headers = {"파일명", "국가", "문서종류", "키워드", "키워드 개수"};

            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue(path.getFileName().toString());
            dataRow.createCell(1).setCellValue(jsonLocal);
            dataRow.createCell(2).setCellValue(mostHeader);

            if (DELETED_CHECK) {
                String keywordData = foundData.entrySet().stream().map(entry -> entry.getKey() + ": (" + entry.getValue().entrySet().stream().map(val -> val.getKey() + "(" + val.getValue() + ")").collect(Collectors.joining(", ")) + "); ").collect(Collectors.joining());

                dataRow.createCell(3).setCellValue(keywordData);

                String headerCountData = columnData.entrySet().stream().map(entry -> entry.getKey() + "(" + (foundData.get(entry.getKey()) != null ? foundData.get(entry.getKey()).size() : 0) + ")").collect(Collectors.joining(" "));

                dataRow.createCell(4).setCellValue(headerCountData);
            } else {
                dataRow.createCell(3).setCellValue("키워드 정보 포함 안 함");
                dataRow.createCell(4).setCellValue("키워드 개수 포함 안 함");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(fileOut);
            log.info("엑셀 파일 작성 및 저장 성공: {}", filePath);

        } catch (FileNotFoundException e) {
            log.error("파일을 찾을 수 없습니다: {}", filePath, e);
        } catch (IOException e) {
            log.error("엑셀 파일 저장 중 오류 발생: {}", filePath, e);
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생: {}", e.getMessage(), e);
        }
    }

//    //문서 전처리용 pdf 파일 분리.
//    public void PDFImageExtractor(String pdfPath) throws IOException {
//        String outputDir = "C:\\Users\\isetda\\OneDrive\\바탕 화면\\";
//
//        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
//            PDFRenderer pdfRenderer = new PDFRenderer(document);
//
//            for(int page = 0; page<document.getNumberOfPages(); ++page){
//                BufferedImage bim = pdfRenderer.renderImageWithDPI(page,300, ImageType.RGB);
//                String fileName = resultFilePath + "page_" + page + ".jpg";
//                ImageIO.write(bim, "jpg", new File(fileName));
//
//                System.out.println("Saved: " + fileName);
//            }
//        }catch (IOException e){
//            e.printStackTrace();
//        }
//
//    }
}