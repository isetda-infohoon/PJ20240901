package com.isetda.idpengine;

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
            log.info("Shut down IDP Engine");
            System.exit(0);
        });

        popup.add(exitItem);

        TrayIcon trayIcon = new TrayIcon(image, "IDP Engine 실행 중", popup);
        trayIcon.setImageAutoSize(true);

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            log.info("Failed to add tray icon");
        }
    }
}
