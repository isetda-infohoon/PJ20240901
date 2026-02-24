package com.isetda.idpengine;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class APICaller {
    private static final Logger log = LogManager.getLogger(APICaller.class);
    public ConfigLoader configLoader = ConfigLoader.getInstance();

    public List<FileInfo> getFileWithStatus(String userId) throws UnirestException {
        String defaultUrl = configLoader.apiURL;
        List<FileInfo> fileList = new ArrayList<>();

        log.info("getFileWithStatus call");

        String apiUrl = defaultUrl + "/doc/process/status";
        HttpResponse<String> response = Unirest.get(apiUrl)
                .queryString("USERID", userId)
                .queryString("FILENAME", "")
                .queryString("SERVICE_TYPE", "")
                .queryString("CLASSIFICATION_STATUS", "")
                .queryString("REQUEST_START_DATE", "")
                .queryString("REQUEST_END_DATE", "")
                .asString();

        if (response.getStatus() == 200) {
            log.debug("STATUS CHECK API 호출 성공: " + response.getBody());
            JSONObject statusJson = new JSONObject(response.getBody());

            if (statusJson.getBoolean("success")) {
                JSONArray dataArray = statusJson.getJSONArray("data");

                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject data = dataArray.getJSONObject(i);

                    if ("Y".equalsIgnoreCase(data.optString("cancelStatus")) ||
                            (data.optString("classificationStatus") != null && !data.optString("classificationStatus").trim().isEmpty())) {
                        continue;
                    }

                    FileInfo fileInfo = new FileInfo();
                    fileInfo.setFilename(data.optString("fileName"));
                    fileInfo.setUserId(data.optString("userId"));
                    fileInfo.setPageNum(data.optInt("pageNum"));
                    fileInfo.setServiceType(data.optString("serviceType"));
                    fileInfo.setGroupUID(data.optString("grouP_UID"));
                    fileInfo.setLanguage(data.optString("language"));
                    fileInfo.setLClassification(data.optString("lClassification"));
                    fileInfo.setMClassification(data.optString("mClassification"));
                    fileInfo.setSClassification(data.optString("sClassification"));
                    fileInfo.setClassificationStatus(data.optString("classificationStatus"));
                    fileInfo.setJobType(data.optString("jobType"));
                    fileInfo.setRequestId(data.optString("requestId"));
                    fileInfo.setReceiveData(data.optString("receiveData"));
                    fileInfo.setUrlData(data.optString("urlData"));
                    fileInfo.setTaskName(data.optString("taskName"));

                    fileList.add(fileInfo);
                }
            }
        } else {
            log.info("STATUS CHECK API 호출 실패: " + response.getStatus() + " - " + response.getStatusText());
        }

//        for (FileInfo file : fileList) {
//            System.out.println(file.getFilename());
//            System.out.println(file.getUserId());
//            System.out.println(file.getPageNum());
//            System.out.println(file.getServiceType());
//            System.out.println(file.getLanguage());
//            System.out.println(file.getLClassification());
//            System.out.println(file.getMClassification());
//            System.out.println(file.getSClassification());
//            System.out.println(file.getClassificationStatus());
//        }
        return fileList;
    }

    // 데이터를 조회하고 해당 정보를 반환
    public FileInfo getFileByName(String userId, String filename) throws UnirestException {
        String defaultUrl = configLoader.apiURL;
        FileInfo fileInfo = new FileInfo();

        //log.trace("getFileWithStatus call");

        filename = toWindowsPath(filename);

        String apiUrl = defaultUrl + "/doc/process/status";
        HttpResponse<String> response = Unirest.get(apiUrl)
                .queryString("USERID", userId)
                .queryString("FILENAME", filename)
                .queryString("SERVICE_TYPE", "")
                .queryString("CLASSIFICATION_STATUS", "")
                .queryString("REQUEST_START_DATE", "")
                .queryString("REQUEST_END_DATE", "")
                .asString();

        if (response.getStatus() == 200) {
            log.debug("STATUS CHECK API 호출 성공: " + response.getBody());
            JSONObject statusJson = new JSONObject(response.getBody());

            if (statusJson.getBoolean("success")) {
                JSONArray dataArray = statusJson.getJSONArray("data");

                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject data = dataArray.getJSONObject(i);

                    if ("Y".equalsIgnoreCase(data.optString("cancelStatus")) ||
                            (data.optString("classificationStatus") != null && !data.optString("classificationStatus").trim().isEmpty())) {
                        continue;
                    }

                    fileInfo.setFilename(data.optString("fileName"));
                    fileInfo.setUserId(data.optString("userId"));
                    fileInfo.setPageNum(data.optInt("pageNum"));
                    fileInfo.setServiceType(data.optString("serviceType"));
                    fileInfo.setGroupUID(data.optString("grouP_UID"));
                    fileInfo.setLanguage(data.optString("language"));
                    fileInfo.setLClassification(data.optString("lClassification"));
                    fileInfo.setMClassification(data.optString("mClassification"));
                    fileInfo.setSClassification(data.optString("sClassification"));
                    fileInfo.setClassificationStatus(data.optString("classificationStatus"));
                    fileInfo.setJobType(data.optString("jobType"));
                    fileInfo.setClassificationStartDateTime(data.optString("classificationStartDateTime"));
                    fileInfo.setClassificationEndDateTime(data.optString("classificationEndDateTime"));
                    fileInfo.setCreateDateTime(data.optString("createDateTime"));
                    fileInfo.setRequestId(data.optString("requestId"));
                    fileInfo.setReceiveData(data.optString("receiveData"));
                    fileInfo.setUrlData(data.optString("urlData"));
                    fileInfo.setTaskName(data.optString("taskName"));
                }
            }
        } else {
            log.info("STATUS CHECK API 호출 실패: " + response.getStatus() + " - " + response.getStatusText());
        }

