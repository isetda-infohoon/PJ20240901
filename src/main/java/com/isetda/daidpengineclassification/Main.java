package com.isetda.daidpengineclassification;

import com.isetda.daidpengineclassification.service.ClassificationService;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

//JavaFX 런타임이 이를 처리하는 데 어려움을 겪을 수 있습니다. 특히 JAR 파일로 실행할 때, JavaFX 런타임이 제대로 초기화되지 않아서
public class Main {
    public static void main(String[] arg) throws UnirestException {
        // 로그 저장 경로 세팅
        String home = AppInfo.getHomePath();
        System.setProperty("logPath", home);

        Logger log = LogManager.getLogger(Main.class);
        log.info("HOME: {}", home);

        // 엔진 종료 시 엔진 상태 STOP으로 업데이트
        ClassificationService classificationService = new ClassificationService();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                log.info("종료 신호 감지: 서버에 STOP 상태를 전송합니다.");
                classificationService.callUpdateUserApi("STOP");
            } catch (Exception e) {
                log.error("종료 알림 전송 실패: " + e.getMessage());
            }
        }));

        // 중복 실행 방지
        if (!SingleInstanceChecker.check()) {
            log.info("IDP Engine is already running.");
            System.exit(1);
        }

        // 트레이 아이콘 설정
        TrayIconManager.setupTrayIcon();

        // 엔진 시작 시 엔진 상태 STARTING으로 업데이트
        classificationService.callUpdateUserApi("STARTING");

        log.info("==================================== start ====================================");
        // 애플리케이션 실행
        Application.main(arg);
    }
}