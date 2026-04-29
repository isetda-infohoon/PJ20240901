package com.isetda.daidpengineclassification;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class SingleInstanceChecker {
    private static FileLock lock;
    private static FileChannel channel;
    private static RandomAccessFile raf;
    private static File lockFile;

    // 중복 실행 확인
    public static boolean check() {
        try {
            // 현재 실행중인 클래스가 포함된 JAR 파일의 경로 가져오기
            String baseDir = AppInfo.getHomePath();

            lockFile = new File(baseDir, "app.lock");
            raf = new RandomAccessFile(lockFile, "rw");
            channel = raf.getChannel();

            lock = channel.tryLock();
            if (lock == null) {
                raf.close();
                return false;
            }

            // 종료 시 lock 해제
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (lock != null) lock.release();
                    if (channel != null) channel.close();
                    if (raf != null) raf.close();
                    if (lockFile != null && lockFile.exists()) {
                        lockFile.delete();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
