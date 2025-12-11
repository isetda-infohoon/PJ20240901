package com.isetda.idpengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


//JavaFX 런타임이 이를 처리하는 데 어려움을 겪을 수 있습니다. 특히 JAR 파일로 실행할 때, JavaFX 런타임이 제대로 초기화되지 않아서
public class Main {
    private static final Logger log = LogManager.getLogger(Main.class);
    public static void main(String[] arg){
        // 중복 실행 방지
        if (!SingleInstanceChecker.check()) {
            log.info("IDP Engine is already running.");
            System.exit(1);
        }

        // 트레이 아이콘 설정
        TrayIconManager.setupTrayIcon();

        log.info("==================================== start ====================================");
        // 애플리케이션 실행
        IDPEngineApplication.main(arg);
    }
}