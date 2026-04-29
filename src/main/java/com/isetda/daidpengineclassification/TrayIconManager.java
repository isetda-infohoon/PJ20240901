package com.isetda.daidpengineclassification;

import com.isetda.daidpengineclassification.service.ClassificationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;

public class TrayIconManager {
    private static final Logger log = LogManager.getLogger(Main.class);

    public static void setupTrayIcon() {
        if (!SystemTray.isSupported()) {
            log.info("System tray not supported.");
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().createImage(TrayIconManager.class.getResource("/icon.png"));

        PopupMenu popup = new PopupMenu();
        MenuItem exitItem = new MenuItem("Exit");

        exitItem.addActionListener(e -> {
            log.info("트레이 아이콘을 통해 종료합니다.");
            // 이 메서드를 실행하면 위에서 만든 '셧다운 훅'이 알아서 STOP을 보내고 꺼집니다.
            System.exit(0);
        });

        popup.add(exitItem);

        TrayIcon trayIcon = new TrayIcon(image, "DA.IDP Engine Classification 실행 중", popup);
        trayIcon.setImageAutoSize(true);

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            log.info("Failed to add tray icon");
        }
    }
}
