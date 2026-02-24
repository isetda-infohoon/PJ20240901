package com.isetda.idpengine;

import com.mashape.unirest.http.exceptions.UnirestException;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SynapService {
    private static final Logger log = LogManager.getLogger(SynapService.class);
    public ConfigLoader configLoader = ConfigLoader.getInstance();

    public boolean checkBadImg;

    public void synapTest() {
        File testImage = new File(configLoader.imageFolderPath + "/file_upload_06.pdf");

        if (!testImage.exists()) {
            System.out.println("이미지 존재하지 않음");
        } else {
            synapOCR(testImage, "");
        }
    }

    public void synapOCR(File file, String subPath) {
        log.info("Start Upload and OCR with Synap");

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
                    .addFormDataPart("api_key", configLoader.synapApiKey)
                    .addFormDataPart("type", "upload")
                    .addFormDataPart("image", file.getName(), fileBody)
                    .addFormDataPart("textout", "true")  // full text 생성 여부
                    .addFormDataPart("extract_table", "true")
                    //.addFormDataPart("recog_form", "true")
                    .build();

            Request request = new Request.Builder()
                    .url(configLoader.synapOcrUrl + "ocr")
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Synap OCR Request failed: " + response.code() + ", " + response.message());

                    //TODO: SynapOCR 실패 시 해당 파일의 원본 정보를 DELETE, CALLBACK 호출 (DELETE 시 DIVISION 된 page 모두 삭제 필요)
                    deleteErrorFile(file, localDir, subPath);
                    return;
                }

                String responseBody = response.body().string();
                log.trace("Synap OCR Response Success: " + responseBody);

                if (responseBody.contains("Check your file format.")) {
                    checkBadImg = false;
                    log.warn("Bad image data: {} - Check your file format", file.getName());
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
                            JSONObject json = new JSONObject(responseBody);
                            String prettyJson = json.toString(4);
                            writer.write(prettyJson);
                        }
                        // jsonFilePaths.add(outputPath);
                        log.debug("Synap OCR JSON file download successful");

                        JSONObject json = new JSONObject(responseBody);
                        JSONObject result = json.getJSONObject("result");
                        JSONArray tableList = result.getJSONArray("table_list");


                        // EXCEL 다운로드
                        if (configLoader.excelFileDownload) {
                            String tableExcelFile = result.getString("table_excel_file");
                            if (tableExcelFile != null && !tableExcelFile.trim().isEmpty()) {
                                synapExcelDownload(outputFileName, tableExcelFile);
                                log.debug("Excel file download successful: {}", file.getName());
                            } else {
                                log.debug("No Excel file to download for: {}", file.getName());
                            }
                        }

                        // CSV 다운로드
                        if (configLoader.csvFileDownload) {
                            int cnt = 0;
                            for (int i = 0; i < tableList.length(); i++) {
                                JSONObject table = tableList.getJSONObject(i);
                                String csvFileName = table.optString("csv_file_name", "");

                                if (csvFileName != null && !csvFileName.trim().isEmpty()) {
                                    synapCSVDownload(outputFileName, csvFileName, cnt);
                                    cnt++;
                                    log.debug("CSV file download successful: {}", file.getName());
                                } else {
                                    log.debug("No CSV file to download at index {} for: {}", i, file.getName());
                                }
                            }
                        }

                        // HTML 다운로드
                        if (configLoader.htmlFileDownload) {
                            int cnt = 0;
                            for (int i = 0; i < tableList.length(); i++) {
                                JSONObject table = tableList.getJSONObject(i);
                                String htmlFileName = table.optString("html_file_name", "");

                                if (htmlFileName != null && !htmlFileName.trim().isEmpty()) {
                                    synapHtmlDownload(outputFileName, htmlFileName, cnt);
                                    cnt++;
                                    log.info("HTML file download successful: {}", file.getName());
                                } else {
                                    log.info("No HTML file to download at index {} for: {}", i, file.getName());
                                }
                            }
                        }

                        // OCR 성공 시 ocrResultFileName DB 정보 업데이트
//                        APICaller apiCaller = new APICaller();
//                        String userId = configLoader.apiUserId;
//                        String fileName = file.getName();
//                        FileInfo fileInfo = apiCaller.getFileByName(userId, fileName);
//
//                        if (fileInfo == null || fileInfo.getFilename() == null) {
//                            log.warn("파일 정보가 없습니다. API 호출 생략: {}", fileName);
//                            return;
//                        }
//
//                        JSONObject jsonBody = new JSONObject();
//                        jsonBody.put("fileName", fileInfo.getFilename());
//                        jsonBody.put("pageNum", fileInfo.getPageNum());
//                        jsonBody.put("userId", userId);
//                        jsonBody.put("ocrServiceType", fileInfo.getOcrServiceType());
//                        jsonBody.put("language", fileInfo.getLanguage());
//                        jsonBody.put("ocrResultFileName", outputFileName + "_result.dat");
//
//                        apiCaller.callUpdateApi(jsonBody);
//
//                        FileInfo fileInfoCheck = apiCaller.getFileByName(userId, fileName);
                    } catch (Exception e) {
                        log.error("Error saving Synap OCR response", e);

                        //TODO: TEST 필요 (deleteAPI 호출 -> 원본, division page의 DB 정보 모두 삭제된 후 각 페이지 OCR 진행이 어떻게 처리되는지)
                        deleteErrorFile(file, localDir, subPath);
                    }
                }
                log.info("OCR request successful");
                checkBadImg = true;
            }
        } catch (Exception e) {
            log.error("Synap OCR error: " + e);
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
            apiCaller.callDeleteApi(configLoader.apiUserId, fileInfo.getFilename(), fileInfo.getServiceType());
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

    public void synapExcelDownload(String outputFileName, String tableExcelFileName) {
        log.debug("Start downloading Excel file from Synap OCR");

        OkHttpClient client = new OkHttpClient();

        String downloadUrl = configLoader.synapOcrUrl + "out/" + tableExcelFileName;

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_key", configLoader.synapApiKey)
                .build();

        Request request = new Request.Builder()
                .url(downloadUrl)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Excel download failed: " + response.code() + ", " + response.message());
                return;
            }

            // 저장 경로 설정
            File localDir = new File(configLoader.resultFilePath);
            if (!localDir.exists()) {
                localDir.mkdirs();
            }

            File outputFile = new File(localDir, outputFileName + "_result.xlsx");

            try (InputStream inputStream = response.body().byteStream();
                 FileOutputStream outputStream = new FileOutputStream(outputFile)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                log.debug("Excel file downloaded successfully: {}", outputFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Error downloading Excel file", e);
        }

    }

    public void synapCSVDownload(String outputFileName, String tableCSVFileName, int cnt) {
        log.debug("Start downloading CSV file from Synap OCR");

        OkHttpClient client = new OkHttpClient();

        String downloadUrl = configLoader.synapOcrUrl + "out/" + tableCSVFileName;

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_key", configLoader.synapApiKey)
                .build();

        Request request = new Request.Builder()
                .url(downloadUrl)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("CSV download failed: " + response.code() + ", " + response.message());
                return;
            }

            // 저장 경로 설정
            File localDir = new File(configLoader.resultFilePath);
            if (!localDir.exists()) {
                localDir.mkdirs();
            }

            File outputFile = new File(localDir, outputFileName + "_result_" + cnt + ".csv");

//            if (cnt == 1) {
//                outputFile = new File(localDir, outputFileName + "_result.csv");
//            } else {
//                outputFile = new File(localDir, outputFileName + "_result_" + cnt + ".csv");
//            }

            try (InputStream inputStream = response.body().byteStream();
                 FileOutputStream outputStream = new FileOutputStream(outputFile)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                log.debug("CSV file downloaded successfully (cnt={}): {}", cnt, outputFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Error downloading CSV file", e);
        }
    }

    public void synapHtmlDownload(String outputFileName, String tableHtmlFileName, int cnt) {
        log.debug("Start downloading HTML file from Synap OCR");

        OkHttpClient client = new OkHttpClient();

        String downloadUrl = configLoader.synapOcrUrl + "out/" + tableHtmlFileName;

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_key", configLoader.synapApiKey)
                .build();

        Request request = new Request.Builder()
                .url(downloadUrl)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("HTML download failed: " + response.code() + ", " + response.message());
                return;
            }

            // 저장 경로 설정
            File localDir = new File(configLoader.resultFilePath);
            if (!localDir.exists()) {
                localDir.mkdirs();
            }

            File outputFile = new File(localDir, outputFileName + "_result_" + cnt + ".html");

            try (InputStream inputStream = response.body().byteStream();
                 FileOutputStream outputStream = new FileOutputStream(outputFile)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                log.debug("HTML file downloaded successfully (html={}): {}", cnt, outputFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Error downloading HTML file", e);
        }
    }
}
