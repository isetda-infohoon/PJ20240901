package com.isetda.daidpengineclassification;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppInfo {
    // 실행 파일(JAR) 위치 경로 가져오기 (실행 위치X)
    public static String getHomePath() {
        try {
            String path = AppInfo.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();

            File file = new File(path);

            // JAR 파일 실행 시 (운영 환경)
            if (path.endsWith(".jar")) {
                return file.getParentFile().getAbsolutePath();
            }

            // IDE 실행 시 (개발 환경)
            return System.getProperty("user.dir");
        } catch (Exception e) {
            e.printStackTrace();
            return "."; // 예외 시 현재 디렉토리 반환
        }
    }

    // 프로젝트 버전 가져오기 (프로젝트 버전 관리 파일: resources/version.properties)
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
