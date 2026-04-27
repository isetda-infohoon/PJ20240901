package com.isetda.daidpengineclassification;

import com.isetda.daidpengineclassification.service.ClassificationService;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

//JavaFX 런타임이 이를 처리하는 데 어려움을 겪을 수 있습니다. 특히 JAR 파일로 실행할 때, JavaFX 런타임이 제대로 초기화되지 않아서
public class Main {
    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] arg) throws UnirestException {
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
//        if (!SingleInstanceChecker.check()) {
//            log.info("IDP Engine is already running.");
//            System.exit(1);
//        }

        // 트레이 아이콘 설정
        TrayIconManager.setupTrayIcon();

        classificationService.callUpdateUserApi("STARTING");

        log.info("==================================== start ====================================");
        // 애플리케이션 실행
        Application.main(arg);
    }

    public static String getProjectVersion() {
        Properties prop = new Properties();
        try (InputStream is = Main.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (is != null) {
                prop.load(is);
                return prop.getProperty("project.version");
            } else {
                // 파일을 못 찾았을 때 로그 (static이므로 로그 객체도 static이어야 함)
                System.err.println("version.properties 파일을 찾을 수 없습니다.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "1.0-UNKNOWN"; // 기본값
    }
}