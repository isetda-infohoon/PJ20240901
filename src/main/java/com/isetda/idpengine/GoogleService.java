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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class GoogleService {
    // 로그
    private static final Logger log = LogManager.getLogger(GoogleService.class);
    //환경변수 인스턴스 생성
    public ConfigLoader configLoader;

    private IMGFileIOService service = new IMGFileIOService();

    //json파일에 대한 이름을 가져올려고(어떻게 될 지 모르니까 일단 변수로 저장해 놓자 쓸 곳이 있겠찌??)
    private List<String> jsonFilePaths = new ArrayList<>();


    //구글 버킷에 이미지 올리기 및 ocr 진행
    public void uploadAndOCR(File[] filse) throws IOException {
        log.info("구글 버킷 업로드 및 OCR 처리 시작");
        File[] files = filse;
        Storage storage = getStorageService();
        File localDir = new File(configLoader.resultFilePath);

        if (!localDir.exists()) {
            localDir.mkdirs();
            log.info("결과 디렉토리 생성: {}", configLoader.resultFilePath);
        }
        String accessToken = getAccessToken();
        OkHttpClient client = new OkHttpClient();

        // 파일 처리 카운터 변수
        int fileCounter = 1;

        for (File file : files) {
            String objectName = file.getName();
            BlobId blobId = BlobId.of(configLoader.bucketNames.get(0), objectName);

            // 파일 처리 시작 로그
            log.info("{}번째 파일 처리 시작: {}", fileCounter, objectName);

            // 버킷에 해당 파일이 있는 지 확인
            if (storage.get(blobId) != null) {
                log.warn("이미지 파일이 이미 버킷에 존재: {}", objectName);
                deleteFileInBucket(storage,blobId);
                fileCounter++;
                continue;  // 스킵
            }
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

            // 버킷 업로드
            try { // 이미지 버킷에 업로드
                storage.create(blobInfo, Files.readAllBytes(file.toPath()));
                log.info("{} 파일 업로드 성공", file.getName());
            } catch (IOException e) {
                log.error("파일 업로드 중 오류 발생: {}", file.getName(), e);
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
                    log.error("OCR 요청 실패: {}", response.message());
                    fileCounter++; // 카운터 증가
                    continue;
                }

                String responseBody = response.body().string();
                String outputFileName = file.getName().substring(0, file.getName().lastIndexOf("."));
                String outputPath = configLoader.resultFilePath + "\\" + outputFileName + "_OCR_result.json";
                try (FileWriter writer = new FileWriter(outputPath)) {
                    writer.write(responseBody);
                    jsonFilePaths.add(outputPath); // JSON 파일 경로 리스트에 추가
                }
                log.info("OCR 요청 성공");
                deleteFileInBucket(storage, blobId);
            }
            // 파일 처리 완료 로그
            log.info("{}번째 파일 처리 완료: {}", fileCounter, objectName);
            // 카운터 증가
            fileCounter++;
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
        System.out.println("111 :" + configLoader.bucketNames);
        GoogleCredentials credentials = ServiceAccountCredentials.fromStream(new FileInputStream(configLoader.keyFilePath)).createScoped(configLoader.cloudPlatform);
        return StorageOptions.newBuilder().setProjectId(configLoader.projectId).setCredentials(credentials).build().getService();
    }

    //구글 버킷 파일 삭제
    public void deleteFileInBucket(Storage storage, BlobId blobId) {
        boolean deleted = storage.delete(blobId);
        if (deleted) {
            log.info("{}버킷에서 {} 파일 삭제완료", blobId.getBucket(), blobId.getName());
        } else {
            log.error("{}버킷에서 삭제되지 않은 파일 :{} ", blobId.getBucket(), blobId.getName());
        }
    }

}
