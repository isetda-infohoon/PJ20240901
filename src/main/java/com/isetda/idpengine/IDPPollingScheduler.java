package com.isetda.idpengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IDPPollingScheduler {
    private ConfigLoader configLoader = ConfigLoader.getInstance();
    private final IDPEngineController idpEngineController;
    private final Timer timer;
    private static final Logger log = LogManager.getLogger(IDPPollingScheduler.class);

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public IDPPollingScheduler(IDPEngineController idpEngineController) {
        this.idpEngineController = idpEngineController;
        this.timer = new Timer();
    }

    public void startPolling() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                if (idpEngineController.isProcessing()) return;

                idpEngineController.setProcessing(true);
                log.info("start -------");

                idpEngineController.processPendingFiles();
            } catch (Exception e) {
                log.error("Polling 중 예외 발생: {}", e.getMessage(), e);
            } finally {
                idpEngineController.setProcessing(false); // 상태 복구
            }
        }, 0, configLoader.apiCycle, TimeUnit.SECONDS);
    }

//    public void startPolling() {
//        TimerTask task = new TimerTask() {
//            @Override
//            public void run() {
//                if (idpEngineController.isProcessing()) {
//                    //log.info("작업이 아직 진행중입니다. 다음 주기를 건너뜁니다.");
//                    return;
//                }
//
//                idpEngineController.setProcessing(true);
//                //log.info("프로세스 변경 확인: {}", idpEngineController.isProcessing());
//
//                try {
//                    log.info("start -------");
//                    idpEngineController.processPendingFiles();
//                } catch (Exception e) {
//                    e.printStackTrace(); // 예외 처리
//                }
//            }
//        };
//
//        // 0초 후 시작해서 60초(1분)마다 반복
//        timer.scheduleAtFixedRate(task, 0, configLoader.apiCycle * 1000);
//    }
}