//        System.out.println(fileInfo.getFilename());
//        System.out.println(fileInfo.getUserId());
//        System.out.println(fileInfo.getPageNum());
//        System.out.println(fileInfo.getserviceType());
//        System.out.println(fileInfo.getLanguage());
//        System.out.println(fileInfo.getLClassification());
//        System.out.println(fileInfo.getMClassification());
//        System.out.println(fileInfo.getSClassification());
//        System.out.println(fileInfo.getClassificationStatus());

        return fileInfo;
    }

    public FileInfo getFileByNameAndPageNum(String userId, String filename, int pageNum) throws UnirestException {
        String defaultUrl = configLoader.apiURL;
        FileInfo fileInfo = new FileInfo();

        //log.trace("getFileWithStatus call");

        filename = toWindowsPath(filename);

        String apiUrl = defaultUrl + "/doc/process/status";
        HttpResponse<String> response = Unirest.get(apiUrl)
                .queryString("USERID", userId)
                .queryString("FILENAME", filename)
                .queryString("SERVICE_TYPE", "")
                .queryString("CLASSIFICATION_STATUS", "")
                .queryString("REQUEST_START_DATE", "")
                .queryString("REQUEST_END_DATE", "")
                .queryString("PAGENUM", pageNum)
                .asString();

        if (response.getStatus() == 200) {
            log.debug("STATUS CHECK API 호출 성공: " + response.getBody());
            JSONObject statusJson = new JSONObject(response.getBody());

            if (statusJson.getBoolean("success")) {
                JSONArray dataArray = statusJson.getJSONArray("data");

                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject data = dataArray.getJSONObject(i);

                    if ("Y".equalsIgnoreCase(data.optString("cancelStatus")) ||
                            (data.optString("classificationStatus") != null && !data.optString("classificationStatus").trim().isEmpty())) {
                        continue;
                    }

                    fileInfo.setFilename(data.optString("fileName"));
                    fileInfo.setUserId(data.optString("userId"));
                    fileInfo.setPageNum(data.optInt("pageNum"));
                    fileInfo.setServiceType(data.optString("serviceType"));
                    fileInfo.setGroupUID(data.optString("grouP_UID"));
                    fileInfo.setLanguage(data.optString("language"));
                    fileInfo.setLClassification(data.optString("lClassification"));
                    fileInfo.setMClassification(data.optString("mClassification"));
                    fileInfo.setSClassification(data.optString("sClassification"));
                    fileInfo.setClassificationStatus(data.optString("classificationStatus"));
                    fileInfo.setJobType(data.optString("jobType"));
                    fileInfo.setClassificationStartDateTime(data.optString("classificationStartDateTime"));
                    fileInfo.setClassificationEndDateTime(data.optString("classificationEndDateTime"));
                    fileInfo.setCreateDateTime(data.optString("createDateTime"));
                    fileInfo.setRequestId(data.optString("requestId"));
                    fileInfo.setReceiveData(data.optString("receiveData"));
                    fileInfo.setUrlData(data.optString("urlData"));
                    fileInfo.setTaskName(data.optString("taskName"));
                }
            }
        } else {
            log.info("STATUS CHECK API 호출 실패: " + response.getStatus() + " - " + response.getStatusText());
        }

