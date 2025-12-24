package com.isetda.idpengine;

import com.mashape.unirest.http.exceptions.UnirestException;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocuAnalyzerService {
    private static final Logger log = LogManager.getLogger(SynapService.class);
    public ConfigLoader configLoader = ConfigLoader.getInstance();
    public boolean checkBadImg;

    public void docuAnalyzer(File file, String subPath) {
        log.info("Start Upload and OCR with DocuAnalyzer");

        File localDir = new File(configLoader.resultFilePath);

        if (!localDir.exists()) {
            localDir.mkdirs();
            log.debug("Create a resulting directory: ", configLoader.resultFilePath);
        }

        // PDF 파일 제외
        if (file.getName().toLowerCase().endsWith(".pdf")) {
            log.debug("Skipping PDF file: {}", file.getName());
            return;
        }

        // HWP 파일 제외
        if (file.getName().toLowerCase().endsWith(".hwp")) {
            log.debug("Skipping HWP file: {}", file.getName());
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)  // 연결 타임아웃
                .readTimeout(30, TimeUnit.SECONDS)     // 응답 읽기 타임아웃
                .writeTimeout(30, TimeUnit.SECONDS)    // 요청 쓰기 타임아웃
                .retryOnConnectionFailure(true)
                .addInterceptor(new RetryInterceptor(3, 2000)) // 최대 3회 재시도, 2초씩 증가
                .build();

        try {
            RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("api_key", configLoader.docuAnalyzerApiKey)
                    .addFormDataPart("type", "upload")
                    .addFormDataPart("file", file.getName(), fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(configLoader.docuAnalyzerUrl + "da")
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("DocuAnalyzer Request failed: " + response.code() + ", " + response.message());

                    //TODO: docuAnalyzer 실패 시 해당 파일의 원본 정보를 DELETE, CALLBACK 호출 (DELETE 시 DIVISION 된 page 모두 삭제 필요)
                    //deleteErrorFile(file, localDir, subPath);
                    return;
                }

                String responseBody = response.body().string();
                log.trace("DocuAnalyzer Upload Response: " + responseBody);

                JSONObject json = new JSONObject(responseBody);
                String fid = json.getJSONObject("result").getString("fid");
                log.debug("Received fid: {}", fid);

                if (!waitUntilSuccessOrFail(client, fid)) {
                    log.error("Processing  not completed or failed. fid={}", fid);
                    return;
                }

                RequestBody resultRequestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("api_key", configLoader.docuAnalyzerApiKey)
                        .addFormDataPart("page_index", "1")
                        .addFormDataPart("type", "md")
                        .build();

                Request resultRequest = new Request.Builder()
                        .url(configLoader.docuAnalyzerUrl + "result/" + fid)
                        .post(resultRequestBody)
                        .build();

                try (Response resultResponse = client.newCall(resultRequest).execute()) {
                    if (!resultResponse.isSuccessful()) {
                        log.error("DocuAnalyzer result fetch failed: " + resultResponse.code());
                        return;
                    }

                    String mdContent = resultResponse.body().string();
                    log.trace("DocuAnalyzer Result Content: " + mdContent);

                    String outputFileName = file.getName().substring(0, file.getName().lastIndexOf("."));
                    String outputPath = configLoader.resultFilePath + File.separator + outputFileName + "_result.dat";

                    try {
                        if (configLoader.encodingCheck) {
                            byte[] encData = JsonService.aesEncode(mdContent);
                            Files.write(Paths.get(outputPath), encData);

                            byte[] fileContent = Files.readAllBytes(Paths.get(outputPath));
                            String decodedText = JsonService.aesDecode(fileContent);
                            log.debug("Decoding text: {}", decodedText);
                        } else {
                            Files.write(Paths.get(outputPath), mdContent.getBytes(StandardCharsets.UTF_8));
                        }
                        // jsonFilePaths.add(outputPath);
                        log.debug("DocuAnalyzer md file saved successfully");

                        //if (configLoader.excelFileDownload) {
                            if (fid != null && !fid.trim().isEmpty()) {
                                zipDownload(outputFileName, fid);
                                log.debug("ZIP file download successful: {}", file.getName());
                            } else {
                                log.debug("No ZIP file to download for: {}", file.getName());
                            }
                        //}
                    } catch (Exception e) {
                        log.error("Error saving DocuAnalyzer result file", e);

                        //TODO: TEST 필요 (deleteAPI 호출 -> 원본, division page의 DB 정보 모두 삭제된 후 각 페이지 OCR 진행이 어떻게 처리되는지)
                        //deleteErrorFile(file, localDir, subPath);
                    }
                }

                log.info("DocuAnalyzer request successful");
                checkBadImg = true;
            }
        } catch (Exception e) {
            log.error("DocuAnalyzer error: " + e);
        }
    }

    /** filestatus/{fid} 를 POST로 폴링하여 SUCCESS/FAILED/타임아웃 중 하나가 될 때까지 대기 */
    private boolean waitUntilSuccessOrFail(OkHttpClient client, String fid) {
        final long timeoutMillis = TimeUnit.MINUTES.toMillis(30); // 전체 대기 시간(필요시 조정)
        final long start = System.currentTimeMillis();
        long delayMs = 3000;                                     // 초기 1초
        final long maxDelayMs = 10000;                           // 최대 10초

        while (System.currentTimeMillis() - start < timeoutMillis) {
            try {
                RequestBody statusBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("api_key", configLoader.docuAnalyzerApiKey)
                        .build();

                Request statusReq = new Request.Builder()
                        .url(configLoader.docuAnalyzerUrl + "filestatus/" + fid)
                        .post(statusBody)
                        .build();

                try (Response statusRes = client.newCall(statusReq).execute()) {
                    if (!statusRes.isSuccessful()) {
                        log.warn("Status check failed: {}, {}", statusRes.code(), statusRes.message());
                    } else {
                        String body = statusRes.body().string();
                        log.trace("Status Response: {}", body);

                        JSONObject json = new JSONObject(body);
                        JSONObject result = json.optJSONObject("result");
                        String fileStatus = (result != null) ? result.optString("filestatus", "") : "";

                        if ("SUCCESS".equalsIgnoreCase(fileStatus)) {
                            log.info("Processing SUCCESS for fid={}", fid);
                            return true;
                        } else if ("FAILED".equalsIgnoreCase(fileStatus)) {
                            log.error("Processing FAILED for fid={}", fid);
                            return false;
                        } else if ("RUNNING".equalsIgnoreCase(fileStatus) || "LOADING".equalsIgnoreCase(fileStatus)) {
                            log.debug("Processing... fid={}, status={}", fid, fileStatus);
                        } else {
                            log.debug("Unknown filestatus='{}' for fid={}", fileStatus, fid);
                        }
                    }
                }

                Thread.sleep(delayMs);
                //delayMs = Math.min(delayMs * 2, maxDelayMs);

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Polling interrupted for fid={}", fid);
                return false;
            } catch (Exception e) {
                log.warn("Exception during status polling for fid={}: {}", fid, e.getMessage());
                try { Thread.sleep(delayMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                delayMs = Math.min(delayMs * 2, maxDelayMs);
            }
        }

        log.warn("Status polling timed out for fid={}", fid);
        return false;
    }

    public void zipDownload(String outputFileName, String fid) {
        log.debug("Start downloading ZIP file from Synap DocuAnalyzer");

        OkHttpClient client = new OkHttpClient();

        String downloadUrl = configLoader.docuAnalyzerUrl + "result-all/" + fid;

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_key", configLoader.docuAnalyzerApiKey)
                .build();

        Request request = new Request.Builder()
                .url(downloadUrl)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("ZIP download failed: " + response.code() + ", " + response.message());
                return;
            }

            // 저장 경로 설정
            File localDir = new File(configLoader.resultFilePath);
            if (!localDir.exists()) {
                localDir.mkdirs();
            }


            // ZIP 파일 저장 위치 (필요 시 이름 규칙 변경 가능)
            File zipFile = new File(localDir, outputFileName + "_result" + ".zip");

            // 응답 바디를 ZIP 파일로 저장
            ResponseBody body = response.body();
            if (body == null) {
                log.error("ZIP response body is null");
                return;
            }

            try (InputStream in = body.byteStream();
                 OutputStream out = new FileOutputStream(zipFile, false)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            } catch (IOException ioe) {
                log.error("Failed to save ZIP file: {}", ioe.getMessage(), ioe);
                return;
            }

            log.info("ZIP saved: {}", zipFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to download ZIP: {}", e.getMessage(), e);
            return;
        }
    }

    public void deleteErrorFile(File file, File localDir, String subPath) {
        APICaller apiCaller = new APICaller();
        String originalFileName;
        Pattern pattern = Pattern.compile("(.+)-page\\d+\\.jpg$");
        Matcher matcher = pattern.matcher(file.getName());

        if (matcher.matches()) {
            // -page숫자.jpg 제거 후 .pdf 붙이기
            originalFileName = matcher.group(1) + ".pdf";
        } else {
            // 그대로 반환
            originalFileName = file.getName();
        }

        log.info("subpath: {}, fileName:{}", subPath, originalFileName);
        String apiFileName = Paths.get(subPath, originalFileName).toString();
        log.info("apiFileName: " + apiFileName);
        File imageFolderPath = Paths.get(configLoader.imageFolderPath, originalFileName).toFile();
        log.info("imageFolderPath: " + imageFolderPath);
        IOService.moveFileToErrorDirectory(imageFolderPath, subPath);
        try {
            FileInfo fileInfo = apiCaller.getFileByNameAndPageNum(configLoader.apiUserId, apiFileName,0);
            log.info(fileInfo.getFilename());
            String message = "Synap OCR Error";
            apiCaller.callDeleteApi(configLoader.apiUserId, fileInfo.getFilename(), fileInfo.getOcrServiceType());
            if (fileInfo.getUrlData() != null) {
                String errorDir = Paths.get(configLoader.resultFilePath, "오류", subPath).toString();
                apiCaller.callbackApi(fileInfo, errorDir, 666, message);
            } else {
                log.info("URL DATA IS NULL");
            }
        } catch (UnirestException ex) {
            log.info("DELETE API/CALLBACK API 호출 실패: {}", ex);
        }

        if (localDir.exists() && localDir.isDirectory()) {
            File[] files = localDir.listFiles(File::isFile); // 폴더 내 파일만 가져옴
            if (files != null) {
                for (File f : files) {
                    if (f.delete()) {
                        log.info("FILE DELETE: " + f.getName());
                    } else {
                        log.info("FILE DELETE FAILED: " + f.getName());
                    }
                }
            }
        } else {
            log.warn("폴더가 존재하지 않거나 디렉토리가 아닙니다.");
        }
    }
}
