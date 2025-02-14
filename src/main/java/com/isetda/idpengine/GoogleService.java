package com.isetda.idpengine;

//import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;



public class GoogleService {
    public boolean encode = false;
    // 로그
    private static final Logger log = LogManager.getLogger(GoogleService.class);
    //환경변수 인스턴스 생성
    public ConfigLoader configLoader;

    //json파일에 대한 이름을 가져올려고(어떻게 될 지 모르니까 일단 변수로 저장해 놓자 쓸 곳이 있겠찌??)
    private List<String> jsonFilePaths = new ArrayList<>();

    public boolean checkBadImg;

    public boolean EnablingGoogle = true;


    //구글 버킷에 이미지 올리기 및 ocr 진행
    public void uploadAndOCR(File file) throws IOException {
        EnablingGoogle = true;
        log.info("Start Upload and OCR");
        Storage storage = getStorageService();
        File localDir = new File(configLoader.resultFilePath);

        if (!localDir.exists()) {
            localDir.mkdirs();
            log.info("Create a resulting directory: {}", configLoader.resultFilePath);
        }

        // PDF 파일 제외
        if (file.getName().toLowerCase().endsWith(".pdf")) {
            log.info("Skipping PDF file: {}", file.getName());
            return;
        }

        String accessToken;
        try {
            accessToken = getAccessToken();
        } catch (IOException e) {
            log.error("Error obtaining access token", e);
            return;
        }

        OkHttpClient client = new OkHttpClient();

        String objectName = file.getName();
        BlobId blobId = BlobId.of(configLoader.bucketNames.get(0), objectName);

        // 버킷에 해당 파일이 있는지 확인
//        if (storage.get(blobId) != null) {
//            log.warn("Image file already exists in bucket: {}", objectName);
//            deleteFileInBucket(storage, blobId);
//        }

        try {
            if (storage.get(blobId) != null) {
                log.warn("Image file already exists in bucket: {}", objectName);
                deleteFileInBucket(storage, blobId);
            }
        } catch (StorageException e) {
            // 403 Forbidden 오류가 발생하면 결제 계정이 비활성화된 것임
            if (e.getCause() instanceof GoogleJsonResponseException) {
        GoogleJsonResponseException googleJsonException = (GoogleJsonResponseException) e.getCause();
        String errorMessage = googleJsonException.getMessage();
        log.error("Google error message: {}", errorMessage);
        EnablingGoogle = false;  // 이후 작업을 중단하거나 비활성화 설정
        checkBadImg = true;
        return;  // 이후 처리 중단
    }
    log.error("Error checking if file exists in bucket: {}", e.getMessage());
    return;
}
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        // 버킷 업로드
        try {
            storage.create(blobInfo, Files.readAllBytes(file.toPath()));
            log.info("{} File upload successful", file.getName());
        } catch (IOException e) {
            log.error("Error uploading file: {}", file.getName(), e);
            return;
        }

        // OCR 수행
        try {
            byte[] imgBytes = storage.readAllBytes(blobId);
            String imgBase64 = Base64.getEncoder().encodeToString(imgBytes);

            String jsonRequest = new JSONObject()
                    .put("requests", new JSONArray()
                            .put(new JSONObject()
                                    .put("image", new JSONObject().put("content", imgBase64))
                                    .put("features", new JSONArray().put(new JSONObject().put("type", "TEXT_DETECTION")))
                            )).toString();

            RequestBody body = RequestBody.create(jsonRequest, MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(configLoader.ocrUrl)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("OCR fail: {}", response.message());
                    return;
                }

                String responseBody = response.body().string();
                if (responseBody.contains("Bad image data")) {
                    checkBadImg = false;
                    deleteFileInBucket(storage, blobId);
                } else {
                    String outputFileName = file.getName().substring(0, file.getName().lastIndexOf("."));
                    String outputPath = configLoader.resultFilePath + "\\" + outputFileName + "_result.dat";

                    try (FileWriter writer = new FileWriter(outputPath)) {
                        if (configLoader.encodingCheck) {
                            byte[] encData = JsonService.aesEncode(responseBody);
                            Files.write(Paths.get(outputPath), encData);

                            byte[] fileContent = Files.readAllBytes(Paths.get(outputPath));
                            String decodedText = JsonService.aesDecode(fileContent);
                            log.debug("Decoding text: {}", decodedText);
                        } else {
                            writer.write(responseBody);
                        }
                        jsonFilePaths.add(outputPath); // JSON 파일 경로 리스트에 추가
                        log.info("JSON file download successful");
                    } catch (Exception e) {
                        log.error("Error saving OCR response", e);
                    }
                }
                log.info("OCR request successful");
                deleteFileInBucket(storage, blobId);
                checkBadImg = true;
            }
        } catch (IOException e) {
            log.error("Network error during OCR request", e);
        }
    }
