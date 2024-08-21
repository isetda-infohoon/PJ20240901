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
import java.util.Base64;
import java.util.List;

public class GoogleService {
    // 로그
    private static final Logger log = LogManager.getLogger(GoogleService.class);
    //환경변수 인스턴스 생성
    private ConfigLoader configLoader = ConfigLoader.getInstance();



    // 환경 변수
    public String PROJECT_ID;
    public List<String> BUCKET_NAMES;
    public String KEY_FILE_PATH;
    public boolean DELETED_CHECK;
    public String OCR_URL;
    public String CLOUD_PLATFORM;
    public String EXCEL_FILEPATH;
    public String IMAGE_FOLDPATH;
    public String RESULT_FILEPATH;


    // 환경 변수 불러오기
    // 생성자
    public GoogleService() {
        PROJECT_ID = configLoader.getProjectId();
        BUCKET_NAMES = configLoader.getBucketNames();
        KEY_FILE_PATH = configLoader.getKeyFilePath();
        DELETED_CHECK = configLoader.isDeletedCheck();
        OCR_URL = configLoader.getOcrUrl();
        CLOUD_PLATFORM = configLoader.getCloudPlatform();
        EXCEL_FILEPATH = configLoader.getExcelFilePath();
        IMAGE_FOLDPATH = configLoader.getImageFolderPath();
        RESULT_FILEPATH = configLoader.getResultFilePath();
    }

    //구글 버킷에 이미지 올리기 및 ocr 진행
//    public void uploadAndOCR() throws IOException {
//        File[] files = getFilteredFiles(imageFolderPath);
//        Storage storage = getStorageService();
//        File localDir = new File(resultFilePath);
//
//        if (!localDir.exists()) {
//            localDir.mkdirs();
//        }
//        String accessToken = getAccessToken();
//        OkHttpClient client = new OkHttpClient();
//
//        for (File file : files) {
//            String objectName = file.getName();
//            BlobId blobId = BlobId.of(BUCKET_NAMES.get(0), objectName);
//
//            // 버킷에 해당 파일이 있는 지 확인
//            if (storage.get(blobId) != null) {
//                log.warn("파일이 이미 버킷에 존재합니다: {}", objectName);
//                continue;  // 스킵
//            }
//            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
//
//            // 버킷 업로드
//            try { // 이미지 버킷에 업로드
//                storage.create(blobInfo, Files.readAllBytes(file.toPath()));
//                log.info("파일 업로드 성공: {}", file.getName());
//            } catch (IOException e) {
//                log.error("파일 업로드 중 오류 발생: {}", file.getName(), e);
//                throw e;
//            }
//
//            // OCR 수행
//            byte[] imgBytes = storage.readAllBytes(blobId);
//            String imgBase64 = Base64.getEncoder().encodeToString(imgBytes);
//
//            String jsonRequest = new JSONObject().put("requests", new JSONArray().put(new JSONObject().put("image", new JSONObject().put("content", imgBase64)).put("features", new JSONArray().put(new JSONObject().put("type", "TEXT_DETECTION"))))).toString();
//
//            RequestBody body = RequestBody.create(jsonRequest, MediaType.parse("application/json; charset=utf-8"));
//            Request request = new Request.Builder().url(OCR_URL).addHeader("Authorization", "Bearer " + accessToken).post(body).build();
//
//            try (Response response = client.newCall(request).execute()) {
//                if (!response.isSuccessful()) {
//                    log.error("OCR 요청 실패: {}", response.message());
//                    continue;
//                }
//
//                String responseBody = response.body().string();
//                String outputFileName = file.getName().substring(0, file.getName().lastIndexOf("."));
//                String outputPath = resultFilePath + outputFileName + "_OCR_result.json";
//                try (FileWriter writer = new FileWriter(outputPath)) {
//                    writer.write(responseBody);
//                    log.info("OCR 결과 경로 :{}", outputPath);
//                    jsonFilePaths.add(outputPath); // JSON 파일 경로 리스트에 추가
//                }
//                deleteFileInBucket(storage, blobId);
//            }
//        }
//    }

    //구글 인증 토근 설정
    public String getAccessToken() throws IOException {
        GoogleCredentials credentials = ServiceAccountCredentials.fromStream(new FileInputStream(KEY_FILE_PATH)).createScoped(CLOUD_PLATFORM);
        //https://www.googleapis.com/auth/cloud-platform 이거 향후에 바뀔 수 있는 지 확인하기(환경파일로 빼기)
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }
    //구글 저장소 가져오기
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

}
