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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
                    deleteErrorFile(file, localDir, subPath);
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

                        if (fid != null && !fid.trim().isEmpty()) {
                            zipDownload(outputFileName, fid);
                            log.debug("ZIP file download successful: {}", file.getName());
                        } else {
                            log.debug("No ZIP file to download for: {}", file.getName());
                        }
                    } catch (Exception e) {
                        log.error("Error saving DocuAnalyzer result file", e);

                        //TODO: TEST 필요 (deleteAPI 호출 -> 원본, division page의 DB 정보 모두 삭제된 후 각 페이지 OCR 진행이 어떻게 처리되는지)
                        deleteErrorFile(file, localDir, subPath);
                    }
                }

                log.info("DocuAnalyzer request successful");
                checkBadImg = true;
            }
        } catch (Exception e) {
            log.error("DocuAnalyzer error: " + e);
        }
    }

    public void docuAnalyzerForExtendedFormats(File file, String subPath) {
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
                    deleteErrorFile(file, localDir, subPath);
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

                String outputFileName = file.getName().substring(0, file.getName().lastIndexOf("."));
                String outputPath = configLoader.resultFilePath + File.separator + outputFileName + "_result.dat";

                try {
                    if (fid != null && !fid.trim().isEmpty()) {
                        zipDownload(outputFileName, fid);
                        log.debug("ZIP file download successful: {}", file.getName());

                        File downloadedZip = new File(configLoader.resultFilePath, outputFileName + "_result" + ".zip");
                        extractMdFromZipAndSaveAsDat(downloadedZip, outputFileName, configLoader.resultFilePath);

                    } else {
                        log.debug("No ZIP file to download for: {}", file.getName());
                    }
                } catch (Exception e) {
                    log.error("Error saving DocuAnalyzer result file", e);

                    //TODO: TEST 필요 (deleteAPI 호출 -> 원본, division page의 DB 정보 모두 삭제된 후 각 페이지 OCR 진행이 어떻게 처리되는지)
                    deleteErrorFile(file, localDir, subPath);
                }

                log.info("DocuAnalyzer request successful");
                checkBadImg = true;
            }
        } catch (Exception e) {
            log.error("DocuAnalyzer error: " + e);
        }
    }

    public void extractMdFromZipAndSaveAsDat(File zipFile, String baseOutputName, String resultDirPath) throws IOException {
        // ZIP 내부 파일명: 원본명.확장자_0001.md → 결과: 원본명-page1_result.dat
        Pattern pat = Pattern.compile("^(.+)\\.(.+)_(\\d{4})\\.md$", Pattern.CASE_INSENSITIVE);

        Path outDir = Paths.get(resultDirPath);
        Files.createDirectories(outDir);

        try (ZipFile zf = new ZipFile(zipFile, StandardCharsets.UTF_8)) { // ⚠️ 엔트리 이름 인코딩: UTF-8 기준
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (e.isDirectory()) continue;

                String name = Paths.get(e.getName()).getFileName().toString(); // 하위 폴더 제거
                Matcher m = pat.matcher(name);
                if (!m.matches()) continue;

                int page = Integer.parseInt(m.group(3)); // "0001" → 1
                Path target = outDir.resolve(baseOutputName + "-page" + page + "_result.dat");

                try (InputStream is = zf.getInputStream(e);
                     Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                     Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8,
                             StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                    char[] buf = new char[8192];
                    int n;
                    while ((n = reader.read(buf)) != -1) {
                        writer.write(buf, 0, n);
                    }
                }
            }
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

            log.debug("ZIP saved: {}", zipFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to download ZIP: {}", e.getMessage(), e);
            return;
        }
    }

    public void deleteErrorFile(File file, File localDir, String subPath) {
        APICaller apiCaller = new APICaller();

        // 원본 확장자 확인
        Resolution res = resolveOriginal(file);

        // 오류 디렉토리 이동
        for (File f : res.toMove) {
            try {
                IOService.moveFileToErrorDirectory(f, subPath);
            } catch (Exception e) {
                log.warn("Move failed: {}", f.getName(), e);
            }
        }

        String apiFileName = Paths.get(subPath, res.originalName)
                .toString()
                .replace(File.separatorChar, '/');

        try {
            FileInfo info = apiCaller.getFileByNameAndPageNum(
                    configLoader.apiUserId, apiFileName, 0);

            if (info != null) {
                apiCaller.callDeleteApi(
                        configLoader.apiUserId, info.getFilename(), info.getOcrServiceType());

                if (info.getUrlData() != null) {
                    String errDir = Paths.get(configLoader.resultFilePath, "오류", subPath)
                            .toString()
                            .replace(File.separatorChar, '/');
                    apiCaller.callbackApi(info, errDir, 666, "DocuAnalyzer Error");
                }
            }
        } catch (Exception e) {
            log.error("DELETE/CALLBACK 실패", e);
        }

        // localDir 정리 (파일만 삭제)
        if (localDir.exists()) {
            File[] arr = localDir.listFiles(File::isFile);
            if (arr != null) {
                for (File f : arr) {
                    if (!f.delete()) log.warn("DELETE FAIL: {}", f.getName());
                }
            }
        }
    }

    private Resolution resolveOriginal(File file) {
        String name = file.getName();
        String ext = FileExtensionUtil.getExtension(name).toLowerCase();
        String base = stripExt(name);

        // PDF → JPG: xxx-pageN.jpg
        if (name.matches("(?i).+-page\\d+\\.jpg$")) {
            String original = name.replaceAll("(?i)-page\\d+\\.jpg$", ".pdf");
            return new Resolution(original, List.of(file));
        }

        // JPG 원본
        if (ext.equals("jpg")) {
            return new Resolution(name, List.of(file));
        }

        // DA 확장자 (docx/pptx/xlsx/hwp 등)
        if (FileExtensionUtil.DA_SUPPORTED_EXT.contains(ext)) {
            List<File> list = new ArrayList<>();
            File zip = new File(configLoader.resultFilePath, base + "_result.zip");
            File dat = new File(configLoader.resultFilePath, base + "_result.dat");

            if (zip.exists()) list.add(zip);
            if (dat.exists()) list.add(dat);
            return new Resolution(name, list);
        }

        // 그 외 → 그냥 원본 파일
        return new Resolution(name, List.of(file));
    }

    private static class Resolution {
        final String originalName;  // API용 원본 파일명
        final List<File> toMove;    // 에러 디렉토리로 이동할 파일들

        Resolution(String originalName, List<File> toMove) {
            this.originalName = originalName;
            this.toMove = toMove;
        }
    }

    private static String stripExt(String name) {
        int idx = name.lastIndexOf('.');
        return idx > 0 ? name.substring(0, idx) : name;
    }
}
