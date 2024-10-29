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


    //구글 버킷에 이미지 올리기 및 ocr 진행
    public void uploadAndOCR(File file) throws IOException {
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

        String accessToken = getAccessToken();
        OkHttpClient client = new OkHttpClient();


            String objectName = file.getName();
            BlobId blobId = BlobId.of(configLoader.bucketNames.get(0), objectName);

            // 버킷에 해당 파일이 있는 지 확인
            if (storage.get(blobId) != null) {
                log.warn("Image file already exists in bucket: {}", objectName);
                deleteFileInBucket(storage,blobId);
            }
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

            // 버킷 업로드
            try { // 이미지 버킷에 업로드
                storage.create(blobInfo, Files.readAllBytes(file.toPath()));
                log.info("{} File upload successful", file.getName());
            } catch (IOException e) {
                log.error("Error uploading file: {}", file.getName(), e);
                throw e;
            }

            // OCR 수행
            byte[] imgBytes = storage.readAllBytes(blobId);
            String imgBase64 = Base64.getEncoder().encodeToString(imgBytes);

            String jsonRequest = new JSONObject().put("requests", new JSONArray().put(new JSONObject().put("image", new JSONObject().put("content", imgBase64)).put("features", new JSONArray().put(new JSONObject().put("type", "TEXT_DETECTION"))))).toString();

            RequestBody body = RequestBody.create(jsonRequest, MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder().url(configLoader.ocrUrl).addHeader("Authorization", "Bearer " + accessToken).post(body).build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("OCR fail: {}", response.message());
                    return;
                }

                String responseBody = response.body().string();
                String outputFileName = file.getName().substring(0, file.getName().lastIndexOf("."));
                //json ->  dat로 변경
                String outputPath = configLoader.resultFilePath + "\\" + outputFileName + "_result.dat";
                try (FileWriter writer = new FileWriter(outputPath)) {

                    if(configLoader.encodingCheck==true){
                        //인코딩 디코딩 하는 메서드 ---------------------
                        byte[] encData = JsonService.aesEncode(responseBody);
                        Files.write(Paths.get(outputPath),encData);

                        byte[] fileContent = Files.readAllBytes(Paths.get(outputPath));
                        String decodedText = JsonService.aesDecode(fileContent);
                        log.debug("Decoding text: {}", decodedText);
//                    ------------------------------------------------------------------
                    }
                    if (!configLoader.encodingCheck==true){
                        writer.write(responseBody);
                    }
                    jsonFilePaths.add(outputPath); // JSON 파일 경로 리스트에 추가
                    log.info("JSON file download successful");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                log.info("OCR request successful");
                deleteFileInBucket(storage, blobId);
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