//학습 제외 국가용 ocr 업로드 메서드
    public void FullTextOCR(File file) throws IOException {
        log.info("Start Upload and OCR");
        Storage storage = getStorageService();
        File localDir = new File(configLoader.resultFilePath);
        EnablingGoogle = true;


        if (!localDir.exists()) {
            localDir.mkdirs();
            log.info("Create a resulting directory: {}", configLoader.resultFilePath);
        }

        // PDF 파일 제외
        if (file.getName().toLowerCase().endsWith(".pdf")) {
            log.info("Skipping PDF file: {}", file.getName());
            return;
        }

        String accessToken;

        try {
            accessToken = getAccessToken();
        } catch (IOException e) {
            log.error("Error obtaining access token", e);
            return;
        }

        OkHttpClient client = new OkHttpClient();

        String objectName = file.getName();
        BlobId blobId = BlobId.of(configLoader.bucketNames.get(0), objectName);

        try {
            if (storage.get(blobId) != null) {
                log.warn("Image file already exists in bucket: {}", objectName);
                deleteFileInBucket(storage, blobId);
            }
        } catch (StorageException e) {
            // 403 Forbidden 오류가 발생하면 결제 계정이 비활성화된 것임
            if (e.getCause() instanceof GoogleJsonResponseException) {
                GoogleJsonResponseException googleJsonException = (GoogleJsonResponseException) e.getCause();
                String errorMessage = googleJsonException.getMessage();
                log.error("Google error message: {}", errorMessage);
                EnablingGoogle = false;  // 이후 작업을 중단하거나 비활성화 설정
                checkBadImg = true;
                return;  // 이후 처리 중단
            }
            log.error("Error checking if file exists in bucket: {}", e.getMessage());
            return;
        }
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        // 버킷 업로드
        try {
            storage.create(blobInfo, Files.readAllBytes(file.toPath()));
            log.info("{} File upload successful", file.getName());
        } catch (IOException e) {
            log.error("Error uploading file: {}", file.getName(), e);
            return;
        }

        // OCR 수행
        try {
            byte[] imgBytes = storage.readAllBytes(blobId);
            String imgBase64 = Base64.getEncoder().encodeToString(imgBytes);

            String jsonRequest = new JSONObject()
                    .put("requests", new JSONArray()
                            .put(new JSONObject()
                                    .put("image", new JSONObject().put("content", imgBase64))
                                    .put("features", new JSONArray().put(new JSONObject().put("type", "TEXT_DETECTION")))
                            )).toString();

            RequestBody body = RequestBody.create(jsonRequest, MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(configLoader.ocrUrl)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("OCR fail: {}", response.message());
                    return;
                }

                String responseBody = response.body().string();
                if (responseBody.contains("Bad image data")){
                    checkBadImg = false;
                    deleteFileInBucket(storage, blobId);
                }else {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String extractedText = new String();

                    JSONArray responses = jsonResponse.getJSONArray("responses");
                    for (int i = 0; i < responses.length(); i++) {
                        JSONObject responseObj = responses.getJSONObject(i);
                        if (responseObj.has("textAnnotations")) {
                            JSONArray textAnnotations = responseObj.getJSONArray("textAnnotations");
                            if (textAnnotations.length() > 0) {
                                // textAnnotations의 첫 번째 항목이 전체 텍스트를 포함
                                String description = textAnnotations.getJSONObject(0).getString("description").replaceAll("\\n", "++++");
                                extractedText= description;
                                log.info("OCR text extraction successful");
                            }
                        }else {
                            log.warn("OCR error");
                        }
                    }

                    if (!extractedText.isEmpty()) {
                        String outputFileName = file.getName().substring(0, file.getName().lastIndexOf("."));
                        String outputPath = configLoader.resultFilePath + "\\" + outputFileName + "_result.dat";

                        try (FileWriter writer = new FileWriter(outputPath)) {
                            writer.write(extractedText);
                            jsonFilePaths.add(outputPath);
                            log.info("OCR result file saved successfully: {}", outputPath);
                        } catch (Exception e) {
                            log.error("Error saving OCR response", e);
                        }
                    } else {
                        log.warn("Skipping file save due to no extracted text for: {}", file.getName());
                    }

                    log.info("OCR request successful");
                    deleteFileInBucket(storage, blobId);
                    checkBadImg = true;
                }
            }
        } catch (IOException e) {
            log.error("Network error during OCR request", e);
        }
    }

    //구글 인증 토근 설정
    public String getAccessToken() throws IOException {
        GoogleCredentials credentials = ServiceAccountCredentials.fromStream(new FileInputStream(configLoader.keyFilePath)).createScoped(configLoader.cloudPlatform);
        //https://www.googleapis.com/auth/cloud-platform 이거 향후에 바뀔 수 있는 지 확인하기(환경파일로 빼기)
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }
    //구글 저장소 가져오기
    public Storage getStorageService() throws IOException {
        GoogleCredentials credentials = ServiceAccountCredentials.fromStream(new FileInputStream(configLoader.keyFilePath)).createScoped(configLoader.cloudPlatform);
        return StorageOptions.newBuilder().setProjectId(configLoader.projectId).setCredentials(credentials).build().getService();
    }

    //구글 버킷 파일 삭제
    public void deleteFileInBucket(Storage storage, BlobId blobId) {
        boolean deleted = storage.delete(blobId);
        if (deleted) {
            log.info("{}From the bucket {} File Deletion Completed", blobId.getBucket(), blobId.getName());
        } else {
            log.error("File not deleted from {}bucket: {} ", blobId.getBucket(), blobId.getName());
        }
    }

}
