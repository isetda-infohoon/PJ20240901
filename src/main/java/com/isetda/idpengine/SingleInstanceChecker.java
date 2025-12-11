package com.isetda.idpengine;

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

    public static boolean check() {
        try {
            lockFile = new File("app.lock");
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
                    lock.release();
                    channel.close();
                    raf.close();
                    lockFile.delete();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
