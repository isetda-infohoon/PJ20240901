package com.isetda.idpengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class NetworkDriveConnector {
    private static final Logger log = LogManager.getLogger(DocumentService.class);
    private final ConfigLoader configLoader = ConfigLoader.getInstance();


    public boolean connectNetworkDrive() {
        String drivePath = configLoader.drivePath;
        String driveUNCPath = configLoader.driveUNCPath;
        String driveUsername = configLoader.driveUsername;
        String drivePassword = configLoader.drivePassword;

        // 설정 값 검증
        if (drivePath.isEmpty() || driveUNCPath.isEmpty() || driveUsername.isEmpty() || drivePassword.isEmpty()) {
            log.info("네트워크 드라이브 설정 값이 누락되었습니다. 연결을 시도하지 않습니다.");
            return false;
        }

        // net use Z: \\192.168.219.97\share /user:username password /persistent:no
        String command = String.format("net use %s %s /user:%s %s /persistent:no",
                drivePath, driveUNCPath, driveUsername, drivePassword);

        try {
            log.info("네트워크 드라이브 연결 시도: {}", command);
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("네트워크 드라이브 연결 성공: {}", drivePath);
                return true;
            } else {
                log.error("네트워크 드라이브 연결 실패 (exitCode={}): {}", exitCode, command);
                printProcessError(process);
                return false;
            }
        } catch (Exception e) {
            log.error("네트워크 드라이브 연결 중 예외 발생", e);
            return false;
        }
    }

    private void printProcessError(Process process) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.trace("net use 오류: {}", line);
            }
        } catch (IOException e) {
            log.error("오류 스트림 읽기 실패", e);
        }
    }
}
