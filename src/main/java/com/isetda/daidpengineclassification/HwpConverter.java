package com.isetda.daidpengineclassification;

import com.isetda.daidpengineclassification.service.ClassificationService;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.File;

public class HwpConverter {
    private static final Logger log = LogManager.getLogger(HwpConverter.class);
    ConfigLoader configLoader = ConfigLoader.getInstance();

    ExcelService excelService = new ExcelService();

    public void convertToMarkdown(String inputFullPath, String outputDir, FileInfo fileInfo) {
        try {
            String startDateTime = excelService.getCurrentTime();
            String sofficePath = configLoader.officePath;

            // 출력 폴더 생성
            File dir = new File(outputDir);
            if (!dir.exists()) dir.mkdirs();

            // 파일명 rename
            File inputFile = new File(inputFullPath);
            String fileName = inputFile.getName();
            int lastDot = fileName.lastIndexOf(".");
            String baseName = (lastDot == -1) ? fileName : fileName.substring(0, lastDot);
            String ext = (lastDot == -1) ? fileName : fileName.substring(lastDot);

            // 명령어 조합 (UI에서 '다른 이름으로 저장' 하는 것과 동일한 효과)
            // --headless: 화면 안 띄움
            // --convert-to md: 마크다운으로 변환
            // --outdir: 저장 위치 지정
            ProcessBuilder pbMd = new ProcessBuilder(
                    sofficePath,
                    "--headless",
                    "--convert-to", "md",
                    inputFullPath,
                    "--outdir", outputDir
            );

            // 환경 변수에 LibreOffice 경로가 없다면 전체 경로를 적어주세요.
            // 예: "C:\\Program Files\\LibreOffice\\program\\soffice.exe"

            //Process process = pbMd.start();
            //int exitCode = process.waitFor();

            int exitCodeMd = pbMd.start().waitFor();

            if (exitCodeMd == 0) {
                File genMd = new File(outputDir, baseName + ".md"); // 기본 파일명
                File targetFile = new File(outputDir, baseName + "-page1.md"); // 수정된 파일명

                if (genMd.exists()) {
                    if (targetFile.exists()) targetFile.delete(); // 이미 있으면 삭제 후 교체
                    genMd.renameTo(targetFile);
                }
                visionStatusUpdate(fileInfo, startDateTime, "VS");
                log.info(ext + " → MD 변환 성공: " + genMd.getPath());
            }

            ProcessBuilder pbPdf = new ProcessBuilder(
                    sofficePath,
                    "--headless",
                    "--convert-to", "pdf",
                    inputFullPath,
                    "--outdir", outputDir
            );

            int exitCodePdf = pbPdf.start().waitFor();

            if (exitCodePdf == 0) {
                File genPdf = new File(outputDir, baseName + ".pdf");
                //File targetPdf = new File(outputDir, baseName + "-page1.pdf");
                if (genPdf.exists()) {
//                    if (targetPdf.exists()) targetPdf.delete();
//                    genPdf.renameTo(targetPdf);
                    log.info(ext + " → PDF 변환 성공: " + genPdf.getPath());
                }
            }

            if (exitCodeMd == 0 && exitCodePdf == 0) {
                visionStatusUpdate(fileInfo, startDateTime, "VS");
                //log.info("HWP → MD & PDF 변환 성공: " + outputDir);
            } else {
                log.error("변환 중 오류 발생 (MD: " + exitCodeMd + ", PDF: " + exitCodePdf + ")");
            }
        } catch (Exception e) {
            log.error("변환 프로세스 예외 발생", e);
        }
    }

    public void visionStatusUpdate(FileInfo fileInfo, String startDateTime, String visionStatus) throws UnirestException {
        ClassificationService classificationService = new ClassificationService();

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("FileName", fileInfo.getFilename());
        jsonBody.put("PageNum", 0);
        jsonBody.put("UserID", configLoader.apiUserId);
        jsonBody.put("ServiceType", fileInfo.getServiceType());
        jsonBody.put("TaskName", fileInfo.getTaskName());
        jsonBody.put("GroupUID", fileInfo.getGroupUID());
        jsonBody.put("VisionStatus", visionStatus);
        jsonBody.put("VisionStartDateTime", startDateTime);
        jsonBody.put("VisionEndDateTime", excelService.getCurrentTime());

        classificationService.callUpdateApi(jsonBody);
    }
}