//        System.out.println(fileInfo.getFilename());
//        System.out.println(fileInfo.getUserId());
//        System.out.println(fileInfo.getPageNum());
//        System.out.println(fileInfo.getServiceType());
//        System.out.println(fileInfo.getLanguage());
//        System.out.println(fileInfo.getLClassification());
//        System.out.println(fileInfo.getMClassification());
//        System.out.println(fileInfo.getSClassification());
//        System.out.println(fileInfo.getClassificationStatus());

        return fileInfo;
    }

    // CLASSIFICATION_STATUS 가 NULL 이 아닐 때에도 사용 가능
    public FileInfo getFileByNameAndPageNumAndStatusNotNull(String userId, String filename, String status, int pageNum) throws UnirestException {
        String defaultUrl = configLoader.apiURL;
        FileInfo fileInfo = new FileInfo();

        //log.trace("getFileWithStatus call");

        filename = toWindowsPath(filename);

        String apiUrl = defaultUrl + "/doc/process/status";
        HttpResponse<String> response = Unirest.get(apiUrl)
                .queryString("USERID", userId)
                .queryString("FILENAME", filename)
                .queryString("SERVICE_TYPE", "")
                .queryString("CLASSIFICATION_STATUS", status)
                .queryString("REQUEST_START_DATE", "")
                .queryString("REQUEST_END_DATE", "")
                .queryString("PAGENUM", pageNum)
                .asString();

        if (response.getStatus() == 200) {
            log.debug("STATUS CHECK API 호출 성공: " + response.getBody());
            JSONObject statusJson = new JSONObject(response.getBody());

            if (statusJson.getBoolean("success")) {
                JSONArray dataArray = statusJson.getJSONArray("data");

                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject data = dataArray.getJSONObject(i);

                    if ("Y".equalsIgnoreCase(data.optString("cancelStatus"))) {
                        continue;
                    }

                    fileInfo.setFilename(data.optString("fileName"));
                    fileInfo.setUserId(data.optString("userId"));
                    fileInfo.setPageNum(data.optInt("pageNum"));
                    fileInfo.setServiceType(data.optString("serviceType"));
                    fileInfo.setGroupUID(data.optString("grouP_UID"));
                    fileInfo.setLanguage(data.optString("language"));
                    fileInfo.setLClassification(data.optString("lClassification"));
                    fileInfo.setMClassification(data.optString("mClassification"));
                    fileInfo.setSClassification(data.optString("sClassification"));
                    fileInfo.setClassificationStatus(data.optString("classificationStatus"));
                    fileInfo.setJobType(data.optString("jobType"));
                    fileInfo.setClassificationStartDateTime(data.optString("classificationStartDateTime"));
                    fileInfo.setClassificationEndDateTime(data.optString("classificationEndDateTime"));
                    fileInfo.setCreateDateTime(data.optString("createDateTime"));
                    fileInfo.setRequestId(data.optString("requestId"));
                    fileInfo.setReceiveData(data.optString("receiveData"));
                    fileInfo.setUrlData(data.optString("urlData"));
                    fileInfo.setTaskName(data.optString("taskName"));
                }
            }
        } else {
            log.info("STATUS CHECK API 호출 실패: " + response.getStatus() + " - " + response.getStatusText());
        }

//        System.out.println(fileInfo.getFilename());
//        System.out.println(fileInfo.getUserId());
//        System.out.println(fileInfo.getPageNum());
//        System.out.println(fileInfo.getServiceType());
//        System.out.println(fileInfo.getLanguage());
//        System.out.println(fileInfo.getLClassification());
//        System.out.println(fileInfo.getMClassification());
//        System.out.println(fileInfo.getSClassification());
//        System.out.println(fileInfo.getClassificationStatus());

        return fileInfo;
    }

    // 이미 분류 완료된 데이터를 조회하기 위해 CLASSIFICATION_STATUS를 추가 전달
    public FileInfo getFileByNameAndStatus(String userId, String filename, String status) throws UnirestException {
        String defaultUrl = configLoader.apiURL;

        FileInfo fileInfo = new FileInfo();

        //log.trace("getFileWithStatus Call");

        filename = toWindowsPath(filename);

        String apiUrl = defaultUrl + "/doc/process/status";
        HttpResponse<String> response = Unirest.get(apiUrl)
                .queryString("USERID", userId)
                .queryString("FILENAME", filename)
                .queryString("SERVICE_TYPE", "")
                .queryString("CLASSIFICATION_STATUS", status)
                .queryString("REQUEST_START_DATE", "")
                .queryString("REQUEST_END_DATE", "")
                .asString();

        if (response.getStatus() == 200) {
            log.debug("STATUS CHECK API 호출 성공: " + response.getBody());
            JSONObject statusJson = new JSONObject(response.getBody());

            if (statusJson.getBoolean("success")) {
                JSONArray dataArray = statusJson.getJSONArray("data");

                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject data = dataArray.getJSONObject(i);

                    if ("Y".equalsIgnoreCase(data.optString("cancelStatus"))) {
                        continue;
                    }

                    fileInfo.setFilename(data.optString("fileName"));
                    fileInfo.setUserId(data.optString("userId"));
                    fileInfo.setPageNum(data.optInt("pageNum"));
                    fileInfo.setServiceType(data.optString("serviceType"));
                    fileInfo.setGroupUID(data.optString("grouP_UID"));
                    fileInfo.setLanguage(data.optString("language"));
                    fileInfo.setLClassification(data.optString("lClassification"));
                    fileInfo.setMClassification(data.optString("mClassification"));
                    fileInfo.setSClassification(data.optString("sClassification"));
                    fileInfo.setClassificationStatus(data.optString("classificationStatus"));
                    fileInfo.setJobType(data.optString("jobType"));
                    fileInfo.setClassificationStartDateTime(data.optString("classificationStartDateTime"));
                    fileInfo.setClassificationEndDateTime(data.optString("classificationEndDateTime"));
                    fileInfo.setRequestId(data.optString("requestId"));
                    fileInfo.setReceiveData(data.optString("receiveData"));
                    fileInfo.setUrlData(data.optString("urlData"));
                    fileInfo.setTaskName(data.optString("taskName"));
                }
            }
        } else {
            log.info("STATUS CHECK API 호출 실패: " + response.getStatus() + " - " + response.getStatusText());
        }

        return fileInfo;
    }

    public List<FileInfo> getAllFilesWithStatus(String userId, String status) throws UnirestException{
        String defaultUrl = configLoader.apiURL;

        List<FileInfo> fileList = new ArrayList<>();

        log.info("getFileWithStatus Call");

        String apiUrl = defaultUrl + "/doc/process/status";
        HttpResponse<String> response = Unirest.get(apiUrl)
                .queryString("USERID", userId)
                .queryString("FILENAME", "")
                .queryString("SERVICE_TYPE", "")
                .queryString("CLASSIFICATION_STATUS", status)
                .queryString("REQUEST_START_DATE", "")
                .queryString("REQUEST_END_DATE", "")
                .queryString("PAGENUM", "")
                .asString();

        //System.out.println("Status: " + response.getStatus());
        //System.out.println("Body: " + response.getBody());

        if (response.getStatus() == 200) {
            log.info("STATUS CHECK API 호출 성공");
            JSONObject statusJson = new JSONObject(response.getBody());

            if (statusJson.getBoolean("success")) {
                JSONArray dataArray = statusJson.getJSONArray("data");

                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject data = dataArray.getJSONObject(i);

                    if ("Y".equalsIgnoreCase(data.optString("cancelStatus"))) {
                        continue;
                    }

                    FileInfo fileInfo = new FileInfo();
                    fileInfo.setFilename(data.optString("fileName"));
                    fileInfo.setUserId(data.optString("userId"));
                    fileInfo.setPageNum(data.optInt("pageNum"));
                    fileInfo.setServiceType(data.optString("serviceType"));
                    fileInfo.setGroupUID(data.optString("grouP_UID"));
                    fileInfo.setLanguage(data.optString("language"));
                    fileInfo.setLClassification(data.optString("lClassification"));
                    fileInfo.setMClassification(data.optString("mClassification"));
                    fileInfo.setSClassification(data.optString("sClassification"));
                    fileInfo.setClassificationStatus(data.optString("classificationStatus"));
                    fileInfo.setJobType(data.optString("jobType"));
                    fileInfo.setClassificationStartDateTime(data.optString("classificationStartDateTime"));
                    fileInfo.setClassificationEndDateTime(data.optString("classificationEndDateTime"));
                    fileInfo.setTaskName(data.optString("taskName"));

                    fileList.add(fileInfo);
                }
            }
        } else {
            log.info("STATUS CHECK API 호출 실패: " + response.getStatus() + " - " + response.getStatusText());
        }

        return fileList;
    }

    // 가장 처음 파일 조회
    public List<FileInfo> getAllFilesWithCase(String userId, String serviceType, String status, int pageNum, String formattedDate) throws UnirestException{
        String defaultUrl = configLoader.apiURL;

        List<FileInfo> fileList = new ArrayList<>();

        //log.info("getFileWithStatus Call");

        try {
            String apiUrl = defaultUrl + "/doc/process/status";
            HttpResponse<String> response = Unirest.get(apiUrl)
                    .queryString("USERID", userId)
                    .queryString("FILENAME", "")
                    .queryString("SERVICE_TYPE", serviceType)
                    .queryString("CLASSIFICATION_STATUS", status)
                    .queryString("REQUEST_START_DATE", "")
                    .queryString("REQUEST_END_DATE", "")
                    .queryString("PAGENUM", pageNum)
                    .queryString("JOB_REQUEST_START_DATE", formattedDate) // TODO: API 조회 시 정렬 기능 추가
                    .asString();

            if (response.getStatus() == 200) {
                //log.info("STATUS CHECK API 호출 성공: " + response.getBody());
                JSONObject statusJson = new JSONObject(response.getBody());

                if (statusJson.getBoolean("success")) {
                    JSONArray dataArray = statusJson.getJSONArray("data");

                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject data = dataArray.getJSONObject(i);

                        if ("Y".equalsIgnoreCase(data.optString("cancelStatus"))) {
                            continue;
                        }

                        FileInfo fileInfo = new FileInfo();
                        fileInfo.setFilename(data.optString("fileName"));
                        fileInfo.setUserId(data.optString("userId"));
                        fileInfo.setPageNum(data.optInt("pageNum"));
                        fileInfo.setServiceType(data.optString("serviceType"));
                        fileInfo.setGroupUID(data.optString("grouP_UID"));
                        fileInfo.setLanguage(data.optString("language"));
                        fileInfo.setLClassification(data.optString("lClassification"));
                        fileInfo.setMClassification(data.optString("mClassification"));
                        fileInfo.setSClassification(data.optString("sClassification"));
                        fileInfo.setClassificationStatus(data.optString("classificationStatus"));
                        fileInfo.setJobType(data.optString("jobType"));
                        fileInfo.setClassificationStartDateTime(data.optString("classificationStartDateTime"));
                        fileInfo.setClassificationEndDateTime(data.optString("classificationEndDateTime"));
                        fileInfo.setCreateDateTime(data.optString("createDateTime"));
                        fileInfo.setTaskName(data.optString("taskName"));

                        fileList.add(fileInfo);
                    }
                }
            } else {
                log.warn("STATUS CHECK API 호출 실패: " + response.getStatus() + " - " + response.getStatusText());
            }

            // createDateTime 시간 오름차순 정렬
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                    .optionalStart()
                    .appendFraction(ChronoField.MILLI_OF_SECOND, 1, 3, true)
                    .optionalEnd()
                    .toFormatter();

            fileList.sort(Comparator.comparing(info -> {
                String dateTimeStr = info.getCreateDateTime();
                if (dateTimeStr == null || dateTimeStr.isEmpty()) {
                    return LocalDateTime.MAX;
                }
                return LocalDateTime.parse(dateTimeStr, formatter);
            }));

            //                    // 정렬 결과 로그 출력
            //                    log.info("FileList 시간순 정렬 확인");
            //                    for (FileInfo info : fileList) {
            //                        log.info("Filename: {}, StartDateTime: {}",
            //                                info.getFilename(),
            //                                info.getClassificationStartDateTime());
            //                    }
            return fileList;
        } catch (UnirestException e) {
            log.warn("API 호출 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<FileInfo> getAllFilesWithStatusAndJobType(String userId, String status, String jobType) throws UnirestException{
        String defaultUrl = configLoader.apiURL;

        List<FileInfo> fileList = new ArrayList<>();

        //log.info("getFileWithStatus Call");

        String apiUrl = defaultUrl + "/doc/process/status";
        HttpResponse<String> response = Unirest.get(apiUrl)
                .queryString("USERID", userId)
                .queryString("FILENAME", "")
                .queryString("SERVICE_TYPE", "")
                .queryString("CLASSIFICATION_STATUS", status)
                .queryString("REQUEST_START_DATE", "")
                .queryString("REQUEST_END_DATE", "")
                .queryString("JOB_TYPE", jobType)
                .asString();

        if (response.getStatus() == 200) {
            log.debug("STATUS CHECK API 호출 성공: " + response.getBody());
            JSONObject statusJson = new JSONObject(response.getBody());

            if (statusJson.getBoolean("success")) {
                JSONArray dataArray = statusJson.getJSONArray("data");

                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject data = dataArray.getJSONObject(i);

                    if ("Y".equalsIgnoreCase(data.optString("cancelStatus"))) {
                        continue;
                    }

                    FileInfo fileInfo = new FileInfo();
                    fileInfo.setFilename(data.optString("fileName"));
                    fileInfo.setUserId(data.optString("userId"));
                    fileInfo.setPageNum(data.optInt("pageNum"));
                    fileInfo.setServiceType(data.optString("serviceType"));
                    fileInfo.setGroupUID(data.optString("grouP_UID"));
                    fileInfo.setLanguage(data.optString("language"));
                    fileInfo.setLClassification(data.optString("lClassification"));
                    fileInfo.setMClassification(data.optString("mClassification"));
                    fileInfo.setSClassification(data.optString("sClassification"));
                    fileInfo.setClassificationStatus(data.optString("classificationStatus"));
                    fileInfo.setJobType(data.optString("jobType"));
                    fileInfo.setClassificationStartDateTime(data.optString("classificationStartDateTime"));
                    fileInfo.setClassificationEndDateTime(data.optString("classificationEndDateTime"));
                    fileInfo.setTaskName(data.optString("taskName"));

                    fileList.add(fileInfo);
                }
            }
        } else {
            log.warn("STATUS CHECK API 호출 실패: " + response.getStatus() + " - " + response.getStatusText());
        }

        return fileList;
    }

    public void callDivisionApi(String userId, int maxPage, String fileName, String serviceType, String taskName) throws UnirestException {
        String defaultUrl = configLoader.apiURL;
        Unirest.setTimeouts(0, 0);

        fileName = toWindowsPath(fileName);

        String apiUrl = defaultUrl + "/doc/page/division";
        HttpResponse<String> response = Unirest.post(apiUrl)
                .queryString("USERID", userId)
                .queryString("FILENAME", fileName)
                .queryString("MAX_PAGE", maxPage)
                .queryString("SERVICE_TYPE", serviceType)
                .queryString("TASKNAME", taskName)
                .body("").asString();

        if (response.getStatus() == 200) {
            log.info("DIVISION API call successful");
            log.trace("DIVISION API 호출 성공: " + response.getBody());
        } else {
            log.warn("DIVISION API 호출 실패: " + response.getStatus() + " - " + response.getStatusText());
            log.trace("DIVISION API 응답 내용: {}", response.getBody());
        }
    }

    public void callUpdateApi(JSONObject jsonBody) throws UnirestException {
        String defaultUrl = configLoader.apiURL;
        Unirest.setTimeouts(0, 0);

        String filename = jsonBody.optString("fileName");
        jsonBody.put("fileName", toWindowsPath(filename));

        String apiUrl = defaultUrl + "/doc/update";
        HttpResponse<String> response = Unirest.post(apiUrl)
                .header("Content-Type", "application/json")
                .body(jsonBody.toString()).asString();

        log.trace("UPDATE API 응답 코드: " + response.getStatus());
        log.trace("UPDATE API 응답 내용: " + response.getBody());

        if (response.getStatus() == 200) {
            log.info("UPDATE API call successful");
        } else {
            log.warn("UPDATE API 호출 실패: " + response.getStatus() + " - " + response.getStatusText());
            log.trace("UPDATE API 응답 내용: {}", response.getBody());
        }
    }

    public void callDeleteApi(String userId, String fileName, String serviceType) throws UnirestException {
        String defaultUrl = configLoader.apiURL;
        Unirest.setTimeouts(0, 0);

        fileName = toWindowsPath(fileName);

        String apiUrl = defaultUrl + "/doc/delete";
        HttpResponse<String> response = Unirest.delete(apiUrl)
                .queryString("USERID", userId)
                .queryString("FILENAME", fileName)
                .queryString("SERVICE_TYPE", serviceType)
                .body("").asString();

        if (response.getStatus() == 200) {
            log.info("DELETE API call successful");
            log.trace("DELETE API 호출 성공: " + response.getBody());
        } else {
            log.warn("DELETE API 호출 실패: " + response.getStatus() + " - " + response.getStatusText());
            log.trace("DELETE API 응답 내용: {}", response.getBody());
        }
    }

    // 파일 에러 시 -> 오류 코드 호출 / DA 작업 완료 시 -> 성공 코드 호출
    public void callbackApi(FileInfo fileInfo, String imagePath, int resultCode, String message) throws UnirestException {
        String defaultUrl = fileInfo.getUrlData();
        Unirest.setTimeouts(0, 0);

        HttpResponse<String> response;

        imagePath = toWindowsPath(imagePath);

        if (configLoader.useUrlEncoding) {
            response = Unirest.post(defaultUrl)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .field("ocrRequestId", fileInfo.getRequestId())
                    .field("imagePath", imagePath)
                    .field("resultCode", resultCode)
                    .field("resultContents", message)
                    .asString();
        } else {
            response = Unirest.post(defaultUrl)
                    .queryString("ocrRequestId", fileInfo.getRequestId())
                    .queryString("imagePath", imagePath)
                    .queryString("resultCode", resultCode)
                    .queryString("resultContents", message)
                    .body("").asString();
        }

        if (response.getStatus() == 200) {
            log.info("CALLBACK API call successful");
            log.info("CALLBACK API 호출 성공: " + response.getBody());
        } else {
            log.warn("CALLBACK API 호출 실패: " + response.getStatus() + " - " + response.getStatusText());
            log.info("CALLBACK API 응답 내용: {}", response.getBody());
        }
    }

    private String toWindowsPath(String path) {
        if (path == null) return null;

        // 이미 윈도우 경로이면 그대로
        if (path.contains("\\")) {
            return path;
        }

        // 리눅스 경로(/) → 윈도우 경로(\) 변환
        return path.replace("/", "\\");
    }
}
